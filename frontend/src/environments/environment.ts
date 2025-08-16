// src/environments/environment.ts
const getApiUrl = () => {
  const hostname = window.location.hostname;
  
  // Para desarrollo local
  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return 'http://localhost:8080/api';
  }
  
  // Para red local (tu IP)
  if (hostname.startsWith('192.168')) {
    return `http://${hostname}:8080/api`;
  }
  
  // Para dev tunnels, usar el mismo host pero puerto 8080
  if (hostname.includes('devtunnels.ms')) {
    // Reemplazar el puerto en la URL del túnel
    return `https://${hostname.replace('-4200', '-8080')}/api`;
  }
  
  // Fallback
  return 'http://localhost:8080/api';
};

export const environment = {
  production: false,
  apiUrl: getApiUrl(), // 👈 Se adapta automáticamente a cualquier entorno
  version: '1.0.0',
  features: {
    enableGoogleAuth: false,
    enableEmailVerification: true
  }
};