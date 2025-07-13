import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Backend } from './services/backend'; // ← Importamos el servicio

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected title = 'Gamelyx';
  
  // ← Nuevas propiedades para mostrar respuestas del backend
  helloMessage = '';
  statusMessage = '';
  isLoading = false;

  // ← Inyectar el servicio Backend
  constructor(private backend: Backend) { }

  // ← Método para probar la conexión
  testBackendConnection() {
    this.isLoading = true;
    
    // Llamar al endpoint /api/hello
    this.backend.getHello().subscribe({
      next: (response) => {
        this.helloMessage = response;
        console.log('✅ Hello response:', response);
      },
      error: (error) => {
        console.error('❌ Error calling hello:', error);
        this.helloMessage = 'Error al conectar con /api/hello';
      }
    });

    // Llamar al endpoint /api/status
    this.backend.getStatus().subscribe({
      next: (response) => {
        this.statusMessage = response;
        console.log('✅ Status response:', response);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error calling status:', error);
        this.statusMessage = 'Error al conectar con /api/status';
        this.isLoading = false;
      }
    });
  }
}