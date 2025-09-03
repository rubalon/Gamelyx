import { Injectable, inject, signal, computed, effect } from '@angular/core';
import { ChatApi, MessageDto, ConversationMessagesDto } from '@core/services/chat-api';
import { ChatWebSocket, WebSocketMessageDto, WebSocketReadReceiptDto } from '@core/services/chat-websocket';
import { AuthStore } from './auth-store';
import { SocialStore } from './social-store';

@Injectable({ providedIn: 'root' })
export class ChatStore {
  private chatApi = inject(ChatApi);
  private chatWs = inject(ChatWebSocket);
  private authStore = inject(AuthStore);
  private socialStore = inject(SocialStore);

  // 💬 Estado de LA conversación activa (solo una, se recarga cada vez)
  private _activeConversation = signal<ConversationMessagesDto | null>(null);
  private _isLoading = signal(false);
  private _error = signal<string | null>(null);

  // 🔌 Estado de conexión WebSocket (readonly)
  public readonly wsConnected = this.chatWs.isConnected.asReadonly();
  public readonly wsConnecting = this.chatWs.isConnecting.asReadonly();
  public readonly wsError = this.chatWs.connectionError.asReadonly();

  // 📖 Computed signals públicos
  public readonly activeConversation = this._activeConversation.asReadonly();
  public readonly isLoading = this._isLoading.asReadonly();
  public readonly error = this._error.asReadonly();
  
  // Computed para acceso fácil
  public readonly otherUser = computed(() => this._activeConversation()?.otherUser || null);
  public readonly messages = computed(() => this._activeConversation()?.messages || []);

  constructor() {
    // Configurar callbacks de WebSocket (sin auto-conectar)
    this.chatWs.onNewMessage((wsMessage) => this.handleNewMessage(wsMessage));
    this.chatWs.onReadReceipt((receipt) => this.handleReadReceipt(receipt));

    // 🔄 Observar cambios de autenticación para auto-conectar/desconectar WebSocket
    effect(() => {
      const isAuthenticated = this.authStore.isAuthenticated();
      
      if (isAuthenticated) {
        console.log('🔌 Usuario autenticado - conectando WebSocket automáticamente');
        this.connectWebSocket();
      } else {
        console.log('❌ Usuario no autenticado - desconectando WebSocket automáticamente');
        this.disconnectWebSocket();
      }
    });
  }

  /**
   * 🔌 Conectar WebSocket (llamado automáticamente por effect cuando usuario se autentica)
   */
  connectWebSocket(): void {
    console.log('🔌 Iniciando conexión WebSocket desde ChatStore');
    this.chatWs.connect();
  }

  /**
   * 🔌 Desconectar WebSocket (llamado automáticamente por effect cuando usuario sale)
   */
  disconnectWebSocket(): void {
    console.log('❌ Desconectando WebSocket desde ChatStore');
    this.chatWs.disconnect();
    this.closeConversation(); // Limpiar estado también
  }

  /**
   * 📂 Abrir conversación con un usuario
   * ♻️ SIEMPRE recarga todo desde cero (enfoque híbrido)
   */
  openConversation(otherUserId: string): void {
    console.log('📂 Abriendo conversación con:', otherUserId);
    
    this._isLoading.set(true);
    this._error.set(null);
    this._activeConversation.set(null);

    // Cargar conversación completa desde HTTP
    this.chatApi.getMessages(otherUserId).subscribe({
      next: (conversation) => {
        this._activeConversation.set(conversation);
        this._isLoading.set(false);
        console.log('✅ Conversación cargada:', conversation);
        
        // Marcar como leídos si WebSocket está conectado
        if (this.wsConnected()) {
          this.markMessagesAsRead(otherUserId);
        }
      },
      error: (error) => {
        console.error('❌ Error cargando conversación:', error);
        this._error.set('Error cargando mensajes');
        this._isLoading.set(false);
      }
    });
  }

  /**
   * 📤 Enviar mensaje
   */
  sendMessage(content: string): void {
    const conversation = this._activeConversation();
    if (!conversation || !this.wsConnected()) {
      console.error('❌ No se puede enviar: sin conversación o WebSocket');
      return;
    }

    const trimmedContent = content.trim();
    if (!trimmedContent) return;

    console.log('📤 Enviando mensaje:', trimmedContent);

    // Enviar via WebSocket
    this.chatWs.sendMessage({
      recipientId: conversation.otherUser.userId,
      content: trimmedContent
    });
  }

  /**
   * 👁️ Marcar mensajes como leídos
   */
  markMessagesAsRead(otherUserId: string): void {
    if (!this.wsConnected()) {
      console.log('⏳ WebSocket no conectado, no se pueden marcar mensajes como leídos');
      return;
    }

    console.log('👁️ Marcando mensajes como leídos para:', otherUserId);
    this.chatWs.markMessagesAsRead({ otherUserId });
  }

  /**
   * ❌ Cerrar conversación activa
   * 🗑️ Limpia todo el estado (enfoque híbrido)
   */
  closeConversation(): void {
    console.log('❌ Cerrando conversación activa');
    this._activeConversation.set(null);
    this._isLoading.set(false);
    this._error.set(null);
  }

  /**
   * 📨 Manejar nuevo mensaje de WebSocket (ENFOQUE HÍBRIDO)
   */
  private handleNewMessage(wsMessage: WebSocketMessageDto): void {
    const message = wsMessage.message;
    const currentUserId = this.authStore.currentUser()?.id;
    const activeConv = this._activeConversation();

    console.log('📨 Mensaje WebSocket recibido:', message);

    // CASO 1: Si hay conversación activa y el mensaje es para ella → Actualizar
    if (activeConv) {
      const isForActiveConversation = 
        (message.senderId === activeConv.otherUser.userId) ||
        (message.senderId === currentUserId && wsMessage.recipientId === activeConv.otherUser.userId);

      if (isForActiveConversation) {
        console.log('✅ Mensaje para conversación activa, actualizando');
        this._activeConversation.set({
          ...activeConv,
          messages: [...activeConv.messages, message]
        });
        return;
      }
    }

    // CASO 2: Mensaje para otra conversación → Solo notificar si NO es nuestro mensaje
    if (message.senderId !== currentUserId) {
      console.log('🔔 Mensaje recibido de otro usuario, notificando social-store:', message.senderId);
      this.socialStore.setNewMessages(message.senderId, true);
    }
    // Si es nuestro mensaje → no hacer nada (ya se procesó en el dispositivo que envió)
  }

  /**
   * 👁️ Manejar confirmación de lectura de WebSocket
   */
  private handleReadReceipt(receipt: WebSocketReadReceiptDto): void {
    const conversation = this._activeConversation();
    if (!conversation) return;

    const currentUserId = this.authStore.currentUser()?.id;
    
    console.log('👁️ Confirmación de lectura recibida:', receipt);

    // Solo si es para la conversación activa
    if (receipt.readByUserId === conversation.otherUser.userId) {
      console.log('✅ Marcando mensajes enviados como leídos');
      this._activeConversation.set({
        ...conversation,
        messages: conversation.messages.map(msg => 
          msg.senderId === currentUserId ? { ...msg, isRead: true } : msg
        )
      });
    }
  }
}