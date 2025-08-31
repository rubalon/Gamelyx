// src/app/core/stores/social-store.ts
import { Injectable, inject, signal, computed, Signal } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { 
  SocialApiService, 
  HomeSocialDataDto,
  ContactUserDto,
  FriendRequestDto,
  PreferredGameDto,
  SendFriendRequestDto,
  UserSearchResultDto,
  SuggestedUserDto,
  DeleteFriendResponseDto,
  FriendRequestResponseDto,
  RequestSource,
  FriendRequestStatus
} from '@core/services/social-api';

// 🏗️ Estado global de la funcionalidad social
export interface SocialState {
  // 📊 Datos del home (usando las interfaces corregidas)
  friends: ContactUserDto[];
  incomingRequests: FriendRequestDto[];
  outgoingRequests: FriendRequestDto[];
  preferredGames: PreferredGameDto[];
  
  // 🔄 Estados de loading específicos
  isLoadingHomeSocialData: boolean;
  isLoadingSendRequest: boolean;
  isLoadingSearchUsers: boolean;
  isLoadingRespondRequest: boolean;
  
  // ❌ Error handling
  error: string | null;
  
  // 🔍 Estado de búsqueda de usuarios
  searchResults: UserSearchResultDto | null;
  
  // 💡 Estado de sugerencias
  currentSuggestion: SuggestedUserDto | null;
  
  // 📈 Estadísticas rápidas (calculadas dinámicamente)
  totalFriends: number;
  totalIncomingRequests: number;
  totalOutgoingRequests: number;
}

@Injectable({
  providedIn: 'root'
})
export class SocialStore {
  private socialApi = inject(SocialApiService);

  // 🎯 Estado principal usando signals (Angular 20)
  private _socialState = signal<SocialState>({
    friends: [],
    incomingRequests: [],
    outgoingRequests: [],
    preferredGames: [],
    
    isLoadingHomeSocialData: false,
    isLoadingSendRequest: false,
    isLoadingSearchUsers: false,
    isLoadingRespondRequest: false,
    
    error: null,
    
    searchResults: null,
    currentSuggestion: null,
    
    totalFriends: 0,
    totalIncomingRequests: 0,
    totalOutgoingRequests: 0
  });

  // 📖 Estado público readonly - ACCESO DIRECTO
  public readonly state = this._socialState.asReadonly();

  /**
   * 🎯 Selector dinámico para acceder a propiedades específicas
   * Patrón recomendado: crear computed signals bajo demanda
   */
  public select<K extends keyof SocialState>(key: K): Signal<SocialState[K]> {
    return computed(() => this._socialState()[key]);
  }

  // 🧮 Computed signals corregidos para el componente
  public readonly incomingRequests = computed(() => 
    this._socialState().incomingRequests
  );
  
  public readonly outgoingRequests = computed(() => 
    this._socialState().outgoingRequests
  );

  // 📊 Computed para estadísticas calculadas 
  public readonly totalFriends = computed(() => this._socialState().friends.length);
  public readonly totalIncomingRequests = computed(() => this.incomingRequests().length);
  public readonly totalOutgoingRequests = computed(() => this.outgoingRequests().length);

  /**
   * 🔄 Actualiza el estado social
   */
  private updateSocialState(newState: Partial<SocialState>): void {
    this._socialState.update(current => ({
      ...current,
      ...newState
    }));
  }

  /**
   * 📊 Recalcula estadísticas basadas en los arrays actuales
   */
  private updateStatistics(): void {
    const current = this._socialState();
    this.updateSocialState({
      totalFriends: current.friends.length,
      totalIncomingRequests: current.incomingRequests.length,
      totalOutgoingRequests: current.outgoingRequests.length
    });
  }

  /**
   * ❌ Maneja errores HTTP y los convierte a mensajes legibles
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Ha ocurrido un error inesperado en el sistema social';

    if (error.error && typeof error.error === 'object') {
      errorMessage = error.error.message || error.error.error || errorMessage;
    } else if (error.error && typeof error.error === 'string') {
      errorMessage = error.error;
    } else if (error.message) {
      errorMessage = error.message;
    }

    this.updateSocialState({ error: errorMessage });
    return throwError(() => errorMessage);
  }

  /**
   * 🏠 Cargar todos los datos sociales para el home
   * Método principal que se llamará desde HomePage
   */
  loadHomeSocialData(): Observable<HomeSocialDataDto> {
    this.updateSocialState({ 
      isLoadingHomeSocialData: true, 
      error: null 
    });

    return this.socialApi.getHomeSocialData()
      .pipe(
        tap(data => {
          console.log('✅ Home social data loaded:', data);
          
          // 📊 Actualizar estado con datos recibidos
          this.updateSocialState({
            friends: data.friends,
            incomingRequests: data.incomingRequests,
            outgoingRequests: data.outgoingRequests,
            preferredGames: data.preferredGames,
            isLoadingHomeSocialData: false,
            error: null
          });
          
          // 📈 Recalcular estadísticas
          this.updateStatistics();
        }),
        catchError(error => {
          console.error('❌ Error loading home social data:', error);
          this.updateSocialState({ isLoadingHomeSocialData: false });
          return this.handleError(error);
        })
      );
  }

  /**
   * 📤 Enviar solicitud de amistad
   */
  sendFriendRequest(requestData: SendFriendRequestDto): Observable<any> {
    this.updateSocialState({ 
      isLoadingSendRequest: true, 
      error: null 
    });

    return this.socialApi.sendFriendRequest(requestData)
      .pipe(
        tap(response => {
          console.log('✅ Friend request sent:', response);
          this.updateSocialState({
            isLoadingSendRequest: false,
            error: null
          });
          
        }),
        catchError(error => {
          console.error('❌ Error sending friend request:', error);
          this.updateSocialState({ isLoadingSendRequest: false });
          return this.handleError(error);
        })
      );
  }

  /**
   * ✅❌ Responder a solicitud de amistad
   * 🔧 CORREGIDO: No auto-refresh para evitar circular updates
   */
  respondToFriendRequest(requestId: string, action: 'ACCEPT' | 'REJECT'): Observable<FriendRequestResponseDto> {
    this.updateSocialState({ 
      isLoadingRespondRequest: true, 
      error: null 
    });

    return this.socialApi.respondToFriendRequest(requestId, action)
      .pipe(
        tap(response => {
          console.log(`✅ Friend request ${action.toLowerCase()}ed:`, response);
          this.updateSocialState({
            isLoadingRespondRequest: false,
            error: null
          });
          

        }),
        catchError(error => {
          console.error(`❌ Error ${action.toLowerCase()}ing friend request:`, error);
          this.updateSocialState({ isLoadingRespondRequest: false });
          return this.handleError(error);
        })
      );
  }

  /**
   * 👁️ Marcar solicitud como notificada y eliminarla de outgoing requests
   */
  markAsNotified(requestId: string): Observable<any> {
    return this.socialApi.markRequestAsNotified(requestId)
      .pipe(
        tap(() => {
          console.log('✅ Request marked as notified:', requestId);
          // Eliminar de outgoingRequests
          const currentState = this._socialState();
          const updatedOutgoing = currentState.outgoingRequests.filter(
            request => request.requestId !== requestId
          );
          
          this.updateSocialState({
            outgoingRequests: updatedOutgoing,
            totalOutgoingRequests: updatedOutgoing.length
          });
        }),
        catchError(error => {
          console.error('❌ Error marking request as notified:', error);
          return this.handleError(error);
        })
      );
  }

  /**
   * 🔍 Buscar usuarios
   */
  searchUsers(query: string, limit: number = 10): Observable<UserSearchResultDto> {
    this.updateSocialState({ 
      isLoadingSearchUsers: true, 
      error: null 
    });

    return this.socialApi.searchUsers(query, limit)
      .pipe(
        tap(results => {
          console.log('✅ User search results:', results);
          this.updateSocialState({
            searchResults: results,
            isLoadingSearchUsers: false,
            error: null
          });
        }),
        catchError(error => {
          console.error('❌ Error searching users:', error);
          this.updateSocialState({ isLoadingSearchUsers: false });
          return this.handleError(error);
        })
      );
  }

  /**
   * 🗑️ Eliminar amigo
   */
  deleteFriend(friendId: string): Observable<DeleteFriendResponseDto> {
    return this.socialApi.deleteFriend(friendId)
      .pipe(
        tap(response => {
          console.log('✅ Friend deleted:', response);
          

        }),
        catchError(error => {
          console.error('❌ Error deleting friend:', error);
          return this.handleError(error);
        })
      );
  }

  /**
   * 🧹 Limpiar error actual
   */
  clearError(): void {
    this.updateSocialState({ error: null });
  }

  /**
   * 🧹 Limpiar resultados de búsqueda
   */
  clearSearchResults(): void {
    this.updateSocialState({ searchResults: null });
  }

  /**
   * 💡 Obtener sugerencia de amigo por juego
   */
  getFriendSuggestionByGame(gameSlug: string, userRating: number): Observable<SuggestedUserDto | null> {
    this.updateSocialState({ 
      error: null 
    });

    return this.socialApi.getFriendSuggestionByGame(gameSlug, userRating)
      .pipe(
        tap(suggestion => {
          console.log('✅ Friend suggestion received:', suggestion);
          this.updateSocialState({
            currentSuggestion: suggestion,
            error: null
          });
        }),
        catchError(error => {
          console.error('❌ Error getting friend suggestion:', error);
          this.updateSocialState({ currentSuggestion: null });
          return this.handleError(error);
        })
      );
  }

  /**
   * 🚫 Rechazar sugerencia de amigo
   */
  rejectFriendSuggestion(rejectedUserId: string, gameSlug: string): Observable<any> {
    this.updateSocialState({ 
      error: null 
    });

    return this.socialApi.rejectFriendSuggestion(rejectedUserId, gameSlug)
      .pipe(
        tap(response => {
          console.log('✅ Friend suggestion rejected:', response);
          // Limpiar la sugerencia actual después de rechazarla
          this.updateSocialState({
            currentSuggestion: null,
            error: null
          });
        }),
        catchError(error => {
          console.error('❌ Error rejecting friend suggestion:', error);
          return this.handleError(error);
        })
      );
  }

  /**
   * 🧹 Limpiar sugerencia actual
   */
  clearCurrentSuggestion(): void {
    this.updateSocialState({ currentSuggestion: null });
  }

  /**
   * 🔄 Refrescar todos los datos sociales
   * Método de conveniencia para recargar todo
   */
  refreshAllData(): Observable<HomeSocialDataDto> {
    return this.loadHomeSocialData();
  }
}