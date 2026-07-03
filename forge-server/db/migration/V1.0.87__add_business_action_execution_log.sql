-- 通用业务动作执行日志。页面按钮、触发器、流程回调后续统一沉淀到该日志。

CREATE TABLE IF NOT EXISTS `ai_business_action_execution_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `suite_code` varchar(64) DEFAULT NULL COMMENT '业务套件编码',
  `object_code` varchar(64) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(128) NOT NULL DEFAULT '' COMMENT '业务记录ID',
  `action_code` varchar(64) NOT NULL COMMENT '动作编码',
  `action_name` varchar(128) DEFAULT NULL COMMENT '动作名称',
  `execute_status` varchar(32) NOT NULL COMMENT '执行状态：RUNNING/SUCCESS/FAILED/TODO',
  `request_digest` varchar(1024) DEFAULT NULL COMMENT '请求摘要',
  `step_result` json DEFAULT NULL COMMENT '步骤执行结果JSON',
  `result_message` varchar(500) DEFAULT NULL COMMENT '执行结果提示',
  `error_message` varchar(2000) DEFAULT NULL COMMENT '错误信息',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时毫秒',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_action_idem` (`tenant_id`, `object_code`, `record_id`, `action_code`, `idempotency_key`),
  KEY `idx_ai_business_action_record` (`tenant_id`, `object_code`, `record_id`, `create_time`),
  KEY `idx_ai_business_action_code` (`tenant_id`, `object_code`, `action_code`, `execute_status`, `create_time`),
  KEY `idx_ai_business_action_corr` (`tenant_id`, `correlation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-通用动作执行日志';

SET @app_center_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center'
  ORDER BY id
  LIMIT 1
);

SET @business_action_parent_id := COALESCE(@app_center_id, 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '执行业务动作', @business_action_parent_id, 3, 210, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessAction:execute', NULL,
       NULL, NULL, 0, 0, NULL, '通用业务动作执行权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource r
  WHERE r.tenant_id = 1
    AND r.client_code = 'pc'
    AND r.perms = 'ai:businessAction:execute'
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '查看业务动作日志', @business_action_parent_id, 3, 211, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessAction:log', NULL,
       NULL, NULL, 0, 0, NULL, '通用业务动作执行日志查看权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource r
  WHERE r.tenant_id = 1
    AND r.client_code = 'pc'
    AND r.perms = 'ai:businessAction:log'
);
