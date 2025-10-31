# ChatWebSocketController - Endpoints WebSocket de Chat

## Introducción

El **ChatWebSocketController** gestiona la comunicación en tiempo real del sistema de chat mediante **WebSocket** con protocolo **STOMP** (Simple Text Oriented Messaging Protocol). A diferencia de los endpoints REST, los WebSocket mantienen una conexión persistente bidireccional, permitiendo la entrega instantánea de mensajes.

**Funcionalidades principales:**
- Envío de mensajes en tiempo real con notificación instantánea
- Marcado de mensajes como leídos con confirmación en tiempo real
- Sincronización automática entre múltiples dispositivos

**Arquitectura WebSocket:**
- **Conexión:** `ws://localhost:8080/ws` (o `wss://` en producción)
- **Protocolo:** STOMP sobre WebSocket
- **Autenticación:** Token JWT en el handshake inicial
- **Envío al servidor:** Prefijo `/app`
- **Recepción del servidor:** Prefijo `/topic`

---

## Endpoints WebSocket

### `@MessageMapping /app/chat/send`

**Descripción:** Envía mensajes en tiempo real. Cifra el mensaje, lo almacena en BD y lo reenvía instantáneamente al destinatario y al remitente (para sincronización).

**Protocolo:** WebSocket (STOMP)

**Autenticación:** JWT en WebSocket headers (handshake)

**Parámetros de entrada (JSON enviado a `/app/chat/send`):**
```json
{
  "recipientId": "uuid",           // UUID del destinatario
  "content": "Hola, ¿cómo estás?"  // Contenido del mensaje (máx. 1000 caracteres)
}
```

**Validaciones:**
- `recipientId`: NotNull
- `content`: NotBlank, Size(max=1000)

**Respuesta WebSocket:**

El servidor envía el mensaje a **dos destinos**:

1. **Al destinatario:** `/topic/chat/user/{recipientId}`
2. **Al remitente (confirmación):** `/topic/chat/user/{senderId}`

```json
{
  "type": "NEW_MESSAGE",
  "message": {
    "messageId": "uuid",
    "senderId": "uuid",
    "senderUsername": "string",
    "content": "Hola, ¿cómo estás?",
    "sentAt": "2024-01-15T10:30:00",
    "isRead": false
  },
  "conversationId": "uuid",
  "recipientId": "uuid"
}
```

**Flujo de funcionamiento:**
1. Cliente envía mensaje a `/app/chat/send`
2. Servidor valida autenticación JWT
3. Servidor cifra el contenido con AES-256
4. Servidor guarda el mensaje en base de datos
5. Servidor descifra y envía a destinatario via `/topic/chat/user/{recipientId}`
6. Servidor envía confirmación a remitente via `/topic/chat/user/{senderId}`

**Excepciones:**
- `IllegalArgumentException`: Usuario no autenticado
- `RuntimeException`: Error al cifrar/guardar mensaje

**DTO utilizado:**
- Request: `ChatRequestDtos.SendMessageRequestDto`
- Response: `ChatResponseDtos.WebSocketMessageDto`
  - Incluye: `MessageDto`

---

### `@MessageMapping /app/chat/mark-read`

**Descripción:** Marca mensajes como leídos en tiempo real. Actualiza el estado en BD y notifica al remitente (confirmaciones de lectura / "checks azules").

**Protocolo:** WebSocket (STOMP)

**Autenticación:** JWT en WebSocket headers (handshake)

**Parámetros de entrada (JSON enviado a `/app/chat/mark-read`):**
```json
{
  "otherUserId": "uuid"  // UUID del otro usuario de la conversación
}
```

**Validaciones:**
- `otherUserId`: NotNull

**Respuesta WebSocket:**

El servidor notifica al **remitente original** via `/topic/chat/user/{otherUserId}`:

```json
{
  "type": "MESSAGES_READ",
  "readByUserId": "uuid",     // UUID del usuario que leyó los mensajes
  "messageCount": 5           // Cantidad de mensajes marcados como leídos
}
```

**Flujo de funcionamiento:**
1. Usuario abre conversación con otro usuario
2. Cliente envía petición a `/app/chat/mark-read` con `otherUserId`
3. Servidor identifica todos los mensajes no leídos enviados por `otherUserId`
4. Servidor marca esos mensajes como leídos (`isRead = true`)
5. Servidor notifica al `otherUserId` via `/topic/chat/user/{otherUserId}`
6. Cliente del `otherUserId` actualiza la UI (muestra "checks azules")

**Excepciones:**
- `IllegalArgumentException`: Usuario no autenticado

**DTO utilizado:**
- Request: `ChatRequestDtos.MarkAsReadRequestDto`
- Response: `ChatResponseDtos.WebSocketReadReceiptDto`

---

## Suscripciones WebSocket del Cliente

Para recibir mensajes en tiempo real, el cliente debe suscribirse a:

### **Topic personal del usuario**
```
/topic/chat/user/{currentUserId}
```

**Mensajes recibidos:**
- `type: "NEW_MESSAGE"` - Mensajes nuevos de otros usuarios
- `type: "NEW_MESSAGE"` - Confirmaciones de envío de propios mensajes
- `type: "MESSAGES_READ"` - Confirmaciones de lectura de mensajes enviados

---

## Ejemplo de implementación del cliente

**JavaScript (SockJS + STOMP):**

```javascript
// Conectar al WebSocket
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// Headers de autenticación
const headers = {
  'Authorization': 'Bearer ' + accessToken
};

stompClient.connect(headers, () => {
  // Suscribirse a mensajes personales
  stompClient.subscribe('/topic/chat/user/' + currentUserId, (message) => {
    const data = JSON.parse(message.body);

    if (data.type === 'NEW_MESSAGE') {
      // Mostrar mensaje nuevo en la UI
      displayMessage(data.message);
    } else if (data.type === 'MESSAGES_READ') {
      // Actualizar UI con checks de lectura
      updateReadReceipts(data.readByUserId, data.messageCount);
    }
  });
});

// Enviar mensaje
function sendMessage(recipientId, content) {
  stompClient.send('/app/chat/send', {}, JSON.stringify({
    recipientId: recipientId,
    content: content
  }));
}

// Marcar mensajes como leídos
function markAsRead(otherUserId) {
  stompClient.send('/app/chat/mark-read', {}, JSON.stringify({
    otherUserId: otherUserId
  }));
}
```

---

## Resumen de DTOs

### DTOs de Request
- `SendMessageRequestDto(recipientId, content)`
  - Validaciones:
    - `recipientId`: NotNull
    - `content`: NotBlank, Size(max=1000)

- `MarkAsReadRequestDto(otherUserId)`
  - Validaciones:
    - `otherUserId`: NotNull

### DTOs de Response
- `WebSocketMessageDto(type, message, conversationId, recipientId)`
  - `type`: "NEW_MESSAGE"
  - `message`: MessageDto

- `WebSocketReadReceiptDto(type, readByUserId, messageCount)`
  - `type`: "MESSAGES_READ"

- `MessageDto(messageId, senderId, senderUsername, content, sentAt, isRead)`

- `SendMessageResponseDto(message, conversationId)` - Usado internamente

- `MarkAsReadResponseDto(success, markedCount, readByUserId)` - Usado internamente

---

## Seguridad

- **Cifrado de mensajes:** AES-256 en base de datos
- **Autenticación:** JWT token requerido en handshake WebSocket
- **Validación de usuarios:** Solo el remitente puede enviar mensajes
- **Autorización:** Solo el destinatario puede marcar mensajes como leídos
