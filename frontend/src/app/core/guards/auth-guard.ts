import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { CanActivateFn } from '@angular/router';
import { AuthStore } from '@core/stores/auth-store';

export const canActivateAuth: CanActivateFn = (route, state) => {
  const authStore = inject(AuthStore);
  const router = inject(Router);

  // Verificar si el usuario está autenticado
  if (authStore.isAuthenticated()) {
    return true;
  }

  // Si no está autenticado, redirigir a landing
  console.log('Usuario no autenticado, redirigiendo a landing');
  router.navigate(['/landing']);
  return false;
};

// 👈 BONUS: Guard para prevenir acceso a landing si ya está logueado
export const canActivateGuest: CanActivateFn = (route, state) => {
  const authStore = inject(AuthStore);
  const router = inject(Router);

  // Si está autenticado, no puede ver landing
  if (authStore.isAuthenticated()) {
    console.log('Usuario ya autenticado, redirigiendo a home');
    router.navigate(['/home']);
    return false;
  }

  return true;
};