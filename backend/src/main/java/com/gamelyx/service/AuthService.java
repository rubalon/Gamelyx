package com.gamelyx.service;

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
     * Registra un nuevo usuario en el sistema
     */
    public AuthResponse register(RegisterRequest request) {
        // Validar que el usuario no existe
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El username ya está en uso");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear nuevo usuario
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // Configurar verificación de email
        user.setEmailVerified(false); // Requerirá verificación
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
            // El registro continúa aunque falle el email
        }

        // Generar tokens JWT
        String accessToken = jwtUtil.generateToken(savedUser.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getUsername());

        return new AuthResponse(
                accessToken,
                refreshToken,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                "Usuario registrado exitosamente. Verifica tu email."
        );
    }

    /**
     * Autentica un usuario existente
     */
    public AuthResponse login(LoginRequest request) {
        try {
            // Autenticar usando Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            // Si llegamos aquí, la autenticación fue exitosa
            String username = authentication.getName();

            // Buscar usuario para obtener información adicional
            User user = userRepository.findByEmailOrUsername(request.getUsernameOrEmail() )
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Generar tokens JWT
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

    // Clases DTO para requests y responses
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String confirmPassword;

        // Constructores
        public RegisterRequest() {}

        public RegisterRequest(String username, String email, String password, String confirmPassword) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.confirmPassword = confirmPassword;
        }

        // Getters y setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }

    public static class LoginRequest {
        private String usernameOrEmail;
        private String password;

        // Constructores
        public LoginRequest() {}

        public LoginRequest(String usernameOrEmail, String password) {
            this.usernameOrEmail = usernameOrEmail;
            this.password = password;
        }

        // Getters y setters
        public String getUsernameOrEmail() { return usernameOrEmail; }
        public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private UUID userId;
        private String username;
        private String email;
        private String message;

        // Constructor
        public AuthResponse(String accessToken, String refreshToken, UUID userId,
                            String username, String email, String message) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.message = message;
        }

        // Getters y setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}