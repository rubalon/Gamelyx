import { Component } from '@angular/core';
import { Header } from '../../../../shared/components/header/header';
import { HeroSection } from '../../components/hero-section/hero-section';
import { FeaturesSection } from '../../components/features-section/features-section';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    Header,
    HeroSection,
    FeaturesSection
  ],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.scss'
})
export class LandingPage {

}