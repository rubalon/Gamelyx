// src/app/features/home/components/friend-finder/components/friend-finder-result/friend-finder-result.ts
import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { AvatarComponent } from '@shared/components/avatar/avatar';

export interface FriendSuggestion {
  user: {
    userId: string;
    username: string;
  };
  gameSlug: string;
  yourRating: number;
  theirRating: number;
}

@Component({
  selector: 'app-friend-finder-result',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    AvatarComponent  // 👈 Importamos nuestro Avatar
  ],
  templateUrl: './friend-finder-result.html',
  styleUrl: './friend-finder-result.scss'
})
export class FriendFinderResultComponent {
  // 📥 Inputs
  currentSuggestion = input<FriendSuggestion | null>(null);

  // 📤 Outputs
  sendRequest = output<{userId: string, username: string, gameSlug: string}>();
  rejectSuggestion = output<{userId: string, username: string, gameSlug: string}>();

  /**
   * 📤 Enviar solicitud a usuario sugerido
   */
  onSendRequest(): void {
    const suggestion = this.currentSuggestion();
    if (suggestion) {
      this.sendRequest.emit({
        userId: suggestion.user.userId,
        username: suggestion.user.username,
        gameSlug: suggestion.gameSlug
      });
    }
  }

  /**
   * ❌ Rechazar sugerencia
   */
  onReject(): void {
    const suggestion = this.currentSuggestion();
    if (suggestion) {
      this.rejectSuggestion.emit({
        userId: suggestion.user.userId,
        username: suggestion.user.username,
        gameSlug: suggestion.gameSlug
      });
    }
  }


}