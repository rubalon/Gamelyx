package com.gamelyx.dto;

import com.gamelyx.entity.FriendRequest;

/**
 * DTOs para requests del sistema social.
 * Utilizan records para eliminar boilerplate y mejorar inmutabilidad.
 */
public class SocialRequestDtos {

    // ================================================
    // REQUEST PARA ENVÍO DE SOLICITUDES DE AMISTAD
    // ================================================

    /**
     * Request para enviar solicitud de amistad
     *
     * @param targetUsername Usuario al que se envía la solicitud
     * @param source Origen de la solicitud (SEARCH o SUGGESTION)
     * @param gameSlug Slug del juego sugerido (opcional, solo para SUGGESTION)
     */
    public record SendFriendRequestDto(
            String targetUsername,
            FriendRequest.RequestSource source,
            String gameSlug
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public SendFriendRequestDto {
            if (targetUsername == null || targetUsername.trim().isEmpty()) {
                throw new IllegalArgumentException("Target username cannot be null or empty");
            }
            if (source == null) {
                throw new IllegalArgumentException("Request source cannot be null");
            }
            if (source == FriendRequest.RequestSource.SUGGESTION &&
                    (gameSlug == null || gameSlug.trim().isEmpty())) {
                throw new IllegalArgumentException("Game slug is required for suggestion-based requests");
            }

            // Trim whitespace
            targetUsername = targetUsername.trim();
            gameSlug = gameSlug != null ? gameSlug.trim() : null;
        }

        /**
         * Factory method para solicitud desde búsqueda manual
         */
        public static SendFriendRequestDto fromSearch(String targetUsername) {
            return new SendFriendRequestDto(targetUsername, FriendRequest.RequestSource.SEARCH, null);
        }

        /**
         * Factory method para solicitud desde sugerencia
         */
        public static SendFriendRequestDto fromSuggestion(String targetUsername, String gameSlug) {
            return new SendFriendRequestDto(targetUsername, FriendRequest.RequestSource.SUGGESTION, gameSlug);
        }

        /**
         * Verifica si es una solicitud por sugerencia
         */
        public boolean isFromSuggestion() {
            return source == FriendRequest.RequestSource.SUGGESTION;
        }
    }

    // ================================================
    // REQUEST PARA RESPONDER SOLICITUDES
    // ================================================

    /**
     * Request para aceptar o rechazar una solicitud de amistad
     *
     * @param action Acción a realizar (ACCEPT o REJECT)
     */
    public record RespondToRequestDto(
            FriendRequestAction action
    ) {
        /**
         * Constructor compacto con validación
         */
        public RespondToRequestDto {
            if (action == null) {
                throw new IllegalArgumentException("Action cannot be null");
            }
        }

        /**
         * Factory method para aceptar solicitud
         */
        public static RespondToRequestDto accept() {
            return new RespondToRequestDto(FriendRequestAction.ACCEPT);
        }

        /**
         * Factory method para rechazar solicitud
         */
        public static RespondToRequestDto reject() {
            return new RespondToRequestDto(FriendRequestAction.REJECT);
        }

        /**
         * Verifica si la acción es aceptar
         */
        public boolean isAccept() {
            return action == FriendRequestAction.ACCEPT;
        }

        /**
         * Verifica si la acción es rechazar
         */
        public boolean isReject() {
            return action == FriendRequestAction.REJECT;
        }
    }

    // ================================================
    // REQUEST PARA BÚSQUEDAS
    // ================================================

    /**
     * Parámetros para búsqueda de usuarios por nombre
     *
     * @param query Texto a buscar en usernames
     * @param limit Máximo número de resultados
     */
    public record UserSearchRequestDto(
            String query,
            int limit
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public UserSearchRequestDto {
            if (query == null || query.trim().length() < 2) {
                throw new IllegalArgumentException("Query must have at least 2 characters");
            }
            if (limit <= 0 || limit > 50) {
                throw new IllegalArgumentException("Limit must be between 1 and 50");
            }

            // Trim y lowercase para búsqueda case-insensitive
            query = query.trim().toLowerCase();
        }

        /**
         * Factory method con límite por defecto
         */
        public static UserSearchRequestDto of(String query) {
            return new UserSearchRequestDto(query, 10);
        }
    }

    /**
     * Parámetros para búsqueda de usuarios por juego en común
     *
     * @param gameSlug Slug del juego para buscar coincidencias
     * @param userRating Rating del usuario actual para ese juego
     * @param maxResults Máximo número de sugerencias
     * @param ratingTolerance Tolerancia en diferencia de ratings (±)
     */
    public record GameBasedSearchRequestDto(
            String gameSlug,
            int userRating,
            int maxResults,
            int ratingTolerance
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public GameBasedSearchRequestDto {
            if (gameSlug == null || gameSlug.trim().isEmpty()) {
                throw new IllegalArgumentException("Game slug cannot be null or empty");
            }
            if (userRating < 1 || userRating > 10) {
                throw new IllegalArgumentException("User rating must be between 1 and 10");
            }
            if (maxResults <= 0 || maxResults > 20) {
                throw new IllegalArgumentException("Max results must be between 1 and 20");
            }
            if (ratingTolerance < 0 || ratingTolerance > 5) {
                throw new IllegalArgumentException("Rating tolerance must be between 0 and 5");
            }

            gameSlug = gameSlug.trim();
        }

        /**
         * Factory method con valores por defecto
         */
        public static GameBasedSearchRequestDto of(String gameSlug, int userRating) {
            return new GameBasedSearchRequestDto(gameSlug, userRating, 5, 2);
        }

        /**
         * Calcula el rating mínimo aceptable
         */
        public int getMinAcceptableRating() {
            return Math.max(1, userRating - ratingTolerance);
        }

        /**
         * Calcula el rating máximo aceptable
         */
        public int getMaxAcceptableRating() {
            return Math.min(10, userRating + ratingTolerance);
        }
    }

    // ================================================
    // REQUEST PARA SUGERENCIAS AUTOMÁTICAS
    // ================================================

    /**
     * Parámetros para obtener sugerencias automáticas de amigos
     *
     * @param limit Máximo número de sugerencias
     * @param minCommonGames Mínimo número de juegos en común requerido
     * @param minRatingThreshold Rating mínimo para considerar un juego como "preferido"
     */
    public record FriendSuggestionsRequestDto(
            int limit,
            int minCommonGames,
            int minRatingThreshold
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public FriendSuggestionsRequestDto {
            if (limit <= 0 || limit > 50) {
                throw new IllegalArgumentException("Limit must be between 1 and 50");
            }
            if (minCommonGames < 1) {
                throw new IllegalArgumentException("Min common games must be at least 1");
            }
            if (minRatingThreshold < 5 || minRatingThreshold > 10) {
                throw new IllegalArgumentException("Min rating threshold must be between 5 and 10");
            }
        }

        /**
         * Factory method con valores por defecto optimizados
         */
        public static FriendSuggestionsRequestDto defaultParams() {
            return new FriendSuggestionsRequestDto(10, 2, 7);
        }

        /**
         * Factory method para sugerencias estrictas (usuarios muy compatibles)
         */
        public static FriendSuggestionsRequestDto strict() {
            return new FriendSuggestionsRequestDto(5, 3, 8);
        }

        /**
         * Factory method para sugerencias amplias (más usuarios)
         */
        public static FriendSuggestionsRequestDto relaxed() {
            return new FriendSuggestionsRequestDto(15, 1, 6);
        }
    }

    // ================================================
    // REQUEST PARA PARÁMETROS DE CONSULTA
    // ================================================

    /**
     * Parámetros generales para paginación y filtrado
     *
     * @param page Número de página (0-based)
     * @param size Tamaño de página
     * @param sortBy Campo por el que ordenar
     * @param sortDirection Dirección del ordenamiento (ASC/DESC)
     */
    public record PaginationRequestDto(
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        /**
         * Constructor compacto con validaciones
         */
        public PaginationRequestDto {
            if (page < 0) {
                throw new IllegalArgumentException("Page must be non-negative");
            }
            if (size <= 0 || size > 100) {
                throw new IllegalArgumentException("Size must be between 1 and 100");
            }
            if (sortBy == null || sortBy.trim().isEmpty()) {
                sortBy = "createdAt";
            }
            if (sortDirection == null || (!sortDirection.equalsIgnoreCase("ASC") && !sortDirection.equalsIgnoreCase("DESC"))) {
                sortDirection = "DESC";
            }

            sortBy = sortBy.trim();
            sortDirection = sortDirection.toUpperCase();
        }

        /**
         * Factory method con valores por defecto
         */
        public static PaginationRequestDto defaultPagination() {
            return new PaginationRequestDto(0, 20, "createdAt", "DESC");
        }

        /**
         * Calcula el offset para consultas SQL
         */
        public int getOffset() {
            return page * size;
        }

        /**
         * Verifica si el ordenamiento es ascendente
         */
        public boolean isAscending() {
            return "ASC".equals(sortDirection);
        }
    }

    // ================================================
    // ENUMS PARA REQUESTS
    // ================================================

    /**
     * Acciones posibles para responder a una solicitud de amistad
     */
    public enum FriendRequestAction {
        ACCEPT("Aceptar"),
        REJECT("Rechazar");

        private final String displayName;

        FriendRequestAction(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Verifica si la acción es positiva (acepta la solicitud)
         */
        public boolean isPositive() {
            return this == ACCEPT;
        }
    }

    /**
     * Criterios de ordenamiento para listas de amigos
     */
    public enum FriendSortCriteria {
        USERNAME_ASC("username", "ASC"),
        USERNAME_DESC("username", "DESC"),
        FRIENDSHIP_DATE_ASC("createdAt", "ASC"),
        FRIENDSHIP_DATE_DESC("createdAt", "DESC"),
        LAST_ACTIVITY_ASC("lastSeen", "ASC"),
        LAST_ACTIVITY_DESC("lastSeen", "DESC");

        private final String field;
        private final String direction;

        FriendSortCriteria(String field, String direction) {
            this.field = field;
            this.direction = direction;
        }

        public String getField() {
            return field;
        }

        public String getDirection() {
            return direction;
        }
    }
}