// src/app/features/landing/pages/landing-page/landing-page.ts
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Header } from '../../../../shared/components/header/header';
import { HeroSection } from '../../components/hero-section/hero-section';
import { FeaturesSection } from '../../components/features-section/features-section';
import { AuthModal } from '@shared/components/auth-modal/auth-modal';
import { ModalService } from '@shared/services/modal';
import { AsyncPipe } from '@angular/common';
import { FooterComponent } from '@shared/components/footer/footer';
import { AuthStore } from '@core/stores/auth-store';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    Header,
    HeroSection,
    FeaturesSection,
    AuthModal,
    AsyncPipe,
    FooterComponent
  ],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.scss'
})
export class LandingPage implements OnInit {
  protected modalService = inject(ModalService);
  private route = inject(ActivatedRoute);
  private authStore = inject(AuthStore);

  // Controlar visibilidad del banner
  showVerificationBanner = false;

  ngOnInit(): void {
    // Manejar parámetros de verificación de email
    const verified = this.route.snapshot.queryParams['verified'];
    const openModal = this.route.snapshot.queryParams['openModal'];

    if (verified === 'true') {
      this.showVerificationBanner = true;

      // Si se solicita abrir modal en login
      if (openModal === 'login') {
        this.modalService.setAuthModalTab('login');
        this.modalService.openAuthModal();
      }
    }

    //  Abrir modal automáticamente si hay un error de autenticación (ej. JWT expirado)
    if (this.authStore.error()) {
      this.modalService.setAuthModalTab('login');
      this.modalService.openAuthModal();
    }
  }
}