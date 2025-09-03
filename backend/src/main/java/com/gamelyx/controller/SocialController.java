package com.gamelyx.controller;

import com.gamelyx.dto.SocialRequestDtos.*;
import com.gamelyx.dto.SocialResponseDtos.*;
import com.gamelyx.service.SocialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller para manejar todas las funcionalidades sociales:
 * - Gestión de amigos y solicitudes
 * - Búsqueda de usuarios
 * - Sugerencias automáticas
 */
@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    // ========================================================================
    // ENDPOINT PRINCIPAL PARA HOME SOCIAL HU-16 , HU-17 , HU-18 ,HU-19 Y HU20
    // ========================================================================

    /**
     * Obtiene toda la información social necesaria para el home.
     * Incluye: amigos, solicitudes entrantes/salientes, juegos preferidos
     *
     * GET /api/social/home-data
     */
    @GetMapping("/home-social-data")
    public ResponseEntity<HomeSocialDataDto> getHomeSocialData(
            @AuthenticationPrincipal String username) {
        HomeSocialDataDto homeData = socialService.getHomeSocialData(username);
        return ResponseEntity.ok(homeData);
    }

    // ================================================
    // GESTIÓN DE AMISTADES HU-17 , HU-19
    // ================================================

    /**
     * Envía una solicitud de amistad a otro usuario.
     * Valida que no existan solicitudes duplicadas o rechazadas.
     *
     * POST /api/social/friend-requests
     */
    @PostMapping("/friend-requests")
    public ResponseEntity<FriendRequestDto> sendFriendRequest(
            @AuthenticationPrincipal String username,
            @RequestBody SendFriendRequestDto request) {

        FriendRequestDto response = socialService.sendFriendRequest(
                username,
                request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Responde a una solicitud de amistad (ACCEPT o REJECT).
     *
     * PUT /api/social/friend-requests/{requestId}/respond
     */
    @PutMapping("/friend-requests/{requestId}/respond")
    public ResponseEntity<FriendRequestResponseDto> respondToFriendRequest(
            @AuthenticationPrincipal String username,
            @PathVariable String requestId,
            @RequestParam FriendRequestAction action) {

        FriendRequestResponseDto response = socialService.respondToFriendRequest(
                username,
                requestId,
                action
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Marca una solicitud específica como notificada al sender.
     * Se usa después de mostrar la notificación al usuario que envió la solicitud.
     *
     * PUT /api/social/friend-requests/{requestId}/mark-notified
     */
    @PutMapping("/friend-requests/{requestId}/mark-notified")
    public ResponseEntity<Void> markFriendRequestAsNotified(
            @AuthenticationPrincipal String username,
            @PathVariable String requestId) {

        socialService.markFriendRequestAsNotified(username, requestId);
        return ResponseEntity.ok().build();
    }

    /**
     * Elimina a un usuario de la lista de amigos.
     * Elimina la amistad bidireccional completa.
     * Elimina las solicitudes de amistad relacionadas si existen
     *
     * DELETE /api/social/friends/{friendUsername}
     */
    @DeleteMapping("/friends/{friendId}")
    public ResponseEntity<DeleteFriendResponseDto> deleteFriend(
            @AuthenticationPrincipal String username,
            @PathVariable UUID friendId) {

        DeleteFriendResponseDto response = socialService.deleteFriend(
                username,
                friendId
        );

        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================================================
    // BÚSQUEDA DE USUARIOS HU-16
    // ================================================

    /**
     * Busca usuarios por nombre (coincidencias parciales, case-insensitive).
     * Excluye usuarios que ya son amigos o tienen solicitudes pendientes.
     *
     * GET /api/social/search/users?query=string&limit=10
     */
    @GetMapping("/search/users")
    public ResponseEntity<UserSearchResultDto> searchUsers(
            @AuthenticationPrincipal String username,
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {

        //Construimos el DTO
        UserSearchRequestDto searchRequest = new UserSearchRequestDto(q, limit);

        //Buscamos el usuario
        UserSearchResultDto results = socialService.searchUsersByName(username, searchRequest);

        return ResponseEntity.ok(results);
    }


    // ================================================
    // SUGERENCIAS AUTOMÁTICAS (HU-20)
    // ================================================

    /**
    * Obtiene UNA sugerencia de usuario basada en un juego específico y rating.
    * Busca usuarios con rating similar
     *
     * GET /api/social/friend-suggestion/by-game?gameSlug=zelda-breath-of-the-wild&userRating=9
     */
    @GetMapping("/friend-suggestion/by-game")
    public ResponseEntity<SuggestedUserDto> getFriendSuggestionByGame(
            @AuthenticationPrincipal String username,
            @RequestParam String gameSlug,
            @RequestParam int userRating) {


        SuggestedUserDto suggestion = socialService.getFriendSuggestionByGame(
                username,
                gameSlug,
                userRating
        );

        if (suggestion == null) {
            return ResponseEntity.noContent().build(); // 204 - No hay sugerencias
        }

        return ResponseEntity.ok(suggestion);
    }

    /**
     * Rechaza una sugerencia específica para que no vuelva a aparecer.
     * Marca al usuario como rechazado para el juego específico.
     *
     * POST /api/social/friend-suggestion/reject?rejectedUserId=uuid&gameSlug=zelda
     */
    @PostMapping("/friend-suggestion/reject")
    public ResponseEntity<Void> rejectFriendSuggestion(
            @AuthenticationPrincipal String username,
            @RequestParam UUID rejectedUserId,
            @RequestParam String gameSlug) {

        socialService.rejectFriendSuggestion(
                username,
                rejectedUserId,
                gameSlug
        );

        return ResponseEntity.ok().build(); // 200 OK sin contenido
    }


}

