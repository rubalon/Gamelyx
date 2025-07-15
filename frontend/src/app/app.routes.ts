import { Routes } from '@angular/router';

export const routes: Routes = [
  // Ruta por defecto - redirige a landing
  {
    path: '',
    redirectTo: '/landing',
    pathMatch: 'full'
  },
  
  // Ruta de landing page
  {
    path: 'landing',
    loadComponent: () => import('./features/landing/pages/landing-page/landing-page').then(m => m.LandingPage)
  },
  
  // Ruta de home page  
  {
    path: 'home',
    loadComponent: () => import('./features/home/pages/home-page/home-page').then(m => m.HomePage)
  },
  
  // Ruta wildcard - página no encontrada (redirige a landing)
  {
    path: '**',
    redirectTo: '/landing'
  }
];