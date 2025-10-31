# GameTestController - Endpoints de Testing

## Introducción

⚠️ **NOTA:** Este es un **controlador temporal de testing** utilizado durante el desarrollo para probar la integración con la API externa RAWG (Video Games Database). No está destinado para uso en producción.

El **GameTestController** (`/api/test/games`) proporciona endpoints de prueba para verificar la correcta integración con la API de RAWG, que es la fuente principal de datos de videojuegos en Gamelyx.

**Funcionalidades principales:**
- Testing de búsqueda de juegos
- Testing de detalles de juegos específicos
- Testing de juegos populares y trending
- Testing de screenshots de juegos
- Health checks del servicio externo

---

## Endpoints

### `GET /api/test/games/health`

**Descripción:** Health check para verificar que el servicio de testing y la conexión con RAWG API están operativos.

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada:** Ninguno

**Parámetros de salida (JSON):**
```json
{
  "status": "UP",
  "service": "GameTestController",
  "rawgService": {
    "status": "...",
    "baseUrl": "..."
  },
  "timestamp": 1234567890
}
```

**Códigos de estado:**
- `200 OK`: Servicio operativo

**DTO utilizado:** Ninguno (respuesta Map genérico)

---

### `GET /api/test/games/search`

**Descripción:** Prueba la funcionalidad de búsqueda de juegos en la API de RAWG.

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada (Query params):**
- `q` (string, requerido): Término de búsqueda
- `page` (integer, opcional): Número de página (default: 1)
- `size` (integer, opcional): Tamaño de página (default: 10)

**Ejemplo:**
```
GET /api/test/games/search?q=zelda&page=1&size=5
```

**Parámetros de salida (JSON):**
```json
{
  "count": 150,
  "next": "...",
  "previous": null,
  "results": [
    {
      "id": 12345,
      "slug": "the-legend-of-zelda-breath-of-the-wild",
      "name": "The Legend of Zelda: Breath of the Wild",
      "background_image": "https://...",
      "rating": 4.7,
      ...
    }
  ]
}
```

**Notas:**
- La respuesta es el JSON directo de la API de RAWG
- No está transformado a DTOs de Gamelyx

**Códigos de estado:**
- `200 OK`: Búsqueda exitosa
- `500 Internal Server Error`: Error en API externa

**DTO utilizado:**
- Response: `RawgApiDtos.GameSearchResponse` (DTOs externos)

---

### `GET /api/test/games/details/{gameId}`

**Descripción:** Obtiene los detalles completos de un juego específico desde la API de RAWG.

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada (Path params):**
- `gameId` (integer, requerido): ID del juego en RAWG

**Ejemplo:**
```
GET /api/test/games/details/3498
```

**Parámetros de salida (JSON):**
```json
{
  "id": 3498,
  "slug": "grand-theft-auto-v",
  "name": "Grand Theft Auto V",
  "description": "HTML description...",
  "description_raw": "Plain text description...",
  "background_image": "https://...",
  "rating": 4.5,
  "platforms": [...],
  "genres": [...],
  ...
}
```

**Notas:**
- Respuesta directa de RAWG API sin transformación

**Códigos de estado:**
- `200 OK`: Juego encontrado
- `404 Not Found`: Juego no encontrado

**DTO utilizado:**
- Response: `RawgApiDtos.GameDetails` (DTOs externos)

---

### `GET /api/test/games/popular`

**Descripción:** Obtiene una lista de juegos populares desde RAWG API.

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada (Query params):**
- `page` (integer, opcional): Número de página (default: 1)
- `size` (integer, opcional): Tamaño de página (default: 10)

**Ejemplo:**
```
GET /api/test/games/popular?page=1&size=5
```

**Parámetros de salida (JSON):**
```json
{
  "count": 500,
  "results": [...]
}
```

**Códigos de estado:**
- `200 OK`: Juegos populares obtenidos
- `500 Internal Server Error`: Error en API externa

**DTO utilizado:**
- Response: `RawgApiDtos.GameSearchResponse` (DTOs externos)

---

### `GET /api/test/games/trending`

**Descripción:** Obtiene una lista de juegos trending (tendencia) desde RAWG API.

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada (Query params):**
- `page` (integer, opcional): Número de página (default: 1)
- `size` (integer, opcional): Tamaño de página (default: 10)

**Ejemplo:**
```
GET /api/test/games/trending?page=1&size=5
```

**Parámetros de salida (JSON):**
```json
{
  "count": 200,
  "results": [...]
}
```

**Códigos de estado:**
- `200 OK`: Juegos trending obtenidos
- `500 Internal Server Error`: Error en API externa

**DTO utilizado:**
- Response: `RawgApiDtos.GameSearchResponse` (DTOs externos)

---

### `GET /api/test/games/screenshots/{gameId}`

**Descripción:** Obtiene las capturas de pantalla de un juego específico desde RAWG API.

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada (Path params):**
- `gameId` (integer, requerido): ID del juego en RAWG

**Ejemplo:**
```
GET /api/test/games/screenshots/3498
```

**Parámetros de salida (JSON):**
```json
{
  "count": 12,
  "results": [
    {
      "id": 12345,
      "image": "https://...",
      "width": 1920,
      "height": 1080
    }
  ]
}
```

**Códigos de estado:**
- `200 OK`: Screenshots obtenidas
- `404 Not Found`: Juego no encontrado

**DTO utilizado:**
- Response: `RawgApiDtos.ScreenshotsResponse` (DTOs externos)

---

### `GET /api/test/games/quick-test`

**Descripción:** Endpoint de prueba rápida que obtiene información de un juego conocido (GTA V, ID: 3498) sin necesidad de parámetros. Útil para verificar rápidamente que la integración funciona.

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada:** Ninguno

**Parámetros de salida (JSON - Éxito):**
```json
{
  "status": "SUCCESS",
  "gameName": "Grand Theft Auto V",
  "rating": 4.5,
  "platforms": ["PC", "PlayStation 4", "Xbox One"],
  "description": "Primeros 100 caracteres de la descripción...",
  "testTimestamp": 1234567890
}
```

**Parámetros de salida (JSON - Error):**
```json
{
  "status": "FAILED",
  "error": "Mensaje de error...",
  "testTimestamp": 1234567890
}
```

**Códigos de estado:**
- `200 OK`: Test completado (verificar campo `status` en el JSON)

**DTO utilizado:** Ninguno (respuesta Map genérico construido a partir de GameDetails)

---

## Resumen de DTOs

### DTOs de Response (Externos - RAWG API)

Todos los DTOs pertenecen al paquete `com.gamelyx.dto.external.RawgApiDtos`:

- `GameSearchResponse` - Respuesta de búsqueda de juegos
  - `count`, `next`, `previous`, `results`

- `GameDetails` - Detalles completos de un juego
  - `id`, `slug`, `name`, `description`, `description_raw`
  - `background_image`, `rating`, `platforms`, `genres`, etc.

- `ScreenshotsResponse` - Lista de screenshots
  - `count`, `results`

**Nota:** Estos son DTOs que mapean directamente la estructura JSON de la API de RAWG y no han sido transformados a los DTOs internos de Gamelyx.

---

## Propósito de este controlador

1. **Validación de integración:** Verificar que la conexión con RAWG API funciona correctamente
2. **Testing de endpoints:** Probar diferentes endpoints de RAWG sin afectar la lógica de negocio
3. **Debug:** Facilitar el debugging de problemas con la API externa
4. **Desarrollo:** Útil durante el desarrollo para explorar los datos disponibles

⚠️ **Este controlador debe eliminarse o deshabilitarse en producción**, ya que expone directamente los DTOs de la API externa sin la capa de transformación y lógica de negocio de Gamelyx.
