package com.gamelyx.security.websocket;

import com.gamelyx.security.jwt.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Interceptor para autenticar conexiones WebSocket usando JWT.
 * Valida el token en el handshake inicial y establece el Principal.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Solo autenticar en el CONNECT inicial
            authenticateUser(accessor);
        }
        
        return message;
    }

    /**
     * Extrae JWT token y autentica usuario en el handshake WebSocket.
     */
    private void authenticateUser(StompHeaderAccessor accessor) {
        try {
            // 1. Extraer token de headers WebSocket
            String token = extractTokenFromHeaders(accessor);
            
            if (token == null) {
                throw new RuntimeException("No JWT token provided in WebSocket connection");
            }

            // 2. Validar token y extraer username
            if (!jwtUtil.validateToken(token)) {
                throw new RuntimeException("Invalid JWT token in WebSocket connection");
            }

            String username = jwtUtil.extractUsername(token);

            // 3. Cargar UserDetails y crear Authentication
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    null, 
                    userDetails.getAuthorities()
            );

            // 4. Establecer Principal en sesión WebSocket
            accessor.setUser(auth);

        } catch (Exception e) {
            throw new RuntimeException("WebSocket authentication failed: " + e.getMessage());
        }
    }

    /**
     * Extrae JWT token del header Authorization.
     */
    private String extractTokenFromHeaders(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        return null;
    }
}