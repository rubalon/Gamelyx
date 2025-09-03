package com.gamelyx.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa las solicitudes de amistad entre usuarios.
 * Incluye información sobre el origen de la solicitud (búsqueda o sugerencia)
 * y metadatos adicionales para funcionalidades futuras como chat.
 */
@Entity
@Table(name = "friend_requests",
        indexes = {
                @Index(name = "idx_friend_requests_receiver_status", columnList = "receiver_id, status"),
                @Index(name = "idx_friend_requests_sender_status", columnList = "sender_id, status"),
                @Index(name = "idx_friend_requests_suggested_game", columnList = "suggested_game_id"),
                @Index(name = "idx_friend_requests_created_at", columnList = "created_at")
        })
@Check(constraints = "sender_id <> receiver_id")
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Usuario que envía la solicitud de amistad
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * Usuario que recibe la solicitud de amistad
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    /**
     * Estado actual de la solicitud
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status = FriendRequestStatus.PENDING;

    /**
     * Origen de la solicitud (búsqueda manual o sugerencia automática)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_source", nullable = false)
    private RequestSource source = RequestSource.SEARCH;

    /**
     * Juego en común que generó la sugerencia (solo para source = SUGGESTION)
     * Este campo será null para solicitudes originadas desde búsqueda manual
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_game_id")
    private Game sharedGame;

    /**
     * ID del chat asociado a esta solicitud (para funcionalidad futura HU-21)
     * Permite asociar mensajes iniciales con la solicitud de amistad
     */
    @Column(name = "chat_id")
    private UUID chatId;

    /**
     * Timestamp de cuando se creó la solicitud
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp de cuando se respondió la solicitud (aceptada o rechazada)
     */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    /**
     * Indica si el sender ha sido notificado de la respuesta.
     * - Para solicitudes ACCEPTED: no se usa (se eliminan directamente)
     * - Para solicitudes REJECTED: true = ya notificado
     * - Para solicitudes PENDING: siempre false
     */
    @Column(name = "is_sender_notified", nullable = false)
    private Boolean isSenderNotified = false;

    // Constructors
    public FriendRequest() {
        this.createdAt = LocalDateTime.now();
        this.isSenderNotified = false;
    }

    public FriendRequest(User sender, User receiver, RequestSource source) {
        this();
        this.sender = sender;
        this.receiver = receiver;
        this.source = source;
    }

    public FriendRequest(User sender, User receiver, RequestSource source, Game sharedGame) {
        this(sender, receiver, source);
        this.sharedGame = sharedGame;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public FriendRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FriendRequestStatus status) {
        this.status = status;
        if (status != FriendRequestStatus.PENDING && this.respondedAt == null) {
            this.respondedAt = LocalDateTime.now();
        }
    }

    public RequestSource getSource() {
        return source;
    }

    public void setSource(RequestSource source) {
        this.source = source;
    }

    public Game getSharedGame() {
        return sharedGame;
    }

    public void setSharedGame(Game sharedGame) {
        this.sharedGame = sharedGame;
    }

    public UUID getChatId() {
        return chatId;
    }

    public void setChatId(UUID chatId) {
        this.chatId = chatId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Boolean getIsSenderNotified() {
        return isSenderNotified;
    }

    public void setIsSenderNotified(Boolean isSenderNotified) {
        this.isSenderNotified = isSenderNotified;
    }

    // Business Methods

    /**
     * Acepta la solicitud de amistad
     */
    public void accept() {
        this.status = FriendRequestStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * Rechaza la solicitud de amistad
     */
    public void reject() {
        this.status = FriendRequestStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * Marca que el sender ha sido notificado de la respuesta.
     * Usado principalmente para solicitudes rechazadas.
     */
    public void markSenderAsNotified() {
        this.isSenderNotified = true;
    }

    /**
     * Verifica si la solicitud está pendiente
     */
    public boolean isPending() {
        return this.status == FriendRequestStatus.PENDING;
    }

    /**
     * Verifica si la solicitud fue originada por una sugerencia automática
     */
    public boolean isFromSuggestion() {
        return this.source == RequestSource.SUGGESTION;
    }

    @Override
    public String toString() {
        return "FriendRequest{" +
                "id=" + id +
                ", sender=" + (sender != null ? sender.getUsername() : null) +
                ", receiver=" + (receiver != null ? receiver.getUsername() : null) +
                ", status=" + status +
                ", source=" + source +
                ", isSenderNotified=" + isSenderNotified +
                ", createdAt=" + createdAt +
                '}';
    }

    // ================================================
    // ENUMS INTERNOS
    // ================================================

    /**
     * Estados posibles de una solicitud de amistad
     */
    public enum FriendRequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED;

        /**
         * Verifica si el estado representa una solicitud resuelta (no pendiente)
         */
        public boolean isResolved() {
            return this == ACCEPTED || this == REJECTED;
        }

        /**
         * Verifica si el estado representa una solicitud exitosa
         */
        public boolean isSuccessful() {
            return this == ACCEPTED;
        }
    }

    /**
     * Origen de una solicitud de amistad - determina cómo se generó
     */
    public enum RequestSource {
        SEARCH,      // Desde buscador manual de usuarios (HU-16)
        SUGGESTION;  // Desde sugerencias automáticas (HU-20)
    }
}