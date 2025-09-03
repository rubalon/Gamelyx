package com.gamelyx.security.websocket;

import com.gamelyx.security.jwt.JwtUtil;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Interceptor para autenticar el handshake HTTP inicial de WebSocket.
 * Lee el token JWT del query parameter 'token' y establece la autenticación.
 */
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public WebSocketHandshakeInterceptor(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            System.out.println("🤝 WebSocketHandshakeInterceptor: Processing handshake for " + request.getURI());

            // Extraer token del query parameter
            String token = extractTokenFromQuery(request.getURI());

            if (token == null) {
                System.out.println("❌ No JWT token provided in WebSocket handshake");
                return false; // Rechazar handshake
            }

            // Validar token
            if (!jwtUtil.validateToken(token)) {
                System.out.println("❌ Invalid JWT token in WebSocket handshake");
                return false; // Rechazar handshake
            }

            String username = jwtUtil.extractUsername(token);
            System.out.println("✅ WebSocket handshake authenticated for user: " + username);

            // Cargar UserDetails y crear Authentication
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    null, 
                    userDetails.getAuthorities()
            );

            // Guardar autenticación en atributos de sesión WebSocket
            attributes.put("SPRING_SECURITY_CONTEXT", auth);
            attributes.put("username", username);

            return true; // Permitir handshake

        } catch (Exception e) {
            System.out.println("❌ WebSocket handshake authentication failed: " + e.getMessage());
            return false; // Rechazar handshake
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            System.out.println("❌ WebSocket handshake failed: " + exception.getMessage());
        } else {
            System.out.println("✅ WebSocket handshake completed successfully");
        }
    }

    /**
     * Extrae el token JWT del query parameter 'token' de la URL.
     */
    private String extractTokenFromQuery(URI uri) {
        try {
            String query = uri.getQuery();
            if (query == null) return null;

            return UriComponentsBuilder.fromUriString("?" + query)
                    .build()
                    .getQueryParams()
                    .getFirst("token");
        } catch (Exception e) {
            System.out.println("❌ Error extracting token from query: " + e.getMessage());
            return null;
        }
    }
}