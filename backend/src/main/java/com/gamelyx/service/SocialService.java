package com.gamelyx.service;

import com.gamelyx.dto.SocialRequestDtos.*;
import com.gamelyx.dto.SocialResponseDtos.*;
import com.gamelyx.entity.*;
import com.gamelyx.mapper.SocialMapper;
import com.gamelyx.repository.*;
import com.gamelyx.validator.SocialValidator;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final UserGameDetailsRepository userGameDetailsRepository;
    private final SuggestionRejectionRepository suggestionRejectionRepository;
    private final SocialMapper socialMapper;
    private final SocialValidator socialValidator;
    private final Logger logger = LoggerFactory.getLogger(SocialService.class);

    // Constructor actualizado:
    public SocialService(
            UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            FriendRequestRepository friendRequestRepository,
            GameRepository gameRepository, UserGameDetailsRepository userGameDetailsRepository, SuggestionRejectionRepository suggestionRejectionRepository,
            SocialMapper socialMapper,
            SocialValidator socialValidator) {

        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.gameRepository = gameRepository;
        this.userGameDetailsRepository = userGameDetailsRepository;
        this.suggestionRejectionRepository = suggestionRejectionRepository;
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
    /**
     * Obtiene toda la información social para el home.
     * IMPLEMENTACIÓN REAL: Carga amigos, solicitudes y juegos preferidos del usuario.
     */
    public HomeSocialDataDto getHomeSocialData(String username) {
        // 1. Validar usuario actual usando el validator
        User callerUser = socialValidator.validateUserExists(username);


        // 2. Obtener amigos con información de contacto
        List<User> friendUsers = friendshipRepository.findFriendsByUserId(callerUser.getId());
        List<ContactUserDto> friends = socialMapper.toContactUserDtoList(friendUsers);

        // 3. Obtener solicitudes entrantes pendientes
        List<FriendRequestRepository.FriendRequestProjection> incomingRequests = friendRequestRepository.findPendingRequestsReceivedBy(
                callerUser.getId(),
                FriendRequest.FriendRequestStatus.PENDING
        );
        List<FriendRequestDto> incomingRequestDtos = socialMapper.toFriendRequestDtoList(incomingRequests);

        // 4. Obtener solicitudes salientes pendientes
        List<FriendRequestRepository.FriendRequestProjection> outgoingRequests = friendRequestRepository.findRequestsSentBy(
                callerUser.getId()
        );
        List<FriendRequestDto> outgoingRequestDtos = socialMapper.toFriendRequestDtoList(outgoingRequests);

        // 5. Obtener juegos preferidos del usuario (rating >= 7)
        List<UserGameDetails> preferredGames = userGameDetailsRepository.findPreferredGamesByUserId(
                callerUser.getId()
        );
        List<PreferredGameDto> preferredGameDtos = socialMapper.toPreferredGameDtoList(preferredGames);

        // 6. Construir respuesta completa
        return new HomeSocialDataDto(
                friends,
                incomingRequestDtos,
                outgoingRequestDtos,
                preferredGameDtos
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
                validation.getCallerUser(),
                validation.getTargetUser(),
                request.source()
        );

        if (validation.getGame() != null) {
            friendRequest.setSharedGame(validation.getGame());
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



    // ================================================
    // GESTIÓN DE AMISTADES HU-19
    // ================================================

    /**
     * Elimina un amigo de la lista.
     * IMPLEMENTACIÓN REAL: Usa validator para validaciones limpias.
     */
    @Transactional
    public DeleteFriendResponseDto deleteFriend(String currentUsername, UUID friendId) {
        // 1. Validar todo usando el validator
        SocialValidator.ValidationResult validation = socialValidator.validateDeleteFriend(currentUsername, friendId);

        // 2. Eliminar amistad bidireccional (validación ya pasada)
        friendshipRepository.deleteBidirectionalFriendship(
                validation.getCallerUser().getId(),
                friendId
        );

        // 3. Opcional: Limpiar solicitudes relacionadas (pendientes entre estos usuarios)
        cleanupRelatedFriendRequests(validation.getCallerUser().getId(), friendId);

        // 4. Convertir a DTO usando el mapper
        return socialMapper.toDeleteFriendResponseDto(validation.getTargetUser(), true);
    }

    /**
     * Limpia solicitudes de amistad relacionadas entre dos usuarios.
     * Elimina solicitudes PENDIENTES en ambas direcciones.
     */
    private void cleanupRelatedFriendRequests(UUID userId1, UUID userId2) {
        // Buscar solicitudes pendientes en ambas direcciones
        List<FriendRequest> pendingRequests = friendRequestRepository
                .findPendingRequestsBetweenUsers(userId1, userId2);

        // Eliminar directamente las solicitudes pendientes
        if (!pendingRequests.isEmpty()) {
            friendRequestRepository.deleteAll(pendingRequests);
        }
    }

    // ================================================
    // SUGERENCIAS AUTOMÁTICAS (HU-20)
    // ================================================

    /**
     * Obtiene UNA sugerencia de amigo basada en juego y rating.
     * VERSIÓN OPTIMIZADA: Una sola query + búsqueda progresiva en memoria.
     */
    public SuggestedUserDto getFriendSuggestionByGame(String currentUsername, String gameSlug, int userRating) {
        // 1. Validaciones esenciales únicamente
        SocialValidator.ValidationResult validation = socialValidator.validateFriendSuggestionByGame(
                currentUsername, gameSlug, userRating);

        // 2. Query única optimizada con exclusiones en BD (más eficiente)
        List<UserGameDetails> availableCandidates = userGameDetailsRepository
                .findAvailableCandidatesForSuggestion(
                        validation.getCallerUser().getId(),
                        validation.getGame().getId(),
                        7,
                        10
                );

        // 3. Early return si no hay candidatos
        if (availableCandidates.isEmpty()) {
            return null; // 204 No Content - comportamiento natural
        }

        // 4. Búsqueda progresiva optimizada en memoria
        UserGameDetails bestMatch = findBestMatchWithProgression(availableCandidates, userRating);

        if (bestMatch == null) {
            return null; // No hay matches en rango 7-10
        }

        // 5. Conversión a DTO con mapper optimizado
        return socialMapper.toSuggestedUserDto(bestMatch, gameSlug, userRating);
    }

    /**
     * Busca el mejor candidato usando búsqueda progresiva.
     * Algoritmo: Busca rating exacto → ±1 → ±2 → ±3
     */
    private UserGameDetails findBestMatchWithProgression(List<UserGameDetails> candidates, int targetRating) {
        if (candidates.isEmpty()) {
            return null;
        }

        Map<Integer, List<UserGameDetails>> usersByRating = groupByRating(candidates);

        // Búsqueda exacta primero
        List<UserGameDetails> exactMatch = usersByRating.get(targetRating);
        if (exactMatch != null && !exactMatch.isEmpty()) {
            return exactMatch.get(0);
        }

        // Búsqueda progresiva con tolerancia
        for (int tolerance = 1; tolerance <= 3; tolerance++) {
            int higherRating = targetRating + tolerance;
            int lowerRating = targetRating - tolerance;

            // Buscar rating mayor si está en rango válido
            if (higherRating <= 10) {
                List<UserGameDetails> higherMatch = usersByRating.get(higherRating);
                if (higherMatch != null && !higherMatch.isEmpty()) {
                    return higherMatch.get(0);
                }
            }

            // Buscar rating menor si está en rango válido
            if (lowerRating >= 7) {
                List<UserGameDetails> lowerMatch = usersByRating.get(lowerRating);
                if (lowerMatch != null && !lowerMatch.isEmpty()) {
                    return lowerMatch.get(0);
                }
            }
        }

        return null;
    }

    /**
     * Agrupa usuarios por su rating para búsqueda más eficiente
     */
    private Map<Integer, List<UserGameDetails>> groupByRating(List<UserGameDetails> candidates) {
        return candidates.stream()
                .collect(Collectors.groupingBy(UserGameDetails::getRating));
    }

    /**
     * Rechaza una sugerencia de amigo para un juego específico.
     * IMPLEMENTACIÓN REAL: Usa validator para validaciones limpias.
     */
    public void rejectFriendSuggestion(String currentUsername, UUID rejectedUserId, String gameSlug) {
        // 1. Validar usando el validator
        SocialValidator.ValidationResult validation = socialValidator.validateRejectSuggestion(
                currentUsername, rejectedUserId, gameSlug);

        // 2. Crear el registro de rechazo
        SuggestionRejection rejection = new SuggestionRejection(
                validation.getCallerUser(),
                validation.getTargetUser(),
                validation.getGame()
        );

        // 3. Guardar en BD (el unique constraint previene duplicados)
        suggestionRejectionRepository.save(rejection);
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