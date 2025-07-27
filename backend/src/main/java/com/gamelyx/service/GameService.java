package com.gamelyx.service;

import com.gamelyx.dto.external.RawgApiDtos;
import com.gamelyx.dto.GameDtos;
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
    public Mono<GameDtos.GameSearchResultsDto> searchGamesForResults(String query, int page, int size) {
        logger.info("🔍 SEARCH SERVICE: query='{}', page={}, size={}", query, page, size);

        return rawgApiService.searchGames(query, page, size)
                .map(rawgResponse -> {
                    // Convertir items de RAWG a nuestros DTOs
                    List<GameDtos.GameSearchItem> gameItems = rawgResponse.getResults().stream()
                            .map(this::convertRawgToSearchItem)
                            .toList();

                    // Construir respuesta con paginación
                    GameDtos.GameSearchResultsDto response = new GameDtos.GameSearchResultsDto(
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
    public Mono<GameDtos.GamePageDto> getGamePageWithUserData(String identifier, User user) {
        logger.info("🎮 GAME PAGE WITH USER: identifier='{}', user={}", identifier, user.getUsername());

        return resolveGameFromIdentifier(identifier)
                .map(game -> {
                    GameDtos.GamePageDto gamePageDto = buildGamePageDto(game);

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
    public Mono<GameDtos.GamePageDto> getGamePagePublic(String identifier) {
        logger.info("🎮 GAME PAGE PUBLIC: identifier='{}'", identifier);

        return resolveGameFromIdentifier(identifier)
                .map(game -> {
                    GameDtos.GamePageDto gamePageDto = buildGamePageDto(game);
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
    public Mono<GameDtos.UpdatedGameStatusDto> updateMyGameReview(
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
                    GameDtos.UpdatedGameStatusDto response = new GameDtos.UpdatedGameStatusDto(
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
    public List<GameDtos.MyReviewDto> getMyRecentReviews(User user, int limit) {
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
    public GameDtos.MyReviewsResponseDto getMyReviewsPaginated(User user, int page, int size) {
        logger.info("📝 MY REVIEWS PAGINATED: user={}, page={}, size={}",
                user.getUsername(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewUpdatedAt").descending());
        var reviewsPage = userGameDetailsRepository.findUserReviews(user.getId(), pageable);

        List<GameDtos.MyReviewDto> reviewDtos = reviewsPage.getContent()
                .stream()
                .map(this::convertToMyReviewDto)
                .toList();

        return GameDtos.MyReviewsResponseDto.forPage(
                reviewDtos,
                page,
                reviewsPage.getTotalPages(),
                reviewsPage.getTotalElements(),
                reviewsPage.hasNext()
        );
    }

    // ===== MÉTODOS PRIVADOS DE CONVERSIÓN =====

    /**
     * Resuelve identifier (rawgId o slug) → Game entity
     * Si no existe, lo obtiene de RAWG y lo guarda
     */
    private Mono<Game> resolveGameFromIdentifier(String identifier) {
        boolean isRawgId = identifier.matches("\\d+");

        if (isRawgId) {
            Integer rawgId = Integer.parseInt(identifier);

            // Buscar en BD primero
            Optional<Game> existing = gameRepository.findByRawgId(rawgId);
            if (existing.isPresent()) {
                logger.debug("Game found in database: rawgId={}", rawgId);
                return Mono.just(existing.get());
            }

            // Si no existe, obtener de RAWG
            logger.debug("Game not in database, fetching from RAWG: rawgId={}", rawgId);
            return rawgApiService.getGameDetails(rawgId)
                    .map(this::saveGameFromRawgDetails);

        } else {
            // Búsqueda por slug
            // TODO: Implementar búsqueda por slug cuando tengamos el campo
            // Por ahora, error
            logger.warn("Slug search not implemented yet: {}", identifier);
            return Mono.error(new RuntimeException("Slug search not implemented: " + identifier));
        }
    }

    /**
     * Convierte RawgApiDtos.GameSummary → GameDtos.GameSearchItem
     */
    private GameDtos.GameSearchItem convertRawgToSearchItem(RawgApiDtos.GameSummary rawgGame) {
        return new GameDtos.GameSearchItem(
                rawgGame.getId(),
                null, // slug - no lo tenemos hasta guardarlo en BD
                rawgGame.getName(),
                rawgGame.getBackgroundImage(),
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
    private GameDtos.GamePageDto buildGamePageDto(Game game) {
        GameDtos.GamePageDto dto = new GameDtos.GamePageDto();

        // Datos básicos
        dto.setRawgId(game.getRawgId());
        dto.setSlug(generateSlugFromName(game.getName())); // TODO: usar slug real de BD
        dto.setName(game.getName());
        dto.setDescription(game.getDescription());
        dto.setDescriptionRaw(game.getDescriptionRaw());
        dto.setBackgroundImage(game.getBackgroundImage());
        dto.setCoverImage(game.getBackgroundImage()); // Usar backgroundImage como coverImage
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
    private GameDtos.MyGameStatus convertToMyGameStatus(UserGameDetails ugd) {
        return new GameDtos.MyGameStatus(
                ugd.getStatus() != null ? ugd.getStatus().name() : null,
                ugd.getRating(),
                ugd.getReviewText(),
                ugd.getReviewUpdatedAt()
        );
    }

    /**
     * Convierte UserGameDetails → OtherUserReview
     */
    private GameDtos.OtherUserReview convertToOtherUserReview(UserGameDetails ugd) {
        return new GameDtos.OtherUserReview(
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
    private GameDtos.MyReviewDto convertToMyReviewDto(UserGameDetails ugd) {
        Game game = ugd.getGame();
        return new GameDtos.MyReviewDto(
                game.getRawgId(),
                generateSlugFromName(game.getName()), // TODO: usar slug real
                game.getName(),
                game.getBackgroundImage(), // coverImage
                ugd.getRating(),
                ugd.getReviewText(),
                ugd.getStatus() != null ? ugd.getStatus().name() : null,
                ugd.getReviewCreatedAt(),
                ugd.getReviewUpdatedAt()
        );
    }

    /**
     * Guarda Game desde RawgApiDtos.GameDetails
     */
    private Game saveGameFromRawgDetails(RawgApiDtos.GameDetails rawgGame) {
        Game game = new Game();
        game.setRawgId(rawgGame.getId());
        game.setName(rawgGame.getName());
        game.setDescription(rawgGame.getDescription());
        game.setDescriptionRaw(rawgGame.getDescriptionRaw());
        game.setBackgroundImage(rawgGame.getBackgroundImage());
        game.setBackgroundImageAdditional(rawgGame.getBackgroundImageAdditional());
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
        logger.info("Game saved from RAWG: '{}' (ID: {})", saved.getName(), saved.getId());
        return saved;
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
        // TODO: Implementar truncamiento inteligente
        return text != null && text.length() > 150 ? text.substring(0, 150) + "..." : text;
    }

    private String truncateReviewText(String text, int maxLength) {
        return text != null && text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private String generateSlugFromName(String name) {
        // TODO: Implementar generación de slug real
        return name != null ? name.toLowerCase().replaceAll("[^a-z0-9]", "-") : null;
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

    // Record para request (simplificado)
    public record UpdateMyGameRequest(String status, Integer rating, String reviewText) {}
}