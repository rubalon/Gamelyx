// src/app/core/interceptors/jwt.interceptor.ts
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStore } from '../stores/auth-store';

/**
 * Automáticamente añade el token JWT a todas las peticiones HTTP
 * que vayan dirigidas a nuestro backend API
 */
export const jwtInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>, 
  next: HttpHandlerFn
) => {
  // Inyectar el AuthStore para acceder al token
  const authStore = inject(AuthStore);
  
  // Obtener el token de acceso
  const accessToken = authStore.getAccessToken();
  
  // Solo añadir JWT si:
  // 1. Tenemos token
  // 2. La request va dirigida a nuestro backend API
  if (accessToken && isApiRequest(req.url)) {
    // Clonar la request original y añadir el header Authorization
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`
      }
    });
    
    // Continuar con la request modificada
    return next(authReq);
  }
  
  // Si no hay token o no es una API request, continuar sin modificar
  return next(req);
};

/**
 * 🔍 Determina si una URL necesita JWT
 * 
 * Incluye: APIs privadas (/api/games)
 * Excluye: Auth endpoints (/api/auth) y requests externas
 */
function isApiRequest(url: string): boolean {
  // Solo añadir JWT a endpoints que lo requieren
  const isApi = url.includes('/api/') 
  
  // Excluir explícitamente endpoints de auth
  const isAuthEndpoint = url.includes('/api/auth');
  
  return isApi && !isAuthEndpoint;
}