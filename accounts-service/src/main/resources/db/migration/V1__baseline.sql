-- Fase 1.1 / 1.7: baseline idempotente de bank_accounts.
-- Generado desde las entidades JPA con Hibernate 7 + MySQLDialect.
-- Se puede ejecutar tanto en una BD vacia como en una BD creada por ddl-auto=update.

CREATE TABLE IF NOT EXISTS account_movements (
    amount decimal(18,2) not null,
    balance_after decimal(18,2) not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    account_id binary(16) not null,
    transfer_id binary(16),
    movement_number varchar(20) not null,
    type enum ('CREDIT','DEBIT') not null,
    primary key (id),
    constraint uk_movement_transfer_account_type unique (transfer_id, account_id, type),
    constraint uk_movement_number unique (movement_number)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS accounts (
    balance decimal(18,2) not null,
    currency varchar(3) not null,
    created_at datetime(6) not null,
    updated_at datetime(6),
    id binary(16) not null,
    user_id binary(16) not null,
    account_number varchar(20) not null,
    status enum ('ACTIVE','BLOCKED') not null,
    primary key (id),
    constraint uk_account_number unique (account_number)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS card_accounts (
    currency varchar(3) not null,
    is_primary bit not null,
    account_id binary(16) not null,
    card_id binary(16) not null,
    primary key (account_id, card_id)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS cards (
    pin4 varchar(4) not null,
    pin6 varchar(6) not null,
    expiry_date varchar(7) not null,
    created_at datetime(6) not null,
    updated_at datetime(6),
    id binary(16) not null,
    user_id binary(16) not null,
    pan varchar(19) not null,
    status enum ('ACTIVE','BLOCKED','EXPIRED') not null,
    primary key (id),
    constraint uk_card_pan unique (pan)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS outbox_events (
    attempts integer not null default 0,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    next_attempt_at datetime(6) not null default current_timestamp(6),
    sent_at datetime(6),
    topic varchar(100) not null,
    event_id binary(16),
    last_error varchar(1000),
    event_type varchar(255) not null,
    payload TEXT,
    status enum ('FAILED','PENDING','SENT') not null,
    primary key (id)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS processed_events (
    id bigint not null auto_increment,
    processed_at datetime(6) not null,
    consumer varchar(64) not null,
    event_id varchar(64) not null,
    primary key (id),
    constraint uk_processed_event unique (consumer, event_id)
) engine=InnoDB;

-- Fase 1.2: eventId del evento publicado (contrato comun DomainEvent).
SET @sql = IF((SELECT count(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'outbox_events' AND COLUMN_NAME = 'event_id') = 0,
    'ALTER TABLE outbox_events ADD COLUMN event_id binary(16)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fase 1.2: columnas nuevas para reintentos en BDs ya existentes.
SET @sql = IF((SELECT count(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'outbox_events' AND COLUMN_NAME = 'attempts') = 0,
    'ALTER TABLE outbox_events ADD COLUMN attempts integer not null default 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'outbox_events' AND COLUMN_NAME = 'next_attempt_at') = 0,
    'ALTER TABLE outbox_events ADD COLUMN next_attempt_at datetime(6) not null default current_timestamp(6)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'outbox_events' AND COLUMN_NAME = 'last_error') = 0,
    'ALTER TABLE outbox_events ADD COLUMN last_error varchar(1000)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fase 0.1: quitar el unique incorrecto de transfer_id si todavia existe.
SET @idx_name = (
    SELECT s.INDEX_NAME
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = database()
      AND s.TABLE_NAME = 'account_movements'
      AND s.COLUMN_NAME = 'transfer_id'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'uk_movement_transfer_account_type'
      AND (SELECT count(*) FROM information_schema.STATISTICS s2
           WHERE s2.TABLE_SCHEMA = s.TABLE_SCHEMA
             AND s2.TABLE_NAME = s.TABLE_NAME
             AND s2.INDEX_NAME = s.INDEX_NAME) = 1
    LIMIT 1
);
SET @sql = IF(@idx_name IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE account_movements DROP INDEX `', @idx_name, '`'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'account_movements'
      AND INDEX_NAME = 'uk_movement_transfer_account_type') = 0,
    'ALTER TABLE account_movements ADD CONSTRAINT uk_movement_transfer_account_type unique (transfer_id, account_id, type)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'processed_events'
      AND INDEX_NAME = 'uk_processed_event') = 0,
    'ALTER TABLE processed_events ADD CONSTRAINT uk_processed_event unique (consumer, event_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fase 1.7: invariantes en BD, no solo en Java.
SET @sql = IF((SELECT count(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = database() AND TABLE_NAME = 'accounts'
      AND CONSTRAINT_NAME = 'chk_accounts_balance_non_negative') = 0,
    'ALTER TABLE accounts ADD CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = database() AND TABLE_NAME = 'account_movements'
      AND CONSTRAINT_NAME = 'chk_movements_amount_positive') = 0,
    'ALTER TABLE account_movements ADD CONSTRAINT chk_movements_amount_positive CHECK (amount > 0)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = database() AND TABLE_NAME = 'account_movements'
      AND CONSTRAINT_NAME = 'chk_movements_balance_non_negative') = 0,
    'ALTER TABLE account_movements ADD CONSTRAINT chk_movements_balance_non_negative CHECK (balance_after >= 0)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
