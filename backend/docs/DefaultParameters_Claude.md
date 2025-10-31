# Default Parameters - Parámetros por Defecto en Endpoints

## Introducción

Este documento lista todos los endpoints de la API de Gamelyx que tienen **parámetros con valores por defecto**. Estos valores se aplican cuando el cliente no proporciona el parámetro en la petición.

Los valores por defecto se implementan de dos formas:
1. **En el controlador**: Usando `@RequestParam(defaultValue = "...")`
2. **En el DTO**: Usando compact constructors en los records

---

## GameController (`/api/games`)

### `GET /api/games/search`

**Parámetros con valores por defecto:**

| Parámetro | Tipo | Valor por Defecto | Ubicación |
|-----------|------|-------------------|-----------|
| `page` | integer | `1` | Controlador |
| `size` | integer | `20` | Controlador |

**Implementación:**
```java
@RequestParam(value = "page", defaultValue = "1") int page
@RequestParam(value = "size", defaultValue = "20") int size
```

**Ejemplo de uso:**
```
GET /api/games/search?q=Minecraft
// Equivale a: GET /api/games/search?q=Minecraft&page=1&size=20
```

---

### `GET /api/games/my-reviews`

**Parámetros con valores por defecto (aplicados en lógica interna):**

| Parámetro | Tipo | Valor por Defecto | Ubicación |
|-----------|------|-------------------|-----------|
| `limit` | integer | `3` | Lógica del controlador |
| `page` | integer | `0` | Lógica del controlador |
| `size` | integer | `20` | Lógica del controlador |

**Implementación:**
```java
// Parámetros opcionales (required = false)
@RequestParam(value = "limit", required = false) Integer limit
@RequestParam(value = "page", required = false) Integer page
@RequestParam(value = "size", required = false) Integer size

// Valores por defecto aplicados en el código:
int finalLimit = (limit != null) ? limit : 3;     // Default: 3
int finalPage = (page != null) ? page : 0;         // Default: 0
int finalSize = (size != null) ? size : 20;        // Default: 20
```

**Notas:**
- Si se omiten `page` y `size`, se usa modo "home" con `limit` (default 3)
- Si se proporciona `page` o `size`, se usa modo "completo" con paginación

**Ejemplo de uso:**
```
GET /api/games/my-reviews
// Modo home: limit=3

GET /api/games/my-reviews?limit=5
// Modo home: limit=5

GET /api/games/my-reviews?page=0&size=10
// Modo completo: page=0, size=10
```

---

## SocialController (`/api/social`)

### `GET /api/social/search/users`

**Parámetros con valores por defecto:**

| Parámetro | Tipo | Valor por Defecto | Ubicación |
|-----------|------|-------------------|-----------|
| `limit` | integer | `10` | Controlador |

**Implementación:**
```java
@RequestParam(defaultValue = "10") int limit
```

**Ejemplo de uso:**
```
GET /api/social/search/users?q=Juan
// Equivale a: GET /api/social/search/users?q=Juan&limit=10
```

---

## ChatRestController (`/api/chat`)

### `GET /api/chat/messages`

**Parámetros con valores por defecto (aplicados en el DTO):**

| Parámetro | Tipo | Valor por Defecto | Ubicación |
|-----------|------|-------------------|-----------|
| `page` | integer | `0` | DTO (compact constructor) |
| `limit` | integer | `20` | DTO (compact constructor) |

**Implementación en el controlador:**
```java
@RequestParam(required = false) Integer page
@RequestParam(required = false) Integer limit
```

**Implementación en `GetMessagesRequestDto`:**
```java
public record GetMessagesRequestDto(
        UUID otherUserId,
        Integer page,
        Integer limit
) {
    public GetMessagesRequestDto {
        // Valores por defecto aplicados en el compact constructor
        if (page == null || page < 0) page = 0;
        if (limit == null || limit <= 0 || limit > 50) limit = 20;
    }
}
```

**Validaciones adicionales:**
- `page`: Si es null o negativo → 0
- `limit`: Si es null, ≤0 o >50 → 20 (max: 50)

**Ejemplo de uso:**
```
GET /api/chat/messages?otherUserId=550e8400-e29b-41d4-a716-446655440000
// Equivale a: GET /api/chat/messages?otherUserId=...&page=0&limit=20

GET /api/chat/messages?otherUserId=...&limit=100
// limit > 50, se ajusta a: limit=20
```

---

## GameTestController (`/api/test/games`)

### `GET /api/test/games/search`

**Parámetros con valores por defecto:**

| Parámetro | Tipo | Valor por Defecto | Ubicación |
|-----------|------|-------------------|-----------|
| `page` | integer | `1` | Controlador |
| `size` | integer | `10` | Controlador |

**Implementación:**
```java
@RequestParam(value = "page", defaultValue = "1") int page
@RequestParam(value = "size", defaultValue = "10") int size
```

**Ejemplo de uso:**
```
GET /api/test/games/search?q=zelda
// Equivale a: GET /api/test/games/search?q=zelda&page=1&size=10
```

---

### `GET /api/test/games/popular`

**Parámetros con valores por defecto:**

| Parámetro | Tipo | Valor por Defecto | Ubicación |
|-----------|------|-------------------|-----------|
| `page` | integer | `1` | Controlador |
| `size` | integer | `10` | Controlador |

**Implementación:**
```java
@RequestParam(value = "page", defaultValue = "1") int page
@RequestParam(value = "size", defaultValue = "10") int size
```

**Ejemplo de uso:**
```
GET /api/test/games/popular
// Equivale a: GET /api/test/games/popular?page=1&size=10
```

---

### `GET /api/test/games/trending`

**Parámetros con valores por defecto:**

| Parámetro | Tipo | Valor por Defecto | Ubicación |
|-----------|------|-------------------|-----------|
| `page` | integer | `1` | Controlador |
| `size` | integer | `10` | Controlador |

**Implementación:**
```java
@RequestParam(value = "page", defaultValue = "1") int page
@RequestParam(value = "size", defaultValue = "10") int size
```

**Ejemplo de uso:**
```
GET /api/test/games/trending
// Equivale a: GET /api/test/games/trending?page=1&size=10
```

---

## Resumen por Controlador

### AuthController
- ✅ **Sin parámetros por defecto**

### GameController
- `GET /search`: `page=1`, `size=20`
- `GET /my-reviews`: `limit=3` (modo home), `page=0`, `size=20` (modo completo)

### SocialController
- `GET /search/users`: `limit=10`

### ChatRestController
- `GET /messages`: `page=0`, `limit=20` (aplicados en DTO)

### ChatWebSocketController
- ✅ **Sin parámetros por defecto** (WebSocket usa mensajes JSON completos)

### GameTestController
- `GET /search`: `page=1`, `size=10`
- `GET /popular`: `page=1`, `size=10`
- `GET /trending`: `page=1`, `size=10`

---

## Notas Técnicas

### Diferencia entre `defaultValue` y lógica interna

**Método 1: `defaultValue` en `@RequestParam`**
```java
@RequestParam(defaultValue = "10") int limit
```
- El valor por defecto se aplica **antes** de entrar al método
- El parámetro nunca es `null` en el método

**Método 2: `required = false` + lógica en código**
```java
@RequestParam(required = false) Integer limit
// En el código:
int finalLimit = (limit != null) ? limit : 10;
```
- El parámetro puede ser `null` en el método
- El valor por defecto se aplica mediante lógica condicional
- Permite lógica más compleja (ej: diferentes comportamientos según valores null)

**Método 3: Compact constructor en DTO**
```java
public record GetMessagesRequestDto(Integer page, Integer limit) {
    public GetMessagesRequestDto {
        if (page == null || page < 0) page = 0;
        if (limit == null || limit <= 0) limit = 20;
    }
}
```
- Los valores por defecto se aplican al construir el DTO
- Permite validaciones y normalizaciones adicionales
- Centraliza la lógica de valores por defecto en el DTO

---

## Importancia para el Frontend

Los valores por defecto permiten que el frontend:

1. **Simplifique las peticiones**: No es necesario enviar todos los parámetros
2. **Optimice el código**: Puede omitir parámetros comunes
3. **Mejore la UX**: Comportamientos predecibles sin configuración adicional

**Ejemplo:**
```javascript
// Petición simplificada
fetch('/api/games/search?q=Minecraft')

// Equivale a:
fetch('/api/games/search?q=Minecraft&page=1&size=20')
```
