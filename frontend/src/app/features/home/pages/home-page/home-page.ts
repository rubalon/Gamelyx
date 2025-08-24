// src/app/features/home/pages/home-page/home-page.ts
import { Component, inject, OnInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { Header } from '../../../../shared/components/header/header';
import { GameSearchBar } from '../../../../shared/components/game-search-bar/game-search-bar';
import { SocialStore } from '../../../../core/stores/social-store';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    Header, 
    TranslateModule,
    CommonModule,        // Para @if, @for
    GameSearchBar        // Componente existente de búsqueda de juegos
  ],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss'
})
export class HomePage implements OnInit {

  // 🏪 Inyectar SocialStore siguiendo patrón Angular 20
  private socialStore = inject(SocialStore);

  // 📊 Acceso público a datos sociales (patrón correcto)
  get socialData() { 
    return this.socialStore.state(); 
  }

  // 🔄 Loading states
  get isLoadingSocialData() { 
    return this.socialStore.state().isLoadingHomeSocialData; 
  }

  // ❌ Error state
  get socialError() { 
    return this.socialStore.state().error; 
  }

  // 📈 Estadísticas computed (patrón correcto - solo para transformaciones)
  get totalFriends() { 
    return this.socialStore.totalFriends(); 
  }

  get pendingIncomingRequests() { 
    return this.socialStore.pendingIncomingRequests(); 
  }

  get pendingOutgoingRequests() { 
    return this.socialStore.pendingOutgoingRequests(); 
  }

  /**
   * 🚀 Inicialización del componente
   * Carga datos sociales al entrar al home
   */
  ngOnInit(): void {
    console.log('🏠 HomePage initialized - Loading social data...');
    
    // 📞 Llamar al endpoint principal del home social
    this.socialStore.loadHomeSocialData().subscribe({
      next: (data) => {
        console.log('✅ Social data loaded successfully:', data);
      },
      error: (error) => {
        console.error('❌ Error loading social data:', error);
        // El error ya se maneja en el store, aquí solo logging
      }
    });
  }

  /**
   * 🔄 Método para refrescar datos manualmente
   * Para uso futuro (botón refresh, pull-to-refresh, etc.)
   */
  refreshSocialData(): void {
    console.log('🔄 Manual refresh requested');
    this.socialStore.refreshAllData().subscribe();
  }

  /**
   * 🧹 Limpiar error si existe
   */
  clearError(): void {
    this.socialStore.clearError();
  }
}