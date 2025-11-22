// src/main/java/com/gamelyx/service/AuthService.java
package com.gamelyx.service;

import com.gamelyx.dto.AuthDtos.RegisterRequest;
import com.gamelyx.dto.AuthDtos.RegisterResponse;
import com.gamelyx.dto.AuthDtos.LoginRequest;
import com.gamelyx.dto.AuthDtos.AuthResponse;
import com.gamelyx.dto.AuthDtos.GoogleAuthRequest;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final IEmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       IEmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    /**
     * NUEVO: Autenticación única con Google
     * Maneja automáticamente login o registro según si el usuario existe
     */
    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request) {
        System.out.println("=== GOOGLE AUTH INICIADO ===");
        System.out.println("Google ID: " + request.googleId());
        System.out.println("Email: " + request.email());
        System.out.println("Name: " + request.name());

        try {
            // Paso 1: Buscar por Google ID (usuario existente de Google)
            Optional<User> existingGoogleUser = userRepository.findByGoogleId(request.googleId());

            if (existingGoogleUser.isPresent()) {
                System.out.println("Usuario Google existente encontrado: " + existingGoogleUser.get().getUsername());
                return loginExistingGoogleUser(existingGoogleUser.get());
            }

            // Paso 2: Verificar si el email ya existe con contraseña
            Optional<User> existingEmailUser = userRepository.findByEmail(request.email());

            if (existingEmailUser.isPresent()) {
                // Si el usuario ya existe pero sin Google ID, es una cuenta tradicional
                if (existingEmailUser.get().getGoogleId() == null) {
                    throw new RuntimeException(
                            "Este email ya está registrado con contraseña. " +
                                    "Por favor, inicia sesión con tu contraseña."
                    );
                }

                // Si tiene Google ID pero diferente, error de consistencia
                throw new RuntimeException("Error de consistencia en la cuenta de Google");
            }

            // Paso 3: Crear nuevo usuario con Google
            System.out.println("Creando nuevo usuario desde Google...");
            return createNewGoogleUser(request);

        } catch (Exception e) {
            System.err.println("Error en autenticación Google: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Login de usuario Google existente
     */
    private AuthResponse loginExistingGoogleUser(User user) {
        System.out.println("Haciendo login de usuario Google existente: " + user.getUsername());

        // Generar tokens JWT
        String accessToken = jwtUtil.generateToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Actualizar último acceso
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                "Login con Google exitoso"
        );
    }

    /**
     * Crear nuevo usuario desde Google
     */
    private AuthResponse createNewGoogleUser(GoogleAuthRequest request) {
        // Generar username único
        String uniqueUsername = generateUniqueUsername(request.name());
        System.out.println("Username generado: " + uniqueUsername);

        // Crear usuario
        User newUser = new User();
        newUser.setUsername(uniqueUsername);
        newUser.setEmail(request.email());
        newUser.setPasswordHash(null); // Sin contraseña para usuarios de Google
        newUser.setGoogleId(request.googleId());
        newUser.setEmailVerified(true); // Google emails están verificados
        newUser.setEmailVerificationToken(null);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        // Guardar usuario
        User savedUser = userRepository.save(newUser);
        System.out.println("Usuario Google creado exitosamente: " + savedUser.getUsername());

        // Generar tokens JWT
        String accessToken = jwtUtil.generateToken(savedUser.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getUsername());

        return new AuthResponse(
                accessToken,
                refreshToken,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                "Cuenta creada y login exitoso con Google"
        );
    }

    /**
     * Generar username único basado en el nombre de Google
     */
    private String generateUniqueUsername(String name) {
        // Limpiar el nombre: solo letras y números, sin espacios ni caracteres especiales
        // IMPORTANTE: Mantener las mayúsculas originales
        String cleanName = name.replaceAll("[^a-zA-Z0-9]", "")
                .trim();

        // Asegurar longitud mínima y máxima
        if (cleanName.isEmpty()) {
            cleanName = "User";
        }

        String baseUsername = cleanName.length() > 15 ? cleanName.substring(0, 15) : cleanName;

        // Si el base es muy corto, añadir padding manteniendo case
        if (baseUsername.length() < 3) {
            baseUsername = baseUsername + "User";
        }

        System.out.println("Base username (preservando mayúsculas): " + baseUsername);

        // Intentar primero el username base sin modificaciones
        if (!isUsernameExists(baseUsername)) {
            System.out.println("Username base disponible: " + baseUsername);
            return baseUsername;
        }

        // Si está ocupado, intentar con números aleatorios
        int attempts = 0;
        int bound = 99;
        String finalUsername;

        do {
            int randomSuffix = ThreadLocalRandom.current().nextInt(10, bound + 1);
            finalUsername = baseUsername + randomSuffix;
            attempts++;
            System.out.println("Intento " + attempts + ": " + finalUsername);

            if (attempts >= 5) {
                bound = 999;
            }

        } while (isUsernameExists(finalUsername) && attempts < 10);

        if (attempts >= 10) {
            throw new RuntimeException("No se pudo generar un username único después de 10 intentos para: " + baseUsername);
        }

        return finalUsername;
    }

    /**
     * Verificar si existe username (case-insensitive)
     */
    private boolean isUsernameExists(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    /**
     * Registra un nuevo usuario SIN devolver JWT
     * El usuario debe verificar su email antes de poder autenticarse
     */
    public RegisterResponse register(RegisterRequest request) {
        // Validar que el usuario no existe
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
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
            throw new RuntimeException("No se pudo enviar el email de verificación. Por favor, intenta registrarte nuevamente.", e);
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
            String usernameOrEmail = request.usernameOrEmail();
            String password = request.password();

            String cleanInput = usernameOrEmail.trim();

            // Buscar usuario ANTES de autenticar para verificar email
            User user = userRepository.findByEmailOrUsername(cleanInput)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            System.out.println("Usuario encontrado: " + user.getUsername() + " (" + user.getEmail() + ")");

            // Verificar que el email esté verificado
            if (!user.getEmailVerified()) {
                System.out.println("Email no verificado para usuario: " + user.getUsername());
                throw new RuntimeException("Debes verificar tu email antes de iniciar sesión. Revisa tu bandeja de entrada.");
            }

            System.out.println("Email verificado, procediendo con autenticación...");

            // Autenticar usando Spring Security (usar input limpio)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cleanInput, password)
            );

            // Si llegamos aquí, la autenticación fue exitosa
            String username = authentication.getName();
            System.out.println("Autenticación exitosa para: " + username);

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
            System.out.println("AuthenticationException: " + e.getMessage());
            throw new BadCredentialsException("Credenciales inválidas");
        } catch (Exception e) {
            System.out.println("Exception en login: " + e.getMessage());
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