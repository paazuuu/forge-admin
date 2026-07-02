-- 通用编码规则生成器：表单字段自动编号与单据编号共用。

CREATE TABLE IF NOT EXISTS `ai_code_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `scene` varchar(64) NOT NULL DEFAULT 'COMMON' COMMENT '适用场景：COMMON通用/DOCUMENT单据/MATERIAL物料等',
  `template` varchar(256) NOT NULL COMMENT '编码模板',
  `reset_policy` varchar(32) NOT NULL DEFAULT 'AUTO' COMMENT '流水重置策略：AUTO/NONE/YEAR/MONTH/DAY/HOUR/MINUTE/SECOND',
  `seq_length` int NOT NULL DEFAULT 4 COMMENT '默认流水号长度',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `builtin` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否内置规则',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `options` json DEFAULT NULL COMMENT '扩展配置JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_code_rule_code` (`tenant_id`, `rule_code`),
  KEY `idx_ai_code_rule_scene` (`tenant_id`, `scene`, `status`),
  KEY `idx_ai_code_rule_update` (`tenant_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-通用编码规则';

INSERT INTO ai_code_rule (id, tenant_id, rule_code, rule_name, scene, template, reset_policy, seq_length,
                          status, builtin, remark, options, create_by, create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, seed.rule_code, seed.rule_name, seed.scene, seed.template, seed.reset_policy, seed.seq_length,
       1, 1, seed.remark, NULL, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000008601 id, 'material_code' rule_code, '物料编码' rule_name, 'COMMON' scene,
         'WL${yyyyMMddHHmmss}${seq:3}' template, 'SECOND' reset_policy, 3 seq_length,
         '适合物料编码：WL + 年月日时分秒 + 3位同秒流水' remark
  UNION ALL
  SELECT 1910000000000008602, 'document_daily_no', '通用单据日流水', 'COMMON',
         'DOC${yyyyMMdd}${seq:4}', 'DAY', 4,
         '适合普通业务单据：DOC + 年月日 + 4位日流水'
  UNION ALL
  SELECT 1910000000000008603, 'order_no', '订单编号', 'COMMON',
         'ORD${yyyyMMdd}${seq:5}', 'DAY', 5,
         '适合订单类单据：ORD + 年月日 + 5位日流水'
  UNION ALL
  SELECT 1910000000000008604, 'contract_no', '合同编号', 'COMMON',
         'HT${yyyyMM}${seq:5}', 'MONTH', 5,
         '适合合同类单据：HT + 年月 + 5位月流水'
  UNION ALL
  SELECT 1910000000000008605, 'customer_code', '客户编码', 'COMMON',
         'C${seq:6}', 'NONE', 6,
         '适合主数据编码：C + 6位全局流水'
  UNION ALL
  SELECT 1910000000000008606, 'org_daily_no', '组织日流水', 'COMMON',
         '${orgCode}${yyyyMMdd}${seq:4}', 'DAY', 4,
         '适合按组织隔离的日流水：组织编码 + 年月日 + 4位流水'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_code_rule r
  WHERE r.tenant_id = 1
    AND r.rule_code = seed.rule_code
);

SET @app_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 1
    AND resource_name = '应用中心'
  ORDER BY id
  LIMIT 1
);

SET @app_overview_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center'
    AND component = 'app-center/index'
  ORDER BY id
  LIMIT 1
);

SET @code_rule_parent_id := COALESCE(@app_root_id, @app_overview_id, 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '编码规则', @code_rule_parent_id, 2, 8, '/app-center/code-rules', 'app-center/code-rules', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessObject:design', 'ionicons5:BarcodeOutline',
       NULL, NULL, 0, 0, NULL, '通用编码规则维护页面', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource r
  WHERE r.tenant_id = 1
    AND r.client_code = 'pc'
    AND r.resource_type = 2
    AND r.path = '/app-center/code-rules'
);

SET @code_rule_route_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/app-center/code-rules'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT source.tenant_id, source.role_id, @code_rule_route_id, NOW()
FROM (
  SELECT DISTINCT rr.tenant_id, rr.role_id
  FROM sys_role_resource rr
  INNER JOIN sys_resource src ON src.id = rr.resource_id
  WHERE src.tenant_id = 1
    AND (
      src.id IN (@app_root_id, @app_overview_id)
      OR src.path LIKE '/app-center%'
      OR src.perms = 'ai:businessObject:design'
    )
) source
WHERE @code_rule_route_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = source.tenant_id
      AND exists_rr.role_id = source.role_id
      AND exists_rr.resource_id = @code_rule_route_id
  );
