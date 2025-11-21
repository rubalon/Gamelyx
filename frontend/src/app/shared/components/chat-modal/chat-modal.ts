import { Component, input, output, signal, computed, inject, effect, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
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
    TranslateModule,
    AvatarComponent
  ],
  templateUrl: './chat-modal.html',
  styleUrl: './chat-modal.scss'
})
export class ChatModalComponent implements AfterViewInit {
  private chatStore = inject(ChatStore);
  private translateService = inject(TranslateService);

  // 📜 ViewChild para el contenedor de mensajes
  @ViewChild('messagesContainer') messagesContainer!: ElementRef<HTMLDivElement>;
  
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
  
  // 📊 Contador de mensajes total en la conversación (para límite de 25)
  totalMessagesCount = computed(() => this.messages().length);
  
  // 🚫 Límite de mensajes alcanzado
  messageLimitReached = computed(() => this.totalMessagesCount() >= 25);
  
  // ✅ Puede enviar mensaje (considerando límite)
  canSend = computed(() => 
    this.messageText().trim().length > 0 && 
    this.wsConnected() && 
    !this.messageLimitReached()
  );
  
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

    // 🔄 Conversación se cierra desde ContactUserCard cuando se cierra el modal
    
    // 📜 Effect para hacer scroll al final cuando cambian los mensajes
    effect(() => {
      const messages = this.messages();
      if (messages.length > 0) {
        // Usar setTimeout para que el DOM se actualice primero
        setTimeout(() => this.scrollToBottom(), 0);
      }
    });
  }

  ngAfterViewInit(): void {
    // Scroll inicial cuando se inicializa la vista
    setTimeout(() => this.scrollToBottom(), 100);
  }

  /**
   * 📜 Hacer scroll al final del área de mensajes
   */
  private scrollToBottom(): void {
    if (this.messagesContainer) {
      const element = this.messagesContainer.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
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
   * ⌨️ Manejar Enter en textarea (respetando límite de mensajes)
   */
  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      // Solo enviar si se puede (respeta límite de mensajes)
      if (this.canSend()) {
        this.onSendMessage();
      }
    }
  }
  
  /**
   * 💬 Obtener subtítulo vacío traducido
   */
  getEmptySubtitle(): string {
    return this.translateService.instant('chat.empty.subtitle', { username: this.userData().username });
  }

  /**
   * 🔢 Obtener texto de contador de caracteres traducido
   */
  getCharacterCountText(): string {
    return this.translateService.instant('chat.input.characterCount', { count: this.messageText().length });
  }

  /**
   * 📊 Obtener texto de contador de mensajes traducido
   */
  getMessageCountText(): string {
    return this.translateService.instant('chat.input.messageCount', { count: this.totalMessagesCount() });
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