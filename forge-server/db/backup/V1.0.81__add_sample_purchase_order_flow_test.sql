-- Sample purchase order approval flow test business.

CREATE TABLE IF NOT EXISTS `sample_purchase_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `order_no` varchar(64) NOT NULL COMMENT '采购单号',
  `title` varchar(128) NOT NULL COMMENT '采购主题',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商',
  `amount_cent` bigint NOT NULL DEFAULT 0 COMMENT '采购金额，单位分',
  `purchase_items` text DEFAULT NULL COMMENT '采购明细',
  `need_date` date DEFAULT NULL COMMENT '需求日期',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '业务状态',
  `applicant_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(64) DEFAULT NULL COMMENT '申请人名称',
  `applicant_dept_id` bigint DEFAULT NULL COMMENT '申请部门ID',
  `applicant_dept_name` varchar(128) DEFAULT NULL COMMENT '申请部门名称',
  `business_key` varchar(128) DEFAULT NULL COMMENT '流程业务Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `dept_leader_id` bigint DEFAULT NULL COMMENT '部门负责人ID',
  `engineering_manager_id` bigint DEFAULT NULL COMMENT '工程部经理ID',
  `countersign_user_ids` varchar(500) DEFAULT NULL COMMENT '会签用户ID，逗号分隔',
  `cc_role_keys` varchar(500) DEFAULT NULL COMMENT '流程完成抄送角色编码，逗号分隔',
  `arrival_list_file_ids` varchar(1000) DEFAULT NULL COMMENT '负责人节点上传清单文件ID',
  `applicant_modify_remark` varchar(500) DEFAULT NULL COMMENT '申请人修改说明',
  `dept_leader_remark` varchar(500) DEFAULT NULL COMMENT '部门负责人意见',
  `engineering_manager_remark` varchar(500) DEFAULT NULL COMMENT '工程部经理意见',
  `countersign_remark` varchar(500) DEFAULT NULL COMMENT '会签意见',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sample_purchase_order_no` (`tenant_id`, `order_no`),
  UNIQUE KEY `uk_sample_purchase_order_business_key` (`tenant_id`, `business_key`),
  KEY `idx_sample_purchase_order_status` (`tenant_id`, `status`, `update_time`),
  KEY `idx_sample_purchase_order_process` (`tenant_id`, `process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单审批测试表';

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, seed.dict_status, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, '采购单审批测试状态' dict_name, 'sample_purchase_order_status' dict_type, 1 dict_status, '采购单审批测试业务状态' remark
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
  SELECT 1 tenant_id, 1 dict_sort, '草稿' dict_label, 'DRAFT' dict_value, 'sample_purchase_order_status' dict_type, 'default' list_class, 'Y' is_default, '草稿' remark
  UNION ALL SELECT 1, 2, '审批中', 'IN_PROCESS', 'sample_purchase_order_status', 'info', 'N', '审批中'
  UNION ALL SELECT 1, 3, '待修改', 'NEED_MODIFY', 'sample_purchase_order_status', 'warning', 'N', '被驳回后等待申请人修改'
  UNION ALL SELECT 1, 4, '已通过', 'APPROVED', 'sample_purchase_order_status', 'success', 'N', '审批通过'
  UNION ALL SELECT 1, 5, '已拒绝', 'REJECTED', 'sample_purchase_order_status', 'error', 'N', '流程拒绝结束'
  UNION ALL SELECT 1, 6, '已取消', 'CANCELED', 'sample_purchase_order_status', 'default', 'N', '流程取消'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

INSERT INTO sys_role (tenant_id, role_name, role_key, role_type, data_scope, sort, role_status, is_system, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, '总经理', 'general_manager', 3, 1, 90, 1, 0, '采购单审批测试抄送角色，可按需绑定用户', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role
  WHERE tenant_id = 1
    AND role_key = 'general_manager'
);

SET @app_center_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 1
    AND parent_id = 0
    AND resource_name = '应用中心'
  ORDER BY id
  LIMIT 1
);

SET @developer_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 1
    AND parent_id = 0
    AND resource_name = '开发者工具'
  ORDER BY id
  LIMIT 1
);

SET @business_parent_id := COALESCE(@app_center_root_id, @developer_root_id, 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '业务测试', @business_parent_id, 1, 30, '/business', NULL, 0,
       0, NULL, '_self', 0, 1, 1, 'business:test', 'ionicons5:BriefcaseOutline',
       NULL, NULL, 0, 1, NULL, '业务测试目录', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 1
    AND path = '/business'
);

SET @business_test_dir_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 1
    AND path = '/business'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '采购单审批测试', @business_test_dir_id, 2, 10, '/business/purchase-order-test', 'business/purchase-order-test', 0,
       0, NULL, '_self', 0, 1, 1, 'business:purchaseOrderTest:list', 'ionicons5:CartOutline',
       NULL, NULL, 0, 0, NULL, '采购单审批流程联动测试页面', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @business_test_dir_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND client_code = 'pc'
      AND resource_type = 2
      AND path = '/business/purchase-order-test'
  );

SET @purchase_test_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND client_code = 'pc'
    AND resource_type = 2
    AND path = '/business/purchase-order-test'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, @purchase_test_menu_id, 3, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '采购单查询' resource_name, 1 sort, 'business:purchaseOrderTest:query' perms, '采购单查询按钮权限' remark
  UNION ALL SELECT 1, '采购单新增', 2, 'business:purchaseOrderTest:add', '采购单新增按钮权限'
  UNION ALL SELECT 1, '采购单修改', 3, 'business:purchaseOrderTest:edit', '采购单修改按钮权限'
  UNION ALL SELECT 1, '采购单删除', 4, 'business:purchaseOrderTest:remove', '采购单删除按钮权限'
  UNION ALL SELECT 1, '采购单提交审批', 5, 'business:purchaseOrderTest:submit', '采购单提交审批按钮权限'
  UNION ALL SELECT 1, '采购单初始化流程', 6, 'business:purchaseOrderTest:initFlow', '采购单测试流程初始化按钮权限'
) seed
WHERE @purchase_test_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id
      AND r.client_code = 'pc'
      AND r.resource_type = 3
      AND r.perms = seed.perms
  );

SET @admin_role_id := (
  SELECT id
  FROM sys_role
  WHERE tenant_id = 1
    AND role_key = 'admin'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, @admin_role_id, target.id, NOW()
FROM sys_resource target
WHERE @admin_role_id IS NOT NULL
  AND target.tenant_id = 1
  AND target.client_code = 'pc'
  AND (
    target.path IN ('/business', '/business/purchase-order-test')
    OR target.perms LIKE 'business:purchaseOrderTest:%'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = 1
      AND exists_rr.role_id = @admin_role_id
      AND exists_rr.resource_id = target.id
  );
