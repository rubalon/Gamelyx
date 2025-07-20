package com.gamelyx.repository;

import com.gamelyx.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Método automático de Spring Data JPA - busca por username
    Optional<User> findByUsername(String username);

    // Método automático de Spring Data JPA - busca por email
    Optional<User> findByEmail(String email);

    // Método automático de Spring Data JPA - busca por Google ID
    Optional<User> findByGoogleId(String googleId);

    // Método automático - busca por token de verificación
    Optional<User> findByEmailVerificationToken(String token);

    // Verificar si existe un username (útil para validación)
    boolean existsByUsername(String username);

    // Verificar si existe un email (útil para validación)
    boolean existsByEmail(String email);

    // Verificar si existe un Google ID
    boolean existsByGoogleId(String googleId);

    // Query personalizada - buscar por email o username
    @Query("SELECT u FROM User u WHERE u.email = :emailOrUsername OR u.username = :emailOrUsername")
    Optional<User> findByEmailOrUsername(@Param("emailOrUsername") String emailOrUsername);

    // Query personalizada - buscar usuarios verificados
    @Query("SELECT u FROM User u WHERE u.emailVerified = true")
    java.util.List<User> findVerifiedUsers();

    // Query personalizada - contar usuarios verificados
    @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = true")
    long countVerifiedUsers();



}