// src/app/features/games/pages/game-page/components/game-reviews-section/game-reviews-section.ts
import { Component, inject, signal, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameDetails, GameReview, GameUserStatus, UpdateReviewResponse } from '@core/services/game-api';
import { AuthStore } from '@core/stores/auth-store';
import { ReviewModal } from '../review-modal/review-modal';
import { StarRating } from '@shared/components/star-rating/star-rating';
import { AvatarComponent } from '@shared/components/avatar/avatar';  

@Component({
  selector: 'app-game-reviews-section',
  standalone: true,
  imports: [
    CommonModule, 
    ReviewModal, 
    StarRating,
    AvatarComponent  
  ],
  templateUrl: './game-reviews-section.html',
  styleUrl: './game-reviews-section.scss'
})
export class GameReviewsSection {
  // 📡 Signal-based inputs/outputs
  gameDetails = input.required<GameDetails>();
  reviewUpdated = output<UpdateReviewResponse>(); // 🆕 CAMBIO: Ahora pasa datos del backend

  // 🏪 Dependencies
  private authStore = inject(AuthStore);

  // 🎯 Estado interno del componente con Signals
  showReviewModal = signal(false);
  editingReview = signal<GameUserStatus | null>(null);
  gameIdentifier = signal('');

  // 📊 SOLO computed signals que realmente añaden valor
  currentUser = computed(() => 
    this.authStore.user()
  );

  myReview = computed<GameUserStatus | null>(() => 
    this.gameDetails().myStatus
  );

  otherReviews = computed<GameReview[]>(() => 
    this.gameDetails().recentReviews.slice(0, 3)
  );

  /**
   * ✏️ Abrir modal para editar mi review
   */
  onEditMyReview(): void {
    this.editingReview.set(this.gameDetails().myStatus);
    this.gameIdentifier.set(this.gameDetails().slug);
    this.showReviewModal.set(true);
  }

  /**
   * ➕ Abrir modal para añadir nueva review
   */
  onAddReview(): void {
    this.editingReview.set(null);
    this.gameIdentifier.set(this.gameDetails().slug);
    this.showReviewModal.set(true);
  }

  /**
   * ❌ Cerrar modal
   */
  onCloseModal(): void {
    this.showReviewModal.set(false);
    this.editingReview.set(null);
    this.gameIdentifier.set('');
  }

  /**
   * 💾 Manejar envío de review exitoso - RECIBE DATOS DEL BACKEND
   */
  onReviewSubmitted(backendResponse: UpdateReviewResponse): void {
    // 🆕 CAMBIO: Cerrar modal y pasar datos al padre
    this.onCloseModal();
    this.reviewUpdated.emit(backendResponse); // 👈 Pasar datos del backend
  }

  // 🛠️ MÉTODOS UTILITARIOS (mantenidos exactamente igual que tu código)

  /**
   * 📅 Formatear fecha de review de manera inteligente
   */
  formatReviewDate(dateString: string | undefined): string {
    if (!dateString) return 'Fecha no disponible';

    try {
      const date = new Date(dateString);
      const now = new Date();
      const diffInMs = now.getTime() - date.getTime();
      const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));
      
      if (diffInDays === 0) {
        // Mismo día - mostrar horas
        const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
        if (diffInHours === 0) {
          const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
          return diffInMinutes <= 1 ? 'Ahora mismo' : `Hace ${diffInMinutes} minutos`;
        }
        return diffInHours === 1 ? 'Hace 1 hora' : `Hace ${diffInHours} horas`;
      }
      
      if (diffInDays === 1) return 'Ayer';
      if (diffInDays < 7) return `Hace ${diffInDays} días`;
      if (diffInDays < 30) {
        const weeks = Math.floor(diffInDays / 7);
        return weeks === 1 ? 'Hace 1 semana' : `Hace ${weeks} semanas`;
      }
      if (diffInDays < 365) {
        const months = Math.floor(diffInDays / 30);
        return months === 1 ? 'Hace 1 mes' : `Hace ${months} meses`;
      }
      
      return date.toLocaleDateString('es-ES', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric' 
      });
    } catch (error) {
      console.warn('Error formatting date:', error);
      return dateString;
    }
  }

  /**
   * 🎨 Obtener clases CSS para el estado del juego
   */
  getStatusClasses(status: string | null | undefined): string {
    const baseClasses = 'text-xs px-2 py-1 rounded-full border font-medium';
    
    // ✅ SOLUCIÓN: Guard clause para null/undefined
    if (!status) {
      return `${baseClasses} text-gray-400 border-gray-400 bg-gray-400/10`;
    }
    
    switch (status.toLowerCase()) {
      case 'completed':
        return `${baseClasses} text-green-400 border-green-400 bg-green-400/10`;
      case 'playing':
        return `${baseClasses} text-blue-400 border-blue-400 bg-blue-400/10`;
      case 'wishlist':
        return `${baseClasses} text-purple-400 border-purple-400 bg-purple-400/10`;
      case 'archived':
        return `${baseClasses} text-yellow-600 border-yellow-700 bg-yellow-700/10`;
      default:
        return `${baseClasses} text-gray-400 border-gray-400 bg-gray-400/10`;
    }
  }

  /**
   * 📖 Obtener texto legible del estado del juego
   */
  getStatusText(status: string | null | undefined): string {
    // ✅ SOLUCIÓN: Manejar null/undefined antes de toUpperCase()
    if (!status) {
      return 'Sin estado';
    }

    const statusMap: Record<string, string> = {
      'COMPLETED': 'Completado',
      'PLAYING': 'Jugando',
      'WISHLIST': 'En lista de deseos',
      'ARCHIVED': 'Archivado'
    };
    
    return statusMap[status.toUpperCase()] || status;
  }

  /**
   * 🎨 Obtener color del estado del juego
   */
  getStatusColor(status: string | null | undefined): string {
    // ✅ SOLUCIÓN: También corregir este método por consistencia
    if (!status) {
      return 'text-gray-400';
    }

    switch (status.toUpperCase()) {
      case 'COMPLETED': return 'text-green-400';
      case 'PLAYING': return 'text-blue-400';
      case 'WISHLIST': return 'text-purple-400';
      case 'ARCHIVED': return 'text-yellow-600';
      default: return 'text-gray-400';
    }
  }

  // ❌ REMOVIDO - Lógica de avatar duplicada (sustituida por AvatarComponent)
  // getUserInitial(username: string): string { ... }
  // getAvatarClasses(isCurrentUser: boolean = false): string { ... }

  /**
   * 🔢 Formatear número de reviews
   */
  formatReviewCount(count: number): string {
    if (count === 0) return 'Sin reviews';
    if (count === 1) return '1 review';
    if (count < 1000) return `${count} reviews`;
    if (count < 1000000) return `${(count / 1000).toFixed(1)}k reviews`;
    return `${(count / 1000000).toFixed(1)}M reviews`;
  }

  /**
   * 🎨 Obtener color del rating basado en el valor
   */
  getRatingColor(rating: number): string {
    if (rating >= 8) return 'text-green-400';
    if (rating >= 6) return 'text-yellow-400';
    if (rating >= 4) return 'text-orange-400';
    return 'text-red-400';
  }

  /**
   * 🏷️ Obtener mensaje de estado de reviews
   */
  getEmptyStateMessage(): string {
    const hasMyReview = this.myReview !== null;
    const otherReviews = this.otherReviews();
    
    if (!hasMyReview && otherReviews.length === 0) {
      return 'Sé el primero en compartir tu experiencia con este juego';
    }
    
    if (hasMyReview && otherReviews.length === 0) {
      return 'Eres el único que ha revieweado este juego hasta ahora';
    }
    
    return '';
  }
}