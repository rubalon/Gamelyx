// src/app/features/home/components/friend-finder/friend-finder.ts
import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { SocialStore } from '@core/stores/social-store';
import { ContactUserCard, ContactUserData } from '@shared/components/contact-user-card/contact-user-card';

@Component({
  selector: 'app-friend-finder',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    ContactUserCard
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

  // 🔄 Computed para convertir sugerencia a ContactUserData
  suggestionAsContactData = computed(() => {
    const suggestion = this.currentSuggestion;
    if (!suggestion) return null;

    return {
      requestId: '', // No aplica para sugerencias
      contactUser: {
        user: {
          userId: suggestion.user.userId,
          username: suggestion.user.username
        },
        chatId: null,
        newMessages: false
      },
      receivedAt: new Date().toISOString(), // Tiempo actual
      status: 'SUGGESTION', 
      sharedGame: {
        gameSlug: suggestion.sharedGameInfoDto.gameSlug,
        gameName: suggestion.sharedGameInfoDto.gameName,
        yourRating: suggestion.sharedGameInfoDto.yourRating,
        theirRating: suggestion.sharedGameInfoDto.theirRating
      }
    } as ContactUserData;
  });

  /**
   * 🔍 Buscar amigos que compartan gustos
   */
  onFindFriends(): void {
    console.log('🔍 Find friends with similar tastes');
    
    if (this.preferredGames.length === 0) {
      console.log('❌ No tienes juegos favoritos para buscar sugerencias');
      return;
    }
    
    // Seleccionar juego aleatoriamente con ponderación por rating
    const selectedGame = this.selectWeightedRandomGame(this.preferredGames);
    
    if (!selectedGame) {
      console.log('❌ No se pudo seleccionar un juego');
      return;
    }
    
    console.log('🎮 Juego seleccionado:', selectedGame.gameName, 'con rating:', selectedGame.userRating);
    
    // Llamar al API para obtener sugerencia
    this.socialStore.getFriendSuggestionByGame(selectedGame.gameSlug, selectedGame.userRating).subscribe({
      next: (suggestion) => {
        console.log('✅ Sugerencia obtenida:', suggestion);
      },
      error: (error) => {
        console.error('❌ Error obteniendo sugerencia:', error);
      }
    });
  }

  /**
   * 🔄 Manejar solicitud de nueva sugerencia
   * Se llama cuando se envía/rechaza una sugerencia
   */
  onRequestNewSuggestion(): void {
    console.log('🔄 Requesting new suggestion...');
    // Volver a buscar amigos automáticamente
    this.onFindFriends();
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

  /**
   * 🎲 Seleccionar juego aleatoriamente con ponderación por rating
   * Los juegos con mayor rating tienen más probabilidad de ser seleccionados
   */
  private selectWeightedRandomGame(games: typeof this.preferredGames) {
    if (games.length === 0) return null;
    if (games.length === 1) return games[0];

    // Crear pesos basados en el rating (rating^2 para dar más peso a los más altos)
    const weights = games.map(game => Math.pow(game.userRating, 2));
    const totalWeight = weights.reduce((sum, weight) => sum + weight, 0);
    
    // Generar número aleatorio
    const random = Math.random() * totalWeight;
    
    // Encontrar el juego correspondiente
    let cumulativeWeight = 0;
    for (let i = 0; i < games.length; i++) {
      cumulativeWeight += weights[i];
      if (random <= cumulativeWeight) {
        return games[i];
      }
    }
    
    // Fallback (no debería ocurrir)
    return games[games.length - 1];
  }
}