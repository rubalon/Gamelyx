import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-confirmation-modal',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './confirmation-modal.html'
})
export class ConfirmationModalComponent {
  // Inputs para personalizar el modal
  title = input<string>('Confirmar acción');
  message = input<string>('¿Estás seguro de que quieres realizar esta acción?');
  confirmText = input<string>('Confirmar');
  cancelText = input<string>('Cancelar');

  // Outputs para las acciones
  confirm = output<void>();
  cancel = output<void>();

  onConfirm(): void {
    this.confirm.emit();
  }

  onCancel(): void {
    this.cancel.emit();
  }
}