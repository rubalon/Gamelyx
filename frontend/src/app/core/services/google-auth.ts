// src/app/core/services/google-auth.ts
import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

// Declaración de tipos para Google Identity Services completa
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
        oauth2: {
          initTokenClient: (config: {
            client_id: string;
            scope: string;
            prompt?: string;
            callback: (response: any) => void;
            error_callback?: (error: any) => void;
          }) => {
            requestAccessToken: () => void;
          };
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
    console.log('Iniciando carga de Google Script...');
    console.log('Window disponible:', typeof window !== 'undefined');
    console.log('Google disponible:', typeof window.google);
    
    // Verificar si Google ya está cargado
    if (typeof window !== 'undefined' && window.google?.accounts?.id) {
      console.log('Google ya estaba cargado');
      this.initializeGoogleAuth();
      return;
    }

    console.log('Esperando a que se cargue Google Script...');
    let attempts = 0;
    
    // Esperar a que se cargue el script
    const checkGoogleLoaded = () => {
      attempts++;
      console.log(`Intento ${attempts} - Google disponible:`, typeof window.google);
      
      if (window.google?.accounts?.id) {
        console.log('Google Script cargado correctamente');
        this.initializeGoogleAuth();
      } else if (attempts > 50) { // 5 segundos máximo
        console.error('Timeout esperando Google Script');
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
    console.log('Intentando inicializar Google Auth...');
    console.log('Ya inicializado:', this.isInitialized);
    console.log('Client ID:', environment.google.clientId);
    
    if (this.isInitialized) {
      console.log('Google Auth ya estaba inicializado');
      return;
    }

    try {
      // Solo inicializar la API básica, OAuth2 se manejará separadamente
      window.google.accounts.id.initialize({
        client_id: environment.google.clientId,
        callback: this.handleCredentialResponse.bind(this),
        auto_select: false,
        cancel_on_tap_outside: false,
        itp_support: true
      });

      this.isInitialized = true;
      console.log('Google Auth inicializado correctamente');
      
    } catch (error) {
      console.error('Error inicializando Google Auth:', error);
    }
  }

  /**
   * Callback que maneja la respuesta de Google (no usado en OAuth2)
   */
  private handleCredentialResponse(response: GoogleAuthResponse): void {
    console.log('Respuesta de Google recibida:', response);
    
    const userInfo = this.decodeJWT(response.credential);
    console.log('Info del usuario:', userInfo);
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
      console.error('Error decodificando JWT:', error);
      throw new Error('Token inválido');
    }
  }

  /**
   * Inicia el flujo de autenticación con Google usando Authorization API
   * @param mode - 'login' o 'register'
   */
  signInWithGoogle(mode: 'login' | 'register' = 'login'): Promise<GoogleUserInfo> {
    return new Promise((resolve, reject) => {
      if (!this.isInitialized) {
        reject(new Error('Google Auth no está inicializado'));
        return;
      }

      console.log(`Iniciando Google Sign-In - Modo: ${mode}`);

      try {
        // Usar Authorization API oficial para forzar selección de cuenta
        const client = window.google.accounts.oauth2.initTokenClient({
          client_id: environment.google.clientId,
          scope: 'openid email profile', // Scopes básicos para autenticación
          prompt: 'select_account', // Fuerza selección de cuenta siempre
          callback: async (tokenResponse: any) => {
            try {
              if (tokenResponse.error) {
                reject(new Error(`Google Auth error: ${tokenResponse.error}`));
                return;
              }

              // Obtener información del usuario usando el access token
              const userInfoResponse = await fetch('https://www.googleapis.com/oauth2/v2/userinfo', {
                headers: {
                  'Authorization': `Bearer ${tokenResponse.access_token}`
                }
              });

              if (!userInfoResponse.ok) {
                reject(new Error('Error obteniendo información del usuario'));
                return;
              }

              const userInfo = await userInfoResponse.json();
              
              resolve({
                sub: userInfo.id,
                email: userInfo.email,
                name: userInfo.name,
                picture: userInfo.picture,
                given_name: userInfo.given_name,
                family_name: userInfo.family_name
              });

            } catch (error) {
              console.error(`Error procesando respuesta de Google:`, error);
              reject(error);
            }
          },
          error_callback: (error: any) => {
            console.error('Google Auth error callback:', error);
            reject(new Error(`Google Auth failed: ${error.type || 'unknown error'}`));
          }
        });

        // Solicitar token (abre popup de Google)
        client.requestAccessToken();

      } catch (error) {
        console.error('Error iniciando Google Sign-In:', error);
        reject(error);
      }
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