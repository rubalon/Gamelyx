package com.gamelyx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

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
                // Habilitar SockJS para fallback en navegadores sin WebSocket nativo
                .withSockJS();
    }
}