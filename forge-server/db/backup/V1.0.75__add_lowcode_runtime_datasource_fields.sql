-- Add runtime datasource metadata for low-code apps and tenant business modules.

SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'gen_datasource'
);

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'gen_datasource'
    AND column_name = 'usage_scope'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE gen_datasource ADD COLUMN usage_scope varchar(32) NOT NULL DEFAULT ''BOTH'' COMMENT ''数据源用途范围：LOWCODE_RUNTIME/TENANT_BUSINESS/DEVELOPER_IMPORT/BOTH''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'gen_datasource'
    AND column_name = 'allow_runtime_write'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE gen_datasource ADD COLUMN allow_runtime_write tinyint NOT NULL DEFAULT 1 COMMENT ''是否允许低代码运行时写入''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'gen_datasource'
    AND column_name = 'allow_runtime_ddl'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE gen_datasource ADD COLUMN allow_runtime_ddl tinyint NOT NULL DEFAULT 0 COMMENT ''是否允许低代码执行运行时DDL''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'gen_datasource'
    AND column_name = 'readonly'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE gen_datasource ADD COLUMN readonly tinyint NOT NULL DEFAULT 0 COMMENT ''是否只读''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'gen_datasource'
    AND column_name = 'risk_level'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE gen_datasource ADD COLUMN risk_level varchar(16) NOT NULL DEFAULT ''MEDIUM'' COMMENT ''风险等级：LOW/MEDIUM/HIGH''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_lowcode_model'
);

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_lowcode_model'
    AND column_name = 'runtime_datasource_id'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_lowcode_model ADD COLUMN runtime_datasource_id bigint DEFAULT NULL COMMENT ''模型运行数据源ID''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_lowcode_model'
    AND column_name = 'runtime_datasource_code'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_lowcode_model ADD COLUMN runtime_datasource_code varchar(64) DEFAULT NULL COMMENT ''模型运行数据源编码''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_lowcode_model'
    AND column_name = 'runtime_table_name'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_lowcode_model ADD COLUMN runtime_table_name varchar(128) DEFAULT NULL COMMENT ''模型运行表名''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_lowcode_model'
    AND column_name = 'table_mode'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_lowcode_model ADD COLUMN table_mode varchar(16) DEFAULT NULL COMMENT ''表模式：CREATE/EXISTING''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_lowcode_model'
    AND index_name = 'idx_lowcode_model_runtime_ds'
);
SET @ddl := IF(@table_exists > 0 AND @index_exists = 0,
  'ALTER TABLE ai_lowcode_model ADD INDEX idx_lowcode_model_runtime_ds (tenant_id, runtime_datasource_id, status)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
);

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'runtime_datasource_id'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN runtime_datasource_id bigint DEFAULT NULL COMMENT ''运行数据源ID''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'runtime_datasource_code'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN runtime_datasource_code varchar(64) DEFAULT NULL COMMENT ''运行数据源编码''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'runtime_datasource_snapshot'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN runtime_datasource_snapshot json DEFAULT NULL COMMENT ''运行数据源快照，不含密码''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'runtime_table_name'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN runtime_table_name varchar(128) DEFAULT NULL COMMENT ''运行表名''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'primary_key_field'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN primary_key_field varchar(64) NOT NULL DEFAULT ''id'' COMMENT ''主键字段名''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'primary_key_column'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN primary_key_column varchar(64) NOT NULL DEFAULT ''id'' COMMENT ''主键列名''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'primary_key_type'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN primary_key_type varchar(32) NOT NULL DEFAULT ''bigint'' COMMENT ''主键类型''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'tenant_strategy'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN tenant_strategy json DEFAULT NULL COMMENT ''租户隔离策略''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'audit_strategy'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN audit_strategy json DEFAULT NULL COMMENT ''审计字段策略''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'logic_delete_strategy'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN logic_delete_strategy json DEFAULT NULL COMMENT ''逻辑删除策略''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND index_name = 'idx_ai_crud_runtime_ds'
);
SET @ddl := IF(@table_exists > 0 AND @index_exists = 0,
  'ALTER TABLE ai_crud_config ADD INDEX idx_ai_crud_runtime_ds (tenant_id, runtime_datasource_id, publish_status)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
);

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND column_name = 'runtime_datasource_id'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN runtime_datasource_id bigint DEFAULT NULL COMMENT ''运行数据源ID''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND column_name = 'runtime_datasource_code'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN runtime_datasource_code varchar(64) DEFAULT NULL COMMENT ''运行数据源编码''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND column_name = 'runtime_datasource_snapshot'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN runtime_datasource_snapshot json DEFAULT NULL COMMENT ''运行数据源快照，不含密码''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND column_name = 'runtime_table_name'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN runtime_table_name varchar(128) DEFAULT NULL COMMENT ''运行表名''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND column_name = 'primary_key_field'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN primary_key_field varchar(64) NOT NULL DEFAULT ''id'' COMMENT ''主键字段名''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND column_name = 'primary_key_column'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN primary_key_column varchar(64) NOT NULL DEFAULT ''id'' COMMENT ''主键列名''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND column_name = 'primary_key_type'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN primary_key_type varchar(32) NOT NULL DEFAULT ''bigint'' COMMENT ''主键类型''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND column_name = 'tenant_strategy'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN tenant_strategy json DEFAULT NULL COMMENT ''租户隔离策略''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND column_name = 'audit_strategy'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN audit_strategy json DEFAULT NULL COMMENT ''审计字段策略''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND column_name = 'logic_delete_strategy'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN logic_delete_strategy json DEFAULT NULL COMMENT ''逻辑删除策略''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_tenant'
);

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_tenant'
    AND column_name = 'default_business_datasource_id'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN default_business_datasource_id bigint DEFAULT NULL COMMENT ''默认业务数据源ID''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_tenant'
    AND column_name = 'default_business_datasource_code'
);
SET @ddl := IF(@table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN default_business_datasource_code varchar(64) DEFAULT NULL COMMENT ''默认业务数据源编码''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
