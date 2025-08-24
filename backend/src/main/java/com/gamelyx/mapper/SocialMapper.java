package com.gamelyx.mapper;

import com.gamelyx.dto.SocialResponseDtos.*;
import com.gamelyx.entity.FriendRequest;
import com.gamelyx.entity.User;
import com.gamelyx.entity.UserGameDetails;
import com.gamelyx.repository.FriendRequestRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper para convertir entidades del sistema social a DTOs.
 * Mantiene la lógica de mapping separada del Service.
 */
@Component
public class SocialMapper {

    // ================================================
    // MAPPERS PARA LISTAS - HOME SOCIAL
    // ================================================

    /**
     * Convierte lista de Users a lista de ContactUserDto
     */
    public List<ContactUserDto> toContactUserDtoList(List<User> users) {
        return users.stream()
                .map(this::toContactUserDto)
                .toList();
    }

    /**
     * Convierte lista de FriendRequests a DTOs
     */
    public List<FriendRequestDto> toFriendRequestDtoList(List<FriendRequestRepository.FriendRequestProjection> projections) {
        return projections.stream()
                .map(this::toFriendRequestDto)
                .toList();
    }

    /**
     * Convierte lista de UserGameDetails a PreferredGameDto
     */
    public List<PreferredGameDto> toPreferredGameDtoList(List<UserGameDetails> gameDetails) {
        return gameDetails.stream()
                .map(this::toPreferredGameDto)
                .toList();
    }

// ================================================
// MAPPERS INDIVIDUALES QUE NECESITARÁS
// ================================================

    /**
     * Convierte FriendRequest a IncomingRequestDto
     */
    public FriendRequestDto toFriendRequestDto(FriendRequestRepository.FriendRequestProjection projection) {

        // 1. Crear ContactUserDto del sender usando los datos de la projection
        UserDto senderUserDto = new UserDto(
                projection.getContactId(),
                projection.getContactUsername()
        );
        ContactUserDto senderDto = new ContactUserDto(
                senderUserDto,
                null,
                false
        );

        // 2. Crear SharedGameInfoDto solo si hay juego sugerido
        SharedGameInfoDto gameInfo = getSharedGameInfoDto(projection);


        // 4. Construir el DTO final
        return new FriendRequestDto(
                projection.getRequestId(),          //  Desde projection
                senderDto,                          //  Contact User
                projection.getRequestSource(),      //  Source
                projection.getStatus(),             //  Status
                gameInfo,                           //  Con ratings reales o null
                projection.getReceivedAt()          //  Desde projection
        );
    }

    private SharedGameInfoDto getSharedGameInfoDto(FriendRequestRepository.FriendRequestProjection projection) {
        SharedGameInfoDto gameInfo = null;

        if (FriendRequest.RequestSource.SUGGESTION.equals(projection.getRequestSource()) &&
                projection.getGameSlug() != null) {

            gameInfo = new SharedGameInfoDto(
                    projection.getGameSlug(),
                    projection.getGameName(),
                    projection.getYourRating() != null ? projection.getYourRating() : 0,
                    projection.getTheirRating() != null ? projection.getTheirRating() : 0
            );
        }
        return gameInfo;
    }

    /**
     * Convierte UserGameDetails a PreferredGameDto
     */
    public PreferredGameDto toPreferredGameDto(UserGameDetails gameDetails) {
        return new PreferredGameDto(
                gameDetails.getGame().getSlug(),
                gameDetails.getGame().getName(),
                gameDetails.getRating()
        );
    }

    // ================================================
    // MAPPERS PARA BÚSQUEDA DE USUARIOS HU-16
    // ================================================


    /**
     * Crea UserSearchResultDto con query y resultados enriquecidos
     */
    public UserSearchResultDto toUserSearchResultDto(String query, List<SearchedUserDto> searchedUsers) {
        return new UserSearchResultDto(query, searchedUsers);
    }

    /**
     * Convierte User a SearchedUserDto con información de relación
     */
    public SearchedUserDto toSearchedUserDto(User user, Boolean isFriend, Boolean hasPendingRequest, Boolean hasRejectedRequest) {
        if (user == null) {
            return null;
        }

        UserDto userDto = toUserDto(user);

        return new SearchedUserDto(
                userDto,
                isFriend != null ? isFriend : false,
                hasPendingRequest != null ? hasPendingRequest : false,
                hasRejectedRequest != null ? hasRejectedRequest : false
        );
    }

    // ================================================
    // MAPPERS PARA SOLICITUDES DE AMISTAD HU-17
    // ================================================

    public OutgoingRequestDto toOutgoingRequestDto(FriendRequest friendRequest) {
        return new OutgoingRequestDto(
                friendRequest.getId(),
                new ContactUserDto(
                        new UserDto(friendRequest.getReceiver().getId(), friendRequest.getReceiver().getUsername()),
                        null, // No hay chatId hasta que se IMPLEMENTE
                        false // siempre son false de momento
                ),
                friendRequest.getSource(),
                friendRequest.getStatus(),
                friendRequest.getSharedGame() != null ? friendRequest.getSharedGame().getSlug() : null,
                friendRequest.getCreatedAt()
        );
    }

    /**
     * Convierte User a ContactUserDto (para nuevo amigo tras aceptar solicitud).
     */
    public ContactUserDto toContactUserDto(User user) {
        UserDto userDto = new UserDto(user.getId(), user.getUsername());

        return new ContactUserDto(userDto, null, false);
    }

    /**
     * Convierte a DTO de respuesta para eliminación de amigo.
     */
    public DeleteFriendResponseDto toDeleteFriendResponseDto(User deletedFriend, boolean success) {
        return new DeleteFriendResponseDto(
                success,
                deletedFriend.getUsername(),
                deletedFriend.getId()
        );
    }

    // ================================================
    // MAPPERS SUGERENCIA DE USUARIOS HU-20
    // ================================================
    /**
     * Convierte UserGameDetails a SuggestedUserDto para respuesta de sugerencias
     */
    public SuggestedUserDto toSuggestedUserDto(UserGameDetails userGameDetails, String gameSlug, int yourRating) {
        return new SuggestedUserDto(
                // Información básica del usuario sugerido
                new UserDto(
                        userGameDetails.getUser().getId(),
                        userGameDetails.getUser().getUsername()
                ),
                gameSlug,                           // Slug del juego en común
                yourRating,                         // Tu rating del juego
                userGameDetails.getRating()         // Su rating del juego
        );
    }

    // ================================================
    // MAPPERS GENERALES
    // ================================================

    /**
     * Convierte User entity a UserDto básico
     */
    public UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserDto(
                user.getId(),
                user.getUsername()
        );
    }








}