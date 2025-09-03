package com.gamelyx.mapper;

import com.gamelyx.dto.ChatResponseDtos.*;
import com.gamelyx.entity.Conversation;
import com.gamelyx.entity.Message;
import com.gamelyx.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Mapper para convertir entidades de chat a DTOs.
 * Siguiendo el patrón de arquitectura del proyecto.
 */
@Component
public class ChatMapper {

    /**
     * Convierte una entidad Message a MessageDto.
     * IMPORTANTE: Requiere el contenido ya descifrado como parámetro separado
     * porque la entidad solo tiene el contenido cifrado.
     * 
     * @param message Entidad Message de la BD (con contenido cifrado)
     * @param decryptedContent Contenido del mensaje ya descifrado
     * @return MessageDto con toda la info del mensaje para el frontend
     */
    public MessageDto toMessageDto(Message message, String decryptedContent) {
        return new MessageDto(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                decryptedContent,  // Contenido ya descifrado
                message.getSentAt(),
                message.getIsRead()
        );
    }

    /**
     * Convierte una lista de mensajes a DTOs.
     * Mapea cada mensaje con su contenido descifrado correspondiente.
     * Las dos listas deben tener el mismo tamaño y orden.
     * 
     * @param messages Lista de entidades Message de la BD
     * @param decryptedContents Lista de contenidos ya descifrados (mismo orden)
     * @return Lista de MessageDto para el frontend
     */
    public List<MessageDto> toMessageDtoList(List<Message> messages, List<String> decryptedContents) {
        if (messages.size() != decryptedContents.size()) {
            throw new IllegalArgumentException("Las listas de mensajes y contenidos descifrados deben tener el mismo tamaño");
        }

        return messages.stream()
                .map(message -> {
                    int index = messages.indexOf(message);
                    return toMessageDto(message, decryptedContents.get(index));
                })
                .toList();
    }

    /**
     * Convierte User entity a información básica para DTOs de chat.
     * Solo incluye datos esenciales (id, username) sin información sensible.
     * 
     * @param user Entidad User completa
     * @return UserBasicDto con info mínima necesaria
     */
    public UserBasicDto toUserBasicDto(User user) {
        return new UserBasicDto(user.getId(), user.getUsername());
    }


    /**
     * Convierte conversación + mensajes paginados a DTO para historial de chat.
     * Incluye información de paginación y el "otro usuario" de la conversación.
     * 
     * @param conversation Entidad Conversation de la BD
     * @param currentUserId ID del usuario actual (para determinar "el otro")
     * @param messages Lista de MessageDto ya descifrados y procesados
     * @param page Información de paginación de Spring Data
     * @return ConversationMessagesDto con mensajes e info de paginación
     */
    public ConversationMessagesDto toConversationMessagesDto(Conversation conversation, 
                                                           UUID currentUserId,
                                                           List<MessageDto> messages,
                                                           Page<?> page) {
        User otherUser = conversation.getOtherUser(currentUserId);
        
        return new ConversationMessagesDto(
                conversation.getId(),
                toUserBasicDto(otherUser),
                messages,
                page.hasNext(),
                page.getNumber(),
                page.getTotalPages()
        );
    }

    /**
     * Crea response DTO para cuando se envía un mensaje exitosamente.
     * Incluye el mensaje creado y el ID de la conversación (para el frontend).
     * 
     * @param message MessageDto del mensaje recién creado
     * @param conversationId ID de la conversación donde se envió
     * @return SendMessageResponseDto para confirmar el envío
     */
    public SendMessageResponseDto toSendMessageResponseDto(MessageDto message, UUID conversationId) {
        return new SendMessageResponseDto(message, conversationId);
    }

    /**
     * Crea response DTO para operación de marcar mensajes como leídos.
     * 
     * @param success Si la operación fue exitosa
     * @param markedCount Número de mensajes que se marcaron como leídos
     * @param readByUserId ID del usuario que leyó los mensajes
     * @return MarkAsReadResponseDto con resultado de la operación
     */
    public MarkAsReadResponseDto toMarkAsReadResponseDto(boolean success, int markedCount, UUID readByUserId) {
        return new MarkAsReadResponseDto(success, markedCount, readByUserId);
    }

    /**
     * Convierte un mensaje a formato WebSocket para envío en tiempo real.
     * Incluye información adicional necesaria para enrutamiento WebSocket.
     * 
     * @param message MessageDto del mensaje a enviar
     * @param conversationId ID de la conversación
     * @param recipientId ID del usuario destinatario
     * @return WebSocketMessageDto formateado para WebSocket
     */
    public WebSocketMessageDto toWebSocketMessageDto(MessageDto message, UUID conversationId, UUID recipientId) {
        return new WebSocketMessageDto(
                "NEW_MESSAGE",
                message,
                conversationId,
                recipientId
        );
    }
}