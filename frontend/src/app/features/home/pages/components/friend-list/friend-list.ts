// src/app/features/home/components/friend-list/friend-list.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { SocialStore } from '@core/stores/social-store';
import { AvatarComponent } from '@shared/components/avatar/avatar';
import { ConfirmationModalComponent } from '@shared/components/confirmation-modal/confirmation-modal';

@Component({
  selector: 'app-friend-list',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    AvatarComponent,
    ConfirmationModalComponent
  ],
  templateUrl: './friend-list.html',
  styleUrl: './friend-list.scss'
})
export class FriendListComponent {
  private socialStore = inject(SocialStore);

  // 🎯 Estado de expansión para móvil (expandido por defecto)
  isExpanded = signal(true);

  // 🗑️ Estado del modal de confirmación
  showDeleteModal = signal(false);
  friendToDelete = signal<{ id: string; username: string } | null>(null);

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

  /**
   * 💬 Abrir chat con usuario (sin funcionalidad)
   */
  onOpenChat(friendId: string, username: string): void {
    console.log('💬 Open chat with:', username, friendId);
    // TODO: Implementar funcionalidad de chat
  }

  /**
   * 🗑️ Mostrar modal de confirmación para eliminar amigo
   */
  onDeleteFriend(friendId: string, username: string): void {
    this.friendToDelete.set({ id: friendId, username });
    this.showDeleteModal.set(true);
  }

  /**
   * ✅ Confirmar eliminación de amigo
   */
  onConfirmDelete(): void {
    const friend = this.friendToDelete();
    if (friend) {
      this.socialStore.deleteFriend(friend.id).subscribe({
        next: (response) => {
          console.log('✅ Friend deleted successfully:', response);
        },
        error: (error) => {
          console.error('❌ Error deleting friend:', error);
        }
      });
    }
    this.closeDeleteModal();
  }

  /**
   * ❌ Cancelar eliminación de amigo
   */
  onCancelDelete(): void {
    this.closeDeleteModal();
  }

  /**
   * 🔒 Cerrar modal y limpiar estado
   */
  private closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.friendToDelete.set(null);
  }

  // ✅ Ya no necesitamos estos métodos, los maneja AvatarComponent
  // getAvatarInitial() - ELIMINADO
  // getAvatarColor() - ELIMINADO
}