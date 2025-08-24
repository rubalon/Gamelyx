// src/app/features/home/components/search-toggle/search-toggle.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { GameSearchBar } from '@shared/components/game-search-bar/game-search-bar';
import { SocialStore } from '@core/stores/social-store';

type SearchType = 'games' | 'users';

@Component({
  selector: 'app-search-toggle',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    GameSearchBar
  ],
  templateUrl: './search-toggle.html',
  styleUrl: './search-toggle.scss'
})
export class SearchToggleComponent {
  private socialStore = inject(SocialStore);

  // 🎯 Estado del componente
  searchType = signal<SearchType>('games');
  searchQuery = '';

  // 📊 Datos del store
  get searchResults() {
    return this.socialStore.state().searchResults;
  }

  get isLoadingUsers() {
    return this.socialStore.state().isLoadingSearchUsers;
  }

  /**
   * 🔍 Maneja input de búsqueda de usuarios
   */
  onUserSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery = input.value.trim();
    
    console.log('🔍 User search input:', this.searchQuery);
    
    // TODO: Implementar debounce y llamada al store
    // if (this.searchQuery.length >= 2) {
    //   this.socialStore.searchUsers(this.searchQuery).subscribe();
    // }
  }

  /**
   * 📤 Enviar solicitud de amistad  
   */
  onSendFriendRequest(userId: string): void {
    console.log('📤 Send friend request to:', userId);
    // TODO: Implementar funcionalidad
  }
}