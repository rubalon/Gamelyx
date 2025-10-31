# GameController - Endpoints de Juegos

## Introducción

El **GameController** (`/api/games`) gestiona todas las funcionalidades relacionadas con videojuegos. Se integra con APIs externas (RAWG Video Games Database y Steam Web API) para obtener información de juegos, y permite a los usuarios crear y gestionar sus propias reviews personales.

**Funcionalidades principales:**
- Búsqueda de videojuegos con paginación
- Visualización detallada de juegos (con datos personales si está autenticado)
- Gestión de reviews y estados personales (WISHLIST, PLAYING, COMPLETED, ARCHIVED)
- Listado de reviews del usuario (modo home y paginado)

---

## Endpoints

### `GET /api/games/search`

**Descripción:** Busca videojuegos por nombre o término. Endpoint público que devuelve resultados paginados con información básica de cada juego.

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada (Query params):**
- `q` (string, requerido): Término de búsqueda
- `page` (integer, opcional): Número de página (default: 1)
- `size` (integer, opcional): Tamaño de página (default: 20)

**Ejemplo:**
```
GET /api/games/search?q=Minecraft&page=1&size=20
```

**Parámetros de salida (JSON):**
```json
{
  "games": [
    {
      "rawgId": 12345,
      "slug": "minecraft",
      "name": "Minecraft",
      "coverImage": "https://...",
      "rating": 4.5,
      "released": "2011-11-18",
      "platforms": ["PC", "PlayStation"],
      "genres": ["Sandbox", "Survival"]
    }
  ],
  "currentPage": 1,
  "totalPages": 8,
  "totalResults": 150,
  "hasMore": true,
  "searchQuery": "Minecraft"
}
```

**Códigos de estado:**
- `200 OK`: Búsqueda exitosa
- `500 Internal Server Error`: Error en API externa

**DTO utilizado:**
- Response: `GameResponseDtos.GameSearchResultsDto`
  - Contiene lista de `GameSearchItem`

---

### `GET /api/games/game/{identifier}`

**Descripción:** Obtiene información completa de un juego específico. Si el usuario está autenticado, incluye su estado personal (rating, review, status). Si no, devuelve solo datos públicos.

**Método HTTP:** GET

**Autenticación:** Bearer Token (opcional)

**Parámetros de entrada (Path params):**
- `identifier` (string, requerido): ID numérico (rawgId) o slug del juego

**Ejemplo:**
```
GET /api/games/game/minecraft
GET /api/games/game/12345
```

**Parámetros de salida (JSON):**
```json
{
  "rawgId": 12345,
  "slug": "minecraft",
  "name": "Minecraft",
  "description": "HTML description...",
  "descriptionRaw": "Plain text description...",
  "backgroundImage": "https://...",
  "coverImage": "https://...",
  "screenshots": ["https://...", "https://..."],

  "rating": 4.5,
  "communityRating": 4.7,
  "totalCommunityReviews": 1500,
  "released": "2011-11-18",
  "website": "https://minecraft.net",
  "metacriticScore": 93,

  "platforms": ["PC", "PlayStation 4", "Xbox One"],
  "genres": ["Adventure", "Survival"],
  "developers": ["Mojang Studios"],
  "publishers": ["Microsoft Studios"],
  "tags": ["Open World", "Crafting"],

  "myStatus": {
    "status": "COMPLETED",
    "rating": 9,
    "reviewText": "El mejor juego de sandbox",
    "reviewUpdatedAt": "2024-01-15T10:30:00"
  },

  "recentReviews": [
    {
      "username": "usuario123",
      "rating": 10,
      "reviewText": "Increíble juego",
      "status": "PLAYING",
      "reviewCreatedAt": "2024-01-10T15:20:00"
    }
  ],

  "lastUpdated": "2024-01-15T10:30:00"
}
```

**Notas:**
- `myStatus`: Solo presente si el usuario está autenticado
- `recentReviews`: 2-3 reviews recientes de otros usuarios

**Códigos de estado:**
- `200 OK`: Juego encontrado
- `404 Not Found`: Juego no encontrado

**DTO utilizado:**
- Response: `GameResponseDtos.GamePageDto`
  - Incluye: `MyGameStatus`, `OtherUserReview`

---

### `PUT /api/games/game/{identifier}/my-game-details`

**Descripción:** Actualiza el estado personal del usuario para un juego: rating (1-10), review (texto), y estado (WISHLIST, PLAYING, COMPLETED, ARCHIVED).

**Método HTTP:** PUT

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada (Path params):**
- `identifier` (string, requerido): ID o slug del juego

**Parámetros de entrada (RequestBody - JSON):**
```json
{
  "status": "string",      // WISHLIST, PLAYING, COMPLETED (opcional)
  "rating": 9,             // 1-10 (opcional)
  "reviewText": "string"   // Texto de review (opcional)
}
```

**Notas:**
- Todos los campos son opcionales
- `status`: Valores válidos: "WISHLIST", "PLAYING", "COMPLETED", "ARCHIVED"
- `rating`: Entero entre 1 y 10

**Parámetros de salida (JSON):**
```json
{
  "status": "COMPLETED",
  "rating": 9,
  "reviewText": "El mejor juego de sandbox",
  "updatedAt": "2024-01-15T10:30:00",
  "communityRating": 4.7,
  "totalReviews": 1501
}
```

**Códigos de estado:**
- `200 OK`: Review actualizada exitosamente
- `400 Bad Request`: Datos inválidos o juego no encontrado
- `401 Unauthorized`: Usuario no autenticado

**DTO utilizado:**
- Request: `GameRequestDtos.UpdateMyGameRequest`
- Response: `GameResponseDtos.UpdatedGameStatusDto`

---

### `GET /api/games/my-reviews`

**Descripción:** Obtiene las reviews del usuario autenticado. Soporta dos modos:
- **Modo Home** (`limit`): Devuelve las últimas N reviews
- **Modo Completo** (`page` + `size`): Devuelve reviews paginadas

**Método HTTP:** GET

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada (Query params - Modo Home):**
- `limit` (integer, opcional): Número de reviews (default: 3)

**Parámetros de entrada (Query params - Modo Completo):**
- `page` (integer, opcional): Número de página (default: 0)
- `size` (integer, opcional): Reviews por página (default: 20)

**Ejemplos:**
```
GET /api/games/my-reviews?limit=5              // Modo home
GET /api/games/my-reviews?page=0&size=20       // Modo completo
```

**Parámetros de salida (JSON - Modo Home):**
```json
{
  "reviews": [
    {
      "gameRawgId": 12345,
      "gameSlug": "minecraft",
      "gameName": "Minecraft",
      "coverImage": "https://...",
      "rating": 9,
      "reviewText": "El mejor juego de sandbox",
      "status": "COMPLETED",
      "reviewCreatedAt": "2024-01-10T15:20:00",
      "reviewUpdatedAt": "2024-01-15T10:30:00"
    }
  ],
  "currentPage": null,
  "totalPages": null,
  "totalReviews": null,
  "hasMore": null
}
```

**Parámetros de salida (JSON - Modo Completo):**
```json
{
  "reviews": [...],
  "currentPage": 0,
  "totalPages": 3,
  "totalReviews": 45,
  "hasMore": true
}
```

**Notas:**
- En modo home, los campos de paginación son `null`
- Método `isForHome()` retorna `true` si `currentPage == null`

**Códigos de estado:**
- `200 OK`: Reviews obtenidas exitosamente
- `401 Unauthorized`: Usuario no autenticado
- `500 Internal Server Error`: Error interno

**DTO utilizado:**
- Response: `GameResponseDtos.MyReviewsResponseDto`
  - Contiene lista de `MyReviewDto`
  - Factory methods: `forHome()`, `forPage()`

---

### `GET /api/games/health`

**Descripción:** Health check del servicio de juegos.

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada:** Ninguno

**Parámetros de salida (JSON):**
```json
{
  "status": "UP",
  "controller": "GameController (REFACTORED - JWT Stateless)",
  "timestamp": 1234567890,
  "authentication": "Uses @AuthenticationPrincipal String username",
  "architecture": "GameService handles User entity lookup when needed",
  "endpoints": [
    "GET /search - Buscador home",
    "GET /game/{identifier} - Página completa del juego",
    "PUT /game/{identifier}/my-review - Mi review/estado",
    "GET /my-reviews - Mis reviews (home + paginación)"
  ]
}
```

**Códigos de estado:**
- `200 OK`: Servicio operativo

**DTO utilizado:** Ninguno (respuesta Map genérico)

---

## Resumen de DTOs

### DTOs de Request
- `UpdateMyGameRequest(status, rating, reviewText)` - Todos los campos opcionales

### DTOs de Response
- `GameSearchResultsDto` (clase con getters/setters)
  - `games`: List<GameSearchItem>
  - `currentPage`, `totalPages`, `totalResults`, `hasMore`, `searchQuery`

- `GameSearchItem(rawgId, slug, name, coverImage, rating, released, platforms, genres)`

- `GamePageDto` (clase con getters/setters)
  - Datos del juego: rawgId, slug, name, description, images, ratings, metadata
  - `myStatus`: MyGameStatus (si autenticado)
  - `recentReviews`: List<OtherUserReview>

- `MyGameStatus(status, rating, reviewText, reviewUpdatedAt)`

- `OtherUserReview(username, rating, reviewText, status, reviewCreatedAt)`

- `UpdatedGameStatusDto(status, rating, reviewText, updatedAt, communityRating, totalReviews)`

- `MyReviewsResponseDto` (clase con factory methods)
  - `reviews`: List<MyReviewDto>
  - `currentPage`, `totalPages`, `totalReviews`, `hasMore`
  - Factory methods: `forHome()`, `forPage()`

- `MyReviewDto(gameRawgId, gameSlug, gameName, coverImage, rating, reviewText, status, reviewCreatedAt, reviewUpdatedAt)`
