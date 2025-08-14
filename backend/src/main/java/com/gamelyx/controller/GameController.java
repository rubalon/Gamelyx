package com.gamelyx.controller;

import com.gamelyx.dto.GameResponseDtos;
import com.gamelyx.dto.GameRequestDtos.*;
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
 * 🔧 REFACTORIZADO: Ahora usa @AuthenticationPrincipal String username
 * - JWT stateless: solo username en el filtro de autenticación
 * - GameService: responsable de buscar User entity si lo necesita
 * - Separación clara de responsabilidades
 */
@RestController
@RequestMapping("/api/games")
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
     * SIN CAMBIOS: No requiere autenticación
     */
    @GetMapping("/search")
    public ResponseEntity<GameResponseDtos.GameSearchResultsDto> searchGames(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        logger.info("🔍 SEARCH: query='{}', page={}, size={}", query, page, size);

        try {
            GameResponseDtos.GameSearchResultsDto response = gameService.searchGamesForResults(query, page, size)
                    .block();

            logger.debug("Search completed: {} games found", response.getTotalResults());
            return ResponseEntity.ok(response);

        } catch (Exception error) {
            logger.error("Search failed for query '{}': {}", query, error.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ===== FUNCIONALIDAD 2: PÁGINA COMPLETA DEL JUEGO =====

    /**
     * 🎯 OBJETIVO: Cargar página completa de un juego individual
     *
     * 🔧 REFACTORIZADO:
     * - @AuthenticationPrincipal String username (en lugar de User)
     * - GameService recibe username y busca User entity si lo necesita
     */
    @GetMapping("/game/{identifier}")
    public ResponseEntity<GameResponseDtos.GamePageDto> getGamePage(
            @PathVariable String identifier,
            @AuthenticationPrincipal String username) {

        logger.info("🎮 GAME PAGE: identifier='{}', user={}", identifier,
                username != null ? username : "anonymous");

        try {
            GameResponseDtos.GamePageDto gamePageDto;

            if (username != null) {
                // Usuario autenticado → incluir mi estado personal
                // GameService busca User entity por username si lo necesita
                gamePageDto = gameService.getGamePageWithUserData(identifier, username)
                        .block();

                logger.debug("Game page loaded with user data: '{}'", gamePageDto.getName());
            } else {
                // Usuario no autenticado → solo datos públicos
                gamePageDto = gameService.getGamePagePublic(identifier)
                        .block();

                logger.debug("Game page loaded (public): '{}'", gamePageDto.getName());
            }

            return ResponseEntity.ok(gamePageDto);

        } catch (Exception error) {
            logger.error("Failed to load game page '{}' for user {}: {}",
                    identifier,
                    username != null ? username : "anonymous",
                    error.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ===== FUNCIONALIDAD 3: GESTIÓN DE MI REVIEW/ESTADO =====

    /**
     * 🎯 OBJETIVO: Actualizar mi estado/rating/review de un juego
     *
     * 🔧 REFACTORIZADO:
     * - @AuthenticationPrincipal String username (en lugar de User)
     * - GameService recibe username y busca User entity internamente
     */
    @PutMapping("/game/{identifier}/my-game-details")
    public ResponseEntity<GameResponseDtos.UpdatedGameStatusDto> updateMyGameReview(
            @PathVariable String identifier,
            @AuthenticationPrincipal String username,
            @Valid @RequestBody UpdateMyGameRequest request) {

        logger.info("💭 UPDATE REVIEW: identifier='{}', user={}, status={}, rating={}",
                identifier, username, request.status(), request.rating());

        try {
            // GameService busca User entity por username si lo necesita
            GameResponseDtos.UpdatedGameStatusDto updatedStatus = gameService.updateMyGameReview(identifier, username, request)
                    .block();

            logger.info("Review updated successfully: '{}' for user '{}' → community rating: {}",
                    identifier, username, updatedStatus.communityRating());

            return ResponseEntity.ok(updatedStatus);

        } catch (Exception error) {
            logger.error("Failed to update review for '{}' by user {}: {}",
                    identifier, username, error.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== FUNCIONALIDAD 4: MIS REVIEWS (HOME + PÁGINA COMPLETA) =====

    /**
     * 🎯 OBJETIVO: Mostrar mis reviews (home + página completa)
     *
     * 🔧 REFACTORIZADO:
     * - @AuthenticationPrincipal String username (en lugar de User)
     * - GameService busca User entity por username para consultas
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<GameResponseDtos.MyReviewsResponseDto> getMyReviews(
            @AuthenticationPrincipal String username,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        // Determinar si es para home o página completa
        boolean isForHome = (page == null && size == null);

        try {
            if (isForHome) {
                // CASO 1: Para home (sin paginación)
                int finalLimit = (limit != null) ? limit : 3;
                logger.info("📝 MY REVIEWS (HOME): user={}, limit={}",
                        username, finalLimit);

                // GameService busca User entity por username
                var reviews = gameService.getMyRecentReviews(username, finalLimit);
                var response = GameResponseDtos.MyReviewsResponseDto.forHome(reviews);

                logger.debug("Home reviews retrieved: {} reviews", reviews.size());
                return ResponseEntity.ok(response);

            } else {
                // CASO 2: Para página completa (con paginación)
                int finalPage = (page != null) ? page : 0;
                int finalSize = (size != null) ? size : 20;
                logger.info("📝 MY REVIEWS (FULL): user={}, page={}, size={}",
                        username, finalPage, finalSize);

                // GameService busca User entity por username
                var response = gameService.getMyReviewsPaginated(username, finalPage, finalSize);

                logger.debug("Paginated reviews retrieved: {} total reviews", response.getTotalReviews());
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            logger.error("Failed to get reviews for user {}: {}",
                    username, e.getMessage());
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
                "controller", "GameController (REFACTORED - JWT Stateless)",
                "timestamp", System.currentTimeMillis(),
                "authentication", "Uses @AuthenticationPrincipal String username",
                "architecture", "GameService handles User entity lookup when needed",
                "endpoints", java.util.List.of(
                        "GET /search - Buscador home",
                        "GET /game/{identifier} - Página completa del juego",
                        "PUT /game/{identifier}/my-review - Mi review/estado",
                        "GET /my-reviews - Mis reviews (home + paginación)"
                )
        ));
    }
}