package com.gamelyx.service;

import com.gamelyx.dto.SocialRequestDtos.*;
import com.gamelyx.dto.SocialResponseDtos.*;
import com.gamelyx.entity.FriendRequest;
import com.gamelyx.entity.Friendship;
import com.gamelyx.entity.Game;
import com.gamelyx.entity.User;
import com.gamelyx.mapper.SocialMapper;
import com.gamelyx.repository.FriendRequestRepository;
import com.gamelyx.repository.FriendshipRepository;
import com.gamelyx.repository.GameRepository;
import com.gamelyx.repository.UserRepository;
import com.gamelyx.validator.SocialValidator;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service mock para funcionalidades sociales.
 * TEMPORAL: Datos hardcodeados para que el controller compile sin errores.
 * TODO: Implementar lógica real con repositories en próximas iteraciones.
 */
@Service
public class SocialService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final GameRepository gameRepository;
    private final SocialMapper socialMapper;
    private final SocialValidator socialValidator;
    private final Logger logger = LoggerFactory.getLogger(SocialService.class);

    // Constructor actualizado:
    public SocialService(
            UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            FriendRequestRepository friendRequestRepository,
            GameRepository gameRepository,
            SocialMapper socialMapper,
            SocialValidator socialValidator) {

        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.gameRepository = gameRepository;
        this.socialMapper = socialMapper;
        this.socialValidator = socialValidator;
    }

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
    // GESTIÓN DE SOLICITUDES DE AMISTAD HU-17
    // ================================================

    /**
     * Envía una solicitud de amistad.
     * IMPLEMENTACIÓN REAL: Usa validator para validaciones limpias.
     */
    public OutgoingRequestDto sendFriendRequest(String senderUsername, SendFriendRequestDto request) {
        // 1. Validar todo usando el validator
        SocialValidator.ValidationResult validation = socialValidator.validateSendFriendRequest(senderUsername, request);

        // 2. Crear la solicitud (validación ya pasada)
        FriendRequest friendRequest = new FriendRequest(
                validation.getSender(),
                validation.getReceiver(),
                request.source()
        );

        if (validation.getSuggestedGame() != null) {
            friendRequest.setSuggestedGame(validation.getSuggestedGame());
        }

        // 3. Guardar en BD
        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);

        // 4. Convertir a DTO usando el mapper
        return socialMapper.toOutgoingRequestDto(savedRequest);
    }

    /**
     * Responde a una solicitud de amistad (acepta o rechaza).
     * IMPLEMENTACIÓN REAL: Usa validator y mapper para lógica limpia.
     */
    public FriendRequestResponseDto respondToFriendRequest(
            String currentUsername,
            String requestId,
            FriendRequestAction action) {

        // 1. Validar la operación usando el validator
        SocialValidator.ValidationResult validation = socialValidator.validateRespondToFriendRequest(
                currentUsername,
                requestId,
                action
        );

        // 2. Obtener la solicitud validada
        FriendRequest friendRequest = validation.getFriendRequest();

        // 3. Procesar según la acción
        if (action == FriendRequestAction.ACCEPT) {
            // Aceptar: cambiar status y crear amistad bidireccional
            friendRequest.accept();
            friendRequestRepository.save(friendRequest);

            // Crear amistad bidireccional
            Friendship friendship1 = new Friendship(friendRequest.getReceiver(), friendRequest.getSender());
            Friendship friendship2 = new Friendship(friendRequest.getSender(), friendRequest.getReceiver());
            friendshipRepository.saveAll(List.of(friendship1, friendship2));

            // Retornar nuevo amigo
            ContactUserDto newFriend = socialMapper.toContactUserDto(friendRequest.getSender());
            return new FriendRequestResponseDto(true, newFriend);

        } else {
            // Rechazar: cambiar status solamente
            friendRequest.reject();
            friendRequestRepository.save(friendRequest);

            return new FriendRequestResponseDto(true, null);
        }
    }

    /**
     * Marca una solicitud como notificada al sender.
     * Solo el sender puede marcar su propia solicitud como notificada.
     */
    public void markFriendRequestAsNotified(String senderUsername, String requestId) {
        // 1. Validar usando el validator
        SocialValidator.ValidationResult validation = socialValidator.validateMarkAsNotified(
                senderUsername,
                requestId
        );

        // 2. Obtener la solicitud validada
        FriendRequest friendRequest = validation.getFriendRequest();

        // 3. Procesar según el estado
        if (friendRequest.getStatus() == FriendRequest.FriendRequestStatus.ACCEPTED) {
            // Las solicitudes aceptadas se eliminan directamente
            friendRequestRepository.delete(friendRequest);
        } else if (friendRequest.getStatus() == FriendRequest.FriendRequestStatus.REJECTED) {
            // Las solicitudes rechazadas se marcan como notificadas
            friendRequest.markSenderAsNotified();
            friendRequestRepository.save(friendRequest);
        }
    }



    // ================================================
    // BÚSQUEDA DE USUARIOS HU-16
    // ================================================

    /**
     * Busca usuarios por nombre (coincidencias parciales, case-insensitive).
     * Implementa HU-16: Excluye usuario actual, amigos existentes y solicitudes pendientes.
     */
    public UserSearchResultDto searchUsersByName(String currentUsername, UserSearchRequestDto searchRequest) {

        //Validaciones
        socialValidator.validateUserSearch(searchRequest.query(), searchRequest.limit());

        String cleanQuery = searchRequest.query().trim();
        int limit = searchRequest.limit();

        try {
            List<User> foundUsers = new ArrayList<>();

            // 1. Buscar coincidencias exactas primero (máximo 1)
            List<User> exactMatches = userRepository.findUsernameExactMatch(cleanQuery, currentUsername);
            foundUsers.addAll(exactMatches);

            // 2. Si no tenemos suficientes, buscar coincidencias parciales
            if (foundUsers.size() < limit) {
                int remainingLimit = limit - foundUsers.size(); // Restar exactas encontradas
                List<User> partialMatches = userRepository.searchUsersByUsername(
                        cleanQuery,
                        currentUsername,
                        remainingLimit
                );
                foundUsers.addAll(partialMatches);
            }

            UUID currentUserId = getUserByUsername(currentUsername).getId();

            // 3. Enriquecer cada usuario con información de relación
            List<SearchedUserDto> searchedUsers = foundUsers.stream()
                    .map(user -> enrichUserWithRelationshipInfo(user, currentUserId))
                    .toList();

            // 4. Convertir a DTO final
            return socialMapper.toUserSearchResultDto(cleanQuery, searchedUsers);

        } catch (Exception e) {
            logger.error("Error searching users with query '{}' for user '{}': {}",
                    cleanQuery, currentUsername, e.getMessage());
            throw new IllegalArgumentException("Error en la búsqueda de usuarios");
        }
    }

    /**
     * Enriquece un usuario con información de su relación con el usuario actual
     */
    private SearchedUserDto enrichUserWithRelationshipInfo(User user, UUID currentUserId) {
        // Verificar si son amigos
        boolean isFriend = friendshipRepository.areUsersFriends(currentUserId, user.getId());

        // Verificar si hay solicitud pendiente (en cualquier dirección)
        boolean hasPendingRequest = friendRequestRepository.existsPendingRequestBetween(currentUserId, user.getId());

        // Verificar si ya hay una solicitud rechazada
        boolean hasRejectedRequest = friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                currentUserId,
                user.getId(),
                FriendRequest.FriendRequestStatus.REJECTED
        );

        return socialMapper.toSearchedUserDto(user, isFriend, hasPendingRequest, hasRejectedRequest);
    }


    /**
     * Obtiene User entity por username - método auxiliar
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
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