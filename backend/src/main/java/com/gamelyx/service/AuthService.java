// src/main/java/com/gamelyx/service/AuthService.java
package com.gamelyx.service;

import com.gamelyx.dto.AuthDtos.RegisterRequest;
import com.gamelyx.dto.AuthDtos.RegisterResponse;
import com.gamelyx.dto.AuthDtos.LoginRequest;
import com.gamelyx.dto.AuthDtos.AuthResponse;
import com.gamelyx.entity.User;
import com.gamelyx.repository.UserRepository;
import com.gamelyx.security.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmailService emailService;

    /**
     * Registra un nuevo usuario SIN devolver JWT
     * El usuario debe verificar su email antes de poder autenticarse
     */
    public RegisterResponse register(RegisterRequest request) {
        // Validar que el usuario no existe
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("El username ya está en uso");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear nuevo usuario
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        // Configurar verificación de email
        user.setEmailVerified(false);
        user.setEmailVerificationToken(UUID.randomUUID().toString());

        // Timestamps automáticos
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Guardar usuario
        User savedUser = userRepository.save(user);

        // Enviar email de verificación
        try {
            emailService.sendVerificationEmail(savedUser, savedUser.getEmailVerificationToken());
            System.out.println("Email de verificación enviado a: " + savedUser.getEmail());
        } catch (Exception e) {
            System.err.println("Error enviando email de verificación: " + e.getMessage());
        }

        // Devolver RegisterResponse sin tokens JWT
        return RegisterResponse.success(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                "Usuario registrado exitosamente. Te hemos enviado un email de verificación."
        );
    }

    /**
     * Login solo si el email está verificado
     */
    public AuthResponse login(LoginRequest request) {
        try {
            // Buscar usuario ANTES de autenticar para verificar email
            User user = userRepository.findByEmailOrUsername(request.usernameOrEmail())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Verificar que el email esté verificado
            if (!user.getEmailVerified()) {
                throw new RuntimeException("Debes verificar tu email antes de iniciar sesión. Revisa tu bandeja de entrada.");
            }

            // Autenticar usando Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.usernameOrEmail(),
                            request.password()
                    )
            );

            // Si llegamos aquí, la autenticación fue exitosa
            String username = authentication.getName();

            // Generar tokens JWT (email verificado)
            String accessToken = jwtUtil.generateToken(username);
            String refreshToken = jwtUtil.generateRefreshToken(username);

            // Actualizar último login
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    "Login exitoso"
            );

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
    }

    /**
     * Renueva un access token usando un refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        // Validar que es un refresh token válido
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Refresh token inválido");
        }

        // Extraer username del refresh token
        String username = jwtUtil.extractUsername(refreshToken);

        // Verificar que el usuario aún existe
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Generar nuevo access token
        String newAccessToken = jwtUtil.generateToken(username);

        return new AuthResponse(
                newAccessToken,
                refreshToken, // Reutilizamos el mismo refresh token
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                "Token renovado exitosamente"
        );
    }

    /**
     * Verifica un email usando el token de verificación
     */
    public String verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token de verificación inválido"));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null); // Limpiar token usado
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return "Email verificado exitosamente";
    }
}