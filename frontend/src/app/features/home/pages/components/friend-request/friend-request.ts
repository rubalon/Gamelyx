// src/app/features/home/components/friend-request/friend-request.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { SocialStore } from '@core/stores/social-store';
import { ContactUserCard } from '@shared/components/contact-user-card/contact-user-card';

type RequestType = 'incoming' | 'outgoing';
type OutgoingStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

@Component({
  selector: 'app-friend-request',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    ContactUserCard  // 👈 Nueva card reutilizable
  ],
  templateUrl: './friend-request.html',
  styleUrl: './friend-request.scss'
})
export class FriendRequestComponent {
  private socialStore = inject(SocialStore);

  // 🎯 Estado del componente
  requestType = signal<RequestType>('incoming');
  outgoingStatus = signal<OutgoingStatus>('PENDING'); // 👈 Nuevo selector

  // 📊 Datos del store
  get incomingRequests() {
    return this.socialStore.incomingRequests();
  }

  get outgoingRequests() {
    // Filtrar por estado cuando estamos en "outgoing"
    const allOutgoing = this.socialStore.outgoingRequests();
    if (this.requestType() === 'outgoing') {
      return allOutgoing.filter(request => request.status === this.outgoingStatus());
    }
    return allOutgoing;
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
    return this.socialStore.outgoingRequests().length; // 👈 Total sin filtrar
  }

  /**
   * 💬 Abrir chat con usuario (delegado desde la card)
   */
  onOpenChat(chatId: string, username: string): void {
    console.log('💬 Opening chat with:', username, 'ChatId:', chatId);
    // TODO: Implementar navegación al chat
    // router.navigate(['/chat', chatId]);
  }

  /**
   * ✅ Aceptar solicitud de amistad (delegado desde la card)
   */
  onAcceptRequest(requestId: string, username: string): void {
    console.log('✅ Accept request from:', username, requestId);
    // TODO: Implementar funcionalidad
  }

  /**
   * ❌ Rechazar solicitud de amistad (delegado desde la card)
   */
  onRejectRequest(requestId: string, username: string): void {
    console.log('❌ Reject request from:', username, requestId);
    // TODO: Implementar funcionalidad
  }
}