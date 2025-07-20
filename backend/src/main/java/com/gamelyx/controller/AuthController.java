package com.gamelyx.controller;

import com.gamelyx.service.AuthService;
import com.gamelyx.service.AuthService.AuthResponse;
import com.gamelyx.service.AuthService.LoginRequest;
import com.gamelyx.service.AuthService.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:*"})
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Endpoint para registrar nuevos usuarios
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Validación básica de confirmación de contraseña
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Las contraseñas no coinciden"));
            }

            // Validación de longitud de contraseña (mínimo 6 caracteres como especifica el reporte)
            if (request.getPassword().length() < 6) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("La contraseña debe tener al menos 6 caracteres"));
            }

            // Registrar usuario
            AuthResponse response = authService.register(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    /**
     * Endpoint para login de usuarios
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Credenciales inválidas"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    /**
     * Endpoint para renovar access token usando refresh token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            if (request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Refresh token requerido"));
            }

            AuthResponse response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Refresh token inválido o expirado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    /**
     * Endpoint para verificar email
     * POST /api/auth/verify-email
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody EmailVerificationRequest request) {
        try {
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Token de verificación requerido"));
            }

            String message = authService.verifyEmail(request.getToken());
            return ResponseEntity.ok(createSuccessResponse(message));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    /**
     * Endpoint para logout (invalidar tokens)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Por ahora, con JWT stateless, el logout se maneja en el frontend
        // eliminando los tokens del almacenamiento local
        // En futuras fases se puede implementar blacklist de tokens
        return ResponseEntity.ok(createSuccessResponse("Logout exitoso"));
    }

    /**
     * Endpoint de salud para verificar que el servicio de auth funciona
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Authentication Service");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    // Métodos de utilidad para crear respuestas consistentes

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        response.put("success", "false");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }

    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put("success", "true");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }

    // DTOs para requests específicos del controller

    public static class RefreshTokenRequest {
        private String refreshToken;

        public RefreshTokenRequest() {}

        public RefreshTokenRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class EmailVerificationRequest {
        private String token;

        public EmailVerificationRequest() {}

        public EmailVerificationRequest(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}