package com.gamelyx.mapper;

import com.gamelyx.dto.response.GameResponseDtos;
import com.gamelyx.entity.Game;
import com.gamelyx.entity.UserGameDetails;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidades y DTOs de respuesta
 *
 * Este componente centraliza todas las conversiones, manteniendo
 * la lógica de transformación separada de los servicios.
 */
@Component
public class GameMapper {

    /**
     * Convierte una entidad Game a GameSummary DTO
     * Usado para: listados, búsquedas, carousels
     */
    public GameResponseDtos.GameSummary toGameSummary(Game game) {
        if (game == null) {
            return null;
        }

        GameResponseDtos.GameSummary dto = new GameResponseDtos.GameSummary();

        // Campos básicos
        dto.setId(game.getId());
        dto.setRawgId(game.getRawgId());
        dto.setName(game.getName());
        dto.setBackgroundImage(game.getBackgroundImage());
        dto.setRating(game.getRating());
        dto.setReleased(game.getReleased());

        // Convertir strings con comas a listas
        dto.setPlatforms(stringToList(game.getPlatforms()));
        dto.setGenres(stringToList(game.getGenres()));

        // Ratings de comunidad
        dto.setCommunityRating(game.getCommunityRating());
        dto.setCommunityReviewsCount(
                game.getCommunityReviewsCount() != null ?
                        game.getCommunityReviewsCount().longValue() : 0L
        );

        return dto;
    }

    /**
     * Convierte una entidad Game a GameDetails DTO
     * Usado para: página de detalles del juego
     */
    public GameResponseDtos.GameDetails toGameDetails(Game game) {
        if (game == null) {
            return null;
        }

        GameResponseDtos.GameDetails dto = new GameResponseDtos.GameDetails();

        // Campos heredados de GameSummary
        dto.setId(game.getId());
        dto.setRawgId(game.getRawgId());
        dto.setName(game.getName());
        dto.setBackgroundImage(game.getBackgroundImage());
        dto.setRating(game.getRating());
        dto.setReleased(game.getReleased());
        dto.setPlatforms(stringToList(game.getPlatforms()));
        dto.setGenres(stringToList(game.getGenres()));
        dto.setCommunityRating(game.getCommunityRating());
        dto.setCommunityReviewsCount(
                game.getCommunityReviewsCount() != null ?
                        game.getCommunityReviewsCount().longValue() : 0L
        );

        // Campos específicos de GameDetails
        dto.setDescription(game.getDescription());
        dto.setDescriptionRaw(game.getDescriptionRaw());
        dto.setWebsite(game.getWebsite());
        dto.setMetacriticScore(game.getMetacriticScore());
        dto.setScreenshots(stringToList(game.getScreenshots()));
        dto.setDataSource(game.getDataSource());
        dto.setLastExternalUpdate(game.getLastExternalUpdate());

        return dto;
    }

    /**
     * Convierte una entidad UserGameDetails a UserGameDetails DTO
     * Usado para: estado personal del usuario con un juego
     */
    public GameResponseDtos.UserGameDetails toUserGameDetailsDto(UserGameDetails entity) {
        if (entity == null) {
            return null;
        }

        GameResponseDtos.UserGameDetails dto = new GameResponseDtos.UserGameDetails();

        // IDs y relaciones
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        dto.setGameId(entity.getGame() != null ? entity.getGame().getId() : null);

        // Estado del juego
        dto.setStatus(entity.getStatus());
        dto.setAddedAt(entity.getAddedAt());
        dto.setStatusUpdatedAt(entity.getStatusUpdatedAt());
        dto.setCompletedAt(entity.getCompletedAt());

        // Review del usuario
        dto.setRating(entity.getRating());
        dto.setReviewText(entity.getReviewText());
        dto.setReviewCreatedAt(entity.getReviewCreatedAt());
        dto.setReviewUpdatedAt(entity.getReviewUpdatedAt());

        // Metadata
        dto.setLastUpdatedAt(entity.getLastUpdatedAt());

        return dto;
    }

    /**
     * Convierte Game + UserGameDetails a GameWithUserDetails DTO
     * Usado para: biblioteca personal del usuario
     */
    public GameResponseDtos.GameWithUserDetails toGameWithUserDetails(
            Game game, UserGameDetails userDetails) {

        if (game == null) {
            return null;
        }

        GameResponseDtos.GameWithUserDetails dto = new GameResponseDtos.GameWithUserDetails();

        // Campos de GameSummary
        dto.setId(game.getId());
        dto.setRawgId(game.getRawgId());
        dto.setName(game.getName());
        dto.setBackgroundImage(game.getBackgroundImage());
        dto.setRating(game.getRating());
        dto.setReleased(game.getReleased());
        dto.setPlatforms(stringToList(game.getPlatforms()));
        dto.setGenres(stringToList(game.getGenres()));
        dto.setCommunityRating(game.getCommunityRating());
        dto.setCommunityReviewsCount(
                game.getCommunityReviewsCount() != null ?
                        game.getCommunityReviewsCount().longValue() : 0L
        );

        // Detalles del usuario
        dto.setUserDetails(toUserGameDetailsDto(userDetails));

        return dto;
    }

    /**
     * Convierte UserGameDetails + User a GameReview DTO
     * Usado para: lista de reviews públicas de un juego
     */
    public GameResponseDtos.GameReview toGameReview(UserGameDetails userGameDetails) {
        if (userGameDetails == null || !userGameDetails.hasReview()) {
            return null;
        }

        GameResponseDtos.GameReview dto = new GameResponseDtos.GameReview();

        // IDs
        dto.setId(userGameDetails.getId());
        dto.setGameId(userGameDetails.getGame().getId());
        dto.setUserId(userGameDetails.getUser().getId());

        // Información del juego
        dto.setGameName(userGameDetails.getGame().getName());
        dto.setGameImage(userGameDetails.getGame().getBackgroundImage());

        // Información del usuario
        dto.setUsername(userGameDetails.getUser().getUsername());

        // Contenido de la review
        dto.setRating(userGameDetails.getRating());
        dto.setReviewText(userGameDetails.getReviewText());
        dto.setReviewCreatedAt(userGameDetails.getReviewCreatedAt());
        dto.setReviewUpdatedAt(userGameDetails.getReviewUpdatedAt());

        // Estado del juego para el usuario
        dto.setStatus(userGameDetails.getStatus());
        dto.setCompletedAt(userGameDetails.getCompletedAt());

        return dto;
    }

    /**
     * Convierte una lista de Game entities a lista de GameSummary DTOs
     */
    public List<GameResponseDtos.GameSummary> toGameSummaryList(List<Game> games) {
        if (games == null) {
            return List.of();
        }

        return games.stream()
                .map(this::toGameSummary)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una lista de UserGameDetails a lista de GameReview DTOs
     * Filtra automáticamente las que no tienen review
     */
    public List<GameResponseDtos.GameReview> toGameReviewList(List<UserGameDetails> userGameDetailsList) {
        if (userGameDetailsList == null) {
            return List.of();
        }

        return userGameDetailsList.stream()
                .filter(UserGameDetails::hasReview) // Solo los que tienen review
                .map(this::toGameReview)
                .collect(Collectors.toList());
    }

    /**
     * Convierte un Page<Game> a PagedGameResponse DTO
     * Usado para: respuestas de búsqueda paginadas
     */
    public GameResponseDtos.PagedGameResponse toPagedGameResponse(Page<Game> page) {
        if (page == null) {
            return new GameResponseDtos.PagedGameResponse();
        }

        List<GameResponseDtos.GameSummary> gameSummaries = page.getContent().stream()
                .map(this::toGameSummary)
                .collect(Collectors.toList());

        return new GameResponseDtos.PagedGameResponse(
                gameSummaries,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    /**
     * Crea un UserGameStats DTO a partir de conteos individuales
     * Usado para: estadísticas de la biblioteca del usuario
     */
    public GameResponseDtos.UserGameStats createUserGameStats(
            java.util.UUID userId,
            Long totalGames,
            Long wishlistCount,
            Long playingCount,
            Long completedCount,
            Long archivedCount,
            Long totalReviews,
            Double averageRating) {

        GameResponseDtos.UserGameStats stats = new GameResponseDtos.UserGameStats();
        stats.setUserId(userId);
        stats.setTotalGames(totalGames != null ? totalGames : 0L);
        stats.setWishlistCount(wishlistCount != null ? wishlistCount : 0L);
        stats.setPlayingCount(playingCount != null ? playingCount : 0L);
        stats.setCompletedCount(completedCount != null ? completedCount : 0L);
        stats.setArchivedCount(archivedCount != null ? archivedCount : 0L);
        stats.setTotalReviews(totalReviews != null ? totalReviews : 0L);
        stats.setAverageRating(averageRating);

        return stats;
    }

    // ===== MÉTODOS DE UTILIDAD =====

    /**
     * Convierte string con comas a lista de strings
     * Ejemplo: "PC,PlayStation 4,Xbox One" → ["PC", "PlayStation 4", "Xbox One"]
     */
    private List<String> stringToList(String commaSeparatedString) {
        if (commaSeparatedString == null || commaSeparatedString.trim().isEmpty()) {
            return List.of();
        }

        return List.of(commaSeparatedString.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Convierte lista de strings a string con comas
     * Ejemplo: ["PC", "PlayStation 4", "Xbox One"] → "PC,PlayStation 4,Xbox One"
     */
    public String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        return list.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.joining(","));
    }
}