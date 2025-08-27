import { Component, inject } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { ModalService } from '@shared/services/modal';

@Component({
  selector: 'app-hero-section',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './hero-section.html',
  styleUrl: './hero-section.scss'
})
export class HeroSection {

  protected modalService = inject(ModalService);

  onStartClick(): void {
    this.modalService.openAuthModal('register'); // Abre en registro
  }

  onLearnMoreClick(): void {
    const featuresSection = document.getElementById('features-section');
    if (featuresSection) {
      featuresSection.scrollIntoView({ 
        behavior: 'smooth',
        block: 'start'
      });
    }
  }

}