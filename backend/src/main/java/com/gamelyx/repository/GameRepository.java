package com.gamelyx.repository;

import com.gamelyx.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository para la entidad Game
 *
 * Métodos básicos para buscar juegos por IDs externos, nombre y slug.
 */
@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

    // ===== BÚSQUEDAS POR APIs EXTERNAS =====

    /**
     * Busca un juego por su ID de RAWG
     */
    Optional<Game> findByRawgId(Integer rawgId);

    /**
     * Busca un juego por su Steam App ID
     */
    Optional<Game> findBySteamAppId(String steamAppId);

    /**
     * Verifica si existe un juego con el RAWG ID dado
     */
    boolean existsByRawgId(Integer rawgId);

    /**
     * Verifica si existe un juego con el Steam App ID dado
     */
    boolean existsBySteamAppId(String steamAppId);

    // ===== BÚSQUEDAS POR SLUG =====

    /**
     * Busca un juego por su slug (URLs amigables)
     * Ejemplo: findBySlug("minecraft") o findBySlug("grand-theft-auto-v")
     */
    Optional<Game> findBySlug(String slug);

    /**
     * Verifica si existe un juego con el slug dado
     * Útil para generar slugs únicos en GameService
     */
    boolean existsBySlug(String slug);

    // ===== BÚSQUEDAS POR NOMBRE =====

    /**
     * Busca juegos por nombre exacto (case insensitive)
     */
    Optional<Game> findByNameIgnoreCase(String name);

    /**
     * Busca juegos que contengan el texto en el nombre
     */
    @Query("SELECT g FROM Game g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Game> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
}