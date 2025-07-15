import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Feature {
  id: string; 
  title: string;
  description: string;
  icon: string;
  comingSoon: boolean;
  gradientBg: string;
  gradientHover: string;
  badgeStyle: string;
}

@Component({
  selector: 'app-features-section',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './features-section.html',
  styleUrl: './features-section.scss'
})
export class FeaturesSection {
  
  features: Feature[] = [
    {
      id: 'game-reviews',  // ← ID único que nunca cambia
      title: 'Game Reviews & Tracking',
      description: 'Mantén un registro completo de todos los juegos que has completado, los que estás jugando y tu lista de deseos. Comparte reseñas y descubre nuevos títulos.',
      icon: 'game-controller',
      gradientBg: 'bg-gradient-to-br from-green-400 to-emerald-600',
      gradientHover: 'bg-gradient-to-br from-green-400/10 to-emerald-600/10',
      badgeStyle: 'bg-green-900/50 text-green-300 border-green-700',
      comingSoon: false
    },
    {
      id: 'team-finding',  // ← ID único que nunca cambia
      title: 'Team Finding',
      description: 'Encuentra compañeros de equipo perfectos para rankear competitivamente o simplemente amigos para disfrutar de tus juegos favoritos en modo casual.',
      icon: 'users',
      gradientBg: 'bg-gradient-to-br from-blue-400 to-cyan-600',
      gradientHover: 'bg-gradient-to-br from-blue-400/10 to-cyan-600/10',
      badgeStyle: 'bg-blue-900/50 text-blue-300 border-blue-700',
      comingSoon: false
    },
    {
      id: 'ai-matchmaking',  // ← ID único que nunca cambia
      title: 'AI Matchmaking',
      description: 'Nuestro sistema de inteligencia artificial analiza tu estilo de juego, personalidad y preferencias para encontrarte los compañeros más compatibles.',
      icon: 'ai-chip',
      gradientBg: 'bg-gradient-to-br from-purple-400 to-pink-600',
      gradientHover: 'bg-gradient-to-br from-purple-400/10 to-pink-600/10',
      badgeStyle: 'bg-purple-900/50 text-purple-300 border-purple-700',
      comingSoon: true
    }
  ];
}