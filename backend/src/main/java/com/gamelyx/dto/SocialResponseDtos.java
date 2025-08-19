package com.gamelyx.dto;

import com.gamelyx.entity.FriendRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs para responses del sistema social.
 * Utilizan records para eliminar boilerplate y mejorar inmutabilidad.
 */
public class SocialResponseDtos {

    // ================================================
    // RESPONSE PRINCIPAL PARA HOME SOCIAL
    // ================================================

    /**
     * Respuesta principal del endpoint /api/social/home-data
     * Contiene toda la información social necesaria para el home
     *
     * @param friends Lista de amigos del usuario
     * @param incomingRequests Solicitudes de amistad recibidas
     * @param outgoingRequests Solicitudes de amistad enviadas
     * @param preferredGames Juegos preferidos del usuario (rating >= 7)
     */
    public record HomeSocialDataDto(
            List<FriendDto> friends,
            IncomingRequestsDto incomingRequests,
            List<OutgoingRequestDto> outgoingRequests,
            List<PreferredGameDto> preferredGames
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public HomeSocialDataDto {
            friends = friends != null ? List.copyOf(friends) : List.of();
            outgoingRequests = outgoingRequests != null ? List.copyOf(outgoingRequests) : List.of();
            preferredGames = preferredGames != null ? List.copyOf(preferredGames) : List.of();

            if (incomingRequests == null) {
                incomingRequests = new IncomingRequestsDto(0, List.of());
            }
        }

        /**
         * Factory method para usuario sin actividad social
         */
        public static HomeSocialDataDto empty() {
            return new HomeSocialDataDto(
                    List.of(),
                    new IncomingRequestsDto(0, List.of()),
                    List.of(),
                    List.of()
            );
        }

        /**
         * Obtiene el número total de amigos
         */
        public int getFriendsCount() {
            return friends.size();
        }

        /**
         * Obtiene el número total de juegos preferidos
         */
        public int getPreferredGamesCount() {
            return preferredGames.size();
        }

        /**
         * Verifica si el usuario tiene actividad social
         */
        public boolean hasAnyActivity() {
            return !friends.isEmpty() ||
                    incomingRequests.count() > 0 ||
                    !outgoingRequests.isEmpty() ||
                    !preferredGames.isEmpty();
        }
    }

    // ================================================
    // DTOs PARA AMIGOS
    // ================================================

    /**
     * Información básica de un amigo para mostrar en la lista
     *
     * @param userId ID único del amigo
     * @param username Nombre de usuario
     * @param chatId ID del chat (para funcionalidad futura, puede ser null)
     * @param friendsSince Fecha desde que son amigos
     */
    public record FriendDto(
            UUID userId,
            String username,
            UUID chatId,
            LocalDateTime friendsSince
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public FriendDto {
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty");
            }
            if (friendsSince == null) {
                throw new IllegalArgumentException("Friends since date cannot be null");
            }

            username = username.trim();
        }

        /**
         * Factory method para amigo sin chat
         */
        public static FriendDto withoutChat(UUID userId, String username, LocalDateTime friendsSince) {
            return new FriendDto(userId, username, null, friendsSince);
        }

        /**
         * Verifica si tiene chat disponible
         */
        public boolean hasChatAvailable() {
            return chatId != null;
        }
    }

    // ================================================
    // DTOs PARA SOLICITUDES ENTRANTES
    // ================================================

    /**
     * Contenedor para solicitudes entrantes con contador
     *
     * @param count Número total de solicitudes pendientes
     * @param requests Lista de solicitudes pendientes
     */
    public record IncomingRequestsDto(
            int count,
            List<IncomingRequestDto> requests
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public IncomingRequestsDto {
            if (count < 0) {
                throw new IllegalArgumentException("Count cannot be negative");
            }
            requests = requests != null ? List.copyOf(requests) : List.of();

            // Validar consistencia entre count y size de la lista
            if (count != requests.size()) {
                throw new IllegalArgumentException("Count must match requests list size");
            }
        }

        /**
         * Factory method para sin solicitudes
         */
        public static IncomingRequestsDto empty() {
            return new IncomingRequestsDto(0, List.of());
        }

        /**
         * Verifica si hay solicitudes pendientes
         */
        public boolean hasRequests() {
            return count > 0;
        }
    }

    /**
     * Solicitud de amistad entrante individual
     *
     * @param requestId ID único de la solicitud
     * @param senderUsername Username del remitente
     * @param senderUserId ID del remitente
     * @param source Origen de la solicitud (SEARCH o SUGGESTION)
     * @param suggestedGame Información del juego sugerido (solo si source = SUGGESTION)
     * @param receivedAt Fecha de recepción
     */
    public record IncomingRequestDto(
            UUID requestId,
            String senderUsername,
            UUID senderUserId,
            FriendRequest.RequestSource source,
            SuggestedGameInfoDto suggestedGame,
            LocalDateTime receivedAt
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public IncomingRequestDto {
            if (requestId == null) {
                throw new IllegalArgumentException("Request ID cannot be null");
            }
            if (senderUsername == null || senderUsername.trim().isEmpty()) {
                throw new IllegalArgumentException("Sender username cannot be null or empty");
            }
            if (senderUserId == null) {
                throw new IllegalArgumentException("Sender user ID cannot be null");
            }
            if (source == null) {
                throw new IllegalArgumentException("Source cannot be null");
            }
            if (receivedAt == null) {
                throw new IllegalArgumentException("Received at cannot be null");
            }

            // Validar que si es SUGGESTION, debe tener suggestedGame
            if (source == FriendRequest.RequestSource.SUGGESTION && suggestedGame == null) {
                throw new IllegalArgumentException("Suggested game is required for SUGGESTION source");
            }

            senderUsername = senderUsername.trim();
        }

        /**
         * Factory method para solicitud desde búsqueda
         */
        public static IncomingRequestDto fromSearch(
                UUID requestId,
                String senderUsername,
                UUID senderUserId,
                LocalDateTime receivedAt) {
            return new IncomingRequestDto(
                    requestId,
                    senderUsername,
                    senderUserId,
                    FriendRequest.RequestSource.SEARCH,
                    null,
                    receivedAt
            );
        }

        /**
         * Verifica si es una solicitud por sugerencia
         */
        public boolean isFromSuggestion() {
            return source == FriendRequest.RequestSource.SUGGESTION;
        }
    }

    /**
     * Información del juego sugerido (para solicitudes por sugerencia)
     *
     * @param gameSlug Slug del juego
     * @param gameName Nombre del juego
     * @param yourRating Rating del receptor del request
     * @param theirRating Rating del emisor del request
     */
    public record SuggestedGameInfoDto(
            String gameSlug,
            String gameName,
            int yourRating,
            int theirRating
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public SuggestedGameInfoDto {
            if (gameSlug == null || gameSlug.trim().isEmpty()) {
                throw new IllegalArgumentException("Game slug cannot be null or empty");
            }
            if (gameName == null || gameName.trim().isEmpty()) {
                throw new IllegalArgumentException("Game name cannot be null or empty");
            }
            if (yourRating < 1 || yourRating > 10) {
                throw new IllegalArgumentException("Your rating must be between 1 and 10");
            }
            if (theirRating < 1 || theirRating > 10) {
                throw new IllegalArgumentException("Their rating must be between 1 and 10");
            }

            gameSlug = gameSlug.trim();
            gameName = gameName.trim();
        }

        /**
         * Calcula la diferencia absoluta entre ratings
         */
        public int getRatingDifference() {
            return Math.abs(yourRating - theirRating);
        }

        /**
         * Verifica si los ratings son muy similares (diferencia <= 1)
         */
        public boolean hasVeryCloseRatings() {
            return getRatingDifference() <= 1;
        }

        /**
         * Obtiene el rating promedio
         */
        public double getAverageRating() {
            return (yourRating + theirRating) / 2.0;
        }
    }

    // ================================================
    // DTOs PARA SOLICITUDES SALIENTES
    // ================================================

    /**
     * Solicitud de amistad saliente (enviada por el usuario actual)
     *
     * @param requestId ID de la solicitud
     * @param receiverUsername Username del receptor
     * @param receiverUserId ID del receptor
     * @param source Origen de la solicitud
     * @param gameSlug Slug del juego (solo si source = SUGGESTION)
     * @param sentAt Fecha de envío
     */
    public record OutgoingRequestDto(
            UUID requestId,
            String receiverUsername,
            UUID receiverUserId,
            FriendRequest.RequestSource source,
            String gameSlug,
            LocalDateTime sentAt
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public OutgoingRequestDto {
            if (requestId == null) {
                throw new IllegalArgumentException("Request ID cannot be null");
            }
            if (receiverUsername == null || receiverUsername.trim().isEmpty()) {
                throw new IllegalArgumentException("Receiver username cannot be null or empty");
            }
            if (receiverUserId == null) {
                throw new IllegalArgumentException("Receiver user ID cannot be null");
            }
            if (source == null) {
                throw new IllegalArgumentException("Source cannot be null");
            }
            if (sentAt == null) {
                throw new IllegalArgumentException("Sent at cannot be null");
            }

            receiverUsername = receiverUsername.trim();
            gameSlug = gameSlug != null ? gameSlug.trim() : null;
        }

        /**
         * Verifica si es una solicitud por sugerencia
         */
        public boolean isFromSuggestion() {
            return source == FriendRequest.RequestSource.SUGGESTION;
        }
    }

    // ================================================
    // DTOs PARA JUEGOS PREFERIDOS
    // ================================================

    /**
     * Juego preferido del usuario (rating >= 7)
     *
     * @param gameSlug Slug del juego
     * @param gameName Nombre del juego
     * @param userRating Rating del usuario
     */
    public record PreferredGameDto(
            String gameSlug,
            String gameName,
            int userRating
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public PreferredGameDto {
            if (gameSlug == null || gameSlug.trim().isEmpty()) {
                throw new IllegalArgumentException("Game slug cannot be null or empty");
            }
            if (gameName == null || gameName.trim().isEmpty()) {
                throw new IllegalArgumentException("Game name cannot be null or empty");
            }
            if (userRating < 1 || userRating > 10) {
                throw new IllegalArgumentException("User rating must be between 1 and 10");
            }

            gameSlug = gameSlug.trim();
            gameName = gameName.trim();
        }

        /**
         * Verifica si es un juego muy bien valorado (rating >= 9)
         */
        public boolean isHighlyRated() {
            return userRating >= 9;
        }

        /**
         * Verifica si es un juego preferido estándar (rating >= 7)
         */
        public boolean isPreferred() {
            return userRating >= 7;
        }
    }

    // ================================================
    // DTOs PARA RESPUESTAS DE OPERACIONES
    // ================================================

    /**
     * Response del envío de solicitud de amistad
     *
     * @param success Si la operación fue exitosa
     * @param message Mensaje descriptivo
     * @param requestId ID de la solicitud creada (solo si success = true)
     */
    public record SendFriendRequestResponseDto(
            boolean success,
            String message,
            UUID requestId
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public SendFriendRequestResponseDto {
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Message cannot be null or empty");
            }

            message = message.trim();
        }

        /**
         * Factory method para éxito
         */
        public static SendFriendRequestResponseDto success(String message, UUID requestId) {
            return new SendFriendRequestResponseDto(true, message, requestId);
        }

        /**
         * Factory method para error
         */
        public static SendFriendRequestResponseDto error(String message) {
            return new SendFriendRequestResponseDto(false, message, null);
        }
    }

    /**
     * Response de aceptar/rechazar solicitud
     *
     * @param success Si la operación fue exitosa
     * @param message Mensaje descriptivo
     * @param action Acción realizada
     * @param newFriend Información del nuevo amigo (solo si action = ACCEPT y success = true)
     */
    public record FriendRequestResponseDto(
            boolean success,
            String message,
            SocialRequestDtos.FriendRequestAction action,
            FriendDto newFriend
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public FriendRequestResponseDto {
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Message cannot be null or empty");
            }
            if (action == null) {
                throw new IllegalArgumentException("Action cannot be null");
            }

            message = message.trim();
        }

        /**
         * Factory method para aceptación exitosa
         */
        public static FriendRequestResponseDto accepted(String message, FriendDto newFriend) {
            return new FriendRequestResponseDto(true, message, SocialRequestDtos.FriendRequestAction.ACCEPT, newFriend);
        }

        /**
         * Factory method para rechazo exitoso
         */
        public static FriendRequestResponseDto rejected(String message) {
            return new FriendRequestResponseDto(true, message, SocialRequestDtos.FriendRequestAction.REJECT, null);
        }

        /**
         * Factory method para error
         */
        public static FriendRequestResponseDto error(String message, SocialRequestDtos.FriendRequestAction action) {
            return new FriendRequestResponseDto(false, message, action, null);
        }
    }

    /**
     * Response para eliminar amigo
     *
     * @param success Si la operación fue exitosa
     * @param message Mensaje descriptivo
     * @param deletedFriendUsername Username del amigo eliminado
     */
    public record DeleteFriendResponseDto(
            boolean success,
            String message,
            String deletedFriendUsername
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public DeleteFriendResponseDto {
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Message cannot be null or empty");
            }

            message = message.trim();
            deletedFriendUsername = deletedFriendUsername != null ? deletedFriendUsername.trim() : null;
        }

        /**
         * Factory method para éxito
         */
        public static DeleteFriendResponseDto success(String message, String deletedFriendUsername) {
            return new DeleteFriendResponseDto(true, message, deletedFriendUsername);
        }

        /**
         * Factory method para error
         */
        public static DeleteFriendResponseDto error(String message) {
            return new DeleteFriendResponseDto(false, message, null);
        }
    }

    // ================================================
    // DTOs PARA BÚSQUEDAS
    // ================================================

    /**
     * Resultado de búsqueda de usuarios por nombre
     *
     * @param query Query de búsqueda utilizada
     * @param users Lista de usuarios encontrados
     * @param totalFound Número total de usuarios encontrados
     */
    public record UserSearchResultDto(
            String query,
            List<SearchedUserDto> users,
            int totalFound
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public UserSearchResultDto {
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("Query cannot be null or empty");
            }
            if (totalFound < 0) {
                throw new IllegalArgumentException("Total found cannot be negative");
            }

            users = users != null ? List.copyOf(users) : List.of();
            query = query.trim();
        }

        /**
         * Factory method para búsqueda sin resultados
         */
        public static UserSearchResultDto empty(String query) {
            return new UserSearchResultDto(query, List.of(), 0);
        }

        /**
         * Verifica si se encontraron resultados
         */
        public boolean hasResults() {
            return totalFound > 0;
        }
    }

    /**
     * Usuario encontrado en búsqueda
     *
     * @param userId ID del usuario
     * @param username Username del usuario
     * @param canSendRequest Si se puede enviar solicitud de amistad
     */
    public record SearchedUserDto(
            UUID userId,
            String username,
            boolean canSendRequest
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public SearchedUserDto {
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty");
            }

            username = username.trim();
        }
    }

    // ================================================
    // DTOs PARA SUGERENCIAS POR JUEGO
    // ================================================

    /**
     * Sugerencias basadas en juego específico
     *
     * @param gameSlug Slug del juego utilizado para la búsqueda
     * @param userRating Rating del usuario actual para ese juego
     * @param suggestedUsers Lista de usuarios sugeridos
     */
    public record GameBasedSuggestionDto(
            String gameSlug,
            int userRating,
            List<SuggestedUserDto> suggestedUsers
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public GameBasedSuggestionDto {
            if (gameSlug == null || gameSlug.trim().isEmpty()) {
                throw new IllegalArgumentException("Game slug cannot be null or empty");
            }
            if (userRating < 1 || userRating > 10) {
                throw new IllegalArgumentException("User rating must be between 1 and 10");
            }

            suggestedUsers = suggestedUsers != null ? List.copyOf(suggestedUsers) : List.of();
            gameSlug = gameSlug.trim();
        }

        /**
         * Factory method para sin sugerencias
         */
        public static GameBasedSuggestionDto empty(String gameSlug, int userRating) {
            return new GameBasedSuggestionDto(gameSlug, userRating, List.of());
        }

        /**
         * Obtiene el número de usuarios sugeridos
         */
        public int getSuggestionsCount() {
            return suggestedUsers.size();
        }

        /**
         * Verifica si hay sugerencias disponibles
         */
        public boolean hasSuggestions() {
            return !suggestedUsers.isEmpty();
        }
    }

    /**
     * Usuario sugerido basado en juego
     *
     * @param userId ID del usuario sugerido
     * @param username Username del usuario sugerido
     * @param gameSlug Slug del juego en común
     * @param yourRating Rating del usuario actual
     * @param theirRating Rating del usuario sugerido
     */
    public record SuggestedUserDto(
            UUID userId,
            String username,
            String gameSlug,
            int yourRating,
            int theirRating
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public SuggestedUserDto {
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty");
            }
            if (gameSlug == null || gameSlug.trim().isEmpty()) {
                throw new IllegalArgumentException("Game slug cannot be null or empty");
            }
            if (yourRating < 1 || yourRating > 10) {
                throw new IllegalArgumentException("Your rating must be between 1 and 10");
            }
            if (theirRating < 1 || theirRating > 10) {
                throw new IllegalArgumentException("Their rating must be between 1 and 10");
            }

            username = username.trim();
            gameSlug = gameSlug.trim();
        }

        /**
         * Calcula la diferencia absoluta entre ratings
         */
        public int getRatingDifference() {
            return Math.abs(yourRating - theirRating);
        }

        /**
         * Verifica si tienen ratings muy similares (diferencia <= 1)
         */
        public boolean hasVeryCloseRatings() {
            return getRatingDifference() <= 1;
        }

        /**
         * Obtiene el rating promedio entre ambos usuarios
         */
        public double getAverageRating() {
            return (yourRating + theirRating) / 2.0;
        }

        /**
         * Calcula un score de compatibilidad (10 - diferencia de rating)
         */
        public int getCompatibilityScore() {
            return Math.max(0, 10 - getRatingDifference());
        }
    }

    // ================================================
    // DTOs PARA SUGERENCIAS AUTOMÁTICAS (HU-20)
    // ================================================

    /**
     * Sugerencias automáticas de amigos
     *
     * @param suggestions Lista de usuarios sugeridos con información de compatibilidad
     * @param totalSuggestions Número total de sugerencias disponibles
     */
    public record FriendSuggestionsDto(
            List<SuggestedUserDto> suggestions,
            int totalSuggestions
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public FriendSuggestionsDto {
            if (totalSuggestions < 0) {
                throw new IllegalArgumentException("Total suggestions cannot be negative");
            }

            suggestions = suggestions != null ? List.copyOf(suggestions) : List.of();
        }

        /**
         * Factory method para sin sugerencias
         */
        public static FriendSuggestionsDto empty() {
            return new FriendSuggestionsDto(List.of(), 0);
        }

        /**
         * Verifica si hay sugerencias disponibles
         */
        public boolean hasSuggestions() {
            return totalSuggestions > 0;
        }

        /**
         * Obtiene el número de sugerencias mostradas (puede ser menor que el total)
         */
        public int getDisplayedCount() {
            return suggestions.size();
        }

        /**
         * Verifica si hay más sugerencias disponibles
         */
        public boolean hasMoreSuggestions() {
            return totalSuggestions > suggestions.size();
        }
    }

    // ================================================
    // DTOs AUXILIARES Y ESTADÍSTICAS
    // ================================================

    /**
     * Estadísticas sociales del usuario
     *
     * @param friendsCount Número total de amigos
     * @param pendingRequestsReceived Solicitudes pendientes recibidas
     * @param pendingRequestsSent Solicitudes pendientes enviadas
     * @param totalRequestsSent Total de solicitudes enviadas (históricas)
     * @param totalRequestsReceived Total de solicitudes recibidas (históricas)
     */
    public record UserSocialStatsDto(
            int friendsCount,
            int pendingRequestsReceived,
            int pendingRequestsSent,
            int totalRequestsSent,
            int totalRequestsReceived
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public UserSocialStatsDto {
            if (friendsCount < 0) {
                throw new IllegalArgumentException("Friends count cannot be negative");
            }
            if (pendingRequestsReceived < 0) {
                throw new IllegalArgumentException("Pending requests received cannot be negative");
            }
            if (pendingRequestsSent < 0) {
                throw new IllegalArgumentException("Pending requests sent cannot be negative");
            }
            if (totalRequestsSent < 0) {
                throw new IllegalArgumentException("Total requests sent cannot be negative");
            }
            if (totalRequestsReceived < 0) {
                throw new IllegalArgumentException("Total requests received cannot be negative");
            }
        }

        /**
         * Factory method para usuario nuevo sin actividad
         */
        public static UserSocialStatsDto newUser() {
            return new UserSocialStatsDto(0, 0, 0, 0, 0);
        }

        /**
         * Calcula el total de actividad pendiente
         */
        public int getTotalPendingActivity() {
            return pendingRequestsReceived + pendingRequestsSent;
        }

        /**
         * Calcula el total de actividad histórica
         */
        public int getTotalHistoricalActivity() {
            return totalRequestsSent + totalRequestsReceived;
        }

        /**
         * Verifica si el usuario es socialmente activo
         */
        public boolean isSociallyActive() {
            return friendsCount > 0 || getTotalPendingActivity() > 0;
        }

        /**
         * Calcula la tasa de aceptación de solicitudes enviadas (0-1)
         */
        public double getAcceptanceRate() {
            if (totalRequestsSent == 0) return 0.0;
            // Asumiendo que friendsCount representa solicitudes aceptadas exitosas
            return Math.min(1.0, (double) friendsCount / totalRequestsSent);
        }
    }

    /**
     * Estado de amistad entre dos usuarios
     *
     * @param status Estado de la relación
     * @param username Username del otro usuario
     */
    public record FriendshipStatusDto(
            FriendshipStatus status,
            String username
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public FriendshipStatusDto {
            if (status == null) {
                throw new IllegalArgumentException("Status cannot be null");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty");
            }

            username = username.trim();
        }

        /**
         * Verifica si son amigos
         */
        public boolean areFriends() {
            return status == FriendshipStatus.FRIENDS;
        }

        /**
         * Verifica si hay una solicitud pendiente
         */
        public boolean hasPendingRequest() {
            return status == FriendshipStatus.REQUEST_SENT ||
                    status == FriendshipStatus.REQUEST_RECEIVED;
        }
    }

    // ================================================
    // ENUMS PARA RESPONSES
    // ================================================

    /**
     * Estados de la relación entre dos usuarios
     */
    public enum FriendshipStatus {
        FRIENDS("Amigos"),
        REQUEST_SENT("Solicitud enviada"),
        REQUEST_RECEIVED("Solicitud recibida"),
        NOT_FRIENDS("Sin relación"),
        BLOCKED("Bloqueado");

        private final String displayName;

        FriendshipStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Verifica si representa una relación activa
         */
        public boolean isActiveRelationship() {
            return this == FRIENDS || this == REQUEST_SENT || this == REQUEST_RECEIVED;
        }

        /**
         * Verifica si el usuario puede enviar una solicitud
         */
        public boolean canSendRequest() {
            return this == NOT_FRIENDS;
        }
    }

    /**
     * Tipos de respuesta para operaciones
     */
    public enum OperationResult {
        SUCCESS("Operación exitosa"),
        ERROR("Error en la operación"),
        WARNING("Advertencia"),
        INFO("Información");

        private final String displayName;

        OperationResult(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isSuccessful() {
            return this == SUCCESS;
        }
    }
}