package com.gamelyx.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración centralizada para las APIs de juegos (RAWG y Steam)
 *
 * Esta clase mapea automáticamente las propiedades del application.properties
 * y proporciona validación de configuración al arranque de la aplicación.
 */
@Configuration
@ConfigurationProperties(prefix = "game")
@Validated
public class GameApiConfig {

    private final RawgApi rawg = new RawgApi();
    private final SteamApi steam = new SteamApi();
    private final Search search = new Search();

    // Getters principales
    public RawgApi getRawg() { return rawg; }
    public SteamApi getSteam() { return steam; }
    public Search getSearch() { return search; }

    /**
     * Configuración para RAWG API (servicio principal)
     * Valores obtenidos desde application.properties
     */
    public static class RawgApi {
        @NotBlank(message = "RAWG API base URL es requerida")
        private String baseurl;

        @NotBlank(message = "RAWG API key es requerida")
        private String key;

        @Min(value = 1000, message = "Timeout debe ser al menos 1000ms")
        private int timeout;

        private final Retry retry = new Retry();

        // Getters y Setters
        public String getBaseurl() { return baseurl; }
        public void setBaseurl(String baseurl) { this.baseurl = baseurl; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        public Retry getRetry() { return retry; }
    }

    /**
     * Configuración para Steam API (servicio de respaldo)
     * Valores obtenidos desde application.properties
     */
    public static class SteamApi {
        @NotBlank(message = "Steam API base URL es requerida")
        private String baseurl;

        @NotBlank(message = "Steam API key es requerida")
        private String key;

        @Min(value = 1000, message = "Timeout debe ser al menos 1000ms")
        private int timeout;

        private final Retry retry = new Retry();

        // Getters y Setters
        public String getBaseurl() { return baseurl; }
        public void setBaseurl(String baseurl) { this.baseurl = baseurl; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        public Retry getRetry() { return retry; }
    }

    /**
     * Configuración para búsquedas de juegos
     */
    public static class Search {
        @Min(value = 1, message = "Default page size debe ser al menos 1")
        private int defaultPageSize;

        @Min(value = 1, message = "Max page size debe ser al menos 1")
        private int maxPageSize;

        private final Cache cache = new Cache();

        // Getters y Setters
        public int getDefaultPageSize() { return defaultPageSize; }
        public void setDefaultPageSize(int defaultPageSize) { this.defaultPageSize = defaultPageSize; }

        public int getMaxPageSize() { return maxPageSize; }
        public void setMaxPageSize(int maxPageSize) { this.maxPageSize = maxPageSize; }

        public Cache getCache() { return cache; }
    }

    /**
     * Configuración de reintentos para APIs externas
     */
    public static class Retry {
        @Min(value = 1, message = "Max attempts debe ser al menos 1")
        private int maxAttempts;

        @Min(value = 100, message = "Delay debe ser al menos 100ms")
        private long delay;

        // Getters y Setters
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

        public long getDelay() { return delay; }
        public void setDelay(long delay) { this.delay = delay; }
    }

    /**
     * Configuración de caché para búsquedas
     */
    public static class Cache {
        @Min(value = 60, message = "TTL debe ser al menos 60 segundos")
        private long ttl;

        // Getters y Setters
        public long getTtl() { return ttl; }
        public void setTtl(long ttl) { this.ttl = ttl; }
    }

    /**
     * Método de utilidad para logging de configuración (sin exponer API keys)
     */
    @Override
    public String toString() {
        return String.format(
                "GameApiConfig{rawg={url='%s', timeout=%d}, steam={url='%s', timeout=%d}, search={pageSize=%d, maxPageSize=%d}}",
                rawg.baseurl, rawg.timeout,
                steam.baseurl, steam.timeout,
                search.defaultPageSize, search.maxPageSize
        );
    }
}