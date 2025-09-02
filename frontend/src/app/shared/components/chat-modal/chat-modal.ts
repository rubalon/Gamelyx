import { Component, input, output, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AvatarComponent } from '@shared/components/avatar/avatar';

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
  // 📥 Inputs
  isOpen = input<boolean>(false);
  userData = input.required<ChatModalData>();
  
  // 📤 Outputs
  close = output<void>();
  
  // 🎯 Estado interno
  messageText = signal('');
  isTyping = signal(false);
  
  // 💬 Mock messages para el diseño
  messages = signal([
    {
      id: '1',
      content: '¡Hola! ¿Cómo estás?',
      senderId: 'other',
      senderUsername: 'Usuario',
      timestamp: new Date(Date.now() - 3600000).toISOString(),
      isRead: true
    },
    {
      id: '2', 
      content: '¡Muy bien! ¿Qué tal tu partida de Zelda?',
      senderId: 'me',
      senderUsername: 'Yo',
      timestamp: new Date(Date.now() - 1800000).toISOString(),
      isRead: true
    },
    {
      id: '3',
      content: 'Increíble, acabo de completar todos los santuarios. ¡Fue épico!',
      senderId: 'other', 
      senderUsername: 'Usuario',
      timestamp: new Date(Date.now() - 900000).toISOString(),
      isRead: false
    }
  ]);
  
  // 📊 Computed
  hasMessages = computed(() => this.messages().length > 0);
  canSend = computed(() => this.messageText().trim().length > 0);
  
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
    if (text) {
      console.log('📤 Send message:', text, 'to:', this.userData().username);
      
      // Agregar mensaje a la lista local (simulación)
      const newMessage = {
        id: Date.now().toString(),
        content: text,
        senderId: 'me',
        senderUsername: 'Yo',
        timestamp: new Date().toISOString(),
        isRead: false
      };
      
      this.messages.update(current => [...current, newMessage]);
      this.messageText.set('');
      
      // TODO: Aquí se implementará el envío real via WebSocket
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