// src/app/features/games/pages/game-search-results/game-search-results.ts
import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, takeUntil, switchMap, finalize, catchError, of } from 'rxjs';

import { Header } from '../../../../shared/components/header/header';
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
    GameCard
  ],
  templateUrl: './game-search-results.html',
  styleUrl: './game-search-results.scss'
})
export class GameSearchResults implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
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
   * 📊 Generar números de páginas para paginación
   */
  getPageNumbers(): number[] {
    const results = this.searchResults();
    if (!results) return [];

    const totalPages = results.totalPages;
    const current = this.currentPage();
    const pages: number[] = [];

    // Mostrar máximo 7 páginas
    const maxPages = 7;
    let startPage = Math.max(1, current - Math.floor(maxPages / 2));
    let endPage = Math.min(totalPages, startPage + maxPages - 1);

    // Ajustar si estamos cerca del final
    if (endPage - startPage + 1 < maxPages) {
      startPage = Math.max(1, endPage - maxPages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  }
}