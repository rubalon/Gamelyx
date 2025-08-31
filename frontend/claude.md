# Gamelyx Frontend - Guía de Desarrollo

## 🎯 CONTEXTO DEL PROYECTO
- **Framework**: Angular 20 (Standalone Components)
- **Estado**: Implementando caracteristicas sociales en el home
- **Arquitectura**: Signal-based + Functional Guards
- **Estilos**: Tailwind CSS v4
- **i18n**: ngx-translate (ES/EN)

## 🧠 FILOSOFÍA DE DESARROLLO

### Simplicidad > Escalabilidad
- **Variables globales** Siempre que sea posible 
- **Soluciones pragmáticas** para avanzar rápido
- **Refactoring posterior** cuando sea necesario

### Componentes Modulares
- **Si componente > 500 líneas** → dividir en subcomponentes
- **Si componente se beneficia enormemente de una subdivision** → dividir en subcomponentes
- **Composición simple** sobre herencia compleja

## 🚨 REGLAS OBLIGATORIAS - ANGULAR 20

### ✅ **ESTRUCTURA**: Cada componente DEBE tener .ts .html .scss separados 
### - Excepción: componentes < 80 líneas pueden ser solo .ts

### ✅ Sintaxis Moderna (OBLIGATORIO)
```typescript
// ✅ CORRECTO - Angular 20
@if (isLoggedIn) {
  <div>Bienvenido</div>
} @else {
  <div>Inicia sesión</div>
}

@for (item of items; track item.id) {
  <div>{{ item.name }}</div>
}

// ❌ PROHIBIDO - Sintaxis obsoleta
*ngIf="isLoggedIn"
*ngFor="let item of items"
```



### ✅ Nomenclatura (OBLIGATORIO)
```typescript
// ✅ CORRECTO - Sin sufijos
user-profile.ts
auth-modal.ts
header.ts

// ❌ PROHIBIDO - Con sufijos obsoletos
user-profile.component.ts
auth-modal.component.ts
header.component.ts
```

### ✅ Input/Output Signals (OBLIGATORIO)
```typescript
// ✅ CORRECTO - Signal-based Input/Output moderno
import { Component, input, output, model } from '@angular/core';

@Component({
  selector: 'app-user-card',
  standalone: true
})
export class UserCard {
  // Input signals (read-only)
  username = input<string>('');
  email = input.required<string>();
  
  // Output signals  
  userClick = output<User>();
  deleteUser = output<string>();
  
  // Model signals (two-way binding)
  isActive = model<boolean>(false);
  
  // Computed signals derivados
  displayName = computed(() => 
    this.username() || this.email().split('@')[0]
  );
  
  onUserClick() {
    this.userClick.emit(this.user());
  }
}

// ❌ PROHIBIDO - Decorators obsoletos
@Input() username: string = '';
@Output() userClick = new EventEmitter<User>();
```

### ✅ Signals para Estado (OBLIGATORIO)
```typescript
// ✅ CORRECTO - Signals para estado de UI
@Component({})
export class MyComponent {
  // Estado local con signals
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  
  // Computed signals
  hasError = computed(() => this.errorMessage() !== null);
  canSubmit = computed(() => !this.isLoading() && !this.hasError());
  
  // Effects para side effects
  constructor() {
    effect(() => {
      if (this.isLoading()) {
        console.log('Loading started...');
      }
    });
  }
}

// ❌ PROHIBIDO - Observables para estado simple de UI
private loadingSubject = new BehaviorSubject(false);
public isLoading$ = this.loadingSubject.asObservable();
```

## 🎨 ESTILOS Y DISEÑO

### Tailwind CSS v4 Primero
- **Usar Tailwind** para el 95% del styling
- **CSS/SCSS** solo para casos muy específicos
- **Variables CSS custom** cuando Tailwind no sea suficiente
- **Estilo basado en gradientes** 

### ⚠️ IMPORTANTE: Tailwind v4 + SCSS Limitaciones
- **❌ PROHIBIDO**: Usar `@apply` en archivos SCSS
- **✅ PERMITIDO**: Clases Tailwind directamente en HTML/templates
- **✅ SOLUCIÓN**: Si necesitas Tailwind en SCSS, usar valores RGB equivalentes:
```scss
// ❌ PROHIBIDO - Causa errores
.my-class {
  @apply bg-gray-800 text-white rounded-lg;
}

// ✅ CORRECTO - Usar valores RGB equivalentes  
.my-class {
  background-color: rgb(31, 41, 55); /* bg-gray-800 */
  color: rgb(255, 255, 255); /* text-white */
  border-radius: 0.5rem; /* rounded-lg */
}
```

```typescript
// ✅ CORRECTO - Tailwind preferido
@Component({
  template: `
    <div class="flex items-center justify-between p-4 bg-white shadow-lg rounded-lg">
      <h2 class="text-xl font-bold text-gray-800">{{ title() }}</h2>
      <button class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors">
        Click me
      </button>
    </div>
  `
})

// ⚠️ SCSS solo para casos específicos
// user-card.scss
.custom-gradient {
  background: linear-gradient(45deg, var(--primary-color), var(--secondary-color));
}
```


## 🔗 LLAMADAS API Y DATOS

### Principio de Responsabilidad Mínima
```typescript
// ✅ CORRECTO - El componente mas pequeño que tiene los datos hace la llamada
@Component({})
export class UserProfileCard {
  private authStore = inject(AuthStore);
  private userApi = inject(UserApi);
  
  userId = input.required<string>();
  
  // Este componente tiene userId, por lo tanto hace la llamada
  user = signal<User | null>(null);
  
  ngOnInit() {
    this.loadUser();
  }
  
  private loadUser() {
    this.userApi.getUserById(this.userId()).subscribe(user => {
      this.user.set(user);
    });
  }
}

// ❌ No esta prohibido pero es poco deseable - Pasar funciones del padre
@Component({})
export class UserList {
  // ❌ NO hacer esto si el hijo puede hacer la llamada directamente
  @Output() loadUser = new EventEmitter<string>();
}
```

### Stores y APIs
```typescript
// Siempre a través de stores/services especializados
private authStore = inject(AuthStore);    // Para autenticación
private socialStore = inject(SocialStore); // Para social features
private userApi = inject(UserApi);         // Para llamadas directas de user
```

## 📁 ESTRUCTURA PREFERIDA

### Organización de Componentes
```
src/app/
├── core/
│   ├── stores/
│   │   ├── auth-store.ts
│   │   └── social-store.ts
│   └── api/
│       ├── user-api.ts
│       └── social-api.ts
├── features/
│   ├── home/
│   │   ├── home-page       // Página principal ( integra todos los componentes del home)
│   │   └── components       // componentes del home
│   │   
│   └── games/  // otra parte de la app que no es tan importante en este punto
│      
└── shared/
    └── components/
        ├── avatar
        ├── footer
        ├── header
        ├── star-rating
        ├── ...
        └── contact-user-card             

```

## 🎮 PATRONES ESPECÍFICOS GAMELYX

### Estados de Loading/Error
```typescript
// Patrón estándar para todos los componentes
@Component({})
export class MyComponent {
  isLoading = signal(false);
  error = signal<string | null>(null);
  
  // Computed signals derivados
  hasError = computed(() => this.error() !== null);
  canInteract = computed(() => !this.isLoading() && !this.hasError());
  
  async loadData() {
    this.isLoading.set(true);
    this.error.set(null);
    
    try {
      const result = await this.api.getData();
      // Handle success
    } catch (err) {
      this.error.set('Error loading data');
    } finally {
      this.isLoading.set(false);
    }
  }
}
```

### Formularios Reactivos con Signals
```typescript
// Integración de signals con reactive forms
@Component({})
export class AuthModal {
  // Form tradicional
  loginForm = this.fb.group({
    emailOrUsername: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });
  
  // Estados con signals
  isSubmitting = signal(false);
  submitError = signal<string | null>(null);
  
  // Computed para validaciones
  canSubmit = computed(() => 
    this.loginForm.valid && !this.isSubmitting()
  );
}
```

## 🌐 INTERNACIONALIZACIÓN

### Traducciones Obligatorias
- **Todos los textos** deben estar en archivos i18n
- **ES y EN** como mínimo
- **Errores** también traducibles

```typescript
// ✅ CORRECTO - Textos traducidos
@Component({
  template: `
    <h1>{{ 'auth.welcome' | translate }}</h1>
    <p>{{ 'auth.subtitle' | translate }}</p>
  `
})

// ❌ PROHIBIDO - Textos hardcodeados
@Component({
  template: `
    <h1>Welcome to Gamelyx</h1>
    <p>Join the gaming community</p>
  `
})
```

## 🚫 PROHIBICIONES ABSOLUTAS

### ❌ JAMÁS usar sintaxis obsoleta
- `*ngIf`, `*ngFor`, `*ngSwitch`
- `@Input()`, `@Output()` decorators
- `ngOnChanges` lifecycle hook
- Sufijos en nombres de archivo

### ❌ JAMÁS para estado de UI
- `BehaviorSubject` para estado simple
- `Observable` cuando Signal es suficiente
- `ngOnInit` para inicializar signals

### ❌ JAMÁS hardcodear
- Textos sin traducir
- URLs de API
- Colores en templates (usar CSS variables)

## 💬 INSTRUCCIONES PARA CLAUDE

### Cómo trabajar conmigo:
- **PASOS PEQUEÑOS**: Implementa una funcionalidad a la vez
- **EXPLICA TODO**: Qué haces y por qué en cada paso
- **PREGUNTA PRIMERO**: Antes de cambios arquitectónicos
- **SIGUE PATRONES**: Usa ejemplos del código existente

### Proceso de implementación:
1. **Analizar**: Entender requerimiento completo
2. **Planificar**: Qué archivos tocar, qué crear
3. **Implementar**: Siguiendo patrones establecidos
4. **Explicar**: Qué hiciste y por qué
5. **Verificar**: Que funciona y no rompe nada

### Para nuevos componentes:
1. **Verificar tamaño**: ¿Necesita dividirse?
2. **Ubicación correcta**: ¿Feature o shared?
4. **API calls**: ¿Este componente debe hacerlas?


### Al implementar nuevas features:
- **Reutilizar AuthStore** como ejemplo para otros stores
- **Seguir patrón** de auth-modal para otros formularios  
- **Mantener** guards funcionales para protección
- **Usar** misma estructura de carpetas

---

**RECUERDA**: Este proyecto prioriza **simplicidad y velocidad de desarrollo**. Las optimizaciones vendrán después. Código que funciona > código perfecto.

**IMPORTANTE**: Cuando veas que un componente crece mucho, **para inmediatamente** y pregúntame cómo dividirlo antes de continuar.

# 🎮 Gamelyx Social API - Guía ENPOINTS para social


## 🏠 **1. HOME SOCIAL DATA**

### **GET** `/home-social-data`
Obtiene toda la información inicial para el dashboard social.

**Response:**
```typescript
interface HomeSocialDataDto {
  friends: ContactUserDto[];           // Lista de amigos
  incomingRequests: FriendRequestDto[];  // Solicitudes recibidas
  outgoingRequests: FriendRequestDto[];  // Solicitudes enviadas
  preferredGames: PreferredGameDto[];      // Juegos favoritos (rating >= 7)
}

interface ContactUserDto {
  user: { userId: string, username: string };
  chatId: string | null;
  newMessages: boolean;
}

interface FriendRequestDto {
  requestId: string;
  contactUser: ContactUserDto;
  source: "SEARCH" | "SUGGESTION";
  sharedGame: SharedGameInfoDto | null;  // Solo si source = "SUGGESTION"
  receivedAt: string;
}
```

---

## 👥 **2. GESTIÓN DE SOLICITUDES**

### **POST** `/friend-requests`
Envía una solicitud de amistad.

**Request:**
```typescript
{
  targetUserId: string,
  source: "SEARCH" | "SUGGESTION",
  gameSlug?: string,     // Solo si source = "SUGGESTION"
  yourRating?: number    // Solo si source = "SUGGESTION"
}
```

### **PUT** `/friend-requests/{requestId}/respond`
Responde a una solicitud recibida.

**Query Params:**
- `action`: `"ACCEPT"` o `"REJECT"`

### **PUT** `/friend-requests/{requestId}/mark-notified`
Marca una solicitud como vista (para outgoing requests).

---

## 🔍 **3. BÚSQUEDA DE USUARIOS**

### **GET** `/search/users`
Busca usuarios por nombre/username.

**Query Params:**
- `q`: string (mínimo 2 caracteres)
- `limit`: number (opcional, default: 10)

**Response:**
```typescript
{
  query: string,
  users: Array<{
    userId: string,
    username: string
  }>
}
```

---

## 🎮 **4. SUGERENCIAS DE AMIGOS**

### **GET** `/friend-suggestion/by-game`
Obtiene UNA sugerencia basada en un juego específico.

**Query Params:**
- `gameSlug`: string
- `userRating`: number (1-10)

**Response:**
```typescript
{
  user: { userId: string, username: string },
  gameSlug: string,
  yourRating: number,
  theirRating: number
} | null  // 204 No Content si no hay sugerencias
```

### **POST** `/friend-suggestion/reject`
Rechaza una sugerencia específica.

**Query Params:**
- `rejectedUserId`: string (UUID)
- `gameSlug`: string

---

## 🗑️ **5. GESTIÓN DE AMIGOS**

### **DELETE** `/friends/{friendId}`
Elimina un amigo (bidireccional).

**Response:**
```typescript
{
  success: boolean,
  deletedFriendUsername: string,
  deletedFriendId: string
}
```

---

## 🚀 **Flujo de Uso Típico**

### **1. Carga Inicial del Home**
```typescript
// Cargar datos completos del home
const homeData = await fetch('/api/social/home-social-data');
```

### **2. Búsqueda de Usuarios**
```typescript
// Buscar usuarios
const results = await fetch(`/api/social/search/users?q=${query}&limit=10`);

// Enviar solicitud
await fetch('/api/social/friend-requests', {
  method: 'POST',
  body: JSON.stringify({
    targetUserId: userId,
    source: 'SEARCH'
  })
});
```

### **3. Gestión de Solicitudes**
```typescript
// Aceptar solicitud
await fetch(`/api/social/friend-requests/${requestId}/respond?action=ACCEPT`, {
  method: 'PUT'
});

// Marcar como vista (para outgoing requests)
await fetch(`/api/social/friend-requests/${requestId}/mark-notified`, {
  method: 'PUT'
});
```

### **4. Sugerencias por Juego**
```typescript
// Obtener sugerencia
const suggestion = await fetch(`/api/social/friend-suggestion/by-game?gameSlug=zelda&userRating=9`);

// Rechazar sugerencia
await fetch(`/api/social/friend-suggestion/reject?rejectedUserId=${userId}&gameSlug=zelda`, {
  method: 'POST'
});
```

---

## 📊 **Estados y Códigos HTTP**

- **200 OK**: Operación exitosa
- **204 No Content**: Sin sugerencias disponibles
- **400 Bad Request**: Datos inválidos
- **401 Unauthorized**: Token JWT inválido
- **404 Not Found**: Recurso no encontrado

---

## 💡 **Notas Importantes**

1. **Outgoing Requests**: Incluyen PENDING + (ACCEPTED/REJECTED no notificadas)
2. **Shared Game Info**: Solo presente en solicitudes por SUGGESTION
3. **Contact User**: Siempre incluye userId y username para referencias
4. **Real-time**: Considera usar WebSockets para notificaciones en tiempo real
5. **Pagination**: Para búsquedas grandes, usar el parámetro `limit`

---

## 🔧 **Integración con Angular 20**

### **Service Pattern Recomendado**
```typescript
@Injectable({ providedIn: 'root' })
export class SocialApiService {
  private baseUrl = 'http://localhost:8080/api/social';
  private http = inject(HttpClient);
  
  getHomeSocialData(): Observable<HomeSocialDataDto> {
    return this.http.get<HomeSocialDataDto>(`${this.baseUrl}/home-social-data`);
  }
  
  searchUsers(query: string, limit = 10): Observable<UserSearchDto> {
    return this.http.get<UserSearchDto>(`${this.baseUrl}/search/users`, {
      params: { q: query, limit: limit.toString() }
    });
  }
  
  sendFriendRequest(request: FriendRequestDto): Observable<any> {
    return this.http.post(`${this.baseUrl}/friend-requests`, request);
  }
}
```

### **Store Pattern con Signals**
```typescript
@Injectable({ providedIn: 'root' })
export class SocialStore {
  private _socialData = signal<HomeSocialDataDto | null>(null);
  public readonly socialData = this._socialData.asReadonly();
  
  private socialApi = inject(SocialApiService);
  
  loadHomeSocialData(): void {
    this.socialApi.getHomeSocialData().subscribe({
      next: data => this._socialData.set(data),
      error: err => console.error('Error loading social data:', err)
    });
  }
}
```

### **Component Integration**
```typescript
@Component({
  selector: 'app-social-dashboard',
  standalone: true,
  imports: [CommonModule]
})
export class SocialDashboard {
  private socialStore = inject(SocialStore);
  
  socialData = this.socialStore.socialData;
  
  ngOnInit(): void {
    this.socialStore.loadHomeSocialData();
  }
}
```

---

## 🎯 **Error Handling Pattern**

```typescript
// Error handling centralizado
export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
}

// En el service
sendFriendRequest(request: FriendRequestDto): Observable<any> {
  return this.http.post(`${this.baseUrl}/friend-requests`, request)
    .pipe(
      catchError(this.handleError),
      retry(1)
    );
}

private handleError = (error: HttpErrorResponse): Observable<never> => {
  let errorMessage = 'Unknown error occurred';
  
  if (error.status === 401) {
    errorMessage = 'Unauthorized - Please login again';
  } else if (error.status === 400) {
    errorMessage = 'Invalid request data';
  }
  
  return throwError(() => new Error(errorMessage));
};
```

---

**💡 Tip**: Esta API está diseñada para ser consumida por el frontend de Gamelyx. Usa los patterns establecidos de Angular 20 con Signals y servicios inyectados para mantener consistencia con la arquitectura existente.