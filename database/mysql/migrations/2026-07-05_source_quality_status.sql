DELIMITER //

CREATE PROCEDURE add_source_quality_status_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'public_data_records'
      AND COLUMN_NAME = 'source_quality_status'
  ) THEN
    ALTER TABLE public_data_records
      ADD COLUMN source_quality_status VARCHAR(40) NULL AFTER source_type;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'public_api_raw_responses'
      AND COLUMN_NAME = 'source_quality_status'
  ) THEN
    ALTER TABLE public_api_raw_responses
      ADD COLUMN source_quality_status VARCHAR(40) NOT NULL DEFAULT 'HEURISTIC_ONLY' AFTER source_type;
  END IF;
END//

DELIMITER ;

CALL add_source_quality_status_columns();
DROP PROCEDURE add_source_quality_status_columns;
