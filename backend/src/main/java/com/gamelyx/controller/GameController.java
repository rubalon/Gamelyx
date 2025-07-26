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
 * - Cada endpoint trae TODO lo necesario para una página completa
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
     * - Frontend navega a /game/{rawgId} y llama este endpoint
     * - Primera vez: obtener de RAWG + guardar en BD
     * - Siguientes veces: usar datos de BD (cache inteligente)
     * - SIEMPRE incluir: mi estado personal + reviews de otros usuarios
     *
     * 📊 RESPUESTA: TODO lo necesario para la página del juego:
     *   - Datos del juego (imagen, descripción, screenshots, plataformas)
     *   - Mi estado personal (rating, review, estado biblioteca)
     *   - Reviews recientes de otros usuarios (2-3 últimas)
     *
     * 📤 FRONTEND: Una sola llamada para cargar página completa
     */
    @GetMapping("/game/{rawgId}")
    public Mono<ResponseEntity<GamePageDto>> getGamePage(
            @PathVariable Integer rawgId,
            @AuthenticationPrincipal User currentUser) {

        logger.info("🎮 GAME PAGE: rawgId={}, user={}", rawgId,
                currentUser != null ? currentUser.getUsername() : "anonymous");

        // TODO: Implementar lógica
        // if (currentUser != null) {
        //     return gameService.getGamePageWithUserData(rawgId, currentUser)
        //         .map(ResponseEntity::ok);
        // } else {
        //     return gameService.getGamePagePublic(rawgId)
        //         .map(ResponseEntity::ok);
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
     * 💭 DEBATE: ¿Un endpoint unificado o separar por acción?
     *    - UNIFICADO: Más simple, menos llamadas
     *    - SEPARADO: Más granular, mejor para UI reactiva
     *
     *    MI OPINIÓN: Unificado, porque el frontend puede enviar solo
     *    los campos que cambiaron
     */
    @PutMapping("/game/{rawgId}/my-review")
    public Mono<ResponseEntity<MyGameStatusDto>> updateMyGameReview(
            @PathVariable Integer rawgId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody UpdateMyGameRequest request) {

        logger.info("💭 UPDATE REVIEW: rawgId={}, user={}, status={}, rating={}",
                rawgId, currentUser.getUsername(), request.getStatus(), request.getRating());

        // TODO: Implementar lógica
        // return gameService.updateMyGameReview(rawgId, currentUser, request)
        //     .map(ResponseEntity::ok)
        //     .onErrorReturn(ResponseEntity.badRequest().build());

        return Mono.just(ResponseEntity.ok(new MyGameStatusDto()));
    }

    // ===== FUNCIONALIDAD 4: MIS REVIEWS RECIENTES (HOME) =====

    /**
     * 🎯 OBJETIVO: Mostrar mis últimas reviews en el home
     *
     * 📋 LÓGICA:
     * - Usuario autenticado entra al home
     * - Frontend llama este endpoint para mostrar sección "Mis últimas reviews"
     * - Devolver últimas 3-5 reviews con datos básicos del juego
     *
     * 📊 RESPUESTA: Lista de mis reviews con imagen y nombre del juego
     * 📤 FRONTEND: Sección en home "Tus reviews recientes"
     */
    @GetMapping("/my-recent-reviews")
    public ResponseEntity<List<MyRecentReviewDto>> getMyRecentReviews(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {

        logger.info("📝 MY RECENT REVIEWS: user={}, limit={}",
                currentUser.getUsername(), limit);

        // TODO: Implementar lógica
        // List<MyRecentReviewDto> reviews = gameService.getMyRecentReviews(currentUser, limit);
        // return ResponseEntity.ok(reviews);

        return ResponseEntity.ok(List.of());
    }

    // ===== FUNCIONALIDADES ADICIONALES SUGERIDAS =====

    /**
     * 🎯 OBJETIVO: Obtener solo mi estado con un juego específico
     *
     * 📋 LÓGICA:
     * - Para cuando el frontend necesita solo verificar mi estado
     * - Sin datos del juego, solo mi relación con él
     * - Útil para botones reactivos sin recargar página completa
     *
     * 💭 PREGUNTA: ¿Es necesario o ya lo cubre getGamePage?
     */
    @GetMapping("/game/{rawgId}/my-status")
    public ResponseEntity<MyGameStatusDto> getMyGameStatus(
            @PathVariable Integer rawgId,
            @AuthenticationPrincipal User currentUser) {

        logger.debug("🔍 MY STATUS: rawgId={}, user={}", rawgId, currentUser.getUsername());

        // TODO: Implementar
        // Optional<MyGameStatusDto> status = gameService.getMyGameStatus(rawgId, currentUser);
        // return status.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());

        return ResponseEntity.ok(new MyGameStatusDto());
    }

    /**
     * 🎯 OBJETIVO: Mi biblioteca/colección personal
     *
     * 📋 LÓGICA:
     * - Página separada con todos mis juegos
     * - Filtros por estado (wishlist, playing, completed)
     * - Útil para gestión personal de biblioteca
     *
     * 💭 PREGUNTA: ¿Es una página importante o se puede omitir inicialmente?
     */
    @GetMapping("/my-library")
    public ResponseEntity<MyLibraryDto> getMyLibrary(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        logger.info("📚 MY LIBRARY: user={}, status={}, page={}",
                currentUser.getUsername(), status, page);

        // TODO: Implementar
        // return ResponseEntity.ok(gameService.getMyLibrary(currentUser, status, page, size));

        return ResponseEntity.ok(new MyLibraryDto());
    }

    /**
     * 🎯 OBJETIVO: Eliminar juego de mi biblioteca
     *
     * 📋 LÓGICA:
     * - Usuario decide que ya no quiere el juego en su biblioteca
     * - Eliminar relación UserGameDetails (hard delete)
     * - Recalcular community rating del juego
     */
    @DeleteMapping("/game/{rawgId}/my-review")
    public ResponseEntity<Void> removeFromMyLibrary(
            @PathVariable Integer rawgId,
            @AuthenticationPrincipal User currentUser) {

        logger.info("🗑️ REMOVE FROM LIBRARY: rawgId={}, user={}",
                rawgId, currentUser.getUsername());

        // TODO: Implementar
        // gameService.removeFromMyLibrary(rawgId, currentUser);
        // return ResponseEntity.ok().build();

        return ResponseEntity.ok().build();
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

    public static class MyLibraryDto {
        // Mi biblioteca completa con paginación
        // List<games>, pagination, stats
    }
}