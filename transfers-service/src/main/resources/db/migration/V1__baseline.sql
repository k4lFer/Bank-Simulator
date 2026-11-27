-- Fase 1.1 / 1.7: baseline idempotente de bank_transfers.
-- Generado desde las entidades JPA con Hibernate 7 + MySQLDialect.

CREATE TABLE IF NOT EXISTS transfers (
    amount decimal(18,2) not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    to_user_id binary(16),
    transfer_id binary(16) not null,
    user_id binary(16) not null,
    from_account varchar(20),
    to_account varchar(20) not null,
    currency varchar(255) not null,
    description varchar(255),
    rejection_reason varchar(255),
    status enum ('COMPENSATED','COMPLETED','DEBITED','FAILED','PENDING','REJECTED') not null,
    primary key (id),
    constraint uk_transfer_id unique (transfer_id)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS transfers_events (
    attempts integer not null default 0,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    next_attempt_at datetime(6) not null default current_timestamp(6),
    sent_at datetime(6),
    transfer_id bigint not null,
    event_type varchar(50) not null,
    event_id binary(16),
    last_error varchar(1000),
    payload TEXT,
    status enum ('FAILED','PENDING','SENT') not null,
    primary key (id),
    constraint fk_transfers_events_transfer foreign key (transfer_id) references transfers (id)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS processed_events (
    id bigint not null auto_increment,
    processed_at datetime(6) not null,
    consumer varchar(64) not null,
    event_id varchar(64) not null,
    primary key (id),
    constraint uk_processed_event unique (consumer, event_id)
) engine=InnoDB;

-- Fase 0.2: COMPENSATED puede no existir en un enum creado antes del hotfix.
SET @sql = IF((SELECT coalesce(max(column_type), '') FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'transfers' AND COLUMN_NAME = 'status') NOT LIKE '%COMPENSATED%',
    'ALTER TABLE transfers MODIFY COLUMN status enum (''COMPENSATED'',''COMPLETED'',''DEBITED'',''FAILED'',''PENDING'',''REJECTED'') not null',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fase 1.2: eventId del evento publicado (contrato comun DomainEvent).
SET @sql = IF((SELECT count(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'transfers_events' AND COLUMN_NAME = 'event_id') = 0,
    'ALTER TABLE transfers_events ADD COLUMN event_id binary(16)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fase 1.2: columnas nuevas para reintentos en BDs ya existentes.
SET @sql = IF((SELECT count(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'transfers_events' AND COLUMN_NAME = 'attempts') = 0,
    'ALTER TABLE transfers_events ADD COLUMN attempts integer not null default 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'transfers_events' AND COLUMN_NAME = 'next_attempt_at') = 0,
    'ALTER TABLE transfers_events ADD COLUMN next_attempt_at datetime(6) not null default current_timestamp(6)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'transfers_events' AND COLUMN_NAME = 'last_error') = 0,
    'ALTER TABLE transfers_events ADD COLUMN last_error varchar(1000)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'processed_events'
      AND INDEX_NAME = 'uk_processed_event') = 0,
    'ALTER TABLE processed_events ADD CONSTRAINT uk_processed_event unique (consumer, event_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fase 1.7: invariantes en BD.
SET @sql = IF((SELECT count(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = database() AND TABLE_NAME = 'transfers'
      AND CONSTRAINT_NAME = 'chk_transfers_amount_positive') = 0,
    'ALTER TABLE transfers ADD CONSTRAINT chk_transfers_amount_positive CHECK (amount > 0)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
