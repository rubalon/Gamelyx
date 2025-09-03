package com.gamelyx.dto;

import com.gamelyx.entity.FriendRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs para responses del sistema social.
 * VERSIÓN PURA: Solo datos, sin validaciones ni factory methods.
 * Las validaciones se manejan en el Service layer.
 */
public class SocialResponseDtos {

    // ================================================
    // RESPONSE PRINCIPAL PARA HOME SOCIAL
    // ================================================

    /**
     * Respuesta principal del endpoint /api/social/home-data
     */
    public record HomeSocialDataDto(
            List<ContactUserDto> friends,
            List<FriendRequestDto> incomingRequests,
            List<FriendRequestDto> outgoingRequests,
            List<PreferredGameDto> preferredGames
    ) {}

    // ================================================
    // DTOs PARA SOLICITUDES ENTRANTES
    // ================================================

    /**
     * Solicitud de amistad entrante individual
     */
    public record FriendRequestDto(
            UUID requestId,
            ContactUserDto contactUser,
            FriendRequest.RequestSource source,
            FriendRequest.FriendRequestStatus status,
            SharedGameInfoDto sharedGame,
            LocalDateTime receivedAt
    ) {}

    /**
     * Información del juego sugerido (para solicitudes por sugerencia)
     */
    public record SharedGameInfoDto(
            String gameSlug,
            String gameName,
            int yourRating,
            int theirRating
    ) {}


    // ================================================
    // DTOs PARA JUEGOS PREFERIDOS
    // ================================================

    /**
     * Juego preferido del usuario (rating >= 7)
     */
    public record PreferredGameDto(
            String gameSlug,
            String gameName,
            int userRating
    ) {}

    // ================================================
    // DTOs PARA RESPUESTAS DE OPERACIONES
    // ================================================

    /**
     * Response de aceptar/rechazar solicitud
     */
    public record FriendRequestResponseDto(
            boolean success,
            ContactUserDto newFriend
    ) {}

    /**
     * Response para eliminar amigo
     */
    public record DeleteFriendResponseDto(
            boolean success,
            String deletedFriendUsername,
            UUID deletedFriendId
    ) {}


    /**
     * Resultado de búsqueda de usuarios por nombre
     */
    public record UserSearchResultDto(
            String query,
            List<SearchedUserDto> users
    ) {}

    public record SearchedUserDto(
            UserDto user,
            Boolean isFriend,
            Boolean hasPendingRequest,
            Boolean hasRejectedRequest
    ) {}

    // ================================================
    // DTOs PARA SUGERENCIAS POR JUEGO
    // ================================================

    /**
     * Usuario sugerido basado en juego
     */
    public record SuggestedUserDto(
            UserDto user,
            SharedGameInfoDto sharedGameInfoDto
    ) {}

    // ================================================
    // DTOs PARA SUGERENCIAS AUTOMÁTICAS (HU-20)
    // ================================================

    /**
     * Información básica de usuario - DTO base
     */
    public record UserDto(
            UUID userId,
            String username
    ) {}

    /**
     * Información básica de usuario junto a informacion de contacto
     */
    public record ContactUserDto(
            UserDto user,
            Boolean newMessages
    ) {}
}