import { Injectable, inject, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { AuthStore } from '@core/stores/auth-store';
import { MessageDto } from './chat-api';

// DTOs para WebSocket basados en el backend
export interface WebSocketMessageDto {
  type: "NEW_MESSAGE";
  message: MessageDto;
  conversationId: string;
  recipientId: string;
}

export interface WebSocketReadReceiptDto {
  type: "MESSAGES_READ";
  readByUserId: string;
  messageCount: number;
}

// Request DTOs para WebSocket
export interface SendMessageRequest {
  recipientId: string; // UUID como string
  content: string;
}

export interface MarkReadRequest {
  otherUserId: string; // UUID como string
}

@Injectable({ providedIn: 'root' })
export class ChatWebSocket {
  private authStore = inject(AuthStore);
  private client: Client | null = null;
  
  // 📡 Estado de conexión
  isConnected = signal(false);
  isConnecting = signal(false);
  connectionError = signal<string | null>(null);

  // 📥 Callbacks para eventos
  private onMessageCallback?: (message: WebSocketMessageDto) => void;
  private onReadReceiptCallback?: (receipt: WebSocketReadReceiptDto) => void;

  /**
   * 🔌 Conectar al WebSocket
   */
  connect(): void {
    if (this.client?.active) return;

    const token = this.authStore.token();
    if (!token) {
      this.connectionError.set('No hay token de autenticación');
      return;
    }

    this.isConnecting.set(true);
    this.connectionError.set(null);

    // Crear cliente STOMP con WebSocket nativo
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: {
        Authorization: `Bearer ${token}` // Como espera WebSocketAuthInterceptor
      },
      debug: (str) => console.log('🔌 STOMP:', str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // Callbacks de conexión
    this.client.onConnect = () => {
      console.log('✅ WebSocket conectado');
      this.isConnected.set(true);
      this.isConnecting.set(false);
      this.connectionError.set(null);
      this.subscribeToUserChannel();
    };

    this.client.onDisconnect = () => {
      console.log('❌ WebSocket desconectado');
      this.isConnected.set(false);
      this.isConnecting.set(false);
    };

    this.client.onStompError = (frame) => {
      console.error('❌ Error STOMP:', frame);
      this.connectionError.set(`Error de conexión: ${frame.headers['message']}`);
      this.isConnected.set(false);
      this.isConnecting.set(false);
    };

    // Activar cliente
    this.client.activate();
  }

  /**
   * 📡 Suscribirse al canal personal del usuario
   */
  private subscribeToUserChannel(): void {
    const currentUser = this.authStore.currentUser();
    if (!currentUser || !this.client) return;

    // Suscribirse a mensajes personales: /topic/chat/user/{userId}
    this.client.subscribe(`/topic/chat/user/${currentUser.id}`, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body);
        
        if (data.type === 'NEW_MESSAGE') {
          console.log('📨 Nuevo mensaje recibido:', data);
          this.onMessageCallback?.(data as WebSocketMessageDto);
        } else if (data.type === 'MESSAGES_READ') {
          console.log('👁️ Mensajes marcados como leídos:', data);
          this.onReadReceiptCallback?.(data as WebSocketReadReceiptDto);
        }
      } catch (error) {
        console.error('❌ Error parseando mensaje WebSocket:', error);
      }
    });
  }

  /**
   * 📤 Enviar mensaje
   * Endpoint: /app/chat/send
   */
  sendMessage(request: SendMessageRequest): void {
    if (!this.client?.active) {
      console.error('❌ WebSocket no conectado');
      return;
    }

    this.client.publish({
      destination: '/app/chat/send', // Como en ChatWebSocketController
      body: JSON.stringify(request)
    });

    console.log('📤 Mensaje enviado:', request);
  }

  /**
   * 👁️ Marcar mensajes como leídos
   * Endpoint: /app/chat/mark-read
   */
  markMessagesAsRead(request: MarkReadRequest): void {
    if (!this.client?.active) {
      console.error('❌ WebSocket no conectado');
      return;
    }

    this.client.publish({
      destination: '/app/chat/mark-read', // Como en ChatWebSocketController
      body: JSON.stringify(request)
    });

    console.log('👁️ Mensajes marcados como leídos:', request);
  }

  /**
   * 📥 Establecer callback para nuevos mensajes
   */
  onNewMessage(callback: (message: WebSocketMessageDto) => void): void {
    this.onMessageCallback = callback;
  }

  /**
   * 📥 Establecer callback para confirmaciones de lectura
   */
  onReadReceipt(callback: (receipt: WebSocketReadReceiptDto) => void): void {
    this.onReadReceiptCallback = callback;
  }

  /**
   * 🔌 Desconectar WebSocket
   */
  disconnect(): void {
    if (this.client?.active) {
      this.client.deactivate();
    }
    this.isConnected.set(false);
    this.isConnecting.set(false);
    this.connectionError.set(null);
  }
}