import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ModalService } from '@shared/services/modal';
import { AuthStore, LoginRequest, RegisterRequest } from '@core/stores/auth-store';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { GoogleAuthService } from '@core/services/google-auth';

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
  private googleAuthService = inject(GoogleAuthService);

  // Control de pestañas
  activeTab: 'login' | 'register' = 'login';

  // 🆕 Estados de loading independientes
  isGoogleLoading = false;
  isTraditionalLoading = false;

  // 👁️ Estados de visibilidad de contraseñas
  showLoginPassword = false;
  showRegisterPassword = false;
  showRegisterConfirmPassword = false;

  // Formularios reactivos
  loginForm!: FormGroup;
  registerForm!: FormGroup;

  constructor() {
    this.initializeForms();
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
    this.authStore.clearRegistrationPending();
  }

  switchTab(tab: 'login' | 'register'): void {
    this.modalService.setAuthModalTab(tab);
    this.authStore.clearError();
    this.authStore.clearRegistrationPending();
  }

  // Método único para Google Auth - funciona igual para login y register
  onGoogleAuth(): void {
    console.log(`Iniciando Google Auth desde pestaña: ${this.activeTab}`);
    
    // 🆕 Activar loading específico de Google
    this.isGoogleLoading = true;
    
    this.googleAuthService.signInWithGoogle(this.activeTab).then(
      (userInfo) => {
        this.handleGoogleAuthSuccess(userInfo);
      }
    ).catch(
      (error) => {
        this.handleGoogleAuthError(error);
      }
    ).finally(() => {
      // 🆕 Desactivar loading específico de Google
      this.isGoogleLoading = false;
    });
  }

  // Manejar éxito de Google Auth
  private handleGoogleAuthSuccess(userInfo: any): void {
    console.log('Google Auth exitoso - Datos recibidos:', {
      googleId: userInfo.sub,
      email: userInfo.email,
      name: userInfo.name
    });
    
    // Crear request único para el backend
    const googleAuthRequest = {
      googleId: userInfo.sub,
      email: userInfo.email,
      name: userInfo.name
    };

    // Llamar al método único del AuthStore (mismo para ambas pestañas)
    this.authStore.authenticateWithGoogle(googleAuthRequest).subscribe({
      next: (response) => {
        console.log('Google Auth completado exitosamente:', response);
        this.modalService.closeAuthModal();
        this.router.navigate(['/home']);
      },
      error: (error) => {
        console.error('Error en Google Auth:', error);
        // El error ya se muestra en el template via authError getter
      },
      complete: () => {
        // 🆕 Asegurar que se desactive el loading al completar
        this.isGoogleLoading = false;
      }
    });
  }

  // Manejar errores de Google Auth
  private handleGoogleAuthError(error: any): void {
    console.error('Error en Google Sign-In:', error);
    // 🆕 Desactivar loading en caso de error
    this.isGoogleLoading = false;
    // Los errores se muestran automáticamente via el authError getter
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

      // 🆕 Activar loading específico de autenticación tradicional
      this.isTraditionalLoading = true;

      this.authStore.login(loginData).subscribe({
        next: (response) => {
          console.log('Login exitoso:', response);
          this.modalService.closeAuthModal();
          this.router.navigate(['/home']);
        },
        error: (error) => {
          console.error('Error en login:', error);
        },
        complete: () => {
          // 🆕 Desactivar loading al completar
          this.isTraditionalLoading = false;
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

      // 🆕 Activar loading específico de autenticación tradicional
      this.isTraditionalLoading = true;

      this.authStore.register(registerData).subscribe({
        next: (response) => {
          console.log('Registro exitoso:', response);
          // NO cerrar modal, mostrar mensaje de verificación de email
        },
        error: (error) => {
          console.error('Error en registro:', error);
        },
        complete: () => {
          // 🆕 Desactivar loading al completar
          this.isTraditionalLoading = false;
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

  // 🆕 Getters actualizados para usar los estados de loading independientes
  get isLoading() {
    return this.authStore.isLoading();
  }

  get authError() {
    return this.authStore.error();
  }

  get isAuthenticated() {
    return this.authStore.isAuthenticated();
  }

  get registrationPending() {
    return this.authStore.registrationPending();
  }

  // 👁️ Métodos para toggle de visibilidad de contraseñas
  toggleLoginPasswordVisibility(): void {
    this.showLoginPassword = !this.showLoginPassword;
  }

  toggleRegisterPasswordVisibility(): void {
    this.showRegisterPassword = !this.showRegisterPassword;
  }

  toggleRegisterConfirmPasswordVisibility(): void {
    this.showRegisterConfirmPassword = !this.showRegisterConfirmPassword;
  }
}