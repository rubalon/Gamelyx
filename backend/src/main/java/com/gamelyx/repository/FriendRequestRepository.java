package com.gamelyx.repository;

import com.gamelyx.entity.FriendRequest;
import com.gamelyx.entity.FriendRequest.FriendRequestStatus;
import com.gamelyx.entity.FriendRequest.RequestSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para gestionar las solicitudes de amistad.
 * Incluye consultas optimizadas para el home social y gestión de solicitudes.
 */
@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    // ================================================
    // CONSULTAS PARA EL HOME SOCIAL (Single Endpoint)
    // ================================================

    interface FriendRequestProjection {
        UUID getRequestId();
        String getContactUsername();
        UUID getContactId();
        FriendRequest.RequestSource getRequestSource();
        FriendRequest.FriendRequestStatus getStatus();
        String getGameSlug();
        String getGameName();
        Integer getYourRating();
        Integer getTheirRating();
        LocalDateTime getReceivedAt();
    }

    // ================================================
    // QUERYs QUE USAn LA INTERFACE
    // ================================================

    @Query("SELECT " +
            "fr.id as requestId, " +
            "fr.sender.username as contactUsername, " +
            "fr.sender.id as contactId, " +
            "fr.source as requestSource, " +
            "fr.status as status, " +
            "fr.sharedGame.slug as gameSlug, " +
            "fr.sharedGame.name as gameName, " +
            "receiverGameDetails.rating as yourRating, " +
            "senderGameDetails.rating as theirRating, " +
            "fr.createdAt as receivedAt " +
            "FROM FriendRequest fr " +
            "LEFT JOIN UserGameDetails receiverGameDetails ON " +
            "    receiverGameDetails.user = fr.receiver " +
            "    AND receiverGameDetails.game = fr.sharedGame " +
            "LEFT JOIN UserGameDetails senderGameDetails ON " +
            "    senderGameDetails.user = fr.sender " +
            "    AND senderGameDetails.game = fr.sharedGame " +
            "WHERE fr.receiver.id = :userId " +
            "AND fr.status = :status " +
            "ORDER BY fr.createdAt DESC")
    List<FriendRequestProjection> findPendingRequestsReceivedBy(
            @Param("userId") UUID userId,
            @Param("status") FriendRequestStatus status);

    /**
     * Obtiene solicitudes enviadas que necesitan atención del sender:
     * - PENDING: Aún sin respuesta
     * - ACCEPTED
     * - REJECTED: Respondidas pero sender no notificado
     */
    @Query("SELECT " +
            "fr.id as requestId, " +
            "fr.receiver.username as contactUsername, " +
            "fr.receiver.id as contactId, " +
            "fr.source as requestSource, " +
            "fr.status as status, " +
            "fr.sharedGame.slug as gameSlug, " +
            "fr.sharedGame.name as gameName, " +
            "senderGameDetails.rating as yourRating, " +
            "receiverGameDetails.rating as theirRating, " +
            "fr.createdAt as receivedAt " +
            "FROM FriendRequest fr " +
            "LEFT JOIN UserGameDetails senderGameDetails ON " +
            "    senderGameDetails.user = fr.sender " +
            "    AND senderGameDetails.game = fr.sharedGame " +
            "LEFT JOIN UserGameDetails receiverGameDetails ON " +
            "    receiverGameDetails.user = fr.receiver " +
            "    AND receiverGameDetails.game = fr.sharedGame " +
            "WHERE fr.sender.id = :userId " +
            "AND (fr.status IN ('PENDING', 'ACCEPTED') OR " +
            "     (fr.status = 'REJECTED' AND " +
            "      fr.isSenderNotified = false)) " +
            "ORDER BY fr.createdAt DESC")
    List<FriendRequestProjection> findRequestsSentBy(@Param("userId") UUID userId);

    // ================================================
    // CONSULTAS PARA VALIDACIONES DE NEGOCIO
    // ================================================

    /**
     * Verifica si existe una solicitud PENDIENTE entre dos usuarios (en cualquier dirección).
     * Evita duplicados antes de crear nueva solicitud.
     */
    @Query("SELECT COUNT(fr) > 0 FROM FriendRequest fr " +
            "WHERE fr.status = 'PENDING' " +
            "AND ((fr.sender.id = :userId1 AND fr.receiver.id = :userId2) " +
            "     OR (fr.sender.id = :userId2 AND fr.receiver.id = :userId1))")
    boolean existsPendingRequestBetween(@Param("userId1") UUID userId1,
                                        @Param("userId2") UUID userId2);

    /**
     * Busca una solicitud específica entre dos usuarios con estado dado.
     * Usado para aceptar/rechazar solicitudes.
     */
    @Query("SELECT fr FROM FriendRequest fr " +
            "WHERE fr.sender.id = :senderId " +
            "AND fr.receiver.id = :receiverId " +
            "AND fr.status = :status")
    Optional<FriendRequest> findBySenderAndReceiverAndStatus(@Param("senderId") UUID senderId,
                                                             @Param("receiverId") UUID receiverId,
                                                             @Param("status") FriendRequestStatus status);

    // ================================================
    // CONSULTAS PARA ANALYTICS Y ESTADÍSTICAS
    // ================================================

    /**
     * Cuenta solicitudes pendientes recibidas por un usuario.
     * Para mostrar badges de notificación.
     */
    long countByReceiverIdAndStatus(UUID receiverId, FriendRequestStatus status);

    /**
     * Cuenta solicitudes pendientes enviadas por un usuario.
     * Para evitar spam de solicitudes.
     */
    long countBySenderIdAndStatus(UUID senderId, FriendRequestStatus status);

    /**
     * Obtiene solicitudes de un usuario por origen (SEARCH vs SUGGESTION).
     * Para analytics de cómo se conocen los usuarios.
     */
    @Query("SELECT fr FROM FriendRequest fr " +
            "WHERE fr.sender.id = :userId " +
            "AND fr.source = :source " +
            "ORDER BY fr.createdAt DESC")
    List<FriendRequest> findBySenderAndSource(@Param("userId") UUID userId,
                                              @Param("source") RequestSource source);

    // ================================================
    // CONSULTAS PARA SUGERENCIAS (HU-20)
    // ================================================

    /**
     * Obtiene todas las solicitudes originadas por sugerencias de un juego específico.
     * Útil para medir efectividad de sugerencias por juego.
     */
    @Query("SELECT fr FROM FriendRequest fr " +
            "JOIN FETCH fr.sender " +
            "JOIN FETCH fr.receiver " +
            "WHERE fr.sharedGame.id = :gameId " +
            "AND fr.source = 'SUGGESTION' " +
            "ORDER BY fr.createdAt DESC")
    List<FriendRequest> findSuggestionsByGame(@Param("gameId") UUID gameId);

    /**
     * Verifica si ya se envió una solicitud por sugerencia para un juego específico.
     * Evita spam de sugerencias del mismo juego.
     */
    @Query("SELECT COUNT(fr) > 0 FROM FriendRequest fr " +
            "WHERE fr.sender.id = :senderId " +
            "AND fr.receiver.id = :receiverId " +
            "AND fr.sharedGame.id = :gameId " +
            "AND fr.source = 'SUGGESTION'")
    boolean existsSuggestionForGame(@Param("senderId") UUID senderId,
                                    @Param("receiverId") UUID receiverId,
                                    @Param("gameId") UUID gameId);

    /**
     * Busca solicitudes PENDIENTES entre dos usuarios (en ambas direcciones).
     * Para limpieza al eliminar amistad.
     */
    @Query("SELECT fr FROM FriendRequest fr " +
            "WHERE fr.status = 'PENDING' " +
            "AND ((fr.sender.id = :userId1 AND fr.receiver.id = :userId2) " +
            "     OR (fr.sender.id = :userId2 AND fr.receiver.id = :userId1))")
    List<FriendRequest> findPendingRequestsBetweenUsers(@Param("userId1") UUID userId1,
                                                        @Param("userId2") UUID userId2);

    /**
     * Elimina solicitudes rechazadas más antiguas que una fecha.
     * Para mantener la BD limpia.
     */
    void deleteByStatusAndRespondedAtBefore(FriendRequestStatus status, LocalDateTime cutoffDate);

    // ================================================
    // MÉTODOS DERIVADOS SIMPLES
    // ================================================

    /**
     * Busca solicitudes por receptor y estado.
     * Método base para notificaciones.
     */
    List<FriendRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(UUID receiverId,
                                                                      FriendRequestStatus status);

    /**
     * Busca solicitudes por emisor y estado.
     * Método base para seguimiento de solicitudes enviadas.
     */
    List<FriendRequest> findBySenderIdAndStatusOrderByCreatedAtDesc(UUID senderId,
                                                                    FriendRequestStatus status);

    /**
     * Verifica existencia de solicitud específica.
     * Para validaciones rápidas antes de crear nueva solicitud.
     */
    boolean existsBySenderIdAndReceiverIdAndStatus(UUID senderId,
                                                   UUID receiverId,
                                                   FriendRequestStatus status);
}