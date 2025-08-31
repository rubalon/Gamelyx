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

  // 🎯 Lista local para manejar reintentos (se resetea en cada búsqueda)
  private availableGamesForSuggestion: any[] = [];
  
  // 🔍 Flag para trackear si ya se ha iniciado alguna búsqueda
  private hasStartedSearch = false;

  // 🎯 Flag para trackear si ya se procesó al menos una sugerencia (aceptada/rechazada)
  private hasProcessedSuggestions = false;

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

  // 🎯 Verificar si hay juegos disponibles para sugerencias
  get hasAvailableGamesForSuggestion() {
    return this.availableGamesForSuggestion.length > 0;
  }

  // 🚫 Verificar si se agotaron todos los juegos sin encontrar sugerencias
  get allGamesExhausted() {
    return this.hasStartedSearch && 
           this.preferredGames.length > 0 && 
           this.availableGamesForSuggestion.length === 0 && 
           !this.currentSuggestion;
  }

  // 🎯 Verificar si es el primer intento fallido (sin sugerencias procesadas previamente)
  get isFirstAttemptExhausted() {
    return this.allGamesExhausted && !this.hasProcessedSuggestions;
  }

  // 🔄 Verificar si ya se procesaron sugerencias y se agotaron las opciones
  get hasExhaustedAfterProcessing() {
    return this.allGamesExhausted && this.hasProcessedSuggestions;
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
    
    // 🔍 Marcar que se ha iniciado una búsqueda
    this.hasStartedSearch = true;
    
    // 🔄 Inicializar lista de juegos disponibles (copia completa)
    this.availableGamesForSuggestion = [...this.preferredGames];
    
    // 🎯 Iniciar proceso de búsqueda con reintentos
    this.searchSuggestionWithRetries();
  }

  /**
   * 🎯 Buscar sugerencia con sistema de reintentos automáticos
   * Elimina juegos sin resultados y reintenta hasta encontrar uno o agotar opciones
   */
  private searchSuggestionWithRetries(): void {
    // ❌ Si no quedan juegos disponibles, mostrar estado sin juegos
    if (this.availableGamesForSuggestion.length === 0) {
      console.log('❌ Se agotaron todos los juegos disponibles sin encontrar sugerencias');
      // El template ya maneja el caso cuando totalPreferredGames === 0
      // Pero necesitamos limpiar cualquier sugerencia anterior
      this.socialStore.clearCurrentSuggestion();
      return;
    }

    // 🎲 Seleccionar juego aleatoriamente con ponderación por rating
    const selectedGame = this.selectWeightedRandomGame(this.availableGamesForSuggestion);
    
    if (!selectedGame) {
      console.log('❌ No se pudo seleccionar un juego');
      return;
    }
    
    console.log('🎮 Intentando con juego:', selectedGame.gameName, 'con rating:', selectedGame.userRating);
    console.log(`📊 Juegos disponibles restantes: ${this.availableGamesForSuggestion.length}`);
    
    // 🔍 Llamar al API para obtener sugerencia
    this.socialStore.getFriendSuggestionByGame(selectedGame.gameSlug, selectedGame.userRating).subscribe({
      next: (suggestion) => {
        if (suggestion) {
          // ✅ Sugerencia encontrada
          console.log('✅ Sugerencia obtenida:', suggestion);
        } else {
          // 🚫 No hay sugerencias para este juego, eliminarlo y reintentar
          console.log(`🚫 No hay sugerencias para ${selectedGame.gameName}, eliminando y reintentando...`);
          this.removeGameAndRetry(selectedGame.gameSlug);
        }
      },
      error: (error) => {
        console.error('❌ Error obteniendo sugerencia:', error);
        // En caso de error, también eliminar el juego y reintentar
        this.removeGameAndRetry(selectedGame.gameSlug);
      }
    });
  }

  /**
   * 🗑️ Eliminar juego de la lista disponible y reintentar
   */
  private removeGameAndRetry(gameSlugToRemove: string): void {
    // Eliminar el juego que no tuvo resultados
    this.availableGamesForSuggestion = this.availableGamesForSuggestion.filter(
      game => game.gameSlug !== gameSlugToRemove
    );
    
    console.log(`🗑️ Juego eliminado. Juegos restantes: ${this.availableGamesForSuggestion.length}`);
    
    // 🔄 Reintentar automáticamente
    this.searchSuggestionWithRetries();
  }

  /**
   * 🔄 Manejar solicitud de nueva sugerencia
   * Se llama cuando se envía/rechaza una sugerencia
   */
  onRequestNewSuggestion(): void {
    console.log('🔄 Requesting new suggestion...');
    // 🎯 Marcar que ya se ha procesado al menos una sugerencia
    this.hasProcessedSuggestions = true;
    // 🔄 Reiniciar proceso completo de búsqueda (resetear lista y buscar de nuevo)
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