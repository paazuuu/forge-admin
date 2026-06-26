-- Add hierarchy support for app-center business domains.

SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_business_suite'
);

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_business_suite'
    AND column_name = 'parent_id'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_business_suite ADD COLUMN parent_id bigint DEFAULT NULL COMMENT ''上级业务域ID'' AFTER tenant_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_business_suite'
    AND index_name = 'idx_ai_business_suite_parent'
);
SET @ddl := IF(@table_exists > 0 AND @index_exists = 0,
  'ALTER TABLE ai_business_suite ADD INDEX idx_ai_business_suite_parent (tenant_id, parent_id, sort_order, id)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
