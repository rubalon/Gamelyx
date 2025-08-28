// src/main/java/com/gamelyx/dto/AuthDtos.java
package com.gamelyx.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTOs para operaciones de autenticación
 * Agrupa todos los records y clases relacionadas con auth
 */
public class AuthDtos {

    /**
     * Request para registro de usuario
     */
    public record RegisterRequest(
            @NotBlank(message = "El username es obligatorio")
            @Size(min = 3, max = 20, message = "El username debe tener entre 3 y 20 caracteres")
            String username,

            @NotBlank(message = "El email es obligatorio")
            @Email(message = "El formato del email es inválido")
            String email,

            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
            String password,

            @NotBlank(message = "Confirmar contraseña es obligatorio")
            String confirmPassword
    ) {}

    /**
     * Request para login de usuario
     */
    public record LoginRequest(
            @NotBlank(message = "Email o username es obligatorio")
            String usernameOrEmail,

            @NotBlank(message = "La contraseña es obligatoria")
            String password
    ) {}

    /**
     * Request para autenticación con Google (login o registro automático)
     */
    public record GoogleAuthRequest(
            @NotBlank(message = "El Google ID es obligatorio")
            String googleId,

            @NotBlank(message = "El email es obligatorio")
            @Email(message = "El formato del email es inválido")
            String email,

            @NotBlank(message = "El name es obligatorio")
            String name
    ) {}

    /**
     * Respuesta del registro que NO incluye JWT tokens
     * El usuario debe verificar su email antes de poder autenticarse
     */
    public record RegisterResponse(
            UUID userId,
            String username,
            String email,
            String message,
            boolean emailVerificationRequired
    ) {
        /**
         * Constructor helper para crear respuesta de registro exitoso
         */
        public static RegisterResponse success(UUID userId, String username, String email, String message) {
            return new RegisterResponse(userId, username, email, message, true);
        }
    }

    /**
     * Respuesta de autenticación que SÍ incluye JWT tokens
     * Solo se devuelve en login exitoso con email verificado
     */
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            UUID userId,
            String username,
            String email,
            String message
    ) {}

    /**
     * Request para verificación de email
     */
    public record VerifyEmailRequest(
            @NotBlank(message = "El token de verificación es obligatorio")
            String token
    ) {}

    /**
     * Request para refresh token
     */
    public record RefreshTokenRequest(
            @NotBlank(message = "El refresh token es obligatorio")
            String refreshToken
    ) {}
}