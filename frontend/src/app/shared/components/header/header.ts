import { Component ,inject  } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { TranslationService } from '@core/services/translations';

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

  onLoginClick(): void {
    this.router.navigate(['/home']);
  }

  changeLanguage(event: any): void {
    const language = event.target.value;
    this.translationService.changeLanguage(language);
  }

  getCurrentLanguage(): string {
    return this.translationService.getCurrentLanguage();
  }
}