package com.gamelyx.service.external;

import com.gamelyx.config.GameApiConfig;
import com.gamelyx.dto.external.RawgApiDtos;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Servicio para interactuar con la API de RAWG
 *
 * Este servicio maneja todas las llamadas HTTP a la API de RAWG para obtener
 * información de juegos, búsquedas, detalles, screenshots, etc.
 */
@Service
public class RawgApiService {

    private static final Logger logger = LoggerFactory.getLogger(RawgApiService.class);

    private final WebClient webClient;
    private final GameApiConfig gameApiConfig;

    public RawgApiService(WebClient.Builder webClientBuilder, GameApiConfig gameApiConfig) {
        this.gameApiConfig = gameApiConfig;

        // Configuramos el WebClient específico para RAWG con timeout más generoso
        this.webClient = webClientBuilder
                .baseUrl(gameApiConfig.getRawg().getBaseUrl())
                .defaultHeader("User-Agent", "Gamelyx/1.0")
                .defaultHeader("Accept", "application/json")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 2MB buffer
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofMillis(gameApiConfig.getRawg().getTimeout()))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                ))
                .build();

        logger.info("RawgApiService initialized with base URL: {}",
                gameApiConfig.getRawg().getBaseUrl());
    }

    /**
     * Busca juegos por nombre con paginación
     *
     * @param query Término de búsqueda
     * @param page Número de página (1-based)
     * @param pageSize Tamaño de página
     * @return Mono con la respuesta de búsqueda
     */
    public Mono<RawgApiDtos.GameSearchResponse> searchGames(String query, int page, int pageSize) {
        logger.debug("Searching games: query='{}', page={}, pageSize={}", query, page, pageSize);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("key", gameApiConfig.getRawg().getKey())
                        .queryParam("search", query)
                        .queryParam("page", page)
                        .queryParam("page_size", Math.min(pageSize, 40)) // RAWG max es 40
                        .queryParam("ordering", "-rating") // Ordenar por rating descendente
                        .build())
                .retrieve()
                .bodyToMono(RawgApiDtos.GameSearchResponse.class)
                .retryWhen(createRetrySpec("searchGames"))
                .doOnSuccess(response -> logger.debug("Search successful: found {} games",
                        response != null ? response.getCount() : 0))
                .doOnError(error -> logger.error("Search failed for query '{}': {}",
                        query, error.getMessage()));
    }

    /**
     * Obtiene detalles completos de un juego por su ID
     *
     * @param gameId ID del juego en RAWG
     * @return Mono con los detalles del juego
     */
    public Mono<RawgApiDtos.GameDetails> getGameDetails(Integer gameId) {
        logger.debug("Getting game details for ID: {}", gameId);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/{id}")
                        .queryParam("key", gameApiConfig.getRawg().getKey())
                        .build(gameId))
                .retrieve()
                .bodyToMono(RawgApiDtos.GameDetails.class)
                .retryWhen(createRetrySpec("getGameDetails"))
                .doOnSuccess(game -> logger.debug("Game details retrieved: '{}'",
                        game != null ? game.getName() : "unknown"))
                .doOnError(error -> logger.error("Failed to get game details for ID {}: {}",
                        gameId, error.getMessage()));
    }

    /**
     * Obtiene detalles completos de un juego por su SLUG
     *
     * @param gameSlug Slug del juego en RAWG (ej: "minecraft", "grand-theft-auto-v")
     * @return Mono con los detalles del juego
     */
    public Mono<RawgApiDtos.GameDetails> getGameDetailsBySlug(String gameSlug) {
        logger.debug("Getting game details for slug: '{}'", gameSlug);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/{slug}")
                        .queryParam("key", gameApiConfig.getRawg().getKey())
                        .build(gameSlug))
                .retrieve()
                .bodyToMono(RawgApiDtos.GameDetails.class)
                .retryWhen(createRetrySpec("getGameDetailsBySlug"))
                .doOnSuccess(game -> logger.debug("Game details retrieved by slug '{}': '{}'",
                        gameSlug, game != null ? game.getName() : "unknown"))
                .doOnError(error -> logger.error("Failed to get game details for slug '{}': {}",
                        gameSlug, error.getMessage()));
    }

    /**
     * Obtiene screenshots de un juego
     *
     * @param gameId ID del juego en RAWG
     * @return Mono con la respuesta de screenshots
     */
    public Mono<RawgApiDtos.ScreenshotsResponse> getGameScreenshots(Integer gameId) {
        logger.debug("Getting screenshots for game ID: {}", gameId);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/{id}/screenshots")
                        .queryParam("key", gameApiConfig.getRawg().getKey())
                        .queryParam("page_size", 10) // Máximo 10 screenshots
                        .build(gameId))
                .retrieve()
                .bodyToMono(RawgApiDtos.ScreenshotsResponse.class)
                .retryWhen(createRetrySpec("getGameScreenshots"))
                .doOnSuccess(response -> logger.debug("Screenshots retrieved: {} images",
                        response != null && response.getResults() != null ?
                                response.getResults().size() : 0))
                .doOnError(error -> logger.error("Failed to get screenshots for game ID {}: {}",
                        gameId, error.getMessage()));
    }

    /**
     * Obtiene juegos populares (más valorados)
     *
     * @param page Número de página
     * @param pageSize Tamaño de página
     * @return Mono con juegos populares
     */
    public Mono<RawgApiDtos.GameSearchResponse> getPopularGames(int page, int pageSize) {
        logger.debug("Getting popular games: page={}, pageSize={}", page, pageSize);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("key", gameApiConfig.getRawg().getKey())
                        .queryParam("page", page)
                        .queryParam("page_size", Math.min(pageSize, 40))
                        .queryParam("ordering", "-rating,-rating_top") // Los mejor valorados
                        .queryParam("metacritic", "80,100") // Con buena puntuación Metacritic
                        .build())
                .retrieve()
                .bodyToMono(RawgApiDtos.GameSearchResponse.class)
                .retryWhen(createRetrySpec("getPopularGames"))
                .doOnSuccess(response -> logger.debug("Popular games retrieved: {} games",
                        response != null ? response.getCount() : 0))
                .doOnError(error -> logger.error("Failed to get popular games: {}",
                        error.getMessage()));
    }

    /**
     * Obtiene juegos trending (recientes y populares)
     *
     * @param page Número de página
     * @param pageSize Tamaño de página
     * @return Mono con juegos trending
     */
    public Mono<RawgApiDtos.GameSearchResponse> getTrendingGames(int page, int pageSize) {
        logger.debug("Getting trending games: page={}, pageSize={}", page, pageSize);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("key", gameApiConfig.getRawg().getKey())
                        .queryParam("page", page)
                        .queryParam("page_size", Math.min(pageSize, 40))
                        .queryParam("ordering", "-added") // Recientemente añadidos
                        .queryParam("dates", "2024-01-01,2025-12-31") // Juegos recientes
                        .build())
                .retrieve()
                .bodyToMono(RawgApiDtos.GameSearchResponse.class)
                .retryWhen(createRetrySpec("getTrendingGames"))
                .doOnSuccess(response -> logger.debug("Trending games retrieved: {} games",
                        response != null ? response.getCount() : 0))
                .doOnError(error -> logger.error("Failed to get trending games: {}",
                        error.getMessage()));
    }

    /**
     * Crea la especificación de reintentos para llamadas a la API
     *
     * @param operation Nombre de la operación (para logging)
     * @return Retry spec configurado
     */
    private Retry createRetrySpec(String operation) {
        return Retry.backoff(
                        gameApiConfig.getRawg().getRetry().getMaxAttempts(),
                        Duration.ofMillis(gameApiConfig.getRawg().getRetry().getDelay())
                )
                .filter(throwable -> {
                    // Solo reintentar en errores de red o 5xx
                    if (throwable instanceof WebClientResponseException wcre) {
                        return wcre.getStatusCode().is5xxServerError();
                    }
                    return true; // Reintentar otros errores de red
                })
                .doBeforeRetry(retrySignal ->
                        logger.warn("Retrying {} (attempt {}/{}): {}",
                                operation,
                                retrySignal.totalRetries() + 1,
                                gameApiConfig.getRawg().getRetry().getMaxAttempts(),
                                retrySignal.failure().getMessage())
                );
    }

    /**
     * Método de utilidad para obtener información sobre el estado del servicio
     *
     * @return Información del servicio
     */
    public String getServiceInfo() {
        return String.format("RawgApiService{baseUrl='%s', timeout=%dms, maxRetries=%d}",
                gameApiConfig.getRawg().getBaseUrl(),
                gameApiConfig.getRawg().getTimeout(),
                gameApiConfig.getRawg().getRetry().getMaxAttempts());
    }
}