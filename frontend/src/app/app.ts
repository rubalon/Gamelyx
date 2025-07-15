import { Component } from '@angular/core';
import { LandingPage } from './features/landing/pages/landing-page/landing-page';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [LandingPage],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  title = 'gamelyx-frontend';
}