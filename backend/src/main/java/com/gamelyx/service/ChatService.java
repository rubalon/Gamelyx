package com.gamelyx.service;

import com.gamelyx.config.ChatConfig;
import com.gamelyx.dto.ChatRequestDtos.*;
import com.gamelyx.dto.ChatResponseDtos.*;
import com.gamelyx.entity.Conversation;
import com.gamelyx.entity.Message;
import com.gamelyx.entity.User;
import com.gamelyx.mapper.ChatMapper;
import com.gamelyx.repository.ConversationRepository;
import com.gamelyx.repository.MessageRepository;
import com.gamelyx.util.AESUtil;
import com.gamelyx.validator.ChatValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatValidator chatValidator;
    private final ChatMapper chatMapper;
    private final ChatConfig chatConfig;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ChatValidator chatValidator,
            ChatMapper chatMapper,
            ChatConfig chatConfig) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.chatValidator = chatValidator;
        this.chatMapper = chatMapper;
        this.chatConfig = chatConfig;
    }

    /**
     * Envía un mensaje desde un usuario a otro.
     * Crea la conversación automáticamente si no existe.
     * El mensaje se cifra antes de guardarse en BD.
     * 
     * @param senderUsername Username del usuario que envía
     * @param request DTO con recipientId y contenido del mensaje
     * @return SendMessageResponseDto con el mensaje creado
     */
    @Transactional
    public SendMessageResponseDto sendMessage(String senderUsername, SendMessageRequestDto request) {
        // 1. Validar datos del mensaje
        ChatValidator.ValidationResult validation = chatValidator.validateSendMessage(
                senderUsername, request.recipientId(), request.content());

        // 2. Encontrar o crear conversación
        Conversation conversation = findOrCreateConversation(
                validation.getCallerUser().getId(), 
                validation.getTargetUser().getId());

        // 3. Cifrar el contenido del mensaje
        String encryptedContent = AESUtil.encrypt(request.content().trim(), chatConfig.getEncryption().getKey());

        // 4. Crear y guardar el mensaje
        Message message = new Message(conversation, validation.getCallerUser(), encryptedContent);
        Message savedMessage = messageRepository.save(message);

        // 5. Actualizar timestamp de la conversación
        conversation.preUpdate();
        conversationRepository.save(conversation);

        // 6. Convertir a DTO para respuesta
        MessageDto messageDto = chatMapper.toMessageDto(savedMessage, request.content().trim());
        return chatMapper.toSendMessageResponseDto(messageDto, conversation.getId());
    }

    /**
     * Obtiene el historial de mensajes entre el usuario actual y otro usuario.
     * Los mensajes se descifran automáticamente antes de enviarlos.
     * Soporta paginación para manejar conversaciones largas.
     * 
     * @param currentUsername Username del usuario actual
     * @param request DTO con otherUserId y parámetros de paginación
     * @return ConversationMessagesDto con mensajes descifrados y info de paginación
     */
    public ConversationMessagesDto getConversationMessages(String currentUsername, GetMessagesRequestDto request) {
        // 1. Validar acceso a la conversación
        ChatValidator.ValidationResult validation = chatValidator.validateGetConversationMessages(
                currentUsername, request.otherUserId());

        // 2. Encontrar o crear conversación (si no existe, devuelve lista vacía)
        Conversation conversation = findOrCreateConversation(
                validation.getCallerUser().getId(), 
                validation.getTargetUser().getId());

        // 3. Obtener mensajes paginados (más antiguos primero para orden correcto en chat)
        Pageable pageable = PageRequest.of(
                request.page(), 
                request.limit(), 
                Sort.by(Sort.Direction.ASC, "sentAt"));
        
        Page<Message> messagesPage = messageRepository.findByConversationId(conversation.getId(), pageable);
        List<Message> messages = messagesPage.getContent();

        // 4. Descifrar contenidos de mensajes
        List<String> decryptedContents = messages.stream()
                .map(message -> AESUtil.decrypt(message.getEncryptedContent(), chatConfig.getEncryption().getKey()))
                .toList();

        // 5. Convertir a DTOs
        List<MessageDto> messageDtos = chatMapper.toMessageDtoList(messages, decryptedContents);

        return chatMapper.toConversationMessagesDto(conversation, validation.getCallerUser().getId(), messageDtos, messagesPage);
    }

    /**
     * Marca todos los mensajes no leídos de una conversación como leídos.
     * Solo marca los mensajes enviados por el otro usuario.
     * 
     * @param currentUsername Username del usuario actual
     * @param request DTO con otherUserId
     * @return MarkAsReadResponseDto con resultado de la operación
     */
    @Transactional
    public MarkAsReadResponseDto markMessagesAsRead(String currentUsername, MarkAsReadRequestDto request) {
        // 1. Validar acceso
        ChatValidator.ValidationResult validation = chatValidator.validateMarkMessagesAsRead(
                currentUsername, request.otherUserId());

        // 2. Encontrar o crear conversación (si no existe, no hay mensajes que marcar)
        Conversation conversation = findOrCreateConversation(
                validation.getCallerUser().getId(), 
                validation.getTargetUser().getId());

        // 3. Marcar mensajes como leídos usando query bulk (más eficiente)
        int markedCount = messageRepository.markMessagesAsReadBulk(
                conversation.getId(), 
                validation.getCallerUser().getId());

        return chatMapper.toMarkAsReadResponseDto(true, markedCount, validation.getCallerUser().getId());
    }

    // ================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // ================================================

    /**
     * Encuentra una conversación existente o crea una nueva.
     * Aplica el ordenamiento correcto de usuarios automáticamente.
     * 
     * @param user1Id ID del primer usuario
     * @param user2Id ID del segundo usuario
     * @return Conversation existente o recién creada
     */
    private Conversation findOrCreateConversation(UUID user1Id, UUID user2Id) {
        UUID smallerId = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
        UUID largerId = user1Id.compareTo(user2Id) < 0 ? user2Id : user1Id;

        return conversationRepository.findByUserOneIdAndUserTwoId(smallerId, largerId)
                .orElseGet(() -> {
                    User user1 = chatValidator.validateUserExists(smallerId);
                    User user2 = chatValidator.validateUserExists(largerId);
                    
                    Conversation newConversation = new Conversation(user1, user2);
                    return conversationRepository.save(newConversation);
                });
    }
}