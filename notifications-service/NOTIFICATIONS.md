# Notifications Service — Arquitectura

## Flujo

```
transfers-service (u otro servicio)
  ↓ Kafka: bank.notification.events
notifications-service
  → guarda en DB (tabla notifications)
  → SSE: /api/notifications/stream?token=
  → Email (opcional)
  → FCM (opcional)
```

## Evento Kafka: `TransferNotificationEvent`

`shared-contracts` — nuevo evento:

```
TransferNotificationEvent
├── transferId    UUID
├── fromUserId    UUID     (remitente)
├── toUserId      UUID     (destinatario)
├── amount        BigDecimal
├── currency      String
├── description   String
├── status        String   (COMPLETED | REJECTED)
└── rejectionReason String? (solo si REJECTED)
```

Se publica desde `transfers-service` en `AccountEventConsumer` cuando una transferencia finaliza (COMPLETED o REJECTED).  
Topic: `bank.notification.events`

## Tabla: `notifications`

```sql
CREATE TABLE notifications (
    id          CHAR(36) PRIMARY KEY,
    user_id     CHAR(36) NOT NULL,
    type        VARCHAR(50) NOT NULL,  -- 'TRANSFER_RECEIVED' | 'TRANSFER_SENT' | 'TRANSFER_REJECTED'
    title       VARCHAR(200) NOT NULL,
    body        TEXT,
    data        JSON,                  -- { transferId, amount, currency, fromAccount, toAccount }
    read        BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL
);
```

### Tipos de notificación

| type | title guardado | body guardado |
|---|---|---|
| `TRANSFER_SENT` | "Transferencia enviada" | "Giraste $500.00 a cuenta 123-456-789" |
| `TRANSFER_RECEIVED` | "Transferencia recibida" | "Recibiste $500.00 de cuenta 987-654-321" |
| `TRANSFER_REJECTED` | "Transferencia rechazada" | "Tu transferencia fue rechazada: Fondos insuficientes" |

## Endpoints REST

| Método | Ruta | Propósito |
|---|---|---|
| `GET` | `/api/notifications?page=0&size=20` | Listar notificaciones del usuario autenticado |
| `PUT` | `/api/notifications/{id}/read` | Marcar como leída |
| `PUT` | `/api/notifications/read-all` | Marcar todas como leídas |
| `GET` | `/api/notifications/unread-count` | Contador de no leídas |
| `GET` | `/api/notifications/stream?token=` | SSE en vivo |

## Componentes del servicio

```
notifications-service/
├── domain/
│   ├── Notification.java
│   └── enums/
│       └── NotificationType.java
├── application/
│   ├── port/input/       -- use cases (ListNotifications, MarkAsRead, etc.)
│   ├── port/output/      -- NotificationRepository
│   └── dto/response/     -- NotificationResponse
├── infrastructure/
│   ├── persistence/
│   │   ├── jpa/entities/ -- NotificationEntity
│   │   └── mapper/       -- NotificationMapper
│   ├── kafka/
│   │   └── TransferNotificationConsumer.java
│   ├── security/
│   │   └── SecurityConfig.java
│   └── rest/
│       ├── NotificationController.java
│       └── NotificationSseController.java
```

## Dependencias necesarias (pom.xml actual ya las tiene)

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-kafka`
- `spring-boot-starter-mail` (futuro)
- `spring-boot-starter-webmvc`
- `spring-boot-starter-security` (agregar)
- `shared-contracts` (ya incluido)

## Pasos de implementación

1. Agregar `spring-boot-starter-security` al pom.xml
2. Crear `SecurityConfig` (como transfers-service, permitir `/api/notifications/stream`)
3. Agregar `app.jwt.secret` al `application.yaml`
4. Crear `NotificationType` enum
5. Actualizar `NotificationEntity` con `type` y `data` (JSON string)
6. Crear `Notification` domain + `NotificationRepository`
7. Crear `NotificationResponse` DTO + mapper
8. Crear use cases (list, mark-read, mark-all-read, unread-count)
9. Crear `NotificationController` (REST endpoints)
10. Crear `NotificationSseController` (SSE, mismo patrón que `TransferSseController`)
11. Crear `TransferNotificationEvent` en `shared-contracts`
12. Crear `TransferNotificationConsumer` (Kafka consumer)
13. Publicar `TransferNotificationEvent` desde `transfers-service` en `AccountEventConsumer`
14. Email/FCM: opcional, agregar después

## Integración Frontend

- `useNotifications()` hook (similar a `useAuth`) con SSE global en `AppLayout`
- Badge en navbar con `unread-count`
- Dropdown de notificaciones (últimas 5)
- Página `/notifications` con historial completo
