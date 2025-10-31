# AuthController - Endpoints de Autenticación

## Introducción

El **AuthController** (`/api/auth`) gestiona toda la autenticación y autorización de usuarios en Gamelyx. Implementa un sistema de autenticación basado en **JSON Web Tokens (JWT)**, soportando tanto registro tradicional (email/contraseña) como autenticación mediante **Google OAuth 2.0**.

**Funcionalidades principales:**

- Registro de nuevos usuarios con verificación de email
- Inicio de sesión con credenciales o Google OAuth
- Gestión de tokens JWT para autenticación stateless
- Health checks y utilidades de testing

---

## Endpoints

### `POST /api/auth/register`

**Descripción:** Registra un nuevo usuario en la plataforma. Al completar el registro, se envía un correo electrónico de verificación.

**Método HTTP:** POST

**Autenticación:** None (público)

**Parámetros de entrada (RequestBody - JSON):**

```json
{
  "username": "string", // 3-20 caracteres, obligatorio
  "email": "string", // Email válido, obligatorio
  "password": "string", // Mínimo 6 caracteres, obligatorio
  "confirmPassword": "string" // Confirmación de contraseña, obligatorio
}
```

**Parámetros de salida (JSON):**

```json
{
  "userId": "uuid",
  "username": "string",
  "email": "string",
  "message": "string",
  "emailVerificationRequired": true
}
```

**Códigos de estado:**

- `201 Created`: Usuario registrado exitosamente
- `400 Bad Request`: Datos inválidos o email ya registrado
- `500 Internal Server Error`: Error interno del servidor

**DTO utilizado:**

- Request: `AuthDtos.RegisterRequest`
- Response: `AuthDtos.RegisterResponse`

---

### `POST /api/auth/login`

**Descripción:** Autentica a un usuario existente mediante email/username y contraseña. Devuelve tokens JWT (accessToken y refreshToken).

**Método HTTP:** POST

**Autenticación:** None (público)

**Parámetros de entrada (RequestBody - JSON):**

```json
{
  "usernameOrEmail": "string", // Email o username, obligatorio
  "password": "string" // Contraseña, obligatorio
}
```

**Validaciones:**

- `usernameOrEmail`: NotBlank
- `password`: NotBlank

**Parámetros de salida (JSON):**

```json
{
  "accessToken": "string", // Token JWT de acceso
  "refreshToken": "string", // Token JWT de refresco
  "userId": "uuid",
  "username": "string",
  "email": "string",
  "message": "string"
}
```

**Códigos de estado:**

- `200 OK`: Login exitoso
- `401 Unauthorized`: Credenciales inválidas
- `400 Bad Request`: Email no verificado o datos inválidos
- `500 Internal Server Error`: Error interno del servidor

**DTO utilizado:**

- Request: `AuthDtos.LoginRequest`
- Response: `AuthDtos.AuthResponse`

---

### `POST /api/auth/google`

**Descripción:** Autentica o registra automáticamente usuarios mediante Google OAuth 2.0. Si el usuario no existe, se crea automáticamente.

**Método HTTP:** POST

**Autenticación:** None (público)

**Parámetros de entrada (RequestBody - JSON):**

```json
{
  "googleId": "string", // ID de Google, obligatorio
  "email": "string", // Email de Google (validado), obligatorio
  "name": "string" // Nombre del usuario de Google, obligatorio
}
```

**Validaciones:**

- `googleId`: NotBlank
- `email`: NotBlank, Email válido
- `name`: NotBlank

**Parámetros de salida (JSON):**

```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "userId": "uuid",
  "username": "string",
  "email": "string",
  "message": "string"
}
```

**Códigos de estado:**

- `200 OK`: Autenticación exitosa
- `400 Bad Request`: Token de Google inválido
- `500 Internal Server Error`: Error interno del servidor

**DTO utilizado:**

- Request: `AuthDtos.GoogleAuthRequest`
- Response: `AuthDtos.AuthResponse`

---

### `POST /api/auth/logout`

**Descripción:** Cierra la sesión del usuario. El cliente debe eliminar los tokens almacenados localmente.

**Método HTTP:** POST

**Autenticación:** Bearer Token (opcional)

**Parámetros de entrada:** Ninguno

**Parámetros de salida (JSON):**

```json
{
  "message": "Logout exitoso",
  "success": "true",
  "timestamp": "1234567890"
}
```

**Códigos de estado:**

- `200 OK`: Logout exitoso

**DTO utilizado:** Ninguno (respuesta Map genérico)

---

### `GET /api/auth/health`

**Descripción:** Health check para verificar que el servicio de autenticación está operativo.

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada:** Ninguno

**Parámetros de salida (JSON):**

```json
{
  "status": "UP",
  "service": "Authentication Service",
  "timestamp": 1234567890
}
```

**Códigos de estado:**

- `200 OK`: Servicio operativo

**DTO utilizado:** Ninguno (respuesta Map genérico)

---

### `GET /api/auth/test-email`

**Descripción:** Endpoint de testing para verificar el sistema de envío de emails (solo desarrollo).

**Método HTTP:** GET

**Autenticación:** None (público)

**Parámetros de entrada (Query params):**

- `email` (string, requerido): Email de destino

**Ejemplo:**

```
GET /api/auth/test-email?email=test@example.com
```

**Parámetros de salida (JSON - Éxito):**

```json
{
  "success": true,
  "message": "Email de prueba enviado correctamente",
  "email": "test@example.com",
  "timestamp": 1234567890
}
```

**Parámetros de salida (JSON - Error):**

```json
{
  "success": false,
  "error": "Error enviando email de prueba: [mensaje]",
  "email": "test@example.com",
  "timestamp": 1234567890
}
```

**Códigos de estado:**

- `200 OK`: Email enviado exitosamente
- `400 Bad Request`: Email inválido
- `500 Internal Server Error`: Error enviando email

**DTO utilizado:** Ninguno (respuesta Map genérico)

---

## Endpoints NO implementados (comentados)

### `POST /api/auth/refresh` ❌ NO IMPLEMENTADO

**Estado:** Comentado en el código, no disponible actualmente.

**DTO existente pero no utilizado:**

- Request: `AuthDtos.RefreshTokenRequest`

---

### `POST /api/auth/verify-email` ❌ NO IMPLEMENTADO

**Estado:** Comentado en el código, no disponible actualmente.

**DTO existente pero no utilizado:**

- Request: `AuthDtos.VerifyEmailRequest`

---

## Resumen de DTOs

### DTOs de Request

- `RegisterRequest(username, email, password, confirmPassword)`
- `LoginRequest(usernameOrEmail, password)`
- `GoogleAuthRequest(googleId, email, name)`
- `RefreshTokenRequest(refreshToken)` - NO USADO
- `VerifyEmailRequest(token)` - NO USADO

### DTOs de Response

- `RegisterResponse(userId, username, email, message, emailVerificationRequired)`
- `AuthResponse(accessToken, refreshToken, userId, username, email, message)`
