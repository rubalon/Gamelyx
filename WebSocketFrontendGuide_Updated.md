# Frontend Chat Implementation Guide
## **ACTUALIZADA - Con contexto del backend real**

## 🎯 Objetivo General
Implementar interfaz de chat en tiempo real integrada con el sistema social existente, usando WebSockets y arquitectura de Signals, **adaptada al backend específico ya implementado**.

---

## **PASO 0: TESTING BACKEND - Validación Inicial** ⚠️
**NUEVO: Antes de tocar Angular, validamos que el backend funciona**

### **Tarea 0.1: Cliente HTML Simple**
Crear archivo de testing para validar WebSocket backend sin dependencias de Angular.

**Crear: `chat-test.html`**
```html
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Test Chat WebSocket - Gamelyx Backend</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        #messages { border: 1px solid #ccc; height: 300px; overflow-y: auto; padding: 10px; margin: 10px 0; }
        .message { margin: 5px 0; padding: 5px; background: #f0f0f0; border-radius: 3px; }
        .sent { background: #007bff; color: white; }
        .received { background: #28a745; color: white; }
        .error { background: #dc3545; color: white; }
        input, button { margin: 5px; padding: 8px; }
        #status { font-weight: bold; padding: 10px; }
        .connected { color: green; }
        .disconnected { color: red; }
        .connecting { color: orange; }
    </style>
</head>
<body>
    <h1>🧪 Test Chat WebSocket - Gamelyx Backend</h1>
    
    <div id="status" class="disconnected">❌ Desconectado</div>
    
    <div>
        <h3>1. Configuración</h3>
        <input type="text" id="jwtToken" placeholder="Pegar JWT token aquí" style="width: 400px;">
        <button onclick="connect()">🔌 Conectar WebSocket</button>
        <button onclick="disconnect()">🔌 Desconectar</button>
    </div>

    <div>
        <h3>2. Envío de Mensajes</h3>
        <input type="text" id="recipientId" placeholder="UUID del destinatario" style="width: 300px;">
        <input type="text" id="messageContent" placeholder="Contenido del mensaje" style="width: 300px;">
        <button onclick="sendMessage()">📤 Enviar Mensaje</button>
    </div>

    <div>
        <h3>3. Marcar como Leído</h3>
        <input type="text" id="markReadUserId" placeholder="UUID del otro usuario" style="width: 300px;">
        <button onclick="markAsRead()">✅ Marcar como Leído</button>
    </div>

    <div>
        <h3>4. Mensajes Recibidos</h3>
        <div id="messages"></div>
        <button onclick="clearMessages()">🧹 Limpiar</button>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
    <script>
        let stompClient = null;
        let currentUserId = null;

        function updateStatus(message, className) {
            const statusDiv = document.getElementById('status');
            statusDiv.textContent = message;
            statusDiv.className = className;
        }

        function addMessage(text, type = 'message') {
            const messagesDiv = document.getElementById('messages');
            const messageDiv = document.createElement('div');
            messageDiv.className = `message ${type}`;
            messageDiv.textContent = `[${new Date().toLocaleTimeString()}] ${text}`;
            messagesDiv.appendChild(messageDiv);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }

        function connect() {
            const token = document.getElementById('jwtToken').value.trim();
            if (!token) {
                addMessage('❌ ERROR: Debes pegar un JWT token válido', 'error');
                return;
            }

            updateStatus('🔄 Conectando...', 'connecting');

            // Configuración específica para el backend de Gamelyx
            const socket = new SockJS('http://localhost:8080/ws');
            stompClient = new StompJs.Client({
                webSocketFactory: () => socket,
                connectHeaders: {
                    'Authorization': `Bearer ${token}`
                },
                debug: function (str) {
                    console.log('STOMP Debug:', str);
                    addMessage(`🔧 DEBUG: ${str}`, 'debug');
                },
                reconnectDelay: 5000,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
            });

            stompClient.onConnect = function (frame) {
                updateStatus('✅ Conectado exitosamente', 'connected');
                addMessage('✅ CONECTADO: WebSocket establecido correctamente', 'received');
                
                // Extraer userId del JWT (simplificado para testing)
                try {
                    const payload = JSON.parse(atob(token.split('.')[1]));
                    currentUserId = payload.sub; // username en el token
                    addMessage(`👤 Usuario actual: ${currentUserId}`, 'received');
                } catch (e) {
                    addMessage('⚠️ No se pudo extraer usuario del token', 'error');
                }

                // Suscribirse al canal personal (requiere userId real para testing completo)
                if (currentUserId) {
                    // NOTA: Necesitaríamos el UUID real del usuario, no el username
                    addMessage('📡 Para recibir mensajes, necesitamos suscribirnos a /topic/chat/user/{tu-user-id}', 'received');
                }
            };

            stompClient.onStompError = function (frame) {
                updateStatus('❌ Error de conexión', 'disconnected');
                addMessage(`❌ ERROR STOMP: ${frame.headers['message']}`, 'error');
                addMessage(`❌ Detalles: ${frame.body}`, 'error');
            };

            stompClient.onWebSocketError = function (error) {
                updateStatus('❌ Error WebSocket', 'disconnected');
                addMessage(`❌ ERROR WebSocket: ${error}`, 'error');
            };

            stompClient.onDisconnect = function () {
                updateStatus('❌ Desconectado', 'disconnected');
                addMessage('❌ DESCONECTADO del servidor', 'error');
            };

            stompClient.activate();
        }

        function disconnect() {
            if (stompClient !== null) {
                stompClient.deactivate();
                updateStatus('❌ Desconectado', 'disconnected');
                addMessage('🔌 Desconectado manualmente', 'message');
            }
        }

        function sendMessage() {
            const recipientId = document.getElementById('recipientId').value.trim();
            const content = document.getElementById('messageContent').value.trim();

            if (!stompClient || !stompClient.connected) {
                addMessage('❌ ERROR: No hay conexión WebSocket', 'error');
                return;
            }

            if (!recipientId || !content) {
                addMessage('❌ ERROR: Debes llenar recipientId y mensaje', 'error');
                return;
            }

            const messageRequest = {
                recipientId: recipientId,
                content: content
            };

            try {
                // Endpoint específico del backend de Gamelyx
                stompClient.publish({
                    destination: '/app/chat/send',
                    body: JSON.stringify(messageRequest)
                });

                addMessage(`📤 ENVIADO: "${content}" a ${recipientId}`, 'sent');
                document.getElementById('messageContent').value = '';
            } catch (error) {
                addMessage(`❌ ERROR enviando mensaje: ${error}`, 'error');
            }
        }

        function markAsRead() {
            const otherUserId = document.getElementById('markReadUserId').value.trim();

            if (!stompClient || !stompClient.connected) {
                addMessage('❌ ERROR: No hay conexión WebSocket', 'error');
                return;
            }

            if (!otherUserId) {
                addMessage('❌ ERROR: Debes especificar el userId del otro usuario', 'error');
                return;
            }

            const markReadRequest = {
                otherUserId: otherUserId
            };

            try {
                // Endpoint específico del backend de Gamelyx
                stompClient.publish({
                    destination: '/app/chat/mark-read',
                    body: JSON.stringify(markReadRequest)
                });

                addMessage(`✅ MARCADO COMO LEÍDO: Conversación con ${otherUserId}`, 'sent');
            } catch (error) {
                addMessage(`❌ ERROR marcando como leído: ${error}`, 'error');
            }
        }

        function clearMessages() {
            document.getElementById('messages').innerHTML = '';
        }

        // Manejar Enter en inputs
        document.getElementById('messageContent').addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                sendMessage();
            }
        });
    </script>

    <div style="margin-top: 30px; padding: 20px; background: #f8f9fa; border-radius: 5px;">
        <h3>📋 Instrucciones de Testing:</h3>
        <ol>
            <li><strong>Obtener JWT:</strong> <code>curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"tu-usuario","password":"tu-password"}'</code></li>
            <li><strong>Pegar token</strong> en el campo de arriba (solo la parte del token, sin "Bearer ")</li>
            <li><strong>Conectar</strong> y verificar que aparece "✅ Conectado exitosamente"</li>
            <li><strong>Enviar mensaje</strong> usando UUID de otro usuario real</li>
            <li><strong>Verificar logs</strong> del backend para confirmar recepción</li>
        </ol>
        
        <h4>🎯 Objetivos de este test:</h4>
        <ul>
            <li>✅ Validar autenticación JWT en WebSocket</li>
            <li>✅ Confirmar endpoints <code>/app/chat/send</code> y <code>/app/chat/mark-read</code></li>
            <li>✅ Verificar formato de mensajes WebSocket</li>
            <li>✅ Probar manejo de errores</li>
        </ul>

        <p><strong>⚠️ IMPORTANTE:</strong> Este archivo es solo para testing backend. Una vez validado, procedemos con la implementación Angular real.</p>
    </div>
</body>
</html>
```

### **Prueba 0.1:**
- Abrir `chat-test.html` en navegador
- Hacer login via curl para obtener JWT
- Conectar WebSocket y verificar conexión exitosa
- Enviar mensaje de prueba
- Verificar en logs del backend que llega correctamente

---

## 🏗️ Arquitectura Técnica (Adaptada al Backend)

### **Backend Endpoints Disponibles:**
- **WebSocket**: `ws://localhost:8080/ws` (con auth JWT)
- **REST**: `GET /api/chat/messages?otherUserId={uuid}` 
- **WebSocket Send**: `/app/chat/send` → `{recipientId, content}`
- **WebSocket Mark Read**: `/app/chat/mark-read` → `{otherUserId}`
- **WebSocket Subscribe**: `/topic/chat/user/{userId}` (personal channel)

### **Datos del Backend:**
```typescript
// DTOs reales del backend
interface MessageDto {
  messageId: string;
  senderId: string;
  senderUsername: string;
  content: string; // Ya viene descifrado
  sentAt: string; // ISO date
  isRead: boolean;
}

interface WebSocketMessageDto {
  type: 'NEW_MESSAGE';
  message: MessageDto;
  conversationId: string; // Para tracking interno
  recipientId: string;
}

interface WebSocketReadReceiptDto {
  type: 'MESSAGES_READ';
  readByUserId: string;
  messageCount: number;
}
```

### **Estructura de Componentes (Actualizada)**
```
Chat Feature:
├── chat-store.ts - Estado global con DTOs reales del backend
├── websocket.service.ts - Configurado para endpoints específicos
├── chat-http.service.ts - Para historial vía REST
├── chat.component.ts - UI principal
├── message.component.ts - Renderiza MessageDto
├── chat-modal.service.ts - Integra con ContactUserDto existente
└── types/chat.types.ts - DTOs tipados del backend
```

---

## **PASO 1: Dependencias y Configuración** (Actualizado)

### **Tarea 1.1: Instalar Dependencias WebSocket**
```bash
npm install sockjs-client @stomp/stompjs
npm install --save-dev @types/sockjs-client
```

### **Tarea 1.2: Environment Configuration (Específico)**
```typescript
// environment.ts - Configurado para el backend real
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  wsUrl: 'http://localhost:8080/ws', // Endpoint real del backend
  chat: {
    reconnectInterval: 5000,
    maxReconnectAttempts: 10,
    messageHistoryLimit: 20, // Coincide con paginación del backend
    endpoints: {
      send: '/app/chat/send',
      markRead: '/app/chat/mark-read',
      subscribe: '/topic/chat/user/', // + userId
      messages: '/api/chat/messages' // REST endpoint
    }
  }
};
```

---

## **PASO 2: WebSocket Service (Específico del Backend)**

### **Tarea 2.1: Configurar para Backend Real**
```typescript
// websocket.service.ts - Configuración específica
@Injectable({providedIn: 'root'})
export class WebSocketService {
  private stompClient: Client | null = null;
  private readonly wsUrl = environment.wsUrl;

  connect(token: string): Promise<void> {
    const socket = new SockJS(this.wsUrl);
    
    this.stompClient = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        'Authorization': `Bearer ${token}` // Formato específico del backend
      },
      debug: (str) => console.log('WebSocket:', str),
      reconnectDelay: environment.chat.reconnectInterval,
    });

    // Configuración específica para los endpoints del backend...
  }

  sendMessage(recipientId: string, content: string) {
    if (!this.stompClient?.connected) return;
    
    this.stompClient.publish({
      destination: environment.chat.endpoints.send,
      body: JSON.stringify({ recipientId, content }) // Formato exacto del backend
    });
  }

  markAsRead(otherUserId: string) {
    if (!this.stompClient?.connected) return;
    
    this.stompClient.publish({
      destination: environment.chat.endpoints.markRead,
      body: JSON.stringify({ otherUserId }) // Formato exacto del backend
    });
  }

  subscribeToUserMessages(userId: string): Subscription {
    return this.stompClient!.subscribe(
      `${environment.chat.endpoints.subscribe}${userId}`,
      (message) => {
        const data: WebSocketMessageDto | WebSocketReadReceiptDto = 
          JSON.parse(message.body);
        this.handleIncomingMessage(data);
      }
    );
  }
}
```

### **Tarea 2.2: HTTP Service para Historial**
```typescript
// chat-http.service.ts - Para cargar historial via REST
@Injectable({providedIn: 'root'})
export class ChatHttpService {
  constructor(private http: HttpClient) {}

  getConversationMessages(otherUserId: string, page = 0, limit = 20) {
    return this.http.get<ConversationMessagesDto>(
      `${environment.apiUrl}${environment.chat.endpoints.messages}`,
      { 
        params: { otherUserId, page: page.toString(), limit: limit.toString() }
      }
    );
  }
}
```

---

## **PASO 3: Chat Store (Con DTOs Reales)**

### **Tarea 3.1: Store con Tipos del Backend**
```typescript
// chat-store.ts - Usando DTOs reales
@Injectable({providedIn: 'root'})
export class ChatStore {
  private messagesSignal = signal<Map<string, MessageDto[]>>(new Map());
  private connectionStatus = signal<'connected' | 'connecting' | 'disconnected'>('disconnected');

  // Computed para mensajes de una conversación específica
  getConversationMessages = computed(() => (otherUserId: string) => {
    return this.messagesSignal().get(otherUserId) || [];
  });

  addMessage(message: MessageDto, otherUserId: string) {
    const currentMessages = this.messagesSignal();
    const conversationMessages = currentMessages.get(otherUserId) || [];
    
    currentMessages.set(otherUserId, [...conversationMessages, message]);
    this.messagesSignal.set(new Map(currentMessages));
  }

  handleWebSocketMessage(wsMessage: WebSocketMessageDto | WebSocketReadReceiptDto) {
    if (wsMessage.type === 'NEW_MESSAGE') {
      const msg = wsMessage as WebSocketMessageDto;
      const otherUserId = msg.message.senderId; // Determinar conversación
      this.addMessage(msg.message, otherUserId);
    } else if (wsMessage.type === 'MESSAGES_READ') {
      const receipt = wsMessage as WebSocketReadReceiptDto;
      this.markMessagesAsRead(receipt.readByUserId);
    }
  }

  // Métodos específicos para el backend...
}
```

---

## **PASO 4: Integración con Sistema Social Existente**

### **Tarea 4.1: Botones de Chat en ContactUserCard**
```typescript
// Modificar componente existente que usa ContactUserDto
export class ContactUserCardComponent {
  @Input() contact!: ContactUserDto; // DTO existente sin chatId

  openChat() {
    // Usar user.userId del ContactUserDto para abrir chat
    this.chatModalService.openChat(this.contact.user.userId, this.contact.user.username);
  }
}
```

### **Tarea 4.2: Chat Modal Service**
```typescript
// chat-modal.service.ts - Integra con sistema existente
@Injectable({providedIn: 'root'})
export class ChatModalService {
  private activeChats = signal<Map<string, boolean>>(new Map());

  openChat(otherUserId: string, otherUsername: string) {
    // Abrir modal de chat para usuario específico
    // Cargar historial via REST
    this.chatHttpService.getConversationMessages(otherUserId).subscribe(history => {
      this.chatStore.loadConversationHistory(otherUserId, history.messages);
      this.openChatModal(otherUserId, otherUsername, history);
    });
  }
}
```

---

## ✅ Criterios de Éxito Específicos

El frontend estará completo cuando:
1. **WebSocket conecta** con JWT del AuthStore usando `Authorization: Bearer token`
2. **Mensajes se envían** via `/app/chat/send` con formato `{recipientId, content}`
3. **Mensajes se reciben** via `/topic/chat/user/{userId}` con formato `WebSocketMessageDto`
4. **Historial se carga** via `GET /api/chat/messages?otherUserId=uuid`
5. **Mark as read** funciona via `/app/chat/mark-read` con `{otherUserId}`
6. **Integración social** usando `ContactUserDto.user.userId` para abrir chats
7. **Sin conversation IDs** en frontend - todo basado en `otherUserId`

---

## 🚨 Puntos Críticos Específicos del Backend

- **Autenticación**: JWT en `Authorization: Bearer token` header
- **User IDs**: Siempre usar UUID, nunca username para operaciones
- **Endpoints exactos**: `/app/chat/send`, `/app/chat/mark-read`, `/topic/chat/user/{userId}`
- **DTOs tipados**: Usar `MessageDto`, `WebSocketMessageDto`, `WebSocketReadReceiptDto`
- **No conversation IDs**: Frontend nunca maneja `conversationId`
- **Cifrado transparente**: Backend maneja cifrado/descifrado automáticamente
- **Paginación**: Usar `page` y `limit` para historial REST

---

**🎯 PRÓXIMO PASO**: Ejecutar testing HTML para validar backend antes de proceder con Angular.