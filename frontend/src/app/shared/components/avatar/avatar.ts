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
      'from-orange-500 to-yellow-500',
      'from-amber-500 to-orange-600',
      'from-yellow-400 to-amber-600',
      
      // Verdes
      'from-green-400 to-emerald-800',
      'from-emerald-400 to-teal-800',
      'from-lime-500 to-green-800',
      
      // Azules y cianes
      'from-blue-400 to-indigo-800',
      'from-cyan-400 to-blue-800',
      'from-sky-500 to-blue-600',
      
      // Púrpuras y violetas
      'from-purple-500 to-indigo-600',
      'from-violet-500 to-purple-600',
      'from-blue-500 to-violet-700',
      'from-indigo-500 to-purple-600'
    ];
  
    // Usar el hash del username para seleccionar color consistente
    const hash = this.username().split('').reduce((a, b) => {
      a = ((a << 5) - a) + b.charCodeAt(0);
      return a & a;
    }, 0);
  
    return colors[Math.abs(hash) % colors.length];
  }
}