import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ModalService } from '@shared/services/modal';
import { AuthStore, LoginRequest, RegisterRequest } from '@core/stores/auth-store';

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

  // Control de pestañas
  activeTab: 'login' | 'register' = 'login';

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
  }

  switchTab(tab: 'login' | 'register'): void {
    this.modalService.setAuthModalTab(tab);
    this.authStore.clearError();
  }

  private initializeForms(): void {
    this.loginForm = this.formBuilder.group({
      emailOrUsername: ['', [Validators.required]], 
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    // Formulario de registro - CORREGIDO: Sintaxis Angular 20
    this.registerForm = this.formBuilder.nonNullable.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]], 
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: [this.passwordMatchValidator] 
    });
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const form = control as FormGroup;
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    
    // Si las contraseñas no coinciden
    if (password?.value !== confirmPassword?.value) {
      return { passwordMismatch: true }; 
    }
    
    return null; // ✅ Todo bien
  }

  onLogin(): void {
    if (this.loginForm.valid) {
      const loginData = {
        emailOrUsername: this.loginForm.value.emailOrUsername,
        password: this.loginForm.value.password
      };

      console.log('Iniciando login:', loginData);


      this.authStore.login(loginData).subscribe({
        next: (response) => {
          console.log('Login exitoso:', response);
          this.modalService.closeAuthModal();
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
        password: this.registerForm.value.password
      };

      console.log('Iniciando registro:', registerData);

      this.authStore.register(registerData).subscribe({
        next: (response) => {
          console.log('Registro exitoso:', response);
          this.modalService.closeAuthModal();
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
    if (field && field.invalid && field.touched) {
      if (field.errors?.['required']) return 'Este campo es obligatorio';
      if (field.errors?.['email']) return 'Email inválido';
      if (field.errors?.['minlength']) return `Mínimo ${field.errors?.['minlength'].requiredLength} caracteres`;
      if (field.errors?.['maxlength']) return `Máximo ${field.errors?.['maxlength'].requiredLength} caracteres`;
      if (field.errors?.['passwordMismatch']) return 'Las contraseñas no coinciden'; 
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
}