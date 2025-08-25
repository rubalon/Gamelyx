// src/app/features/home/components/friend-request/friend-request.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { SocialStore } from '@core/stores/social-store';
import { AvatarComponent } from '@shared/components/avatar/avatar';

type RequestType = 'incoming' | 'outgoing';

@Component({
  selector: 'app-friend-request',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    AvatarComponent  // 👈 Importamos nuestro Avatar compartido
  ],
  templateUrl: './friend-request.html',
  styleUrl: './friend-request.scss'
})
export class FriendRequestComponent {
  private socialStore = inject(SocialStore);

  // 🎯 Estado del componente
  requestType = signal<RequestType>('incoming');

  // 📊 Datos del store
  get incomingRequests() {
    return this.socialStore.pendingIncomingRequests();
  }

  get outgoingRequests() {
    return this.socialStore.pendingOutgoingRequests();
  }

  get isLoading() {
    return this.socialStore.state().isLoadingHomeSocialData;
  }

  get currentRequests() {
    return this.requestType() === 'incoming' ? this.incomingRequests : this.outgoingRequests;
  }

  get totalIncoming() {
    return this.incomingRequests.length;
  }

  get totalOutgoing() {
    return this.outgoingRequests.length;
  }

  /**
   * ✅ Aceptar solicitud de amistad (sin funcionalidad)
   */
  onAcceptRequest(requestId: string, username: string): void {
    console.log('✅ Accept request from:', username, requestId);
    // TODO: Implementar funcionalidad
  }

  /**
   * ❌ Rechazar solicitud de amistad (sin funcionalidad)
   */
  onRejectRequest(requestId: string, username: string): void {
    console.log('❌ Reject request from:', username, requestId);
    // TODO: Implementar funcionalidad
  }

  /**
   * 🗑️ Cancelar solicitud enviada (sin funcionalidad)
   */
  onCancelRequest(requestId: string, username: string): void {
    console.log('🗑️ Cancel request to:', username, requestId);
    // TODO: Implementar funcionalidad
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