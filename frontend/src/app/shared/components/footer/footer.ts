// src/app/shared/components/footer/footer.ts
import { Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './footer.html',
  styleUrl: './footer.scss'
})
export class FooterComponent {

  /**
   * Obtiene el año actual para el copyright
   */
  getCurrentYear(): number {
    return new Date().getFullYear();
  }
}