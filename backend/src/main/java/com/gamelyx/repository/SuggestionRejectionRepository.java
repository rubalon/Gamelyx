package com.gamelyx.repository;

import com.gamelyx.entity.SuggestionRejection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Repository para gestionar los rechazos de sugerencias de amistad.
 * Permite trackear qué usuarios fueron rechazados por qué juegos específicos.
 */
@Repository
public interface SuggestionRejectionRepository extends JpaRepository<SuggestionRejection, UUID> {

    // ================================================
    // CONSULTAS PRINCIPALES PARA SUGERENCIAS
    // ================================================

    /**
     * Verifica si ya existe un rechazo para esta combinación usuario-rechazado-juego.
     * Evita duplicados antes de crear nuevo rechazo.
     */
    boolean existsByUserIdAndRejectedUserIdAndGameId(UUID userId, UUID rejectedUserId, UUID gameId);


    /**
     * Obtiene rechazos de un usuario para un juego específico con detalles completos.
     */
    @Query("SELECT sr FROM SuggestionRejection sr " +
            "JOIN FETCH sr.rejectedUser " +
            "JOIN FETCH sr.game " +
            "WHERE sr.user.id = :userId AND sr.game.id = :gameId " +
            "ORDER BY sr.rejectedAt DESC")
    List<SuggestionRejection> findByUserAndGameWithDetails(@Param("userId") UUID userId,
                                                           @Param("gameId") UUID gameId);

    // ================================================
    // MÉTODOS DERIVADOS SIMPLES
    // ================================================

    /**
     * Busca rechazos por usuario y juego específico.
     */
    List<SuggestionRejection> findByUserIdAndGameId(UUID userId, UUID gameId);


}