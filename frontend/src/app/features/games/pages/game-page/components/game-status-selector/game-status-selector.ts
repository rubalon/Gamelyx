// src/app/features/games/pages/game-page/components/game-status-selector/game-status-selector.ts
import { Component, inject, signal, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { GameDetails, GameApiService, UpdateReviewRequest, UpdateReviewResponse } from '@core/services/game-api';
import { finalize, catchError, of } from 'rxjs';

export type GameStatus = 'WISHLIST' | 'PLAYING' | 'COMPLETED' | 'ARCHIVED' | null;

interface StatusOption {
  value: GameStatus;
  labelKey: string;
  icon: string;
  activeClass: string;
  inactiveClass: string;
}

@Component({
  selector: 'app-game-status-selector',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './game-status-selector.html',
  styleUrl: './game-status-selector.scss'
})
export class GameStatusSelector {
  // 📡 Signal-based inputs/outputs (MODERNIZADO)
  gameDetails = input.required<GameDetails>();
  statusUpdated = output<UpdateReviewResponse>();

  // 🏪 Dependencies
  private gameApiService = inject(GameApiService);

  // 🎯 Component state con Signals
  isUpdating = signal(false);
  updateError = signal<string | null>(null);

  // 💫 Computed values
  selectedStatus = computed(() => {
    return this.gameDetails()?.myStatus?.status || null;
  });

  gameSlug = computed(() => {
    return this.gameDetails()?.slug || '';
  });

  // 📋 Definición de los estados disponibles
  statusOptions: StatusOption[] = [
    {
      value: 'WISHLIST',
      labelKey: 'games.status.wishlist',
      icon: 'bookmark',
      activeClass: 'bg-purple-500/20 text-purple-400 border-purple-500',
      inactiveClass: 'bg-gray-700 text-gray-400 border-gray-600 hover:bg-gray-600'
    },
    {
      value: 'PLAYING',
      labelKey: 'games.status.playing',
      icon: 'play',
      activeClass: 'bg-blue-500/20 text-blue-400 border-blue-500',
      inactiveClass: 'bg-gray-700 text-gray-400 border-gray-600 hover:bg-gray-600'
    },
    {
      value: 'COMPLETED',
      labelKey: 'games.status.completed',
      icon: 'check',
      activeClass: 'bg-green-500/20 text-green-400 border-green-500',
      inactiveClass: 'bg-gray-700 text-gray-400 border-gray-600 hover:bg-gray-600'
    },
    {
      value: 'ARCHIVED',
      labelKey: 'games.status.archived',
      icon: 'archive',
      activeClass: 'bg-gray-500/20 text-gray-400 border-gray-500',
      inactiveClass: 'bg-gray-700 text-gray-400 border-gray-600 hover:bg-gray-600'
    }
  ];

  /**
   * 🎯 Manejar el cambio de estado
   */
  onStatusClick(status: GameStatus): void {
    // Evitar múltiples clicks mientras se procesa
    if (this.isUpdating()) return;

    const currentStatus = this.selectedStatus();
    let newStatus: GameStatus = null;

    // Toggle: Si ya está seleccionado, lo deseleccionamos
    if (currentStatus === status) {
      newStatus = null;
    } else {
      newStatus = status;
    }

    this.updateGameStatus(newStatus);
  }

  /**
   * 💾 Actualizar el estado del juego en el backend
   */
  private updateGameStatus(newStatus: GameStatus): void {
    console.log('Actualizando estado a:', newStatus);

    // Preparar los datos para actualizar
    const updateData: UpdateReviewRequest = {};

    if (newStatus) {
      updateData.status = newStatus;
    }

    // Mantener los valores existentes si los hay (como en tu code original)
    const currentUserStatus = this.gameDetails().myStatus;
    if (currentUserStatus?.rating) {
      updateData.rating = currentUserStatus.rating;
    }
    if (currentUserStatus?.reviewText) {
      updateData.reviewText = currentUserStatus.reviewText;
    }

    // Indicar que estamos actualizando
    this.isUpdating.set(true);
    this.updateError.set(null);

    // Llamar al servicio con manejo de errores mejorado
    this.gameApiService.updateMyReview(this.gameSlug(), updateData)
      .pipe(
        catchError(error => {
          console.error('Error al actualizar el estado:', error);

          // Manejo de errores específicos (como en tu ReviewModal)
          if (error.status === 401) {
            this.updateError.set('Tu sesión ha expirado. Por favor, inicia sesión de nuevo.');
          } else if (error.status === 404) {
            this.updateError.set('El juego no fue encontrado.');
          } else if (error.status >= 500) {
            this.updateError.set('Error del servidor. Por favor, intenta de nuevo más tarde.');
          } else {
            this.updateError.set('Error al actualizar el estado. Por favor, intenta de nuevo.');
          }

          return of(null);
        }),
        finalize(() => this.isUpdating.set(false))
      )
      .subscribe(response => {
        if (response) {
          console.log('Estado actualizado exitosamente:', response);
          
          // 🆕 CAMBIO: Emitir usando signal output
          this.statusUpdated.emit(response);
          
          // Limpiar error si todo fue bien
          this.updateError.set(null);
        }
      });
  }

  /**
   * 🎨 Obtener las clases CSS para un botón de estado
   */
  getStatusButtonClass(status: GameStatus): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    if (!option) return '';

    const isSelected = this.selectedStatus() === status;
    return isSelected ? option.activeClass : option.inactiveClass;
  }

  /**
   * 🎨 Obtener el color del texto del label basado en el estado
   */
  getLabelColor(status: GameStatus): string {
    const isSelected = this.selectedStatus() === status;
    if (!isSelected) return 'text-gray-400';

    switch (status) {
      case 'WISHLIST': return 'text-purple-400';
      case 'PLAYING': return 'text-blue-400';
      case 'COMPLETED': return 'text-green-400';
      case 'ARCHIVED': return 'text-gray-400';
      default: return 'text-gray-400';
    }
  }

  /**
   * 🎯 Obtener path del icono SVG
   */
  getIconPath(icon: string): string {
    const icons: Record<string, string> = {
      bookmark: 'M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z',
      play: 'M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
      check: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
      archive: 'M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4'
    };

    return icons[icon] || '';
  }

  /**
   * 🛡️ Verificar si un botón debe estar deshabilitado
   */
  isButtonDisabled(): boolean {
    return this.isUpdating();
  }

  /**
   * 📋 Obtener clases CSS para botones deshabilitados
   */
  getDisabledClasses(): string {
    return this.isUpdating() ? 'opacity-50 cursor-not-allowed' : '';
  }

  /**
   * 🎨 Obtener clases CSS para el estado de error
   */
  getErrorClasses(): string {
    return 'text-red-400 text-sm mt-2';
  }

  /**
   * 🔄 Obtener texto del estado de carga
   */
  getLoadingText(): string {
    return 'Actualizando...';
  }

  /**
   * 📊 Obtener estado actual legible para debugging
   */
  getCurrentStatusInfo(): string {
    const status = this.selectedStatus();
    if (!status) return 'Sin estado';
    
    const option = this.statusOptions.find(opt => opt.value === status);
    return option ? option.labelKey : status;
  }
}