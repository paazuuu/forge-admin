-- ============================================================
-- V1.0.68: 为 gen_table_column 增加公式配置字段
-- ============================================================

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gen_table_column'
      AND COLUMN_NAME = 'formula_config'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE gen_table_column ADD COLUMN formula_config TEXT NULL COMMENT ''公式配置JSON {type,expression,mode,dependsOn,aggregate,condition}'' AFTER validate_rule',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
