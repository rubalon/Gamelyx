// src/app/features/home/components/friend-finder/friend-finder.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { SocialStore } from '@core/stores/social-store';

@Component({
  selector: 'app-friend-finder',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule
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
   * 📤 Enviar solicitud a usuario sugerido (sin funcionalidad)
   */
  onSendRequestToSuggestion(userId: string, username: string, gameSlug: string): void {
    console.log('📤 Send request to suggested user:', username, 'based on game:', gameSlug);
    // TODO: Implementar envío de solicitud por sugerencia
  }

  /**
   * ❌ Rechazar sugerencia (sin funcionalidad)
   */
  onRejectSuggestion(userId: string, username: string, gameSlug: string): void {
    console.log('❌ Reject suggestion:', username, 'for game:', gameSlug);
    // TODO: Implementar rechazo de sugerencia
  }

  /**
   * 🎨 Generar avatar con iniciales
   */
  getAvatarInitial(username: string): string {
    return username.charAt(0).toUpperCase();
  }

  /**
   * 🌈 Generar color de avatar basado en username
   */
  getAvatarColor(username: string): string {
    const colors = [
      'from-purple-400 to-blue-500',
      'from-pink-400 to-purple-500', 
      'from-blue-400 to-cyan-500',
      'from-green-400 to-blue-500',
      'from-yellow-400 to-orange-500',
      'from-red-400 to-pink-500'
    ];
    
    const hash = username.split('').reduce((a, b) => {
      a = ((a << 5) - a) + b.charCodeAt(0);
      return a & a;
    }, 0);
    
    return colors[Math.abs(hash) % colors.length];
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