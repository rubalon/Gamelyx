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
      // Rojos y rosas
      'from-red-400 to-orange-700',
      'from-pink-300 to-rose-600', 
      'from-rose-400 to-pink-600',
      
      // Naranjas y amarillos
      'from-orange-500 to-yellow-500',
      'from-amber-500 to-orange-600',
      'from-yellow-400 to-amber-600',
      
      // Verdes
      'from-green-400 to-emerald-800',
      'from-emerald-400 to-teal-800',
      'from-lime-500 to-green-800',
      
      // Azules y cianes
      'from-blue-400 to-indigo-800',
      'from-cyan-400 to-blue-800',
      'from-sky-500 to-blue-600',
      
      // Púrpuras y violetas
      'from-purple-500 to-indigo-600',
      'from-violet-500 to-purple-600',
      'from-blue-500 to-violet-700',
      'from-indigo-500 to-purple-600'
    ];
  
    // Usar el hash del username para seleccionar color consistente
    const hash = username.split('').reduce((a, b) => {
      a = ((a << 5) - a) + b.charCodeAt(0);
      return a & a;
    }, 0);
  
    return colors[Math.abs(hash) % colors.length];
  }
}