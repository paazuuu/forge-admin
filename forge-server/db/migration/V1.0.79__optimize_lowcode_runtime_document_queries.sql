-- Optimize lowcode dynamic page document runtime lookups.

SET @table_exists = (
  SELECT COUNT(*)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_crud_config'
);

SET @index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_crud_config'
    AND INDEX_NAME = 'idx_ai_crud_runtime_object_lookup'
);

SET @sql = IF(
  @table_exists > 0 AND @index_exists = 0,
  'ALTER TABLE ai_crud_config ADD INDEX idx_ai_crud_runtime_object_lookup (tenant_id, object_code, mode, build_mode, publish_status, publish_time)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_crud_config'
    AND INDEX_NAME = 'idx_ai_crud_runtime_config_lookup'
);

SET @sql = IF(
  @table_exists > 0 AND @index_exists = 0,
  'ALTER TABLE ai_crud_config ADD INDEX idx_ai_crud_runtime_config_lookup (tenant_id, config_key, mode, build_mode, publish_status)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists = (
  SELECT COUNT(*)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
);

SET @index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND INDEX_NAME = 'idx_ai_business_flow_business_latest'
);

SET @sql = IF(
  @table_exists > 0 AND @index_exists = 0,
  'ALTER TABLE ai_business_flow_instance_link ADD INDEX idx_ai_business_flow_business_latest (tenant_id, business_key, start_time, create_time, id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
