import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthStore } from '../stores/auth-store';

/**
 * Interceptor de errores HTTP
 * Maneja automáticamente la expiración del JWT cerrando la sesión del usuario
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authStore = inject(AuthStore);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Si el error es 401 (Unauthorized) y el usuario está autenticado,
      // significa que el JWT ha expirado o es inválido
      if (error.status === 401 && authStore.isAuthenticated()) {
        console.warn('🔒 JWT expirado o inválido. Cerrando sesión automáticamente...');

        // Ejecutar logout automáticamente con mensaje de error
        authStore.logout('Tu sesión ha expirado. Por favor, inicia sesión nuevamente.');

        // El AuthStore ya redirige a '/' y establece el error en el estado
      }

      // Propagar el error para que los componentes puedan manejarlo si es necesario
      return throwError(() => error);
    })
  );
};
