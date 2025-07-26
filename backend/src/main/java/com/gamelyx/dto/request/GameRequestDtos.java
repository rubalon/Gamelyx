package com.gamelyx.dto.request;

import com.gamelyx.entity.UserGameDetails.GameStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTOs para las peticiones de las APIs de juegos
 *
 * Estos DTOs definen exactamente qué información puede enviar el frontend,
 * incluyendo validaciones apropiadas.
 */
public class GameRequestDtos {

    /**
     * Petición para crear o actualizar detalles de usuario-juego (unificado)
     */
    public static class UpdateGameDetailsRequest {

        // Estado del juego (opcional)
        private GameStatus status;

        // Rating del usuario (opcional, 1-10)
        @Min(value = 1, message = "El rating mínimo es 1")
        @Max(value = 10, message = "El rating máximo es 10")
        private Integer rating;

        // Texto de la review (opcional)
        @Size(max = 2000, message = "La review no puede exceder 2000 caracteres")
        private String reviewText;

        // Fecha de completado (opcional, para status COMPLETED)
        private LocalDateTime completedAt;

        // Constructores
        public UpdateGameDetailsRequest() {}

        public UpdateGameDetailsRequest(GameStatus status, Integer rating, String reviewText) {
            this.status = status;
            this.rating = rating;
            this.reviewText = reviewText;
        }

        // Getters y Setters
        public GameStatus getStatus() { return status; }
        public void setStatus(GameStatus status) { this.status = status; }

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }

        public String getReviewText() { return reviewText; }
        public void setReviewText(String reviewText) { this.reviewText = reviewText; }

        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

        // Métodos de utilidad
        public boolean hasStatus() { return status != null; }
        public boolean hasRating() { return rating != null; }
        public boolean hasReviewText() { return reviewText != null && !reviewText.trim().isEmpty(); }
        public boolean hasCompletedAt() { return completedAt != null; }

        @Override
        public String toString() {
            return String.format("UpdateGameDetailsRequest{status=%s, rating=%s, hasReview=%s}",
                    status, rating, hasReviewText());
        }
    }

    /**
     * Petición para añadir un juego desde API externa
     */
    public static class AddGameFromExternalRequest {

        // ID del juego en RAWG (requerido)
        @Min(value = 1, message = "El RAWG ID debe ser positivo")
        private Integer rawgId;

        // Estado inicial opcional
        private GameStatus initialStatus;

        // Rating inicial opcional
        @Min(value = 1, message = "El rating mínimo es 1")
        @Max(value = 10, message = "El rating máximo es 10")
        private Integer initialRating;

        // Review inicial opcional
        @Size(max = 2000, message = "La review no puede exceder 2000 caracteres")
        private String initialReviewText;

        // Constructores
        public AddGameFromExternalRequest() {}

        public AddGameFromExternalRequest(Integer rawgId) {
            this.rawgId = rawgId;
        }

        // Getters y Setters
        public Integer getRawgId() { return rawgId; }
        public void setRawgId(Integer rawgId) { this.rawgId = rawgId; }

        public GameStatus getInitialStatus() { return initialStatus; }
        public void setInitialStatus(GameStatus initialStatus) { this.initialStatus = initialStatus; }

        public Integer getInitialRating() { return initialRating; }
        public void setInitialRating(Integer initialRating) { this.initialRating = initialRating; }

        public String getInitialReviewText() { return initialReviewText; }
        public void setInitialReviewText(String initialReviewText) { this.initialReviewText = initialReviewText; }

        @Override
        public String toString() {
            return String.format("AddGameFromExternalRequest{rawgId=%s, status=%s}", rawgId, initialStatus);
        }
    }

    /**
     * Petición de búsqueda de juegos
     */
    public static class GameSearchRequest {

        // Término de búsqueda
        @Size(min = 2, max = 100, message = "La búsqueda debe tener entre 2 y 100 caracteres")
        private String query;

        // Página (opcional, default 0)
        @Min(value = 0, message = "La página no puede ser negativa")
        private Integer page = 0;

        // Tamaño de página (opcional, default 20)
        @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
        @Max(value = 100, message = "El tamaño de página no puede exceder 100")
        private Integer size = 20;

        // Rating mínimo (opcional)
        @Min(value = 1, message = "El rating mínimo es 1")
        @Max(value = 10, message = "El rating máximo es 10")
        private Double minRating;

        // Constructores
        public GameSearchRequest() {}

        public GameSearchRequest(String query) {
            this.query = query;
        }

        // Getters y Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }

        public Double getMinRating() { return minRating; }
        public void setMinRating(Double minRating) { this.minRating = minRating; }

        @Override
        public String toString() {
            return String.format("GameSearchRequest{query='%s', page=%d, size=%d}", query, page, size);
        }
    }

    /**
     * Petición para filtrar biblioteca de usuario
     */
    public static class UserLibraryFilterRequest {

        // Estado específico (opcional)
        private GameStatus status;

        // Búsqueda por nombre de juego (opcional)
        @Size(max = 100, message = "La búsqueda no puede exceder 100 caracteres")
        private String gameName;

        // Rating mínimo (opcional)
        @Min(value = 1, message = "El rating mínimo es 1")
        @Max(value = 10, message = "El rating máximo es 10")
        private Integer minRating;

        // Solo juegos con reviews (opcional)
        private Boolean hasReview;

        // Página (opcional, default 0)
        @Min(value = 0, message = "La página no puede ser negativa")
        private Integer page = 0;

        // Tamaño de página (opcional, default 20)
        @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
        @Max(value = 100, message = "El tamaño de página no puede exceder 100")
        private Integer size = 20;

        // Constructores
        public UserLibraryFilterRequest() {}

        // Getters y Setters
        public GameStatus getStatus() { return status; }
        public void setStatus(GameStatus status) { this.status = status; }

        public String getGameName() { return gameName; }
        public void setGameName(String gameName) { this.gameName = gameName; }

        public Integer getMinRating() { return minRating; }
        public void setMinRating(Integer minRating) { this.minRating = minRating; }

        public Boolean getHasReview() { return hasReview; }
        public void setHasReview(Boolean hasReview) { this.hasReview = hasReview; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }

        @Override
        public String toString() {
            return String.format("UserLibraryFilterRequest{status=%s, gameName='%s', page=%d, size=%d}",
                    status, gameName, page, size);
        }
    }
}