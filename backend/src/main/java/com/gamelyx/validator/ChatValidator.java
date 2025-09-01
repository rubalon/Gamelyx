package com.gamelyx.validator;

import com.gamelyx.entity.Conversation;
import com.gamelyx.entity.User;
import com.gamelyx.repository.ConversationRepository;
import com.gamelyx.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChatValidator {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    public ChatValidator(UserRepository userRepository, ConversationRepository conversationRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
    }

    public ValidationResult validateSendMessage(String senderUsername, UUID recipientId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("El contenido del mensaje no puede estar vacío");
        }

        if (content.trim().length() > 1000) {
            throw new IllegalArgumentException("El mensaje no puede exceder 1000 caracteres");
        }

        User senderUser = validateUserExists(senderUsername);
        User recipientUser = validateUserExists(recipientId);

        if (senderUser.getId().equals(recipientUser.getId())) {
            throw new IllegalArgumentException("No se puede enviar mensaje a uno mismo");
        }

        return new ValidationResult(senderUser, recipientUser, null);
    }

    public ValidationResult validateGetConversationMessages(String currentUsername, UUID otherUserId) {
        User currentUser = validateUserExists(currentUsername);
        User otherUser = validateUserExists(otherUserId);

        if (currentUser.getId().equals(otherUser.getId())) {
            throw new IllegalArgumentException("No se puede obtener conversación consigo mismo");
        }

        return new ValidationResult(currentUser, otherUser, null);
    }

    public ValidationResult validateMarkMessagesAsRead(String currentUsername, UUID otherUserId) {
        User currentUser = validateUserExists(currentUsername);
        User otherUser = validateUserExists(otherUserId);

        if (currentUser.getId().equals(otherUser.getId())) {
            throw new IllegalArgumentException("No se puede marcar mensajes de conversación consigo mismo");
        }

        return new ValidationResult(currentUser, otherUser, null);
    }

    public ValidationResult validateConversationAccess(String currentUsername, UUID conversationId) {
        User currentUser = validateUserExists(currentUsername);
        Conversation conversation = validateConversationExists(conversationId);

        if (!conversation.includesUser(currentUser.getId())) {
            throw new IllegalArgumentException("No tienes acceso a esta conversación");
        }

        return new ValidationResult(currentUser, null, conversation);
    }

    public User validateUserExists(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    public User validateUserExists(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
    }

    public Conversation validateConversationExists(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversación no encontrada: " + conversationId));
    }

    public static class ValidationResult {
        private final User callerUser;
        private final User targetUser;
        private final Conversation conversation;

        public ValidationResult(User callerUser, User targetUser, Conversation conversation) {
            this.callerUser = callerUser;
            this.targetUser = targetUser;
            this.conversation = conversation;
        }

        public User getCallerUser() {
            return callerUser;
        }

        public User getTargetUser() {
            return targetUser;
        }

        public Conversation getConversation() {
            return conversation;
        }
    }
}