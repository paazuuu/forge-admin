-- Excel 导入导出配置增加显式配置类型。

SET @table_exists = (
    SELECT COUNT(1)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_excel_export_config'
);

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(1)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'sys_excel_export_config'
              AND COLUMN_NAME = 'config_type'
        ) = 0,
        'ALTER TABLE sys_excel_export_config ADD COLUMN config_type VARCHAR(16) NOT NULL DEFAULT ''BOTH'' COMMENT ''配置类型：EXPORT导出 IMPORT导入 BOTH导入导出'' AFTER config_key',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'UPDATE sys_excel_export_config
        SET config_type = CASE
            WHEN COALESCE(allow_import, 1) = 0 THEN ''EXPORT''
            WHEN (data_source_bean IS NULL OR data_source_bean = '''' OR query_method IS NULL OR query_method = '''') THEN ''IMPORT''
            ELSE ''BOTH''
        END
      WHERE config_type IS NULL
         OR config_type = ''''
         OR (config_type = ''BOTH'' AND (
                COALESCE(allow_import, 1) = 0
                OR data_source_bean IS NULL OR data_source_bean = ''''
                OR query_method IS NULL OR query_method = ''''
            ))'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'UPDATE sys_excel_export_config
        SET allow_import = CASE WHEN config_type = ''EXPORT'' THEN 0 ELSE 1 END'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'ALTER TABLE sys_excel_export_config MODIFY COLUMN data_source_bean VARCHAR(100) DEFAULT NULL COMMENT ''数据源Bean名称（如：userService，导入-only可为空）'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'ALTER TABLE sys_excel_export_config MODIFY COLUMN query_method VARCHAR(100) DEFAULT NULL COMMENT ''数据查询方法名（如：list、page，导入-only可为空）'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, seed.dict_status, seed.remark, NOW(), NOW()
FROM (
    SELECT 1 tenant_id, 'Excel配置类型' dict_name, 'sys_excel_config_type' dict_type, 1 dict_status, 'Excel导入导出配置类型' remark
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_type t
    WHERE t.tenant_id = seed.tenant_id
      AND t.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, NOW(), NOW()
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '仅导出' dict_label, 'EXPORT' dict_value, 'sys_excel_config_type' dict_type, 'info' list_class, 'N' is_default, '仅用于导出' remark
    UNION ALL SELECT 1, 2, '仅导入', 'IMPORT', 'sys_excel_config_type', 'warning', 'N', '仅用于导入'
    UNION ALL SELECT 1, 3, '导入导出', 'BOTH', 'sys_excel_config_type', 'success', 'Y', '同时用于导入和导出'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_data d
    WHERE d.tenant_id = seed.tenant_id
      AND d.dict_type = seed.dict_type
      AND d.dict_value = seed.dict_value
);

UPDATE sys_resource
SET resource_name = 'Excel导入导出配置',
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND path = '/system/excel-export-config'
  AND resource_name = 'Excel导出配置';
