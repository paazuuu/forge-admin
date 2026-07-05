-- Add flow model designer type for dual designer mode.

SET @table_exists = (
  SELECT COUNT(*)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_flow_model'
);

SET @column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_flow_model'
    AND COLUMN_NAME = 'designer_type'
);

SET @sql = IF(
  @table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_model ADD COLUMN designer_type VARCHAR(32) NOT NULL DEFAULT ''approval'' COMMENT ''设计器类型：approval-审批流程/business-业务流程'' AFTER flow_type',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_flow_model
SET designer_type = 'approval'
WHERE designer_type IS NULL OR designer_type = '';
