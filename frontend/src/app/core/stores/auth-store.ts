// src/app/core/stores/auth-store.ts (ACTUALIZADO CON GOOGLE AUTH)
import { Injectable, inject, signal, computed, Injector } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ChatStore } from './chat-store';

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

// Interface única para Google Auth
export interface GoogleAuthRequest {
  googleId: string;
  email: string;
  name: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  username: string;
  email: string;
  message: string;
}

// Respuesta del registro sin JWT
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

// Estado con información de registro pendiente
export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
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
  private injector = inject(Injector);

  private readonly API_URL = `${environment.apiUrl}/auth`;

  // Estado de autenticación usando signals (Angular 20)
  private _authState = signal<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,
    registrationPending: null
  });

  // Estado público readonly
  public readonly authState = this._authState.asReadonly();

  // Computed signals para acceso fácil
  public readonly isAuthenticated = computed(() => this._authState().isAuthenticated);
  public readonly isLoading = computed(() => this._authState().isLoading);
  public readonly user = computed(() => this._authState().user);
  public readonly error = computed(() => this._authState().error);
  public readonly registrationPending = computed(() => this._authState().registrationPending);

  constructor() {
    this.checkStoredAuth();
  }

  /**
   * 🔌 Obtener ChatStore con lazy loading usando Injector para evitar circular dependency
   */
  private getChatStore() {
    return this.injector.get(ChatStore);
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
        
        // 🔌 Conectar WebSocket si hay token válido al iniciar la app
        this.getChatStore().connectWebSocket();
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
    const cleanCredentials: LoginRequest = {
      usernameOrEmail: credentials.usernameOrEmail.trim(),
      password: credentials.password
    };

    this.updateAuthState({ isLoading: true, error: null });

    return this.http.post<AuthResponse>(`${this.API_URL}/login`, cleanCredentials)
      .pipe(
        tap(response => {
          this.storeAuthData(response);
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
            registrationPending: null
          });
          
          // 🔌 Conectar WebSocket después de login exitoso
          this.getChatStore().connectWebSocket();
        }),
        catchError(error => this.handleError(error))
      );
  }

  /**
   * Registro tradicional con email y contraseña
   */
  register(userData: RegisterRequest): Observable<RegisterResponse> {
    this.updateAuthState({ isLoading: true, error: null });

    return this.http.post<RegisterResponse>(`${this.API_URL}/register`, userData)
      .pipe(
        tap(response => {
          this.updateAuthState({
            user: null,
            isAuthenticated: false,
            isLoading: false,
            error: null,
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
   * Autenticación única con Google
   * Maneja automáticamente login o registro según si el usuario existe
   */
  authenticateWithGoogle(googleUserInfo: GoogleAuthRequest): Observable<AuthResponse> {
    this.updateAuthState({ isLoading: true, error: null });

    return this.http.post<AuthResponse>(`${this.API_URL}/google`, googleUserInfo)
      .pipe(
        tap(response => {
          this.storeAuthData(response);
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
            registrationPending: null
          });
          
          // 🔌 Conectar WebSocket después de Google auth exitoso
          this.getChatStore().connectWebSocket();
        }),
        catchError(error => this.handleError(error))
      );
  }

  /**
   * Limpia el estado de registro pendiente
   */
  clearRegistrationPending(): void {
    this.updateAuthState({ registrationPending: null });
  }

  /**
   * Cierra la sesión del usuario
   */
  logout(): void {
    // 🔌 Desconectar WebSocket al hacer logout
    this.getChatStore().disconnectWebSocket();
    
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
   * 🔗 Signal público para que chat-store pueda acceder al token
   */
  public readonly token = computed(() => localStorage.getItem('accessToken'));

  /**
   * 🔗 Signal público para que chat-store pueda acceder al usuario actual
   */
  public readonly currentUser = computed(() => this._authState().user);

  /**
   * Obtiene el token de refresh almacenado
   */
  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  /**
   * Verifica el email del usuario
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