// src/environments/environment.ts
const getApiUrl = () => {
  // Si estamos en localhost, usar localhost
  // Si estamos en otra IP, usar esa IP
  const hostname = window.location.hostname;
  return hostname === 'localhost' || hostname === '127.0.0.1' 
    ? 'http://localhost:8080/api'
    : `http://${hostname}:8080/api`;
};

export const environment = {
  production: false,
  apiUrl: getApiUrl(), // 👈 Se adapta automáticamente
  version: '1.0.0',
  features: {
    enableGoogleAuth: false,
    enableEmailVerification: true
  }
};