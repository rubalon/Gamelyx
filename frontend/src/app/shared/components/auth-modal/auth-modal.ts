
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ModalService } from '../../services/modal';


@Component({
  selector: 'app-auth-modal',
  standalone: true,
  imports: [TranslateModule, CommonModule, ReactiveFormsModule],
  templateUrl: './auth-modal.html',
  styleUrl: './auth-modal.scss'
})
export class AuthModal implements OnInit , OnDestroy {

  private modalService = inject(ModalService);
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
  }

  switchTab(tab: 'login' | 'register'): void {
    this.modalService.setAuthModalTab(tab);
  }

  private initializeForms(): void {
    // Formulario de login
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    // Formulario de registro
    this.registerForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  onLogin(): void {
    if (this.loginForm.valid) {
      console.log('Login datos:', this.loginForm.value);
      // TODO: Implementar lógica de login
    } else {
      console.log('Formulario de login inválido');
    }
  }

  onRegister(): void {
    if (this.registerForm.valid) {
      console.log('Registro datos:', this.registerForm.value);
      // TODO: Implementar lógica de registro
    } else {
      console.log('Formulario de registro inválido');
    }
  }

  // Helper para mostrar errores
  getFieldError(form: FormGroup, fieldName: string): string | null {
    const field = form.get(fieldName);
    if (field && field.invalid && field.touched) {
      if (field.errors?.['required']) return 'Este campo es obligatorio';
      if (field.errors?.['email']) return 'Email inválido';
      if (field.errors?.['minlength']) return `Mínimo ${field.errors?.['minlength'].requiredLength} caracteres`;
    }
    return null;
  }

}