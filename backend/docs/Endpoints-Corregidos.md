# Endpoints Corregidos

Este documento contiene la información corregida de los endpoints que tenían errores o información faltante en la documentación original.

---

## 1. Buscar Juegos

### Descripción
Busca videojuegos por nombre o término y devuelve resultados paginados con información básica de cada juego.

### URL
`/api/games/search`

### Método
`GET`

### Auth
No (Sin autenticación requerida)

### Parámetros de entrada (Query Parameters)
- `q` (string) **requerido** - Término de búsqueda
- `page` (integer, default: 1) - Número de página
- `size` (integer, default: 20) - Tamaño de la página

### Parámetros de salida (JSON)
```json
{
  "games": "[List<GameSearchItem>]",  // Lista de juegos encontrados
  "currentPage": "integer",            // Página actual
  "totalPages": "integer",             // Total de páginas disponibles
  "totalResults": "long",              // Total de resultados encontrados
  "hasMore": "boolean",                // Indica si hay más páginas
  "searchQuery": "string"              // Término de búsqueda utilizado
}
```

**Estructura de GameSearchItem:**
```json
{
  "rawgId": "integer",          // ID del juego en la API externa
  "slug": "string",             // Identificador textual del juego
  "name": "string",             // Nombre del juego
  "coverImage": "string",       // URL imagen de portada
  "rating": "double",           // Rating promedio (RAWG)
  "released": "string",         // Fecha de lanzamiento (formato: "yyyy-MM-dd")
  "platforms": "[List<string>]", // Lista de plataformas
  "genres": "[List<string>]"    // Lista de géneros
}
```

### Códigos de estado
- `200` - Búsqueda exitosa
- `500` - Error interno del servidor

---

## 2. Actualizar Estado Personal del Juego

### Descripción
Permite al usuario crear o actualizar su estado personal para un juego, incluyendo rating (1-10), una reseña textual y un estado (ej. WISHLIST, PLAYING, COMPLETED).

### URL
`/api/games/game/{identifier}/my-game-details`

### Método
`PUT`

### Auth
Sí (Bearer Token)

### Parámetros de entrada (Path Parameters)
- `identifier` (string) **requerido** - ID o slug del juego

### Parámetros de entrada (Request Body - JSON)
```json
{
  "status": "string",      // Estado del juego: "WISHLIST", "PLAYING", "COMPLETED", "ARCHIVED"
  "rating": "integer",     // Rating de 1 a 10
  "reviewText": "string"   // Texto de la reseña
}
```

### Parámetros de salida (JSON)
```json
{
  "status": "string",              // Estado actualizado
  "rating": "integer",             // Rating actualizado (1-10)
  "reviewText": "string",          // Texto de reseña actualizado
  "updatedAt": "datetime",         // Marca de tiempo de la actualización (formato: "yyyy-MM-dd'T'HH:mm:ss")
  "communityRating": "double",     // Rating promedio de la comunidad actualizado
  "totalReviews": "integer"        // Conteo total de reseñas en la comunidad
}
```

### Códigos de estado
- `200` - Review actualizada exitosamente
- `400` - Datos inválidos o error en la actualización
- `401` - Usuario no autenticado

---

## 3. Responder a Solicitud de Amistad

### Descripción
Permite al usuario destinatario aceptar o rechazar una solicitud de amistad pendiente. Si se acepta, se crea la relación de amistad bidireccional.

### URL
`/api/social/friend-requests/{requestId}/respond`

### Método
`PUT`

### Auth
Sí (Bearer Token)

### Parámetros de entrada (Path Parameters)
- `requestId` (string) **requerido** - ID de la solicitud de amistad a responder

### Parámetros de entrada (Query Parameters)
- `action` (enum) **requerido** - Acción a realizar: `ACCEPT` o `REJECT`

### Parámetros de salida (JSON)
```json
{
  "success": "boolean",        // Indicador de éxito de la operación
  "newFriend": "ContactUserDto" // DTO del nuevo amigo (null si se rechaza)
}
```

**Estructura de ContactUserDto:**
```json
{
  "user": {
    "userId": "UUID",        // UUID del usuario
    "username": "string"     // Nombre de usuario
  },
  "newMessages": "boolean"   // Indica si hay nuevos mensajes
}
```

### Códigos de estado
- `200` - Solicitud respondida exitosamente
- `400` - Acción inválida
- `401` - Usuario no autenticado
- `404` - Solicitud no encontrada

---

## 4. Eliminar Amigo

### Descripción
Elimina a un usuario de la lista de amigos. La eliminación es bidireccional (ambos usuarios dejan de ser amigos) y también elimina las solicitudes de amistad relacionadas si existen.

### URL
`/api/social/friends/{friendId}`

### Método
`DELETE`

### Auth
Sí (Bearer Token)

### Parámetros de entrada (Path Parameters)
- `friendId` (UUID) **requerido** - UUID del amigo a eliminar

### Parámetros de salida (JSON)
```json
{
  "success": "boolean",              // Indicador de éxito de la operación
  "deletedFriendUsername": "string", // Nombre de usuario del amigo eliminado
  "deletedFriendId": "UUID"          // UUID del amigo eliminado
}
```

### Códigos de estado
- `200` - Amigo eliminado exitosamente
- `400` - Error al eliminar amigo
- `401` - Usuario no autenticado
- `404` - Amigo no encontrado

---

## Notas adicionales

### Formato de fechas
Todas las fechas y timestamps en las respuestas utilizan el formato ISO 8601:
- Fechas: `yyyy-MM-dd` (ejemplo: "2024-01-15")
- Timestamps: `yyyy-MM-dd'T'HH:mm:ss` (ejemplo: "2024-01-15T14:30:00")

### Autenticación
Los endpoints que requieren autenticación deben incluir el header:
```
Authorization: Bearer <token>
```

### Tipos de datos
- `string` - Cadena de texto
- `integer` - Número entero
- `double` - Número decimal de doble precisión
- `long` - Número entero largo
- `boolean` - Valor booleano (true/false)
- `datetime` - Fecha y hora en formato ISO 8601
- `date` - Fecha en formato ISO 8601 (solo fecha)
- `UUID` - Identificador único universal
- `[List<T>]` - Lista de elementos del tipo T
