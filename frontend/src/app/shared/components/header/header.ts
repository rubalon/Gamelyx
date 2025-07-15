import { Component } from '@angular/core';

@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './header.html',
  styleUrl: './header.scss'
})
export class HeaderComponent {
  
  onLoginClick(): void {
    // TODO: Implementar lógica de login más adelante
    console.log('Login clicked');
  }
}