# Buscaminas Web — Examen II (Programación IV)

Aplicación web full-stack de **Buscaminas** desarrollada en **Java 17** con **Spring Boot 3**, **Spring Security**, **Spring Data JPA** y **MySQL 8**, con interfaz SPA en **React 18 (CDN)** servida directamente desde el backend en `http://localhost:8080`.

Incluye lógica del tablero 100 % en el servidor (el cliente nunca conoce las coordenadas de las minas mientras la partida está en curso), persistencia del estado del tablero en JSON, ranking global por nivel mediante vista SQL (`vw_ranking`), historial personal, autenticación con sesiones HTTP (`JSESSIONID`) y control de acceso basado en roles (`JUGADOR` y `ADMIN`).

## 📋 Resumen técnico y credenciales de prueba

| Concepto | Detalle |
|---|---|
| **Clase principal** | `com.bachillerato.buscaminas.BuscaminasApplication` |
| **Puerto por defecto** | `8080` (`http://localhost:8080`) |
| **Base de datos** | MySQL 8 — esquema `minas` (`usuario`, `partida` y vista `vw_ranking`) |
| **URL JDBC por defecto** | `jdbc:mysql://localhost:3306/minas?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8` |
| **Credenciales MySQL** | Usuario `root` / contraseña `root` (configurable mediante `DB_USER` y `DB_PASS`) |
| **Cifrado de contraseñas** | `PBKDF2WithHmacSHA256` (sal aleatoria de 16 bytes, **600 000 iteraciones**, llave de 256 bits) |
| **Sesiones y seguridad** | Spring Security con `HttpSession` (`JSESSIONID`) y control RBAC (`ROLE_JUGADOR`, `ROLE_ADMIN`) |

### 👤 Usuarios precargados en `minas.sql`

| Usuario | Correo electrónico | Contraseña | Rol | Acceso |
|---|---|---|---|---|
| `docente` | `docente@minas.local` | `Prueba123$` | `ADMIN` | Juego, ranking, historial y panel de administración |
| `jugador1` | `jugador1@minas.local` | `Prueba123$` | `JUGADOR` | Juego, ranking e historial personal |

> También es posible registrar nuevos jugadores desde la pestaña **Registrarse** en la pantalla de inicio (política mínima: 8 caracteres, al menos una mayúscula, un dígito y un símbolo).

## ⚙️ Requisitos

- **JDK 17** o superior.
- **MySQL Server 8.0** (o MariaDB 10.5+) corriendo en `localhost:3306`.
- **Maven 3.9+** (o el wrapper incluido `mvnw.cmd` / `./mvnw`).
- **IntelliJ IDEA** (Community o Ultimate) y un navegador moderno (Chrome, Edge o Firefox).

## ▶️ Cómo preparar y ejecutar el proyecto

### Paso 1: Crear la base de datos en MySQL

Ejecuta el script **`minas.sql`** ubicado en la raíz del proyecto desde MySQL CLI, MySQL Workbench o IntelliJ Database:

```bash
mysql -u root -p < minas.sql
```

Esto crea la base de datos `minas` con las tablas **`usuario`** y **`partida`**, la vista **`vw_ranking`**, los usuarios de prueba (`docente` y `jugador1`) y una partida de ejemplo terminada en el ranking.

### Paso 2: Configurar credenciales de MySQL (opcional)

`src/main/resources/application.properties` lee las credenciales desde variables de entorno con respaldo local para `root`:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/minas?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8}
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASS:root}
```

Si tu instancia local de MySQL utiliza otra contraseña o usuario, define `DB_USER` y `DB_PASS` en las variables de entorno de ejecución en IntelliJ o en tu terminal antes de arrancar.

### Paso 3: Levantar la aplicación

#### Opción A: IntelliJ IDEA (recomendado)

1. Abre IntelliJ IDEA → **File → Open...** y selecciona la carpeta del proyecto.
2. Verifica que el SDK del proyecto sea **JDK 17** (**File → Project Structure → Project**).
3. Abre `src/main/java/com/bachillerato/buscaminas/BuscaminasApplication.java`.
4. Haz clic derecho sobre el archivo → **Run 'BuscaminasApplication.main()'**.
5. Abre en el navegador: **`http://localhost:8080`**.

#### Opción B: Línea de comandos (Windows / macOS / Linux)

```bash
# En Windows (PowerShell / CMD)
.\mvnw.cmd spring-boot:run

# En Linux / macOS
./mvnw spring-boot:run
```

### Paso 4: Ejecutar pruebas automatizadas

Para verificar la prueba unitaria de apertura en cadena (`PartidaServiceTest`) y la carga de contexto:

```bash
.\mvnw.cmd test
```

## 🔌 Contrato de API REST y correspondencia de rutas

Todos los endpoints del juego, historial y administración exigen sesión autenticada (`401 Unauthorized` si no existe sesión activa).

| Funcionalidad | Ruta de referencia | Ruta implementada en el proyecto | Código HTTP |
|---|---|---|---|
| Registro de usuario | `POST /api/auth/registro` | `POST /api/auth/registro` | `201 Created` |
| Inicio de sesión | `POST /api/auth/login` | `POST /api/auth/login` | `200 OK` |
| Cierre de sesión | `POST /api/auth/logout` | `POST /api/auth/logout` | `204 No Content` (`401` sin sesión) |
| Perfil del usuario actual | `GET /api/auth/me` | `GET /api/auth/me` | `200 OK` |
| Iniciar nueva partida | `POST /api/partidas` | `POST /api/partidas` | `201 Created` (`409` si ya hay activa) |
| Consultar partida en curso | `GET /api/partidas/actual` | `GET /api/partidas/activa` | `200 OK` (`404` si no hay activa) |
| Consultar partida por ID | `GET /api/partidas/{id}` | `GET /api/partidas/{id}` | `200 OK` (`403` si es ajena) |
| Revelar casilla (cascada BFS) | `POST /api/partidas/{id}/acciones` | `POST /api/partidas/{id}/revelar` | `200 OK` |
| Colocar o quitar bandera | `POST /api/partidas/{id}/acciones` | `POST /api/partidas/{id}/bandera` | `200 OK` |
| Abandonar partida en curso | `POST /api/partidas/{id}/abandonar` | `POST /api/partidas/{id}/abandonar` | `200 OK` |
| Historial del jugador actual | `GET /api/usuarios/{id}/partidas` | `GET /api/partidas/historial` | `200 OK` |
| Ranking global por nivel | `GET /api/ranking?nivel=...` | `GET /api/ranking?nivel=...` | `200 OK` |
| Listar usuarios (`ADMIN`) | `GET /api/admin/usuarios` | `GET /api/admin/usuarios` | `200 OK` (`403` sin rol `ADMIN`) |
| Activar/desactivar usuario (`ADMIN`) | `PATCH /api/admin/usuarios/{id}/estado` | `PATCH /api/admin/usuarios/{id}/estado` | `200 OK` (`403` sin rol `ADMIN`) |

## 🗂️ Estructura del proyecto

| Ruta | Descripción |
|---|---|
| `minas.sql` | Script DDL + DML idempotente con la base de datos `minas` (`usuario`, `partida`, `vw_ranking`). |
| `src/main/java/.../controller/` | Controladores REST (`AuthController`, `PartidaController`, `RankingController`, `AdminController`). |
| `src/main/java/.../service/` | Lógica de negocio: motor de Buscaminas, apertura en cadena BFS, puntuación y ranking. |
| `src/main/java/.../security/` | `SecurityConfig`, `CustomUserDetailsService` y `Pbkdf2PasswordEncoder` (600 000 iteraciones). |
| `src/main/java/.../model/` | Entidades JPA (`Usuario`, `Partida`, `EstadoPartida`) y DTOs de petición/respuesta. |
| `src/main/java/.../repository/` | Repositorios Spring Data JPA con bloqueo pesimista (`SELECT ... FOR UPDATE`) y consulta a `vw_ranking`. |
| `src/main/java/.../exception/` | Manejador global de excepciones (`GlobalExceptionHandler`) con respuestas JSON uniformes. |
| `src/main/resources/static/` | Frontend SPA en React 18 CDN (`index.html`, `assets/styles.css`, `assets/app.js`) sin Node/Vite. |
| `src/test/java/.../` | Pruebas automatizadas (`BuscaminasApplicationTests` y `PartidaServiceTest`). |

## 🎮 Niveles de dificultad y reglas del servidor

| Nivel | Dimensiones | Minas | Características |
|---|---|---|---|
| **Principiante** | `9 × 9` | `10` | Primer clic siempre seguro (las minas se colocan tras el primer movimiento). |
| **Intermedio** | `16 × 16` | `40` | Apertura automática en cascada (BFS) para celdas sin minas adyacentes. |
| **Experto** | `16 × 30` | `99` | Estado persistido en columna `JSON`: permite recargar y reanudar partidas `EN_CURSO`. |

## 📝 Notas de arquitectura y seguridad

- **Integridad del tablero:** Las coordenadas de las minas nunca viajan al cliente mientras la partida está `EN_CURSO`; únicamente se revelan al finalizar (`TERMINADA` o `ABANDONADA`).
- **Control de acceso (RBAC):** Las rutas `/api/admin/**` están protegidas en `SecurityFilterChain` exigiendo `ROLE_ADMIN`. Un usuario desactivado (`activo = false`) no puede iniciar sesión ni continuar operando.
- **Concurrencia:** Se emplea bloqueo pesimista (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) sobre `usuario` al crear partidas y bloqueo optimista (`@Version`) en `partida` para evitar condiciones de carrera.