import { Component } from '@angular/core';
import { HeaderComponent } from '../../../../shared/components/header/header';
import { HeroSectionComponent } from '../../components/hero-section/hero-section';
import { FeaturesSectionComponent } from '../../components/features-section/features-section';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    HeaderComponent,
    HeroSectionComponent,
    FeaturesSectionComponent
  ],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.scss'
})
export class LandingPageComponent {

}