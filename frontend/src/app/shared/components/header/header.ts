// src/app/shared/components/header/header.ts
import { Component, inject, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { TranslationService } from '@core/services/translations';
import { ModalService } from '@shared/services/modal';
import { AuthStore } from '@core/stores/auth-store';
import { AvatarComponent } from '@shared/components/avatar/avatar';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    TranslateModule,
    AvatarComponent  // 👈 Importamos nuestro Avatar
  ],
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
  
  // Estado del dropdown de idiomas
  isLanguageDropdownOpen = false;

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
    // Cerrar dropdown de idiomas si está abierto
    this.isLanguageDropdownOpen = false;
  }

  // Cerrar menú móvil
  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }
  
  // Toggle del dropdown de idiomas
  toggleLanguageDropdown(): void {
    this.isLanguageDropdownOpen = !this.isLanguageDropdownOpen;
  }
  
  // Seleccionar idioma desde el dropdown personalizado
  selectLanguage(language: string): void {
    this.translationService.changeLanguage(language);
    this.isLanguageDropdownOpen = false;
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
  
  // Cerrar dropdowns al hacer clic fuera
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    
    // Si el clic no es en el dropdown de idiomas, cerrarlo
    if (!target.closest('.relative') && this.isLanguageDropdownOpen) {
      this.isLanguageDropdownOpen = false;
    }
  }
}