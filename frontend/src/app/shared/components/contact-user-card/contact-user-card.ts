import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AvatarComponent } from '@shared/components/avatar/avatar';
import { StarRating } from '@shared/components/star-rating/star-rating';

export type CardType = 'incoming' | 'outgoing';

export interface ContactUserData {
  requestId: string;
  contactUser: {
    user: {
      username: string;
    };
    chatId?: string | null;
    newMessages: boolean;
  };
  receivedAt: string;
  status: string;
  sharedGame?: {
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
  @Input({ required: true }) userData!: ContactUserData;
  @Input({ required: true }) cardType!: CardType;
  
  @Output() chatClick = new EventEmitter<{chatId: string, username: string}>();
  @Output() acceptClick = new EventEmitter<{requestId: string, username: string}>();
  @Output() rejectClick = new EventEmitter<{requestId: string, username: string}>();

  onChatClick(): void {
    this.chatClick.emit({
      chatId: this.userData.contactUser.chatId ?? '',
      username: this.userData.contactUser.user.username
    });
  }

  onAcceptClick(): void {
    this.acceptClick.emit({
      requestId: this.userData.requestId,
      username: this.userData.contactUser.user.username
    });
  }

  onRejectClick(): void {
    this.rejectClick.emit({
      requestId: this.userData.requestId,
      username: this.userData.contactUser.user.username
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