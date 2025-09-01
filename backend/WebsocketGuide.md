# Backend Chat Implementation Guide

## 🎯 Objetivo General
Implementar un sistema de chat en tiempo real entre dos usuarios usando WebSockets, con mensajes cifrados y persistencia en PostgreSQL.

## 🏗️ Arquitectura Técnica

### **Tecnologías**
- **WebSocket**: Spring Boot WebSocket + STOMP protocol
- **Cifrado**: AES-256 symmetric encryption
- **Base de datos**: PostgreSQL con entidades JPA
- **Autenticación**: Integración con sistema JWT existente

### **Estructura de Datos**
```
Conversation Entity:
├── id (UUID, PK)
├── userOneId (UUID, FK) - Siempre el menor UUID
├── userTwoId (UUID, FK) - Siempre el mayor UUID
├── createdAt (Timestamp)
└── updatedAt (Timestamp)

Message Entity:
├── id (UUID, PK)
├── conversationId (UUID, FK)
├── senderId (UUID, FK)
├── encryptedContent (TEXT)
├── sentAt (Timestamp)
└── isRead (Boolean)
```

## 📋 Roadmap de Implementación

---

## **PASO 1: Dependencias y Configuración Base**

### **Tarea 1.1: Añadir Dependencias WebSocket**
- Añadir `spring-boot-starter-websocket` al pom.xml
- Añadir `spring-messaging` para STOMP
- Verificar que las dependencias son compatibles con Spring Boot 3.5.3

### **Prueba 1.1:**
- Ejecutar `mvn clean compile`
- Verificar que no hay conflictos de dependencias
- El proyecto debe compilar sin errores

### **Tarea 1.2: WebSocket Configuration**
- Crear `WebSocketConfig.java` en package `com.gamelyx.config`
- Configurar endpoint `/ws` con SockJS fallback
- Configurar message broker: `/app` para cliente→servidor, `/topic` para broadcast
- Añadir CORS para `http://localhost:4200`

### **Prueba 1.2:**
- Arrancar la aplicación
- Verificar en logs que WebSocket se inicializa correctamente
- No debe haber errores relacionados con WebSocket configuration

---

## **PASO 2: Entidades y Repositorios**

### **Tarea 2.1: Conversation Entity**
- Crear `Conversation.java` en package `com.gamelyx.entity`
- Incluir todos los campos mencionados en la arquitectura
- Usar `@GeneratedValue(strategy = GenerationType.UUID)` para el ID
- Añadir validaciones JPA apropiadas
- Crear constructor que ordene automáticamente userOneId < userTwoId

### **Prueba 2.1:**
- Ejecutar la aplicación
- Verificar que la tabla `conversations` se crea en PostgreSQL
- Inspeccionar el esquema de la tabla generada

### **Tarea 2.2: Message Entity**
- Crear `Message.java` en package `com.gamelyx.entity`
- Incluir relación ManyToOne con Conversation
- Incluir relación ManyToOne con User (senderId)
- Usar `encryptedContent` tipo TEXT para almacenar contenido cifrado
- Añadir validaciones apropiadas

### **Prueba 2.2:**
- Reiniciar aplicación
- Verificar que tabla `messages` se crea con las FK correctas
- Inspeccionar relaciones en PostgreSQL

### **Tarea 2.3: Repositories**
- Crear `ConversationRepository.java` con método `findByUserOneIdAndUserTwoId`
- Crear `MessageRepository.java` con método `findByConversationIdOrderBySentAtAsc`
- Añadir método `findTop20ByConversationIdOrderBySentAtDesc` para historial reciente

### **Prueba 2.3:**
- Crear test unitario básico que instancie los repositorios
- Verificar que Spring puede crear los beans sin errores
- Test debe pasar sin tocar la BD

---

## **PASO 3: Sistema de Cifrado**

### **Tarea 3.1: AES Encryption Utility**
- Crear `AESUtil.java` en package `com.gamelyx.util`
- Implementar métodos estáticos `encrypt(String content, String key)` y `decrypt(String encryptedContent, String key)`
- Usar AES-256-GCM para cifrado simétrico seguro
- Manejar excepciones apropiadamente

### **Prueba 3.1:**
- Crear test unitario que cifre un mensaje y lo descifre
- Verificar que el texto original == texto descifrado
- Probar con diferentes longitudes de mensaje
- Verificar que mensajes cifrados son diferentes cada vez (debido al IV)

### **Tarea 3.2: Configuration Properties**
- Añadir property `app.chat.encryption.key` en application.properties
- Crear clase `@ConfigurationProperties` para gestionar la clave
- La clave debe ser de al menos 32 caracteres para AES-256

### **Prueba 3.2:**
- Arrancar aplicación con clave válida
- Arrancar aplicación con clave inválida (debe fallar con error claro)
- Verificar que la clave se lee correctamente desde properties

---

## **PASO 4: Servicios de Chat**

### **Tarea 4.1: ConversationService**
- Crear `ConversationService.java` en package `com.gamelyx.service`
- Implementar `findOrCreateConversation(UUID user1Id, UUID user2Id)`
- El método debe ordenar los IDs automáticamente (menor primero)
- Usar `@Transactional` para operaciones de BD

### **Prueba 4.1:**
- Test unitario que cree conversación entre dos usuarios
- Verificar que calling con (user1, user2) == calling con (user2, user1)
- Verificar que no se crean conversaciones duplicadas

### **Tarea 4.2: MessageService**
- Crear `MessageService.java` en package `com.gamelyx.service`
- Implementar `sendMessage(UUID senderId, UUID recipientId, String content)`
- El mensaje debe cifrarse automáticamente antes de guardar
- Implementar `getConversationMessages(UUID user1Id, UUID user2Id)`
- Los mensajes deben descifrarse automáticamente al recuperar

### **Prueba 4.2:**
- Test de integración que envíe mensaje entre usuarios
- Verificar que el mensaje se guarda cifrado en BD
- Verificar que el mensaje se recupera descifrado correctamente
- Probar recuperar historial de conversación

---

## **PASO 5: WebSocket Controllers**

### **Tarea 5.1: Basic Chat Controller**
- Crear `ChatController.java` en package `com.gamelyx.controller`
- Implementar `@MessageMapping("/chat/send")` que reciba mensajes del cliente
- Implementar `@SendTo("/topic/chat/{conversationId}")` para broadcast
- Integrar con MessageService para persistir mensajes

### **Prueba 5.1:**
- Usar herramienta cliente WebSocket (ej: Postman, navegador DevTools)
- Conectar a `ws://localhost:8080/ws`
- Suscribirse a `/topic/chat/test`
- Enviar mensaje a `/app/chat/send`
- Verificar que el mensaje se reenvía a todos los suscritos

### **Tarea 5.2: Authentication Integration**
- Integrar autenticación JWT en WebSocket connections
- Extraer user information del token en mensajes WebSocket
- Validar que el usuario puede enviar mensajes

### **Prueba 5.2:**
- Intentar enviar mensaje sin JWT token (debe fallar)
- Enviar mensaje con JWT token válido (debe funcionar)
- Verificar que el senderId se extrae correctamente del token

---

## **PASO 6: DTOs y Endpoints REST**

### **Tarea 6.1: Chat DTOs**
- Crear `SendMessageRequest.java` (sin conversationId, solo recipientId + content)
- Crear `MessageResponse.java` (con información descifrada del mensaje)
- Crear `ConversationResponse.java` (información básica de conversación)
- NO incluir conversationId en requests del frontend

### **Prueba 6.1:**
- Validar DTOs con `@Valid` annotations
- Test unitario de serialización/deserialización JSON
- Verificar que campos sensibles no se exponen

### **Tarea 6.2: REST Endpoints Complementarios**
- Crear `GET /api/chat/conversations` - lista conversaciones del usuario
- Crear `GET /api/chat/messages` con parámetros `otherUserId` para obtener historial
- Crear `POST /api/chat/mark-read` para marcar mensajes como leídos
- Todos los endpoints deben usar autenticación JWT

### **Prueba 6.2:**
- Test con Postman/curl para cada endpoint
- Verificar autenticación JWT funciona
- Verificar que usuario solo ve sus propias conversaciones
- Probar edge cases (usuario inexistente, etc.)

---

## **PASO 7: Integration Testing**

### **Tarea 7.1: WebSocket Integration Test**
- Crear test de integración completo WebSocket + BD
- Simular dos usuarios conectados enviando mensajes
- Verificar que mensajes se persisten cifrados
- Verificar que mensajes se broadcasted correctamente

### **Prueba 7.1:**
- Test debe crear usuarios, conversación y mensajes
- Verificar end-to-end: envío WebSocket → cifrado → BD → descifrado → broadcast
- Test debe pasar de forma consistente

### **Tarea 7.2: Performance Testing**
- Configurar límites de rate limiting para mensajes por usuario
- Añadir logging apropiado para debugging
- Optimizar queries si es necesario

### **Prueba 7.2:**
- Enviar múltiples mensajes rápidamente (verificar rate limiting)
- Verificar logs de performance en operaciones de BD
- Medir tiempo de respuesta de endpoints

---

## 🔧 Configuración Final

### **Variables de Entorno Necesarias:**
```
# Cifrado
CHAT_ENCRYPTION_KEY=your-32-character-secret-key-here-minimum-length

# WebSocket (si es diferente del puerto principal)
WEBSOCKET_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:*
```

### **Properties Adicionales:**
```
# application.properties
app.chat.encryption.key=${CHAT_ENCRYPTION_KEY:default-dev-key-not-for-production-32char}
spring.websocket.servlet.allowed-origins=${WEBSOCKET_ALLOWED_ORIGINS:http://localhost:4200}

# Limitar tamaño de mensajes
spring.websocket.servlet.buffer-size=8192
```

## ✅ Criterios de Éxito

El backend estará completo cuando:
1. **WebSocket funcional**: Mensajes se envían/reciben en tiempo real
2. **Cifrado verificado**: Mensajes se almacenan cifrados en BD
3. **Autenticación integrada**: Solo usuarios autenticados pueden usar chat
4. **DTOs sin chatId**: Frontend nunca ve conversation IDs internos
5. **Tests pasando**: Toda funcionalidad tiene tests de integración
6. **Performance aceptable**: Mensajes se procesan < 100ms

## 🚨 Puntos Críticos

- **Cifrado es mandatory**: Nunca almacenar mensajes en texto plano
- **Autenticación WebSocket**: Validar JWT en todas las operaciones
- **Order userIds**: Siempre userOneId < userTwoId para evitar conversaciones duplicadas
- **Error handling**: WebSocket debe manejar desconexiones gracefully
- **Rate limiting**: Prevenir spam de mensajes

---

*Esta guía está optimizada para desarrollo incremental con Claude Code. Cada paso tiene pruebas específicas para validar el progreso antes de continuar.*