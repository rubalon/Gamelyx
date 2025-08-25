// src/app/features/home/components/friend-finder/components/friend-finder-result/friend-finder-result.ts
import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

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
    TranslateModule
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

  /**
   * 🎨 Generar avatar con iniciales
   */
  getAvatarInitial(username: string): string {
    return username.charAt(0).toUpperCase();
  }

  /**
   * 🌈 Generar color de avatar basado en username
   */
  getAvatarColor(username: string): string {
    const colors = [
      'from-purple-400 to-blue-500',
      'from-pink-400 to-purple-500', 
      'from-blue-400 to-cyan-500',
      'from-green-400 to-blue-500',
      'from-yellow-400 to-orange-500',
      'from-red-400 to-pink-500'
    ];
    
    const hash = username.split('').reduce((a, b) => {
      a = ((a << 5) - a) + b.charCodeAt(0);
      return a & a;
    }, 0);
    
    return colors[Math.abs(hash) % colors.length];
  }
}