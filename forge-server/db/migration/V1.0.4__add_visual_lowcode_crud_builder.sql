-- AI 低代码 CRUD 可视化搭建体系基础表结构

-- ai_crud_config 增量字段：使用 information_schema 防重复，兼容已部署环境。
SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_crud_config'
    AND column_name = 'app_name'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN app_name varchar(128) DEFAULT NULL COMMENT ''低代码应用名称'' AFTER table_comment',
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
    AND column_name = 'build_mode'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN build_mode varchar(16) NOT NULL DEFAULT ''AI'' COMMENT ''构建模式：AI智能生成 LOWCODE可视化低代码 CODEGEN代码生成'' AFTER mode',
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
    AND column_name = 'publish_status'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN publish_status varchar(16) NOT NULL DEFAULT ''DRAFT'' COMMENT ''发布状态：DRAFT草稿 PUBLISHED已发布 STOPPED已停用'' AFTER status',
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
    AND column_name = 'model_schema'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN model_schema json DEFAULT NULL COMMENT ''可视化数据模型协议'' AFTER layout_type',
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
    AND column_name = 'page_schema'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN page_schema json DEFAULT NULL COMMENT ''可视化页面搭建协议'' AFTER model_schema',
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
    AND column_name = 'draft_version'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN draft_version int NOT NULL DEFAULT 0 COMMENT ''草稿版本号'' AFTER page_schema',
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
    AND column_name = 'published_version'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN published_version int NOT NULL DEFAULT 0 COMMENT ''已发布版本号'' AFTER draft_version',
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
    AND column_name = 'publish_time'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN publish_time datetime DEFAULT NULL COMMENT ''发布时间'' AFTER published_version',
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
    AND column_name = 'publish_by'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_crud_config ADD COLUMN publish_by bigint DEFAULT NULL COMMENT ''发布人'' AFTER publish_time',
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
    AND index_name = 'idx_ai_crud_publish_status'
);
SET @ddl := IF(@index_exists = 0,
  'ALTER TABLE ai_crud_config ADD INDEX idx_ai_crud_publish_status (tenant_id, publish_status, update_time)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ai_crud_config
SET app_name = COALESCE(NULLIF(menu_name, ''), NULLIF(table_comment, ''), config_key)
WHERE app_name IS NULL;

UPDATE ai_crud_config
SET publish_status = 'PUBLISHED',
    published_version = CASE WHEN published_version = 0 THEN 1 ELSE published_version END
WHERE publish_status = 'DRAFT'
  AND mode = 'CONFIG';

CREATE TABLE IF NOT EXISTS `ai_crud_config_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `config_id` bigint NOT NULL COMMENT 'CRUD配置ID',
  `config_key` varchar(64) NOT NULL COMMENT 'CRUD配置键',
  `version_no` int NOT NULL COMMENT '版本号',
  `version_type` varchar(20) NOT NULL DEFAULT 'publish' COMMENT '版本类型：publish发布 rollback回滚',
  `model_schema` json DEFAULT NULL COMMENT '可视化数据模型协议',
  `page_schema` json DEFAULT NULL COMMENT '可视化页面搭建协议',
  `search_schema` json DEFAULT NULL COMMENT '搜索表单Schema JSON',
  `columns_schema` json DEFAULT NULL COMMENT '表格列Schema JSON',
  `edit_schema` json DEFAULT NULL COMMENT '编辑表单Schema JSON',
  `api_config` json DEFAULT NULL COMMENT 'API配置JSON',
  `options` json DEFAULT NULL COMMENT 'AiCrudPage运行时配置',
  `publish_snapshot` longtext COMMENT '发布快照JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_crud_version_no` (`tenant_id`, `config_id`, `version_no`),
  KEY `idx_ai_crud_version_config` (`tenant_id`, `config_id`, `create_time`),
  KEY `idx_ai_crud_version_key` (`tenant_id`, `config_key`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI CRUD低代码发布版本表';

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
SELECT 1, '低代码应用', @ai_root_id, 2, 13, '/ai/lowcode-apps', 'ai/lowcode-apps', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:list', 'ionicons5:ConstructOutline',
       NULL, NULL, 1, 0, NULL, 'AI低代码应用管理', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @ai_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'ai:lowcode:list'
  );

SET @lowcode_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:lowcode:list'
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '低代码搭建器', @ai_root_id, 2, 14, '/ai/lowcode-builder', 'ai/lowcode-builder', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:edit', 'ionicons5:BuildOutline',
       NULL, NULL, 1, 0, NULL, 'AI低代码可视化搭建器', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @ai_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'ai:lowcode:edit'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '发布低代码应用', @lowcode_menu_id, 3, 1, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:publish', NULL,
       NULL, NULL, 0, 0, NULL, '发布低代码应用按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:publish'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '回滚低代码应用', @lowcode_menu_id, 3, 2, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:rollback', NULL,
       NULL, NULL, 0, 0, NULL, '回滚低代码应用按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:rollback'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '在线建表发布', @lowcode_menu_id, 3, 3, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:deploy-ddl', NULL,
       NULL, NULL, 0, 0, NULL, '低代码在线建表发布按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @lowcode_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:deploy-ddl'
  );
