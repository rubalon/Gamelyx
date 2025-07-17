import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root'
})
export class TranslationService {

  constructor(private translate: TranslateService) {
    this.initializeTranslation();
  }

  private initializeTranslation(): void {
    // Idiomas disponibles
    this.translate.addLangs(['es', 'en']);
    
    // Idioma por defecto
    this.translate.setDefaultLang('es');
    
    // Detectar idioma desde localStorage o usar español por defecto
    const savedLanguage = localStorage.getItem('language');
    const languageToUse = savedLanguage || 'es';
    
    this.translate.use(languageToUse);
  }

  changeLanguage(language: string): void {
    this.translate.use(language);
    localStorage.setItem('language', language);
  }

  getCurrentLanguage(): string {
    return this.translate.currentLang || 'es';
  }

  getAvailableLanguages(): string[] {
    return this.translate.getLangs();
  }
}