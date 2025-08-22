package com.gamelyx.mapper;

import com.gamelyx.dto.SocialResponseDtos.*;
import com.gamelyx.entity.FriendRequest;
import com.gamelyx.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Mapper para convertir entidades del sistema social a DTOs.
 * Mantiene la lógica de mapping separada del Service.
 */
@Component
public class SocialMapper {

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
                friendRequest.getSuggestedGame() != null ? friendRequest.getSuggestedGame().getSlug() : null,
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