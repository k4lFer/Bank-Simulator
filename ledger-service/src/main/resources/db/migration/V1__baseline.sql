-- Fase 1.1 / 1.7: baseline idempotente de bank_ledger.
-- Generado desde las entidades JPA con Hibernate 7 + MySQLDialect.

CREATE TABLE IF NOT EXISTS ledger_entries (
    amount decimal(38,2) not null,
    currency varchar(3) not null,
    entry_type varchar(3) not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    transfer_id binary(16),
    account_number varchar(20) not null,
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

SET @sql = IF((SELECT count(*) FROM (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'ledger_entries' AND NON_UNIQUE = 0
      AND COLUMN_NAME IN ('transfer_id','entry_type','account_number')
    GROUP BY INDEX_NAME HAVING count(*) = 3) x) = 0,
    'ALTER TABLE ledger_entries ADD CONSTRAINT uk_ledger_entry unique (transfer_id, entry_type, account_number)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'processed_events'
      AND INDEX_NAME = 'uk_processed_event') = 0,
    'ALTER TABLE processed_events ADD CONSTRAINT uk_processed_event unique (consumer, event_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fase 1.7: invariantes en BD.
SET @sql = IF((SELECT count(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = database() AND TABLE_NAME = 'ledger_entries'
      AND CONSTRAINT_NAME = 'chk_ledger_amount_positive') = 0,
    'ALTER TABLE ledger_entries ADD CONSTRAINT chk_ledger_amount_positive CHECK (amount > 0)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
