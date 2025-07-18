import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ModalService {

  // Estado del modal de autenticación
  private authModalVisible = new BehaviorSubject<boolean>(false);
  private authModalTab = new BehaviorSubject<'login' | 'register'>('login');

  // Observables públicos
  authModalVisible$ = this.authModalVisible.asObservable();
  authModalTab$ = this.authModalTab.asObservable();

  constructor() { }

  // Abrir modal con pestaña específica
  openAuthModal(tab: 'login' | 'register' = 'login'): void {
    this.authModalTab.next(tab);
    this.authModalVisible.next(true);
  }

  // Cerrar modal
  closeAuthModal(): void {
    this.authModalVisible.next(false);
  }

  // Cambiar pestaña
  setAuthModalTab(tab: 'login' | 'register'): void {
    this.authModalTab.next(tab);
  }

}