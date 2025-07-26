package com.gamelyx.repository;

import com.gamelyx.entity.UserGameDetails;
import com.gamelyx.entity.UserGameDetails.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para UserGameDetails - Maneja la relación unificada usuario-juego
 *
 * Incluye consultas para:
 * - Biblioteca personal del usuario (por estado)
 * - Reviews y ratings del usuario
 * - Reviews públicas de un juego
 * - Estadísticas de usuario y juego
 */
@Repository
public interface UserGameDetailsRepository extends JpaRepository<UserGameDetails, UUID> {

    // ===== CONSULTAS BÁSICAS DE RELACIÓN =====

    /**
     * Busca la relación específica entre un usuario y un juego
     */
    Optional<UserGameDetails> findByUserIdAndGameId(UUID userId, UUID gameId);

    /**
     * Verifica si existe relación entre usuario y juego
     */
    boolean existsByUserIdAndGameId(UUID userId, UUID gameId);

    /**
     * Elimina la relación entre usuario y juego (hard delete para esta entidad)
     */
    void deleteByUserIdAndGameId(UUID userId, UUID gameId);

    // ===== BIBLIOTECA DEL USUARIO =====

    /**
     * Obtiene todos los juegos de un usuario filtrados por estado
     */
    @Query("SELECT ugd FROM UserGameDetails ugd JOIN FETCH ugd.game g " +
            "WHERE ugd.user.id = :userId AND ugd.status = :status")
    Page<UserGameDetails> findByUserIdAndStatus(@Param("userId") UUID userId,
                                                @Param("status") GameStatus status,
                                                Pageable pageable);

    /**
     * Obtiene todos los juegos de un usuario (sin filtro de estado)
     */
    @Query("SELECT ugd FROM UserGameDetails ugd JOIN FETCH ugd.game g " +
            "WHERE ugd.user.id = :userId")
    Page<UserGameDetails> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    // ===== REVIEWS DEL USUARIO =====

    /**
     * Obtiene todas las reviews de un usuario
     */
    @Query("SELECT ugd FROM UserGameDetails ugd JOIN FETCH ugd.game g " +
            "WHERE ugd.user.id = :userId AND ( ugd.reviewText IS NOT NULL) " +
            "ORDER BY ugd.reviewUpdatedAt DESC")
    Page<UserGameDetails> findUserReviews(@Param("userId") UUID userId, Pageable pageable);


    // ===== REVIEWS DE UN JUEGO =====

    /**
     * Obtiene todas las reviews de un juego específico
     */
    @Query("SELECT ugd FROM UserGameDetails ugd JOIN FETCH ugd.user u " +
            "WHERE ugd.game.id = :gameId AND ugd.reviewText IS NOT NULL " +
            "ORDER BY ugd.reviewUpdatedAt DESC")
    Page<UserGameDetails> findGamePublicReviews(@Param("gameId") UUID gameId, Pageable pageable);

    /**
     * Obtiene reviews de un juego con rating específico o superior
     */
    @Query("SELECT ugd FROM UserGameDetails ugd JOIN FETCH ugd.user u " +
            "WHERE ugd.game.id = :gameId AND ugd.rating >= :minRating " +
            "ORDER BY ugd.rating DESC, ugd.reviewUpdatedAt DESC")
    Page<UserGameDetails> findGameReviewsByMinRating(@Param("gameId") UUID gameId,
                                                     @Param("minRating") Integer minRating,
                                                     Pageable pageable);

    // ===== ESTADÍSTICAS DE USUARIO =====

    /**
     * Cuenta juegos por estado para un usuario
     */
    @Query("SELECT COUNT(ugd) FROM UserGameDetails ugd " +
            "WHERE ugd.user.id = :userId AND ugd.status = :status")
    Long countUserGamesByStatus(@Param("userId") UUID userId, @Param("status") GameStatus status);

    /**
     * Cuenta total de juegos del usuario
     */
    @Query("SELECT COUNT(ugd) FROM UserGameDetails ugd " +
            "WHERE ugd.user.id = :userId")
    Long countUserGames(@Param("userId") UUID userId);

    /**
     * Cuenta reviews escritas por un usuario
     */
    @Query("SELECT COUNT(ugd) FROM UserGameDetails ugd " +
            "WHERE ugd.user.id = :userId AND ( ugd.reviewText IS NOT NULL)")
    Long countUserReviews(@Param("userId") UUID userId);

    /**
     * Calcula rating promedio dado por un usuario
     */
    @Query("SELECT AVG(ugd.rating) FROM UserGameDetails ugd " +
            "WHERE ugd.user.id = :userId AND ugd.rating IS NOT NULL")
    Optional<Double> findUserAverageRating(@Param("userId") UUID userId);


    // ===== ESTADÍSTICAS DE JUEGO =====

    /**
     * Calcula rating promedio de comunidad para un juego
     */
    @Query("SELECT AVG(ugd.rating) FROM UserGameDetails ugd " +
            "WHERE ugd.game.id = :gameId AND ugd.rating IS NOT NULL")
    Optional<Double> findGameAverageRating(@Param("gameId") UUID gameId);

    /**
     * Cuenta total de reviews para un juego
     */
    @Query("SELECT COUNT(ugd) FROM UserGameDetails ugd " +
            "WHERE ugd.game.id = :gameId AND (ugd.rating IS NOT NULL OR ugd.reviewText IS NOT NULL)")
    Long countGameReviews(@Param("gameId") UUID gameId);

    /**
     * Cuenta usuarios que han dado rating a un juego específico
     */
    @Query("SELECT COUNT(ugd) FROM UserGameDetails ugd " +
            "WHERE ugd.game.id = :gameId AND ugd.rating IS NOT NULL")
    Long countByGameIdAndRatingNotNull(@Param("gameId") UUID gameId);

    /**
     * Cuenta usuarios que tienen el juego por cada estado
     */
    @Query("SELECT ugd.status, COUNT(ugd) FROM UserGameDetails ugd " +
            "WHERE ugd.game.id = :gameId AND ugd.status IS NOT NULL " +
            "GROUP BY ugd.status")
    List<Object[]> countGameUsersByStatus(@Param("gameId") UUID gameId);

    // ===== DESCUBRIMIENTO Y RECOMENDACIONES =====

    /**
     * Encuentra usuarios que también jugaron un juego específico
     */
    @Query("SELECT ugd FROM UserGameDetails ugd JOIN FETCH ugd.user u " +
            "WHERE ugd.game.id = :gameId AND ugd.user.id != :excludeUserId " +
            "AND ugd.status IN (:statuses)")
    List<UserGameDetails> findOtherUsersWhoPlayed(@Param("gameId") UUID gameId,
                                                  @Param("excludeUserId") UUID excludeUserId,
                                                  @Param("statuses") List<GameStatus> statuses);

    // ===== BÚSQUEDAS AVANZADAS =====

    /**
     * Busca en biblioteca del usuario por nombre de juego
     */
    @Query("SELECT ugd FROM UserGameDetails ugd JOIN ugd.game g " +
            "WHERE ugd.user.id = :userId AND LOWER(g.name) LIKE LOWER(CONCAT('%', :gameName, '%'))")
    Page<UserGameDetails> searchUserLibraryByGameName(@Param("userId") UUID userId,
                                                      @Param("gameName") String gameName,
                                                      Pageable pageable);

    /**
     * Encuentra juegos completados recientemente por un usuario
     */
    @Query("SELECT ugd FROM UserGameDetails ugd JOIN FETCH ugd.game g " +
            "WHERE ugd.user.id = :userId AND ugd.status = 'COMPLETED' " +
            "AND ugd.completedAt IS NOT NULL " +
            "ORDER BY ugd.completedAt DESC")
    List<UserGameDetails> findRecentlyCompletedGames(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Encuentra mejores reviews de un usuario (rating alto + texto)
     */
    @Query("SELECT ugd FROM UserGameDetails ugd JOIN FETCH ugd.game g " +
            "WHERE ugd.user.id = :userId AND ugd.rating >= :minRating " +
            "AND ugd.reviewText IS NOT NULL AND LENGTH(ugd.reviewText) >= :minTextLength " +
            "ORDER BY ugd.rating DESC, LENGTH(ugd.reviewText) DESC")
    List<UserGameDetails> findUserTopReviews(@Param("userId") UUID userId,
                                             @Param("minRating") Integer minRating,
                                             @Param("minTextLength") Integer minTextLength,
                                             Pageable pageable);
}