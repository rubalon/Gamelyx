// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { canActivateAuth, canActivateGuest } from './core/guards/auth-guard';

export const routes: Routes = [
  { 
    path: '', 
    redirectTo: '/landing', 
    pathMatch: 'full' 
  },
  {
    path: 'landing',
    loadComponent: () => import('./features/landing/pages/landing-page/landing-page')
      .then(m => m.LandingPage),
    canActivate: [canActivateGuest] // 👈 Solo si NO está logueado
  },
  {
    path: 'home',
    loadComponent: () => import('./features/home/pages/home-page/home-page')
      .then(m => m.HomePage),
    canActivate: [canActivateAuth] // 👈 Solo si está logueado
  },
  
  // 🎮 Games Feature Routes - Solo para usuarios autenticados
  {
    path: 'games',
    canActivate: [canActivateAuth], // Proteger toda la sección de games
    children: [
      // 🔍 Search Results Page
      {
        path: 'search',
        loadComponent: () => import('./features/games/pages/game-search-results/game-search-results')
          .then(m => m.GameSearchResults)
      },
      
      // 🎮 Game Details Page (para el futuro)

      {
        path: 'game/:identifier',
        loadComponent: () => import('./features/games/pages/game-page/game-page')
          .then(m => m.GamePage)
      }

    ]
  },
  
  {
    path: '**',
    redirectTo: '/landing'
  }
];