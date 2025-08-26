// src/app/features/home/components/search-toggle/search-toggle.ts
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { GameSearchBar } from '@shared/components/game-search-bar/game-search-bar';
import { UserSearchBar } from '@shared/components/user-search-bar/user-search-bar';

type SearchType = 'games' | 'users';

@Component({
  selector: 'app-search-toggle',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    GameSearchBar,
    UserSearchBar
  ],
  templateUrl: './search-toggle.html',
  styleUrl: './search-toggle.scss'
})
export class SearchToggleComponent {
  // 🎯 Estado del componente
  searchType = signal<SearchType>('games');
}