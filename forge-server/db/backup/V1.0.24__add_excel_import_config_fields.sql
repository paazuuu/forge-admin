SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_excel_export_config'
);

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'sys_excel_export_config'
              AND COLUMN_NAME = 'include_sample'
        ) = 0,
        'ALTER TABLE sys_excel_export_config ADD COLUMN include_sample TINYINT(1) DEFAULT 0 COMMENT ''是否包含示例数据（用于模板）'' AFTER status',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'sys_excel_export_config'
              AND COLUMN_NAME = 'allow_import'
        ) = 0,
        'ALTER TABLE sys_excel_export_config ADD COLUMN allow_import TINYINT(1) DEFAULT 1 COMMENT ''是否允许导入'' AFTER include_sample',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'UPDATE sys_excel_export_config SET include_sample = COALESCE(include_sample, 0), allow_import = COALESCE(allow_import, 1)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_excel_column_config'
);

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'sys_excel_column_config'
              AND COLUMN_NAME = 'importable'
        ) = 0,
        'ALTER TABLE sys_excel_column_config ADD COLUMN importable TINYINT(1) DEFAULT 1 COMMENT ''是否可导入（1-是，0-否）'' AFTER dict_type',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'sys_excel_column_config'
              AND COLUMN_NAME = 'required'
        ) = 0,
        'ALTER TABLE sys_excel_column_config ADD COLUMN required TINYINT(1) DEFAULT 0 COMMENT ''是否必填（1-是，0-否）'' AFTER importable',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'sys_excel_column_config'
              AND COLUMN_NAME = 'example_value'
        ) = 0,
        'ALTER TABLE sys_excel_column_config ADD COLUMN example_value VARCHAR(255) DEFAULT NULL COMMENT ''示例值（用于模板）'' AFTER required',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'sys_excel_column_config'
              AND COLUMN_NAME = 'validation_rule'
        ) = 0,
        'ALTER TABLE sys_excel_column_config ADD COLUMN validation_rule VARCHAR(255) DEFAULT NULL COMMENT ''校验规则（正则表达式）'' AFTER example_value',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'sys_excel_column_config'
              AND COLUMN_NAME = 'validation_message'
        ) = 0,
        'ALTER TABLE sys_excel_column_config ADD COLUMN validation_message VARCHAR(255) DEFAULT NULL COMMENT ''校验失败提示信息'' AFTER validation_rule',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'UPDATE sys_excel_column_config SET importable = COALESCE(importable, export), required = COALESCE(required, 0)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
