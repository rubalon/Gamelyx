package com.gamelyx.security.jwt;

import com.gamelyx.entity.User;
import com.gamelyx.security.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

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
                // Cargar los detalles del usuario desde la base de datos
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Validar el token con los detalles del usuario
                if (jwtUtil.validateToken(jwt, userDetails)) {

                    // User Entity
                    User user = ((CustomUserDetailsService.CustomUserPrincipal) userDetails).getUser();


                    // Crear el objeto de autenticación
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // Añadir detalles adicionales de la petición
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Establecer la autenticación en el contexto de seguridad
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.debug("Usuario autenticado: " + username);
                }
            } catch (Exception e) {
                // Log del error al cargar usuario o validar token
                logger.warn("Error durante la autenticación JWT: " + e.getMessage());
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