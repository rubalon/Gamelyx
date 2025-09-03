export const environment = {
  production: true,
  apiUrl: 'https://api.gamelyx.es/api', // URL de producción
  wsUrl: 'wss://api.gamelyx.es/ws', // WebSocket URL para producción
  version: '1.0.0',
  features: {
    enableGoogleAuth: false,
    enableEmailVerification: true
  },
  google: {
    clientId: '662461007359-647kj5vf6430eakt2ke00ufimdmseksl.apps.googleusercontent.com' 
  }
};