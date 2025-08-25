// src/app/features/home/components/friend-finder/friend-finder.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { SocialStore } from '@core/stores/social-store';
import { FriendFinderResultComponent } from './components/friend-finder-result/friend-finder-result';

@Component({
  selector: 'app-friend-finder',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    FriendFinderResultComponent
  ],
  templateUrl: './friend-finder.html',
  styleUrl: './friend-finder.scss'
})
export class FriendFinderComponent {
  private socialStore = inject(SocialStore);

  // 📊 Datos del store
  get preferredGames() {
    return this.socialStore.state().preferredGames;
  }

  get totalPreferredGames() {
    return this.preferredGames.length;
  }

  get isLoading() {
    return this.socialStore.state().isLoadingHomeSocialData;
  }

  get currentSuggestion() {
    return this.socialStore.state().currentSuggestion;
  }

  /**
   * 🔍 Buscar amigos que compartan gustos (sin funcionalidad)
   */
  onFindFriends(): void {
    console.log('🔍 Find friends with similar tastes');
    console.log('📊 Based on preferred games:', this.preferredGames);
    // TODO: Implementar funcionalidad de búsqueda automática
  }

  /**
   * 📤 Enviar solicitud a usuario sugerido (delegado desde child component)
   */
  onSendRequestToSuggestion(data: {userId: string, username: string, gameSlug: string}): void {
    console.log('📤 Send request to suggested user:', data.username, 'based on game:', data.gameSlug);
    // TODO: Implementar envío de solicitud por sugerencia
  }

  /**
   * ❌ Rechazar sugerencia (delegado desde child component)
   */
  onRejectSuggestion(data: {userId: string, username: string, gameSlug: string}): void {
    console.log('❌ Reject suggestion:', data.username, 'for game:', data.gameSlug);
    // TODO: Implementar rechazo de sugerencia
  }

  /**
   * ⭐ Obtener emoji de rating
   */
  getRatingEmoji(rating: number): string {
    if (rating >= 9) return '🌟';
    if (rating >= 8) return '⭐';
    if (rating >= 7) return '✨';
    return '🔹';
  }

  /**
   * 🎮 Formatear lista de juegos para mostrar
   */
  getTopGames(limit: number = 3): typeof this.preferredGames {
    return this.preferredGames
      .sort((a, b) => b.userRating - a.userRating)
      .slice(0, limit);
  }
}