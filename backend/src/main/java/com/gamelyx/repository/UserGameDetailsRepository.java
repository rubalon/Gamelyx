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

    // ===== ESTADÍSTICAS DE JUEGO =====

    /**
     * Calcula rating promedio de comunidad para un juego
     */
    @Query("SELECT AVG(ugd.rating) FROM UserGameDetails ugd " +
            "WHERE ugd.game.id = :gameId AND ugd.rating IS NOT NULL")
    Optional<Double> findGameAverageRating(@Param("gameId") UUID gameId);

    /**
     * Cuenta usuarios que han dado rating a un juego específico
     */
    @Query("SELECT COUNT(ugd) FROM UserGameDetails ugd " +
            "WHERE ugd.game.id = :gameId AND ugd.rating IS NOT NULL")
    Long countByGameIdAndRatingNotNull(@Param("gameId") UUID gameId);

    /**
     * Obtiene los juegos preferidos de un usuario (rating >= 7).
     * Ordenados por rating descendente para mostrar los mejor valorados primero.
     *
     * @param userId ID del usuario
     * @return Lista de UserGameDetails con rating >= 7
     */
    @Query("SELECT ugd FROM UserGameDetails ugd " +
            "JOIN FETCH ugd.game " +
            "WHERE ugd.user.id = :userId " +
            "AND ugd.rating >= 7 " +
            "ORDER BY ugd.rating DESC")
    List<UserGameDetails> findPreferredGamesByUserId(@Param("userId") UUID userId);

    /**
     * QUERY OPTIMIZADA: Una sola consulta que hace todo:
     * - Busca usuarios en rango min-max
     * - Excluye usuario actual
     * - Excluye amigos existentes
     * - Excluye usuarios con solicitudes pendientes
     * - Incluye JOIN FETCH para evitar N+1
     * - Ordena por rating DESC para mejor performance en búsqueda progresiva
     */
    @Query("SELECT ugd FROM UserGameDetails ugd " +
            "JOIN FETCH ugd.user u " +
            "WHERE ugd.game.id = :gameId " +
            "AND ugd.user.id != :currentUserId " +
            "AND ugd.rating BETWEEN :minRating AND :maxRating " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Friendship f " +
            "    WHERE (f.user.id = :currentUserId AND f.friend.id = u.id)" +
            ") " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM FriendRequest fr " +
            "    WHERE fr.status = 'PENDING' " +
            "    AND ((fr.sender.id = :currentUserId AND fr.receiver.id = u.id) " +
            "         OR (fr.sender.id = u.id AND fr.receiver.id = :currentUserId))" +
            ") " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM SuggestionRejection sr " +
            "    WHERE sr.user.id = :currentUserId " +
            "    AND sr.rejectedUser.id = u.id " +
            "    AND sr.game.id = :gameId" +
            ") " +
            "ORDER BY ugd.rating DESC")
    List<UserGameDetails> findAvailableCandidatesForSuggestion(
            @Param("currentUserId") UUID currentUserId,
            @Param("gameId") UUID gameId,
            @Param("minRating") int minRating,
            @Param("maxRating") int maxRating);
}