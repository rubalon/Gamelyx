package com.gamelyx.repository;

import com.gamelyx.entity.Friendship;
import com.gamelyx.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para gestionar las amistades confirmadas.
 * Maneja el enfoque bidireccional donde cada amistad son 2 registros.
 */
@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    // ================================================
    // CONSULTAS PRINCIPALES PARA HOME SOCIAL
    // ================================================

    /**
     * Obtiene la lista completa de amigos de un usuario.
     * Con información del usuario amigo cargada (JOIN FETCH).
     * Usado en el home social para mostrar la lista de amigos.
     */
    @Query("SELECT f.friend FROM Friendship f " +
            "WHERE f.user.id = :userId " +
            "ORDER BY f.friend.username ASC")
    List<User> findFriendsByUserId(@Param("userId") UUID userId);

    /**
     * Obtiene las amistades completas (con metadatos) de un usuario.
     * Incluye timestamp de cuándo se hicieron amigos.
     */
    @Query("SELECT f FROM Friendship f " +
            "JOIN FETCH f.friend " +
            "WHERE f.user.id = :userId " +
            "ORDER BY f.createdAt DESC")
    List<Friendship> findFriendshipsWithMetadataByUserId(@Param("userId") UUID userId);

    // ================================================
    // CONSULTAS PARA VERIFICACIONES DE AMISTAD
    // ================================================

    /**
     * Verifica si dos usuarios son amigos mutuos.
     * Verifica la dirección user -> friend (la otra dirección debe existir también).
     */
    @Query("SELECT COUNT(f) > 0 FROM Friendship f " +
            "WHERE f.user.id = :userId AND f.friend.id = :friendId")
    boolean areUsersFriends(@Param("userId") UUID userId, @Param("friendId") UUID friendId);

    /**
     * Verifica amistad bidireccional completa.
     * Confirma que existen ambas direcciones de la amistad.
     */
    @Query("SELECT COUNT(f) = 2 FROM Friendship f " +
            "WHERE (f.user.id = :userId1 AND f.friend.id = :userId2) " +
            "OR (f.user.id = :userId2 AND f.friend.id = :userId1)")
    boolean existsBidirectionalFriendship(@Param("userId1") UUID userId1,
                                          @Param("userId2") UUID userId2);

    /**
     * Busca la amistad específica en una dirección.
     * Para obtener metadatos como cuándo se hicieron amigos.
     */
    @Query("SELECT f FROM Friendship f " +
            "WHERE f.user.id = :userId AND f.friend.id = :friendId")
    Friendship findByUserIdAndFriendId(@Param("userId") UUID userId, @Param("friendId") UUID friendId);

    // ================================================
    // CONSULTAS PARA ESTADÍSTICAS
    // ================================================

    /**
     * Cuenta el número total de amigos de un usuario.
     * Para mostrar en el home: "Amigos (X)".
     */
    long countByUserId(UUID userId);

    /**
     * Obtiene los amigos más recientes de un usuario.
     * Para mostrar "Amigos recientes" o notificaciones.
     */
    @Query("SELECT f.friend FROM Friendship f " +
            "WHERE f.user.id = :userId " +
            "ORDER BY f.createdAt DESC " +
            "LIMIT :limit")
    List<User> findRecentFriendsByUserId(@Param("userId") UUID userId, @Param("limit") int limit);

    /**
     * Obtiene usuarios con más amigos en común.
     * Útil para sugerencias de "amigos de amigos".
     */
    @Query("SELECT f2.friend, COUNT(f2.friend) as mutualCount " +
            "FROM Friendship f1 " +
            "JOIN Friendship f2 ON f1.friend.id = f2.user.id " +
            "WHERE f1.user.id = :userId " +
            "AND f2.friend.id != :userId " +
            "AND f2.friend.id NOT IN (" +
            "    SELECT f3.friend.id FROM Friendship f3 WHERE f3.user.id = :userId" +
            ") " +
            "GROUP BY f2.friend " +
            "ORDER BY mutualCount DESC")
    List<Object[]> findMutualFriendsSuggestions(@Param("userId") UUID userId);

    // ================================================
    // OPERACIONES DE GESTIÓN DE AMISTADES
    // ================================================

    /**
     * Elimina amistad bidireccional completa.
     * Elimina ambas direcciones: A->B y B->A
     */
    @Modifying
    @Query("DELETE FROM Friendship f " +
            "WHERE (f.user.id = :userId1 AND f.friend.id = :userId2) " +
            "OR (f.user.id = :userId2 AND f.friend.id = :userId1)")
    void deleteBidirectionalFriendship(@Param("userId1") UUID userId1,
                                       @Param("userId2") UUID userId2);

    /**
     * Elimina todas las amistades de un usuario.
     * Para cuando se elimina una cuenta de usuario.
     */
    @Modifying
    @Query("DELETE FROM Friendship f " +
            "WHERE f.user.id = :userId OR f.friend.id = :userId")
    void deleteAllFriendshipsForUser(@Param("userId") UUID userId);

    // ================================================
    // CONSULTAS PARA SUGERENCIAS DE AMIGOS (HU-20)
    // ================================================

    /**
     * Obtiene amigos de un usuario excluyendo al usuario actual.
     * Para sugerencias de "amigos de amigos".
     */
    @Query("SELECT DISTINCT f2.friend FROM Friendship f1 " +
            "JOIN Friendship f2 ON f1.friend.id = f2.user.id " +
            "WHERE f1.user.id = :userId " +
            "AND f2.friend.id != :userId " +
            "AND f2.friend.id NOT IN (" +
            "    SELECT f3.friend.id FROM Friendship f3 WHERE f3.user.id = :userId" +
            ")")
    List<User> findFriendsOfFriends(@Param("userId") UUID userId);

    /**
     * Obtiene lista de usuarios que NO son amigos del usuario actual.
     * Para filtrar en algoritmos de sugerencias.
     */
    @Query("SELECT u FROM User u " +
            "WHERE u.id != :userId " +
            "AND u.id NOT IN (" +
            "    SELECT f.friend.id FROM Friendship f WHERE f.user.id = :userId" +
            ")")
    List<User> findNonFriends(@Param("userId") UUID userId);

    // ================================================
    // CONSULTAS PARA ANALYTICS Y REPORTES
    // ================================================

    /**
     * Estadísticas de amistades por rango de fechas.
     * Para analytics de crecimiento de la red social.
     */
    @Query("SELECT DATE(f.createdAt), COUNT(f) FROM Friendship f " +
            "WHERE f.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(f.createdAt) " +
            "ORDER BY DATE(f.createdAt)")
    List<Object[]> getFriendshipStatsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Usuarios con más amigos (rankings).
     * Para rankings y gamificación.
     */
    @Query("SELECT f.user, COUNT(f.friend) as friendCount " +
            "FROM Friendship f " +
            "GROUP BY f.user " +
            "ORDER BY friendCount DESC")
    List<Object[]> getUsersByFriendCount();

    // ================================================
    // MÉTODOS DERIVADOS SIMPLES
    // ================================================

    /**
     * Busca todas las amistades donde el usuario aparece como 'user'.
     */
    List<Friendship> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Busca todas las amistades donde el usuario aparece como 'friend'.
     * Para verificaciones o análisis bidireccionales.
     */
    List<Friendship> findByFriendIdOrderByCreatedAtDesc(UUID friendId);

    /**
     * Verifica si existe amistad en una dirección específica.
     */
    boolean existsByUserIdAndFriendId(UUID userId, UUID friendId);

    /**
     * Busca amistades creadas después de una fecha específica.
     */
    List<Friendship> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);
}