// src/app/core/services/google-auth.ts
import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

// 🔧 Declaración de tipos para Google Identity Services
declare global {
  interface Window {
    google: {
      accounts: {
        id: {
          initialize: (config: any) => void;
          prompt: () => void;
          renderButton: (element: HTMLElement, config: any) => void;
          disableAutoSelect: () => void;
        };
      };
    };
  }
}

export interface GoogleAuthResponse {
  credential: string; // JWT token de Google
  select_by: string;
}

export interface GoogleUserInfo {
  sub: string;      // Google User ID
  email: string;    // Email del usuario
  name: string;     // Nombre completo
  picture: string;  // URL de la foto de perfil
  given_name: string; // Nombre
  family_name: string; // Apellido
}

@Injectable({
  providedIn: 'root'
})
export class GoogleAuthService {
  
  private isInitialized = false;

  constructor() {
    this.loadGoogleScript();
  }

  /**
   * Carga e inicializa Google Identity Services
   */
  private loadGoogleScript(): void {
    // 🔍 Verificar si Google ya está cargado
    if (typeof window !== 'undefined' && window.google?.accounts?.id) {
      this.initializeGoogleAuth();
      return;
    }

    // 🕒 Esperar a que se cargue el script
    const checkGoogleLoaded = () => {
      if (window.google?.accounts?.id) {
        this.initializeGoogleAuth();
      } else {
        setTimeout(checkGoogleLoaded, 100);
      }
    };

    checkGoogleLoaded();
  }

  /**
   * Inicializa la configuración de Google Auth
   */
  private initializeGoogleAuth(): void {
    if (this.isInitialized) return;

    try {
      window.google.accounts.id.initialize({
        client_id: environment.google.clientId,
        callback: this.handleCredentialResponse.bind(this),
        auto_select: false,
        cancel_on_tap_outside: true
      });

      this.isInitialized = true;
      console.log('✅ Google Auth inicializado correctamente');
      
    } catch (error) {
      console.error('❌ Error inicializando Google Auth:', error);
    }
  }

  /**
   * Callback que maneja la respuesta de Google
   */
  private handleCredentialResponse(response: GoogleAuthResponse): void {
    console.log('🔍 Respuesta de Google recibida:', response);
    
    // 🔧 Decodificar el JWT token para obtener info del usuario
    const userInfo = this.decodeJWT(response.credential);
    console.log('👤 Info del usuario:', userInfo);

    // 🚧 Por ahora solo logging, en la siguiente iteración integraremos con AuthStore
    // TODO: Enviar al AuthStore para procesar login/registro
  }

  /**
   * Decodifica el JWT token de Google para obtener la información del usuario
   */
  private decodeJWT(token: string): GoogleUserInfo {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));

      return JSON.parse(jsonPayload) as GoogleUserInfo;
    } catch (error) {
      console.error('❌ Error decodificando JWT:', error);
      throw new Error('Token inválido');
    }
  }

  /**
   * Inicia el flujo de autenticación con Google
   * @param mode - 'login' o 'register'
   */
  signInWithGoogle(mode: 'login' | 'register' = 'login'): Promise<GoogleUserInfo> {
    return new Promise((resolve, reject) => {
      if (!this.isInitialized) {
        reject(new Error('Google Auth no está inicializado'));
        return;
      }

      console.log(`🚀 Iniciando Google Sign-In - Modo: ${mode}`);

      // 🔧 Configurar callback temporal para esta operación específica
      const originalCallback = this.handleCredentialResponse.bind(this);
      
      window.google.accounts.id.initialize({
        client_id: environment.google.clientId,
        callback: (response: GoogleAuthResponse) => {
          try {
            const userInfo = this.decodeJWT(response.credential);
            console.log(`✅ ${mode} con Google exitoso:`, userInfo);
            resolve(userInfo);
          } catch (error) {
            console.error(`❌ Error en ${mode} con Google:`, error);
            reject(error);
          }
        },
        auto_select: false,
        cancel_on_tap_outside: true
      });

      // 🎯 Mostrar el popup de Google
      window.google.accounts.id.prompt();
    });
  }

  /**
   * Desactiva la selección automática de Google
   */
  disableAutoSelect(): void {
    if (this.isInitialized) {
      window.google.accounts.id.disableAutoSelect();
    }
  }
}