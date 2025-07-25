package com.gamelyx.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad UserGameDetails - Relación unificada entre Usuario y Juego
 *
 * Esta entidad combina:
 * - Estado del juego en la biblioteca del usuario (wishlist, playing, completed, archived)
 * - Rating y review del usuario sobre el juego
 * - Metadata de progreso (horas jugadas, fecha de finalización, etc.)
 *
 * Diseño unificado: Una sola entidad maneja tanto el estado como las reviews,
 * simplificando las consultas y manteniendo la consistencia de datos.
 */
@Entity
@Table(name = "user_game_details",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "game_id"},
                name = "uk_user_game"
        ),
        indexes = {
                @Index(name = "idx_user_game_user_id", columnList = "user_id"),
                @Index(name = "idx_user_game_game_id", columnList = "game_id"),
                @Index(name = "idx_user_game_status", columnList = "status"),
                @Index(name = "idx_user_game_rating", columnList = "rating"),
                @Index(name = "idx_user_game_created", columnList = "created_at")
        })
public class UserGameDetails {

    /**
     * Estados posibles para un juego en la biblioteca del usuario
     */
    public enum GameStatus {
        WISHLIST,
        PLAYING,
        COMPLETED,
        ARCHIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ===== RELACIONES =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    // ===== SECCIÓN: ESTADO Y PROGRESO =====

    /**
     * Estado del juego en la biblioteca del usuario
     * NULLABLE - El usuario puede agregar un juego sin categorizar inicialmente
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GameStatus status;

    /**
     * Cuándo se añadió el juego a la biblioteca del usuario
     */
    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    /**
     * Última vez que se cambió el status
     */
    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    /**
     * Cuándo se marcó como completado (solo para status COMPLETED)
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    // ===== SECCIÓN: RATING Y REVIEW =====

    /**
     * Rating del usuario para este juego (1-10)
     * NULLABLE - El usuario puede no haber puntuado aún
     */
    @Column(name = "rating")
    @Min(value = 1, message = "El rating mínimo es 1")
    @Max(value = 10, message = "El rating máximo es 10")
    private Integer rating;

    /**
     * Texto de la review del usuario
     * NULLABLE - El usuario puede puntuar sin escribir review
     */
    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    /**
     * Cuándo se escribió la primera review
     */
    @Column(name = "review_created_at")
    private LocalDateTime reviewCreatedAt;

    /**
     * Última vez que se actualizó la review
     */
    @Column(name = "review_updated_at")
    private LocalDateTime reviewUpdatedAt;

    // ===== SECCIÓN: METADATA Y CONTROL =====

    /**
     * Cualquier cambio en este registro
     */
    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    // ===== CONSTRUCTORES =====

    public UserGameDetails() {}

    public UserGameDetails(User user, Game game) {
        this.user = user;
        this.game = game;
    }

    public UserGameDetails(User user, Game game, GameStatus status) {
        this.user = user;
        this.game = game;
        this.status = status;
        this.statusUpdatedAt = LocalDateTime.now();
    }

    // ===== GETTERS Y SETTERS =====

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) {
        this.status = status;
        this.statusUpdatedAt = LocalDateTime.now();

        // Auto-set completedAt cuando se marca como completado
        if (status == GameStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public LocalDateTime getStatusUpdatedAt() { return statusUpdatedAt; }
    public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }


    public Integer getRating() { return rating; }
    public void setRating(Integer rating) {
        this.rating = rating;

        // Auto-set review timestamps cuando se agrega rating
        if (rating != null) {
            if (this.reviewCreatedAt == null) {
                this.reviewCreatedAt = LocalDateTime.now();
            }
            this.reviewUpdatedAt = LocalDateTime.now();
        }
    }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;

        // Auto-set review timestamps cuando se agrega texto
        if (reviewText != null && !reviewText.trim().isEmpty()) {
            if (this.reviewCreatedAt == null) {
                this.reviewCreatedAt = LocalDateTime.now();
            }
            this.reviewUpdatedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getReviewCreatedAt() { return reviewCreatedAt; }
    public void setReviewCreatedAt(LocalDateTime reviewCreatedAt) { this.reviewCreatedAt = reviewCreatedAt; }

    public LocalDateTime getReviewUpdatedAt() { return reviewUpdatedAt; }
    public void setReviewUpdatedAt(LocalDateTime reviewUpdatedAt) { this.reviewUpdatedAt = reviewUpdatedAt; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }


    // ===== MÉTODOS DE UTILIDAD =====

    /**
     * Verifica si el usuario ha escrito una review (rating o texto)
     */
    public boolean hasReview() {
        return rating != null || (reviewText != null && !reviewText.trim().isEmpty());
    }

    /**
     * Verifica si el juego tiene un estado asignado
     */
    public boolean hasStatus() {
        return status != null;
    }

    /**
     * Verifica si el juego está en la biblioteca del usuario
     */
    public boolean isInLibrary() {
        return status != null;
    }

    /**
     * Verifica si la review es completa (tiene rating y texto)
     */
    public boolean hasCompleteReview() {
        return rating != null && reviewText != null && !reviewText.trim().isEmpty();
    }

    /**
     * Verifica si puede tener una review completa basado en el estado
     */
    public boolean canHaveCompleteReview() {
        return status == GameStatus.COMPLETED || status == GameStatus.PLAYING;
    }

    /**
     * Obtiene un resumen del progreso para mostrar en UI
     */
    public String getProgressSummary() {
        if (status == null) {
            return "Sin estado";
        }

        StringBuilder summary = new StringBuilder(status.name());


        if (rating != null) {
            summary.append(" • ").append("★").append(rating).append("/10");
        }

        return summary.toString();
    }

    /**
     * Actualiza tanto rating como review de una vez
     */
    public void updateReview(Integer newRating, String newReviewText) {
        setRating(newRating);
        setReviewText(newReviewText);
    }

    /**
     * Actualiza el estado y opcionalmente la fecha de completado
     */
    public void updateStatus(GameStatus newStatus, LocalDateTime completionDate) {
        setStatus(newStatus);
        if (newStatus == GameStatus.COMPLETED && completionDate != null) {
            this.completedAt = completionDate;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "UserGameDetails{id=%s, user=%s, game=%s, status=%s, rating=%s, hasReview=%s}",
                id,
                user != null ? user.getUsername() : "null",
                game != null ? game.getName() : "null",
                status,
                rating,
                hasReview()
        );
    }
}