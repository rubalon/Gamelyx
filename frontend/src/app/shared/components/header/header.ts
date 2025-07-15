import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './header.html',
  styleUrl: './header.scss'
})
export class Header {
  
  constructor(private router: Router) {}

  onLoginClick(): void {
    // Navegar a la página de home (temporalmente)
    this.router.navigate(['/home']);
  }
}