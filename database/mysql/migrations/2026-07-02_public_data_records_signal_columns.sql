USE growgate;

DELIMITER //

CREATE PROCEDURE add_public_data_records_signal_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'public_data_records'
      AND COLUMN_NAME = 'confidence'
  ) THEN
    ALTER TABLE public_data_records ADD COLUMN confidence DOUBLE PRECISION NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'public_data_records'
      AND COLUMN_NAME = 'source_type'
  ) THEN
    ALTER TABLE public_data_records ADD COLUMN source_type VARCHAR(20) NULL;
  END IF;
END//

DELIMITER ;

CALL add_public_data_records_signal_columns();
DROP PROCEDURE add_public_data_records_signal_columns;
