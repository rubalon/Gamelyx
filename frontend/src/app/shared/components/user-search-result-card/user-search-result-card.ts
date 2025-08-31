// src/app/shared/components/user-search-result-card/user-search-result-card.ts
import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { AvatarComponent } from '@shared/components/avatar/avatar';
import { SearchedUserDto } from '@core/services/social-api';


@Component({
  selector: 'app-user-search-result-card',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    AvatarComponent
  ],
  templateUrl: './user-search-result-card.html',
  styleUrl: './user-search-result-card.scss'
})
export class UserSearchResultCard {
  // 📥 Inputs
  searchedUser = input.required<SearchedUserDto>();

  // 📤 Outputs
  sendFriendRequest = output<string>(); // Emite el userId

  /**
   * 📤 Enviar solicitud de amistad
   */
  onSendFriendRequest(): void {
    const user = this.searchedUser();
    this.sendFriendRequest.emit(user.user.userId);
  }

  /**
   * 🎯 Verificar si puede enviar solicitud
   */
  canSendRequest(): boolean {
    const user = this.searchedUser();
    return !user.isFriend && !user.hasPendingRequest && !user.hasRejectedRequest;
  }
}