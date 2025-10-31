# Arquitectura del Core - Cliente Frontend

Este documento describe técnicamente las clases y componentes del módulo core de la aplicación cliente, organizado en cuatro categorías principales: Guards, Interceptors, Services y Stores.

---

## Guards

Los guards son funciones que controlan el acceso a las rutas de la aplicación mediante el sistema de routing de Angular.

### canActivateAuth

**Ubicación:** [src/app/core/guards/auth-guard.ts](src/app/core/guards/auth-guard.ts)

Guard funcional que protege rutas que requieren autenticación. Consulta el estado de autenticación mediante el AuthStore y verifica si existe un usuario autenticado. En caso negativo, redirige al usuario a la página de landing y deniega el acceso a la ruta solicitada.

### canActivateGuest

**Ubicación:** [src/app/core/guards/auth-guard.ts](src/app/core/guards/auth-guard.ts)

Guard funcional inverso que protege rutas de acceso público (como landing o login). Verifica si el usuario ya está autenticado mediante el AuthStore. Si lo está, redirige automáticamente a la página home para evitar que usuarios logueados accedan a páginas de invitados.

---

## Interceptors

Los interceptors son funciones que interceptan y modifican las peticiones HTTP de forma global antes de que sean enviadas.

### jwtInterceptor

**Ubicación:** [src/app/core/interceptors/jwt-interceptor.ts](src/app/core/interceptors/jwt-interceptor.ts)

Interceptor funcional HTTP que implementa autenticación mediante JWT (JSON Web Token). Intercepta todas las peticiones HTTP salientes, obtiene el token de acceso del AuthStore y lo adjunta automáticamente en el header Authorization con formato Bearer. Incluye lógica de filtrado para determinar qué peticiones requieren el token: aplica el JWT a endpoints de API privados mientras excluye endpoints de autenticación y recursos externos.

---

## Services

Los servicios encapsulan la lógica de negocio, comunicación con APIs externas y funcionalidades reutilizables.

### ChatApi

**Ubicación:** [src/app/core/services/chat-api.ts](src/app/core/services/chat-api.ts)

Servicio HTTP que gestiona las operaciones de carga de mensajes mediante peticiones REST. Proporciona métodos para obtener el historial de conversaciones con paginación, enviando parámetros de usuario objetivo, página y límite de resultados. Las respuestas incluyen metadatos de paginación como páginas totales y disponibilidad de más mensajes.

### ChatWebSocket

**Ubicación:** [src/app/core/services/chat-websocket.ts](src/app/core/services/chat-websocket.ts)

Cliente WebSocket basado en protocolo STOMP que gestiona mensajería en tiempo real. Establece conexión autenticada mediante token JWT incluido en query string y headers STOMP, con configuración de heartbeat y reconexión automática. Implementa suscripción a canales personales por usuario, emisión de mensajes a destinatarios específicos y envío de confirmaciones de lectura. Mantiene signals reactivos de estado de conexión y expone callbacks para manejo de eventos de mensajes nuevos y recibos de lectura.

### GameApi

**Ubicación:** [src/app/core/services/game-api.ts](src/app/core/services/game-api.ts)

Servicio HTTP que encapsula toda la comunicación con el backend de juegos. Proporciona métodos para búsqueda de juegos con paginación, obtención de detalles completos de juegos individuales mediante slug o ID, actualización de reviews y estados personales del usuario, y consulta de reviews propias con opciones de paginación y límites. Define interfaces TypeScript fuertemente tipadas para todas las respuestas, incluyendo detalles de juegos, reviews, estados de usuario y metadatos de paginación.

### GoogleAuthService

**Ubicación:** [src/app/core/services/google-auth.ts](src/app/core/services/google-auth.ts)

Servicio que integra Google Identity Services para autenticación OAuth2. Gestiona la carga e inicialización del SDK de Google, declarando tipos globales para la API. Implementa flujo de autorización mediante token client que fuerza selección de cuenta y solicita scopes básicos de perfil. Tras la autorización, obtiene información del usuario desde la API de Google y decodifica tokens JWT para extraer datos de perfil. Maneja callbacks de éxito y error del proceso de autenticación OAuth.

### SocialApiService

**Ubicación:** [src/app/core/services/social-api.ts](src/app/core/services/social-api.ts)

Servicio HTTP que centraliza todas las operaciones sociales de la aplicación. Proporciona endpoints para carga de datos sociales del home (amigos, solicitudes entrantes/salientes, juegos preferidos), gestión completa del ciclo de vida de solicitudes de amistad (envío, respuesta, marcado de notificaciones), búsqueda de usuarios con filtros, obtención de sugerencias de amigos basadas en juegos compartidos, y eliminación de amistades. Define interfaces TypeScript exhaustivas para DTOs de usuarios, solicitudes, juegos compartidos y respuestas, manteniendo correspondencia con los modelos del backend.

### TranslationService

**Ubicación:** [src/app/core/services/translations.ts](src/app/core/services/translations.ts)

Servicio de internacionalización basado en ngx-translate que gestiona múltiples idiomas en la aplicación. Configura idiomas disponibles (español e inglés) y establece español como idioma por defecto. Implementa persistencia de preferencia de idioma mediante localStorage, detectando y restaurando la selección del usuario entre sesiones. Expone métodos para cambio dinámico de idioma, consulta del idioma actual y listado de idiomas disponibles.

---

## Stores

Los stores son servicios que implementan gestión de estado reactivo centralizado mediante signals de Angular.

### AuthStore

**Ubicación:** [src/app/core/stores/auth-store.ts](src/app/core/stores/auth-store.ts)

Store centralizado que gestiona todo el estado de autenticación de la aplicación mediante signals reactivos. Mantiene estado de usuario, token, estados de carga y errores. Proporciona métodos para login tradicional (username/email + password), registro con verificación de email, autenticación unificada con Google (que detecta automáticamente si es login o registro), y logout con limpieza de estado. Implementa persistencia de sesión mediante localStorage para tokens de acceso y refresh, y datos de usuario. Expone signals computados para acceso reactivo al estado de autenticación, usuario actual y errores. Gestiona verificación de email mediante tokens y limpieza de estados temporales.

### ChatStore

**Ubicación:** [src/app/core/stores/chat-store.ts](src/app/core/stores/chat-store.ts)

Store reactivo que gestiona el estado de mensajería mediante arquitectura híbrida HTTP/WebSocket. Mantiene una única conversación activa en memoria, recargándola completamente desde HTTP cada vez que cambia. Coordina conexión automática del WebSocket mediante effects reactivos que observan cambios en el estado de autenticación. Implementa recepción de mensajes en tiempo real: si el mensaje pertenece a la conversación activa, actualiza el estado local inmediatamente; si es de otra conversación, notifica al SocialStore para marcar como no leído. Gestiona envío de mensajes mediante WebSocket, marcado de mensajes como leídos, y procesamiento de confirmaciones de lectura para actualizar estado visual. Expone signals readonly de conversación activa, mensajes, estado de conexión WebSocket y errores.

### SocialStore

**Ubicación:** [src/app/core/stores/social-store.ts](src/app/core/stores/social-store.ts)

Store centralizado que gestiona todo el estado social de la aplicación con optimización de actualizaciones locales. Mantiene listas reactivas de amigos, solicitudes de amistad entrantes/salientes, juegos preferidos, resultados de búsqueda y sugerencias de amigos. Implementa carga inicial de datos sociales desde el backend mediante endpoint único. Optimiza operaciones para evitar refetching innecesario: al enviar solicitud de amistad, agrega la nueva solicitud saliente localmente usando la respuesta del backend; al aceptar/rechazar solicitudes, actualiza listas de amigos y solicitudes localmente; al eliminar amigos, actualiza la lista local inmediatamente. Gestiona búsqueda de usuarios con filtros, obtención de sugerencias de amigos basadas en juegos compartidos con ratings similares, y actualización de estado de mensajes nuevos desde ChatStore. Expone signals computados para estadísticas (total de amigos, solicitudes pendientes) y método selector dinámico para acceso granular al estado.

---
