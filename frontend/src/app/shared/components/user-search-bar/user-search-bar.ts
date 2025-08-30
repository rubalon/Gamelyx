// src/app/shared/components/user-search-bar/user-search-bar.ts
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { Subject, EMPTY } from 'rxjs';
import { SocialStore } from '@core/stores/social-store';
import { RequestSource } from '@core/services/social-api';
import { UserSearchResultCard } from '@shared/components/user-search-result-card/user-search-result-card';

@Component({
  selector: 'app-user-search-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, UserSearchResultCard],
  templateUrl: './user-search-bar.html',
  styleUrl: './user-search-bar.scss'
})
export class UserSearchBar {
  private socialStore = inject(SocialStore);

  // 🎯 Estado del componente con Signals
  searchQuery = '';
  isSearching = signal(false);
  searchError = signal<string | null>(null);
  showResults = signal(false);

  // 🔍 Subject para debounce de búsqueda
  private searchSubject = new Subject<string>();

  // 📊 Datos del store
  get searchResults() {
    return this.socialStore.state().searchResults;
  }

  ngOnInit() {
    // 🚀 Configurar debounce para búsqueda en tiempo real
    this.searchSubject
      .pipe(
        debounceTime(400),
        distinctUntilChanged(),
        switchMap(query => {
          if (query.trim().length < 2) {
            this.showResults.set(false);
            this.socialStore.clearSearchResults();
            return EMPTY;
          }
          
          this.isSearching.set(true);
          this.searchError.set(null);
          
          // 🔍 Llamada real al store para buscar usuarios
          return this.socialStore.searchUsers(query.trim(), 10)
            .pipe(
              catchError(error => {
                console.error('❌ Error in search:', error);
                this.searchError.set('Error al buscar usuarios');
                this.isSearching.set(false);
                this.showResults.set(false);
                return EMPTY;
              })
            );
        })
      )
      .subscribe({
        next: () => {
          this.isSearching.set(false);
          this.showResults.set(true);
        },
        error: (error) => {
          console.error('❌ Search subscription error:', error);
          this.isSearching.set(false);
          this.showResults.set(false);
        }
      });
  }

  /**
   * 🔍 Manejar input de búsqueda
   */
  onInputChange(): void {
    if (this.searchError()) {
      this.searchError.set(null);
    }
    
    // Trigger search con debounce
    this.searchSubject.next(this.searchQuery);
  }

  /**
   * 🔍 Realizar búsqueda manual (Enter o botón)
   */
  performSearch(): void {
    const query = this.searchQuery.trim();
    
    // Validaciones
    if (!query) return;
    if (query.length < 2) {
      this.searchError.set('Mínimo 2 caracteres para buscar');
      return;
    }

    // Limpiar errores previos
    this.searchError.set(null);
    this.searchSubject.next(query);
  }

  /**
   * 📤 Enviar solicitud de amistad  
   */
  onSendFriendRequest(userId: string): void {
    console.log('📤 Send friend request to:', userId);
    
    // 📤 Preparar datos de la solicitud con tipo SEARCH
    const requestData = {
      targetUserId: userId,
      source: RequestSource.SEARCH
      // No se incluyen gameSlug ni yourRating para búsqueda directa
    };
    
    // 🚀 Enviar solicitud a través del store
    this.socialStore.sendFriendRequest(requestData).subscribe({
      next: (response) => {
        console.log('✅ Friend request sent successfully:', response);
        // Actualizar resultados de búsqueda después de enviar solicitud
        if (this.searchQuery.trim().length >= 2) {
          this.socialStore.searchUsers(this.searchQuery.trim(), 10).subscribe();
        }
      },
      error: (error) => {
        console.error('❌ Error sending friend request:', error);
        // El error ya se maneja en el store y se muestra en la UI
      }
    });
  }

  /**
   * 🚪 Cerrar resultados
   */
  closeResults(): void {
    this.showResults.set(false);
  }

  /**
   * 🔍 Obtener estado de loading desde el store
   */
  get isLoadingUsers() {
    return this.socialStore.state().isLoadingSearchUsers;
  }
}