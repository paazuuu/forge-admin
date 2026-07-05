-- 低代码数据模型主数据。模型独立于应用：一个业务领域下可有多个模型，一个应用后续可引用多个模型。

CREATE TABLE IF NOT EXISTS `ai_lowcode_model` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `domain_id` bigint NOT NULL COMMENT '业务领域ID',
  `domain_code` varchar(48) NOT NULL COMMENT '业务领域编码',
  `model_code` varchar(48) NOT NULL COMMENT '模型编码',
  `model_name` varchar(128) NOT NULL COMMENT '模型名称',
  `model_desc` varchar(512) DEFAULT NULL COMMENT '模型描述',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用 DISABLED停用',
  `tenant_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用多租户',
  `master_data` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否主数据模型',
  `model_schema` longtext COMMENT '模型结构协议JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_lowcode_model_code` (`tenant_id`, `domain_id`, `model_code`),
  KEY `idx_ai_lowcode_model_domain` (`tenant_id`, `domain_id`, `status`),
  KEY `idx_ai_lowcode_model_master` (`tenant_id`, `master_data`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI低代码数据模型表';

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
SELECT 1, '数据模型设计', @domain_menu_id, 2, 2, '/ai/lowcode-models', 'ai/lowcode-models', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:model:list', 'ionicons5:GitBranchOutline',
       NULL, NULL, 1, 0, NULL, '低代码数据模型设计', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @domain_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'ai:lowcode:model:list'
  );

SET @model_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:lowcode:model:list'
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '新增数据模型', @model_menu_id, 3, 1, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:model:add', NULL,
       NULL, NULL, 0, 0, NULL, '新增低代码数据模型按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @model_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:model:add'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '编辑数据模型', @model_menu_id, 3, 2, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:model:edit', NULL,
       NULL, NULL, 0, 0, NULL, '编辑低代码数据模型按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @model_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:model:edit'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '启停数据模型', @model_menu_id, 3, 3, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:lowcode:model:status', NULL,
       NULL, NULL, 0, 0, NULL, '启停低代码数据模型按钮权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @model_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 3
      AND perms = 'ai:lowcode:model:status'
  );
