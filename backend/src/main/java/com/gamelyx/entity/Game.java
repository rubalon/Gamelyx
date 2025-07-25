package com.gamelyx.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entidad Game - Representa un juego en nuestra base de datos
 *
 * Esta entidad almacena información de juegos obtenida de APIs externas (RAWG, Steam)
 * para evitar llamadas constantes y mejorar el rendimiento.
 */
@Entity
@Table(name = "games", indexes = {
        @Index(name = "idx_game_rawg_id", columnList = "rawg_id"),
        @Index(name = "idx_game_steam_app_id", columnList = "steam_app_id"),
        @Index(name = "idx_game_name", columnList = "name"),
        @Index(name = "idx_game_rating", columnList = "rating")
})
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // IDs de APIs externas
    @Column(name = "rawg_id", unique = true)
    private Integer rawgId;

    @Column(name = "steam_app_id")
    private String steamAppId;

    // Información básica del juego
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_raw", columnDefinition = "TEXT")
    private String descriptionRaw;

    @Column(name = "background_image", length = 500)
    private String backgroundImage;

    @Column(name = "background_image_additional", length = 500)
    private String backgroundImageAdditional;

    // Ratings y valoraciones
    @Column(precision = 3, scale = 2)
    private Double rating; // Rating externo (RAWG/Steam)

    @Column(name = "rating_top")
    private Integer ratingTop;

    @Column(name = "community_rating", precision = 3, scale = 2)
    private Double communityRating; // Rating promedio de nuestra comunidad

    @Column(name = "community_reviews_count")
    private Integer communityReviewsCount = 0;

    // Fechas y metadata
    private String released; // Fecha de lanzamiento como string (formato YYYY-MM-DD)

    @Column(length = 500)
    private String website;

    @Column(name = "metacritic_score")
    private Integer metacriticScore;

    @Column(name = "average_playtime")
    private Integer averagePlaytime;

    // Listas como JSON (almacenadas como strings separadas por comas)
    @Column(name = "platforms", columnDefinition = "TEXT")
    private String platforms; // "PC,PlayStation 4,Xbox One"

    @Column(columnDefinition = "TEXT")
    private String genres; // "Action,Adventure,RPG"

    @Column(columnDefinition = "TEXT")
    private String screenshots; // URLs separadas por comas

    @Column(columnDefinition = "TEXT")
    private String developers; // "Rockstar Games,Rockstar North"

    @Column(columnDefinition = "TEXT")
    private String publishers; // "Rockstar Games"

    @Column(columnDefinition = "TEXT")
    private String tags; // Tags del juego

    // Control de datos
    @Column(name = "data_source", length = 50)
    private String dataSource; // "RAWG", "STEAM", "MANUAL"

    @Column(name = "last_external_update")
    private LocalDateTime lastExternalUpdate; // Última actualización desde API externa

    @Column(name = "is_active")
    private Boolean isActive = true; // Para soft delete

    // Timestamps automáticos
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructores
    public Game() {}

    public Game(String name) {
        this.name = name;
    }

    // Getters y Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Integer getRawgId() { return rawgId; }
    public void setRawgId(Integer rawgId) { this.rawgId = rawgId; }

    public String getSteamAppId() { return steamAppId; }
    public void setSteamAppId(String steamAppId) { this.steamAppId = steamAppId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDescriptionRaw() { return descriptionRaw; }
    public void setDescriptionRaw(String descriptionRaw) { this.descriptionRaw = descriptionRaw; }

    public String getBackgroundImage() { return backgroundImage; }
    public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }

    public String getBackgroundImageAdditional() { return backgroundImageAdditional; }
    public void setBackgroundImageAdditional(String backgroundImageAdditional) { this.backgroundImageAdditional = backgroundImageAdditional; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getRatingTop() { return ratingTop; }
    public void setRatingTop(Integer ratingTop) { this.ratingTop = ratingTop; }

    public Double getCommunityRating() { return communityRating; }
    public void setCommunityRating(Double communityRating) { this.communityRating = communityRating; }

    public Integer getCommunityReviewsCount() { return communityReviewsCount; }
    public void setCommunityReviewsCount(Integer communityReviewsCount) { this.communityReviewsCount = communityReviewsCount; }

    public String getReleased() { return released; }
    public void setReleased(String released) { this.released = released; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public Integer getMetacriticScore() { return metacriticScore; }
    public void setMetacriticScore(Integer metacriticScore) { this.metacriticScore = metacriticScore; }

    public Integer getAveragePlaytime() { return averagePlaytime; }
    public void setAveragePlaytime(Integer averagePlaytime) { this.averagePlaytime = averagePlaytime; }

    public String getPlatforms() { return platforms; }
    public void setPlatforms(String platforms) { this.platforms = platforms; }

    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }

    public String getScreenshots() { return screenshots; }
    public void setScreenshots(String screenshots) { this.screenshots = screenshots; }

    public String getDevelopers() { return developers; }
    public void setDevelopers(String developers) { this.developers = developers; }

    public String getPublishers() { return publishers; }
    public void setPublishers(String publishers) { this.publishers = publishers; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public LocalDateTime getLastExternalUpdate() { return lastExternalUpdate; }
    public void setLastExternalUpdate(LocalDateTime lastExternalUpdate) { this.lastExternalUpdate = lastExternalUpdate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Métodos de utilidad
    public List<String> getPlatformsList() {
        return platforms != null ? List.of(platforms.split(",")) : List.of();
    }

    public void setPlatformsList(List<String> platformsList) {
        this.platforms = platformsList != null ? String.join(",", platformsList) : null;
    }

    public List<String> getGenresList() {
        return genres != null ? List.of(genres.split(",")) : List.of();
    }

    public void setGenresList(List<String> genresList) {
        this.genres = genresList != null ? String.join(",", genresList) : null;
    }

    public List<String> getScreenshotsList() {
        return screenshots != null ? List.of(screenshots.split(",")) : List.of();
    }

    public void setScreenshotsList(List<String> screenshotsList) {
        this.screenshots = screenshotsList != null ? String.join(",", screenshotsList) : null;
    }

    @Override
    public String toString() {
        return String.format("Game{id=%s, name='%s', rating=%.2f, platforms='%s'}",
                id, name, rating, platforms);
    }
}