import { Component, inject, OnDestroy, OnInit, effect } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ModalService } from '@shared/services/modal';
import { AuthStore, LoginRequest, RegisterRequest } from '@core/stores/auth-store';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { GoogleAuthService } from '@core/services/google-auth'; // 🆕 NUEVO

@Component({
  selector: 'app-auth-modal',
  standalone: true,
  imports: [TranslateModule, CommonModule, ReactiveFormsModule],
  templateUrl: './auth-modal.html',
  styleUrl: './auth-modal.scss'
})
export class AuthModal implements OnInit, OnDestroy {

  private modalService = inject(ModalService);
  private authStore = inject(AuthStore); 
  private destroy$ = new Subject<void>();
  private formBuilder = inject(FormBuilder);
  private translateService = inject(TranslateService);
  private router = inject(Router);
  private googleAuthService = inject(GoogleAuthService); // 🆕 NUEVO 

  // Control de pestañas
  activeTab: 'login' | 'register' = 'login';

  // Formularios reactivos
  loginForm!: FormGroup;
  registerForm!: FormGroup;

  constructor() {
    this.initializeForms();
    console.log('🔧 AuthModal: Inyectando GoogleAuthService para forzar inicialización');
  }

  ngOnInit(): void {
    this.modalService.authModalTab$
      .pipe(takeUntil(this.destroy$))
      .subscribe(tab => {
        this.activeTab = tab;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onClose(): void {
    this.modalService.closeAuthModal();
    this.authStore.clearError();
    // 🆕 NUEVO: Limpiar estado de registro pendiente al cerrar
    this.authStore.clearRegistrationPending();
  }

  switchTab(tab: 'login' | 'register'): void {
    this.modalService.setAuthModalTab(tab);
    this.authStore.clearError();
    // 🆕 NUEVO: Limpiar estado de registro pendiente al cambiar pestañas
    this.authStore.clearRegistrationPending();
  }

  // 🆕 NUEVO: Método para manejar autenticación con Google
  onGoogleAuth(): void {
    console.log(`🚀 Iniciando Google Auth - Modo: ${this.activeTab}`);
    
    // Usar el GoogleAuthService real
    this.googleAuthService.signInWithGoogle(this.activeTab).then(
      (userInfo) => {
        console.log(`✅ Google Auth exitoso en modo ${this.activeTab}:`, userInfo);
        this.handleGoogleAuthSuccess(userInfo);
      }
    ).catch(
      (error) => {
        console.error(`❌ Error en Google Auth (${this.activeTab}):`, error);
        this.handleGoogleAuthError(error);
      }
    );
  }

  // 🆕 NUEVO: Manejar éxito de Google Auth
  private handleGoogleAuthSuccess(userInfo: any): void {
    console.log('🎉 Procesando éxito de Google Auth:', userInfo);
    
    if (this.activeTab === 'login') {
      console.log('🔐 Procesando Google Login...');
      this.processGoogleLogin(userInfo);
    } else {
      console.log('📝 Procesando Google Register...');
      this.processGoogleRegister(userInfo);
    }
  }

  // 🆕 NUEVO: Manejar errores de Google Auth
  private handleGoogleAuthError(error: any): void {
    console.error('❌ Error en Google Auth:', error);
    
    // TODO: Mostrar error en UI
    // Por ahora solo logging, en siguiente iteración mostraremos en template
  }

  // 🆕 NUEVO: Procesar login con Google (placeholder)
  private processGoogleLogin(userInfo: any): void {
    console.log('🔍 Login con Google - Info recibida:', {
      email: userInfo.email,
      name: userInfo.name,
      googleId: userInfo.sub
    });
    
    // 🚧 TODO: Enviar al backend para login/crear usuario
    // Por ahora simulamos éxito
    console.log('✅ Google Login simulado exitoso');
    
    // Cerrar modal y redirigir (simulado)
    // this.modalService.closeAuthModal();
    // this.router.navigate(['/home']);
  }

  // 🆕 NUEVO: Procesar registro con Google (placeholder)  
  private processGoogleRegister(userInfo: any): void {
    console.log('📝 Registro con Google - Info recibida:', {
      email: userInfo.email,
      name: userInfo.name,
      googleId: userInfo.sub,
      picture: userInfo.picture
    });
    
    // 🚧 TODO: Enviar al backend para crear usuario
    // Por ahora simulamos éxito
    console.log('✅ Google Register simulado exitoso');
    
    // Cerrar modal y redirigir (simulado)
    // this.modalService.closeAuthModal(); 
    // this.router.navigate(['/home']);
  }

  private initializeForms(): void {
    this.loginForm = this.formBuilder.group({
      emailOrUsername: ['', [Validators.required]], 
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.registerForm = this.formBuilder.nonNullable.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]], 
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required,]]
    }, {
      validators: [this.passwordMatchValidator] 
    });

    // Manejar estado disabled con effect (Angular 20 Signals)
    effect(() => {
      const loading = this.authStore.isLoading();
      if (loading) {
        this.loginForm.disable();
        this.registerForm.disable();
      } else {
        this.loginForm.enable();
        this.registerForm.enable();
      }
    });
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const form = control as FormGroup;
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    
    if (password?.value !== confirmPassword?.value) {
      return { passwordMismatch: true }; 
    }
    
    return null;
  }

  onLogin(): void {
    if (this.loginForm.valid) {
      const loginData = {
        usernameOrEmail: this.loginForm.value.emailOrUsername,
        password: this.loginForm.value.password
      };

      console.log('Iniciando login tradicional:', loginData);

      this.authStore.login(loginData).subscribe({
        next: (response) => {
          console.log('Login exitoso:', response);
          this.modalService.closeAuthModal();
          this.router.navigate(['/home']);
        },
        error: (error) => {
          console.error('Error en login:', error);
        }
      });
    } else {
      console.log('Formulario de login inválido');
      this.markFormGroupTouched(this.loginForm);
    }
  }

  onRegister(): void {
    if (this.registerForm.valid) {
      const registerData = {
        username: this.registerForm.value.username,
        email: this.registerForm.value.email,
        password: this.registerForm.value.password,
        confirmPassword: this.registerForm.value.confirmPassword
      };

      console.log('Iniciando registro tradicional:', registerData);

      this.authStore.register(registerData).subscribe({
        next: (response) => {
          console.log('Registro exitoso:', response);
          // 🔧 CORREGIDO: NO cerrar modal, mostrar mensaje de verificación
          // ❌ this.modalService.closeAuthModal();
          
          // 🆕 NUEVO: El estado de registrationPending se actualiza automáticamente
          // El template mostrará el mensaje de verificación
        },
        error: (error) => {
          console.error('Error en registro:', error);
        }
      });
    } else {
      console.log('Formulario de registro inválido');
      this.markFormGroupTouched(this.registerForm);
    }
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(field => {
      const control = formGroup.get(field);
      control?.markAsTouched({ onlySelf: true });
    });
  }

  getFieldError(form: FormGroup, fieldName: string): string | null {
    const field = form.get(fieldName);
    if (field && field.touched) {
      if (field.errors?.['required']) {
        return this.translateService.instant('auth.errors.required');
      }
      if (field.errors?.['email']) {
        return this.translateService.instant('auth.errors.email');
      }
      if (field.errors?.['minlength']) {
        const requiredLength = field.errors?.['minlength'].requiredLength;
        return this.translateService.instant('auth.errors.minlength', { length: requiredLength });
      }
      if (field.errors?.['maxlength']) {
        const requiredLength = field.errors?.['maxlength'].requiredLength;
        return this.translateService.instant('auth.errors.maxlength', { length: requiredLength });
      }
      if (fieldName === 'confirmPassword' && form.errors?.['passwordMismatch']) {
        return this.translateService.instant('auth.errors.passwordMismatch');
      }
    }
    return null;
  }

  get isLoading() {
    return this.authStore.isLoading();
  }

  get authError() {
    return this.authStore.error();
  }

  get isAuthenticated() {
    return this.authStore.isAuthenticated();
  }

  // 🆕 NUEVO: Getter para estado de registro pendiente
  get registrationPending() {
    return this.authStore.registrationPending();
  }
}