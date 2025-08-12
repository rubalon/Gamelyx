// src/app/features/auth/pages/email-verification/email-verification.ts
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { AuthStore } from '@core/stores/auth-store';

@Component({
  selector: 'app-email-verification',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './email-verification.html',
  styleUrl: './email-verification.scss'
})
export class EmailVerification implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authStore = inject(AuthStore);

  isLoading = true;
  verificationStatus: 'loading' | 'success' | 'error' | 'no-token' = 'loading';
  errorMessage: string | null = null;

  ngOnInit(): void {
    const token = this.route.snapshot.queryParams['token'];
    
    if (!token) {
      this.verificationStatus = 'no-token';
      this.isLoading = false;
      return;
    }

    console.log('Token de verificación recibido:', token);
    this.verifyEmailToken(token);
  }

  private verifyEmailToken(token: string): void {
    this.authStore.verifyEmail(token).subscribe({
      next: (response) => {
        console.log('Verificación exitosa:', response);
        this.verificationStatus = 'success';
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error en verificación:', error);
        this.verificationStatus = 'error';
        this.errorMessage = error || 'Error al verificar el email';
        this.isLoading = false;
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/landing'], { 
      queryParams: { 
        verified: 'true',
        openModal: 'login'
      }
    });
  }

  goToLanding(): void {
    this.router.navigate(['/landing']);
  }

  contactSupport(): void {
    console.log('Contactar soporte');
  }
}