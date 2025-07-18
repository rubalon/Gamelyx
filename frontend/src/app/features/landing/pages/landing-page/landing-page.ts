import { Component , inject} from '@angular/core';
import { Header } from '../../../../shared/components/header/header';
import { HeroSection } from '../../components/hero-section/hero-section';
import { FeaturesSection } from '../../components/features-section/features-section';
import { AuthModal } from '@shared/components/auth-modal/auth-modal';
import { ModalService } from '@shared/services/modal';
import { AsyncPipe } from '@angular/common';



@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    Header,
    HeroSection,
    FeaturesSection,
    AuthModal,
    AsyncPipe
  ],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.scss'
})
export class LandingPage {
  protected modalService = inject(ModalService);
}