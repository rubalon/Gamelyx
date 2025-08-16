import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { TranslationService } from '@core/services/translations';
import { ModalService } from '@shared/services/modal';
import { AuthStore } from '@core/stores/auth-store';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './header.html',
  styleUrl: './header.scss'
})
export class Header {
  
  private router = inject(Router);
  private translationService = inject(TranslationService);
  private modalService = inject(ModalService);
  private authStore = inject(AuthStore);

  // Estado del menú móvil
  isMobileMenuOpen = false;

  // Getters para acceder al estado de autenticación desde el template
  get isAuthenticated() {
    return this.authStore.isAuthenticated();
  }

  get currentUser() {
    return this.authStore.user();
  }

  // Toggle del menú móvil
  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  // Cerrar menú móvil
  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  onLoginClick(): void {
    // Solo abrir modal si NO está logueado
    if (!this.isAuthenticated) {
      this.modalService.openAuthModal('login');
    }
  }

  onLogoutClick(): void {
    this.authStore.logout();
    // El logout ya redirige a '/landing' desde el AuthStore
  }

  changeLanguage(event: any): void {
    const language = event.target.value;
    this.translationService.changeLanguage(language);
  }

  getCurrentLanguage(): string {
    return this.translationService.getCurrentLanguage();
  }
}