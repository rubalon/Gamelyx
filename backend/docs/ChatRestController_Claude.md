# ChatRestController - Endpoints REST de Chat

## Introducción

El **ChatRestController** (`/api/chat`) proporciona endpoints REST para la funcionalidad de chat. Este controlador está especializado en la **obtención del historial de mensajes** entre usuarios. El envío de mensajes en tiempo real se realiza mediante WebSocket (ver ChatWebSocketController).

**Funcionalidades principales:**
- Recuperación del historial de mensajes con paginación
- Mensajes descifrados automáticamente (cifrado AES-256)
- Soporte para conversaciones largas con múltiples páginas

---

## Endpoints

### `GET /api/chat/messages`

**Descripción:** Obtiene el historial de mensajes de una conversación entre el usuario autenticado y otro usuario específico. Los mensajes se devuelven descifrados y ordenados cronológicamente.

**Método HTTP:** GET

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada (Query params):**
- `otherUserId` (uuid, requerido): UUID del otro usuario en la conversación
- `page` (integer, opcional): Número de página (default: 0)
- `limit` (integer, opcional): Mensajes por página (default: 20, max: 50)

**Ejemplo:**
```
GET /api/chat/messages?otherUserId=550e8400-e29b-41d4-a716-446655440000&page=0&limit=20
```

**Validaciones:**
- `otherUserId`: NotNull
- `page`: Si es null o negativo, se usa 0
- `limit`: Si es null, <= 0 o > 50, se usa 20

**Parámetros de salida (JSON):**
```json
{
  "conversationId": "uuid",
  "otherUser": {
    "userId": "uuid",
    "username": "string"
  },
  "messages": [
    {
      "messageId": "uuid",
      "senderId": "uuid",
      "senderUsername": "string",
      "content": "Hola, ¿cómo estás?",
      "sentAt": "2024-01-15T10:30:00",
      "isRead": true
    }
  ],
  "hasMore": true,
  "currentPage": 0,
  "totalPages": 3
}
```

**Notas:**
- Los mensajes están ordenados cronológicamente (del más antiguo al más reciente)
- El sistema descifra automáticamente el contenido usando AES-256
- **Limitación actual:** Máximo 25 mensajes por conversación para optimizar rendimiento

**Códigos de estado:**
- `200 OK`: Mensajes obtenidos exitosamente
- `401 Unauthorized`: Usuario no autenticado
- `404 Not Found`: Usuario no encontrado

**DTO utilizado:**
- Request: `ChatRequestDtos.GetMessagesRequestDto`
- Response: `ChatResponseDtos.ConversationMessagesDto`
  - Incluye: `UserBasicDto`, `MessageDto`

---

## Resumen de DTOs

### DTOs de Request
- `GetMessagesRequestDto(otherUserId, page, limit)`
  - Validaciones:
    - `otherUserId`: NotNull
    - `page`: Default 0 si null/negativo
    - `limit`: Default 20 si null/inválido, max 50

### DTOs de Response
- `ConversationMessagesDto(conversationId, otherUser, messages, hasMore, currentPage, totalPages)`
- `MessageDto(messageId, senderId, senderUsername, content, sentAt, isRead)`
- `UserBasicDto(userId, username)`

---

## Notas sobre WebSocket

El envío de mensajes en tiempo real NO se realiza mediante REST, sino mediante WebSocket. Ver el documento `ChatWebSocketController.md` para detalles sobre:
- `@MessageMapping /app/chat/send` - Enviar mensajes
- `@MessageMapping /app/chat/mark-read` - Marcar mensajes como leídos
