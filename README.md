# Gamelyx 🎮

Aplicación web para amantes de los videojuegos desarrollada como Trabajo Fin de Grado.

## 📁 Estructura del Proyecto

```
Gamelyx/
├── frontend/          # Aplicación Angular
├── backend/           # API Spring Boot  
├── docs/              # Documentación
└── README.md          # Este archivo
```

## 🚀 Tecnologías

### Frontend
- **Angular** - Framework principal
- **TypeScript** - Lenguaje de programación
- **SCSS** - Preprocesador CSS
- **Angular Material** - Componentes UI (a implementar)

### Backend
- **Spring Boot** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **MySQL/PostgreSQL** - Base de datos (a definir)
- **Spring Security** - Autenticación y autorización

## 🛠️ Configuración del Entorno de Desarrollo

### Prerrequisitos
- Node.js (v18+)
- Angular CLI
- Java 17+
- Maven
- Git

### Instalación

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/tu-usuario/Gamelyx.git
   cd Gamelyx
   ```

2. **Configurar Frontend**
   ```bash
   cd frontend
   npm install
   ng serve
   ```
   La aplicación estará disponible en `http://localhost:4200`

3. **Configurar Backend**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   La API estará disponible en `http://localhost:8080`

## 📋 Desarrollo por Iteraciones

### Iteración 1: Configuración inicial ✅
- [x] Configuración de entornos
- [x] Estructura base del proyecto
- [ ] Primer endpoint REST
- [ ] Consumo desde Angular

### Iteración 2: Funcionalidad básica
- [ ] Autenticación de usuarios
- [ ] CRUD de videojuegos
- [ ] Interfaz básica

## 🤝 Contribución

Este es un proyecto académico individual, pero cualquier feedback es bienvenido.

## 📝 Licencia

Proyecto académico - Universidad de Jaén

## 👨‍💻 Autor

Rubén Alonso Cruz - Grado en Ingeniería Informática