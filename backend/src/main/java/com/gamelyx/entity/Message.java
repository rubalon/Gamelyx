package com.gamelyx.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @NotNull
    @Size(max = 10000)
    @Column(name = "encrypted_content", nullable = false, columnDefinition = "TEXT")
    private String encryptedContent;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    public Message() {
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }

    public Message(Conversation conversation, User sender, String encryptedContent) {
        this();
        this.conversation = conversation;
        this.sender = sender;
        this.encryptedContent = encryptedContent;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getEncryptedContent() {
        return encryptedContent;
    }

    public void setEncryptedContent(String encryptedContent) {
        this.encryptedContent = encryptedContent;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}