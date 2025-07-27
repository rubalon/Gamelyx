package com.gamelyx.service;

import com.gamelyx.dto.external.RawgApiDtos;
import com.gamelyx.dto.GameResponseDtos;
import com.gamelyx.dto.GameRequestDtos.*;
import com.gamelyx.entity.Game;
import com.gamelyx.entity.User;
import com.gamelyx.entity.UserGameDetails;
import com.gamelyx.mapper.GameMapper;
import com.gamelyx.repository.GameRepository;
import com.gamelyx.repository.UserGameDetailsRepository;
import com.gamelyx.service.external.RawgApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * GameService rediseñado para funcionalidades centradas en el frontend
 *
 * FILOSOFÍA:
 * - Métodos que coinciden exactamente con endpoints del controller
 * - Transparencia total entre RAWG y BD para el frontend
 * - Un método = una funcionalidad completa
 * - Generación automática de slugs únicos
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

    // ===== FUNCIONALIDAD 1: BÚSQUEDA PARA RESULTADOS =====

    /**
     * 🎯 Para: GET /search
     *
     * LÓGICA:
     * - Siempre usar RAWG API (datos frescos)
     * - Convertir respuesta RAWG a nuestros DTOs
     * - No guardar en BD aquí (solo cuando usuario visite página específica)
     */
    public Mono<GameResponseDtos.GameSearchResultsDto> searchGamesForResults(String query, int page, int size) {
        logger.info("🔍 SEARCH SERVICE: query='{}', page={}, size={}", query, page, size);

        return rawgApiService.searchGames(query, page, size)
                .map(rawgResponse -> {
                    // Convertir items de RAWG a nuestros DTOs
                    List<GameResponseDtos.GameSearchItem> gameItems = rawgResponse.getResults().stream()
                            .map(this::convertRawgToSearchItem)
                            .toList();

                    // Construir respuesta con paginación
                    GameResponseDtos.GameSearchResultsDto response = new GameResponseDtos.GameSearchResultsDto(
                            gameItems,
                            page,
                            calculateTotalPages(rawgResponse.getCount(), size),
                            rawgResponse.getCount().longValue(),
                            rawgResponse.getNext() != null,
                            query
                    );

                    logger.debug("Search results: {} games found", gameItems.size());
                    return response;
                })
                .doOnError(error -> logger.error("Search failed for query '{}': {}", query, error.getMessage()));
    }

    // ===== FUNCIONALIDAD 2: PÁGINA COMPLETA DEL JUEGO =====

    /**
     * 🎯 Para: GET /game/{identifier} (usuario autenticado)
     *
     * LÓGICA:
     * - Resolver identifier (rawgId o slug)
     * - Si no existe en BD → obtener de RAWG + guardar + generar slug
     * - Si existe en BD → usar datos locales
     * - SIEMPRE incluir: mi estado + reviews de otros
     */
    public Mono<GameResponseDtos.GamePageDto> getGamePageWithUserData(String identifier, User user) {
        logger.info("🎮 GAME PAGE WITH USER: identifier='{}', user={}", identifier, user.getUsername());

        return resolveGameFromIdentifier(identifier)
                .map(game -> {
                    GameResponseDtos.GamePageDto gamePageDto = buildGamePageDto(game);

                    // Agregar mi estado personal
                    Optional<UserGameDetails> myGameDetails = userGameDetailsRepository
                            .findByUserIdAndGameId(user.getId(), game.getId());

                    if (myGameDetails.isPresent()) {
                        gamePageDto.setMyStatus(convertToMyGameStatus(myGameDetails.get()));
                    }
                    // Si no hay myGameDetails, myStatus queda null (usuario no ha interactuado)

                    // Agregar reviews de otros usuarios (últimas 3)
                    List<UserGameDetails> otherReviews = getOtherUsersReviews(game.getId(), user.getId(), 3);
                    gamePageDto.setRecentReviews(
                            otherReviews.stream()
                                    .map(this::convertToOtherUserReview)
                                    .toList()
                    );

                    logger.debug("Game page built: '{}' with user data", game.getName());
                    return gamePageDto;
                });
    }

    /**
     * 🎯 Para: GET /game/{identifier} (usuario NO autenticado)
     *
     * LÓGICA:
     * - Igual que arriba pero sin mi estado personal
     * - Solo datos del juego + reviews de otros
     */
    public Mono<GameResponseDtos.GamePageDto> getGamePagePublic(String identifier) {
        logger.info("🎮 GAME PAGE PUBLIC: identifier='{}'", identifier);

        return resolveGameFromIdentifier(identifier)
                .map(game -> {
                    GameResponseDtos.GamePageDto gamePageDto = buildGamePageDto(game);
                    // myStatus queda null (no autenticado)

                    // Reviews de otros usuarios (últimas 3)
                    List<UserGameDetails> recentReviews = getPublicReviews(game.getId(), 3);
                    gamePageDto.setRecentReviews(
                            recentReviews.stream()
                                    .map(this::convertToOtherUserReview)
                                    .toList()
                    );

                    logger.debug("Game page built: '{}' (public)", game.getName());
                    return gamePageDto;
                });
    }

    // ===== FUNCIONALIDAD 3: ACTUALIZAR MI REVIEW/ESTADO =====

    /**
     * 🎯 Para: PUT /game/{identifier}/my-review
     *
     * LÓGICA:
     * - Resolver identifier → Game
     * - Crear/actualizar UserGameDetails
     * - Recalcular community rating
     * - Devolver estado actualizado + community rating nuevo
     */
    public Mono<GameResponseDtos.UpdatedGameStatusDto> updateMyGameReview(
            String identifier, User user, UpdateMyGameRequest request) {

        logger.info("💭 UPDATE REVIEW SERVICE: identifier='{}', user={}, status={}, rating={}",
                identifier, user.getUsername(), request.status(), request.rating());

        return resolveGameFromIdentifier(identifier)
                .map(game -> {
                    // Buscar o crear UserGameDetails
                    UserGameDetails userGameDetails = userGameDetailsRepository
                            .findByUserIdAndGameId(user.getId(), game.getId())
                            .orElse(new UserGameDetails(user, game));

                    // Aplicar cambios del request
                    if (request.status() != null) {
                        userGameDetails.setStatus(UserGameDetails.GameStatus.valueOf(request.status()));
                    }
                    if (request.rating() != null) {
                        userGameDetails.setRating(request.rating());
                    }
                    if (request.reviewText() != null) {
                        userGameDetails.setReviewText(request.reviewText());
                    }

                    // Guardar cambios
                    UserGameDetails saved = userGameDetailsRepository.save(userGameDetails);

                    // Recalcular community rating
                    updateCommunityRating(game);

                    // Recargar game con community rating actualizado
                    Game updatedGame = gameRepository.findById(game.getId()).orElse(game);

                    // Construir respuesta
                    GameResponseDtos.UpdatedGameStatusDto response = new GameResponseDtos.UpdatedGameStatusDto(
                            saved.getStatus() != null ? saved.getStatus().name() : null,
                            saved.getRating(),
                            saved.getReviewText(),
                            LocalDateTime.now(),
                            updatedGame.getCommunityRating(),
                            updatedGame.getCommunityReviewsCount()
                    );

                    logger.info("Review updated: '{}' for user '{}' → community rating: {}",
                            game.getName(), user.getUsername(), updatedGame.getCommunityRating());

                    return response;
                });
    }

    // ===== FUNCIONALIDAD 4: MIS REVIEWS =====

    /**
     * 🎯 Para: GET /my-reviews (para home - sin paginación)
     */
    public List<GameResponseDtos.MyReviewDto> getMyRecentReviews(User user, int limit) {
        logger.info("📝 MY RECENT REVIEWS: user={}, limit={}", user.getUsername(), limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by("reviewUpdatedAt").descending());

        return userGameDetailsRepository.findUserReviews(user.getId(), pageable)
                .getContent()
                .stream()
                .map(this::convertToMyReviewDto)
                .toList();
    }

    /**
     * 🎯 Para: GET /my-reviews (para página completa - con paginación)
     */
    public GameResponseDtos.MyReviewsResponseDto getMyReviewsPaginated(User user, int page, int size) {
        logger.info("📝 MY REVIEWS PAGINATED: user={}, page={}, size={}",
                user.getUsername(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewUpdatedAt").descending());
        var reviewsPage = userGameDetailsRepository.findUserReviews(user.getId(), pageable);

        List<GameResponseDtos.MyReviewDto> reviewDtos = reviewsPage.getContent()
                .stream()
                .map(this::convertToMyReviewDto)
                .toList();

        return GameResponseDtos.MyReviewsResponseDto.forPage(
                reviewDtos,
                page,
                reviewsPage.getTotalPages(),
                reviewsPage.getTotalElements(),
                reviewsPage.hasNext()
        );
    }

    // ===== MÉTODOS PRIVADOS DE RESOLUCIÓN Y CONVERSIÓN =====

    /**
     * Resuelve identifier (rawgId o slug) → Game entity
     * Si no existe, lo obtiene de RAWG y lo guarda
     */
    private Mono<Game> resolveGameFromIdentifier(String identifier) {
        boolean isRawgId = identifier.matches("\\d+");

        if (isRawgId) {
            Integer rawgId = Integer.parseInt(identifier);
            logger.debug("Resolving as rawgId: {}", rawgId);
            return resolveByRawgId(rawgId);
        } else {
            logger.debug("Resolving as slug: '{}'", identifier);
            return resolveBySlug(identifier);
        }
    }

    /**
     * Resuelve juego por rawgId
     */
    private Mono<Game> resolveByRawgId(Integer rawgId) {
        // Buscar en BD primero
        Optional<Game> existing = gameRepository.findByRawgId(rawgId);
        if (existing.isPresent()) {
            logger.debug("Game found in database: rawgId={}, slug='{}'", rawgId, existing.get().getSlug());
            return Mono.just(existing.get());
        }

        // Si no existe, obtener de RAWG por ID
        logger.debug("Game not in database, fetching from RAWG: rawgId={}", rawgId);
        return rawgApiService.getGameDetails(rawgId)
                .map(this::saveGameFromRawgDetails);
    }

    /**
     * Resuelve juego por slug
     */
    private Mono<Game> resolveBySlug(String slug) {
        // Buscar en BD primero
        Optional<Game> existing = gameRepository.findBySlug(slug);
        if (existing.isPresent()) {
            logger.debug("Game found by slug: '{}' → '{}'", slug, existing.get().getName());
            return Mono.just(existing.get());
        }

        // Si no existe, obtener DIRECTAMENTE de RAWG por slug
        logger.debug("Game not found by slug '{}', fetching from RAWG", slug);
        return rawgApiService.getGameDetailsBySlug(slug)
                .map(this::saveGameFromRawgDetails)
                .doOnError(error -> logger.error("Failed to get game by slug '{}': {}", slug, error.getMessage()));
    }

    /**
     * Convierte RawgApiDtos.GameSummary → GameDtos.GameSearchItem
     */
    private GameResponseDtos.GameSearchItem convertRawgToSearchItem(RawgApiDtos.GameSummary rawgGame) {
        // Verificar si ya existe en BD para incluir el slug de BD (puede ser diferente)
        Optional<Game> existingGame = gameRepository.findByRawgId(rawgGame.getId());
        String slug = existingGame.map(Game::getSlug).orElse(rawgGame.getSlug()); // Usar slug de BD o RAWG

        return new GameResponseDtos.GameSearchItem(
                rawgGame.getId(),
                slug, // Slug disponible desde RAWG o BD
                rawgGame.getName(),
                rawgGame.getBackgroundImage(), // Usar como coverImage para lista
                truncateDescription(rawgGame.getName()), // TODO: obtener descripción real
                rawgGame.getRating(),
                rawgGame.getReleased(),
                rawgGame.getPlatforms() != null ?
                        rawgGame.getPlatforms().stream().map(p -> p.getPlatform().getName()).toList() :
                        List.of(),
                rawgGame.getGenres() != null ?
                        rawgGame.getGenres().stream().map(RawgApiDtos.Genre::getName).toList() :
                        List.of()
        );
    }

    /**
     * Construye GamePageDto a partir de Game entity
     */
    private GameResponseDtos.GamePageDto buildGamePageDto(Game game) {
        GameResponseDtos.GamePageDto dto = new GameResponseDtos.GamePageDto();

        // Datos básicos
        dto.setRawgId(game.getRawgId());
        dto.setSlug(game.getSlug()); // Usar slug real de BD
        dto.setName(game.getName());
        dto.setDescription(game.getDescription());
        dto.setDescriptionRaw(game.getDescriptionRaw());
        dto.setBackgroundImage(game.getBackgroundImage());
        dto.setCoverImage(game.getCoverImage()); // Usar coverImage real
        dto.setScreenshots(game.getScreenshotsList());

        // Ratings
        dto.setRating(game.getRating());
        dto.setCommunityRating(game.getCommunityRating());
        dto.setTotalCommunityReviews(game.getCommunityReviewsCount());

        // Metadata
        dto.setReleased(game.getReleased());
        dto.setWebsite(game.getWebsite());
        dto.setMetacriticScore(game.getMetacriticScore());
        dto.setAveragePlaytime(game.getAveragePlaytime());

        // Listas
        dto.setPlatforms(game.getPlatformsList());
        dto.setGenres(game.getGenresList());
        dto.setDevelopers(parseDevelopers(game.getDevelopers()));
        dto.setPublishers(parsePublishers(game.getPublishers()));
        dto.setTags(parseTags(game.getTags()));

        dto.setLastUpdated(game.getUpdatedAt());

        return dto;
    }

    /**
     * Convierte UserGameDetails → MyGameStatus
     */
    private GameResponseDtos.MyGameStatus convertToMyGameStatus(UserGameDetails ugd) {
        return new GameResponseDtos.MyGameStatus(
                ugd.getStatus() != null ? ugd.getStatus().name() : null,
                ugd.getRating(),
                ugd.getReviewText(),
                ugd.getReviewUpdatedAt()
        );
    }

    /**
     * Convierte UserGameDetails → OtherUserReview
     */
    private GameResponseDtos.OtherUserReview convertToOtherUserReview(UserGameDetails ugd) {
        return new GameResponseDtos.OtherUserReview(
                ugd.getUser().getUsername(),
                ugd.getRating(),
                truncateReviewText(ugd.getReviewText(), 200), // Truncar para preview
                ugd.getStatus() != null ? ugd.getStatus().name() : null,
                ugd.getReviewCreatedAt()
        );
    }

    /**
     * Convierte UserGameDetails → MyReviewDto
     */
    private GameResponseDtos.MyReviewDto convertToMyReviewDto(UserGameDetails ugd) {
        Game game = ugd.getGame();
        return new GameResponseDtos.MyReviewDto(
                game.getRawgId(),
                game.getSlug(), // Usar slug real de BD
                game.getName(),
                game.getCoverImage() != null ? game.getCoverImage() : game.getBackgroundImage(), // coverImage o fallback
                ugd.getRating(),
                ugd.getReviewText(),
                ugd.getStatus() != null ? ugd.getStatus().name() : null,
                ugd.getReviewCreatedAt(),
                ugd.getReviewUpdatedAt()
        );
    }

    /**
     * Guarda Game desde RawgApiDtos.GameDetails usando slug de RAWG
     */
    private Game saveGameFromRawgDetails(RawgApiDtos.GameDetails rawgGame) {
        Game game = new Game();
        game.setRawgId(rawgGame.getId());
        game.setName(rawgGame.getName());
        game.setSlug(rawgGame.getSlug()); // ✅ Usar slug directo de RAWG
        game.setDescription(rawgGame.getDescription());
        game.setDescriptionRaw(rawgGame.getDescriptionRaw());
        game.setBackgroundImage(rawgGame.getBackgroundImage());
        game.setCoverImage(rawgGame.getBackgroundImageAdditional()); // Mapear backgroundImageAdditional → coverImage
        game.setRating(rawgGame.getRating());
        game.setRatingTop(rawgGame.getRatingTop());
        game.setReleased(rawgGame.getReleased());
        game.setWebsite(rawgGame.getWebsite());
        game.setMetacriticScore(rawgGame.getMetacriticScore());
        game.setAveragePlaytime(rawgGame.getAveragePlaytime());
        game.setDataSource("RAWG");
        game.setLastExternalUpdate(LocalDateTime.now());

        // Convertir listas
        if (rawgGame.getPlatforms() != null) {
            game.setPlatformsList(rawgGame.getPlatforms().stream()
                    .map(p -> p.getPlatform().getName())
                    .toList());
        }
        if (rawgGame.getGenres() != null) {
            game.setGenresList(rawgGame.getGenres().stream()
                    .map(RawgApiDtos.Genre::getName)
                    .toList());
        }
        // TODO: Agregar developers, publishers, tags, screenshots

        Game saved = gameRepository.save(game);
        logger.info("Game saved from RAWG: '{}' with slug '{}' (ID: {})",
                saved.getName(), saved.getSlug(), saved.getId());
        return saved;
    }

    /**
     * Genera slug base desde nombre del juego
     */
    private String generateSlugFromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "unnamed-game";
        }

        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "") // Remover caracteres especiales
                .replaceAll("\\s+", "-")         // Espacios → guiones
                .replaceAll("-+", "-")           // Múltiples guiones → uno solo
                .replaceAll("^-|-$", "");        // Remover guiones al inicio/final
    }

    /**
     * Asegura que el slug sea único agregando número si es necesario
     */
    private String ensureUniqueSlug(String baseSlug) {
        String candidateSlug = baseSlug;
        int counter = 1;

        while (gameRepository.existsBySlug(candidateSlug)) {
            candidateSlug = baseSlug + "-" + counter;
            counter++;

            // Prevenir bucle infinito
            if (counter > 1000) {
                candidateSlug = baseSlug + "-" + System.currentTimeMillis();
                break;
            }
        }

        logger.debug("Generated unique slug: '{}' from base: '{}'", candidateSlug, baseSlug);
        return candidateSlug;
    }

    /**
     * Recalcula community rating para un juego
     */
    private void updateCommunityRating(Game game) {
        Optional<Double> avgRating = userGameDetailsRepository.findGameAverageRating(game.getId());
        Long reviewCount = userGameDetailsRepository.countByGameIdAndRatingNotNull(game.getId());

        game.setCommunityRating(avgRating.orElse(null));
        game.setCommunityReviewsCount(reviewCount != null ? reviewCount.intValue() : 0);

        gameRepository.save(game);
        logger.debug("Community rating updated for '{}': {} (from {} reviews)",
                game.getName(), avgRating.orElse(null), reviewCount);
    }

    /**
     * Obtiene reviews de otros usuarios (excluye al usuario actual)
     */
    private List<UserGameDetails> getOtherUsersReviews(UUID gameId, UUID excludeUserId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("reviewUpdatedAt").descending());

        // TODO: Crear query en repositorio que excluya al usuario actual
        // Por ahora, usar el método existente y filtrar
        return userGameDetailsRepository.findGamePublicReviews(gameId, pageable)
                .getContent()
                .stream()
                .filter(ugd -> !ugd.getUser().getId().equals(excludeUserId))
                .limit(limit)
                .toList();
    }

    /**
     * Obtiene reviews públicas para usuarios no autenticados
     */
    private List<UserGameDetails> getPublicReviews(UUID gameId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("reviewUpdatedAt").descending());
        return userGameDetailsRepository.findGamePublicReviews(gameId, pageable)
                .getContent()
                .stream()
                .limit(limit)
                .toList();
    }

    // ===== MÉTODOS DE UTILIDAD =====

    private int calculateTotalPages(Integer totalResults, int size) {
        return (totalResults + size - 1) / size;
    }

    private String truncateDescription(String text) {
        return text != null && text.length() > 150 ? text.substring(0, 150) + "..." : text;
    }

    private String truncateReviewText(String text, int maxLength) {
        return text != null && text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private List<String> parseDevelopers(String developers) {
        return developers != null ? List.of(developers.split(",")) : List.of();
    }

    private List<String> parsePublishers(String publishers) {
        return publishers != null ? List.of(publishers.split(",")) : List.of();
    }

    private List<String> parseTags(String tags) {
        return tags != null ? List.of(tags.split(",")) : List.of();
    }
}