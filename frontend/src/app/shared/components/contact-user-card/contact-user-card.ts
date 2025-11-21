import { Component, input, output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AvatarComponent } from '@shared/components/avatar/avatar';
import { StarRating } from '@shared/components/star-rating/star-rating';
import { ConfirmationModalComponent } from '@shared/components/confirmation-modal/confirmation-modal';
import { ChatModalComponent, ChatModalData } from '@shared/components/chat-modal/chat-modal';
import { SocialStore } from '@core/stores/social-store';
import { ChatStore } from '@core/stores/chat-store';
import { RequestSource } from '@core/services/social-api';

export type CardType = 'incoming' | 'outgoing-pending' | 'outgoing-completed' | 'suggestion' | 'empty-result' | 'friend';

export interface ContactUserData {
  requestId?: string;  // Opcional para 'friend'
  contactUser: {
    user: {
      userId: string;
      username: string;
    };
    newMessages: boolean;
  };
  receivedAt?: string;  // Opcional para 'friend'
  status?: string;      // Opcional para 'friend'
  sharedGame?: {
    gameSlug: string;
    gameName: string;
    yourRating: number;
    theirRating: number;
  } | null;
}

// Interface removida ya que contact-user-card maneja todo directamente con el store

@Component({
  selector: 'app-contact-user-card',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    AvatarComponent,
    StarRating,
    ConfirmationModalComponent,
    ChatModalComponent
  ],
  templateUrl: './contact-user-card.html',
  styleUrl: './contact-user-card.scss'
})
export class ContactUserCard {
  private socialStore = inject(SocialStore);
  private chatStore = inject(ChatStore);
  private translateService = inject(TranslateService);
  
  // 📥 Inputs modernos
  userData = input<ContactUserData | null>(null);  // Opcional para 'empty-result'
  cardType = input.required<CardType>();
  
  // 📤 Outputs modernos - Solo los necesarios
  // Para notificar que necesita nueva sugerencia
  requestNewSuggestion = output<void>();
  
  // 🚀 Estado interno para modales
  showDeleteModal = signal(false);
  friendToDelete = signal<{ id: string; username: string } | null>(null);
  
  // 💬 Estado para modal de chat
  showChatModal = signal(false);
  chatData = signal<ChatModalData | null>(null);

   /****************************************************************************
   * BOTONES PARA INCOMING Y OUTGOING-PENDING
   * ***************************************************************************/

  onChatClick(): void {
    const userData = this.userData();
    if (!userData) return;
    
    // Configurar datos del chat y abrir modal
    this.chatData.set({
      userId: userData.contactUser.user.userId,
      username: userData.contactUser.user.username
    });
    this.showChatModal.set(true);
  }

  /****************************************************************************
   * BOTONES PARA FRIEND
   * ***************************************************************************/

  /**
   * 🗑️ Mostrar modal de confirmación para eliminar amigo
   */
  onDeleteFriendClick(): void {
    const userData = this.userData();
    if (!userData) return;
    
    this.friendToDelete.set({ 
      id: userData.contactUser.user.userId, 
      username: userData.contactUser.user.username 
    });
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

  /****************************************************************************
   * MÉTODOS PARA CHAT MODAL
   * ***************************************************************************/

  /**
   * ❌ Cerrar modal de chat
   */
  onCloseChatModal(): void {
    this.showChatModal.set(false);
    this.chatData.set(null);
    
    // Cerrar conversación en ChatStore (el effect del modal no se ejecuta por el timing)
    this.chatStore.closeConversation();
  }

   /****************************************************************************
   * BOTONES PARA INCOMING 
   * ****************************************************************************/

  onAcceptClick(): void {
    const userData = this.userData();
    if (!userData || !userData.requestId) return;
    
    // Para incoming: aceptar solicitud a través del store
    this.socialStore.respondToFriendRequest(userData.requestId, 'ACCEPT').subscribe({
      next: () => {
        console.log('✅ Request accepted');
      },
      error: (error) => {
        console.error('❌ Error accepting request:', error);
      }
    });
  }

  onRejectClick(): void {
    const userData = this.userData();
    if (!userData || !userData.requestId) return;
    
    // Para incoming: rechazar solicitud a través del store
    this.socialStore.respondToFriendRequest(userData.requestId, 'REJECT').subscribe({
      next: () => {
        console.log('✅ Request rejected');
      },
      error: (error) => {
        console.error('❌ Error rejecting request:', error);
      }
    });
  }

  /****************************************************************************
   * BOTONES PARA OUTGOING-COMPLETED 
   * ****************************************************************************/

  /**
   * 👁️ Marcar como notificado 
   */
  onMarkAsNotified(): void {
    const userData = this.userData();
    if (!userData || !userData.requestId) return;
    
    this.socialStore.markAsNotified(userData.requestId).subscribe({
      next: () => {
        console.log('✅ Request marked as notified and removed');
      },
      error: (error) => {
        console.error('❌ Error marking request as notified:', error);
      }
    });
  }

  /****************************************************************************
   * BOTONES PARA SUGGESTION 
   * ****************************************************************************/

  /**
   * 📤 Enviar solicitud de amistad 
   */
  onSendRequestClick(): void {
    const userData = this.userData();
    if (!userData) return;
    
    const sharedGame = userData.sharedGame;
    if (!sharedGame) {
      console.error('❌ No shared game data for suggestion');
      return;
    }

    const requestData = {
      targetUserId: userData.contactUser.user.userId,
      source: RequestSource.SUGGESTION,
      gameSlug: sharedGame.gameSlug,
      yourRating: sharedGame.yourRating
    };

    this.socialStore.sendFriendRequest(requestData).subscribe({
      next: () => {
        console.log('✅ Friend request sent for suggestion');
        // Notificar que necesita nueva sugerencia
        this.requestNewSuggestion.emit();
      },
      error: (error) => {
        console.error('❌ Error sending friend request:', error);
      }
    });
  }

  /**
   * ❌ Rechazar sugerencia
   */
  onRejectSuggestionClick(): void {
    const userData = this.userData();
    if (!userData) return;
    
    const sharedGame = userData.sharedGame;
    if (!sharedGame) {
      console.error('❌ No shared game data for suggestion');
      return;
    }

    this.socialStore.rejectFriendSuggestion(
      userData.contactUser.user.userId, 
      sharedGame.gameSlug
    ).subscribe({
      next: () => {
        console.log('✅ Suggestion rejected');
        // Notificar que necesita nueva sugerencia
        this.requestNewSuggestion.emit();
      },
      error: (error) => {
        console.error('❌ Error rejecting suggestion:', error);
      }
    });
  }

    /****************************************************************************
   * MÉTODOS COMUNES
   * ****************************************************************************/

  /**
   * 🗑️ Obtener mensaje de eliminación traducido
   */
  getDeleteMessage(): string {
    const friend = this.friendToDelete();
    if (!friend) return '';
    return this.translateService.instant('contactUserCard.deleteModal.message', { username: friend.username });
  }

  /**
   * 📅 Formatear fecha relativa
   */
  getRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
    const diffMinutes = Math.floor(diffTime / (1000 * 60));

    if (diffDays > 0) {
      return diffDays === 1
        ? this.translateService.instant('gamePage.timeAgo.daysAgo', { count: diffDays })
        : this.translateService.instant('gamePage.timeAgo.daysAgoPlural', { count: diffDays });
    } else if (diffHours > 0) {
      return diffHours === 1
        ? this.translateService.instant('gamePage.timeAgo.hoursAgo', { count: diffHours })
        : this.translateService.instant('gamePage.timeAgo.hoursAgoPlural', { count: diffHours });
    } else if (diffMinutes > 0) {
      return diffMinutes === 1
        ? this.translateService.instant('gamePage.timeAgo.minutesAgo', { count: diffMinutes })
        : this.translateService.instant('gamePage.timeAgo.minutesAgoPlural', { count: diffMinutes });
    } else {
      return this.translateService.instant('gamePage.timeAgo.justNow');
    }
  }
}