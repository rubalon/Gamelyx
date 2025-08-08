// src/app/features/games/components/game-card/game-card.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { GameSearchItem } from '@core/services/game-api';

@Component({
  selector: 'app-game-card',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './game-card.html',
  styleUrl: './game-card.scss'
})
export class GameCard {
  @Input({ required: true }) game!: GameSearchItem;

  /**
   * 🖼️ Optimizar URL de imagen RAWG para reducir tamaño
   * Convierte: https://media.rawg.io/media/games/image.jpg
   * En: https://media.rawg.io/media/resize/400/-/games/image.jpg
   */
  getOptimizedImageUrl(originalUrl: string): string {
    if (!originalUrl) {
      return this.getPlaceholderImage();
    }
    
    // Solo optimizar imágenes de RAWG API
    if (originalUrl.includes('media.rawg.io')) {
      // Insertar parámetro resize en la URL
      return originalUrl.replace('/media/', '/media/resize/640/-/');
    }
    
    // Para otras URLs, devolver original
    return originalUrl;
  }

  /**
   * 🎨 Generar placeholder SVG cuando no hay imagen
   */
  private getPlaceholderImage(): string {
    return `data:image/svg+xml;charset=UTF-8,%3Csvg width="400" height="225" xmlns="http://www.w3.org/2000/svg"%3E%3Crect width="100%25" height="100%25" fill="%23374151"/%3E%3Ctext x="50%25" y="50%25" font-size="16" fill="%239CA3AF" text-anchor="middle" dy=".3em"%3E🎮%3C/text%3E%3C/svg%3E`;
  }

  /**
   * 📅 Formatear fecha de lanzamiento
   */
  formatDate(dateString: string): string {
    try {
      const date = new Date(dateString);
      return date.getFullYear().toString();
    } catch {
      return dateString;
    }
  }

  /**
   * 🖼️ Manejar error de imagen con placeholder mejorado
   */
  onImageError(event: any): void {
    console.warn('Error loading image:', {
      src: event.target.src,
      game: this.game.name
    });
    
    // Usar placeholder personalizado con nombre del juego
    const placeholder = `data:image/svg+xml;charset=UTF-8,%3Csvg width="400" height="225" xmlns="http://www.w3.org/2000/svg"%3E%3Crect width="100%25" height="100%25" fill="%23374151"/%3E%3Ctext x="50%25" y="50%25" font-size="16" fill="%23D1D5DB" text-anchor="middle" dy=".3em"%3E${encodeURIComponent(this.game.name)}%3C/text%3E%3C/svg%3E`;
    
    event.target.src = placeholder;
  }
}
