// src/app/features/home/components/friend-list/friend-list.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { SocialStore } from '@core/stores/social-store';

@Component({
  selector: 'app-friend-list',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule
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
    
    // Usar el hash del username para seleccionar color consistente
    const hash = username.split('').reduce((a, b) => {
      a = ((a << 5) - a) + b.charCodeAt(0);
      return a & a;
    }, 0);
    
    return colors[Math.abs(hash) % colors.length];
  }
}