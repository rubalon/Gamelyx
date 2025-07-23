import { Routes } from '@angular/router';
import { canActivateAuth, canActivateGuest } from '@core/guards/auth-guard';

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
  {
    path: '**',
    redirectTo: '/landing'
  }
];