package com.gamelyx.service;

import com.gamelyx.dto.SocialRequestDtos.*;
import com.gamelyx.dto.SocialResponseDtos.*;
import com.gamelyx.entity.FriendRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service mock para funcionalidades sociales.
 * TEMPORAL: Datos hardcodeados para que el controller compile sin errores.
 * TODO: Implementar lógica real con repositories en próximas iteraciones.
 */
@Service
public class SocialService {

    // ================================================
    // ENDPOINT PRINCIPAL PARA HOME SOCIAL
    // ================================================

    /**
     * Obtiene toda la información social para el home.
     * MOCK: Retorna datos de ejemplo hardcodeados.
     */
    public HomeSocialDataDto getHomeSocialData(String currentUsername) {
        // Datos mock de amigos
        List<ContactUserDto> friends = List.of(
                new ContactUserDto(
                        new UserDto(UUID.randomUUID(), "SofiaQueen"),
                        UUID.randomUUID(), // chatId
                        true // newMessages
                ),
                new ContactUserDto(
                        new UserDto(UUID.randomUUID(), "CarlosRPG"),
                        UUID.randomUUID(),
                        false
                ),
                new ContactUserDto(
                        new UserDto(UUID.randomUUID(), "AnaShooter"),
                        UUID.randomUUID(),
                        false
                )
        );

        // Datos mock de solicitudes entrantes
        List<IncomingRequestDto> incomingRequests = List.of(
                new IncomingRequestDto(
                        UUID.randomUUID(),
                        new ContactUserDto(
                                new UserDto(UUID.randomUUID(), "AlexGamer"),
                                null,
                                false
                        ),
                        FriendRequest.RequestSource.SEARCH,
                        null, // No hay juego sugerido para búsqueda manual
                        LocalDateTime.now().minusHours(2)
                ),
                new IncomingRequestDto(
                        UUID.randomUUID(),
                        new ContactUserDto(
                                new UserDto(UUID.randomUUID(), "MariaPro"),
                                null,
                                false
                        ),
                        FriendRequest.RequestSource.SUGGESTION,
                        new SuggestedGameInfoDto(
                                "zelda-breath-of-the-wild",
                                "The Legend of Zelda: Breath of the Wild",
                                9, // Tu rating
                                8  // Su rating
                        ),
                        LocalDateTime.now().minusMinutes(30)
                )
        );

        // Datos mock de solicitudes salientes
        List<OutgoingRequestDto> outgoingRequests = List.of(
                new OutgoingRequestDto(
                        UUID.randomUUID(),
                        new ContactUserDto(
                                new UserDto(UUID.randomUUID(), "LuisArcade"),
                                null,
                                false
                        ),
                        FriendRequest.RequestSource.SEARCH,
                        FriendRequest.FriendRequestStatus.PENDING,
                        null, // Sin juego para búsqueda manual
                        LocalDateTime.now().minusHours(1)
                )
        );

        // Datos mock de juegos preferidos
        List<PreferredGameDto> preferredGames = List.of(
                new PreferredGameDto(
                        "zelda-breath-of-the-wild",
                        "The Legend of Zelda: Breath of the Wild",
                        9
                ),
                new PreferredGameDto(
                        "elden-ring",
                        "Elden Ring",
                        8
                ),
                new PreferredGameDto(
                        "minecraft",
                        "Minecraft",
                        7
                )
        );

        return new HomeSocialDataDto(
                friends,
                incomingRequests,
                outgoingRequests,
                preferredGames
        );
    }

    // ================================================
    // GESTIÓN DE SOLICITUDES DE AMISTAD
    // ================================================

    /**
     * Envía una solicitud de amistad.
     * MOCK: Simula el envío exitoso.
     */
    public OutgoingRequestDto sendFriendRequest(String senderUsername, SendFriendRequestDto request) {
        // Simular creación de solicitud
        return new OutgoingRequestDto(
                UUID.randomUUID(), // requestId generado
                new ContactUserDto(
                        new UserDto(request.targetUserId(), "UsuarioMock"),
                        null,
                        false
                ),
                request.source(),
                FriendRequest.FriendRequestStatus.PENDING,
                request.gameSlug(),
                LocalDateTime.now()
        );
    }

    /**
     * Responde a una solicitud de amistad (acepta o rechaza).
     * MOCK: Simula aceptación/rechazo exitoso.
     */
    public FriendRequestResponseDto respondToFriendRequest(
            String currentUsername,
            String requestId,
            RespondToFriendRequestDto request) {

        if (request.action() == FriendRequestAction.ACCEPT) {
            // Simular aceptación exitosa
            ContactUserDto newFriend = new ContactUserDto(
                    new UserDto(UUID.randomUUID(), "NuevoAmigo"),
                    UUID.randomUUID(), // chatId creado
                    false
            );

            return new FriendRequestResponseDto(true, newFriend);
        } else {
            // Simular rechazo exitoso
            return new FriendRequestResponseDto(false, null);
        }
    }

    // ================================================
    // BÚSQUEDA DE USUARIOS
    // ================================================

    /**
     * Busca usuarios por nombre.
     * MOCK: Retorna resultados simulados basados en la query.
     */
    public UserSearchResultDto searchUsersByName(String currentUsername, UserSearchRequestDto searchRequest) {
        // Validación básica
        if (searchRequest.query().length() < 2) {
            throw new IllegalArgumentException("Query debe tener al menos 2 caracteres");
        }

        // Datos mock que simulan coincidencias
        List<UserDto> mockResults = List.of(
                new UserDto(UUID.randomUUID(), searchRequest.query() + "Fan"),
                new UserDto(UUID.randomUUID(), "Pro" + searchRequest.query()),
                new UserDto(UUID.randomUUID(), searchRequest.query() + "Master")
        );

        // Limitar resultados según el parámetro
        List<UserDto> limitedResults = mockResults.stream()
                .limit(searchRequest.limit())
                .toList();

        return new UserSearchResultDto(searchRequest.query(), limitedResults);
    }

    /**
     * Busca usuarios por preferencias de juego.
     * MOCK: Retorna sugerencias simuladas.
     */
    public GameBasedSuggestionDto searchUsersByGamePreference(
            String currentUsername,
            GameBasedSearchRequestDto searchRequest) {

        // Validación básica
        if (searchRequest.userRating() < 1 || searchRequest.userRating() > 10) {
            throw new IllegalArgumentException("Rating debe estar entre 1 y 10");
        }

        // Datos mock de usuarios con gustos similares
        List<SuggestedUserDto> mockSuggestions = List.of(
                new SuggestedUserDto(
                        new UserDto(UUID.randomUUID(), "GamerCompatible1"),
                        searchRequest.gameSlug(),
                        searchRequest.userRating(),
                        searchRequest.userRating() + 1 // Rating similar
                ),
                new SuggestedUserDto(
                        new UserDto(UUID.randomUUID(), "GamerCompatible2"),
                        searchRequest.gameSlug(),
                        searchRequest.userRating(),
                        searchRequest.userRating() - 1 // Rating similar
                )
        );

        // Limitar resultados
        List<SuggestedUserDto> limitedSuggestions = mockSuggestions.stream()
                .limit(searchRequest.maxResults())
                .toList();

        return new GameBasedSuggestionDto(
                searchRequest.gameSlug(),
                searchRequest.userRating(),
                limitedSuggestions
        );
    }

    // ================================================
    // GESTIÓN DE AMISTADES
    // ================================================

    /**
     * Elimina un amigo de la lista.
     * MOCK: Simula eliminación exitosa.
     */
    public DeleteFriendResponseDto deleteFriend(String currentUsername, String friendUsername) {
        // Simular eliminación exitosa
        return new DeleteFriendResponseDto(
                true,
                friendUsername,
                UUID.randomUUID() // ID del amigo eliminado
        );
    }

    // ================================================
    // SUGERENCIAS AUTOMÁTICAS (HU-20)
    // ================================================

    /**
     * Obtiene la siguiente sugerencia de amigo.
     * MOCK: Retorna una sugerencia simulada.
     */
    public SuggestedUserDto getNextFriendSuggestion(String currentUsername) {
        // Simular que siempre hay una sugerencia disponible
        return new SuggestedUserDto(
                new UserDto(UUID.randomUUID(), "SugerenciaAutomatica"),
                "zelda-breath-of-the-wild",
                9, // Tu rating
                8  // Su rating
        );
    }

    /**
     * Rechaza una sugerencia específica.
     * MOCK: Simula rechazo de sugerencia.
     */
    public void rejectSuggestion(String currentUsername, String rejectedUsername, String gameSlug) {
        // Mock: No hace nada, pero evita errores de compilación
        System.out.println("Sugerencia rechazada: " + rejectedUsername + " por juego: " + gameSlug);
    }

    // ================================================
    // MÉTODOS AUXILIARES MOCK
    // ================================================

    /**
     * Simula la obtención del usuario actual por username.
     * MOCK: Retorna usuario simulado.
     */
    private UserDto getCurrentUserMock(String username) {
        return new UserDto(UUID.randomUUID(), username);
    }

    /**
     * Simula la verificación de existencia de usuario.
     * MOCK: Siempre retorna true.
     */
    private boolean userExistsMock(String username) {
        return true;
    }

    /**
     * Simula la verificación de amistad existente.
     * MOCK: Siempre retorna false (no son amigos).
     */
    private boolean areAlreadyFriendsMock(String user1, String user2) {
        return false;
    }
}