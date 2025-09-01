package com.gamelyx.controller;

import com.gamelyx.dto.ChatRequestDtos.*;
import com.gamelyx.dto.ChatResponseDtos.*;
import com.gamelyx.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * WebSocket Controller para chat en tiempo real.
 * Maneja mensajes y operaciones que requieren notificación instantánea.
 */
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Recibe mensajes del cliente via WebSocket y los reenvía al destinatario.
     * Endpoint: /app/chat/send
     * 
     * @param request DTO con recipientId y contenido del mensaje
     * @param headerAccessor Headers WebSocket (para obtener usuario autenticado)
     */
    @MessageMapping("/chat/send")
    public void sendMessage(SendMessageRequestDto request, SimpMessageHeaderAccessor headerAccessor) {
        // 1. Obtener username del usuario autenticado desde WebSocket headers
        Principal user = headerAccessor.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        
        String senderUsername = user.getName();

        // 2. Enviar mensaje usando el service
        SendMessageResponseDto response = chatService.sendMessage(senderUsername, request);

        // 3. Crear mensaje formateado para WebSocket
        WebSocketMessageDto wsMessage = new WebSocketMessageDto(
                "NEW_MESSAGE",
                response.message(),
                response.conversationId(),
                request.recipientId()
        );
        
        // 4. Enviar mensaje al destinatario via WebSocket
        messagingTemplate.convertAndSend(
                "/topic/chat/user/" + request.recipientId(), 
                wsMessage
        );

        // 5. También enviar confirmación al remitente (para sincronizar su UI)
        messagingTemplate.convertAndSend(
                "/topic/chat/user/" + response.message().senderId(), 
                wsMessage
        );
    }

    /**
     * Marca mensajes como leídos y notifica en tiempo real.
     * Endpoint: /app/chat/mark-read
     * 
     * @param request DTO con otherUserId
     * @param headerAccessor Headers WebSocket (para obtener usuario autenticado)
     */
    @MessageMapping("/chat/mark-read")
    public void markMessagesAsRead(MarkAsReadRequestDto request, SimpMessageHeaderAccessor headerAccessor) {
        // 1. Obtener username del usuario autenticado
        Principal user = headerAccessor.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        
        String currentUsername = user.getName();

        // 2. Marcar mensajes como leídos usando el service
        MarkAsReadResponseDto response = chatService.markMessagesAsRead(currentUsername, request);

        // 3. Crear notificación para WebSocket
        WebSocketReadReceiptDto readReceipt = new WebSocketReadReceiptDto(
                "MESSAGES_READ",
                request.otherUserId(),
                response.markedCount()
        );

        // 4. Notificar al otro usuario que sus mensajes fueron leídos
        messagingTemplate.convertAndSend(
                "/topic/chat/user/" + request.otherUserId(), 
                readReceipt
        );
    }
}