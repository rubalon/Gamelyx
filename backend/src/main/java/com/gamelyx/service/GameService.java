package com.gamelyx.service;

import com.gamelyx.dto.external.RawgApiDtos;
import com.gamelyx.dto.request.GameRequestDtos;
import com.gamelyx.dto.response.GameResponseDtos;
import com.gamelyx.entity.Game;
import com.gamelyx.entity.User;
import com.gamelyx.entity.UserGameDetails;
import com.gamelyx.mapper.GameMapper;
import com.gamelyx.repository.GameRepository;
import com.gamelyx.repository.UserGameDetailsRepository;
import com.gamelyx.service.external.RawgApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio principal para la gestión de juegos
 *
 * Este servicio coordina:
 * - Búsquedas en APIs externas (RAWG)
 * - Almacenamiento en base de datos local
 * - Gestión de estado usuario-juego
 * - Conversiones entre entidades y DTOs
 */
@Service
@Transactional
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private final RawgApiService rawgApiService;
    private final GameRepository gameRepository;
    private final UserGameDetailsRepository userGameDetailsRepository;
    private final GameMapper gameMapper;

    public GameService(
            RawgApiService rawgApiService,
            GameRepository gameRepository,
            UserGameDetailsRepository userGameDetailsRepository,
            GameMapper gameMapper) {
        this.rawgApiService = rawgApiService;
        this.gameRepository = gameRepository;
        this.userGameDetailsRepository = userGameDetailsRepository;
        this.gameMapper = gameMapper;
    }

    // ===== BÚSQUEDA Y DESCUBRIMIENTO =====

    /**
     * Busca juegos combinando base de datos local y API externa
     *
     * Flujo:
     * 1. Buscar primero en BD local para respuesta rápida
     * 2. Si no hay suficientes resultados, consultar RAWG API
     * 3. Guardar nuevos juegos en BD para futuras consultas
     */
    public Mono<GameResponseDtos.PagedGameResponse> searchGames(
            GameRequestDtos.GameSearchRequest request) {

        logger.info("Searching games: query='{}', page={}, size={}",
                request.getQuery(), request.getPage(), request.getSize());

        // Primero buscar en nuestra BD local
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by("communityRating").descending().and(Sort.by("rating").descending())
        );

        Page<Game> localResults = gameRepository.findByNameContainingIgnoreCase(
                request.getQuery(), pageable);

        // Si tenemos suficientes resultados locales, devolverlos
        if (localResults.getTotalElements() >= request.getSize()) {
            logger.debug("Found {} local results for query '{}'",
                    localResults.getTotalElements(), request.getQuery());
            return Mono.just(gameMapper.toPagedGameResponse(localResults));
        }

        // Si no, complementar con RAWG API
        logger.debug("Local results insufficient ({}), querying RAWG API",
                localResults.getTotalElements());

        return rawgApiService.searchGames(request.getQuery(), request.getPage() + 1, request.getSize())
                .map(rawgResponse -> {
                    // Procesar y guardar juegos de RAWG si no existen
                    rawgResponse.getResults().forEach(this::saveGameFromRawgIfNotExists);

                    // Convertir respuesta RAWG a nuestro formato
                    return convertRawgResponseToPagedResponse(rawgResponse, request);
                })
                .doOnSuccess(response -> logger.info("Search completed: {} total games found",
                        response.getTotalElements()))
                .doOnError(error -> logger.error("Search failed for query '{}': {}",
                        request.getQuery(), error.getMessage()));
    }

    /**
     * Obtiene juegos populares (combinando rating externo y comunidad)
     */
    @Transactional(readOnly = true)
    public GameResponseDtos.PagedGameResponse getPopularGames(int page, int size) {
        logger.info("Getting popular games: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("rating").descending()
                        .and(Sort.by("communityRating").descending()));

        Page<Game> popularGames = gameRepository.findAll(pageable);

        logger.debug("Found {} popular games", popularGames.getTotalElements());
        return gameMapper.toPagedGameResponse(popularGames);
    }

    /**
     * Obtiene detalles completos de un juego por ID
     */
    @Transactional(readOnly = true)
    public Optional<GameResponseDtos.GameDetails> getGameDetails(UUID gameId) {
        logger.debug("Getting game details for ID: {}", gameId);

        return gameRepository.findById(gameId)
                .map(game -> {
                    logger.debug("Game details found: '{}'", game.getName());
                    return gameMapper.toGameDetails(game);
                });
    }

    /**
     * Obtiene detalles de un juego desde RAWG y lo guarda en BD
     */
    public Mono<GameResponseDtos.GameDetails> getOrCreateGameFromRawg(Integer rawgId) {
        logger.info("Getting or creating game from RAWG ID: {}", rawgId);

        // Primero verificar si ya existe en BD
        Optional<Game> existingGame = gameRepository.findByRawgId(rawgId);
        if (existingGame.isPresent()) {
            logger.debug("Game already exists in database: '{}'", existingGame.get().getName());
            return Mono.just(gameMapper.toGameDetails(existingGame.get()));
        }

        // Si no existe, obtener de RAWG y guardar
        return rawgApiService.getGameDetails(rawgId)
                .map(rawgGame -> {
                    Game savedGame = saveGameFromRawg(rawgGame);
                    logger.info("Game created from RAWG: '{}' (ID: {})",
                            savedGame.getName(), savedGame.getId());
                    return gameMapper.toGameDetails(savedGame);
                })
                .doOnError(error -> logger.error("Failed to get/create game from RAWG ID {}: {}",
                        rawgId, error.getMessage()));
    }

    // ===== GESTIÓN USUARIO-JUEGO =====

    /**
     * Añade un juego a la biblioteca del usuario desde RAWG
     */
    public Mono<GameResponseDtos.GameWithUserDetails> addGameToUserLibrary(
            User user, GameRequestDtos.AddGameFromExternalRequest request) {

        logger.info("Adding game to user library: user={}, rawgId={}",
                user.getUsername(), request.getRawgId());

        return getOrCreateGameFromRawg(request.getRawgId())
                .map(gameDetails -> {
                    // Buscar el juego en BD (debe existir después de getOrCreate)
                    Game game = gameRepository.findByRawgId(request.getRawgId())
                            .orElseThrow(() -> new RuntimeException("Game not found after creation"));

                    // Verificar si el usuario ya tiene este juego
                    Optional<UserGameDetails> existing = userGameDetailsRepository
                            .findByUserIdAndGameId(user.getId(), game.getId());

                    UserGameDetails userGameDetails;
                    if (existing.isPresent()) {
                        // Actualizar estado existente
                        userGameDetails = existing.get();
                        if (request.getInitialStatus() != null) {
                            userGameDetails.setStatus(request.getInitialStatus());
                        }
                        if (request.getInitialRating() != null) {
                            userGameDetails.setRating(request.getInitialRating());
                        }
                        if (request.getInitialReviewText() != null) {
                            userGameDetails.setReviewText(request.getInitialReviewText());
                        }
                        logger.debug("Updated existing user game details");
                    } else {
                        // Crear nueva relación
                        userGameDetails = new UserGameDetails(user, game);
                        if (request.getInitialStatus() != null) {
                            userGameDetails.setStatus(request.getInitialStatus());
                        }
                        if (request.getInitialRating() != null) {
                            userGameDetails.setRating(request.getInitialRating());
                        }
                        if (request.getInitialReviewText() != null) {
                            userGameDetails.setReviewText(request.getInitialReviewText());
                        }
                        logger.debug("Created new user game details");
                    }

                    UserGameDetails saved = userGameDetailsRepository.save(userGameDetails);

                    // Recalcular rating de comunidad
                    updateCommunityRating(game);

                    logger.info("Game '{}' added to user '{}' library with status: {}",
                            game.getName(), user.getUsername(), saved.getStatus());

                    return gameMapper.toGameWithUserDetails(game, saved);
                });
    }

    /**
     * Actualiza los detalles de un juego en la biblioteca del usuario
     */
    public Optional<GameResponseDtos.GameWithUserDetails> updateUserGameDetails(
            User user, UUID gameId, GameRequestDtos.UpdateGameDetailsRequest request) {

        logger.info("Updating user game details: user={}, gameId={}",
                user.getUsername(), gameId);

        Optional<UserGameDetails> existingOpt = userGameDetailsRepository
                .findByUserIdAndGameId(user.getId(), gameId);

        if (existingOpt.isEmpty()) {
            logger.warn("User game details not found: user={}, gameId={}",
                    user.getUsername(), gameId);
            return Optional.empty();
        }

        UserGameDetails existing = existingOpt.get();

        // Actualizar campos proporcionados
        if (request.hasStatus()) {
            existing.setStatus(request.getStatus());
            if (request.hasCompletedAt() && request.getStatus() == UserGameDetails.GameStatus.COMPLETED) {
                existing.setCompletedAt(request.getCompletedAt());
            }
        }

        if (request.hasRating()) {
            existing.setRating(request.getRating());
        }

        if (request.hasReviewText()) {
            existing.setReviewText(request.getReviewText());
        }

        UserGameDetails saved = userGameDetailsRepository.save(existing);

        // Recalcular rating de comunidad si cambió el rating
        if (request.hasRating()) {
            updateCommunityRating(existing.getGame());
        }

        logger.info("Updated user game details: status={}, rating={}, hasReview={}",
                saved.getStatus(), saved.getRating(), saved.hasReview());

        return Optional.of(gameMapper.toGameWithUserDetails(existing.getGame(), saved));
    }

    /**
     * Obtiene la biblioteca de juegos del usuario con filtros
     */
    @Transactional(readOnly = true)
    public GameResponseDtos.PagedGameResponse getUserLibrary(
            User user, GameRequestDtos.UserLibraryFilterRequest request) {

        logger.debug("Getting user library: user={}, status={}, page={}",
                user.getUsername(), request.getStatus(), request.getPage());

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
                Sort.by("addedAt").descending());

        Page<UserGameDetails> userGames;

        if (request.getStatus() != null) {
            userGames = userGameDetailsRepository.findByUserIdAndStatus(
                    user.getId(), request.getStatus(), pageable);
        } else {
            userGames = userGameDetailsRepository.findByUserId(user.getId(), pageable);
        }

        // Convertir a GameWithUserDetails
        List<GameResponseDtos.GameSummary> gameSummaries = userGames.getContent().stream()
                .map(ugd -> gameMapper.toGameWithUserDetails(ugd.getGame(), ugd))
                .map(gwud -> (GameResponseDtos.GameSummary) gwud) // Cast a GameSummary para la respuesta
                .toList();

        return new GameResponseDtos.PagedGameResponse(
                gameSummaries,
                userGames.getNumber(),
                userGames.getSize(),
                userGames.getTotalElements(),
                userGames.getTotalPages(),
                userGames.hasNext(),
                userGames.hasPrevious()
        );
    }

    // ===== MÉTODOS PRIVADOS =====

    /**
     * Guarda un juego desde RAWG si no existe en BD
     */
    private void saveGameFromRawgIfNotExists(RawgApiDtos.GameSummary rawgGame) {
        if (gameRepository.findByRawgId(rawgGame.getId()).isEmpty()) {
            saveGameFromRawg(rawgGame);
        }
    }

    /**
     * Convierte y guarda un juego desde RAWG GameSummary
     */
    private Game saveGameFromRawg(RawgApiDtos.GameSummary rawgGame) {
        Game game = new Game();
        game.setRawgId(rawgGame.getId());
        game.setName(rawgGame.getName());
        game.setBackgroundImage(rawgGame.getBackgroundImage());
        game.setRating(rawgGame.getRating());
        game.setReleased(rawgGame.getReleased());
        game.setDataSource("RAWG");
        game.setLastExternalUpdate(LocalDateTime.now());

        // Convertir listas a strings
        if (rawgGame.getPlatforms() != null) {
            game.setPlatforms(gameMapper.listToString(
                    rawgGame.getPlatforms().stream()
                            .map(p -> p.getPlatform().getName())
                            .toList()
            ));
        }

        if (rawgGame.getGenres() != null) {
            game.setGenres(gameMapper.listToString(
                    rawgGame.getGenres().stream()
                            .map(RawgApiDtos.Genre::getName)
                            .toList()
            ));
        }

        return gameRepository.save(game);
    }

    /**
     * Convierte y guarda un juego desde RAWG GameDetails
     */
    private Game saveGameFromRawg(RawgApiDtos.GameDetails rawgGame) {
        Game game = saveGameFromRawg((RawgApiDtos.GameSummary) rawgGame);

        // Campos adicionales de GameDetails
        game.setDescription(rawgGame.getDescription());
        game.setDescriptionRaw(rawgGame.getDescriptionRaw());
        game.setWebsite(rawgGame.getWebsite());
        game.setMetacriticScore(rawgGame.getMetacriticScore());
        game.setAveragePlaytime(rawgGame.getAveragePlaytime());

        return gameRepository.save(game);
    }

    /**
     * Convierte respuesta de RAWG a nuestro formato paginado
     */
    private GameResponseDtos.PagedGameResponse convertRawgResponseToPagedResponse(
            RawgApiDtos.GameSearchResponse rawgResponse,
            GameRequestDtos.GameSearchRequest request) {

        List<GameResponseDtos.GameSummary> games = rawgResponse.getResults().stream()
                .map(rawgGame -> {
                    // Buscar si ya existe en BD (puede haberse guardado en saveGameFromRawgIfNotExists)
                    Optional<Game> existing = gameRepository.findByRawgId(rawgGame.getId());
                    if (existing.isPresent()) {
                        return gameMapper.toGameSummary(existing.get());
                    } else {
                        // Convertir directamente desde RAWG (sin guardar)
                        return convertRawgSummaryToDto(rawgGame);
                    }
                })
                .toList();

        // Calcular paginación basada en la respuesta de RAWG
        int totalPages = (rawgResponse.getCount() + request.getSize() - 1) / request.getSize();

        return new GameResponseDtos.PagedGameResponse(
                games,
                request.getPage(),
                request.getSize(),
                rawgResponse.getCount().longValue(),
                totalPages,
                rawgResponse.getNext() != null,
                rawgResponse.getPrevious() != null
        );
    }

    /**
     * Convierte RawgApiDtos.GameSummary a GameResponseDtos.GameSummary
     */
    private GameResponseDtos.GameSummary convertRawgSummaryToDto(RawgApiDtos.GameSummary rawgGame) {
        GameResponseDtos.GameSummary dto = new GameResponseDtos.GameSummary();
        dto.setRawgId(rawgGame.getId());
        dto.setName(rawgGame.getName());
        dto.setBackgroundImage(rawgGame.getBackgroundImage());
        dto.setRating(rawgGame.getRating());
        dto.setReleased(rawgGame.getReleased());

        if (rawgGame.getPlatforms() != null) {
            dto.setPlatforms(rawgGame.getPlatforms().stream()
                    .map(p -> p.getPlatform().getName())
                    .toList());
        }

        if (rawgGame.getGenres() != null) {
            dto.setGenres(rawgGame.getGenres().stream()
                    .map(RawgApiDtos.Genre::getName)
                    .toList());
        }

        return dto;
    }

    /**
     * Recalcula el rating de comunidad para un juego
     */
    private void updateCommunityRating(Game game) {
        // ✅ findGameAverageRating devuelve Optional<Double>
        Optional<Double> avgRatingOpt = userGameDetailsRepository.findGameAverageRating(game.getId());
        Double avgRating = avgRatingOpt.orElse(null);

        // ✅ countByGameIdAndRatingNotNull ahora existirá
        Long reviewCount = userGameDetailsRepository.countByGameIdAndRatingNotNull(game.getId());

        game.setCommunityRating(avgRating);
        game.setCommunityReviewsCount(reviewCount != null ? reviewCount.intValue() : 0);

        gameRepository.save(game);
    }
}