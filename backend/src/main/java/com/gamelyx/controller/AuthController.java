package com.gamelyx.controller;

import com.gamelyx.dto.AuthDtos.RegisterRequest;
import com.gamelyx.dto.AuthDtos.RegisterResponse;
import com.gamelyx.dto.AuthDtos.LoginRequest;
import com.gamelyx.dto.AuthDtos.AuthResponse;
import com.gamelyx.dto.AuthDtos.VerifyEmailRequest;
import com.gamelyx.dto.AuthDtos.GoogleAuthRequest;
import com.gamelyx.dto.AuthDtos.RefreshTokenRequest;
import com.gamelyx.service.AuthService;
import com.gamelyx.service.ResendEmailService;
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
    private final ResendEmailService resendEmailService;

    public AuthController(AuthService authService, ResendEmailService resendEmailService) {
        this.authService = authService;
        this.resendEmailService = resendEmailService;
    }

    /**
     * Endpoint para registrar nuevos usuarios
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {

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

    // =====================================================
    // GOOGLE AUTH ENDPOINT ÚNICO
    // =====================================================

    /**
     * Endpoint único para autenticación con Google
     * POST /api/auth/google
     * Maneja tanto login como registro automáticamente
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        try {
            AuthResponse response = authService.authenticateWithGoogle(request);
            return ResponseEntity.ok(response);

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
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            if (request.refreshToken() == null || request.refreshToken().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Refresh token requerido"));
            }

            AuthResponse response = authService.refreshToken(request.refreshToken());
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
            if (email == null || !email.contains("@")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email inválido"));
            }

            resendEmailService.sendTestEmail(email);

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

    // =====================================================
    // MÉTODOS DE UTILIDAD
    // =====================================================

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
}