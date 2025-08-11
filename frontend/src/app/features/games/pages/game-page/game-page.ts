// src/app/features/games/pages/game-page/game-page.ts
import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, takeUntil, switchMap, finalize, catchError, of } from 'rxjs';

import { Header } from '../../../../shared/components/header/header';
import { GameApiService, GameDetails as GameDetailsInterface } from '@core/services/game-api';
import { AuthStore } from '../../../../core/stores/auth-store';
import { GameReviewsSection } from './components/game-reviews-section/game-reviews-section';

@Component({
  selector: 'app-game-page',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    Header,
    GameReviewsSection
  ],
  templateUrl: './game-page.html',
  styleUrl: './game-page.scss'
}) 
export class GamePage implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private gameApiService = inject(GameApiService);
  private authStore = inject(AuthStore);
  private destroy$ = new Subject<void>();

  // 🎯 Estado del componente
  gameDetails = signal<GameDetailsInterface | null>(null);
  isLoading = signal(true);
  error = signal<string | null>(null);
  currentIdentifier = signal('');
  
  // 🎨 UI State para controlar secciones
  activeScreenshot = signal(0);
  showFullDescription = signal(false);

  ngOnInit(): void {
    // 📡 Escuchar cambios en el parámetro de ruta
    this.route.params
      .pipe(
        takeUntil(this.destroy$),
        switchMap(params => {
          const identifier = params['identifier'];
          this.currentIdentifier.set(identifier);
          
          if (!identifier) {
            this.router.navigate(['/games/search']);
            return of(null);
          }

          // 🔄 Ahora sí devuelve un Observable
          return this.getGameDetailsObservable(identifier);
        })
      )
      .subscribe(gameDetails => {
        if (gameDetails) {
          this.gameDetails.set(gameDetails);
          // Resetear UI state
          this.activeScreenshot.set(0);
          this.showFullDescription.set(false);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * 🎮 Obtener detalles del juego como Observable (para switchMap)
   */
  private getGameDetailsObservable(identifier: string) {
    this.isLoading.set(true);
    this.error.set(null);

    return this.gameApiService.getGameDetails(identifier)
      .pipe(
        catchError(error => {
          console.error('Error loading game details:', error);
          
          if (error.status === 404) {
            this.error.set('El juego no fue encontrado');
          } else if (error.status === 401) {
            this.error.set('Necesitas iniciar sesión para ver este juego');
          } else {
            this.error.set('Error al cargar los detalles del juego. Por favor, intenta de nuevo.');
          }
          
          return of(null);
        }),
        finalize(() => this.isLoading.set(false))
      );
  }

  /**
   * 🎮 Cargar detalles del juego (método directo para retry)
   */
  private loadGameDetails(identifier: string) {
    this.getGameDetailsObservable(identifier)
      .subscribe(gameDetails => {
        if (gameDetails) {
          this.gameDetails.set(gameDetails);
          this.activeScreenshot.set(0);
          this.showFullDescription.set(false);
        }
      });
  }

  /**
   * 🔄 Reintentar carga
   */
  retryLoad(): void {
    const identifier = this.currentIdentifier();
    if (identifier) {
      this.loadGameDetails(identifier);
    }
  }

  /**
   * 🖼️ Cambiar screenshot activo
   */
  setActiveScreenshot(index: number): void {
    const game = this.gameDetails();
    if (game && index >= 0 && index < game.screenshots.length) {
      this.activeScreenshot.set(index);
    }
  }

  /**
   * 📝 Toggle descripción completa
   */
  toggleDescription(): void {
    this.showFullDescription.set(!this.showFullDescription());
  }

  /**
   * 📅 Formatear fecha de lanzamiento
   */
  formatReleaseDate(dateString: string): string {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('es-ES', { 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
      });
    } catch {
      return dateString;
    }
  }

  /**
   * ⭐ Generar array para mostrar estrellas de rating con fracciones
   */
  getRatingStars(rating: number): { filled: number }[] {
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

  /**
   * 🎨 Obtener color del score metacritic
   */
  getMetacriticColor(score: number): string {
    if (score >= 75) return 'text-green-500';
    if (score >= 50) return 'text-yellow-500';
    return 'text-red-500';
  }

  /**
   * 🏠 Navegación de vuelta a búsqueda
   */
  //todo: Almacenar la ultima búsqueda para volver a ella
  goBack(): void {
    this.router.navigate(['/games/search'], {
    });
  }

  /**
   * 🔗 Abrir sitio web oficial
   */
  openWebsite(url: string): void {
    if (url) {
      window.open(url, '_blank', 'noopener noreferrer');
    }
  }

  /**
   * 🖼️ Optimizar URL de imagen RAWG para reducir tamaño
   * Convierte: https://media.rawg.io/media/games/image.jpg
   * En: https://media.rawg.io/media/resize/640/-/games/image.jpg
   */
  getOptimizedImageUrl(originalUrl: string): string {

    
    // Solo optimizar imágenes de RAWG API
    if (originalUrl.includes('media.rawg.io')) {
      // Insertar parámetro resize en la URL
      return originalUrl.replace('/media/', '/media/resize/640/-/');
    }
    
    // Para otras URLs, devolver original
    return originalUrl;
  }

  /**
   * 🔄 Manejar actualización de review (recargar datos del juego)
   */
  onReviewUpdated(): void {
    console.log('Review actualizada, recargando datos del juego');
    // Recargar gameDetails para reflejar la nueva review
    const identifier = this.currentIdentifier();
    if (identifier) {
      this.getGameDetailsObservable(identifier).subscribe(gameDetails => {
        if (gameDetails) {
          this.gameDetails.set(gameDetails);
        }
      });
    }
  }

  // 🎯 Getters para template
  get isAuthenticated() {
    return this.authStore.isAuthenticated();
  }

  get currentUser() {
    return this.authStore.user();
  }
}