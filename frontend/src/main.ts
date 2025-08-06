// src/main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { appConfig } from './app/app.config';

/**
 * 🚀 Bootstrap de la aplicación Angular
 * 
 * Responsabilidad única: inicializar la aplicación
 * Toda la configuración está centralizada en app.config.ts
 */
bootstrapApplication(App, appConfig)
  .catch(err => console.error('Error al inicializar la aplicación:', err));