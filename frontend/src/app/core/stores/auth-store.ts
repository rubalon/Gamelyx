// src/app/core/stores/auth-store.ts (ACTUALIZADO)
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// Interfaces para tipado
export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  username: string;
  email: string;
  message: string;
}

// 🆕 NUEVA: Respuesta del registro sin JWT
export interface RegisterResponse {
  userId: string;
  username: string;
  email: string;
  message: string;
  emailVerificationRequired: boolean;
}

export interface User {
  id: string;
  username: string;
  email: string;
  emailVerified: boolean;
}

// 🆕 ACTUALIZADO: Estado con información de registro pendiente
export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  // 🆕 NUEVO: Estado para mostrar mensaje de verificación
  registrationPending: {
    email: string;
    message: string;
  } | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthStore {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly API_URL = `${environment.apiUrl}/auth`;

  // Estado de autenticación usando signals (Angular 20)
  private _authState = signal<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,
    registrationPending: null // 🆕 NUEVO
  });

  // Estado público readonly
  public readonly authState = this._authState.asReadonly();

  // Computed signals para acceso fácil
  public readonly isAuthenticated = computed(() => this._authState().isAuthenticated);
  public readonly isLoading = computed(() => this._authState().isLoading);
  public readonly user = computed(() => this._authState().user);
  public readonly error = computed(() => this._authState().error);
  
  // 🆕 NUEVO: Signal para estado de registro pendiente
  public readonly registrationPending = computed(() => this._authState().registrationPending);

  constructor() {
    this.checkStoredAuth();
  }

  /**
   * Verifica si hay tokens almacenados y restaura el estado de autenticación
   */
  private checkStoredAuth(): void {
    const accessToken = localStorage.getItem('accessToken');
    const userData = localStorage.getItem('userData');

    if (accessToken && userData) {
      try {
        const user = JSON.parse(userData);
        this.updateAuthState({
          user,
          isAuthenticated: true,
          isLoading: false,
          error: null
        });
      } catch (error) {
        this.clearAuthData();
      }
    }
  }

  /**
   * Actualiza el estado de autenticación
   */
  private updateAuthState(newState: Partial<AuthState>): void {
    this._authState.update(current => ({
      ...current,
      ...newState
    }));
  }

  /**
   * Almacena los datos de autenticación en localStorage
   */
  private storeAuthData(response: AuthResponse): void {
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('userData', JSON.stringify({
      id: response.userId,
      username: response.username,
      email: response.email,
      emailVerified: true
    }));
  }

  /**
   * Limpia todos los datos de autenticación
   */
  private clearAuthData(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userData');
  }

  /**
   * Maneja errores HTTP y los convierte a mensajes legibles
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Ha ocurrido un error inesperado';

    if (error.error && typeof error.error === 'object') {
      errorMessage = error.error.message || error.error.error || errorMessage;
    } else if (error.error && typeof error.error === 'string') {
      errorMessage = error.error;
    } else if (error.message) {
      errorMessage = error.message;
    }

    this.updateAuthState({ error: errorMessage, isLoading: false });
    return throwError(() => errorMessage);
  }

  /**
   * Realiza el login del usuario
   */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    // 🧹 LIMPIAR DATOS ANTES DE ENVIAR
    const cleanCredentials: LoginRequest = {
      usernameOrEmail: credentials.usernameOrEmail.trim(), // 👈 Solo trim, sin toLowerCase para usernames
      password: credentials.password // Password no se modifica
    };

    // 🔍 DEBUG DETALLADO
    /*
    console.log('🔍 LOGIN DEBUG - Original:', credentials);
    console.log('🔍 LOGIN DEBUG - Cleaned:', cleanCredentials);
    console.log('🔍 LOGIN DEBUG - Comparison:', {
      originalLength: credentials.usernameOrEmail.length,
      cleanedLength: cleanCredentials.usernameOrEmail.length,
      hasSpaces: credentials.usernameOrEmail !== credentials.usernameOrEmail.trim(),
      originalBytes: Array.from(credentials.usernameOrEmail).map(c => c.charCodeAt(0)),
      cleanedBytes: Array.from(cleanCredentials.usernameOrEmail).map(c => c.charCodeAt(0))
    });
    */

    this.updateAuthState({ isLoading: true, error: null });

    return this.http.post<AuthResponse>(`${this.API_URL}/login`, cleanCredentials)
      .pipe(
        tap(response => {
//          console.log('✅ Login successful:', response);
          // Almacenar tokens y datos del usuario
          this.storeAuthData(response);
          
          // Actualizar estado como autenticado
          this.updateAuthState({
            user: {
              id: response.userId,
              username: response.username,
              email: response.email,
              emailVerified: true
            },
            isAuthenticated: true,
            isLoading: false,
            error: null,
            registrationPending: null // Limpiar estado de registro
          });
        }),
        catchError(error => {
 //         console.error('❌ Login error:', error);
          return this.handleError(error);
        })
      );
  }

  /**
   * 🔧 CORREGIDO: Registro NO autentica, solo muestra mensaje de verificación
   */
  register(userData: RegisterRequest): Observable<RegisterResponse> {
    this.updateAuthState({ isLoading: true, error: null });

    return this.http.post<RegisterResponse>(`${this.API_URL}/register`, userData)
      .pipe(
        tap(response => {
          // 🔧 CORREGIDO: NO almacenar tokens (no los hay)
          // ❌ this.storeAuthData(response);
          
          // 🔧 CORREGIDO: NO marcar como autenticado
          this.updateAuthState({
            user: null, // 👈 NO hay usuario autenticado
            isAuthenticated: false, // 👈 NO está autenticado
            isLoading: false,
            error: null,
            // 🆕 NUEVO: Guardar info de registro pendiente
            registrationPending: {
              email: response.email,
              message: response.message
            }
          });
        }),
        catchError(error => this.handleError(error))
      );
  }

  /**
   * 🆕 NUEVO: Limpia el estado de registro pendiente
   */
  clearRegistrationPending(): void {
    this.updateAuthState({ registrationPending: null });
  }

  /**
   * Cierra la sesión del usuario
   */
  logout(): void {
    this.clearAuthData();
    this.updateAuthState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
      registrationPending: null
    });
    this.router.navigate(['/']);
  }

  /**
   * Obtiene el token de acceso almacenado
   */
  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  /**
   * Obtiene el token de refresh almacenado
   */
  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  /**
   * 🆕 NUEVO: Verifica el email del usuario
   */
  verifyEmail(token: string): Observable<any> {
    this.updateAuthState({ isLoading: true, error: null });

    return this.http.post(`${this.API_URL}/verify-email`, { token })
      .pipe(
        tap((response) => {
          console.log('Email verificado exitosamente:', response);
          this.updateAuthState({
            isLoading: false,
            error: null
          });
        }),
        catchError(error => {
          this.updateAuthState({ isLoading: false });
          return this.handleError(error);
        })
      );
  }

  /**
   * Limpia el error actual
   */
  clearError(): void {
    this.updateAuthState({ error: null });
  }
}