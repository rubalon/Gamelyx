package com.gamelyx.controller;

import com.gamelyx.dto.ChatRequestDtos.*;
import com.gamelyx.dto.ChatResponseDtos.*;
import com.gamelyx.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller para funcionalidades de chat que no requieren WebSocket.
 * Solo para obtener historial de mensajes.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Obtiene el historial de mensajes entre el usuario actual y otro usuario.
     * Soporta paginación para conversaciones largas.
     * 
     * GET /api/chat/messages?otherUserId={uuid}&page={n}&limit={n}
     * 
     * @param otherUserId ID del otro usuario en la conversación
     * @param page Número de página (opcional, default: 0)
     * @param limit Mensajes por página (opcional, default: 20, max: 50)
     * @param authentication Usuario autenticado (del JWT)
     * @return ConversationMessagesDto con mensajes descifrados y paginación
     */
    @GetMapping("/messages")
    public ResponseEntity<ConversationMessagesDto> getConversationMessages(
            @RequestParam UUID otherUserId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            //Existe un bug con el limit porque al poner limit te recupera empezando por los primeros mensajes enviados en lugar de por los ultimos.
            // Como vamos a poner un maximo de 25 mensajes por chat para que no nos colapsen la app este bug se soluciona solo pero si esto se quita habria que arreglarlo.
            Authentication authentication) {

        String currentUsername = authentication.getName();
        
        GetMessagesRequestDto request = new GetMessagesRequestDto(otherUserId, page, limit);
        ConversationMessagesDto response = chatService.getConversationMessages(currentUsername, request);
        
        return ResponseEntity.ok(response);
    }
}