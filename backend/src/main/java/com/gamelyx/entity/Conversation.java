package com.gamelyx.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_one_id", "user_two_id"}))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_one_id", nullable = false)
    private User userOne;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_two_id", nullable = false)
    private User userTwo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Conversation() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Conversation(User user1, User user2) {
        this();
        
        // Garantizar ordenamiento: userOne siempre tiene el UUID menor
        if (user1.getId().compareTo(user2.getId()) < 0) {
            this.userOne = user1;
            this.userTwo = user2;
        } else {
            this.userOne = user2;
            this.userTwo = user1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUserOne() {
        return userOne;
    }

    public void setUserOne(User userOne) {
        this.userOne = userOne;
    }

    public User getUserTwo() {
        return userTwo;
    }

    public void setUserTwo(User userTwo) {
        this.userTwo = userTwo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean includesUser(UUID userId) {
        return userOne.getId().equals(userId) || userTwo.getId().equals(userId);
    }

    public User getOtherUser(UUID currentUserId) {
        if (userOne.getId().equals(currentUserId)) {
            return userTwo;
        } else if (userTwo.getId().equals(currentUserId)) {
            return userOne;
        }
        throw new IllegalArgumentException("User not part of this conversation");
    }
}