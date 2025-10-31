# SocialController - Endpoints Sociales

## Introducción

El **SocialController** (`/api/social`) gestiona todas las funcionalidades sociales de Gamelyx. Permite a los usuarios conectar entre sí, gestionar amistades, buscar otros jugadores y recibir sugerencias automáticas basadas en gustos similares.

**Funcionalidades principales:**
- Gestión de solicitudes de amistad (enviar, aceptar, rechazar)
- Eliminación de amistades bidireccionales
- Búsqueda de usuarios por nombre
- Sugerencias automáticas de amigos basadas en juegos y ratings
- Vista consolidada de datos sociales para el home

---

## Endpoints

### `GET /api/social/home-social-data`

**Descripción:** Obtiene toda la información social necesaria para el home del usuario: amigos, solicitudes entrantes/salientes, y juegos preferidos.

**Método HTTP:** GET

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada:** Ninguno

**Parámetros de salida (JSON):**
```json
{
  "friends": [
    {
      "user": {
        "userId": "uuid",
        "username": "string"
      },
      "newMessages": true
    }
  ],
  "incomingRequests": [
    {
      "requestId": "uuid",
      "contactUser": {
        "user": {
          "userId": "uuid",
          "username": "string"
        },
        "newMessages": false
      },
      "source": "MANUAL",
      "status": "PENDING",
      "sharedGame": {
        "gameSlug": "minecraft",
        "gameName": "Minecraft",
        "yourRating": 9,
        "theirRating": 8
      },
      "receivedAt": "2024-01-15T10:30:00"
    }
  ],
  "outgoingRequests": [
    {
      "requestId": "uuid",
      "contactUser": {...},
      "source": "SUGGESTION",
      "status": "PENDING",
      "sharedGame": null,
      "receivedAt": "2024-01-14T09:20:00"
    }
  ],
  "preferredGames": [
    {
      "gameSlug": "minecraft",
      "gameName": "Minecraft",
      "userRating": 9
    }
  ]
}
```

**Notas:**
- `source`: Enum con valores `MANUAL` o `SUGGESTION`
- `status`: Enum con valores `PENDING`, `ACCEPTED`, `REJECTED`
- `sharedGame`: Solo presente si la solicitud vino de una sugerencia por juego
- `preferredGames`: Juegos con rating >= 7

**Códigos de estado:**
- `200 OK`: Datos obtenidos exitosamente
- `401 Unauthorized`: Usuario no autenticado

**DTO utilizado:**
- Response: `SocialResponseDtos.HomeSocialDataDto`
  - Incluye: `ContactUserDto`, `FriendRequestDto`, `PreferredGameDto`, `SharedGameInfoDto`

---

### `POST /api/social/friend-requests`

**Descripción:** Envía una solicitud de amistad a otro usuario. Valida que no existan solicitudes duplicadas ni rechazadas previamente.

**Método HTTP:** POST

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada (RequestBody - JSON):**
```json
{
  "targetUserId": "uuid",
  "source": "MANUAL",
  "gameSlug": "minecraft",
  "yourRating": 9
}
```

**Notas:**
- `source`: Enum `MANUAL` o `SUGGESTION`
- `gameSlug` y `yourRating`: Requeridos si `source` es `SUGGESTION`, opcionales si es `MANUAL`

**Parámetros de salida (JSON):**
```json
{
  "requestId": "uuid",
  "contactUser": {
    "user": {
      "userId": "uuid",
      "username": "string"
    },
    "newMessages": false
  },
  "source": "MANUAL",
  "status": "PENDING",
  "sharedGame": null,
  "receivedAt": "2024-01-15T10:30:00"
}
```

**Códigos de estado:**
- `200 OK`: Solicitud enviada exitosamente
- `400 Bad Request`: Solicitud duplicada o usuario no encontrado
- `401 Unauthorized`: Usuario no autenticado

**DTO utilizado:**
- Request: `SocialRequestDtos.SendFriendRequestDto`
- Response: `SocialResponseDtos.FriendRequestDto`

---

### `PUT /api/social/friend-requests/{requestId}/respond`

**Descripción:** Responde a una solicitud de amistad (aceptar o rechazar). Si se acepta, crea la relación de amistad bidireccional.

**Método HTTP:** PUT

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada (Path params):**
- `requestId` (string, requerido): ID de la solicitud

**Parámetros de entrada (Query params):**
- `action` (string, requerido): `ACCEPT` o `REJECT`

**Ejemplo:**
```
PUT /api/social/friend-requests/550e8400-e29b-41d4-a716-446655440000/respond?action=ACCEPT
```

**Parámetros de salida (JSON):**
```json
{
  "success": true,
  "newFriend": {
    "user": {
      "userId": "uuid",
      "username": "string"
    },
    "newMessages": false
  }
}
```

**Notas:**
- Si `action` es `REJECT`, `newFriend` será `null`

**Códigos de estado:**
- `200 OK`: Solicitud respondida exitosamente
- `404 Not Found`: Solicitud no encontrada
- `400 Bad Request`: Acción inválida
- `401 Unauthorized`: Usuario no autenticado

**DTO utilizado:**
- Request: `SocialRequestDtos.FriendRequestAction` (enum)
- Response: `SocialResponseDtos.FriendRequestResponseDto`

---

### `PUT /api/social/friend-requests/{requestId}/mark-notified`

**Descripción:** Marca una solicitud como "notificada" al remitente. Se usa después de mostrar notificaciones para evitar duplicados.

**Método HTTP:** PUT

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada (Path params):**
- `requestId` (string, requerido): ID de la solicitud

**Parámetros de entrada:** Ninguno adicional

**Parámetros de salida:** Void (respuesta vacía)

**Códigos de estado:**
- `200 OK`: Marcada como notificada
- `401 Unauthorized`: Usuario no autenticado

**DTO utilizado:** Ninguno

---

### `DELETE /api/social/friends/{friendId}`

**Descripción:** Elimina a un usuario de la lista de amigos. La eliminación es bidireccional y también elimina las solicitudes relacionadas.

**Método HTTP:** DELETE

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada (Path params):**
- `friendId` (uuid, requerido): UUID del amigo a eliminar

**Parámetros de salida (JSON):**
```json
{
  "success": true,
  "deletedFriendUsername": "string",
  "deletedFriendId": "uuid"
}
```

**Códigos de estado:**
- `200 OK`: Amigo eliminado exitosamente
- `400 Bad Request`: Error al eliminar (no son amigos)
- `401 Unauthorized`: Usuario no autenticado

**DTO utilizado:**
- Response: `SocialResponseDtos.DeleteFriendResponseDto`

---

### `GET /api/social/search/users`

**Descripción:** Busca usuarios por nombre (coincidencias parciales, case-insensitive). Excluye amigos actuales y usuarios con solicitudes pendientes.

**Método HTTP:** GET

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada (Query params):**
- `q` (string, requerido): Término de búsqueda
- `limit` (integer, opcional): Límite de resultados (default: 10)

**Ejemplo:**
```
GET /api/social/search/users?q=Juan&limit=10
```

**Parámetros de salida (JSON):**
```json
{
  "query": "Juan",
  "users": [
    {
      "user": {
        "userId": "uuid",
        "username": "Juan123"
      },
      "isFriend": false,
      "hasPendingRequest": false,
      "hasRejectedRequest": false
    }
  ]
}
```

**Códigos de estado:**
- `200 OK`: Búsqueda completada
- `401 Unauthorized`: Usuario no autenticado

**DTO utilizado:**
- Request: `SocialRequestDtos.UserSearchRequestDto`
- Response: `SocialResponseDtos.UserSearchResultDto`
  - Incluye: `SearchedUserDto`, `UserDto`

---

### `GET /api/social/friend-suggestion/by-game`

**Descripción:** Obtiene una sugerencia de usuario basada en un juego y rating similar. Excluye amigos actuales y usuarios rechazados.

**Método HTTP:** GET

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada (Query params):**
- `gameSlug` (string, requerido): Slug del juego
- `userRating` (integer, requerido): Rating del usuario (1-10)

**Ejemplo:**
```
GET /api/social/friend-suggestion/by-game?gameSlug=minecraft&userRating=9
```

**Parámetros de salida (JSON):**
```json
{
  "user": {
    "userId": "uuid",
    "username": "string"
  },
  "sharedGameInfoDto": {
    "gameSlug": "minecraft",
    "gameName": "Minecraft",
    "yourRating": 9,
    "theirRating": 8
  }
}
```

**Códigos de estado:**
- `200 OK`: Sugerencia obtenida
- `204 No Content`: No hay sugerencias disponibles
- `401 Unauthorized`: Usuario no autenticado

**DTO utilizado:**
- Response: `SocialResponseDtos.SuggestedUserDto`
  - Incluye: `UserDto`, `SharedGameInfoDto`

---

### `POST /api/social/friend-suggestion/reject`

**Descripción:** Rechaza una sugerencia específica para que no vuelva a aparecer en el contexto de ese juego.

**Método HTTP:** POST

**Autenticación:** Bearer Token (requerido)

**Parámetros de entrada (Query params):**
- `rejectedUserId` (uuid, requerido): UUID del usuario rechazado
- `gameSlug` (string, requerido): Slug del juego

**Ejemplo:**
```
POST /api/social/friend-suggestion/reject?rejectedUserId=550e8400-e29b-41d4-a716-446655440000&gameSlug=minecraft
```

**Parámetros de salida:** Void (respuesta vacía)

**Códigos de estado:**
- `200 OK`: Sugerencia rechazada
- `401 Unauthorized`: Usuario no autenticado

**DTO utilizado:** Ninguno

---

## Resumen de DTOs

### DTOs de Request
- `SendFriendRequestDto(targetUserId, source, gameSlug, yourRating)`
- `UserSearchRequestDto(query, limit)`
- `FriendRequestAction` (enum: ACCEPT, REJECT)

### DTOs de Response
- `HomeSocialDataDto(friends, incomingRequests, outgoingRequests, preferredGames)`
- `FriendRequestDto(requestId, contactUser, source, status, sharedGame, receivedAt)`
- `ContactUserDto(user, newMessages)`
- `UserDto(userId, username)`
- `SharedGameInfoDto(gameSlug, gameName, yourRating, theirRating)`
- `PreferredGameDto(gameSlug, gameName, userRating)`
- `FriendRequestResponseDto(success, newFriend)`
- `DeleteFriendResponseDto(success, deletedFriendUsername, deletedFriendId)`
- `UserSearchResultDto(query, users)`
- `SearchedUserDto(user, isFriend, hasPendingRequest, hasRejectedRequest)`
- `SuggestedUserDto(user, sharedGameInfoDto)`

### Enums
- `FriendRequest.RequestSource`: MANUAL, SUGGESTION
- `FriendRequest.FriendRequestStatus`: PENDING, ACCEPTED, REJECTED
