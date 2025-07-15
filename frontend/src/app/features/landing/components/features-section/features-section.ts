import { Component } from '@angular/core';

interface Feature {
  title: string;
  description: string;
  icon: string;
  comingSoon?: boolean;
  gradient: string;
}

@Component({
  selector: 'app-features-section',
  standalone: true,
  templateUrl: './features-section.html',
  styleUrl: './features-section.scss'
})
export class FeaturesSectionComponent {
  
  features: Feature[] = [
    {
      title: 'Game Reviews & Tracking',
      description: 'Mantén un registro completo de todos los juegos que has completado, los que estás jugando y tu lista de deseos. Comparte reseñas y descubre nuevos títulos.',
      icon: 'game-controller',
      gradient: 'from-green-400 to-emerald-600'
    },
    {
      title: 'Team Finding',
      description: 'Encuentra compañeros de equipo perfectos para rankear competitivamente o simplemente amigos para disfrutar de tus juegos favoritos en modo casual.',
      icon: 'users',
      gradient: 'from-blue-400 to-cyan-600'
    },
    {
      title: 'AI Matchmaking',
      description: 'Nuestro sistema de inteligencia artificial analiza tu estilo de juego, personalidad y preferencias para encontrarte los compañeros más compatibles.',
      icon: 'ai-chip',
      gradient: 'from-purple-400 to-pink-600',
      comingSoon: true
    }
  ];
}