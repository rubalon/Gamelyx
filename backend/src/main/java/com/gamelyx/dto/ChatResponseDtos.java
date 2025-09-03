package com.gamelyx.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs para responses del sistema de chat.
 * Siguiendo el patrón de arquitectura del proyecto.
 */
public class ChatResponseDtos {

    /**
     * Mensaje individual
     */
    public record MessageDto(
            UUID messageId,
            UUID senderId,
            String senderUsername,
            String content,
            LocalDateTime sentAt,
            Boolean isRead
    ) {}


    /**
     * Historial de mensajes de una conversación
     */
    public record ConversationMessagesDto(
            UUID conversationId,
            UserBasicDto otherUser,
            List<MessageDto> messages,
            Boolean hasMore,
            Integer currentPage,
            Integer totalPages
    ) {}

    /**
     * Response de envío de mensaje
     */
    public record SendMessageResponseDto(
            MessageDto message,
            UUID conversationId
    ) {}

    /**
     * Información básica de usuario para DTOs de chat
     */
    public record UserBasicDto(
            UUID userId,
            String username
    ) {}

    /**
     * Response para marcar mensajes como leídos
     */
    public record MarkAsReadResponseDto(
            Boolean success,
            Integer markedCount,
            UUID readByUserId
    ) {}

    /**
     * DTO para mensajes WebSocket
     */
    public record WebSocketMessageDto(
            String type,
            MessageDto message,
            UUID conversationId,
            UUID recipientId
    ) {}

    /**
     * DTO para notificaciones de lectura via WebSocket
     */
    public record WebSocketReadReceiptDto(
            String type,
            UUID readByUserId,
            Integer messageCount
    ) {}
}