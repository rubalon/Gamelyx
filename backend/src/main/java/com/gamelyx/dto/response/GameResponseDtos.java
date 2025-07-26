package com.gamelyx.dto.response;

import com.gamelyx.entity.UserGameDetails.GameStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs para las respuestas de las APIs de juegos
 *
 * Estos DTOs definen exactamente qué información enviamos al frontend,
 * separando la representación interna (entidades) de la externa (API).
 */
public class GameResponseDtos {

    /**
     * Información básica de un juego para listados y búsquedas
     */
    public static class GameSummary {
        private UUID id;
        private Integer rawgId;
        private String name;
        private String backgroundImage;
        private Double rating;
        private String released;
        private List<String> platforms;
        private List<String> genres;

        // Rating de nuestra comunidad
        private Double communityRating;
        private Long communityReviewsCount;

        // Constructores
        public GameSummary() {}

        public GameSummary(UUID id, String name, Double rating) {
            this.id = id;
            this.name = name;
            this.rating = rating;
        }

        // Getters y Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public Integer getRawgId() { return rawgId; }
        public void setRawgId(Integer rawgId) { this.rawgId = rawgId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getBackgroundImage() { return backgroundImage; }
        public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }

        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }

        public String getReleased() { return released; }
        public void setReleased(String released) { this.released = released; }

        public List<String> getPlatforms() { return platforms; }
        public void setPlatforms(List<String> platforms) { this.platforms = platforms; }

        public List<String> getGenres() { return genres; }
        public void setGenres(List<String> genres) { this.genres = genres; }

        public Double getCommunityRating() { return communityRating; }
        public void setCommunityRating(Double communityRating) { this.communityRating = communityRating; }

        public Long getCommunityReviewsCount() { return communityReviewsCount; }
        public void setCommunityReviewsCount(Long communityReviewsCount) { this.communityReviewsCount = communityReviewsCount; }
    }

    /**
     * Información completa de un juego para la página de detalles
     */
    public static class GameDetails extends GameSummary {
        private String description;
        private String descriptionRaw;
        private String website;
        private Integer metacriticScore;
        private List<String> screenshots;
        private String dataSource;
        private LocalDateTime lastExternalUpdate;

        // Getters y Setters adicionales
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDescriptionRaw() { return descriptionRaw; }
        public void setDescriptionRaw(String descriptionRaw) { this.descriptionRaw = descriptionRaw; }

        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }

        public Integer getMetacriticScore() { return metacriticScore; }
        public void setMetacriticScore(Integer metacriticScore) { this.metacriticScore = metacriticScore; }

        public List<String> getScreenshots() { return screenshots; }
        public void setScreenshots(List<String> screenshots) { this.screenshots = screenshots; }

        public String getDataSource() { return dataSource; }
        public void setDataSource(String dataSource) { this.dataSource = dataSource; }

        public LocalDateTime getLastExternalUpdate() { return lastExternalUpdate; }
        public void setLastExternalUpdate(LocalDateTime lastExternalUpdate) { this.lastExternalUpdate = lastExternalUpdate; }
    }

    /**
     * Información de la relación usuario-juego
     */
    public static class UserGameDetails {
        private UUID id;
        private UUID userId;
        private UUID gameId;

        // Estado del juego
        private GameStatus status;
        private LocalDateTime addedAt;
        private LocalDateTime statusUpdatedAt;
        private LocalDateTime completedAt;

        // Review del usuario
        private Integer rating;
        private String reviewText;
        private LocalDateTime reviewCreatedAt;
        private LocalDateTime reviewUpdatedAt;

        // Metadata
        private LocalDateTime lastUpdatedAt;

        // Constructores
        public UserGameDetails() {}

        // Getters y Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public UUID getGameId() { return gameId; }
        public void setGameId(UUID gameId) { this.gameId = gameId; }

        public GameStatus getStatus() { return status; }
        public void setStatus(GameStatus status) { this.status = status; }

        public LocalDateTime getAddedAt() { return addedAt; }
        public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

        public LocalDateTime getStatusUpdatedAt() { return statusUpdatedAt; }
        public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }

        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }

        public String getReviewText() { return reviewText; }
        public void setReviewText(String reviewText) { this.reviewText = reviewText; }

        public LocalDateTime getReviewCreatedAt() { return reviewCreatedAt; }
        public void setReviewCreatedAt(LocalDateTime reviewCreatedAt) { this.reviewCreatedAt = reviewCreatedAt; }

        public LocalDateTime getReviewUpdatedAt() { return reviewUpdatedAt; }
        public void setReviewUpdatedAt(LocalDateTime reviewUpdatedAt) { this.reviewUpdatedAt = reviewUpdatedAt; }

        public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
        public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    }

    /**
     * Juego con información del usuario (para bibliotecas personales)
     */
    public static class GameWithUserDetails extends GameSummary {
        private UserGameDetails userDetails;

        public GameWithUserDetails() {}

        public UserGameDetails getUserDetails() { return userDetails; }
        public void setUserDetails(UserGameDetails userDetails) { this.userDetails = userDetails; }
    }

    /**
     * Review de un juego con información del usuario
     */
    public static class GameReview {
        private UUID id;
        private UUID gameId;
        private String gameName;
        private String gameImage;

        // Información del usuario que escribió la review
        private UUID userId;
        private String username;

        // Contenido de la review
        private Integer rating;
        private String reviewText;
        private LocalDateTime reviewCreatedAt;
        private LocalDateTime reviewUpdatedAt;

        // Estado del juego para el usuario
        private GameStatus status;
        private LocalDateTime completedAt;

        // Constructores
        public GameReview() {}

        // Getters y Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public UUID getGameId() { return gameId; }
        public void setGameId(UUID gameId) { this.gameId = gameId; }

        public String getGameName() { return gameName; }
        public void setGameName(String gameName) { this.gameName = gameName; }

        public String getGameImage() { return gameImage; }
        public void setGameImage(String gameImage) { this.gameImage = gameImage; }

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }

        public String getReviewText() { return reviewText; }
        public void setReviewText(String reviewText) { this.reviewText = reviewText; }

        public LocalDateTime getReviewCreatedAt() { return reviewCreatedAt; }
        public void setReviewCreatedAt(LocalDateTime reviewCreatedAt) { this.reviewCreatedAt = reviewCreatedAt; }

        public LocalDateTime getReviewUpdatedAt() { return reviewUpdatedAt; }
        public void setReviewUpdatedAt(LocalDateTime reviewUpdatedAt) { this.reviewUpdatedAt = reviewUpdatedAt; }

        public GameStatus getStatus() { return status; }
        public void setStatus(GameStatus status) { this.status = status; }

        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }

    /**
     * Respuesta de búsqueda paginada
     */
    public static class PagedGameResponse {
        private List<GameSummary> games;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;

        // Constructores
        public PagedGameResponse() {}

        public PagedGameResponse(List<GameSummary> games, int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {
            this.games = games;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }

        // Getters y Setters
        public List<GameSummary> getGames() { return games; }
        public void setGames(List<GameSummary> games) { this.games = games; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }

        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    }

    /**
     * Estadísticas de usuario
     */
    public static class UserGameStats {
        private UUID userId;
        private Long totalGames;
        private Long wishlistCount;
        private Long playingCount;
        private Long completedCount;
        private Long archivedCount;
        private Long totalReviews;
        private Double averageRating;

        // Constructores
        public UserGameStats() {}

        // Getters y Setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public Long getTotalGames() { return totalGames; }
        public void setTotalGames(Long totalGames) { this.totalGames = totalGames; }

        public Long getWishlistCount() { return wishlistCount; }
        public void setWishlistCount(Long wishlistCount) { this.wishlistCount = wishlistCount; }

        public Long getPlayingCount() { return playingCount; }
        public void setPlayingCount(Long playingCount) { this.playingCount = playingCount; }

        public Long getCompletedCount() { return completedCount; }
        public void setCompletedCount(Long completedCount) { this.completedCount = completedCount; }

        public Long getArchivedCount() { return archivedCount; }
        public void setArchivedCount(Long archivedCount) { this.archivedCount = archivedCount; }

        public Long getTotalReviews() { return totalReviews; }
        public void setTotalReviews(Long totalReviews) { this.totalReviews = totalReviews; }

        public Double getAverageRating() { return averageRating; }
        public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    }
}