// src/app/features/games/pages/game-page/game-page.ts
import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, takeUntil, switchMap, finalize, catchError, of } from 'rxjs';

import { Header } from '@shared/components/header/header';
import { GameApiService, GameDetails as GameDetailsInterface, UpdateReviewResponse } from '@core/services/game-api';
import { AuthStore } from '@core/stores/auth-store';
import { GameReviewsSection } from './components/game-reviews-section/game-reviews-section';
import { GameStatusSelector } from './components/game-status-selector/game-status-selector';
import { StarRating } from '@shared/components/star-rating/star-rating';
@Component({
  selector: 'app-game-page',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    Header,
    GameReviewsSection,
    GameStatusSelector,
    StarRating
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

  private isMobile(): boolean {
  return window.innerWidth < 640; // Tailwind SM breakpoint 
  }

  /**
   * 📏 Obtener límite de caracteres según dispositivo
   */
  private getDescriptionLimit(): number {
    return this.isMobile() ? 250 : 1500;
  }

  /**
   * ✂️ Obtener descripción truncada del texto RAW
   */
  getDisplayDescriptionText(): string {
    const game = this.gameDetails();
    if (!game?.descriptionRaw) return '';
    
    const limit = this.getDescriptionLimit();
    const rawText = game.descriptionRaw;
    
    // Si está expandido o el texto es corto, mostrar completo
    if (this.showFullDescription() || rawText.length <= limit) {
      return rawText;
    }
    
    // Truncar en la última palabra completa antes del límite
    const truncated = rawText.substring(0, limit);
    const lastSpaceIndex = truncated.lastIndexOf(' ');
    const cleanCut = lastSpaceIndex > 0 ? truncated.substring(0, lastSpaceIndex) : truncated;
    
    return cleanCut + '...';
  }
  /**
   * 🔍 Verificar si necesita botón "Leer más"
   */
  shouldShowToggleButton(): boolean {
    const game = this.gameDetails();
    if (!game?.descriptionRaw) return false;
    
    const limit = this.getDescriptionLimit();
    return game.descriptionRaw.length > limit;
  }

  /**
   * 📝 Toggle descripción completa (método actualizado)
   */
  toggleDescription(): void {
    this.showFullDescription.set(!this.showFullDescription());
  }

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
   * 🔄 Manejar actualización de review - ACTUALIZACIÓN DIRECTA CON DATOS DEL BACKEND
   */
  onReviewUpdated(reviewData: UpdateReviewResponse): void {
    
    const currentGame = this.gameDetails();
    if (!currentGame) return;

    // 🚀 ACTUALIZAR DIRECTAMENTE SIN LLAMADA AL BACKEND
    const updatedGame: GameDetailsInterface = {
      ...currentGame,
      myStatus: {
        status: reviewData.status as "WISHLIST" | "PLAYING" | "COMPLETED" | "ARCHIVED",
        rating: reviewData.rating,
        reviewText: reviewData.reviewText,
        reviewUpdatedAt: reviewData.updatedAt
      },
      communityRating: reviewData.communityRating,
      totalCommunityReviews: reviewData.totalReviews
    };

    // 💫 Actualización instantánea del estado
    this.gameDetails.set(updatedGame);

  }
  

  // 🎯 Getters para template
  get isAuthenticated() {
    return this.authStore.isAuthenticated();
  }

  get currentUser() {
    return this.authStore.user();
  }
}