package com.gamelyx.security.config;

import com.gamelyx.security.jwt.JwtAuthenticationFilter;
import com.gamelyx.security.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    // AÑADIDO: Variable de configuración dinámica
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitar CSRF para APIs REST
                .csrf(csrf -> csrf.disable())

                // Configuración de CORS para Angular 20
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configuración de sesiones - Sin estado para JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configuración de autorización
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos de autenticación
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll() // AÑADIDO: refresh también es público

                        // Endpoints de testing y salud (públicos para desarrollo)
                        .requestMatchers("/api/auth/health", "/api/auth/test-email").permitAll()
                        .requestMatchers("/api/test/**").permitAll()


                        // Enpoints de games ( necesitan autenticación)
                        .requestMatchers("GET","/api/games/health").authenticated()
                        .requestMatchers("GET","/api/games/search").authenticated()
                        .requestMatchers("GET", "/api/games/game/*").authenticated()
                        .requestMatchers("PUT", "/api/games/game/*/my-game-details").authenticated()
                        .requestMatchers("GET","/api/games/my-reviews").authenticated()

                        //Enpoints de social ( necesitan autentificacion)ç
                        .requestMatchers("GET","/api/social/home-social-data").authenticated()
                        .requestMatchers("GET","/api/social/search/users").authenticated()
                        .requestMatchers("POST","/api/social/friend-requests").authenticated()
                        .requestMatchers("PUT","/api/social/friend-requests/*/respond").authenticated()
                        .requestMatchers("PUT","/api/social/friend-requests/*/mark-notified").authenticated()
                        .requestMatchers("DELETE","/api/social/friends/*").authenticated()
                        .requestMatchers("GET","/api/social/friend-suggestion/by-game").authenticated()
                        .requestMatchers("POST","/api/social/friend-suggestion/reject").authenticated()

                        // Swagger y documentación (opcional para desarrollo)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Esto permite que endpoints no definidos devuelvan 404 en lugar de 403
                        .anyRequest().permitAll()
                )

                // Manejo de excepciones
                .exceptionHandling(exceptions -> exceptions
                                // Cuando falta autenticación (no hay token), devolver 401 en lugar de 403
                                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // Nota: accessDeniedHandler se usa cuando SÍ hay token pero no tiene permisos
                        // Por ahora no lo necesitamos porque no manejamos roles
                )

                // Añadir nuestro filtro JWT antes del filtro de autenticación estándar
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 🧠 LÓGICA INTELIGENTE: Configuración dinámica según frontendUrl
        List<String> origins = new ArrayList<>();

        // Siempre añadir la URL configurada
        origins.add(frontendUrl);

        // Si frontendUrl contiene localhost, añadir patrones de red local para móviles
        if (frontendUrl.contains("localhost") || frontendUrl.contains("127.0.0.1")) {
            origins.add("http://192.168.*:*");    // Red local más común
            origins.add("http://10.*:*");         // Otra red privada común
            origins.add("http://172.16.*:*");     // Red Docker/privada
            origins.add("https://*.devtunnels.ms");
            origins.add("http://*.devtunnels.ms");
        }

        configuration.setAllowedOriginPatterns(origins);

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Permitir credenciales
        configuration.setAllowCredentials(true);

        // Exponer headers de autorización
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}