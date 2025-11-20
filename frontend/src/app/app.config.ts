// src/app/app.config.ts
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection, provideAppInitializer, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { HttpClient } from '@angular/common/http';

import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt-interceptor';
import { errorInterceptor } from './core/interceptors/error-interceptor';
import { GoogleAuthService } from './core/services/google-auth'; // 🆕 NUEVO

/**
 * 🏭 Factory function para ngx-translate
 * Configura cómo cargar los archivos de traducción
 */
export function httpLoaderFactory(http: HttpClient): TranslateHttpLoader {
  return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

/**
 * 🏭 Factory function para inicializar Google Auth (Angular 20)
 * Garantiza que Google Identity Services esté listo antes de usar la app
 */
export function initializeGoogleAuth(googleAuthService: GoogleAuthService): () => Promise<void> {
  return () => {
    console.log('🚀 Inicializando GoogleAuthService desde app.config (Angular 20)...');
    // El solo hecho de inyectar el servicio lo inicializa automáticamente
    // Retornamos Promise resuelto para que el inicializador continúe
    return Promise.resolve();
  };
}

/**
 * 🎯 Configuración central de la aplicación Angular
 * 
 * Centraliza todos los providers necesarios para la app:
 * - Browser & Zone configuration
 * - Routing
 * - HTTP Client + JWT Interceptor
 * - Internationalization (ngx-translate)
 * - Google Authentication initialization
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // 🛡️ Browser & Zone Configuration
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    
    // 🛣️ Routing
    provideRouter(routes),
    
    // 🌐 HTTP Client + Interceptors
    provideHttpClient(
      withInterceptors([
        jwtInterceptor,    // Añade JWT a todas las requests
        errorInterceptor   // Maneja errores de autenticación (auto-logout en JWT expirado)
      ])
    ),
    
    // 🗣️ Internationalization (ngx-translate)
    importProvidersFrom(
      TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useFactory: httpLoaderFactory,
          deps: [HttpClient]
        }
      })
    ),
    
    // 🔐 Google Authentication Initialization (Angular 20)
    provideAppInitializer(() => {
      console.log('🚀 Inicializando GoogleAuthService desde app.config (Angular 20)...');
      const googleAuthService = inject(GoogleAuthService);
      return Promise.resolve();
    })
  ]
  
};