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

  /**
   * 🎯 Determinar el tipo de card según el contexto
   */
  getCardType(): 'incoming' | 'outgoing-pending' | 'outgoing-completed' {
    if (this.requestType() === 'incoming') {
      return 'incoming';
    }
    
    // Para outgoing, determinar si es pending o completed
    return this.outgoingStatus() === 'PENDING' ? 'outgoing-pending' : 'outgoing-completed';
  }

  get totalIncoming() {
    return this.incomingRequests.length;
  }

  get totalOutgoing() {
    return this.socialStore.outgoingRequests().length; // 👈 Total sin filtrar
  }

  // ✅ Toda la lógica de interacción ahora está en contact-user-card
  // No necesitamos métodos de delegación aquí
}