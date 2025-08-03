package com.gamelyx.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs diseñados específicamente para las funcionalidades del frontend
 *
 * FILOSOFÍA:
 * - Records para DTOs simples (inmutables, limpios)
 * - Clases para DTOs complejos con factory methods
 */
public class GameResponseDtos {

    // ===== 1. RECORDS SIMPLES =====

    /**
     * Item individual en lista de búsqueda - RECORD
     * Solo datos esenciales para mostrar en lista
     */
    public record GameSearchItem(
            Integer rawgId,           // Para construir URL /game/{rawgId}
            String slug,              // Para URL amigable /game/{slug} (si ya existe)
            String name,              // "Minecraft"
            String BackgroundImageAlt,        // Carátula del juego para mostrar en lista
            String description,       // Descripción corta/truncada para preview
            Double rating,            // Rating RAWG (4.2)
            String released,          // "2011-11-18"
            List<String> platforms,   // ["PC", "PlayStation 4"] - opcional para UI
            List<String> genres       // ["Adventure", "Survival"] - opcional para UI
    ) {}

    /**
     * Para: PUT /game/{identifier}/my-review → Response - RECORD
     * Uso: Confirmar que mi acción se procesó + mostrar community rating actualizado
     */
    public record UpdatedGameStatusDto(
            // Mi estado personal actualizado
            String status,            // "COMPLETED", "PLAYING", etc.
            Integer rating,           // Mi rating 1-10
            String reviewText,        // Mi review text
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime updatedAt,  // Cuándo hice el cambio

            // Community rating actualizado (lo importante)
            Double communityRating,   // Rating actualizado después de mi cambio
            Integer totalReviews      // Cuántas reviews tiene ahora el juego
    ) {}

    /**
     * Review individual mía - RECORD
     * Usado dentro de MyReviewsResponseDto
     */
    public record MyReviewDto(
            // Datos del juego (para mostrar con la review)
            Integer gameRawgId,       // Para construir link
            String gameSlug,          // Para URL amigable
            String gameName,          // "Minecraft"
            String BackgroundImageAlt,         // Imagen del juego

            // Mi review
            Integer rating,           // Mi rating 1-10
            String reviewText,        // Mi texto review
            String status,            // Mi estado: "COMPLETED", "PLAYING"

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime reviewCreatedAt,   // Cuándo escribí la review

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime reviewUpdatedAt   // Última actualización

    ) {}

    /**
     * Mi estado personal con el juego - RECORD
     * Parte de GamePageDto
     */
    public record MyGameStatus(
            String status,            // "WISHLIST", "PLAYING", "COMPLETED", "ARCHIVED"
            Integer rating,           // Mi rating 1-10
            String reviewText,        // Mi review

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime reviewUpdatedAt // Última vez que actualicé mi review
    ) {}

    /**
     * Review de otro usuario - RECORD
     * Parte de GamePageDto
     */
    public record OtherUserReview(
            String username,          // Usuario que escribió la review
            Integer rating,           // Su rating 1-10
            String reviewText,        // Su review (puede ser excerpt si es muy larga)
            String status,            // Su estado con el juego

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime reviewCreatedAt

    ) {}

    // ===== 2. CLASES CON FACTORY METHODS =====

    /**
     * Para: GET /search → Response - CLASE
     * Uso: Lista de resultados de búsqueda en página de resultados
     */
    public static class GameSearchResultsDto {
        private List<GameSearchItem> games;
        private int currentPage;
        private int totalPages;
        private long totalResults;
        private boolean hasMore;
        private String searchQuery;       // Para mostrar "Resultados para: minecraft"

        // Constructores
        public GameSearchResultsDto() {}

        public GameSearchResultsDto(List<GameSearchItem> games, int currentPage,
                                    int totalPages, long totalResults, boolean hasMore, String searchQuery) {
            this.games = games;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.totalResults = totalResults;
            this.hasMore = hasMore;
            this.searchQuery = searchQuery;
        }

        // Getters y Setters
        public List<GameSearchItem> getGames() { return games; }
        public void setGames(List<GameSearchItem> games) { this.games = games; }

        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public long getTotalResults() { return totalResults; }
        public void setTotalResults(long totalResults) { this.totalResults = totalResults; }

        public boolean isHasMore() { return hasMore; }
        public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }

        public String getSearchQuery() { return searchQuery; }
        public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    }

    /**
     * Para: GET /my-reviews → Response - CLASE
     * Uso: Flexible - Home (3 reviews) o página completa (paginado)
     * MANTENIDA COMO CLASE por los factory methods complejos
     */
    public static class MyReviewsResponseDto {
        private List<MyReviewDto> reviews;

        // Campos de paginación (null para home, presentes para página completa)
        private Integer currentPage;      // null = home, 0+ = página completa
        private Integer totalPages;
        private Long totalReviews;
        private Boolean hasMore;

        // Constructores
        public MyReviewsResponseDto() {}

        // Factory methods para claridad - RAZÓN de mantener como clase
        public static MyReviewsResponseDto forHome(List<MyReviewDto> reviews) {
            MyReviewsResponseDto dto = new MyReviewsResponseDto();
            dto.reviews = reviews;
            // Campos de paginación quedan null (indica que es para home)
            return dto;
        }

        public static MyReviewsResponseDto forPage(List<MyReviewDto> reviews,
                                                   int currentPage, int totalPages,
                                                   long totalReviews, boolean hasMore) {
            MyReviewsResponseDto dto = new MyReviewsResponseDto();
            dto.reviews = reviews;
            dto.currentPage = currentPage;
            dto.totalPages = totalPages;
            dto.totalReviews = totalReviews;
            dto.hasMore = hasMore;
            return dto;
        }

        // Getters y Setters
        public List<MyReviewDto> getReviews() { return reviews; }
        public void setReviews(List<MyReviewDto> reviews) { this.reviews = reviews; }

        public Integer getCurrentPage() { return currentPage; }
        public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }

        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

        public Long getTotalReviews() { return totalReviews; }
        public void setTotalReviews(Long totalReviews) { this.totalReviews = totalReviews; }

        public Boolean getHasMore() { return hasMore; }
        public void setHasMore(Boolean hasMore) { this.hasMore = hasMore; }

        // Método de utilidad para frontend
        public boolean isForHome() { return currentPage == null; }
    }

    /**
     * Para: GET /game/{identifier} → Response - CLASE
     * Uso: Página completa del juego con TODO lo necesario
     * MANTENIDA COMO CLASE por su complejidad y posibles extensiones futuras
     */
    public static class GamePageDto {
        // ===== DATOS DEL JUEGO =====
        private Integer rawgId;
        private String slug;              // Para URLs amigables
        private String name;
        private String description;       // HTML description
        private String descriptionRaw;    // Plain text para excerpts
        private String backgroundImage;   // Imagen principal/hero
        private String BackgroundImageAlt; // Imagen secundaria si existe
        private List<String> screenshots; // URLs de capturas

        // Metadata del juego
        private Double rating;            // Rating RAWG
        private Double communityRating;   // Nuestro community rating
        private Integer totalCommunityReviews; // Cuántas reviews en nuestra plataforma
        private String released;          // "2011-11-18"
        private String website;           // Sitio oficial
        private Integer metacriticScore;  // Puntuación Metacritic
        private Integer averagePlaytime;  // Horas promedio de juego

        // Categorización
        private List<String> platforms;   // ["PC", "PlayStation 4"]
        private List<String> genres;      // ["Adventure", "Survival"]
        private List<String> developers;  // ["Mojang Studios"]
        private List<String> publishers;  // ["Microsoft Studios"]
        private List<String> tags;        // ["Open World", "Crafting"]

        // ===== MI ESTADO PERSONAL (si estoy autenticado) =====
        private MyGameStatus myStatus;    // null si no estoy autenticado

        // ===== REVIEWS DE OTROS USUARIOS =====
        private List<OtherUserReview> recentReviews; // 2-3 reviews recientes de otros

        // Metadata
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastUpdated; // Cuándo se actualizó la info del juego

        // Constructores
        public GamePageDto() {}

        // Getters y Setters principales (agregar resto según necesidad)
        public Integer getRawgId() { return rawgId; }
        public void setRawgId(Integer rawgId) { this.rawgId = rawgId; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDescriptionRaw() { return descriptionRaw; }
        public void setDescriptionRaw(String descriptionRaw) { this.descriptionRaw = descriptionRaw; }

        public String getBackgroundImage() { return backgroundImage; }
        public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }

        public String getBackgroundImageAlt() { return BackgroundImageAlt; }
        public void setBackgroundImageAlt(String BackgroundImageAlt) { this.BackgroundImageAlt = BackgroundImageAlt; }

        public List<String> getScreenshots() { return screenshots; }
        public void setScreenshots(List<String> screenshots) { this.screenshots = screenshots; }

        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }

        public Double getCommunityRating() { return communityRating; }
        public void setCommunityRating(Double communityRating) { this.communityRating = communityRating; }

        public Integer getTotalCommunityReviews() { return totalCommunityReviews; }
        public void setTotalCommunityReviews(Integer totalCommunityReviews) { this.totalCommunityReviews = totalCommunityReviews; }

        public String getReleased() { return released; }
        public void setReleased(String released) { this.released = released; }

        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }

        public Integer getMetacriticScore() { return metacriticScore; }
        public void setMetacriticScore(Integer metacriticScore) { this.metacriticScore = metacriticScore; }

        public Integer getAveragePlaytime() { return averagePlaytime; }
        public void setAveragePlaytime(Integer averagePlaytime) { this.averagePlaytime = averagePlaytime; }

        public List<String> getPlatforms() { return platforms; }
        public void setPlatforms(List<String> platforms) { this.platforms = platforms; }

        public List<String> getGenres() { return genres; }
        public void setGenres(List<String> genres) { this.genres = genres; }

        public List<String> getDevelopers() { return developers; }
        public void setDevelopers(List<String> developers) { this.developers = developers; }

        public List<String> getPublishers() { return publishers; }
        public void setPublishers(List<String> publishers) { this.publishers = publishers; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public MyGameStatus getMyStatus() { return myStatus; }
        public void setMyStatus(MyGameStatus myStatus) { this.myStatus = myStatus; }

        public List<OtherUserReview> getRecentReviews() { return recentReviews; }
        public void setRecentReviews(List<OtherUserReview> recentReviews) { this.recentReviews = recentReviews; }

        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}