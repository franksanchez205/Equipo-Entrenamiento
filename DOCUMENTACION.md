# Documentación del Proyecto: Equipo Fútbol ADSO

API REST para gestión de equipo de fútbol con autenticación JWT y registro de resultados de entrenamiento.

---

## Stack Tecnológico

| Tecnología | Versión |
|---|---|
| Spring Boot | 3.2.0 |
| Java | 17 |
| Maven | - |
| MySQL | (conector runtime) |
| JPA/Hibernate | (Spring Boot Starter Data JPA) |
| JWT (jjwt) | 0.12.5 |
| BCrypt (spring-security-crypto) | 6.3.1 |
| Lombok | 1.18.46 |
| SpringDoc OpenAPI | 2.2.0 |
| Validation | (Spring Boot Starter Validation) |

---

## Configuración

### `pom.xml` — `src/main/resources/application.yaml`

- **Puerto:** `9090`
- **Context-path:** `/api/v1/`
- **Base de datos:** MySQL en `localhost:3306/futbol`, usuario `root`, contraseña `12345`
- **JPA:** `ddl-auto: create-drop` (reinicia esquema en cada inicio), dialecto `MySQLDialect`
- **JWT:** Clave secreta Base64: `WCrD7ZwsNH2yZx4d29X3522BEAYTjm7BiNJxi9xksjU=`. Expiración: 600000 ms (10 min)

---

## Estructura del Proyecto

```
src/main/java/com/equipofutbol/equipofutbol_adso/
├── EquipofutbolAdsoApplication.java   # Punto de entrada Spring Boot
├── config/                            # Configuraciones globales
│   ├── AppConfig.java                 # Bean PasswordEncoder (BCrypt)
│   ├── FilterConfig.java              # Registro del filtro JWT
│   └── SecurityContext.java           # Acceso al rol desde cualquier capa
├── controller/                        # Controladores REST
│   ├── AuthController.java            # /auth - register, login, refreshToken
│   ├── ResponseController.java        # /resultados - POST crear resultado
│   └── UserController.java            # /user - CRUD jugadores, top 5
├── dto/                               # Objetos de transferencia de datos
│   ├── EntrenamientoDTO.java          # Datos de un entrenamiento individual
│   ├── JwtResponseDTO.java            # Respuesta con token JWT
│   ├── LoginRequestDTO.java           # Solicitud de login
│   ├── MessageResponseDTO.java        # Respuesta genérica con mensaje
│   ├── RegisterRequestDTO.java        # Solicitud de registro
│   ├── ResultadoRequestDTO.java       # Solicitud de resultado de entrenamiento
│   ├── UserRequestDTO.java            # Solicitud de creación de jugador
│   └── UserResponseDTO.java           # Respuesta con datos del jugador
├── entity/                            # Entidades JPA
│   ├── Users.java                     # Tabla "users"
│   └── Resultados.java                # Tabla "resultados"
├── enums/                             # Enumeraciones
│   └── UserRole.java                  # ADMINISTRATOR, JUGADOR
├── exception/                         # Manejo de excepciones
│   ├── GlobalExceptionHandler.java    # @RestControllerAdvice global
│   └── SecurityAuthorizationException.java # Excepción de autorización
├── filter/                            # Filtros HTTP
│   └── JwtValidationFilter.java       # Validación JWT en cada petición
├── repository/                        # Repositorios JPA
│   ├── EmployeesRepository.java       # CRUD + consultas personalizadas
│   └── ResultadoRepository.java       # Conteo de entrenamientos
└── service/                           # Lógica de negocio
    ├── AuthService.java               # Autenticación y registro
    ├── JwtService.java                # Generación y validación de tokens
    ├── ResultadoService.java          # Registro de resultados
    └── UserService.java               # Gestión de jugadores
```

---

## Endpoints

### Autenticación — `/api/v1/auth`

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/register` | Registrar usuario (rol: 0=ADMIN, 1=JUGADOR) | No |
| POST | `/login` | Login, devuelve JWT | No |
| GET | `/refreshToken` | Refrescar JWT expirado | Bearer |

### Usuarios — `/api/v1/user`

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/` | Crear jugador (solo ADMIN) | No |
| GET | `/` | Top 5 puntajes | No |
| GET | `/top5` | Top 5 puntajes (alternativo) | No |
| GET | `/{id}` | Obtener jugador por número de camiseta | No |

### Resultados — `/api/v1/resultados`

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/` | Registrar resultado de entrenamiento (máx. 3) | No |

### Documentación

| Método | Ruta |
|--------|------|
| GET | `/api/v1/swagger-ui/index.html` |

---

## Entidades

### `Users`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | Long (PK, auto) | Identificador único |
| username | String | Nombre de usuario |
| password | String | Contraseña cifrada con BCrypt |
| role | UserRole (Enum STRING) | ADMINISTRATOR o JUGADOR |
| posicion | String | Posición en el campo |
| numeroCamiseta | Integer | Número de camiseta |
| puntajeTotal | Double | Puntaje promedio del jugador |
| active | Boolean | Si la cuenta está activa |
| createdAt | LocalDateTime | Fecha de creación |
| listResultados | List\<Resultados\> (OneToMany) | Entrenamientos del jugador |

### `Resultados`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | Long (PK, auto) | Identificador único |
| numeroEntrenamiento | Integer | Número del entrenamiento (1, 2 o 3) |
| pasesEfectivos | Double | Cantidad de pases efectivos |
| potenciaTiro | Double | Potencia de tiro |
| velocidadJugador | Double | Velocidad del jugador |
| users | Users (ManyToOne) | Jugador asociado |

---

## Lógica de Negocio

### Cálculo de puntaje de entrenamiento

```
puntajeEntrenamiento = (pasesEfectivos * 0.5) + (velocidadJugador * 0.3) + (potenciaTiro * 0.2)
```

### Cálculo de puntaje total del jugador

```
puntajeTotal = promedio(puntajeEntrenamiento de todos sus entrenamientos)
```

### Límite de entrenamientos

Cada jugador puede tener un máximo de **3 entrenamientos** registrados. Controlado por `ResultadoRepository.countNumeroEntrenamiento()`.

### Obtención del equipo titular (Top 5)

El endpoint `GET /user` y `GET /user/top5` retorna los 5 jugadores con mayor `puntajeTotal`, ordenados de mayor a menor. Cada jugador incluye el detalle de sus entrenamientos con los aportes desglosados:
- `aportePases = pasesEfectivos * 0.5`
- `aporteVelocidad = velocidadJugador * 0.3`
- `aportePotencia = potenciaTiro * 0.2`

---

## Autenticación JWT

### Flujo

1. **Registro:** `POST /auth/register` con username, password y rol (0=ADMIN, 1=JUGADOR). La contraseña se cifra con BCrypt.
2. **Login:** `POST /auth/login` con username y password. Devuelve un JWT con claims: `userId`, `rolId` y subject (username).
3. **Peticiones autenticadas:** El cliente envía el token en el header `Authorization: Bearer <token>`.
4. **Validación:** `JwtValidationFilter` intercepta cada petición, valida el token y establece atributos `username`, `userId`, `rolId` y `role` en la request.
5. **Refresco:** `GET /auth/refreshToken` genera un nuevo token a partir del actual (incluso si expiró, siempre que la firma sea válida).

### Rutas que omiten validación JWT

- `/api/v1/auth/*`
- `/api/v1/swagger*`
- `/api/v1/v3/api-docs*`
- `/api/v1/resultados*`
- `/api/v1/user*` (NOTA: aunque está en `shouldNotFilter`, el método `createUser` verifica el rol ADMIN mediante `SecurityContext`)

### Claims del token

| Claim | Descripción |
|-------|-------------|
| `userId` | ID del usuario en la BD |
| `rolId` | Nombre del rol (ADMINISTRATOR/JUGADOR) |
| `sub` (subject) | Nombre de usuario |
| `iat` | Fecha de emisión |
| `exp` | Fecha de expiración |

---

## Seguridad

- **Contraseñas:** Cifradas con BCrypt mediante `BCryptPasswordEncoder`.
- **JWT:** Firmado con HMAC-SHA usando clave secreta de 256 bits.
- **Autorización:** El servicio `UserService.createUser()` verifica que el rol sea ADMINISTRATOR usando `SecurityContext.getCurrentRole()`.
- **Excepciones:** `GlobalExceptionHandler` captura y formatea errores de validación, `RuntimeException` y `SecurityAuthorizationException`.

---

## Descripción Detallada de Archivos

### `EquipofutbolAdsoApplication.java`
Clase principal con `@SpringBootApplication`. Contiene el método `main()` que arranca la aplicación Spring Boot.

### `config/AppConfig.java`
Configuración global con `@Configuration`. Define un bean `PasswordEncoder` que retorna un `BCryptPasswordEncoder` para cifrado de contraseñas.

### `config/FilterConfig.java`
Registra `JwtValidationFilter` en la cadena de filtros usando `FilterRegistrationBean`. Se aplica a todas las rutas (`/*`) con orden `1` (prioridad máxima).

### `config/SecurityContext.java`
Componente con `@Component` que accede al rol del usuario autenticado desde `RequestContextHolder`. Lee el atributo `"role"` establecido por `JwtValidationFilter` en la petición actual.

### `controller/AuthController.java`
Controlador REST para `/auth`. Expone:
- `POST /register` — Registra un nuevo usuario.
- `POST /login` — Inicia sesión y devuelve JWT.
- `GET /refreshToken` — Refresca un token JWT expirado.

### `controller/ResponseController.java`
Controlador REST para `/resultados`. Expone:
- `POST /` — Crea un resultado de entrenamiento.

### `controller/UserController.java`
Controlador REST para `/user`. Expone:
- `POST /` — Crea un jugador (requiere rol ADMIN).
- `GET /` — Obtiene top 5 jugadores.
- `GET /top5` — Obtiene top 5 jugadores (ruta alternativa).
- `GET /{id}` — Obtiene jugador por número de camiseta.

### `dto/EntrenamientoDTO.java`
DTO con datos de un entrenamiento individual: `numeroEntrenamiento`, `aportePases`, `aporteVelocidad`, `aportePotencia`, `puntajeEntrenamiento`.

### `dto/JwtResponseDTO.java`
DTO para respuesta de login/refresh. Contiene `jwt` (token), `role` (nombre del rol) y `name` (username).

### `dto/LoginRequestDTO.java`
DTO para solicitud de login. Campos: `username` y `password` (ambos `@NotBlank`).

### `dto/MessageResponseDTO.java`
DTO genérico para respuestas con un mensaje de texto. Campo: `message`.

### `dto/RegisterRequestDTO.java`
DTO para solicitud de registro. Campos: `username` (`@NotBlank`), `password` (`@NotBlank`), `rol` (`@NotNull`, Long: 0=ADMIN, 1=JUGADOR).

### `dto/ResultadoRequestDTO.java`
DTO para solicitud de creación de resultado. Campos: `id`, `numeroEntrenamiento`, `pasesEfectivos`, `potenciaTiro`, `velocidadJugador`, `users` (String con número de camiseta).

### `dto/UserRequestDTO.java`
DTO para solicitud de creación de jugador. Campos: `nombre` (`@NotBlank`), `posicion` (`@NotBlank`), `numeroCamiseta` (`@Positive`), `rol` (`@NotNull`), `puntajeTotal`.

### `dto/UserResponseDTO.java`
DTO para respuesta con datos del jugador. Campos: `id`, `nombre`, `posicion`, `numeroCamiseta`, `puntajeTotal`, `entrenamientos` (List\<EntrenamientoDTO\>).

### `entity/Users.java`
Entidad JPA mapeada a la tabla `users`. Incluye relación `@OneToMany` con `Resultados` (cascade ALL, fetch LAZY).

### `entity/Resultados.java`
Entidad JPA mapeada a la tabla `resultados`. Incluye relación `@ManyToOne` con `Users` (fetch LAZY, join column `users_id`).

### `enums/UserRole.java`
Enumeración con dos valores: `ADMINISTRATOR` y `JUGADOR`. El valor numérico 0 corresponde a ADMINISTRATOR y 1 a JUGADOR.

### `exception/GlobalExceptionHandler.java`
Manejador global de excepciones con `@RestControllerAdvice`. Maneja:
- `MethodArgumentNotValidException` — Errores de validación (400).
- `RuntimeException` — Errores genéricos (500).
- `SecurityAuthorizationException` — Errores de autorización (401).

### `exception/SecurityAuthorizationException.java`
Excepción personalizada que extiende `RuntimeException`. Se lanza cuando un usuario intenta acceder a un recurso sin el rol adecuado.

### `filter/JwtValidationFilter.java`
Filtro que extiende `OncePerRequestFilter`. Intercepta cada petición, extrae el token del header `Authorization`, lo valida con `JwtService`, y establece atributos `username`, `userId`, `rolId` y `role` en la request. Si el token falta o es inválido, responde con 401. Omite validación para rutas `/auth`, `/swagger`, `/v3/api-docs` y `/resultados`.

### `repository/EmployeesRepository.java`
Interfaz que extiende `JpaRepository<Users, Long>`. Métodos:
- `findByUsername(String)` — Busca usuario por username.
- `findByNumeroCamiseta(Integer)` — Busca jugador por número de camiseta.
- `existsByNumeroCamiseta(Integer)` — Verifica si existe una camiseta.
- `obtenerPuntajePorCamiseta(Integer)` — Obtiene puntaje por camiseta (JPQL).
- `obtenerPuntaje(Long)` — Obtiene puntaje por ID (JPQL).
- `obtenerPuntajesDeTodosLosJugadores()` — Lista jugadores ordenados por puntaje descendente (JPQL con `WHERE role = 'JUGADOR'`).

### `repository/ResultadoRepository.java`
Interfaz que extiende `JpaRepository<Resultados, Long>`. Método:
- `countNumeroEntrenamiento(Long userId)` — Cuenta entrenamientos de un usuario (JPQL).

### `service/JwtService.java`
Servicio para gestión de tokens JWT. Funcionalidades:
- `getSigninKey()` — Deriva clave HMAC-SHA desde Base64.
- `generateToken(userId, rolId, userName)` — Genera token con claims personalizados.
- `isTokenValid(token)` — Valida firma y expiración.
- `extractClaims(token, resolver)` — Extrae claims genéricamente.
- `extractUsername(token)` — Obtiene subject (username).
- `extractUserId(token)` — Obtiene claim "userId".
- `extractRolId(token)` — Obtiene claim "rolId".
- `extractRole(token)` — Alias de extractRolId.
- `extractEmail(token)` — Retorna el username (por compatibilidad futura).
- `refreshToken(token)` — Genera nuevo token a partir del actual (permite expirado).

### `service/AuthService.java`
Servicio de autenticación. Funcionalidades:
- `register(RegisterRequestDTO)` — Valida unicidad de username, convierte rol numérico a enum, cifra password con BCrypt, guarda usuario.
- `login(LoginRequestDTO)` — Verifica credenciales, estado activo, genera y retorna JWT.
- `refreshToken(String)` — Refresca token y verifica que el usuario aún exista.

### `service/UserService.java`
Servicio de gestión de jugadores. Funcionalidades:
- `createUser(UserRequestDTO)` — Crea jugador (valida rol ADMIN, unicidad de camiseta, asigna rol JUGADOR).
- `getAll()` — Lista todos los usuarios (requiere ADMIN).
- `obtenerPuntajePorCamiseta(Integer)` — Busca jugador por número de camiseta.
- `obtenerTablaDePuntajes()` — Lista jugadores ordenados por puntaje descendente.
- `obtenerTop5()` — Retorna top 5 jugadores con detalle de entrenamientos y desglose de aportes.

### `service/ResultadoService.java`
Servicio de registro de resultados. Funcionalidades:
- `createResultado(ResultadoRequestDTO)` — Busca jugador por camiseta, valida máximo 3 entrenamientos, guarda resultado, actualiza puntaje total del jugador.
- `actualizarPuntajeTotal(Users)` — Calcula y actualiza el puntaje promedio del jugador basado en todos sus entrenamientos.

---

## Pruebas con Postman

El archivo `equipoFutbol.postman_collection.json` contiene una colección completa con:

### Carpeta Auth
- **Register Admin** — `POST /auth/register` — Crea usuario admin (rol: 0).
- **Login Admin** — `POST /auth/login` — Obtiene token JWT.
- **Refresh Token** — `GET /auth/refreshToken` — Refresca token (usa variable `{{jwt_token}}`).

### Carpeta Players
- **Create Player 1-7** — `POST /user` — Crea 7 jugadores con diferentes posiciones y números de camiseta (9, 4, 8, 1, 3, 10, 5).
- **Get Player by Camiseta** — `GET /user/9` — Obtiene jugador por camiseta.
- **Get Top 5** — `GET /user/top5` — Obtiene los 5 mejores jugadores.

### Carpeta Training Results
- **Resultados por jugador** — 3 entrenamientos para cada uno de los 7 jugadores (camisetas: 9, 4, 8, 1, 3, 10, 5), total 21 peticiones POST a `/resultados`.

### Variable de colección
- `jwt_token` — Almacena el token JWT para usarlo en peticiones autenticadas.

---

## Flujo de Uso Típico

1. Iniciar la aplicación Spring Boot.
2. Registrar un administrador: `POST /auth/register` con `rol: 0`.
3. Iniciar sesión como admin: `POST /auth/login`, guardar el token JWT.
4. Crear 7 jugadores: `POST /user` para cada uno (sin auth, el filtro omite `/user`).
5. Registrar 3 entrenamientos por cada jugador: `POST /resultados`.
6. Consultar top 5: `GET /user/top5` — retorna los 5 mejores jugadores con sus puntajes y detalle de entrenamientos.
