// src/app/features/games/pages/game-page/components/game-status-selector/game-status-selector.ts
import { Component, Input, Output, EventEmitter, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { GameDetails, GameApiService, UpdateReviewRequest, UpdateReviewResponse } from '@core/services/game-api';

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
  @Input({ required: true }) gameDetails!: GameDetails;
  @Output() statusUpdated = new EventEmitter<UpdateReviewResponse>();
  
  private translate = inject(TranslateService);
  private gameApiService = inject(GameApiService);
  
  // Estado de carga para deshabilitar botones durante la actualización
  isUpdating = signal(false);
  
  // Estado actual seleccionado
  selectedStatus = computed(() => {
    return this.gameDetails?.myStatus?.status || null;
  });
  
  // Definición de los estados disponibles
  statusOptions: StatusOption[] = [
    {
      value: 'WISHLIST',
      labelKey: 'games.status.wishlist',
      icon: 'bookmark',
      activeClass: 'bg-blue-600 text-white border-blue-500',
      inactiveClass: 'bg-gray-700 text-gray-300 border-gray-600 hover:bg-gray-600'
    },
    {
      value: 'PLAYING',
      labelKey: 'games.status.playing',
      icon: 'play',
      activeClass: 'bg-green-600 text-white border-green-500',
      inactiveClass: 'bg-gray-700 text-gray-300 border-gray-600 hover:bg-gray-600'
    },
    {
      value: 'COMPLETED',
      labelKey: 'games.status.completed',
      icon: 'check',
      activeClass: 'bg-purple-600 text-white border-purple-500',
      inactiveClass: 'bg-gray-700 text-gray-300 border-gray-600 hover:bg-gray-600'
    },
    {
      value: 'ARCHIVED',
      labelKey: 'games.status.archived',
      icon: 'archive',
      activeClass: 'bg-gray-600 text-white border-gray-500',
      inactiveClass: 'bg-gray-700 text-gray-300 border-gray-600 hover:bg-gray-600'
    }
  ];
  
  /**
   * Manejar el cambio de estado
   */
  onStatusClick(status: GameStatus): void {
    // Evitar múltiples clicks mientras se procesa
    if (this.isUpdating()) return;
    
    const currentStatus = this.selectedStatus();
    let newStatus: GameStatus = null;
    
    // Si ya está seleccionado, lo deseleccionamos
    if (currentStatus === status) {
      newStatus = null;
    } else {
      // Si no, seleccionamos el nuevo estado
      newStatus = status;
    }
    
    // Llamar a la API para actualizar
    this.updateGameStatus(newStatus);
  }
  
  /**
   * Actualizar el estado del juego en el backend
   */
  private updateGameStatus(newStatus: GameStatus): void {
    console.log('Actualizando estado a:', newStatus);
    
    // Preparar los datos para actualizar
    const updateData: UpdateReviewRequest = {};
    
    if (newStatus) {
      updateData.status = newStatus;
    }
    
    // Mantener los valores existentes si los hay
    if (this.gameDetails.myStatus?.rating) {
      updateData.rating = this.gameDetails.myStatus.rating;
    }
    if (this.gameDetails.myStatus?.reviewText) {
      updateData.reviewText = this.gameDetails.myStatus.reviewText;
    }
    
    // Indicar que estamos actualizando
    this.isUpdating.set(true);
    
    // Llamar al servicio
    this.gameApiService.updateMyReview(this.gameDetails.slug, updateData)
      .subscribe({
        next: (response) => {
          console.log('Estado actualizado exitosamente:', response);
          
          // Emitir el evento con la respuesta del backend
          this.statusUpdated.emit(response);
          
          // Resetear el estado de carga
          this.isUpdating.set(false);
        },
        error: (error) => {
          console.error('Error al actualizar el estado:', error);
          
          // Resetear el estado de carga
          this.isUpdating.set(false);
          
          // Aquí podrías emitir un evento de error si lo necesitas
          // o mostrar una notificación de error
        }
      });
  }
  
  /**
   * Obtener las clases CSS para un botón de estado
   */
  getStatusButtonClass(status: GameStatus): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    if (!option) return '';
    
    const isSelected = this.selectedStatus() === status;
    return isSelected ? option.activeClass : option.inactiveClass;
  }
  
  /**
   * Obtener el path SVG para cada icono
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
}