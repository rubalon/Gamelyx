package com.gamelyx.controller;

import com.gamelyx.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para la funcionalidad de juegos
 *
 * FILOSOFÍA: Endpoints centrados en páginas/funcionalidades del frontend
 * - El frontend NO sabe de RAWG vs BD (transparencia total)
 * - Los Enpoints principales traen to lo necesario para una página completa
 * - Mínimo número de llamadas desde el frontend
 */
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:4200")
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    // TODO: Inyectar servicios cuando los creemos
    // private final GameService gameService;

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
    public Mono<ResponseEntity<GameSearchResultsDto>> searchGames(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        logger.info("🔍 SEARCH: query='{}', page={}, size={}", query, page, size);

        // TODO: Implementar lógica
        // return gameService.searchGamesForResults(query, page, size)
        //     .map(ResponseEntity::ok)
        //     .onErrorReturn(ResponseEntity.internalServerError().build());

        return Mono.just(ResponseEntity.ok(new GameSearchResultsDto()));
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
    public Mono<ResponseEntity<GamePageDto>> getGamePage(
            @PathVariable String identifier,
            @AuthenticationPrincipal User currentUser) {

        logger.info("🎮 GAME PAGE: identifier='{}', user={}", identifier,
                currentUser != null ? currentUser.getUsername() : "anonymous");

        // TODO: Implementar lógica
        // boolean isRawgId = identifier.matches("\\d+");
        // if (currentUser != null) {
        //     if (isRawgId) {
        //         return gameService.getGamePageWithUserData(Integer.parseInt(identifier), currentUser)
        //             .map(ResponseEntity::ok);
        //     } else {
        //         return gameService.getGamePageBySlugWithUserData(identifier, currentUser)
        //             .map(ResponseEntity::ok);
        //     }
        // } else {
        //     if (isRawgId) {
        //         return gameService.getGamePagePublic(Integer.parseInt(identifier))
        //             .map(ResponseEntity::ok);
        //     } else {
        //         return gameService.getGamePageBySlugPublic(identifier)
        //             .map(ResponseEntity::ok);
        //     }
        // }

        return Mono.just(ResponseEntity.ok(new GamePageDto()));
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
     * 📊 RESPUESTA: Estado actualizado del usuario con el juego
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
    public Mono<ResponseEntity<MyGameStatusDto>> updateMyGameReview(
            @PathVariable String identifier,
            @AuthenticationPrincipal User currentUser,
            @RequestBody UpdateMyGameRequest request) {

        logger.info("💭 UPDATE REVIEW: identifier='{}', user={}, status={}, rating={}",
                identifier, currentUser.getUsername(), request.getStatus(), request.getRating());

        // TODO: Implementar lógica
        // boolean isRawgId = identifier.matches("\\d+");
        // if (isRawgId) {
        //     return gameService.updateMyGameReview(Integer.parseInt(identifier), currentUser, request)
        //         .map(ResponseEntity::ok)
        //         .onErrorReturn(ResponseEntity.badRequest().build());
        // } else {
        //     return gameService.updateMyGameReviewBySlug(identifier, currentUser, request)
        //         .map(ResponseEntity::ok)
        //         .onErrorReturn(ResponseEntity.badRequest().build());
        // }

        return Mono.just(ResponseEntity.ok(new MyGameStatusDto()));
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
     * 📊 RESPUESTA: Lista de mis reviews con paginación
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
    public ResponseEntity<MyReviewsResponseDto> getMyReviews(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        // Determinar si es para home o página completa
        boolean isForHome = (page == null && size == null);

        if (isForHome) {
            // CASO 1: Para home (sin paginación)
            int finalLimit = (limit != null) ? limit : 3;  // Default 3 para home
            logger.info("📝 MY REVIEWS (HOME): user={}, limit={}",
                    currentUser.getUsername(), finalLimit);

            // TODO: Implementar lógica para home
            // List<MyRecentReviewDto> reviews = gameService.getMyRecentReviews(currentUser, finalLimit);
            // return ResponseEntity.ok(MyReviewsResponseDto.forHome(reviews));

        } else {
            // CASO 2: Para página completa (con paginación)
            int finalPage = (page != null) ? page : 0;
            int finalSize = (size != null) ? size : 20;
            logger.info("📝 MY REVIEWS (FULL): user={}, page={}, size={}",
                    currentUser.getUsername(), finalPage, finalSize);

            // TODO: Implementar lógica para página completa
            // MyReviewsResponseDto reviews = gameService.getMyReviewsPaginated(currentUser, finalPage, finalSize);
            // return ResponseEntity.ok(reviews);
        }

        return ResponseEntity.ok(new MyReviewsResponseDto());
    }

    // ===== HEALTH CHECK =====

    /**
     * Health check simplificado
     */
    @GetMapping("/health")
    public ResponseEntity<java.util.Map<String, Object>> health() {
        return ResponseEntity.ok(java.util.Map.of(
                "status", "UP",
                "controller", "GameController (REDESIGNED)",
                "endpoints", java.util.List.of(
                        "GET /search - Buscador home",
                        "GET /game/{rawgId} - Página completa del juego",
                        "PUT /game/{rawgId}/my-review - Mi review/estado",
                        "GET /my-recent-reviews - Home reviews",
                        "GET /my-library - Mi biblioteca (opcional)",
                        "DELETE /game/{rawgId}/my-review - Eliminar"
                )
        ));
    }

    // ===== DTOs TEMPORALES (Los definiremos después) =====

    public static class GameSearchResultsDto {
        // Lista de juegos para search results
        // Campos: rawgId, name, backgroundImage, rating, platforms
    }

    public static class GamePageDto {
        // TODO: Definir estructura completa
        // Datos del juego + mi estado + reviews de otros
    }

    public static class UpdateMyGameRequest {
        private String status;    // WISHLIST, PLAYING, COMPLETED
        private Integer rating;   // 1-10
        private String reviewText;

        // Getters/setters
        public String getStatus() { return status; }
        public Integer getRating() { return rating; }
        public String getReviewText() { return reviewText; }
    }

    public static class MyGameStatusDto {
        // Mi estado personal con un juego
        // status, rating, reviewText, addedAt, completedAt
    }

    public static class MyRecentReviewDto {
        // Mi review + datos básicos del juego
        // gameName, gameImage, rating, reviewText, reviewDate
    }

    public static class MyReviewsResponseDto {
        // Flexible: puede ser lista simple (home) o paginada (página completa)
        private List<MyRecentReviewDto> reviews;

        // Campos de paginación (solo para página completa, null para home)
        private Integer currentPage;
        private Integer totalPages;
        private Long totalReviews;
        private Boolean hasMore;

        // Constructor para home (sin paginación)
        public static MyReviewsResponseDto forHome(List<MyRecentReviewDto> reviews) {
            MyReviewsResponseDto dto = new MyReviewsResponseDto();
            dto.reviews = reviews;
            // Campos de paginación quedan null
            return dto;
        }

        // Constructor para página completa (con paginación)
        public static MyReviewsResponseDto forPage(List<MyRecentReviewDto> reviews,
                                                   int currentPage, int totalPages, long totalReviews, boolean hasMore) {
            MyReviewsResponseDto dto = new MyReviewsResponseDto();
            dto.reviews = reviews;
            dto.currentPage = currentPage;
            dto.totalPages = totalPages;
            dto.totalReviews = totalReviews;
            dto.hasMore = hasMore;
            return dto;
        }

        // Getters
        public List<MyRecentReviewDto> getReviews() { return reviews; }
        public Integer getCurrentPage() { return currentPage; }
        public Integer getTotalPages() { return totalPages; }
        public Long getTotalReviews() { return totalReviews; }
        public Boolean getHasMore() { return hasMore; }

        // Método útil para frontend
        public boolean isForHome() { return currentPage == null; }
    }
}