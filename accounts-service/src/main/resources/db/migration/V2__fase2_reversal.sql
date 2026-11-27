-- Fase 2.6: la compensacion es un movimiento REVERSAL con referencia al original.
-- Idempotente para poder aplicarse sobre esquemas creados con ddl-auto=update.

SET @sql = IF((SELECT count(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'account_movements'
      AND COLUMN_NAME = 'reversal_of') = 0,
    'ALTER TABLE account_movements ADD COLUMN reversal_of varchar(20) null',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT count(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'account_movements'
      AND COLUMN_NAME = 'type' AND COLUMN_TYPE LIKE '%REVERSAL%') = 0,
    "ALTER TABLE account_movements MODIFY COLUMN type enum('CREDIT','DEBIT','REVERSAL') not null",
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
