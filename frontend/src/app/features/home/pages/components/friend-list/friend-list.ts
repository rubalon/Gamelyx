// src/app/features/home/components/friend-list/friend-list.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { SocialStore } from '@core/stores/social-store';
import { ContactUserCard, type ContactUserData } from '@shared/components/contact-user-card/contact-user-card';

@Component({
  selector: 'app-friend-list',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    ContactUserCard
  ],
  templateUrl: './friend-list.html',
  styleUrl: './friend-list.scss'
})
export class FriendListComponent {
  private socialStore = inject(SocialStore);

  // 🎯 Estado de expansión para móvil (expandido por defecto)
  isExpanded = signal(true);

  // 🔄 Transformar datos de amigos para contact-user-card
  get friendsForCard(): ContactUserData[] {
    return this.friends.map(friend => ({
      contactUser: friend,
      // Los campos opcionales no se necesitan para el tipo 'friend'
    }));
  }

  // 📊 Datos del store
  get friends() {
    return this.socialStore.state().friends;
  }

  get totalFriends() {
    return this.socialStore.totalFriends();
  }

  get isLoading() {
    return this.socialStore.state().isLoadingHomeSocialData;
  }

  /**
   * 📱 Alternar expansión en móvil
   */
  toggleExpansion(): void {
    this.isExpanded.set(!this.isExpanded());
  }

  // ✅ Toda la lógica de chat y delete ahora está en contact-user-card
  // No necesitamos métodos adicionales aquí
}