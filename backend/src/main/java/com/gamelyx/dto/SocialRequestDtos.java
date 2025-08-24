package com.gamelyx.dto;

import com.gamelyx.entity.FriendRequest;

import java.util.UUID;

/**
 * DTOs para requests del sistema social.
 * VERSIÓN LIMPIA: Solo datos, sin validaciones ni factory methods.
 * Las validaciones se manejan en el Service layer.
 */
public class SocialRequestDtos {

    // ================================================
    // REQUEST PARA ENVÍO DE SOLICITUDES DE AMISTAD
    // ================================================

    /**
     * Request para enviar solicitud de amistad
     */
    public record SendFriendRequestDto(
            UUID targetUserId,
            FriendRequest.RequestSource source,
            String gameSlug,
            int yourRating
    ) {}

    // ================================================
    // REQUEST PARA BÚSQUEDAS
    // ================================================

    /**
     * Parámetros para búsqueda de usuarios por nombre
     */
    public record UserSearchRequestDto(
            String query,
            int limit
    ) {}


    // ================================================
    // ENUMS PARA REQUESTS
    // ================================================

    /**
     * Acciones posibles para responder a una solicitud de amistad
     */
    public enum FriendRequestAction {
        ACCEPT,
        REJECT
    }
}