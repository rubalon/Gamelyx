// src/app/shared/components/avatar/avatar.ts
import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-avatar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div 
      [class]="'bg-gradient-to-br rounded-full flex items-center justify-center text-white font-bold flex-shrink-0 ' + 
               avatarColor + ' ' + sizeClasses()"
      [title]="username()">
      {{ avatarInitial }}
    </div>
  `
})
export class AvatarComponent {
  username = input.required<string>();
  sizeClasses = input<string>('w-10 h-10 text-base'); // Default: tamaño md

  /**
   * 🎨 Generar avatar inicial
   */
  get avatarInitial(): string {
    return this.username() ? this.username().charAt(0).toUpperCase() : '?';
  }

  /**
   * 🌈 Generar color de avatar basado en username
   */
  get avatarColor(): string {
    const colors = [
      // Rojos y rosas
      'from-red-400 to-orange-700',
      'from-pink-300 to-rose-600', 
      'from-rose-400 to-pink-600',
      
      // Naranjas y amarillos
      'from-orange-300 to-red-700',
      'from-amber-400 to-orange-600',
      'from-yellow-300 to-amber-800',
      'from-yellow-200 to-amber-900',
      
      // Verdes
      'from-green-300 to-yellow-800',
      'from-emerald-400 to-blue-800',
      'from-lime-300 to-emerald-800',
      'from-green-400 to-red-400',
      
      // Azules y cianes
      'from-blue-400 to-indigo-800',
      'from-cyan-400 to-blue-800',
      'from-sky-500 to-blue-600',
      'from-sky-500 to-red-600',
      'from-sky-200 to-amber-600',
      
      // Púrpuras y violetas
      'from-purple-500 to-indigo-600',
      'from-indigo-400 to-green-600',
      'from-violet-500 to-purple-600',
      'from-violet-400 to-red-800',
      'from-blue-500 to-violet-700',
      'from-indigo-300 via-blue-500 to-purple-700'
    ];
  
    // Usar el hash del username para seleccionar color consistente
    const hash = this.username().split('').reduce((a, b) => {
      a = ((a << 5) - a) + b.charCodeAt(0);
      return a & a;
    }, 0);
  
    return colors[Math.abs(hash) % colors.length];
  }
}