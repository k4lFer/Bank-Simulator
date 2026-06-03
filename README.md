# Bank Simulator

Sistema bancario simulado con arquitectura de **microservicios**, comunicación **asíncrona basada en eventos** (Apache Kafka) y principios de **CQRS**, **Outbox Pattern** y **Zero Trust Security**.

---

## Arquitectura General

```
                                    nginx:8888
                                        |
                         ┌──────────────┴──────────────┐
                         │        api-gateway:8000       │
                         │  (Spring Cloud Gateway + JWT) │
                         └──────┬──────┬──────┬──────┬──┘
                                │      │      │      │
                    ┌───────────┘      │      │      └───────────┐
                    │                  │      │                  │
              users:8081         accounts:8082  transfers:8083   │
           (auth, users,        (accounts,     (transfers,      │
            projections)         deposits,      outbox)          │
                                  movements)                     │
                    │                  │      │                  │
                    └───────────┐      │      │     ┌────────────┘
                                │      │      │     │
                           ledger:8084   notifications:8085
                         (double-entry    (email/SMS alerts)
                          accounting)         [scaffold]
                            [scaffold]

              ╔══════════════════════════════════════════════╗
              ║              Apache Kafka                     ║
              ║  bank.transfer.events | bank.account.events   ║
              ╚══════════════════════════════════════════════╝

              ╔══════════════════════════════════════════════╗
              ║              MySQL 8                          ║
              ║  bank_users | bank_accounts | bank_transfers  ║
              ║  bank_ledger | bank_notifications             ║
              ╚══════════════════════════════════════════════╝
```

### Patrones y Principios

| Patrón | Descripción |
|--------|-------------|
| **Event-Driven Architecture** | Comunicación asíncrona entre servicios mediante Apache Kafka |
| **Outbox Pattern** | Las órdenes de transferencia se persisten primero en una tabla `outbox` y luego se publican a Kafka mediante un `@Scheduled` poller, garantizando consistencia |
| **CQRS (ligero)** | Lado de escritura en accounts-service + transfers-service; lado de lectura en users-service (proyecciones de saldos) y ledger-service (libro contable) |
| **Database per Service** | Cada microservicio tiene su propia base de datos MySQL |
| **Zero Trust Security** | Validación JWT tanto en el API Gateway como en cada microservicio de forma individual |
| **Hexagonal Architecture** | Los servicios siguen clean architecture con puertos/adaptadores y paquetes organizados por dominio |

---

## Servicios

### users-service — `:8081`
**Registro, autenticación y administración de usuarios.**

- `POST /api/auth/register` — Registro de nuevo usuario
- `POST /api/auth/register-admin` — Registro de administrador (requiere bootstrap secret)
- `POST /api/auth/login` — Inicio de sesión, devuelve JWT (access + refresh)
- `POST /api/auth/refresh` — Renovación del access token
- `GET /api/users/me` — Perfil propio
- `PUT /api/users/me` — Actualizar perfil propio
- `GET /api/admin/users` — Listar todos los usuarios (admin)
- `PUT /api/admin/users/{id}/status` — Activar/desactivar usuario (admin)
- `PUT /api/admin/users/{id}/role` — Cambiar rol de usuario (admin)

**Consume eventos de Kafka** `bank.account.events` para mantener proyecciones de saldo de cuentas (tabla `AccountProjection`).

---

### accounts-service — `:8082`
**Gestión de cuentas bancarias, depósitos y movimientos.**

- `POST /api/accounts/create` — Crear una cuenta bancaria
- `POST /api/accounts/deposit` — Auto-depósito en cuenta propia
- `GET /api/accounts/me` — Listar cuentas del usuario autenticado
- `GET /api/accounts/{id}` — Detalle de una cuenta
- `GET /api/accounts/{id}/movements` — Movimientos de una cuenta
- `PATCH /api/accounts/{id}/block` — Bloquear/desbloquear cuenta
- `PUT /api/accounts/{id}/pin` — Cambiar PIN

**Escucha** `bank.transfer.events` para procesar transferencias entrantes (valida saldo, debita origen, acredita destino) y **publica** resultados en `bank.account.events` (AccountDebitedEvent, AccountCreditedEvent, AccountRejectedEvent).

---

### transfers-service — `:8083`
**Orquestación de transferencias P2P con Outbox Pattern.**

- `POST /api/transfers` — Solicitar una transferencia
- `GET /api/transfers/{transferId}` — Estado de una transferencia
- `GET /api/transfers/by-account/{accountNumber}` — Transferencias por cuenta

**Publica** `TransferRequestedEvent` en `bank.transfer.events` usando el patrón Outbox: persiste el evento en tabla `transfer_events` y un `@Scheduled` poller lo envía a Kafka cada 5 segundos, marcándolo como `SENT`. **Consume** `bank.account.events` para actualizar el estado de la transferencia (`PENDING → DEBITED → COMPLETED` o `REJECTED`).

---

### ledger-service — `:8084`
**Libro contable de doble entrada.**
> ⚠️ **Estado: Scaffold** — Clases de dominio y entidad JPA creadas, pendiente la implementación del consumidor Kafka y los endpoints REST.

Registrará débitos y créditos de forma inmutable para permitir consultas de estado de cuenta, informes diarios y balances históricos.

---

### notifications-service — `:8085`
**Notificaciones simuladas (email/SMS).**
> ⚠️ **Estado: Scaffold** — Clase principal y configuración creadas, pendiente la implementación del consumidor Kafka y endpoints.

Consumirá eventos de transferencia para generar notificaciones de éxito/fallo.

---

### api-gateway — `:8000`
**Puerta de entrada única (Spring Cloud Gateway).**

| Ruta | Destino |
|------|---------|
| `/api/auth/**` | users-service (público) |
| `/api/users/**` | users-service |
| `/api/admin/**` | users-service |
| `/api/accounts/**` | accounts-service |
| `/api/transfers/**` | transfers-service |
| `/api/ledger/**` | ledger-service |
| `/api/notifications/**` | notifications-service |

Valida el JWT en cada petición entrante (excepto `/api/auth/**`) e inyecta las cabeceras `X-User-Id` y `X-User-Role` hacia los microservicios.

---

### frontend — `:5173`
**Interfaz de usuario en React 19 + TypeScript + Vite + Tailwind CSS 4.**

| Ruta | Vista |
|------|-------|
| `/` | Login con selección de rol |
| `/login/cliente` | Login cliente |
| `/login/admin` | Login admin |
| `/registro` | Registro de usuario |
| `/dashboard` | Panel principal con resumen de cuentas |
| `/accounts/:id` | Detalle de cuenta + movimientos |
| `/admin` | Panel de administración |

---

### shared-contracts
**Librería compartida** (JAR plano, no Spring Boot) con DTOs, eventos de Kafka, componentes de seguridad y utilidades usadas por todos los microservicios. Incluye:

- **Eventos:** `TransferRequestedEvent`, `AccountDebitedEvent`, `AccountCreditedEvent`, `AccountRejectedEvent`, `TransferCompletedEvent`, `TransferFailedEvent`, `AccountCreatedEvent`, `AccountDepositedEvent`, `UserCreatedEvent`
- **Seguridad:** `JwtTokenValidator`, `JwtAuthFilter` — validación JWT en cada servicio
- **Utilidades:** `ApiResponse<T>`, `Result<T>`, `ResponseHelper`, interfaces genéricas

---

### nginx — `:8888`
**Proxy reverso** que expone el sistema completo en el puerto `8888`. Enruta `/api/` hacia el api-gateway, sirve Swagger UI y redirige el resto al frontend.

---

## Infraestructura

| Componente | Tecnología |
|------------|------------|
| **Base de datos** | MySQL 8 (5 bases: `bank_users`, `bank_accounts`, `bank_transfers`, `bank_ledger`, `bank_notifications`) |
| **Mensajería** | Apache Kafka + Zookeeper |
| **UI de Kafka** | Kafka UI (`provectuslabs/kafka-ui`) en `:8080` |
| **Compilación** | Maven (Java 21) |
| **Contenedores** | Docker Compose |

---

## Comandos Rápidos (Makefile)

| Comando | Descripción |
|---------|-------------|
| `make up` | Levantar todo el stack (`docker compose up -d --build`) |
| `make down` | Detener todos los contenedores |
| `make restart` | Reiniciar todo |
| `make logs` | Ver logs de todos los servicios |
| `make ps` | Estado de los contenedores + URLs de acceso |
| `make infra` | Solo MySQL + Zookeeper + Kafka (para desarrollo local) |
| `make install` | Compilar e instalar `shared-contracts` en `.m2` local |
| `make run-accounts` | Compilar shared + ejecutar accounts-service |
| `make run-transfers` | Compilar shared + ejecutar transfers-service |
| `make run-users` | Compilar shared + ejecutar users-service |
| `make run-gateway` | Ejecutar api-gateway |
| `make createsuperuser` | Crear usuario administrador vía API |

---

## Estado del Proyecto

| Servicio | Estado |
|----------|--------|
| users-service | ✅ Completamente implementado |
| accounts-service | ✅ Completamente implementado |
| transfers-service | ✅ Completamente implementado (Outbox incluido) |
| api-gateway | ✅ Completamente implementado |
| frontend | ✅ Funcionalidades principales implementadas |
| shared-contracts | ✅ Completamente implementado |
| ledger-service | 🔧 Scaffold (dominio creado, falta consumidor Kafka y endpoints) |
| notifications-service | 🔧 Scaffold (estructura creada, falta implementación) |
| CI/CD (GitHub Actions) | ❌ Pendiente |
| Currency Exchange Service | 📋 Planificado (ver `plan-exchange-service.md`) |

---

## Configuración del Entorno

Las variables de configuración están organizadas en archivos `.env` (excluidos de Git) por servicio, más un `.env` raíz para infraestructura compartida.

### Estructura

```
.env                     ← infraestructura compartida (MySQL, Kafka, etc.)
.env.example             ← ejemplo del archivo raíz
users-service/.env       ← datasource, JWT, admin secrets
accounts-service/.env    ← datasource, JWT secret
transfers-service/.env   ← datasource
ledger-service/.env      ← datasource
notifications-service/.env ← datasource
api-gateway/.env         ← JWT secret
```

### Variables por archivo

**`./.env` (compartido — inyectado a todos los servicios)**

| Variable | Descripción | Default |
|----------|-------------|---------|
| `MYSQL_ROOT_PASSWORD` | Contraseña root de MySQL | `rootpass` |
| `DB_USERNAME` | Usuario de BD | `root` |
| `DB_PASSWORD` | Contraseña de BD | `rootpass` |
| `JPA_DDL_AUTO` | Estrategia DDL de Hibernate | `update` |
| `KAFKA_BOOTSTRAP_SERVERS` | Servidores Kafka | `kafka:9092` |
| `DOCKER_COMPOSE_ENABLED` | Auto-config de Docker Compose | `false` |
| `API_PROXY_TARGET` | Target del proxy de Vite | `http://api-gateway:8000` |

**`./{servicio}/.env` (específico por servicio)**

| Servicio | Variables |
|----------|-----------|
| users-service | `USERS_DATASOURCE_URL`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`, `ADMIN_BOOTSTRAP_SECRET` |
| accounts-service | `ACCOUNTS_DATASOURCE_URL`, `JWT_SECRET` |
| transfers-service | `TRANSFERS_DATASOURCE_URL` |
| ledger-service | `LEDGER_DATASOURCE_URL` |
| notifications-service | `NOTIFICATIONS_DATASOURCE_URL` |
| api-gateway | `JWT_SECRET` |

### Cómo usar

1. Copia los archivos de ejemplo en cada carpeta:
   ```bash
   cp .env.example .env
   cp users-service/.env.example users-service/.env
   cp accounts-service/.env.example accounts-service/.env
   cp transfers-service/.env.example transfers-service/.env
   cp ledger-service/.env.example ledger-service/.env
   cp notifications-service/.env.example notifications-service/.env
   cp api-gateway/.env.example api-gateway/.env
   ```
2. Ajusta los valores según tu entorno (especialmente `JWT_SECRET` y `MYSQL_ROOT_PASSWORD` en producción)
3. Los archivos `.env` son leídos automáticamente por Docker Compose (`make up`)

> **Nota:** Los `application.yaml` de cada servicio tienen valores por defecto que funcionan localmente mediante `mvnw spring-boot:run`. Las variables del `.env` sobreescriben esos defaults solo cuando se despliega con Docker.

---

## Tecnologías

- **Backend:** Java 21, Spring Boot 4.0.6, Spring Cloud 2025.1.1
- **Frontend:** React 19, TypeScript, Vite 8, Tailwind CSS 4
- **Base de datos:** MySQL 8
- **Mensajería:** Apache Kafka 7.7.0
- **Seguridad:** JWT (jjwt 0.12.6), BCrypt
- **Infraestructura:** Docker, Docker Compose, nginx
