import { Component, input, output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AvatarComponent } from '@shared/components/avatar/avatar';
import { StarRating } from '@shared/components/star-rating/star-rating';
import { SocialStore } from '@core/stores/social-store';
import { RequestSource } from '@core/services/social-api';

export type CardType = 'incoming' | 'outgoing' | 'suggestion' | 'empty-result';

export interface ContactUserData {
  requestId: string;
  contactUser: {
    user: {
      userId: string;
      username: string;
    };
    chatId?: string | null;
    newMessages: boolean;
  };
  receivedAt: string;
  status: string;
  sharedGame?: {
    gameSlug: string;
    gameName: string;
    yourRating: number;
    theirRating: number;
  } | null;
}

export interface CardActions {
  onOpenChat: (chatId: string, username: string) => void;
  onAcceptRequest: (requestId: string, username: string) => void;
  onRejectRequest: (requestId: string, username: string) => void;
}

@Component({
  selector: 'app-contact-user-card',
  standalone: true,
  imports: [
    CommonModule,
    AvatarComponent,
    StarRating
  ],
  templateUrl: './contact-user-card.html',
  styleUrl: './contact-user-card.scss'
})
export class ContactUserCard {
  private socialStore = inject(SocialStore);
  
  // 📥 Inputs modernos
  userData = input<ContactUserData | null>(null);  // Opcional para 'empty-result'
  cardType = input.required<CardType>();
  
  // 📤 Outputs modernos
  chatClick = output<{chatId: string, username: string}>();
  acceptClick = output<{requestId: string, username: string}>();
  rejectClick = output<{requestId: string, username: string}>();
  
  // Para notificar que necesita nueva sugerencia
  requestNewSuggestion = output<void>();

  onChatClick(): void {
    const userData = this.userData();
    this.chatClick.emit({
      chatId: userData.contactUser.chatId ?? '',
      username: userData.contactUser.user.username
    });
  }

  onAcceptClick(): void {
    const userData = this.userData();
    
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

  /**
   * 📤 Enviar solicitud de amistad (para suggestions)
   */
  onSendRequestClick(): void {
    const userData = this.userData();
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
      return `hace ${diffDays} día${diffDays > 1 ? 's' : ''}`;
    } else if (diffHours > 0) {
      return `hace ${diffHours} hora${diffHours > 1 ? 's' : ''}`;
    } else if (diffMinutes > 0) {
      return `hace ${diffMinutes} minuto${diffMinutes > 1 ? 's' : ''}`;
    } else {
      return 'ahora';
    }
  }
}