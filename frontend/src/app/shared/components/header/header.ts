import { Component ,inject  } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { TranslationService } from '@core/services/translations';
import { ModalService } from '@shared/services/modal';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './header.html',
  styleUrl: './header.scss'
})
export class Header {
  
  private router = inject(Router);
  private translationService = inject(TranslationService);
  private modalService = inject(ModalService);

  onLoginClick(): void {
    this.modalService.openAuthModal('login');
  }

  changeLanguage(event: any): void {
    const language = event.target.value;
    this.translationService.changeLanguage(language);
  }

  getCurrentLanguage(): string {
    return this.translationService.getCurrentLanguage();
  }
}