import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * ⭐ Star Rating Component
 * 
 * Componente flexible para mostrar rating con estrellas.
 * Soporta estrellas fraccionadas, diferentes estilos de wrapper y formatos de texto.
 */
@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-center" [ngClass]="wrapperClasses">
      <div class="flex mr-1 sm:mr-2">
        @for (star of stars; track $index) {
          <!-- Siempre modo fraccionado - más preciso -->
          <div class="relative h-3 w-3 sm:h-4 sm:w-4">
            <!-- Estrella de fondo -->
            <svg 
              class="absolute inset-0 h-3 w-3 sm:h-4 sm:w-4 text-gray-600"
              fill="currentColor" 
              viewBox="0 0 20 20">
              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.518 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"/>
            </svg>
            <!-- Estrella llena con clip-path para fracciones -->
            <svg 
              class="absolute inset-0 h-3 w-3 sm:h-4 sm:w-4 text-yellow-400"
              [style.clip-path]="'inset(0 ' + (100 - star.filled * 100) + '% 0 0)'"
              fill="currentColor" 
              viewBox="0 0 20 20">
              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.518 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"/>
            </svg>
          </div>
        }
      </div>
      
      <!-- Rating numérico -->
      @if (showNumeric) {
        <span [ngClass]="textClasses">
          @if (useDecimalFormat) {
            {{ rating | number:'1.1-1' }}/10
          } @else {
            {{ rating }}/10
          }
        </span>
      }
    </div>
  `,
  styles: []
})
export class StarRating {
  
  
  /**
   * 🔢 Rating a mostrar (0-10)
   */
  @Input({ required: true }) rating!: number;
  
  /**
   * 📊 Mostrar el valor numérico junto a las estrellas
   */
  @Input() showNumeric: boolean = true;
  
  /**
   * 📏 Usar formato decimal (1.1-1) en lugar de entero
   */
  @Input() useDecimalFormat: boolean = false;
  
  /**
   * 🎨 Clases CSS adicionales para el wrapper
   */
  @Input() wrapperClasses: string = 'mt-1';
  
  /**
   * 🎨 Clases CSS para el texto del rating
   */
  @Input() textClasses: string = 'text-yellow-400 text-xs sm:text-sm font-medium';
  
  /**
   * ⭐ Array de estrellas calculado
   * 
   * MOVIDO desde UiUtils - Ahora es específico de este componente
   */
  get stars(): { filled: number }[] {
    return this.getRatingStars(this.rating);
  }

  /**
   * ⭐ Generar array para mostrar estrellas de rating con fracciones
   * 
   * Convierte un rating de 0-10 a un array de objetos que representan 5 estrellas
   * con valores de llenado de 0 a 1 para manejar estrellas parciales.
   * 
   * @param rating - Rating de 0 a 10
   * @returns Array de objetos con propiedad 'filled' (0-1)
   * 
   * @example
   * getRatingStars(8.7) → [
   *   { filled: 1 },    // Estrella completa
   *   { filled: 1 },    // Estrella completa  
   *   { filled: 1 },    // Estrella completa
   *   { filled: 1 },    // Estrella completa
   *   { filled: 0.35 }  // Estrella 35% llena
   * ]
   */
  private getRatingStars(rating: number): { filled: number }[] {
    const stars = [];
    const maxStars = 5;
    const scaledRating = rating / 2; // Rating de 10 a 5 estrellas
    
    for (let i = 0; i < maxStars; i++) {
      const starValue = scaledRating - i;
      
      if (starValue >= 1) {
        // Estrella completamente llena
        stars.push({ filled: 1 });
      } else if (starValue > 0) {
        // Estrella parcialmente llena
        stars.push({ filled: starValue });
      } else {
        // Estrella vacía
        stars.push({ filled: 0 });
      }
    }
    
    return stars;
  }
}