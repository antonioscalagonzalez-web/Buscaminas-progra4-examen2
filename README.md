# Buscaminas Web - Examen II (Programacion IV)

Aplicacion web de Buscaminas desarrollada en Java 17 con Spring Boot 3, Spring Security, Spring Data JPA y MySQL 8, con interfaz frontend en React 18 cargada desde CDN y servida directamente desde el backend en `http://localhost:8080`.

Toda la logica del tablero se ejecuta en el servidor (el cliente no recibe las coordenadas de las minas mientras la partida esta en curso). El estado de cada partida se persiste en MySQL en formato JSON, y se incluyen historial por jugador, ranking global por nivel mediante vista SQL (`vw_ranking`), autenticacion por sesiones HTTP (`JSESSIONID`) y control de acceso por roles (`JUGADOR` y `ADMIN`).

## Configuracion general y credenciales

- **Clase principal:** `com.bachillerato.buscaminas.BuscaminasApplication`
- **Puerto del servidor:** `8080` (`http://localhost:8080`)
- **Base de datos:** MySQL 8, esquema `minas` (tablas `usuario`, `partida` y vista `vw_ranking`)
- **URL JDBC por defecto:** `jdbc:mysql://localhost:3306/minas?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8`
- **Credenciales MySQL por defecto:** usuario `root`, contrasena `root` (configurables mediante las variables de entorno `DB_URL`, `DB_USER` y `DB_PASS`)
- **Hash de contrasenas:** `PBKDF2WithHmacSHA256` con sal aleatoria de 16 bytes, 600 000 iteraciones y salida de 256 bits

### Usuarios precargados en `minas.sql`

| Usuario | Correo electronico | Contrasena | Rol | Permisos |
|---|---|---|---|---|
| `docente` | `docente@minas.local` | `Prueba123$` | `ADMIN` | Juego, historial, ranking y administracion de usuarios |
| `jugador1` | `jugador1@minas.local` | `Prueba123$` | `JUGADOR` | Juego, historial personal y ranking |

Tambien se pueden registrar nuevos usuarios desde la pantalla inicial (minimo 8 caracteres, una mayuscula, un digito y un simbolo).

## Requisitos del entorno

- JDK 17 o superior
- MySQL Server 8.0 (o MariaDB 10.5+) ejecutandose en `localhost:3306`
- Maven 3.9+ (o el wrapper incluido `mvnw.cmd` / `./mvnw`)
- Navegador web moderno (Chrome, Edge o Firefox)

## Instrucciones de instalacion y ejecucion

### 1. Crear la base de datos en MySQL

Ejecutar el script `minas.sql` ubicado en la raiz del proyecto desde la consola de MySQL, MySQL Workbench o el panel Database de IntelliJ:

```bash
mysql -u root -p < minas.sql
```

Este script crea la base de datos `minas` con las tablas `usuario` y `partida`, la vista `vw_ranking`, los dos usuarios de prueba y una partida finalizada de ejemplo para el ranking.

### 2. Configurar credenciales de MySQL (opcional)

El archivo `src/main/resources/application.properties` toma los datos de conexion desde variables de entorno con valores por defecto para desarrollo local:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/minas?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8}
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASS:root}
```

Si la instancia local de MySQL utiliza otro usuario o contrasena, definir `DB_USER` y `DB_PASS` en las variables de entorno antes de iniciar la aplicacion.

### 3. Iniciar la aplicacion

**Desde IntelliJ IDEA:**

1. Abrir la carpeta raiz del proyecto mediante **File -> Open...**.
2. Verificar que el SDK configurado sea **JDK 17** en **File -> Project Structure -> Project**.
3. Abrir `src/main/java/com/bachillerato/buscaminas/BuscaminasApplication.java` y ejecutar `main()`.
4. Acceder desde el navegador a `http://localhost:8080`.

**Desde terminal:**

```bash
# Windows (PowerShell / CMD)
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### 4. Ejecutar pruebas automatizadas

Para correr la prueba unitaria de apertura en cadena (`PartidaServiceTest`) y la prueba de contexto:

```bash
.\mvnw.cmd test
```

## Correspondencia de endpoints REST

Todos los endpoints de partidas, historial y administracion requieren sesion autenticada (`401 Unauthorized` si no existe sesion activa).

| Operacion | Ruta de referencia | Ruta implementada | Codigo HTTP |
|---|---|---|---|
| Registro de usuario | `POST /api/auth/registro` | `POST /api/auth/registro` | `201 Created` |
| Inicio de sesion | `POST /api/auth/login` | `POST /api/auth/login` | `200 OK` |
| Cierre de sesion | `POST /api/auth/logout` | `POST /api/auth/logout` | `204 No Content` (`401` sin sesion) |
| Usuario autenticado actual | `GET /api/auth/me` | `GET /api/auth/me` | `200 OK` |
| Iniciar nueva partida | `POST /api/partidas` | `POST /api/partidas` | `201 Created` (`409` si ya hay una activa) |
| Consultar partida en curso | `GET /api/partidas/actual` | `GET /api/partidas/activa` | `200 OK` (`404` si no existe activa) |
| Consultar partida por ID | `GET /api/partidas/{id}` | `GET /api/partidas/{id}` | `200 OK` (`403` si pertenece a otro usuario) |
| Revelar casilla (cascada BFS) | `POST /api/partidas/{id}/acciones` | `POST /api/partidas/{id}/revelar` | `200 OK` |
| Colocar o quitar bandera | `POST /api/partidas/{id}/acciones` | `POST /api/partidas/{id}/bandera` | `200 OK` |
| Abandonar partida en curso | `POST /api/partidas/{id}/abandonar` | `POST /api/partidas/{id}/abandonar` | `200 OK` |
| Historial del jugador actual | `GET /api/usuarios/{id}/partidas` | `GET /api/partidas/historial` | `200 OK` |
| Ranking global por nivel | `GET /api/ranking?nivel=...` | `GET /api/ranking?nivel=...` | `200 OK` |
| Listar usuarios (`ADMIN`) | `GET /api/admin/usuarios` | `GET /api/admin/usuarios` | `200 OK` (`403` sin rol `ADMIN`) |
| Activar o desactivar usuario (`ADMIN`) | `PATCH /api/admin/usuarios/{id}/estado` | `PATCH /api/admin/usuarios/{id}/estado` | `200 OK` (`403` sin rol `ADMIN`) |

## Estructura del proyecto

- `minas.sql`: script DDL y DML idempotente para crear la base de datos `minas`, las tablas `usuario` y `partida`, y la vista `vw_ranking`.
- `src/main/java/com/bachillerato/buscaminas/controller/`: controladores REST (`AuthController`, `PartidaController`, `RankingController`, `AdminController`).
- `src/main/java/com/bachillerato/buscaminas/service/`: logica del juego, generacion de minas tras el primer clic, apertura en cascada mediante BFS, calculo de estrellas y consultas de ranking.
- `src/main/java/com/bachillerato/buscaminas/security/`: configuracion de Spring Security, carga de detalles de usuario y codificador `Pbkdf2PasswordEncoder`.
- `src/main/java/com/bachillerato/buscaminas/model/`: entidades JPA (`Usuario`, `Partida`, `EstadoPartida`) y objetos de transferencia de datos (DTO).
- `src/main/java/com/bachillerato/buscaminas/repository/`: repositorios Spring Data JPA y consulta nativa sobre `vw_ranking`.
- `src/main/java/com/bachillerato/buscaminas/exception/`: manejo global de excepciones con respuestas JSON estandarizadas.
- `src/main/resources/static/`: interfaz frontend SPA (`index.html`, `assets/styles.css`, `assets/app.js`) con React 18 desde CDN sin empaquetadores externos.
- `src/test/java/com/bachillerato/buscaminas/`: pruebas automatizadas (`BuscaminasApplicationTests` y `PartidaServiceTest`).

## Reglas de juego y niveles implementados

| Nivel | Dimensiones | Minas | Comportamiento |
|---|---|---|---|
| `PRINCIPIANTE` | `9 x 9` | `10` | Primer clic siempre seguro (las minas se colocan despues del primer movimiento) |
| `INTERMEDIO` | `16 x 16` | `40` | Apertura automatica en cadena para celdas sin minas adyacentes |
| `EXPERTO` | `16 x 30` | `99` | Estado guardado en columna `JSON` para reanudar partidas `EN_CURSO` |

## Notas tecnicas

- **Proteccion del tablero:** las coordenadas de las minas no se envian en la respuesta JSON mientras la partida esta en estado `EN_CURSO`; unicamente se muestran cuando pasa a `TERMINADA` o `ABANDONADA`.
- **Control de acceso:** las rutas `/api/admin/**` exigen el rol `ROLE_ADMIN`. Si un administrador desactiva una cuenta (`activo = false`), ese usuario no puede iniciar sesion ni ejecutar acciones posteriores.
- **Concurrencia:** se utiliza bloqueo pesimista (`PESSIMISTIC_WRITE`) sobre el registro de usuario al iniciar partidas y bloqueo optimista (`@Version`) en `partida` para evitar condiciones de carrera.