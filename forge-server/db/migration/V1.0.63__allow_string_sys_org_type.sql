-- Allow sys_org.org_type to use dictionary values that are not numeric.

SET @table_exists = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_org'
);

SET @column_is_string = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_org'
    AND COLUMN_NAME = 'org_type'
    AND DATA_TYPE IN ('char', 'varchar')
);

SET @sql = IF(
  @table_exists > 0 AND @column_is_string = 0,
  'ALTER TABLE `sys_org` MODIFY COLUMN `org_type` varchar(64) DEFAULT ''1'' COMMENT ''组织类型（sys_org_type 字典值）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
