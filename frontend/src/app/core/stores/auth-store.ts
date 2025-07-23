import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

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

export interface User {
  id: string;
  username: string;
  email: string;
  emailVerified: boolean;
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthStore {
  private http = inject(HttpClient);
  private router = inject(Router);

  // URL base del backend
  private readonly API_URL = 'http://localhost:8080/api/auth';

  // Estado de autenticación usando signals (Angular 20)
  private _authState = signal<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: false,
    error: null
  });

  // Estado público readonly
  public readonly authState = this._authState.asReadonly();

  // Computed signals para acceso fácil
  public readonly isAuthenticated = computed(() => this._authState().isAuthenticated);
  public readonly isLoading = computed(() => this._authState().isLoading);
  public readonly user = computed(() => this._authState().user);
  public readonly error = computed(() => this._authState().error);

  constructor() {
    // Verificar si hay tokens almacenados al inicializar
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
        // Si hay error al parsear, limpiar storage
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
    localStorage.setItem('refreshToken', response.refreshToken); // TODO : almacenar refresh token de forma segura
    localStorage.setItem('userData', JSON.stringify({
      id: response.userId,
      username: response.username,
      email: response.email,
      emailVerified: true // Asumimos verificado por defecto, se puede actualizar después
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
    this.updateAuthState({ isLoading: true, error: null });

    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credentials)
      .pipe(
        tap(response => {
          // Almacenar tokens y datos del usuario
          this.storeAuthData(response);
          
          // Actualizar estado
          this.updateAuthState({
            user: {
              id: response.userId,
              username: response.username,
              email: response.email,
              emailVerified: true
            },
            isAuthenticated: true,
            isLoading: false,
            error: null
          });
        }),
        catchError(error => this.handleError(error))
      );
  }

  /**
   * Realiza el registro del usuario
   */
  register(userData: RegisterRequest): Observable<AuthResponse> {
    this.updateAuthState({ isLoading: true, error: null });

    return this.http.post<AuthResponse>(`${this.API_URL}/register`, userData)
      .pipe(
        tap(response => {
          // Almacenar tokens y datos del usuario
          this.storeAuthData(response);
          
          // Actualizar estado
          this.updateAuthState({
            user: {
              id: response.userId,
              username: response.username,
              email: response.email,
              emailVerified: false // El email no está verificado al registrarse
            },
            isAuthenticated: true,
            isLoading: false,
            error: null
          });
        }),
        catchError(error => this.handleError(error))
      );
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
      error: null
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
   * Limpia el error actual
   */
  clearError(): void {
    this.updateAuthState({ error: null });
  }
}