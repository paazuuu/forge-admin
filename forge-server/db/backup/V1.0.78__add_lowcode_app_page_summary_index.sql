-- Add an index for the lowcode app summary page query.

SET @index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND index_name = 'idx_ai_crud_lowcode_page'
);
SET @ddl := IF(@index_exists = 0,
  'ALTER TABLE ai_crud_config ADD INDEX idx_ai_crud_lowcode_page (tenant_id, mode, build_mode, update_time, id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
