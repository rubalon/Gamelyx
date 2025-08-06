// src/app/features/home/pages/home-page/home-page.ts
import { Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { Header } from '../../../../shared/components/header/header';
import { GameSearchBar } from '../../../../shared/components/game-search-bar/game-search-bar';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    Header, 
    TranslateModule,
    GameSearchBar // 🆕 Nuevo componente de búsqueda
  ],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss'
})
export class HomePage {

  //  la lógica de búsqueda está en GameSearchBar
}