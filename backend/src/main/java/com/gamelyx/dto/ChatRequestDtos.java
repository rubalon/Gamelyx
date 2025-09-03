package com.gamelyx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTOs para requests del sistema de chat.
 * Siguiendo el patrón de arquitectura del proyecto.
 */
public class ChatRequestDtos {

    /**
     * Request para enviar un mensaje.
     * No incluye conversationId - se deriva de recipientId.
     */
    public record SendMessageRequestDto(
            @NotNull(message = "El ID del destinatario es requerido")
            UUID recipientId,
            
            @NotBlank(message = "El contenido del mensaje no puede estar vacío")
            @Size(max = 1000, message = "El mensaje no puede exceder 1000 caracteres")
            String content
    ) {}

    /**
     * Request para obtener mensajes de una conversación
     */
    public record GetMessagesRequestDto(
            @NotNull(message = "El ID del otro usuario es requerido")
            UUID otherUserId,
            
            Integer page,
            Integer limit
    ) {
        public GetMessagesRequestDto {
            // Valores por defecto
            if (page == null || page < 0) page = 0;
            if (limit == null || limit <= 0 || limit > 50) limit = 20;
        }
    }

    /**
     * Request para marcar mensajes como leídos
     */
    public record MarkAsReadRequestDto(
            @NotNull(message = "El ID del otro usuario es requerido")
            UUID otherUserId
    ) {}
}