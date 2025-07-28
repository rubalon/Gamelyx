package com.gamelyx.mapper;

import com.gamelyx.dto.external.RawgApiDtos;
import com.gamelyx.dto.GameResponseDtos;
import com.gamelyx.entity.Game;
import com.gamelyx.entity.UserGameDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper actualizado para los nuevos DTOs centrados en funcionalidades
 *
 * CAMBIOS:
 * - Métodos alineados con nuevos DTOs (records + classes)
 * - Solo usar campos que existen en entidades simplificadas
 * - Conversiones específicas para cada endpoint
 */
@Component
public class GameMapper {

    // ===== CONVERSIONES PARA GET /search =====

    /**
     * Convierte RawgApiDtos.GameSummary → GameResponseDtos.GameSearchItem
     * Para: GET /search (lista de resultados de búsqueda)
     */
    public GameResponseDtos.GameSearchItem rawgSummaryToSearchItem(RawgApiDtos.GameSummary rawgGame) {
        if (rawgGame == null) {
            return null;
        }

        // Truncar descripción para preview (usar nombre como fallback por ahora)
        String shortDescription = rawgGame.getName() != null ?
                "Juego: " + rawgGame.getName() : "Sin descripción disponible";

        return new GameResponseDtos.GameSearchItem(
                rawgGame.getId(),
                rawgGame.getSlug(),                    // Slug de RAWG
                rawgGame.getName(),
                rawgGame.getBackgroundImage(),         // Usar como coverImage
                shortDescription,                      // Descripción corta
                rawgGame.getRating(),
                rawgGame.getReleased(),
                extractPlatformNames(rawgGame.getPlatforms()),
                extractGenreNames(rawgGame.getGenres())
        );
    }

    /**
     * Convierte lista de RawgApiDtos.GameSummary → lista de GameSearchItem
     */
    public List<GameResponseDtos.GameSearchItem> rawgSummaryListToSearchItems(List<RawgApiDtos.GameSummary> rawgGames) {
        if (rawgGames == null) {
            return List.of();
        }

        return rawgGames.stream()
                .map(this::rawgSummaryToSearchItem)
                .collect(Collectors.toList());
    }

    // ===== CONVERSIONES PARA GET /game/{identifier} =====

    /**
     * Convierte Game entity → GameResponseDtos.GamePageDto (base)
     * Para: GET /game/{identifier} (página completa del juego)
     */
    public GameResponseDtos.GamePageDto gameToPageDto(Game game) {
        if (game == null) {
            return null;
        }

        GameResponseDtos.GamePageDto dto = new GameResponseDtos.GamePageDto();

        // Datos básicos
        dto.setRawgId(game.getRawgId());
        dto.setSlug(game.getSlug());
        dto.setName(game.getName());
        dto.setDescription(game.getDescription());
        dto.setDescriptionRaw(game.getDescriptionRaw());
        dto.setBackgroundImage(game.getBackgroundImage());
        dto.setCoverImage(game.getCoverImage()); // Mapear backgroundImageAdditional → coverImage
        dto.setScreenshots(stringToList(game.getScreenshots()));

        // Ratings
        dto.setRating(game.getRating());
        dto.setCommunityRating(game.getCommunityRating());
        dto.setTotalCommunityReviews(game.getCommunityReviewsCount());

        // Metadata
        dto.setReleased(game.getReleased());
        dto.setWebsite(game.getWebsite());
        dto.setMetacriticScore(game.getMetacriticScore());
        dto.setAveragePlaytime(game.getAveragePlaytime());

        // Listas (convertir strings con comas a listas)
        dto.setPlatforms(stringToList(game.getPlatforms()));
        dto.setGenres(stringToList(game.getGenres()));
        dto.setDevelopers(stringToList(game.getDevelopers()));
        dto.setPublishers(stringToList(game.getPublishers()));
        dto.setTags(stringToList(game.getTags()));

        // Timestamps
        dto.setLastUpdated(game.getUpdatedAt());

        // myStatus y recentReviews se setean externamente en el service
        dto.setMyStatus(null);           // Se asigna en service si hay usuario
        dto.setRecentReviews(List.of()); // Se asigna en service

        return dto;
    }

    /**
     * Convierte UserGameDetails → GameResponseDtos.MyGameStatus
     * Para: parte de GamePageDto (mi estado personal)
     */
    public GameResponseDtos.MyGameStatus userGameDetailsToMyStatus(UserGameDetails ugd) {
        if (ugd == null) {
            return null;
        }

        return new GameResponseDtos.MyGameStatus(
                ugd.getStatus() != null ? ugd.getStatus().name() : null,
                ugd.getRating(),
                ugd.getReviewText(),
                ugd.getReviewUpdatedAt()  // ✅ Campo que existe
        );
    }

    /**
     * Convierte UserGameDetails → GameResponseDtos.OtherUserReview
     * Para: parte de GamePageDto (reviews de otros usuarios)
     */
    public GameResponseDtos.OtherUserReview userGameDetailsToOtherReview(UserGameDetails ugd) {
        if (ugd == null || !ugd.hasReview()) {
            return null;
        }

        return new GameResponseDtos.OtherUserReview(
                ugd.getUser().getUsername(),
                ugd.getRating(),
                truncateText(ugd.getReviewText(), 200), // Truncar para preview
                ugd.getStatus() != null ? ugd.getStatus().name() : null,
                ugd.getReviewCreatedAt()  // ✅ Campo que existe
        );
    }

    /**
     * Convierte lista de UserGameDetails → lista de OtherUserReview
     */
    public List<GameResponseDtos.OtherUserReview> userGameDetailsListToOtherReviews(List<UserGameDetails> ugdList) {
        if (ugdList == null) {
            return List.of();
        }

        return ugdList.stream()
                .filter(UserGameDetails::hasReview) // Solo los que tienen review
                .map(this::userGameDetailsToOtherReview)
                .collect(Collectors.toList());
    }

    // ===== CONVERSIONES PARA GET /my-reviews =====

    /**
     * Convierte UserGameDetails → GameResponseDtos.MyReviewDto
     * Para: GET /my-reviews (mis reviews recientes o paginadas)
     */
    public GameResponseDtos.MyReviewDto userGameDetailsToMyReview(UserGameDetails ugd) {
        if (ugd == null || !ugd.hasReview()) {
            return null;
        }

        Game game = ugd.getGame();

        return new GameResponseDtos.MyReviewDto(
                game.getRawgId(),
                game.getSlug(),
                game.getName(),
                game.getCoverImage() != null ?
                        game.getCoverImage() : game.getBackgroundImage(), // coverImage o fallback
                ugd.getRating(),
                ugd.getReviewText(),
                ugd.getStatus() != null ? ugd.getStatus().name() : null,
                ugd.getReviewCreatedAt(),   // ✅ Campo que existe
                ugd.getReviewUpdatedAt()    // ✅ Campo que existe
        );
    }

    /**
     * Convierte lista de UserGameDetails → lista de MyReviewDto
     */
    public List<GameResponseDtos.MyReviewDto> userGameDetailsListToMyReviews(List<UserGameDetails> ugdList) {
        if (ugdList == null) {
            return List.of();
        }

        return ugdList.stream()
                .filter(UserGameDetails::hasReview) // Solo los que tienen review
                .map(this::userGameDetailsToMyReview)
                .collect(Collectors.toList());
    }

    // ===== CONVERSIONES DESDE RAWG A GAME ENTITY =====

    /**
     * Convierte RawgApiDtos.GameDetails → Game entity (para guardar en BD)
     * Para: cuando obtenemos juego de RAWG por primera vez
     */
    public Game rawgDetailsToGameEntity(RawgApiDtos.GameDetails rawgGame) {
        if (rawgGame == null) {
            return null;
        }

        Game game = new Game();

        // IDs y datos básicos
        game.setRawgId(rawgGame.getId());
        game.setSlug(rawgGame.getSlug());
        game.setName(rawgGame.getName());
        game.setDescription(rawgGame.getDescription());
        game.setDescriptionRaw(rawgGame.getDescriptionRaw());
        game.setBackgroundImage(rawgGame.getBackgroundImage());
        game.setCoverImage(rawgGame.getBackgroundImageAdditional());

        // Ratings
        game.setRating(rawgGame.getRating());
        game.setRatingTop(rawgGame.getRatingTop());

        // Metadata
        game.setReleased(rawgGame.getReleased());
        game.setWebsite(rawgGame.getWebsite());
        game.setMetacriticScore(rawgGame.getMetacriticScore());
        game.setAveragePlaytime(rawgGame.getAveragePlaytime());

        // Convertir listas a strings con comas
        game.setPlatforms(extractPlatformNamesAsString(rawgGame.getPlatforms()));
        game.setGenres(extractGenreNamesAsString(rawgGame.getGenres()));
        game.setDevelopers(extractDeveloperNamesAsString(rawgGame.getDevelopers()));
        game.setPublishers(extractPublisherNamesAsString(rawgGame.getPublishers()));

        // Tags y screenshots
        if (rawgGame.getTags() != null) {
            game.setTags(rawgGame.getTags().stream()
                    .map(RawgApiDtos.Tag::getName)
                    .collect(Collectors.joining(",")));
        }

        // TODO: Screenshots - necesitaremos un endpoint separado en RAWG
        game.setScreenshots(null); // Por ahora null, implementar después

        // Control
        game.setDataSource("RAWG");
        game.setLastExternalUpdate(java.time.LocalDateTime.now());

        return game;
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

    /**
     * Extrae nombres de plataformas de RAWG como lista
     */
    private List<String> extractPlatformNames(List<RawgApiDtos.Platform> platforms) {
        if (platforms == null) {
            return List.of();
        }

        return platforms.stream()
                .map(p -> p.getPlatform().getName())
                .collect(Collectors.toList());
    }

    /**
     * Extrae nombres de plataformas de RAWG como string con comas
     */
    private String extractPlatformNamesAsString(List<RawgApiDtos.Platform> platforms) {
        return listToString(extractPlatformNames(platforms));
    }

    /**
     * Extrae nombres de géneros de RAWG como lista
     */
    private List<String> extractGenreNames(List<RawgApiDtos.Genre> genres) {
        if (genres == null) {
            return List.of();
        }

        return genres.stream()
                .map(RawgApiDtos.Genre::getName)
                .collect(Collectors.toList());
    }

    /**
     * Extrae nombres de géneros de RAWG como string con comas
     */
    private String extractGenreNamesAsString(List<RawgApiDtos.Genre> genres) {
        return listToString(extractGenreNames(genres));
    }

    /**
     * Extrae nombres de desarrolladores de RAWG como string con comas
     */
    private String extractDeveloperNamesAsString(List<RawgApiDtos.Developer> developers) {
        if (developers == null) {
            return null;
        }

        List<String> names = developers.stream()
                .map(RawgApiDtos.Developer::getName)
                .collect(Collectors.toList());

        return listToString(names);
    }

    /**
     * Extrae nombres de publishers de RAWG como string con comas
     */
    private String extractPublisherNamesAsString(List<RawgApiDtos.Publisher> publishers) {
        if (publishers == null) {
            return null;
        }

        List<String> names = publishers.stream()
                .map(RawgApiDtos.Publisher::getName)
                .collect(Collectors.toList());

        return listToString(names);
    }

    /**
     * Trunca texto para previews
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "...";
    }
}