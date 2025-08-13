// src/app/features/games/pages/game-page/components/review-modal/review-modal.ts
import { Component, inject, signal, computed, input, output, effect, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GameApiService, GameUserStatus, UpdateReviewRequest, UpdateReviewResponse } from '@core/services/game-api';
import { AuthStore } from '@core/stores/auth-store';
import { finalize, catchError, of } from 'rxjs';

@Component({
  selector: 'app-review-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './review-modal.html',
  styleUrl: './review-modal.scss'
})
export class ReviewModal implements OnInit {
  // 📡 Inputs/Outputs
  isOpen = input.required<boolean>();
  gameIdentifier = input.required<string>();
  existingReview = input<GameUserStatus | null>(null);
  modalClosed = output<void>();
  reviewSubmitted = output<UpdateReviewResponse>(); // 🆕 CAMBIO: Ahora envía datos

  // 🏪 Dependencies
  private fb = inject(FormBuilder);
  private gameApiService = inject(GameApiService);
  private authStore = inject(AuthStore);

  // 🎯 Component state
  reviewForm!: FormGroup;
  isSubmitting = signal(false);
  submitError = signal<string | null>(null);
  selectedRating = signal(0);
  hoverRating = signal(0);

  // 💫 Computed values
  currentUser = computed(() => this.authStore.user());
  isEditMode = computed(() => this.existingReview() !== null);
  modalTitle = computed(() => 
    this.isEditMode() ? 'Editar tu review' : 'Escribir review'
  );

  ngOnInit(): void {
    this.initializeForm();
    
    // 🔄 Effect para resetear form cuando se abre/cierra o cambia review
    effect(() => {
      if (this.isOpen()) {
        this.setupFormForCurrentReview();
      } else {
        this.resetForm();
      }
    });
  }

  /**
   * 🏗️ Inicializar formulario reactivo
   */
  private initializeForm(): void {
    this.reviewForm = this.fb.group({
      reviewText: ['', [
        Validators.maxLength(1000)
      ]],
      rating: [0, [
        Validators.min(1),
        Validators.max(10)
      ]]
    });
  }

  /**
   * ⚙️ Configurar formulario según review existente o nuevo
   */
  private setupFormForCurrentReview(): void {
    const review = this.existingReview();
    
    if (review) {
      // Modo edición - precargar datos
      this.reviewForm.patchValue({
        reviewText: review.reviewText || '',
        rating: review.rating || 0
      });
      this.selectedRating.set(review.rating || 0);
    } else {
      // Modo nuevo - formulario limpio
      this.resetForm();
    }
    
    this.submitError.set(null);
  }

  /**
   * 🧹 Resetear formulario
   */
  private resetForm(): void {
    this.reviewForm.reset({
      reviewText: '',
      rating: 0
    });
    this.selectedRating.set(0);
    this.hoverRating.set(0);
    this.submitError.set(null);
  }

  /**
   * ⭐ Manejar click en estrella de rating
   */
  onStarClick(rating: number): void {
    this.selectedRating.set(rating);
    this.reviewForm.patchValue({ rating });
    this.hoverRating.set(0);
  }

  /**
   * 🌟 Manejar hover en estrella
   */
  onStarHover(rating: number): void {
    this.hoverRating.set(rating);
  }

  /**
   * 👋 Manejar mouse leave del área de estrellas
   */
  onStarsLeave(): void {
    this.hoverRating.set(0);
  }

  /**
   * 🎨 Obtener clases CSS para estrella
   */
  getStarClasses(index: number): string {
    const baseClasses = 'h-8 w-8 cursor-pointer transition-all duration-200';
    const currentRating = this.hoverRating() || this.selectedRating();
    
    if (index <= currentRating) {
      return `${baseClasses} text-yellow-400 hover:text-yellow-300 transform hover:scale-110`;
    }
    
    return `${baseClasses} text-gray-600 hover:text-yellow-400 transform hover:scale-110`;
  }

  /**
   * 💾 Enviar review al backend
   */
  onSubmit(): void {
    if (this.reviewForm.invalid || this.isSubmitting()) return;

    const formValue = this.reviewForm.value;
    
    // Validar que al menos hay texto o rating
    if (!formValue.reviewText?.trim() && !formValue.rating) {
      this.submitError.set('Debes escribir un comentario o dar una puntuación');
      return;
    }

    this.isSubmitting.set(true);
    this.submitError.set(null);

    // Preparar datos para el backend
    const reviewData: UpdateReviewRequest = {
      reviewText: formValue.reviewText?.trim() || '',
      rating: formValue.rating || 0
    };

    this.gameApiService.updateMyReview(this.gameIdentifier(), reviewData)
      .pipe(
        catchError(error => {
          console.error('Error submitting review:', error);
          
          if (error.status === 401) {
            this.submitError.set('Tu sesión ha expirado. Por favor, inicia sesión de nuevo.');
          } else if (error.status === 404) {
            this.submitError.set('El juego no fue encontrado.');
          } else if (error.status >= 500) {
            this.submitError.set('Error del servidor. Por favor, intenta de nuevo más tarde.');
          } else {
            this.submitError.set('Error al enviar la review. Por favor, intenta de nuevo.');
          }
          
          return of(null);
        }),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe(response => {
        if (response) {
          // 🆕 CAMBIO: Emitir los datos del backend en lugar de void
          this.reviewSubmitted.emit(response);
        }
      });
  }

  /**
   * ❌ Cerrar modal
   */
  onClose(): void {
    if (this.isSubmitting()) return; // No cerrar si está enviando
    
    this.modalClosed.emit();
  }

  /**
   * 🎯 Manejar click en backdrop
   */
  onBackdropClick(event: Event): void {
    if (event.target === event.currentTarget) {
      this.onClose();
    }
  }

  /**
   * ⌨️ Manejar tecla ESC
   */
  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.onClose();
    }
  }

  /**
   * 📝 Obtener contador de caracteres para textarea
   */
  getCharacterCount(): string {
    const current = this.reviewForm.get('reviewText')?.value?.length || 0;
    const max = 1000;
    return `${current}/${max}`;
  }

  /**
   * 🎨 Obtener clases para contador de caracteres
   */
  getCharacterCountClasses(): string {
    const current = this.reviewForm.get('reviewText')?.value?.length || 0;
    const max = 1000;
    const percentage = current / max;
    
    if (percentage >= 0.9) return 'text-red-400';
    if (percentage >= 0.7) return 'text-yellow-400';
    return 'text-gray-500';
  }

  /**
   * 📏 Verificar si el formulario es válido para envío
   */
  get canSubmit(): boolean {
    const formValue = this.reviewForm.value;
    const hasContent = formValue.reviewText?.trim() || formValue.rating > 0;
    return hasContent && !this.isSubmitting() && this.reviewForm.valid;
  }

  /**
   * 🎯 Obtener mensaje descriptivo del rating
   */
  getRatingLabel(rating: number): string {
    const labels: Record<number, string> = {
      1: 'Terrible',
      2: 'Muy malo',
      3: 'Malo', 
      4: 'Flojo',
      5: 'Regular',
      6: 'Decente',
      7: 'Bueno',
      8: 'Muy bueno',
      9: 'Excelente',
      10: 'Obra maestra'
    };
    
    return labels[rating] || '';
  }

  /**
   * 🎨 Obtener color del texto del rating
   */
  getRatingColor(rating: number): string {
    if (rating >= 8) return 'text-green-400';
    if (rating >= 6) return 'text-yellow-400';
    if (rating >= 4) return 'text-orange-400';
    return 'text-red-400';
  }
}