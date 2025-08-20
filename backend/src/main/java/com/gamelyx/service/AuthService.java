// src/main/java/com/gamelyx/service/AuthService.java
package com.gamelyx.service;

import com.gamelyx.dto.AuthDtos.RegisterRequest;
import com.gamelyx.dto.AuthDtos.RegisterResponse;
import com.gamelyx.dto.AuthDtos.LoginRequest;
import com.gamelyx.dto.AuthDtos.AuthResponse;
import com.gamelyx.entity.User;
import com.gamelyx.repository.UserRepository;
import com.gamelyx.security.jwt.JwtUtil;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

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
            // 🔍 DEBUG MEJORADO: Inspección detallada del input
              String usernameOrEmail = request.usernameOrEmail();
              String password = request.password();
//
//            System.out.println("=== LOGIN DEBUG DETALLADO ===");
//            System.out.println("Original input: '" + usernameOrEmail + "'");
//            System.out.println("Length: " + usernameOrEmail.length());
//            System.out.println("Bytes: " + java.util.Arrays.toString(usernameOrEmail.getBytes()));
//            System.out.println("Chars: " + usernameOrEmail.chars()
//                    .mapToObj(c -> String.format("%c(%d)", c, c))
//                    .collect(java.util.stream.Collectors.joining(", ")));
//
//            // 🧹 APLICAR TRIM (pero mantener case original para usernames)
              String cleanInput = usernameOrEmail.trim();
//            System.out.println("Cleaned input: '" + cleanInput + "'");
//            System.out.println("Cleaned length: " + cleanInput.length());
//            System.out.println("Original == Cleaned: " + usernameOrEmail.equals(cleanInput));

            // Buscar usuario ANTES de autenticar para verificar email
            User user = userRepository.findByEmailOrUsername(cleanInput)
                    .orElseThrow(() -> {
//                        System.out.println("❌ Usuario no encontrado para input: '" + cleanInput + "'");
//
//                        // 🔍 DEBUG ADICIONAL: Listar todos los usuarios para comparar
//                        System.out.println("=== USUARIOS EN BD ===");
//                        userRepository.findAll().forEach(u -> {
//                            System.out.println("- Username: '" + u.getUsername() + "' | Email: '" + u.getEmail() + "'");
//                            System.out.println("  Username bytes: " + java.util.Arrays.toString(u.getUsername().getBytes()));
//                            System.out.println("  Email bytes: " + java.util.Arrays.toString(u.getEmail().getBytes()));
//                        });

                        return new RuntimeException("Usuario no encontrado");
                    });

            System.out.println("✅ Usuario encontrado: " + user.getUsername() + " (" + user.getEmail() + ")");

            // Verificar que el email esté verificado
            if (!user.getEmailVerified()) {
                System.out.println("❌ Email no verificado para usuario: " + user.getUsername());
                throw new RuntimeException("Debes verificar tu email antes de iniciar sesión. Revisa tu bandeja de entrada.");
            }

            System.out.println("✅ Email verificado, procediendo con autenticación...");

            // Autenticar usando Spring Security (usar input limpio)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            cleanInput, // 👈 Usar input limpio
                            password
                    )
            );

            // Si llegamos aquí, la autenticación fue exitosa
            String username = authentication.getName();
            System.out.println("✅ Autenticación exitosa para: " + username);

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
            System.out.println("❌ AuthenticationException: " + e.getMessage());
            throw new BadCredentialsException("Credenciales inválidas");
        } catch (Exception e) {
            System.out.println("❌ Exception en login: " + e.getMessage());
            e.printStackTrace();
            throw e;
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