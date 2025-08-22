package com.gamelyx.controller;

import com.gamelyx.dto.SocialRequestDtos.*;
import com.gamelyx.dto.SocialResponseDtos.*;
import com.gamelyx.service.SocialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

    // ================================================
    // ENDPOINT PRINCIPAL PARA HOME SOCIAL
    // ================================================

    /**
     * Obtiene toda la información social necesaria para el home.
     * Incluye: amigos, solicitudes entrantes/salientes, juegos preferidos
     *
     * GET /api/social/home-data
     */
    @GetMapping("/home-social-data")
    public ResponseEntity<HomeSocialDataDto> getHomeSocialData(
            @AuthenticationPrincipal UserDetails userDetails) {

        HomeSocialDataDto homeData = socialService.getHomeSocialData(userDetails.getUsername());
        return ResponseEntity.ok(homeData);
    }

    // ================================================
    // GESTIÓN DE SOLICITUDES DE AMISTAD
    // ================================================

    /**
     * Envía una solicitud de amistad a otro usuario.
     * Valida que no existan solicitudes duplicadas o rechazadas.
     *
     * POST /api/social/friend-requests
     */
    @PostMapping("/friend-requests")
    public ResponseEntity<OutgoingRequestDto> sendFriendRequest(
            @AuthenticationPrincipal String username,
            @RequestBody SendFriendRequestDto request) {

        OutgoingRequestDto response = socialService.sendFriendRequest(
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

    /**
     * Busca usuarios por juego en común y nota similar.
     * Para sugerencias automáticas basadas en gustos.
     *
     * GET /api/social/search/by-game?gameSlug=string&userRating=8&maxResults=5&tolerance=2
     */
    @GetMapping("/search/by-game")
    public ResponseEntity<GameBasedSuggestionDto> searchUsersByGamePreference(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String gameSlug,
            @RequestParam int userRating,
            @RequestParam(defaultValue = "5") int maxResults ){

        try {
            GameBasedSearchRequestDto searchRequest = new GameBasedSearchRequestDto(
                    gameSlug, userRating, maxResults
            );

            GameBasedSuggestionDto suggestions = socialService.searchUsersByGamePreference(
                    userDetails.getUsername(),
                    searchRequest
            );

            return ResponseEntity.ok(suggestions);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ================================================
    // GESTIÓN DE AMISTADES hu -17
    // ================================================

    /**
     * Elimina a un usuario de la lista de amigos.
     * Elimina la amistad bidireccional completa.
     *
     * DELETE /api/social/friends/{friendUsername}
     */
    @DeleteMapping("/friends/{friendUsername}")
    public ResponseEntity<DeleteFriendResponseDto> deleteFriend(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String friendUsername) {

        DeleteFriendResponseDto response = socialService.deleteFriend(
                userDetails.getUsername(),
                friendUsername
        );

        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================================================
    // SUGERENCIAS AUTOMÁTICAS (HU-20)
    // ================================================

    /**
     * Obtiene UNA sugerencia de amigo basada en juegos en común.
     * Analiza los juegos mejor valorados del usuario actual.
     *
     * GET /api/social/suggestions/next
     */
    @GetMapping("/suggestions/next")
    public ResponseEntity<SuggestedUserDto> getNextFriendSuggestion(
            @AuthenticationPrincipal UserDetails userDetails) {
    /*
        SuggestedUserDto suggestion = socialService.getNextFriendSuggestion(
                userDetails.getUsername()
        );

        if (suggestion == null) {
            return ResponseEntity.noContent().build(); // 204 - No hay más sugerencias
        }

        return ResponseEntity.ok(suggestion);

     */
        return null;
    }

    /**
     * Rechaza una sugerencia específica para que no vuelva a aparecer.
     *
     * POST /api/social/suggestions/reject
     */
    /*
    @PostMapping("/suggestions/reject")
    public ResponseEntity<Void> rejectSuggestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RejectSuggestionDto request) {

        socialService.rejectSuggestion(
                userDetails.getUsername(),
                request.rejectedUsername(),
                request.gameSlug()
        );

        return ResponseEntity.ok().build();
    }

     */


}

