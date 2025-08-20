package com.gamelyx.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Extraer el header Authorization
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Verificar si el header contiene un Bearer token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // Extraer el token (quitar "Bearer " del inicio)
            jwt = authorizationHeader.substring(7);

            try {
                // Extraer el username del token
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                // Log del error (token malformado, expirado, etc.)
                logger.warn("No se pudo extraer username del JWT token: " + e.getMessage());
            }
        }

        // Si tenemos username y no hay autenticación previa en el contexto
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                // ✅ CORREGIDO: Solo validar el JWT sin consultar base de datos
                if (jwtUtil.validateToken(jwt)) {


                    // Crear las autoridades por defecto (todos los usuarios verificados tienen ROLE_USER)
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_USER")
                    );

                    // Crear el objeto de autenticación usando SOLO el username del JWT
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    username,   // Principal: SOLO el username (String)
                                    null,       // Credentials: null para JWT
                                    authorities // Authorities: roles por defecto
                            );

                    // Añadir detalles adicionales de la petición
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Establecer la autenticación en el contexto de seguridad
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.debug("Usuario autenticado vía JWT: " + username);
                }
            } catch (Exception e) {
                // Log del error al validar token
                logger.warn("Error durante la validación del JWT: " + e.getMessage());
            }
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Obtener la ruta de la petición
        String path = request.getRequestURI();

        // No filtrar endpoints públicos
        return path.startsWith("/api/auth/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.equals("/") ||
                path.startsWith("/error");
    }
}