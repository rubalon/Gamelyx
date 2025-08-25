// src/app/features/home/components/friend-list/friend-list.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { SocialStore } from '@core/stores/social-store';
import { AvatarComponent } from '@shared/components/avatar/avatar';

@Component({
  selector: 'app-friend-list',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    AvatarComponent  // 👈 Importamos nuestro Avatar
  ],
  templateUrl: './friend-list.html',
  styleUrl: './friend-list.scss'
})
export class FriendListComponent {
  private socialStore = inject(SocialStore);

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
   * 💬 Abrir chat con usuario (sin funcionalidad)
   */
  onOpenChat(friendId: string, username: string): void {
    console.log('💬 Open chat with:', username, friendId);
    // TODO: Implementar funcionalidad de chat
  }

  /**
   * 🗑️ Eliminar amigo (sin funcionalidad)
   */
  onDeleteFriend(friendId: string, username: string): void {
    console.log('🗑️ Delete friend:', username, friendId);
    // TODO: Implementar funcionalidad de eliminar amigo
  }

  // ✅ Ya no necesitamos estos métodos, los maneja AvatarComponent
  // getAvatarInitial() - ELIMINADO
  // getAvatarColor() - ELIMINADO
}