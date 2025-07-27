package com.gamelyx.controller;

import com.gamelyx.dto.GameResponseDtos;
import com.gamelyx.dto.GameRequestDtos.*;
import com.gamelyx.entity.User;
import com.gamelyx.service.GameService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller REST para la funcionalidad de juegos
 *
 * FILOSOFÍA: Endpoints centrados en páginas/funcionalidades del frontend
 * - El frontend NO sabe de RAWG vs BD (transparencia total)
 * - Los Endpoints principales traen todo lo necesario para una página completa
 * - Mínimo número de llamadas desde el frontend
 */
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:4200")
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    // ===== FUNCIONALIDAD 1: BUSCADOR HOME =====

    /**
     * 🎯 OBJETIVO: Buscador del home que lleva a página de resultados
     *
     * 📋 LÓGICA:
     * - Usuario escribe en search box del home y pulsa Enter
     * - Frontend navega a /search-results y llama este endpoint
     * - Siempre usa RAWG API (datos frescos, no cache)
     * - Devuelve lista de juegos con datos mínimos para mostrar en lista
     *
     * 📊 RESPUESTA: Lista de juegos con imagen, nombre, rating, plataformas
     * 📤 FRONTEND: Muestra lista vertical de juegos para elegir
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<GameResponseDtos.GameSearchResultsDto>> searchGames(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        logger.info("🔍 SEARCH: query='{}', page={}, size={}", query, page, size);

        return gameService.searchGamesForResults(query, page, size)
                .map(response -> {
                    logger.debug("Search completed: {} games found", response.getTotalResults());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    logger.error("Search failed for query '{}': {}", query, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().<GameResponseDtos.GameSearchResultsDto>build());
                });
    }

    // ===== FUNCIONALIDAD 2: PÁGINA COMPLETA DEL JUEGO =====

    /**
     * 🎯 OBJETIVO: Cargar página completa de un juego individual
     *
     * 📋 LÓGICA:
     * - Usuario hace click en juego desde search results
     * - Frontend navega a /game/{identifier} y llama este endpoint
     * - Identificador puede ser: rawgId (22511) o slug (minecraft)
     * - Primera vez: obtener de RAWG + guardar en BD + generar slug
     * - Siguientes veces: usar datos de BD (cache inteligente)
     * - SIEMPRE incluir: mi estado personal + reviews de otros usuarios
     *
     * 📊 RESPUESTA: TODO lo necesario para la página del juego:
     *   - Datos del juego (imagen, descripción, screenshots, plataformas)
     *   - Mi estado personal (rating, review, estado biblioteca)
     *   - Reviews recientes de otros usuarios (2-3 últimas)
     *
     * 📤 FRONTEND: Una sola llamada para cargar página completa
     *
     * 🔗 EJEMPLOS DE URLs:
     *   /game/minecraft                    → Búsqueda por slug
     *   /game/22511                       → Búsqueda por rawgId
     *   /game/grand-theft-auto-v          → Slug amigable
     */
    @GetMapping("/game/{identifier}")
    public Mono<ResponseEntity<GameResponseDtos.GamePageDto>> getGamePage(
            @PathVariable String identifier,
            @AuthenticationPrincipal User currentUser) {

        logger.info("🎮 GAME PAGE: identifier='{}', user={}", identifier,
                currentUser != null ? currentUser.getUsername() : "anonymous");

        if (currentUser != null) {
            // Usuario autenticado → incluir mi estado personal
            return gameService.getGamePageWithUserData(identifier, currentUser)
                    .map(gamePageDto -> {
                        logger.debug("Game page loaded with user data: '{}'", gamePageDto.getName());
                        return ResponseEntity.ok(gamePageDto);
                    })
                    .onErrorResume(error -> {
                        logger.error("Failed to load game page '{}' for user {}: {}",
                                identifier, currentUser.getUsername(), error.getMessage());
                        return Mono.just(ResponseEntity.notFound().<GameResponseDtos.GamePageDto>build());
                    });
        } else {
            // Usuario no autenticado → solo datos públicos
            return gameService.getGamePagePublic(identifier)
                    .map(gamePageDto -> {
                        logger.debug("Game page loaded (public): '{}'", gamePageDto.getName());
                        return ResponseEntity.ok(gamePageDto);
                    })
                    .onErrorResume(error -> {
                        logger.error("Failed to load game page '{}' (public): {}", identifier, error.getMessage());
                        return Mono.just(ResponseEntity.notFound().<GameResponseDtos.GamePageDto>build());
                    });
        }
    }

    // ===== FUNCIONALIDAD 3: GESTIÓN DE MI REVIEW/ESTADO =====

    /**
     * 🎯 OBJETIVO: Actualizar mi estado/rating/review de un juego
     *
     * 📋 LÓGICA:
     * - Usuario cambia estado (wishlist/playing/completed)
     * - Usuario da rating (1-10)
     * - Usuario escribe/edita review
     * - TODO puede venir en una sola petición o por separado
     * - Actualizar BD + recalcular community rating
     *
     * 📊 REQUEST: Estado, rating, texto review (todos opcionales)
     * 📊 RESPUESTA: Estado actualizado del usuario con el juego + community rating recalculado
     *
     * 📤 FRONTEND: Botones en página del juego para cambiar estado/rating
     *
     * 💭 ENDPOINT UNIFICADO: Más simple, menos llamadas, campos opcionales
     *
     * 🔗 EJEMPLOS:
     *   PUT /game/minecraft/my-review     → Por slug
     *   PUT /game/22511/my-review        → Por rawgId
     */
    @PutMapping("/game/{identifier}/my-review")
    public Mono<ResponseEntity<GameResponseDtos.UpdatedGameStatusDto>> updateMyGameReview(
            @PathVariable String identifier,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateMyGameRequest request) {

        logger.info("💭 UPDATE REVIEW: identifier='{}', user={}, status={}, rating={}",
                identifier, currentUser.getUsername(), request.status(), request.rating());

        return gameService.updateMyGameReview(identifier, currentUser, request)
                .map(updatedStatus -> {
                    logger.info("Review updated successfully: '{}' for user '{}' → community rating: {}",
                            identifier, currentUser.getUsername(), updatedStatus.communityRating());
                    return ResponseEntity.ok(updatedStatus);
                })
                .onErrorResume(error -> {
                    logger.error("Failed to update review for '{}' by user {}: {}",
                            identifier, currentUser.getUsername(), error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().<GameResponseDtos.UpdatedGameStatusDto>build());
                });
    }

    // ===== FUNCIONALIDAD 4: MIS REVIEWS (HOME + PÁGINA COMPLETA) =====

    /**
     * 🎯 OBJETIVO: Mostrar mis reviews (home + página completa)
     *
     * 📋 LÓGICA:
     * - CASO 1 (Home): Frontend llama sin parámetros → 3 reviews por defecto
     * - CASO 2 (Ver todas): Frontend llama con paginación → página completa
     * - Devolver mis reviews ordenadas por fecha (más recientes primero)
     * - Incluir datos básicos del juego para mostrar con la review
     *
     * 📊 RESPUESTA: Lista de mis reviews con paginación (flexible)
     * 📤 FRONTEND:
     *   - Home: "Tus reviews recientes" (3 reviews)
     *   - Página: "Todas mis reviews" (paginado)
     *
     * 🔗 EJEMPLOS DE USO:
     *   GET /my-reviews                    → Home (3 por defecto)
     *   GET /my-reviews?limit=3            → Home explícito
     *   GET /my-reviews?page=0&size=20     → Página completa con paginación
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<GameResponseDtos.MyReviewsResponseDto> getMyReviews(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        // Determinar si es para home o página completa
        boolean isForHome = (page == null && size == null);

        try {
            if (isForHome) {
                // CASO 1: Para home (sin paginación)
                int finalLimit = (limit != null) ? limit : 3;  // Default 3 para home
                logger.info("📝 MY REVIEWS (HOME): user={}, limit={}",
                        currentUser.getUsername(), finalLimit);

                var reviews = gameService.getMyRecentReviews(currentUser, finalLimit);
                var response = GameResponseDtos.MyReviewsResponseDto.forHome(reviews);

                logger.debug("Home reviews retrieved: {} reviews", reviews.size());
                return ResponseEntity.ok(response);

            } else {
                // CASO 2: Para página completa (con paginación)
                int finalPage = (page != null) ? page : 0;
                int finalSize = (size != null) ? size : 20;
                logger.info("📝 MY REVIEWS (FULL): user={}, page={}, size={}",
                        currentUser.getUsername(), finalPage, finalSize);

                var response = gameService.getMyReviewsPaginated(currentUser, finalPage, finalSize);

                logger.debug("Paginated reviews retrieved: {} total reviews", response.getTotalReviews());
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            logger.error("Failed to get reviews for user {}: {}",
                    currentUser.getUsername(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ===== HEALTH CHECK =====

    /**
     * Health check simplificado
     */
    @GetMapping("/health")
    public ResponseEntity<java.util.Map<String, Object>> health() {
        return ResponseEntity.ok(java.util.Map.of(
                "status", "UP",
                "controller", "GameController (FINAL)",
                "timestamp", System.currentTimeMillis(),
                "endpoints", java.util.List.of(
                        "GET /search - Buscador home",
                        "GET /game/{identifier} - Página completa del juego",
                        "PUT /game/{identifier}/my-review - Mi review/estado",
                        "GET /my-reviews - Mis reviews (home + paginación)"
                ),
                "philosophy", "4 endpoints principales, funcionalidad completa"
        ));
    }
}