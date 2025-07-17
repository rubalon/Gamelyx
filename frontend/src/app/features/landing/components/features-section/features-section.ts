import { Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';

interface Feature {
  id: string;
  titleKey: string; 
  descriptionKey: string;
  icon: string;
  comingSoon: boolean;
  gradientBg: string;
  gradientHover: string;
  badgeStyle: string;
}

@Component({
  selector: 'app-features-section',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './features-section.html',
  styleUrl: './features-section.scss'
})
export class FeaturesSection {
  
  features: Feature[] = [
    {
      id: 'game-reviews',
      titleKey: 'features.gameReviews.title',
      descriptionKey: 'features.gameReviews.description',
      icon: 'game-controller',
      gradientBg: 'bg-gradient-to-br from-green-400 to-emerald-600',
      gradientHover: 'bg-gradient-to-br from-green-400/10 to-emerald-600/10',
      badgeStyle: 'bg-green-900/50 text-green-300 border-green-700',
      comingSoon: false
    },
    {
      id: 'team-finding',
      titleKey: 'features.teamFinding.title',
      descriptionKey: 'features.teamFinding.description',
      icon: 'users',
      gradientBg: 'bg-gradient-to-br from-blue-400 to-cyan-600',
      gradientHover: 'bg-gradient-to-br from-blue-400/10 to-cyan-600/10',
      badgeStyle: 'bg-blue-900/50 text-blue-300 border-blue-700',
      comingSoon: false
    },
    {
      id: 'ai-matchmaking',
      titleKey: 'features.aiMatchmaking.title',
      descriptionKey: 'features.aiMatchmaking.description',
      icon: 'ai-chip',
      gradientBg: 'bg-gradient-to-br from-purple-400 to-pink-600',
      gradientHover: 'bg-gradient-to-br from-purple-400/10 to-pink-600/10',
      badgeStyle: 'bg-purple-900/50 text-purple-300 border-purple-700',
      comingSoon: true
    }
  ];
}