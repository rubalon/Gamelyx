// src/environments/environment.ts (Development - Restaurado)
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api', // 👈 Vuelve a localhost para desarrollo
  version: '1.0.0',
  features: {
    enableGoogleAuth: false,
    enableEmailVerification: true
  }
};