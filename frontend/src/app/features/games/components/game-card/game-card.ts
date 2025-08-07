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
   * 🖼️ Manejar error de imagen
   */
  onImageError(event: any): void {
    // Fallback image placeholder
    event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjMwMCIgdmlld0JveD0iMCAwIDIwMCAzMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIyMDAiIGhlaWdodD0iMzAwIiBmaWxsPSIjMzc0MTUxIi8+CjxwYXRoIGQ9Ik05MiAxMDBMMTA4IDEwMEwxMDAgMTEyTDkyIDEwMFoiIGZpbGw9IiM2QjcyODAiLz4KPHJlY3QgeD0iNjAiIHk9IjEyMCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjQwIiBmaWxsPSIjNkI3MjgwIi8+Cjx0ZXh0IHg9IjEwMCIgeT0iMjAwIiBmaWxsPSIjOUI5QkEyIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmb250LWZhbWlseT0iYXJpYWwiIGZvbnQtc2l6ZT0iMTIiPkdhbWUgSW1hZ2U8L3RleHQ+Cjwvc3ZnPg==';
  }
}