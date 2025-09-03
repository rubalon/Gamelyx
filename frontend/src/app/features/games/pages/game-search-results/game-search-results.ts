// src/app/features/games/pages/game-search-results/game-search-results.ts
import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, takeUntil, switchMap, finalize, catchError, of } from 'rxjs';

import { Header } from '../../../../shared/components/header/header';
import { FooterComponent } from '@shared/components/footer/footer';
import { GameSearchBar } from '../../../../shared/components/game-search-bar/game-search-bar';
import { GameCard } from '../../components/game-card/game-card';
import { GameApiService, GameSearchResponse, GameSearchItem } from '@core/services/game-api';

@Component({
  selector: 'app-game-search-results',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    Header,
    GameSearchBar,
    GameCard,
    FooterComponent
  ],
  templateUrl: './game-search-results.html',
  styleUrl: './game-search-results.scss'
})
export class GameSearchResults implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private gameApiService = inject(GameApiService);
  private destroy$ = new Subject<void>();

  // 🎯 Estado del componente
  searchResults = signal<GameSearchResponse | null>(null);
  isLoading = signal(false);
  error = signal<string | null>(null);
  currentPage = signal(1);
  currentQuery = signal('');

  ngOnInit(): void {
    // 🔍 Escuchar cambios en los query parameters
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const query = params['q'];
        const page = parseInt(params['page']) || 1;
        
        if (query && query !== this.currentQuery()) {
          this.currentQuery.set(query);
          this.currentPage.set(page);
          this.performSearch(query, page);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * 🔍 Realizar búsqueda de juegos
   */
  private performSearch(query: string, page: number = 1): void {
    if (!query.trim()) return;

    this.isLoading.set(true);
    this.error.set(null);

    this.gameApiService.searchGames(query, page, 20)
      .pipe(
        catchError(error => {
          console.error('Error searching games:', error);
          this.error.set('No se pudieron cargar los juegos. Por favor, intenta de nuevo.');
          return of(null);
        }),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe(results => {
        if (results) {
          this.searchResults.set(results);
        }
      });
  }

  /**
   * 🔄 Reintentar búsqueda
   */
  retrySearch(): void {
    this.performSearch(this.currentQuery(), this.currentPage());
  }

  /**
   * 📄 Ir a página específica
   */
  goToPage(page: number): void {
    if (page < 1) return;
    
    const results = this.searchResults();
    if (!results || (page > results.totalPages)) return;

    this.currentPage.set(page);
    this.performSearch(this.currentQuery(), page);
    
    // Scroll to top
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /**
   * 📄 Manejar click en página (helper para template)
   */
  onPageClick(page: number | string): void {
    if (typeof page === 'number') {
      this.goToPage(page);
    }
  }

  /**
   * 📊 Generar números de páginas para paginación con puntos suspensivos
   */
  getPageNumbers(): (number | string)[] {
    const results = this.searchResults();
    if (!results) return [];

    const totalPages = results.totalPages;
    const current = this.currentPage();
    const pages: (number | string)[] = [];

    // Si hay 5 páginas o menos, mostrar todas
    if (totalPages <= 5) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
      return pages;
    }

    // Siempre mostrar la primera página
    pages.push(1);

    // Si la página actual está cerca del principio (páginas 1-3)
    if (current <= 3) {
      for (let i = 2; i <= Math.min(4, totalPages - 1); i++) {
        pages.push(i);
      }
      if (totalPages > 4) {
        pages.push('...');
      }
    }
    // Si la página actual está cerca del final
    else if (current >= totalPages - 2) {
      if (totalPages > 4) {
        pages.push('...');
      }
      for (let i = Math.max(totalPages - 3, 2); i <= totalPages - 1; i++) {
        pages.push(i);
      }
    }
    // Si la página actual está en el medio
    else {
      pages.push('...');
      pages.push(current);
      pages.push('...');
    }

    // Siempre mostrar la última página
    if (totalPages > 1) {
      pages.push(totalPages);
    }

    return pages;
  }

  /**
   * 🏠 Navegar al home
   */
  goToHome(): void {
    this.router.navigate(['/']);
  }

}