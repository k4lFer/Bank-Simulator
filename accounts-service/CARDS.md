# Cards — Arquitectura

## Modelo

```
User
 └── Card (tarjeta débito) ← PIN, dailyLimit, status
      ├── Account USD (primaria)
      ├── Account PEN
      └── Account EUR
```

## Entidades

### `cards`
```sql
CREATE TABLE cards (
    id          CHAR(36) PRIMARY KEY,
    user_id     CHAR(36) NOT NULL,
    pan         VARCHAR(19) NOT NULL UNIQUE,  -- 400000XXXXXXXX1234
    expiry_date DATE NOT NULL,
    pin4        VARCHAR(4) NOT NULL,
    status      VARCHAR(20) NOT NULL,         -- ACTIVE | BLOCKED | EXPIRED
    daily_limit DECIMAL(18,2) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP
);
```

### `card_accounts`
```sql
CREATE TABLE card_accounts (
    card_id     CHAR(36) NOT NULL,
    account_id  CHAR(36) NOT NULL,
    is_primary  BOOLEAN NOT NULL DEFAULT FALSE,
    currency    VARCHAR(3) NOT NULL,
    PRIMARY KEY (card_id, account_id)
);
```

## Endpoints

| Método | Ruta | Propósito |
|---|---|---|
| `POST` | `/api/cards/issue` | Emitir nueva tarjeta |
| `GET` | `/api/cards/me` | Listar mis tarjetas |
| `GET` | `/api/cards/{id}` | Detalle de tarjeta + cuentas vinculadas |
| `PATCH` | `/api/cards/{id}/block` | Bloquear tarjeta |
| `POST` | `/api/cards/{id}/accounts` | Vincular cuenta a tarjeta |

## Integración con pagos (futuro)

Hoy las transferencias externas piden `fromAccount` + `pin4`. Con tarjetas sería:

```
Frontend → POST /api/transfers/card-payment
   Body: { cardId, pin4, toAccount, amount, currency, description }
   Backend: valida card → busca cuenta según moneda → debita
```

Se necesitaría:
1. Nuevo `CardPaymentRequestedEvent` en `shared-contracts`
2. Nuevo consumer en `accounts-service` que valida card + PIN + límite diario
3. Resuelve `fromAccount` según moneda (o primaria)
4. Mismo flujo de débito que hoy
