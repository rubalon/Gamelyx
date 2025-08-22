package com.gamelyx.validator;

import com.gamelyx.dto.SocialRequestDtos.FriendRequestAction;
import com.gamelyx.dto.SocialRequestDtos.SendFriendRequestDto;
import com.gamelyx.entity.FriendRequest;
import com.gamelyx.entity.Game;
import com.gamelyx.entity.User;
import com.gamelyx.repository.FriendRequestRepository;
import com.gamelyx.repository.FriendshipRepository;
import com.gamelyx.repository.GameRepository;
import com.gamelyx.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Validator para operaciones sociales.
 * Centraliza todas las validaciones de negocio del sistema de amistades.
 */
@Component
public class SocialValidator {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final GameRepository gameRepository;

    public SocialValidator(
            UserRepository userRepository,
            FriendRequestRepository friendRequestRepository,
            FriendshipRepository friendshipRepository,
            GameRepository gameRepository) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.gameRepository = gameRepository;
    }

    // ================================================
    // ================================================
    // VALIDACIONES SOLICITUDES DE AMISTAD HU-17
    // ================================================
    // ================================================

    // ================================================
    // VALIDACIONES PARA ENVIAR SOLICITUD DE AMISTAD
    // ================================================

    /**
     * Valida todos los aspectos de enviar una solicitud de amistad.
     * Arroja excepciones específicas para cada tipo de error.
     */
    public ValidationResult validateSendFriendRequest(String senderUsername, SendFriendRequestDto request) {
        // 1. Validar que ambos usuarios existen
        User sender = validateUserExists(senderUsername);
        User receiver = validateUserExistsByUserId(request.targetUserId());

        // 2. Validar que no es auto-solicitud
        validateNotSelfRequest(sender.getId(), receiver.getId());

        // 3. Validar que no son amigos ya
        validateNotAlreadyFriends(sender.getId(), receiver.getId());

        // 4. Validar que no hay solicitud pendiente
        validateNoPendingRequest(sender.getId(), receiver.getId());

        // 5. Validar juego si es por sugerencia
        Game suggestedGame = validateSuggestionGame(request, sender.getId(), receiver.getId());

        return new ValidationResult(sender, receiver, suggestedGame);
    }

    /**
     * Valida que un usuario existe por username.
     */
    public User validateUserExists(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    /**
     * Valida que un usuario existe por username.
     */
    public User validateUserExistsByUserId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
    }

    /**
     * Valida que no es una auto-solicitud.
     */
    public void validateNotSelfRequest(UUID senderId, UUID receiverId) {
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("No puedes enviarte una solicitud a ti mismo");
        }
    }

    /**
     * Valida que los usuarios no son amigos ya.
     */
    public void validateNotAlreadyFriends(UUID userId1, UUID userId2) {
        if (friendshipRepository.areUsersFriends(userId1, userId2)) {
            throw new IllegalArgumentException("Ya sois amigos");
        }
    }

    /**
     * Valida que no hay solicitud pendiente entre los usuarios.
     */
    public void validateNoPendingRequest(UUID userId1, UUID userId2) {
        if (friendRequestRepository.existsPendingRequestBetween(userId1, userId2)) {
            throw new IllegalStateException("Ya existe una solicitud pendiente entre estos usuarios");
        }
    }

    /**
     * Valida el juego para solicitudes por sugerencia.
     */
    public Game validateSuggestionGame(SendFriendRequestDto request, UUID senderId, UUID receiverId) {
        if (request.source() != FriendRequest.RequestSource.SUGGESTION) {
            return null; // No es necesario para búsquedas manuales
        }

        // Para sugerencias, el juego es obligatorio
        if (request.gameSlug() == null || request.gameSlug().trim().isEmpty()) {
            throw new IllegalArgumentException("Las solicitudes por sugerencia requieren un juego");
        }

        // Verificar que el juego existe
        Game game = gameRepository.findBySlug(request.gameSlug())
                .orElseThrow(() -> new IllegalArgumentException("Juego no encontrado: " + request.gameSlug()));

        // Verificar que no se envió ya una sugerencia para este juego
        if (friendRequestRepository.existsSuggestionForGame(senderId, receiverId, game.getId())) {
            throw new IllegalStateException("Ya se envió una solicitud por sugerencia para este juego");
        }

        return game;
    }

    // ================================================
    // VALIDACIONES PARA RESPONDER SOLICITUDES
    // ================================================

    /**
     * Valida la respuesta a una solicitud de amistad.
     */
    public ValidationResult validateRespondToFriendRequest(
            String currentUsername,
            String requestId,
            FriendRequestAction action) {

        ValidationResult result = new ValidationResult();

        // 1. Validar que el usuario actual existe
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Validar formato del requestId
        UUID requestUUID;
        try {
            requestUUID = UUID.fromString(requestId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID de solicitud inválido");
        }

        // 3. Buscar la solicitud
        FriendRequest friendRequest = friendRequestRepository.findById(requestUUID)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        // 4. Validar que la solicitud es para el usuario actual (es el receiver)
        if (!friendRequest.getReceiver().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("No puedes responder a esta solicitud");
        }

        // 5. Validar que la solicitud está pendiente
        if (!friendRequest.isPending()) {
            throw new IllegalArgumentException("Esta solicitud ya fue respondida");
        }

        // 6. Validar que no son ya amigos
        boolean areAlreadyFriends = friendshipRepository.areUsersFriends(
                currentUser.getId(),
                friendRequest.getSender().getId()
        );
        if (areAlreadyFriends) {
            throw new IllegalStateException("Error de consistencia: Los usuarios ya son amigos pero existe una solicitud pendiente");
        }

        result.setReceiver(currentUser);
        result.setSender(friendRequest.getSender());
        result.setFriendRequest(friendRequest);

        return result;
    }

    /**
     * Valida que se puede marcar una solicitud como notificada.
     */
    public ValidationResult validateMarkAsNotified(String senderUsername, String requestId) {
        ValidationResult result = new ValidationResult();

        // 1. Validar que el usuario existe
        User sender = validateUserExists(senderUsername);

        // 2. Validar formato del requestId
        UUID requestUUID;
        try {
            requestUUID = UUID.fromString(requestId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID de solicitud inválido");
        }

        // 3. Buscar la solicitud
        FriendRequest friendRequest = friendRequestRepository.findById(requestUUID)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        // 4. Validar que es el sender de la solicitud
        if (!friendRequest.getSender().getId().equals(sender.getId())) {
            throw new IllegalArgumentException("Solo puedes marcar tus propias solicitudes como notificadas");
        }

        // 5. Validar que la solicitud está resuelta (no PENDING)
        if (friendRequest.isPending()) {
            throw new IllegalArgumentException("No se puede marcar como notificada una solicitud pendiente");
        }

        // 6. Validar que no está ya notificada
        if (friendRequest.getIsSenderNotified()) {
            throw new IllegalArgumentException("Esta solicitud ya está marcada como notificada");
        }

        result.setSender(sender);
        result.setFriendRequest(friendRequest);

        return result;
    }

    // ================================================
    // VALIDACIONES PARA ELIMINAR AMIGOS
    // ================================================

    /**
     * Valida que se puede eliminar una amistad.
     */
    public ValidationResult validateDeleteFriend(String username, String friendUsername) {
        User user = validateUserExists(username);
        User friend = validateUserExists(friendUsername);

        // Verificar que son amigos
        if (!friendshipRepository.areUsersFriends(user.getId(), friend.getId())) {
            throw new IllegalArgumentException("No sois amigos");
        }

        return new ValidationResult(user, friend, null);
    }

    // ================================================
    // VALIDACIONES PARA BÚSQUEDAS
    // ================================================

    /**
     * Valida parámetros de búsqueda de usuarios.
     */
    public void validateUserSearch(String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            throw new IllegalArgumentException("La búsqueda debe tener al menos 2 caracteres");
        }

        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException("El límite debe estar entre 1 y 50");
        }
    }

    /**
     * Valida parámetros de búsqueda por juego.
     */
    public void validateGameBasedSearch(String gameSlug, int userRating, int maxResults) {
        if (gameSlug == null || gameSlug.trim().isEmpty()) {
            throw new IllegalArgumentException("El slug del juego es obligatorio");
        }

        if (userRating < 1 || userRating > 10) {
            throw new IllegalArgumentException("El rating debe estar entre 1 y 10");
        }

        if (maxResults < 1 || maxResults > 20) {
            throw new IllegalArgumentException("Los resultados máximos deben estar entre 1 y 20");
        }

        // Verificar que el juego existe
        gameRepository.findBySlug(gameSlug)
                .orElseThrow(() -> new IllegalArgumentException("Juego no encontrado: " + gameSlug));
    }

    // ================================================
    // CLASE RESULTADO DE VALIDACIÓN
    // ================================================

    /**
     * Resultado de una validación exitosa con datos necesarios.
     */
    public static class ValidationResult {
        private User sender;
        private User receiver;
        private Game suggestedGame;
        private FriendRequest friendRequest; // ✅ NUEVO CAMPO

        // Constructor original (mantener compatibilidad)
        public ValidationResult(User sender, User receiver, Game suggestedGame) {
            this.sender = sender;
            this.receiver = receiver;
            this.suggestedGame = suggestedGame;
        }

        // ✅ NUEVO: Constructor vacío
        public ValidationResult() {}

        // Getters existentes
        public User getSender() { return sender; }
        public User getReceiver() { return receiver; }
        public Game getSuggestedGame() { return suggestedGame; }

        // ✅ NUEVOS: Setters
        public void setSender(User sender) { this.sender = sender; }
        public void setReceiver(User receiver) { this.receiver = receiver; }
        public void setSuggestedGame(Game suggestedGame) { this.suggestedGame = suggestedGame; }

        // ✅ NUEVO: Getter/Setter para FriendRequest
        public FriendRequest getFriendRequest() { return friendRequest; }
        public void setFriendRequest(FriendRequest friendRequest) { this.friendRequest = friendRequest; }
    }

    // ================================================
    // VALIDACIONES AUXILIARES
    // ================================================

    /**
     * Valida que un UUID es válido.
     */
    public void validateUUID(String uuidString, String fieldName) {
        try {
            UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " no es un UUID válido");
        }
    }

    /**
     * Valida que un username tiene formato válido.
     */
    public void validateUsernameFormat(String username) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("El username debe tener al menos 3 caracteres");
        }

        if (username.length() > 20) {
            throw new IllegalArgumentException("El username no puede tener más de 20 caracteres");
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("El username solo puede contener letras, números y guiones bajos");
        }
    }
}