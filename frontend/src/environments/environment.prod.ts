export const environment = {
  production: true,
  apiUrl: 'https://apitfg.gamelyx.es/api', // URL de producción
  wsUrl: 'wss://apitfg.gamelyx.es/ws', // WebSocket URL para producción
  version: '1.0.0',
  features: {
    enableGoogleAuth: true,
    enableEmailVerification: true
  },
  google: {
    clientId: '662461007359-647kj5vf6430eakt2ke00ufimdmseksl.apps.googleusercontent.com' 
  }
};