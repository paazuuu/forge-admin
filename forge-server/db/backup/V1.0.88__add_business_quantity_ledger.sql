-- 通用数量台账。用于库存、额度、席位等数量型领域能力，不绑定具体业务场景。

CREATE TABLE IF NOT EXISTS `ai_business_quantity_balance` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `account_code` varchar(128) NOT NULL COMMENT '数量账户编码',
  `item_code` varchar(128) NOT NULL COMMENT '数量项编码',
  `dimension_key` varchar(256) NOT NULL DEFAULT '' COMMENT '数量维度键',
  `quantity` bigint NOT NULL DEFAULT 0 COMMENT '当前数量',
  `locked_quantity` bigint NOT NULL DEFAULT 0 COMMENT '锁定数量',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_qty_balance` (`tenant_id`, `account_code`, `item_code`, `dimension_key`),
  KEY `idx_ai_business_qty_balance_item` (`tenant_id`, `item_code`, `dimension_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-通用数量余额';

CREATE TABLE IF NOT EXISTS `ai_business_quantity_ledger` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型：INBOUND/LOCK/RELEASE/COMMIT/TRANSFER',
  `account_code` varchar(128) NOT NULL COMMENT '数量账户编码',
  `item_code` varchar(128) NOT NULL COMMENT '数量项编码',
  `dimension_key` varchar(256) NOT NULL DEFAULT '' COMMENT '数量维度键',
  `quantity_delta` bigint NOT NULL DEFAULT 0 COMMENT '数量变动值',
  `balance_quantity` bigint NOT NULL DEFAULT 0 COMMENT '操作后当前数量',
  `locked_quantity` bigint NOT NULL DEFAULT 0 COMMENT '操作后锁定数量',
  `target_account_code` varchar(128) DEFAULT NULL COMMENT '目标数量账户编码',
  `target_item_code` varchar(128) DEFAULT NULL COMMENT '目标数量项编码',
  `target_dimension_key` varchar(256) DEFAULT NULL COMMENT '目标数量维度键',
  `source_object_code` varchar(64) DEFAULT NULL COMMENT '来源业务对象编码',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源业务记录ID',
  `source_detail_id` varchar(128) DEFAULT NULL COMMENT '来源明细ID',
  `lock_id` bigint DEFAULT NULL COMMENT '关联锁定ID',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `extra_data` json DEFAULT NULL COMMENT '扩展数据',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_qty_ledger_idem` (`tenant_id`, `idempotency_key`),
  KEY `idx_ai_business_qty_ledger_item` (`tenant_id`, `account_code`, `item_code`, `dimension_key`, `create_time`),
  KEY `idx_ai_business_qty_ledger_source` (`tenant_id`, `source_object_code`, `source_record_id`),
  KEY `idx_ai_business_qty_ledger_corr` (`tenant_id`, `correlation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-通用数量流水';

CREATE TABLE IF NOT EXISTS `ai_business_quantity_lock` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `lock_code` varchar(128) DEFAULT NULL COMMENT '锁定编码',
  `account_code` varchar(128) NOT NULL COMMENT '数量账户编码',
  `item_code` varchar(128) NOT NULL COMMENT '数量项编码',
  `dimension_key` varchar(256) NOT NULL DEFAULT '' COMMENT '数量维度键',
  `lock_quantity` bigint NOT NULL DEFAULT 0 COMMENT '锁定数量',
  `released_quantity` bigint NOT NULL DEFAULT 0 COMMENT '已释放数量',
  `committed_quantity` bigint NOT NULL DEFAULT 0 COMMENT '已扣减数量',
  `lock_status` varchar(32) NOT NULL DEFAULT 'LOCKED' COMMENT '锁定状态：LOCKED/RELEASED/COMMITTED',
  `source_object_code` varchar(64) DEFAULT NULL COMMENT '来源业务对象编码',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源业务记录ID',
  `source_detail_id` varchar(128) DEFAULT NULL COMMENT '来源明细ID',
  `correlation_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_qty_lock_idem` (`tenant_id`, `idempotency_key`),
  KEY `idx_ai_business_qty_lock_item` (`tenant_id`, `account_code`, `item_code`, `dimension_key`, `lock_status`),
  KEY `idx_ai_business_qty_lock_source` (`tenant_id`, `source_object_code`, `source_record_id`, `source_detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-通用数量锁定';

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

SET @quantity_parent_id := COALESCE(@app_center_id, 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '执行数量台账动作', @quantity_parent_id, 3, 212, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessQuantity:operate', NULL,
       NULL, NULL, 0, 0, NULL, '通用数量台账动作权限', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource r
  WHERE r.tenant_id = 1
    AND r.client_code = 'pc'
    AND r.perms = 'ai:businessQuantity:operate'
);
