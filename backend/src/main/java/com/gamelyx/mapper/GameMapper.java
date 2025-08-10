package com.gamelyx.mapper;

import com.gamelyx.dto.external.RawgApiDtos;
import com.gamelyx.dto.GameResponseDtos;
import com.gamelyx.entity.Game;
import com.gamelyx.entity.UserGameDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * GameMapper - ÚNICA FUENTE DE VERDAD para conversiones
 *
 * RESPONSABILIDADES:
 * - Todas las conversiones entre DTOs y entidades
 * - Manejo consistente de nomenclatura de campos
 * - Sin lógica de negocio (solo transformaciones)
 *
 * PRINCIPIO: Una conversión = un método en GameMapper
 */
@Component
public class GameMapper {

    // ===== CONVERSIONES: RAWG API → SEARCH RESULTS =====

    /**
     * Convierte RawgApiDtos.GameSummary → GameResponseDtos.GameSearchItem
     * Para: GET /search
     */
    public GameResponseDtos.GameSearchItem rawgSummaryToSearchItem(RawgApiDtos.GameSummary rawgGame) {
        if (rawgGame == null) {
            return null;
        }

        return new GameResponseDtos.GameSearchItem(
                rawgGame.getId(),
                rawgGame.getSlug(),
                rawgGame.getName(),
                rawgGame.getBackgroundImage(),         // ✅ Usar backgroundImage como coverImage
                rawgGame.getRating(),
                rawgGame.getReleased(),
                extractPlatformNames(rawgGame.getPlatforms()),
                extractGenreNames(rawgGame.getGenres())
        );
    }

    /**
     * Convierte RawgApiDtos.GameSummary → GameSearchItem con slug de BD si existe
     * Para: búsquedas que verifican BD local
     */
    public GameResponseDtos.GameSearchItem rawgSummaryToSearchItem(RawgApiDtos.GameSummary rawgGame,
                                                                   Optional<Game> existingGame) {
        if (rawgGame == null) {
            return null;
        }

        // Usar slug de BD si existe, sino el de RAWG
        String slug = existingGame.map(Game::getSlug).orElse(rawgGame.getSlug());

        return new GameResponseDtos.GameSearchItem(
                rawgGame.getId(),
                slug,
                rawgGame.getName(),
                rawgGame.getBackgroundImage(),
                rawgGame.getRating(),
                rawgGame.getReleased(),
                extractPlatformNames(rawgGame.getPlatforms()),
                extractGenreNames(rawgGame.getGenres())
        );
    }

    // ===== CONVERSIONES: GAME ENTITY → GAME PAGE =====

    /**
     * Convierte Game entity → GameResponseDtos.GamePageDto
     * Para: GET /game/{identifier}
     */
    public GameResponseDtos.GamePageDto gameToPageDto(Game game) {
        if (game == null) {
            return null;
        }

        GameResponseDtos.GamePageDto dto = new GameResponseDtos.GamePageDto();

        // IDs y datos básicos
        dto.setRawgId(game.getRawgId());
        dto.setSlug(game.getSlug());
        dto.setName(game.getName());
        dto.setDescription(game.getDescription());
        dto.setDescriptionRaw(game.getDescriptionRaw());
        dto.setBackgroundImage(game.getBackgroundImageAlt());
        dto.setCoverImage(game.getBackgroundImage());

        // Listas (convertir strings con comas → listas)
        dto.setScreenshots(stringToList(game.getScreenshots()));
        dto.setPlatforms(stringToList(game.getPlatforms()));
        dto.setGenres(stringToList(game.getGenres()));
        dto.setDevelopers(stringToList(game.getDevelopers()));
        dto.setPublishers(stringToList(game.getPublishers()));
        dto.setTags(stringToList(game.getTags()));

        // Ratings
        dto.setRating(game.getRating());
        dto.setCommunityRating(game.getCommunityRating());
        dto.setTotalCommunityReviews(game.getCommunityReviewsCount());

        // Metadata
        dto.setReleased(game.getReleased());
        dto.setWebsite(game.getWebsite());
        dto.setMetacriticScore(game.getMetacriticScore());
        dto.setLastUpdated(game.getUpdatedAt());

        // Estados iniciales (se setean externamente en service)
        dto.setMyStatus(null);
        dto.setRecentReviews(List.of());

        return dto;
    }

    // ===== CONVERSIONES: USER GAME DETAILS → USER STATUS =====

    /**
     * Convierte UserGameDetails → GameResponseDtos.MyGameStatus
     * Para: mi estado personal en GamePageDto
     */
    public GameResponseDtos.MyGameStatus userGameDetailsToMyStatus(UserGameDetails ugd) {
        if (ugd == null) {
            return null;
        }

        return new GameResponseDtos.MyGameStatus(
                ugd.getStatus() != null ? ugd.getStatus().name() : null,
                ugd.getRating(),
                ugd.getReviewText(),
                ugd.getReviewUpdatedAt()
        );
    }

    /**
     * Convierte UserGameDetails → GameResponseDtos.OtherUserReview
     * Para: reviews de otros usuarios en GamePageDto
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
                ugd.getReviewCreatedAt()
        );
    }

    /**
     * Convierte UserGameDetails → GameResponseDtos.MyReviewDto
     * Para: GET /my-reviews
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
                // ✅ Usar coverImage consistente: backgroundImageAdditional o fallback
                game.getBackgroundImage() != null ?
                        game.getBackgroundImage() : game.getBackgroundImageAlt(),
                ugd.getRating(),
                ugd.getReviewText(),
                ugd.getStatus() != null ? ugd.getStatus().name() : null,
                ugd.getReviewCreatedAt(),
                ugd.getReviewUpdatedAt()
        );
    }

    // ===== CONVERSIONES: RAWG API → GAME ENTITY =====

    /**
     * Convierte RawgApiDtos.GameDetails → Game entity
     * Para: guardar juego desde RAWG en BD
     */
    public Game rawgDetailsToGameEntity(RawgApiDtos.GameDetails rawgGame) {
        if (rawgGame == null) {
            return null;
        }

        Game game = new Game();

        // IDs y slug
        game.setRawgId(rawgGame.getId());
        game.setSlug(rawgGame.getSlug());

        // Datos básicos
        game.setName(rawgGame.getName());
        game.setDescription(rawgGame.getDescription());
        game.setDescriptionRaw(rawgGame.getDescriptionRaw());
        game.setBackgroundImage(rawgGame.getBackgroundImage());
        game.setBackgroundImageAlt(rawgGame.getBackgroundImageAdditional());

        // Ratings
        game.setRating(rawgGame.getRating());
        game.setRatingTop(rawgGame.getRatingTop());

        // Metadata
        game.setReleased(rawgGame.getReleased());
        game.setWebsite(rawgGame.getWebsite());
        game.setMetacriticScore(rawgGame.getMetacriticScore());
        game.setAveragePlaytime(rawgGame.getAveragePlaytime());

        // Listas (convertir listas RAWG → strings con comas)
        game.setPlatforms(extractPlatformNamesAsString(rawgGame.getPlatforms()));
        game.setGenres(extractGenreNamesAsString(rawgGame.getGenres()));
        game.setDevelopers(extractDeveloperNamesAsString(rawgGame.getDevelopers()));
        game.setPublishers(extractPublisherNamesAsString(rawgGame.getPublishers()));
        game.setTags(extractTagNamesAsString(rawgGame.getTags()));

        // Screenshots (por ahora null, implementar después con endpoint específico)
        game.setScreenshots(null);

        // Control de datos
        game.setDataSource("RAWG");
        game.setLastExternalUpdate(LocalDateTime.now());

        return game;
    }

    // ===== CONVERSIONES: LISTAS =====

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

    /**
     * Convierte lista de UserGameDetails → lista de OtherUserReview
     */
    public List<GameResponseDtos.OtherUserReview> userGameDetailsListToOtherReviews(List<UserGameDetails> ugdList) {
        if (ugdList == null) {
            return List.of();
        }

        return ugdList.stream()
                .filter(UserGameDetails::hasReview)
                .map(this::userGameDetailsToOtherReview)
                .collect(Collectors.toList());
    }

    /**
     * Convierte lista de UserGameDetails → lista de MyReviewDto
     */
    public List<GameResponseDtos.MyReviewDto> userGameDetailsListToMyReviews(List<UserGameDetails> ugdList) {
        if (ugdList == null) {
            return List.of();
        }

        return ugdList.stream()
                .filter(UserGameDetails::hasReview)
                .map(this::userGameDetailsToMyReview)
                .collect(Collectors.toList());
    }

    // ===== MÉTODOS DE UTILIDAD PARA CONVERSIONES =====

    /**
     * Convierte string con comas → lista de strings
     * "PC,PlayStation 4,Xbox One" → ["PC", "PlayStation 4", "Xbox One"]
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
     * Convierte lista de strings → string con comas
     * ["PC", "PlayStation 4", "Xbox One"] → "PC,PlayStation 4,Xbox One"
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
     * Extrae nombres de tags de RAWG como string con comas
     */
    private String extractTagNamesAsString(List<RawgApiDtos.Tag> tags) {
        if (tags == null) {
            return null;
        }

        List<String> names = tags.stream()
                .map(RawgApiDtos.Tag::getName)
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

    /**
     * Crea descripción corta para search items
     */
    private String createShortDescription(String gameName) {
        return gameName != null ?
                "Juego: " + gameName :
                "Sin descripción disponible";
    }
}