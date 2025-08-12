package com.gamelyx.service;

import com.gamelyx.dto.external.RawgApiDtos;
import com.gamelyx.dto.GameResponseDtos;
import com.gamelyx.dto.GameRequestDtos.*;
import com.gamelyx.entity.Game;
import com.gamelyx.entity.User;
import com.gamelyx.entity.UserGameDetails;
import com.gamelyx.mapper.GameMapper;
import com.gamelyx.repository.GameRepository;
import com.gamelyx.repository.UserRepository;
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

/**
 * GameService refactorizado - SOLO LÓGICA DE NEGOCIO
 *
 * RESPONSABILIDADES:
 * - Coordinación de flujos de trabajo
 * - Acceso a repositorios y APIs externas
 * - Validaciones y cálculos de negocio
 * - Usar GameMapper para TODAS las conversiones
 *
 * NO HACE:
 * - Conversiones (delegadas a GameMapper)
 * - Transformaciones de datos (delegadas a GameMapper)
 */
@Service
@Transactional
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private final RawgApiService rawgApiService;
    private final GameRepository gameRepository;
    private final UserRepository userRepository; // 🆕 AÑADIDO para buscar User por username
    private final UserGameDetailsRepository userGameDetailsRepository;
    private final GameMapper gameMapper;

    public GameService(
            RawgApiService rawgApiService,
            GameRepository gameRepository,
            UserRepository userRepository, // 🆕 AÑADIDO
            UserGameDetailsRepository userGameDetailsRepository,
            GameMapper gameMapper) {
        this.rawgApiService = rawgApiService;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository; // 🆕 AÑADIDO
        this.userGameDetailsRepository = userGameDetailsRepository;
        this.gameMapper = gameMapper;
    }

    // ===== FUNCIONALIDAD 1: BÚSQUEDA =====

    /**
     * 🎯 Para: GET /search
     * SIN CAMBIOS: No requiere autenticación
     */
    public Mono<GameResponseDtos.GameSearchResultsDto> searchGamesForResults(String query, int page, int size) {
        logger.info("🔍 SEARCH SERVICE: query='{}', page={}, size={}", query, page, size);

        return rawgApiService.searchGames(query, page, size)
                .map(rawgResponse -> {
                    List<GameResponseDtos.GameSearchItem> gameItems = rawgResponse.getResults().stream()
                            .map(rawgGame -> {
                                Optional<Game> existingGame = gameRepository.findByRawgId(rawgGame.getId());
                                return gameMapper.rawgSummaryToSearchItem(rawgGame, existingGame);
                            })
                            .toList();

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

    // ===== FUNCIONALIDAD 2: PÁGINA DEL JUEGO =====

    /**
     * 🎯 Para: GET /game/{identifier} (usuario autenticado)
     *
     * 🔧 REFACTORIZADO: Recibe username y busca User entity
     */
    public Mono<GameResponseDtos.GamePageDto> getGamePageWithUserData(String identifier, String username) {
        logger.info("🎮 GAME PAGE WITH USER: identifier='{}', user={}", identifier, username);

        // 🆕 NUEVO: Buscar User entity por username
        User user = findUserByUsername(username);

        return resolveGameFromIdentifier(identifier)
                .map(game -> {
                    GameResponseDtos.GamePageDto gamePageDto = gameMapper.gameToPageDto(game);

                    // LÓGICA DE NEGOCIO: Obtener mi estado personal
                    Optional<UserGameDetails> myGameDetails = userGameDetailsRepository
                            .findByUserIdAndGameId(user.getId(), game.getId());

                    if (myGameDetails.isPresent()) {
                        gamePageDto.setMyStatus(gameMapper.userGameDetailsToMyStatus(myGameDetails.get()));
                    }

                    // LÓGICA DE NEGOCIO: Obtener reviews de otros usuarios
                    List<UserGameDetails> otherReviews = getOtherUsersReviews(game.getId(), user.getId(), 3);
                    gamePageDto.setRecentReviews(gameMapper.userGameDetailsListToOtherReviews(otherReviews));

                    logger.debug("Game page built: '{}' with user data", game.getName());
                    return gamePageDto;
                });
    }

    /**
     * 🎯 Para: GET /game/{identifier} (usuario NO autenticado)
     *
     * LÓGICA DE NEGOCIO:
     * - Resolver identifier → Game
     * - Obtener reviews públicas
     * - Usar GameMapper para conversiones
     */
    public Mono<GameResponseDtos.GamePageDto> getGamePagePublic(String identifier) {
        logger.info("🎮 GAME PAGE PUBLIC: identifier='{}'", identifier);

        return resolveGameFromIdentifier(identifier)
                .map(game -> {
                    GameResponseDtos.GamePageDto gamePageDto = gameMapper.gameToPageDto(game);

                    // LÓGICA DE NEGOCIO: Obtener reviews públicas
                    List<UserGameDetails> recentReviews = getPublicReviews(game.getId(), 3);
                    gamePageDto.setRecentReviews(gameMapper.userGameDetailsListToOtherReviews(recentReviews));

                    logger.debug("Game page built: '{}' (public)", game.getName());
                    return gamePageDto;
                });
    }

    // ===== FUNCIONALIDAD 3: ACTUALIZAR MI REVIEW =====

    /**
     * 🎯 Para: PUT /game/{identifier}/my-game-details
     *
     * 🔧 REFACTORIZADO: Recibe username y busca User entity
     */
    public Mono<GameResponseDtos.UpdatedGameStatusDto> updateMyGameReview(
            String identifier, String username, UpdateMyGameRequest request) {

        logger.info("💭 UPDATE REVIEW SERVICE: identifier='{}', user={}, status={}, rating={}",
                identifier, username, request.status(), request.rating());

        // 🆕 NUEVO: Buscar User entity por username
        User user = findUserByUsername(username);

        return resolveGameFromIdentifier(identifier)
                .map(game -> {
                    // LÓGICA DE NEGOCIO: Buscar o crear UserGameDetails
                    UserGameDetails userGameDetails = userGameDetailsRepository
                            .findByUserIdAndGameId(user.getId(), game.getId())
                            .orElse(new UserGameDetails(user, game));

                    // LÓGICA DE NEGOCIO: Aplicar cambios del request
                    applyGameDetailsUpdates(userGameDetails, request);

                    // LÓGICA DE NEGOCIO: Guardar cambios
                    UserGameDetails saved = userGameDetailsRepository.save(userGameDetails);

                    // LÓGICA DE NEGOCIO: Recalcular community rating
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
                            game.getName(), username, updatedGame.getCommunityRating());

                    return response;
                });
    }

    // ===== FUNCIONALIDAD 4: MIS REVIEWS =====

    /**
     * 🎯 Para: GET /my-reviews (para home - sin paginación)
     *
     * 🔧 REFACTORIZADO: Recibe username y busca User entity
     */
    public List<GameResponseDtos.MyReviewDto> getMyRecentReviews(String username, int limit) {
        logger.info("📝 MY RECENT REVIEWS: user={}, limit={}", username, limit);

        // 🆕 NUEVO: Buscar User entity por username
        User user = findUserByUsername(username);

        Pageable pageable = PageRequest.of(0, limit, Sort.by("reviewUpdatedAt").descending());

        List<UserGameDetails> userReviews = userGameDetailsRepository.findUserReviews(user.getId(), pageable)
                .getContent();

        return gameMapper.userGameDetailsListToMyReviews(userReviews);
    }

    /**
     * 🎯 Para: GET /my-reviews (para página completa - con paginación)
     *
     * 🔧 REFACTORIZADO: Recibe username y busca User entity
     */
    public GameResponseDtos.MyReviewsResponseDto getMyReviewsPaginated(String username, int page, int size) {
        logger.info("📝 MY REVIEWS PAGINATED: user={}, page={}, size={}", username, page, size);

        // 🆕 NUEVO: Buscar User entity por username
        User user = findUserByUsername(username);

        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewUpdatedAt").descending());
        var reviewsPage = userGameDetailsRepository.findUserReviews(user.getId(), pageable);

        List<GameResponseDtos.MyReviewDto> reviewDtos = gameMapper.userGameDetailsListToMyReviews(
                reviewsPage.getContent());

        return GameResponseDtos.MyReviewsResponseDto.forPage(
                reviewDtos,
                page,
                reviewsPage.getTotalPages(),
                reviewsPage.getTotalElements(),
                reviewsPage.hasNext()
        );
    }

    // ===== 🆕 NUEVO MÉTODO: BÚSQUEDA DE USER =====

    /**
     * 🆕 NUEVO: Buscar User entity por username
     *
     * RESPONSABILIDAD DEL SERVICE:
     * - GameService es responsable de buscar User cuando lo necesita
     * - JwtAuthenticationFilter se mantiene stateless
     * - Separación clara de responsabilidades
     */
    private User findUserByUsername(String username) {
        logger.debug("🔍 Searching user by username: {}", username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found by username: {}", username);
                    return new RuntimeException("Usuario no encontrado: " + username);
                });
    }

    // ===== MÉTODOS PRIVADOS - LÓGICA DE NEGOCIO (SIN CAMBIOS) =====

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

    private Mono<Game> resolveByRawgId(Integer rawgId) {
        Optional<Game> existing = gameRepository.findByRawgId(rawgId);
        if (existing.isPresent()) {
            logger.debug("Game found in database: rawgId={}, slug='{}'", rawgId, existing.get().getSlug());
            return Mono.just(existing.get());
        }

        logger.debug("Game not in database, fetching from RAWG: rawgId={}", rawgId);
        return rawgApiService.getGameDetails(rawgId)
                .map(this::saveGameFromRawg);
    }

    /**
     * LÓGICA DE NEGOCIO: Resolver juego por slug
     */
    private Mono<Game> resolveBySlug(String slug) {
        // Buscar en BD primero
        Optional<Game> existing = gameRepository.findBySlug(slug);
        if (existing.isPresent()) {
            logger.debug("Game found by slug: '{}' → '{}'", slug, existing.get().getName());
            return Mono.just(existing.get());
        }

        // Si no existe, obtener de RAWG por slug
        logger.debug("Game not found by slug '{}', fetching from RAWG", slug);
        return rawgApiService.getGameDetailsBySlug(slug)
                .map(this::saveGameFromRawg)
                .doOnError(error -> logger.error("Failed to get game by slug '{}': {}", slug, error.getMessage()));
    }

    /**
     * LÓGICA DE NEGOCIO: Guardar Game desde RAWG usando GameMapper
     */
    private Game saveGameFromRawg(RawgApiDtos.GameDetails rawgGame) {
        Game game = gameMapper.rawgDetailsToGameEntity(rawgGame);
        Game saved = gameRepository.save(game);
        logger.info("Game saved from RAWG: '{}' with slug '{}' (ID: {})",
                saved.getName(), saved.getSlug(), saved.getId());
        return saved;
    }

    /**
     * LÓGICA DE NEGOCIO: Aplicar actualizaciones a UserGameDetails
     */
    private void applyGameDetailsUpdates(UserGameDetails userGameDetails, UpdateMyGameRequest request) {
        if (request.status() != null) {
            userGameDetails.setStatus(UserGameDetails.GameStatus.valueOf(request.status()));
        }
        if (request.rating() != null) {
            userGameDetails.setRating(request.rating());
        }
        if (request.reviewText() != null) {
            userGameDetails.setReviewText(request.reviewText());
        }
    }

    /**
     * LÓGICA DE NEGOCIO: Recalcular community rating para un juego
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
     * LÓGICA DE NEGOCIO: Obtener reviews de otros usuarios (excluye al usuario actual)
     */
    private List<UserGameDetails> getOtherUsersReviews(java.util.UUID gameId, java.util.UUID excludeUserId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("reviewUpdatedAt").descending());

        // TODO: Crear query específica en repositorio que excluya al usuario actual
        // Por ahora, usar el método existente y filtrar
        return userGameDetailsRepository.findGamePublicReviews(gameId, pageable)
                .getContent()
                .stream()
                .filter(ugd -> !ugd.getUser().getId().equals(excludeUserId))
                .limit(limit)
                .toList();
    }

    /**
     * LÓGICA DE NEGOCIO: Obtener reviews públicas para usuarios no autenticados
     */
    private List<UserGameDetails> getPublicReviews(java.util.UUID gameId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("reviewUpdatedAt").descending());
        return userGameDetailsRepository.findGamePublicReviews(gameId, pageable)
                .getContent()
                .stream()
                .limit(limit)
                .toList();
    }

    // ===== MÉTODOS DE UTILIDAD SIMPLES =====

    /**
     * Calcular total de páginas para paginación
     */
    private int calculateTotalPages(Integer totalResults, int size) {
        return (totalResults + size - 1) / size;
    }
}