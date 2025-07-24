package com.gamelyx.controller;

import com.gamelyx.dto.external.RawgApiDtos;
import com.gamelyx.service.external.RawgApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controller temporal para probar la integración con RAWG API
 *
 * ⚠️ ESTE CONTROLLER ES SOLO PARA TESTING
 * Una vez que verifiquemos que todo funciona, lo eliminaremos
 * y crearemos el controller de producción.
 */
@RestController
@RequestMapping("/api/test/games")
public class GameTestController {

    private static final Logger logger = LoggerFactory.getLogger(GameTestController.class);

    private final RawgApiService rawgApiService;

    public GameTestController(RawgApiService rawgApiService) {
        this.rawgApiService = rawgApiService;
    }

    /**
     * Endpoint de salud para verificar que el servicio está funcionando
     *
     * GET /api/test/games/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        logger.info("Health check requested");

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "GameTestController",
                "rawgService", rawgApiService.getServiceInfo(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Busca juegos por nombre (endpoint de prueba)
     *
     * GET /api/test/games/search?q=zelda&page=1&size=5
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<RawgApiDtos.GameSearchResponse>> searchGames(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        logger.info("Testing game search: query='{}', page={}, size={}", query, page, size);

        return rawgApiService.searchGames(query, page, size)
                .map(response -> {
                    logger.info("Search successful: found {} games", response.getCount());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    logger.error("Search failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().<RawgApiDtos.GameSearchResponse>build());
                });
    }

    /**
     * Obtiene detalles de un juego específico
     *
     * GET /api/test/games/details/3498  (ejemplo: GTA V)
     */
    @GetMapping("/details/{gameId}")
    public Mono<ResponseEntity<RawgApiDtos.GameDetails>> getGameDetails(
            @PathVariable Integer gameId) {

        logger.info("Testing game details for ID: {}", gameId);

        return rawgApiService.getGameDetails(gameId)
                .map(gameDetails -> {
                    logger.info("Game details retrieved: '{}'", gameDetails.getName());
                    return ResponseEntity.ok(gameDetails);
                })
                .onErrorResume(error -> {
                    logger.error("Failed to get game details for ID {}: {}", gameId, error.getMessage());
                    return Mono.just(ResponseEntity.notFound().<RawgApiDtos.GameDetails>build());
                });
    }

    /**
     * Obtiene juegos populares
     *
     * GET /api/test/games/popular?page=1&size=5
     */
    @GetMapping("/popular")
    public Mono<ResponseEntity<RawgApiDtos.GameSearchResponse>> getPopularGames(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        logger.info("Testing popular games: page={}, size={}", page, size);

        return rawgApiService.getPopularGames(page, size)
                .map(response -> {
                    logger.info("Popular games retrieved: {} games", response.getCount());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    logger.error("Failed to get popular games: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().<RawgApiDtos.GameSearchResponse>build());
                });
    }

    /**
     * Obtiene juegos trending
     *
     * GET /api/test/games/trending?page=1&size=5
     */
    @GetMapping("/trending")
    public Mono<ResponseEntity<RawgApiDtos.GameSearchResponse>> getTrendingGames(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        logger.info("Testing trending games: page={}, size={}", page, size);

        return rawgApiService.getTrendingGames(page, size)
                .map(response -> {
                    logger.info("Trending games retrieved: {} games", response.getCount());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    logger.error("Failed to get trending games: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().<RawgApiDtos.GameSearchResponse>build());
                });
    }

    /**
     * Obtiene screenshots de un juego
     *
     * GET /api/test/games/screenshots/3498
     */
    @GetMapping("/screenshots/{gameId}")
    public Mono<ResponseEntity<RawgApiDtos.ScreenshotsResponse>> getGameScreenshots(
            @PathVariable Integer gameId) {

        logger.info("Testing screenshots for game ID: {}", gameId);

        return rawgApiService.getGameScreenshots(gameId)
                .map(screenshots -> {
                    int count = screenshots.getResults() != null ? screenshots.getResults().size() : 0;
                    logger.info("Screenshots retrieved: {} images", count);
                    return ResponseEntity.ok(screenshots);
                })
                .onErrorResume(error -> {
                    logger.error("Failed to get screenshots for game ID {}: {}", gameId, error.getMessage());
                    return Mono.just(ResponseEntity.notFound().<RawgApiDtos.ScreenshotsResponse>build());
                });
    }

    /**
     * Endpoint para probar un juego específico conocido (GTA V)
     * Útil para pruebas rápidas sin parámetros
     *
     * GET /api/test/games/quick-test
     */
    @GetMapping("/quick-test")
    public Mono<ResponseEntity<Map<String, Object>>> quickTest() {
        logger.info("Running quick test with GTA V (ID: 3498)");

        return rawgApiService.getGameDetails(3498)
                .map(gameDetails -> {
                    Map<String, Object> testResult = Map.of(
                            "status", "SUCCESS",
                            "gameName", gameDetails.getName(),
                            "rating", gameDetails.getRating(),
                            "platforms", gameDetails.getPlatforms().stream()
                                    .map(p -> p.getPlatform().getName())
                                    .toList(),
                            "description", gameDetails.getDescriptionRaw() != null ?
                                    gameDetails.getDescriptionRaw().substring(0, Math.min(100, gameDetails.getDescriptionRaw().length())) + "..." :
                                    "No description",
                            "testTimestamp", System.currentTimeMillis()
                    );

                    logger.info("Quick test successful: {}", gameDetails.getName());
                    return ResponseEntity.ok(testResult);
                })
                .onErrorResume(error -> {
                    logger.error("Quick test failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.ok(Map.of(
                            "status", "FAILED",
                            "error", error.getMessage(),
                            "testTimestamp", System.currentTimeMillis()
                    )));
                });
    }
}