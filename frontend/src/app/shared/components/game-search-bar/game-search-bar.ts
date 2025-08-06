// src/app/shared/components/game-search-bar/game-search-bar.ts
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { Subject, EMPTY } from 'rxjs';
import { GameApiService } from '@core/services/game-api';

@Component({
  selector: 'app-game-search-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './game-search-bar.html',
  styleUrl: './game-search-bar.scss'
})
export class GameSearchBar {
  private gameApiService = inject(GameApiService);
  private router = inject(Router);

  // 🎯 Estado del componente con Signals
  searchQuery = '';
  isSearching = signal(false);
  searchError = signal<string | null>(null);

  // 🔍 Subject para debounce de búsqueda futura (opcional)
  private searchSubject = new Subject<string>();

  ngOnInit() {
    // 🚀 Configurar debounce para búsqueda en tiempo real (opcional para futuro)
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query => {
          if (query.trim().length < 2) return EMPTY;
          
          this.isSearching.set(true);
          this.searchError.set(null);
          
          return this.gameApiService.searchGames(query, 1, 5)
            .pipe(
              catchError(error => {
                this.searchError.set('home.search.error');
                return EMPTY;
              })
            );
        })
      )
      .subscribe(results => {
        this.isSearching.set(false);
        // Aquí podrías mostrar sugerencias en tiempo real
        console.log('Sugerencias:', results.games);
      });
  }

  /**
   * 🔍 Realizar búsqueda y navegar a página de resultados
   */
  performSearch(): void {
    const query = this.searchQuery.trim();
    
    // Validaciones
    if (!query) return;
    if (query.length < 2) {
      this.searchError.set('Mínimo 2 caracteres para buscar');
      return;
    }

    // Limpiar errores previos
    this.searchError.set(null);
    
    // Navegar a página de resultados con el query como parámetro
    this.router.navigate(['/games/search'], { 
      queryParams: { q: query } 
    });
  }

  /**
   * 🧹 Limpiar errores cuando el usuario escribe
   */
  onInputChange(): void {
    if (this.searchError()) {
      this.searchError.set(null);
    }
  }
}