// src/app/core/services/social-api.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// 🏷️ Enums (replicados del backend para type safety)
export enum RequestSource {
  SEARCH = 'SEARCH',
  SUGGESTION = 'SUGGESTION'
}

export enum FriendRequestStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED'
}

// 👥 Interfaces para tipado fuerte basadas en los DTOs del backend
export interface UserDto {
  userId: string; // UUID como string en frontend
  username: string;
}

export interface ContactUserDto {
  user: UserDto;
  chatId: string | null; // UUID como string, null si no hay chat
  newMessages: boolean;
}

export interface SharedGameInfoDto {
  gameSlug: string;
  gameName: string;
  yourRating: number;
  theirRating: number;
}

export interface FriendRequestDto {
  requestId: string; // UUID como string
  contactUser: ContactUserDto;
  source: RequestSource;
  status: FriendRequestStatus;
  sharedGame: SharedGameInfoDto | null;
  receivedAt: string; // LocalDateTime como string ISO
}

export interface PreferredGameDto {
  gameSlug: string;
  gameName: string;
  userRating: number;
}

export interface HomeSocialDataDto {
  friends: ContactUserDto[];
  incomingRequests: FriendRequestDto[];
  outgoingRequests: FriendRequestDto[];
  preferredGames: PreferredGameDto[];
}

export interface SendFriendRequestDto {
  targetUserId: string; // UUID como string
  source: RequestSource;
  gameSlug?: string;
  yourRating?: number;
}

export interface SearchedUserDto {
  user: UserDto;
  isFriend: boolean;
  hasPendingRequest: boolean;
  hasRejectedRequest: boolean;
}

export interface UserSearchResultDto {
  query: string;
  users: SearchedUserDto[];
}

export interface SuggestedUserDto {
  user: UserDto;
  sharedGameInfoDto: {
    gameSlug: string;
    gameName: string;
    yourRating: number;
    theirRating: number;
  };
}

export interface FriendRequestResponseDto {
  success: boolean;
  newFriend: ContactUserDto;
}

export interface DeleteFriendResponseDto {
  success: boolean;
  deletedFriendUsername: string;
  deletedFriendId: string; // UUID como string
}

@Injectable({
  providedIn: 'root'
})
export class SocialApiService {
  private http = inject(HttpClient);
  
  // URL base del backend desde environment centralizado
  private readonly API_URL = `${environment.apiUrl}/social`;

  // 🎭 Flag para activar/desactivar mock data
  private readonly USE_MOCK_DATA = false; // Cambiar a false para usar backend real

  /**
   * 🏠 Obtener todos los datos sociales para el home
   * GET /api/social/home-social-data
   * Requiere autenticación JWT
   * 
   * 🎭 TEMPORAL: Usando mock data desde JSON local
   */
  getHomeSocialData(): Observable<HomeSocialDataDto> {
    if (this.USE_MOCK_DATA) {
      // 📁 Cargar datos mock desde JSON local
      return this.http.get<HomeSocialDataDto>('/assets/json/home-mock-data.json')
        .pipe(
          delay(500) // 🕐 Simular latencia de red para testing realista
        );
    }

    // 🌐 Llamada real al backend (cuando USE_MOCK_DATA = false)
    return this.http.get<HomeSocialDataDto>(`${this.API_URL}/home-social-data`);
  }

  /**
   * 📤 Enviar solicitud de amistad
   * POST /api/social/friend-requests
   * Requiere autenticación JWT
   */
  sendFriendRequest(requestData: SendFriendRequestDto): Observable<any> {
    return this.http.post(`${this.API_URL}/friend-requests`, requestData);
  }

  /**
   * ✅❌ Responder a solicitud de amistad
   * PUT /api/social/friend-requests/{requestId}/respond?action=ACCEPT|REJECT
   * Requiere autenticación JWT
   */
  respondToFriendRequest(requestId: string, action: 'ACCEPT' | 'REJECT'): Observable<FriendRequestResponseDto> {
    const params = new HttpParams().set('action', action);
    return this.http.put<FriendRequestResponseDto>(`${this.API_URL}/friend-requests/${requestId}/respond`, null, { params });
  }

  /**
   * 👁️ Marcar solicitud como vista
   * PUT /api/social/friend-requests/{requestId}/mark-notified
   * Requiere autenticación JWT
   */
  markRequestAsNotified(requestId: string): Observable<any> {
    return this.http.put(`${this.API_URL}/friend-requests/${requestId}/mark-notified`, null);
  }

  /**
   * 🔍 Buscar usuarios por nombre/username
   * GET /api/social/search/users?q=query&limit=10
   * Requiere autenticación JWT
   */
  searchUsers(query: string, limit: number = 10): Observable<UserSearchResultDto> {
    const params = new HttpParams()
      .set('q', query)
      .set('limit', limit.toString());
    
    return this.http.get<UserSearchResultDto>(`${this.API_URL}/search/users`, { params });
  }

  /**
   * 💡 Obtener sugerencia de amigo por juego
   * GET /api/social/friend-suggestion/by-game?gameSlug=zelda&userRating=9
   * Requiere autenticación JWT
   * Devuelve 204 No Content si no hay sugerencias
   */
  getFriendSuggestionByGame(gameSlug: string, userRating: number): Observable<SuggestedUserDto | null> {
    const params = new HttpParams()
      .set('gameSlug', gameSlug)
      .set('userRating', userRating.toString());
    
    return this.http.get<SuggestedUserDto>(`${this.API_URL}/friend-suggestion/by-game`, { params });
  }

  /**
   * 🚫 Rechazar sugerencia de amigo
   * POST /api/social/friend-suggestion/reject?rejectedUserId=uuid&gameSlug=zelda
   * Requiere autenticación JWT
   */
  rejectFriendSuggestion(rejectedUserId: string, gameSlug: string): Observable<any> {
    const params = new HttpParams()
      .set('rejectedUserId', rejectedUserId)
      .set('gameSlug', gameSlug);
    
    return this.http.post(`${this.API_URL}/friend-suggestion/reject`, null, { params });
  }

  /**
   * 🗑️ Eliminar amigo
   * DELETE /api/social/friends/{friendId}
   * Requiere autenticación JWT
   */
  deleteFriend(friendId: string): Observable<DeleteFriendResponseDto> {
    return this.http.delete<DeleteFriendResponseDto>(`${this.API_URL}/friends/${friendId}`);
  }
}