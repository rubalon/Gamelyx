// src/app/app.config.ts
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { HttpClient } from '@angular/common/http';

import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt-interceptor';



/**
 * 🏭 Factory function para ngx-translate
 * Configura cómo cargar los archivos de traducción
 */
export function httpLoaderFactory(http: HttpClient): TranslateHttpLoader {
  return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

/**
 * 🎯 Configuración central de la aplicación Angular
 * 
 * Centraliza todos los providers necesarios para la app:
 * - Browser & Zone configuration
 * - Routing
 * - HTTP Client + JWT Interceptor
 * - Internationalization (ngx-translate)
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // 🛡️ Browser & Zone Configuration
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    
    // 🛣️ Routing
    provideRouter(routes),
    
    // 🌐 HTTP Client + JWT Interceptor
    provideHttpClient(
      withInterceptors([jwtInterceptor]) // Automáticamente añade JWT a todas las requests
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
    )
  ]
};