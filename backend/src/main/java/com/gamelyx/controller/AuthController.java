package com.gamelyx.controller;

import com.gamelyx.dto.AuthDtos.RegisterRequest;
import com.gamelyx.dto.AuthDtos.RegisterResponse;
import com.gamelyx.dto.AuthDtos.LoginRequest;
import com.gamelyx.dto.AuthDtos.AuthResponse;
import com.gamelyx.dto.AuthDtos.VerifyEmailRequest;
import com.gamelyx.service.AuthService;
import com.gamelyx.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    public AuthController(AuthService authService, EmailService emailService) {
        this.authService = authService;
        this.emailService = emailService;
    }

    /**
     * Endpoint para registrar nuevos usuarios
     * POST /api/auth/register
     * CORREGIDO: Ahora devuelve RegisterResponse sin JWT
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Validación básica de confirmación de contraseña
            if (!request.password().equals(request.confirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Las contraseñas no coinciden"));
            }

            // Validación de longitud de contraseña (mínimo 6 caracteres)
            if (request.password().length() < 6) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("La contraseña debe tener al menos 6 caracteres"));
            }

            // Registrar usuario - CORREGIDO: Devuelve RegisterResponse
            RegisterResponse response = authService.register(request);

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
     * MEJORADO: Verifica email antes de generar JWT
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
     * MEJORADO: Usa VerifyEmailRequest del AuthDtos
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            String message = authService.verifyEmail(request.token());
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

    /**
     * Endpoint de testing para verificar el sistema de email
     * GET /api/auth/test-email?email=tu@email.com
     */
    @GetMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestParam String email) {
        try {
            // Validación básica del email
            if (email == null || !email.contains("@")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email inválido"));
            }

            emailService.sendTestEmail(email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email de prueba enviado correctamente");
            response.put("email", email);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error enviando email de prueba: " + e.getMessage());
            errorResponse.put("email", email);
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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
}