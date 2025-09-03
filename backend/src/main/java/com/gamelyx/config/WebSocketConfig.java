package com.gamelyx.config;

import com.gamelyx.security.jwt.JwtUtil;
import com.gamelyx.security.websocket.WebSocketAuthInterceptor;
import com.gamelyx.security.websocket.WebSocketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor, 
                          JwtUtil jwtUtil, 
                          UserDetailsService userDetailsService) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefijo para mensajes que van del cliente al servidor
        registry.setApplicationDestinationPrefixes("/app");
        
        // Prefijo para mensajes broadcast del servidor a clientes suscritos
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint principal para conexiones WebSocket
        // CORS se maneja en SecurityConfig, no duplicamos configuración aquí
        registry.addEndpoint("/ws")
                // Agregar interceptor de handshake para autenticación HTTP inicial
                .addInterceptors(new WebSocketHandshakeInterceptor(jwtUtil, userDetailsService))
                // Permitir conexiones desde localhost (desarrollo)  
                .setAllowedOrigins("http://localhost:4200");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Agregar interceptor de autenticación JWT para WebSocket
        registration.interceptors(webSocketAuthInterceptor);
    }
}