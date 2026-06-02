# Agente: Generador de Proyecto Equipo Fútbol (Spring Boot)

Genera un proyecto Spring Boot 4.0.6 con Java 17 para gestión de equipo de fútbol, con autenticación JWT y registro de resultados de entrenamiento.

## Stack
- **Spring Boot 4.0.6** / **Java 17** / **Maven**
- **MySQL** con JPA/Hibernate (MySQLDialect, ddl-auto: create-drop)
- **JWT** (jjwt 0.12.5) + **BCrypt** (spring-security-crypto 6.3.1)
- **Lombok 1.18.46**, **SpringDoc OpenAPI 2.2.0**, **Validation**
- **URL repositorio GIT con la solución.**
- **el algoritmo deberá generar un listado con los 5 jugadores titulares para el partido con su      respectiva puntuación. 
Consideraciones: 
● Los resultados de cada entrenamiento se enviarán en formato JSON. ● El equipo cuenta con 7 jugadores pero sólo juegan 5 por partido (podría escalar a un equipo de fútbol 11). 
● El listado de jugadores titulares se deberá retornar en formato JSON. ● El algoritmo sólo retornará el equipo titular si se realizaron los 3 entrenamientos de la semana, en caso contrario deberá retornar un mensaje diciendo que no hay suficiente información.
● Se deberán generar 2 endpoints, uno para almacenar la información de cada entrenamiento y otro para obtener el equipo titular.**
- **pruebas para POSTMAN**


## Estructura

- Server port: `9090`, context-path: `/api/v1/`

## Estructura de paquetes

```
src/main/java/com/equipofutbol/equipofutbol_adso/
├── EquipofutbolAdsoApplication.java      # @SpringBootApplication entry point
├── config/
│   ├── AppConfig.java                     # @Bean PasswordEncoder (BCrypt)
│   ├── FilterConfig.java                  # FilterRegistrationBean para JwtValidationFilter (/*, order=1)
│   └── SecurityContext.java               # @Component, getCurrentRole() desde RequestAttributes
├── controller/
│   ├── AuthController.java                # /auth (register, login, refreshToken)
│   ├── ResponseController.java            # /resultados (POST createResultado)
│   └── UserController.java                # /user (POST create, GET top5, GET /{id}, GET all)
├── dto/
│   ├── EntrenamientoDTO.java              # numeroEntrenamiento, aportePases, aporteVelocidad, aportePotencia, puntajeEntrenamiento
│   ├── JwtResponseDTO.java                # jwt, role, name (all args constructor)
│   ├── LoginRequestDTO.java               # @NotBlank username, password
│   ├── MessageResponseDTO.java            # message (getter/setter + all/no-args constructor)
│   ├── RegisterRequestDTO.java            # @NotBlank username, password; @NotNull rol
│   ├── ResultadoRequestDTO.java           # id, numeroEntrenamiento, pasesEfectivos, potenciaTiro, velocidadJugador, users
│   ├── UserRequestDTO.java                # @NotBlank nombre, posicion; @Positive numeroCamiseta; @NotNull rol; puntajeTotal
│   └── UserResponseDTO.java               # id, nombre, posicion, numeroCamiseta, puntajeTotal, List<EntrenamientoDTO> entrenamientos
├── entity/
│   ├── Users.java                     # @Entity @Table("Users") - id, username, password, @Enumerated(STRING) role, posicion, numeroCamiseta, puntajeTotal, active, createdAt, @OneToMany listResultados
│   └── Resultados.java                    # @Entity @Table("resultados") - id, numeroEntrenamiento, pasesEfectivos, potenciaTiro, velocidadJugador, @ManyToOne(fetch=LAZY) @JoinColumn("users_id") users
├── enums/
│   └── UserRole.java                      # ADMINISTRATOR, JUGADOR
├── exception/
│   ├── GlobalExceptionHandler.java        # @RestControllerAdvice: handle Validation, RuntimeException, SecurityAuthorizationException
│   └── SecurityAuthorizationException.java # extends RuntimeException
├── filter/
│   └── JwtValidationFilter.java           # extends OncePerRequestFilter; skip /auth, /swagger, /v3/api-docs, /resultados, /user
├── repository/
│   ├── EmployeesRepository.java           # JpaRepository - findByCamiseta, findByUsername, obtenerPuntajePorCamiseta, obtenerPuntajesDeTodosLosJugadores, existsByNumeroCamiseta, obtenerPuntaje
│   └── ResultadoRepository.java           # JpaRepository - countNumeroEntrenamiento
└── service/
    ├── AuthService.java                   # register (crea Employees con BCrypt), login (valida + genera JWT), refreshToken
    ├── JwtService.java                    # generateToken, isTokenValid, extractClaims, extractUsername/UserId/RolId/Role, refreshToken
    ├── ResultadoService.java              # createResultado (valida camiseta, max 3 entrenamientos)
    └── UserService.java                   # createUser, getAll, obtenerTablaDePuntajes, obtenerTop5, obtenerPuntajePorCamiseta

src/test/java/com/equipofutbol/equipofutbol_adso/service/
├── AuthServiceTest.java
├── JwtServiceTest.java
├── ResultadoServiceTest.java
└── UserServiceTest.java
```

## application.yaml

```yaml
spring:
  application:
    name: equipofutbol-adso
  datasource:
    url: jdbc:mysql://localhost:3306/futbol
    username: root
    password: 12345
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        "[format_sql]": true
        dialect: org.hibernate.dialect.MySQLDialect

server:
  port: 9090
  servlet:
    context-path: /api/v1/

security:
  jwt:
    secret-key: WCrD7ZwsNH2yZx4d29X3522BEAYTjm7BiNJxi9xksjU=
    token-expiration: 600000
```

## Endpoints

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/v1/auth/register` | Registrar usuario (rol: 0=ADMIN, 1=JUGADOR) | No |
| POST | `/api/v1/auth/login` | Login, devuelve JWT | No |
| GET | `/api/v1/auth/refreshToken` | Refrescar JWT | Bearer |
| POST | `/api/v1/user` | Crear jugador (rol debe ser JUGADOR) | No (skip en filter) |
| GET | `/api/v1/user` | Top 5 puntajes | No |
| GET | `/api/v1/user/top5` | Top 5 puntajes | No |
| GET | `/api/v1/user/{id}` | Obtener jugador por número camiseta | No |
| POST | `/api/v1/resultados` | Registrar resultado entrenamiento (max 3) | No |
| GET | `/api/v1/swagger-ui/index.html` | Swagger UI | No |

## Lógica de negocio clave

- **Puntaje entrenamiento**: `(pasesEfectivos * 0.5) + (velocidadJugador * 0.3) + (potenciaTiro * 0.2)`
- **Puntaje total jugador**: promedio de `puntajeEntrenamiento` de todos sus entrenamientos
- **Máximo 3 entrenamientos** por jugador (controlado por `countNumeroEntrenamiento`)
- **Roles**: 0 = ADMINISTRATOR, 1 = JUGADOR (se pasan como Long en el DTO y se convierten con `UserRole.values()[intValue]`)

## Dependencias Maven (pom.xml)

```xml
<!-- Spring Boot Starters -->
spring-boot-starter-web, spring-boot-starter-data-jpa,
spring-boot-starter-validation, spring-boot-starter-actuator,
spring-boot-starter-test (test), spring-boot-devtools (runtime/optional)

<!-- Security -->
spring-security-crypto:6.3.1

<!-- JWT -->
jjwt-api:0.12.5, jjwt-impl:0.12.5 (runtime), jjwt-jackson:0.12.5 (runtime)

<!-- Database -->
mysql-connector-j (runtime)

<!-- Lombok -->
lombok:1.18.46 (optional)

<!-- Docs -->
springdoc-openapi-starter-webmvc-ui:2.2.0
```

## Orden de creación

1. `pom.xml` con dependencias + `EquipofutbolAdsoApplication.java`
2. `application.yaml`
3. `enums/UserRole.java`
4. `entity/Employees.java` + `entity/Resultados.java`
5. `repository/EmployeesRepository.java` + `repository/ResultadoRepository.java`
6. `dto/` — todos los DTOs
7. `exception/SecurityAuthorizationException.java` + `GlobalExceptionHandler.java`
8. `service/JwtService.java` + `AuthService.java` + `UserService.java` + `ResultadoService.java`
9. `config/AppConfig.java` + `SecurityContext.java`
10. `filter/JwtValidationFilter.java`
11. `config/FilterConfig.java`
12. `controller/AuthController.java` + `UserController.java` + `ResponseController.java`
13. Tests unitarios para cada servicio
