# Frontend Chat Implementation Guide

## **ACTUALIZADA - Con contexto del backend real**

## 🎯 Objetivo General

Implementar interfaz de chat en tiempo real integrada con el sistema social existente, usando WebSockets y arquitectura de Signals, **adaptada al backend específico ya implementado**.

## 🏗️ Arquitectura Técnica (Adaptada al Backend)

### **Backend Endpoints Disponibles:**

- **WebSocket**: `ws://localhost:8080/ws` (con auth JWT)
- **REST**: `GET /api/chat/messages?otherUserId={uuid}`
- **WebSocket Send**: `/app/chat/send` → `{recipientId, content}`
- **WebSocket Mark Read**: `/app/chat/mark-read` → `{otherUserId}`
- **WebSocket Subscribe**: `/topic/chat/user/{userId}` (personal channel)

### **Datos del Backend:**

```typescript
// DTOs reales del backend
interface MessageDto {
  messageId: string;
  senderId: string;
  senderUsername: string;
  content: string; // Ya viene descifrado
  sentAt: string; // ISO date
  isRead: boolean;
}

interface WebSocketMessageDto {
  type: "NEW_MESSAGE";
  message: MessageDto;
  conversationId: string; // Para tracking interno
  recipientId: string;
}

interface WebSocketReadReceiptDto {
  type: "MESSAGES_READ";
  readByUserId: string;
  messageCount: number;
}
```

---

## **PASO 1: Dependencias y Configuración** (Actualizado)

### **Tarea 1.1: Instalar Dependencias WebSocket**

Instalar dependencias necesarias para WebSocket:

- sockjs-client
- @stomp/stompjs
- @types/sockjs-client

### **Tarea 1.2: Environment Configuration (Específico)**

Configurar environment con endpoints específicos del backend:

- WebSocket URL: ws://localhost:8080/ws
- Endpoints: /app/chat/send, /app/chat/mark-read, /topic/chat/user/
- Configuración de reconexión y límites

---

## **PASO 2: WebSocket Service (Específico del Backend)**

### **Tarea 2.1: Configurar para Backend Real**

Crear WebSocketService con:

- Configuración de conexión SockJS
- Headers de autenticación JWT
- Métodos para enviar mensajes
- Métodos para marcar como leído
- Suscripción a canales de usuario
- Manejo de reconexión automática

### **Tarea 2.2: HTTP Service para Historial**

Crear ChatHttpService para:

- Cargar historial de conversaciones via REST
- Paginación con parámetros page y limit
- Integración con endpoints del backend
- Manejo de respuestas tipadas

---

## **PASO 3: Chat Store (Con DTOs Reales)**

### **Tarea 3.1: Store con Tipos del Backend**

Crear ChatStore con:

- Signals para manejo reactivo de estado
- Map de conversaciones por otherUserId
- Estado de conexión WebSocket
- Métodos para agregar mensajes
- Manejo de mensajes WebSocket entrantes
- Computed values para acceso reactivo a conversaciones

---

## **PASO 4: Integración con Sistema Social Existente**

### **Tarea 4.1: Botones de Chat en ContactUserCard**

Modificar componente existente para:

- Agregar botón de chat en tarjetas de contacto
- Usar ContactUserDto.user.userId para identificar usuario
- Integrar con ChatModalService para abrir conversaciones

### **Tarea 4.2: Chat Modal Service**

Crear ChatModalService para:

- Gestionar ventanas de chat activas
- Cargar historial al abrir conversación
- Coordinar entre ChatStore y ChatHttpService
- Manejar múltiples chats simultáneos

---

## ✅ Criterios de Éxito Específicos

El frontend estará completo cuando:

1. **WebSocket conecta** con JWT del AuthStore usando `Authorization: Bearer token`
2. **Mensajes se envían** via `/app/chat/send` con formato `{recipientId, content}`
3. **Mensajes se reciben** via `/topic/chat/user/{userId}` con formato `WebSocketMessageDto`
4. **Historial se carga** via `GET /api/chat/messages?otherUserId=uuid`
5. **Mark as read** funciona via `/app/chat/mark-read` con `{otherUserId}`
6. **Integración social** usando `ContactUserDto.user.userId` para abrir chats
7. **Sin conversation IDs** en frontend - todo basado en `otherUserId`

---

## 🚨 Puntos Críticos Específicos del Backend

- **Autenticación**: JWT en `Authorization: Bearer token` header

- **Endpoints exactos**: `/app/chat/send`, `/app/chat/mark-read`, `/topic/chat/user/{userId}`
- **DTOs tipados**: Usar `MessageDto`, `WebSocketMessageDto`, `WebSocketReadReceiptDto`

- **Cifrado transparente**: Backend maneja cifrado/descifrado automáticamente
- **Paginación**: Usar `page` y `limit` para historial REST

---
