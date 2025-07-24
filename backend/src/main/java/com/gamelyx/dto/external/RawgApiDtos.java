package com.gamelyx.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * DTOs para la API de RAWG
 *
 * Estas clases mapean las respuestas JSON de la API de RAWG.
 * Usamos @JsonIgnoreProperties para ignorar campos que no necesitamos.
 */
public class RawgApiDtos {

    /**
     * Respuesta de búsqueda de juegos de RAWG
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameSearchResponse {
        private Integer count;
        private String next;
        private String previous;
        private List<GameSummary> results;

        // Getters y Setters
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }

        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }

        public String getPrevious() { return previous; }
        public void setPrevious(String previous) { this.previous = previous; }

        public List<GameSummary> getResults() { return results; }
        public void setResults(List<GameSummary> results) { this.results = results; }
    }

    /**
     * Información resumida de un juego (para búsquedas)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameSummary {
        private Integer id;
        private String name;
        private String slug;

        @JsonProperty("background_image")
        private String backgroundImage;

        private Double rating;

        @JsonProperty("rating_top")
        private Integer ratingTop;

        private String released;

        @JsonProperty("reviews_count")
        private Integer reviewsCount;

        private List<Platform> platforms;
        private List<Genre> genres;

        // Getters y Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }

        public String getBackgroundImage() { return backgroundImage; }
        public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }

        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }

        public Integer getRatingTop() { return ratingTop; }
        public void setRatingTop(Integer ratingTop) { this.ratingTop = ratingTop; }

        public String getReleased() { return released; }
        public void setReleased(String released) { this.released = released; }

        public Integer getReviewsCount() { return reviewsCount; }
        public void setReviewsCount(Integer reviewsCount) { this.reviewsCount = reviewsCount; }

        public List<Platform> getPlatforms() { return platforms; }
        public void setPlatforms(List<Platform> platforms) { this.platforms = platforms; }

        public List<Genre> getGenres() { return genres; }
        public void setGenres(List<Genre> genres) { this.genres = genres; }
    }

    /**
     * Detalles completos de un juego
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameDetails extends GameSummary {
        private String description;

        @JsonProperty("description_raw")
        private String descriptionRaw;

        private String website;

        @JsonProperty("metacritic")
        private Integer metacriticScore;

        @JsonProperty("playtime")
        private Integer averagePlaytime;

        private List<Developer> developers;
        private List<Publisher> publishers;
        private List<String> tags;

        // Getters y Setters adicionales
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDescriptionRaw() { return descriptionRaw; }
        public void setDescriptionRaw(String descriptionRaw) { this.descriptionRaw = descriptionRaw; }

        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }

        public Integer getMetacriticScore() { return metacriticScore; }
        public void setMetacriticScore(Integer metacriticScore) { this.metacriticScore = metacriticScore; }

        public Integer getAveragePlaytime() { return averagePlaytime; }
        public void setAveragePlaytime(Integer averagePlaytime) { this.averagePlaytime = averagePlaytime; }

        public List<Developer> getDevelopers() { return developers; }
        public void setDevelopers(List<Developer> developers) { this.developers = developers; }

        public List<Publisher> getPublishers() { return publishers; }
        public void setPublishers(List<Publisher> publishers) { this.publishers = publishers; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    /**
     * Información de plataforma
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Platform {
        private PlatformInfo platform;

        public PlatformInfo getPlatform() { return platform; }
        public void setPlatform(PlatformInfo platform) { this.platform = platform; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PlatformInfo {
            private Integer id;
            private String name;
            private String slug;

            public Integer getId() { return id; }
            public void setId(Integer id) { this.id = id; }

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }

            public String getSlug() { return slug; }
            public void setSlug(String slug) { this.slug = slug; }
        }
    }

    /**
     * Información de género
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Genre {
        private Integer id;
        private String name;
        private String slug;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
    }

    /**
     * Información de desarrollador
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Developer {
        private Integer id;
        private String name;
        private String slug;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
    }

    /**
     * Información de publisher
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Publisher {
        private Integer id;
        private String name;
        private String slug;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
    }

    /**
     * Respuesta de screenshots de un juego
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScreenshotsResponse {
        private List<Screenshot> results;

        public List<Screenshot> getResults() { return results; }
        public void setResults(List<Screenshot> results) { this.results = results; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Screenshot {
            private Integer id;
            private String image;

            public Integer getId() { return id; }
            public void setId(Integer id) { this.id = id; }

            public String getImage() { return image; }
            public void setImage(String image) { this.image = image; }
        }
    }
}