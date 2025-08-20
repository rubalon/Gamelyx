package com.gamelyx.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que registra cuando un usuario rechaza una sugerencia de amistad
 * basada en un juego específico. Esto evita que el sistema vuelva a sugerir
 * el mismo usuario por el mismo juego.
 */
@Entity
@Table(name = "suggestion_rejections",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_rejection",
                        columnNames = {"user_id", "rejected_user_id", "game_id"})
        },
        indexes = {
                @Index(name = "idx_suggestion_rejections_user_game",
                        columnList = "user_id, game_id"),
                @Index(name = "idx_suggestion_rejections_user",
                        columnList = "user_id"),
                @Index(name = "idx_suggestion_rejections_rejected_at",
                        columnList = "rejected_at")
        })
public class SuggestionRejection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Usuario que rechaza la sugerencia
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Usuario que fue rechazado como sugerencia
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_user_id", nullable = false)
    private User rejectedUser;

    /**
     * Juego por el cual se hizo la sugerencia
     * Permite que el mismo usuario pueda ser sugerido por otros juegos
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * Timestamp de cuando se rechazó la sugerencia
     */
    @Column(name = "rejected_at", nullable = false)
    private LocalDateTime rejectedAt;

    // Constructors
    public SuggestionRejection() {
        this.rejectedAt = LocalDateTime.now();
    }

    public SuggestionRejection(User user, User rejectedUser, Game game) {
        this();
        this.user = user;
        this.rejectedUser = rejectedUser;
        this.game = game;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getRejectedUser() {
        return rejectedUser;
    }

    public void setRejectedUser(User rejectedUser) {
        this.rejectedUser = rejectedUser;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    @Override
    public String toString() {
        return "SuggestionRejection{" +
                "id=" + id +
                ", user=" + (user != null ? user.getUsername() : null) +
                ", rejectedUser=" + (rejectedUser != null ? rejectedUser.getUsername() : null) +
                ", game=" + (game != null ? game.getSlug() : null) +
                ", rejectedAt=" + rejectedAt +
                '}';
    }
}