-- 低代码业务领域化基础结构

CREATE TABLE IF NOT EXISTS `ai_lowcode_domain` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级领域ID，0表示根节点',
  `domain_code` varchar(48) NOT NULL COMMENT '领域编码',
  `domain_name` varchar(128) NOT NULL COMMENT '领域名称',
  `domain_desc` varchar(500) DEFAULT NULL COMMENT '领域说明',
  `icon` varchar(128) DEFAULT NULL COMMENT '领域图标',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用 DISABLED停用',
  `menu_parent_id` bigint DEFAULT NULL COMMENT '默认菜单父级ID',
  `table_prefix` varchar(64) NOT NULL DEFAULT 'biz_' COMMENT '默认表名前缀',
  `config_key_prefix` varchar(64) NOT NULL DEFAULT 'biz_' COMMENT '默认配置键前缀',
  `default_app_type` varchar(32) NOT NULL DEFAULT 'SINGLE' COMMENT '默认应用类型',
  `default_layout_type` varchar(64) NOT NULL DEFAULT 'simple-crud' COMMENT '默认页面模板',
  `default_table_mode` varchar(32) NOT NULL DEFAULT 'CREATE' COMMENT '默认建表模式',
  `domain_schema` json DEFAULT NULL COMMENT '领域扩展协议',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_lowcode_domain_code` (`tenant_id`, `domain_code`),
  UNIQUE KEY `uk_ai_lowcode_domain_name` (`tenant_id`, `parent_id`, `domain_name`),
  KEY `idx_ai_lowcode_domain_parent` (`tenant_id`, `parent_id`, `sort`),
  KEY `idx_ai_lowcode_domain_status` (`tenant_id`, `status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI低代码业务领域表';

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'domain_id'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN domain_id bigint DEFAULT NULL COMMENT ''业务领域ID'' AFTER publish_by',
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
    AND column_name = 'domain_code'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN domain_code varchar(48) DEFAULT NULL COMMENT ''业务领域编码'' AFTER domain_id',
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
    AND column_name = 'object_code'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN object_code varchar(48) DEFAULT NULL COMMENT ''业务对象编码'' AFTER domain_code',
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
    AND column_name = 'object_name'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN object_name varchar(128) DEFAULT NULL COMMENT ''业务对象名称'' AFTER object_code',
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
    AND index_name = 'idx_ai_crud_domain'
);
SET @ddl := IF(@index_exists = 0,
  'ALTER TABLE ai_crud_config ADD INDEX idx_ai_crud_domain (tenant_id, domain_id, publish_status, update_time)',
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
    AND index_name = 'idx_ai_crud_domain_object'
);
SET @ddl := IF(@index_exists = 0,
  'ALTER TABLE ai_crud_config ADD INDEX idx_ai_crud_domain_object (tenant_id, domain_id, object_code)',
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
    AND column_name = 'domain_id'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN domain_id bigint DEFAULT NULL COMMENT ''业务领域ID'' AFTER config_key',
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
    AND column_name = 'domain_code'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN domain_code varchar(48) DEFAULT NULL COMMENT ''业务领域编码'' AFTER domain_id',
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
    AND column_name = 'object_code'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN object_code varchar(48) DEFAULT NULL COMMENT ''业务对象编码'' AFTER domain_code',
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
    AND column_name = 'object_name'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD COLUMN object_name varchar(128) DEFAULT NULL COMMENT ''业务对象名称'' AFTER object_code',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config_version'
    AND index_name = 'idx_ai_crud_version_domain'
);
SET @ddl := IF(@index_exists = 0,
  'ALTER TABLE ai_crud_config_version ADD INDEX idx_ai_crud_version_domain (tenant_id, domain_id, version_no)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO ai_lowcode_domain (id, tenant_id, parent_id, domain_code, domain_name, domain_desc, icon, sort, status,
                               menu_parent_id, table_prefix, config_key_prefix, default_app_type,
                               default_layout_type, default_table_mode, domain_schema, create_by, create_time,
                               update_by, update_time, create_dept)
SELECT 1900000000000000001, 1, 0, 'general', '通用业务域', '历史低代码应用默认归属的通用业务领域',
       'ionicons5:AppsOutline', 0, 'ENABLED', NULL, 'biz_general_', 'general_', 'SINGLE',
       'simple-crud', 'CREATE',
       '{"aiContext":{"description":"通用业务域，用于承接历史低代码应用和未明确归属的业务对象","terms":[],"commonObjects":[],"fieldNamingPreference":"lowerCamel","constraints":[],"generationNotes":[]},"naming":{"tablePrefix":"biz_general_","configKeyPrefix":"general_","objectCodeStyle":"lower_snake"},"defaults":{"appType":"SINGLE","layoutType":"simple-crud","tableMode":"CREATE","menuParentId":null},"fieldTemplates":[],"dictRecommendations":[],"securityPolicies":[]}',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_lowcode_domain
  WHERE tenant_id = 1
    AND domain_code = 'general'
);

SET @general_domain_id := (
  SELECT id
  FROM ai_lowcode_domain
  WHERE tenant_id = 1
    AND domain_code = 'general'
  LIMIT 1
);

UPDATE ai_crud_config
SET domain_id = @general_domain_id,
    domain_code = 'general'
WHERE @general_domain_id IS NOT NULL
  AND tenant_id = 1
  AND mode = 'CONFIG'
  AND build_mode = 'LOWCODE'
  AND domain_id IS NULL;

SET @ai_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 1
    AND path = '/ai'
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '业务领域', @ai_root_id, 2, 12, '/ai/lowcode-domains', 'ai/lowcode-apps', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:domain:list', 'ionicons5:BusinessOutline',
       NULL, NULL, 1, 0, NULL, '低代码业务领域管理', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @ai_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'ai:lowcode:domain:list'
  );

SET @domain_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:lowcode:domain:list'
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '领域工作台', @domain_menu_id, 2, 1, '/ai/lowcode-domain-workspace', 'ai/lowcode-apps', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:domain:workspace', 'ionicons5:GridOutline',
       NULL, NULL, 1, 0, NULL, '低代码领域工作台', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @domain_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'ai:lowcode:domain:workspace'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '新增业务领域', @domain_menu_id, 3, 1, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:domain:add', NULL,
       NULL, NULL, 0, 0, NULL, '新增业务领域按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @domain_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:domain:add'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '编辑业务领域', @domain_menu_id, 3, 2, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:domain:edit', NULL,
       NULL, NULL, 0, 0, NULL, '编辑业务领域按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @domain_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:domain:edit'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '启停业务领域', @domain_menu_id, 3, 3, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:domain:status', NULL,
       NULL, NULL, 0, 0, NULL, '启停业务领域按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @domain_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:domain:status'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '迁移业务领域', @domain_menu_id, 3, 4, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:domain:move', NULL,
       NULL, NULL, 0, 0, NULL, '迁移低代码应用业务领域按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @domain_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:domain:move'
  );
