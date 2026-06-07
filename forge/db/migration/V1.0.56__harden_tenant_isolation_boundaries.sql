-- Harden tenant isolation boundaries for system tables that store tenant data.
-- Menu/resource definitions remain global; role-resource bindings stay tenant scoped.

-- Normalize legacy tenant id values. Project convention is default tenant = 1, not 0.
UPDATE sys_user SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_role SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_role_resource SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_role_data_scope SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_user_role SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_user_org SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_user_post SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_org SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_post SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_resource SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
-- Legacy seed data used tenant_id = 0. Remove duplicate dictionary rows before moving
-- them into the default tenant, otherwise tenant-scoped unique keys can fail.
DELETE legacy
FROM sys_dict_data legacy
JOIN sys_dict_data keeper ON keeper.tenant_id = 1
    AND keeper.dict_type = legacy.dict_type
    AND keeper.dict_value = legacy.dict_value
WHERE (legacy.tenant_id IS NULL OR legacy.tenant_id = 0)
  AND legacy.dict_code <> keeper.dict_code;

DELETE legacy
FROM sys_dict_type legacy
JOIN sys_dict_type keeper ON keeper.tenant_id = 1
    AND keeper.dict_type = legacy.dict_type
WHERE (legacy.tenant_id IS NULL OR legacy.tenant_id = 0)
  AND legacy.dict_id <> keeper.dict_id;

UPDATE sys_dict_type SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_dict_data SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_config SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_data_scope_config SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_notice SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_message SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_message_template SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_message_receiver SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_message_send_record SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_login_log SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_operation_log SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_user_social SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_flow_form SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_flow_model_version SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
UPDATE sys_flow_spel_template SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file_group');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file_group' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_file_group ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_file_group SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file_group' AND INDEX_NAME = 'idx_sys_file_group_tenant');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
              'ALTER TABLE sys_file_group ADD INDEX idx_sys_file_group_tenant (tenant_id, deleted, status)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file_metadata');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file_metadata' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_file_metadata ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_file_metadata SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file_metadata' AND INDEX_NAME = 'idx_sys_file_metadata_tenant');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
              'ALTER TABLE sys_file_metadata ADD INDEX idx_sys_file_metadata_tenant (tenant_id, status, upload_time)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notice_org');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notice_org' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_notice_org ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_notice_org no
JOIN sys_notice n ON n.notice_id = no.notice_id
SET no.tenant_id = n.tenant_id
WHERE n.tenant_id IS NOT NULL AND n.tenant_id <> 0;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notice_org' AND INDEX_NAME = 'idx_sys_notice_org_tenant');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
              'ALTER TABLE sys_notice_org ADD INDEX idx_sys_notice_org_tenant (tenant_id, notice_id, org_id)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notice_read_record');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notice_read_record' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_notice_read_record ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_notice_read_record rr
JOIN sys_notice n ON n.notice_id = rr.notice_id
SET rr.tenant_id = n.tenant_id
WHERE n.tenant_id IS NOT NULL AND n.tenant_id <> 0;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notice_read_record' AND INDEX_NAME = 'idx_sys_notice_read_tenant');
SET @sql = IF(@table_exists > 0 AND @index_exists = 0,
              'ALTER TABLE sys_notice_read_record ADD INDEX idx_sys_notice_read_tenant (tenant_id, notice_id, user_id)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Flow tables are tenant data. Add tenant_id where older schemas missed it.
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_model ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_model SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_business ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_business SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_category');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_category' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_category ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_category SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_cc');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_cc' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_cc ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_cc SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_comment');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_comment' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_comment ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_comment SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_condition_item');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_condition_item' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_condition_item ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_condition_item SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_condition_rule');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_condition_rule' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_condition_rule ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_condition_rule SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_error_log');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_error_log' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_error_log ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_error_log SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_config');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_config' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_node_config ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_node_config SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_approval_level');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_approval_level' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_approval_level ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_approval_level SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_statistics');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_statistics' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_statistics ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_statistics SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_task');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_task' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_task ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_task SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_template');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_template' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_template ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE sys_flow_template SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_operation');
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_node_operation' AND COLUMN_NAME = 'tenant_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_node_operation ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = IF(@table_exists > 0, 'UPDATE sys_flow_node_operation SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add tenant indexes for flow tables.
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model' AND INDEX_NAME = 'idx_sys_flow_model_tenant');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_model ADD INDEX idx_sys_flow_model_tenant (tenant_id, status, create_time)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_task' AND INDEX_NAME = 'idx_sys_flow_task_tenant');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_task') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_task ADD INDEX idx_sys_flow_task_tenant (tenant_id, status, create_time)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND INDEX_NAME = 'idx_sys_flow_business_tenant');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_business ADD INDEX idx_sys_flow_business_tenant (tenant_id, status, create_time)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_comment' AND INDEX_NAME = 'idx_sys_flow_comment_tenant');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_comment') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_comment ADD INDEX idx_sys_flow_comment_tenant (tenant_id, process_instance_id, create_time)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model_version' AND INDEX_NAME = 'idx_sys_flow_model_version_tenant');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model_version') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_model_version ADD INDEX idx_sys_flow_model_version_tenant (tenant_id, model_id, version)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Flow natural keys should be unique inside a tenant, not globally.
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model' AND INDEX_NAME = 'model_key');
SET @sql = IF(@index_exists > 0, 'ALTER TABLE sys_flow_model DROP INDEX model_key', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model' AND INDEX_NAME = 'uk_flow_model_tenant_key');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_model') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_model ADD UNIQUE KEY uk_flow_model_tenant_key (tenant_id, model_key)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND INDEX_NAME = 'uk_business_key');
SET @sql = IF(@index_exists > 0, 'ALTER TABLE sys_flow_business DROP INDEX uk_business_key', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business' AND INDEX_NAME = 'uk_flow_business_tenant_key');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_business') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_business ADD UNIQUE KEY uk_flow_business_tenant_key (tenant_id, business_key)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_category' AND INDEX_NAME = 'category_code');
SET @sql = IF(@index_exists > 0, 'ALTER TABLE sys_flow_category DROP INDEX category_code', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_category' AND INDEX_NAME = 'uk_flow_category_tenant_code');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_category') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_category ADD UNIQUE KEY uk_flow_category_tenant_code (tenant_id, category_code)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form' AND INDEX_NAME = 'form_key');
SET @sql = IF(@index_exists > 0, 'ALTER TABLE sys_flow_form DROP INDEX form_key', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form' AND INDEX_NAME = 'uk_flow_form_tenant_key');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_form ADD UNIQUE KEY uk_flow_form_tenant_key (tenant_id, form_key)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_template' AND INDEX_NAME = 'template_key');
SET @sql = IF(@index_exists > 0, 'ALTER TABLE sys_flow_template DROP INDEX template_key', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_template' AND INDEX_NAME = 'uk_flow_template_tenant_key');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_template') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_template ADD UNIQUE KEY uk_flow_template_tenant_key (tenant_id, template_key)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_statistics' AND INDEX_NAME = 'uk_process_date');
SET @sql = IF(@index_exists > 0, 'ALTER TABLE sys_flow_statistics DROP INDEX uk_process_date', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_statistics' AND INDEX_NAME = 'uk_flow_statistics_tenant_date');
SET @sql = IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_statistics') > 0 AND @index_exists = 0,
              'ALTER TABLE sys_flow_statistics ADD UNIQUE KEY uk_flow_statistics_tenant_date (tenant_id, process_def_key, stat_date)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
