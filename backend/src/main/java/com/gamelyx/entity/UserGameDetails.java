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
 * - Metadata de progreso y timestamps
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
                @Index(name = "idx_user_review_time_updated", columnList = "review_updated_at")
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

    // ===== ID Y RELACIONES =====

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    // ===== ESTADO DEL JUEGO =====

    /**
     * Estado del juego en la biblioteca del usuario
     * NULLABLE - El usuario puede agregar un juego sin categorizar inicialmente
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private GameStatus status;


    // ===== RATING Y REVIEW =====

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

    // ===== TIMESTAMPS AUTOMÁTICOS =====

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
    }

    // ===== GETTERS Y SETTERS =====

    // ID y relaciones
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    // Estado del juego
    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) {
        this.status = status;
    }

    // Rating y review
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) {
        this.rating = rating;
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

    // Timestamps automáticos
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