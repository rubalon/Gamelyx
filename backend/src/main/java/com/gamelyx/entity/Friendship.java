package com.gamelyx.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa una amistad confirmada entre dos usuarios.
 * Utiliza un enfoque bidireccional donde cada amistad se almacena como dos registros:
 * - Usuario A -> Usuario B
 * - Usuario B -> Usuario A
 *
 * Esto facilita las consultas y mantiene la semántica correcta de "mis amigos".
 * Cuando dos usuarios se hacen amigos, se crean automáticamente ambos registros.
 */
@Entity
@Table(name = "friendships",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_friend", columnNames = {"user_id", "friend_id"})
        },
        indexes = {
                @Index(name = "idx_friendships_user_id", columnList = "user_id"),
                @Index(name = "idx_friendships_friend_id", columnList = "friend_id"),
                @Index(name = "idx_friendships_user_friend", columnList = "user_id, friend_id"),
                @Index(name = "idx_friendships_created_at", columnList = "created_at")
        })
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Usuario propietario de esta relación de amistad.
     * Para obtener todos los amigos de un usuario, se busca por este campo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * El amigo del usuario.
     * Representa la otra parte de la relación de amistad.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    /**
     * Timestamp de cuando se estableció la amistad.
     * Se establece cuando se acepta una FriendRequest.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public Friendship() {
        this.createdAt = LocalDateTime.now();
    }

    public Friendship(User user, User friend) {
        this();
        this.user = user;
        this.friend = friend;
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

    public User getFriend() {
        return friend;
    }

    public void setFriend(User friend) {
        this.friend = friend;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Friendship{" +
                "id=" + id +
                ", user=" + (user != null ? user.getUsername() : null) +
                ", friend=" + (friend != null ? friend.getUsername() : null) +
                ", createdAt=" + createdAt +
                '}';
    }
}