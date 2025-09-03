import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// DTOs que coinciden exactamente con el backend
export interface MessageDto {
  messageId: string;
  senderId: string;
  senderUsername: string;
  content: string;
  sentAt: string; // ISO string
  isRead: boolean;
}

export interface ConversationMessagesDto {
  conversationId: string;
  otherUser: {
    userId: string;
    username: string;
  };
  messages: MessageDto[];
  hasMore: boolean;
  currentPage: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ChatApi {
  private baseUrl = `${environment.apiUrl}/chat`;
  private http = inject(HttpClient);

  /**
   * 📥 Cargar historial de conversación con otro usuario
   * Endpoint: GET /api/chat/messages?otherUserId={uuid}&page={n}&limit={n}
   */
  getMessages(otherUserId: string, page: number = 0, limit: number = 25): Observable<ConversationMessagesDto> {
    return this.http.get<ConversationMessagesDto>(`${this.baseUrl}/messages`, {
      params: {
        otherUserId,
        page: page.toString(),
        limit: limit.toString()
      }
    });
  }
}