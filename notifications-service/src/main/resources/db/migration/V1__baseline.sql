-- Fase 1.1 / 1.7: baseline idempotente de bank_notifications.
-- Generado desde las entidades JPA con Hibernate 7 + MySQLDialect.

CREATE TABLE IF NOT EXISTS notifications (
    amount decimal(19,4),
    currency varchar(3),
    is_read bit not null,
    created_at datetime(6) not null,
    id binary(16) not null,
    user_id binary(16) not null,
    related_account varchar(50),
    message varchar(500) not null,
    title varchar(255) not null,
    type enum ('CARD_DEPOSIT','DEPOSIT','TRANSFER_COMPENSATED','TRANSFER_RECEIVED','TRANSFER_REJECTED','TRANSFER_SENT') not null,
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

-- Fase 0.2: TRANSFER_COMPENSATED puede no existir en un enum anterior.
SET @sql = IF((SELECT coalesce(max(column_type), '') FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'type') NOT LIKE '%TRANSFER_COMPENSATED%',
    'ALTER TABLE notifications MODIFY COLUMN type enum (''CARD_DEPOSIT'',''DEPOSIT'',''TRANSFER_COMPENSATED'',''TRANSFER_RECEIVED'',''TRANSFER_REJECTED'',''TRANSFER_SENT'') not null',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'processed_events'
      AND INDEX_NAME = 'uk_processed_event') = 0,
    'ALTER TABLE processed_events ADD CONSTRAINT uk_processed_event unique (consumer, event_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fase 1.7: invariantes en BD.
SET @sql = IF((SELECT count(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = database() AND TABLE_NAME = 'notifications'
      AND CONSTRAINT_NAME = 'chk_notifications_amount_positive') = 0,
    'ALTER TABLE notifications ADD CONSTRAINT chk_notifications_amount_positive CHECK (amount is null or amount > 0)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
