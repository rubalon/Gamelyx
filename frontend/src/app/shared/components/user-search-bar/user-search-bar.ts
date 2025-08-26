// src/app/shared/components/user-search-bar/user-search-bar.ts
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { Subject, EMPTY } from 'rxjs';
import { SocialStore } from '@core/stores/social-store';

@Component({
  selector: 'app-user-search-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
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
            return EMPTY;
          }
          
          this.isSearching.set(true);
          this.searchError.set(null);
          
          // TODO: Reemplazar con la llamada real al store
          return new Promise(resolve => {
            setTimeout(() => resolve({ users: [] }), 1000);
          }).then(() => {
            this.isSearching.set(false);
            this.showResults.set(true);
            return EMPTY;
          }).catch(error => {
            this.searchError.set('Error al buscar usuarios');
            this.isSearching.set(false);
            return EMPTY;
          });
        })
      )
      .subscribe();
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
    // TODO: Implementar funcionalidad real
    // this.socialStore.sendFriendRequest(userId).subscribe();
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