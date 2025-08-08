# Gamelyx 🎮

Aplicación web para amantes de los videojuegos desarrollada como Trabajo Fin de Grado.

Una plataforma social gaming donde los usuarios pueden descobrir, reseñar y gestionar su biblioteca personal de videojuegos, así como conectar con otros gamers que comparten sus mismos gustos.

## 📁 Estructura del Proyecto

```
Gamelyx/
├── frontend/          # Aplicación Angular 20 (Standalone Components)
├── backend/           # API Spring Boot 3.5.3
├── docs/              # Documentación y arquitectura
└── README.md          # Este archivo
```

## 🚀 Tecnologías

### Frontend
- **Angular 20** - Framework principal con Standalone Components
- **TypeScript** - Lenguaje de programación
- **Tailwind CSS v4** - Framework de estilos utilitarios
- **ngx-translate** - Internacionalización (ES/EN)
- **Signals** - Estado reactivo moderno de Angular

### Backend  
- **Spring Boot 3.5.3** - Framework principal
- **Spring Security** - Autenticación JWT + Guards funcionales
- **Spring Data JPA** - Persistencia de datos
- **PostgreSQL** - Base de datos con UUIDs
- **Thymeleaf** - Templates para emails de verificación




## 🛠️ Configuración del Entorno de Desarrollo

### Prerrequisitos
- Node.js (v18+)
- Angular CLI
- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Git



## 📋 Desarrollo por Iteraciones

### ✅ Iteración 1: Arranque del Proyecto 
- [x] Investigación tecnológica y benchmarking
- [x] Definición del alcance del proyecto
- [x] Primera implementación técnica demostrativa
- [x] **HU-01 Landing Page** - Página principal completamente responsive

### ✅ Iteración 2: Sistema de Autenticación 
- [x] **HU-02 Registro** - Sistema completo con validaciones
- [x] **HU-03 Login** - Autenticación flexible (email/username)  
- [x] **HU-04 Logout** - Gestión segura de tokens JWT
- [x] **HU-06 Home** - Dashboard básico protegido por guards
- [x] **HU-05 Google OAuth** - Integración con Google Sign-In
- [x] **HU-07 Idiomas** - Sistema i18n completo (ES/EN)

### ✅ Iteración 3: Gestión de Juegos 
- [x] **HU-08 Buscar Juegos** - Sistema de búsqueda con API externa
- [ ] **HU-09 Página del Juego** - Detalles completos y responsive  
- [ ] **HU-10 Añadir Reseña** - Sistema de calificaciones y comentarios
- [ ] **HU-11 Editar Reseña** - Modificación de reseñas propias
- [ ] **HU-12 Estados de Juego** - Wishlist, Jugando, Completado, etc.

### ⏳ Iteración 4: Despliegue a Producción
- [ ] **HU-13 Despliegue** - Aplicación disponible 24/7

### ⏳ Iteración 5: Sistema Social 
- [ ] **HU-16 Buscar Usuarios** - Sistema de búsqueda por nombre
- [ ] **HU-17 Añadir Amigos** - Solicitudes de amistad
- [ ] **HU-18 Lista de Amigos** - Gestión de contactos
- [ ] **HU-19 Eliminar Amigos** - Administración completa

### ⏳ Iteración 6: Funcionalidades Sociales
- [ ] **HU-20 Recomendaciones** - Algoritmo de sugerencia de usuarios
- [ ] **HU-21 Chat en Tiempo Real** - Mensajería privada con WebSockets

### ⏳ Iteración 7: Refinamiento Final 
- [ ] **HU-15 App Móvil** - Versión para Android
- [ ] Rediseños finales de interfaz
- [ ] Optimizaciones de rendimiento
- [ ] Completar criterios pendientes

### ⏳ Iteración 8: Cierre del Proyecto
- [ ] Incorporación de feedback del tutor
- [ ] Preparación de la defensa
- [ ] Documentación final completa


## 🤝 Contribución

Este es un proyecto académico individual desarrollado siguiendo metodologías ágiles con iteraciones flexibles.

## 📝 Licencia

Proyecto académico - Universidad de Jaén

## 👨‍💻 Autor

**Rubén Alonso Cruz**  
Grado en Ingeniería Informática  
Universidad de Jaén

---