import { Component, input, output, signal, computed, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AvatarComponent } from '@shared/components/avatar/avatar';
import { ChatStore } from '@core/stores/chat-store';

export interface ChatModalData {
  userId: string;
  username: string;
}

@Component({
  selector: 'app-chat-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AvatarComponent
  ],
  templateUrl: './chat-modal.html',
  styleUrl: './chat-modal.scss'
})
export class ChatModalComponent {
  private chatStore = inject(ChatStore);
  
  // 📥 Inputs
  isOpen = input<boolean>(false);
  userData = input.required<ChatModalData>();
  
  // 📤 Outputs
  close = output<void>();
  
  // 🎯 Estado interno
  messageText = signal('');
  isTyping = signal(false);
  
  // 💬 Estado del chat desde ChatStore
  activeConversation = this.chatStore.activeConversation;
  messages = this.chatStore.messages;
  isLoading = this.chatStore.isLoading;
  error = this.chatStore.error;
  wsConnected = this.chatStore.wsConnected;
  
  // 📊 Computed
  hasMessages = computed(() => this.messages().length > 0);
  canSend = computed(() => this.messageText().trim().length > 0 && this.wsConnected());
  
  // 🛡️ Helper seguro para obtener el userId del otro usuario
  otherUserId = computed(() => this.activeConversation()?.otherUser?.userId ?? 'unknown');

  constructor() {
    // 🔄 Abrir conversación cuando se abre el modal
    effect(() => {
      const userData = this.userData();
      const isOpen = this.isOpen();
      
      if (isOpen && userData) {
        console.log('📂 Abriendo conversación con:', userData.username);
        this.chatStore.openConversation(userData.userId);
      }
    });

    // 🔄 Cerrar conversación cuando se cierra el modal
    effect(() => {
      const isOpen = this.isOpen();
      
      if (!isOpen) {
        console.log('❌ Cerrando conversación');
        this.chatStore.closeConversation();
      }
    });
  }
  
  /**
   * ❌ Cerrar modal
   */
  onClose(): void {
    this.close.emit();
  }
  
  /**
   * 📤 Enviar mensaje
   */
  onSendMessage(): void {
    const text = this.messageText().trim();
    if (text && this.wsConnected()) {
      console.log('📤 Enviando mensaje:', text);
      
      // Enviar via ChatStore
      this.chatStore.sendMessage(text);
      this.messageText.set('');
    } else if (!this.wsConnected()) {
      console.error('❌ WebSocket no conectado, no se puede enviar mensaje');
    }
  }
  
  /**
   * ⌨️ Manejar Enter en textarea
   */
  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.onSendMessage();
    }
  }
  
  /**
   * 🕐 Formatear timestamp
   */
  formatTime(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('es-ES', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  }
  
  /**
   * 📅 Formatear fecha relativa
   */
  formatRelativeTime(timestamp: string): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
    const diffMinutes = Math.floor(diffTime / (1000 * 60));
    
    if (diffHours >= 24) {
      const diffDays = Math.floor(diffHours / 24);
      return `hace ${diffDays} día${diffDays > 1 ? 's' : ''}`;
    } else if (diffHours > 0) {
      return `hace ${diffHours} hora${diffHours > 1 ? 's' : ''}`;
    } else if (diffMinutes > 0) {
      return `hace ${diffMinutes} minuto${diffMinutes > 1 ? 's' : ''}`;
    } else {
      return 'ahora';
    }
  }
}