-- Flow form runtime entries, immutable form versions and organization fill batches.

SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form');

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form' AND COLUMN_NAME = 'form_category');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_form ADD COLUMN form_category varchar(64) DEFAULT NULL COMMENT ''表单分类'' AFTER form_name',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form' AND COLUMN_NAME = 'field_registry');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_form ADD COLUMN field_registry json DEFAULT NULL COMMENT ''字段目录快照'' AFTER form_schema',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form' AND COLUMN_NAME = 'default_data_mode');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_form ADD COLUMN default_data_mode varchar(32) NOT NULL DEFAULT ''PROCESS_ONLY'' COMMENT ''默认数据模式'' AFTER form_config',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form' AND COLUMN_NAME = 'publish_status');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_form ADD COLUMN publish_status tinyint NOT NULL DEFAULT 0 COMMENT ''发布状态：0草稿 1已发布'' AFTER status',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_form' AND COLUMN_NAME = 'current_version_id');
SET @sql = IF(@table_exists > 0 AND @column_exists = 0,
              'ALTER TABLE sys_flow_form ADD COLUMN current_version_id bigint DEFAULT NULL COMMENT ''当前发布版本ID'' AFTER publish_status',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `sys_flow_form_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `form_id` bigint NOT NULL COMMENT '流程表单ID',
  `form_key` varchar(128) NOT NULL COMMENT '表单Key',
  `form_name` varchar(128) NOT NULL COMMENT '表单名称',
  `form_category` varchar(64) DEFAULT NULL COMMENT '表单分类',
  `form_type` varchar(32) NOT NULL DEFAULT 'dynamic' COMMENT '表单类型',
  `version` int NOT NULL COMMENT '版本号',
  `form_schema` longtext COMMENT '表单Schema快照',
  `field_registry` json DEFAULT NULL COMMENT '字段目录快照',
  `form_config` json DEFAULT NULL COMMENT '表单配置快照',
  `default_data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '默认数据模式',
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `publish_by` bigint DEFAULT NULL COMMENT '发布人ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_form_version` (`tenant_id`, `form_id`, `version`),
  KEY `idx_flow_form_version_key` (`tenant_id`, `form_key`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程表单发布版本表';

CREATE TABLE IF NOT EXISTS `sys_flow_entry` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `entry_name` varchar(128) NOT NULL COMMENT '入口名称',
  `entry_desc` varchar(500) DEFAULT NULL COMMENT '入口说明',
  `model_key` varchar(128) NOT NULL COMMENT '流程模型Key',
  `form_key` varchar(128) NOT NULL COMMENT '表单Key',
  `form_version_id` bigint NOT NULL COMMENT '表单版本ID',
  `data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '数据模式',
  `object_code` varchar(64) DEFAULT NULL COMMENT '业务对象编码',
  `config_key` varchar(128) DEFAULT NULL COMMENT '动态CRUD配置Key',
  `visible_scope` json DEFAULT NULL COMMENT '可见范围配置',
  `title_template` varchar(256) DEFAULT NULL COMMENT '流程标题模板',
  `business_key_template` varchar(256) DEFAULT NULL COMMENT '业务Key模板',
  `submit_strategy` json DEFAULT NULL COMMENT '提交策略配置',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_entry_code` (`tenant_id`, `entry_code`),
  KEY `idx_flow_entry_model` (`tenant_id`, `model_key`, `status`),
  KEY `idx_flow_entry_form` (`tenant_id`, `form_key`, `form_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程应用入口表';

CREATE TABLE IF NOT EXISTS `sys_flow_entry_field_mapping` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `entry_id` bigint NOT NULL COMMENT '流程入口ID',
  `form_field` varchar(128) NOT NULL COMMENT '表单字段',
  `target_type` varchar(32) NOT NULL DEFAULT 'FLOW_VARIABLE' COMMENT '目标类型：FLOW_VARIABLE/BUSINESS_FIELD',
  `target_field` varchar(128) DEFAULT NULL COMMENT '目标字段',
  `flow_variable` varchar(128) DEFAULT NULL COMMENT '流程变量名',
  `required` tinyint NOT NULL DEFAULT 0 COMMENT '是否必填映射',
  `mapping_config` json DEFAULT NULL COMMENT '映射扩展配置',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_entry_mapping` (`tenant_id`, `entry_id`, `form_field`, `target_type`, `target_field`),
  KEY `idx_flow_entry_mapping_entry` (`tenant_id`, `entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程入口字段映射表';

CREATE TABLE IF NOT EXISTS `sys_flow_form_instance` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `entry_id` bigint DEFAULT NULL COMMENT '流程入口ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `business_key` varchar(256) NOT NULL COMMENT '流程业务Key',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `model_key` varchar(128) NOT NULL COMMENT '流程模型Key',
  `form_key` varchar(128) NOT NULL COMMENT '表单Key',
  `form_version_id` bigint NOT NULL COMMENT '表单版本ID',
  `form_version` int NOT NULL COMMENT '表单版本号',
  `schema_snapshot` longtext COMMENT '表单Schema快照',
  `field_registry` json DEFAULT NULL COMMENT '字段目录快照',
  `form_data` json DEFAULT NULL COMMENT '表单填报数据',
  `data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '数据模式',
  `object_code` varchar(64) DEFAULT NULL COMMENT '业务对象编码',
  `record_id` bigint DEFAULT NULL COMMENT '业务记录ID',
  `title` varchar(256) DEFAULT NULL COMMENT '流程标题',
  `start_user_id` bigint DEFAULT NULL COMMENT '发起人ID',
  `start_user_name` varchar(100) DEFAULT NULL COMMENT '发起人姓名',
  `start_dept_id` bigint DEFAULT NULL COMMENT '发起部门ID',
  `start_dept_name` varchar(128) DEFAULT NULL COMMENT '发起部门名称',
  `status` varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '状态：DRAFT/RUNNING/APPROVED/REJECTED/CANCELED/TERMINATED',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_form_instance_business` (`tenant_id`, `business_key`),
  KEY `idx_flow_form_instance_process` (`tenant_id`, `process_instance_id`),
  KEY `idx_flow_form_instance_entry` (`tenant_id`, `entry_code`, `status`, `create_time`),
  KEY `idx_flow_form_instance_record` (`tenant_id`, `object_code`, `record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程表单填报实例表';

CREATE TABLE IF NOT EXISTS `sys_flow_fill_batch` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `entry_id` bigint NOT NULL COMMENT '流程入口ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `batch_name` varchar(128) NOT NULL COMMENT '批次名称',
  `period_key` varchar(64) DEFAULT NULL COMMENT '周期标识',
  `target_scope` json DEFAULT NULL COMMENT '目标组织范围',
  `owner_rule` json DEFAULT NULL COMMENT '负责人规则',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间',
  `allow_resubmit` tinyint NOT NULL DEFAULT 1 COMMENT '驳回后是否允许重新提交',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '批次状态：DRAFT/PUBLISHED/CLOSED',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_flow_fill_batch_entry` (`tenant_id`, `entry_code`, `status`, `deadline_time`),
  KEY `idx_flow_fill_batch_period` (`tenant_id`, `period_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程组织批量填报批次表';

CREATE TABLE IF NOT EXISTS `sys_flow_fill_batch_item` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `entry_code` varchar(128) NOT NULL COMMENT '入口编码',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `org_name` varchar(128) DEFAULT NULL COMMENT '组织名称',
  `owner_user_id` bigint DEFAULT NULL COMMENT '负责人用户ID',
  `owner_user_name` varchar(100) DEFAULT NULL COMMENT '负责人姓名',
  `form_instance_id` bigint DEFAULT NULL COMMENT '表单实例ID',
  `object_code` varchar(64) DEFAULT NULL COMMENT '业务对象编码',
  `record_id` bigint DEFAULT NULL COMMENT '业务记录ID',
  `process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID',
  `submit_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '填报状态：PENDING/SUBMITTED/RETURNED/OVERDUE',
  `flow_status` varchar(32) DEFAULT NULL COMMENT '流程状态',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0正常 1删除',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_fill_batch_org` (`tenant_id`, `batch_id`, `org_id`),
  KEY `idx_flow_fill_item_owner` (`tenant_id`, `owner_user_id`, `submit_status`),
  KEY `idx_flow_fill_item_process` (`tenant_id`, `process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程组织批量填报明细表';

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '流程数据模式' dict_name, 'sys_flow_data_mode' dict_type, '流程入口数据保存模式' remark
  UNION ALL SELECT 1, '流程入口状态', 'sys_flow_entry_status', '流程入口启停状态'
  UNION ALL SELECT 1, '组织填报批次状态', 'sys_flow_batch_status', '组织批量填报批次状态'
  UNION ALL SELECT 1, '组织填报状态', 'sys_flow_fill_status', '组织批量填报明细状态'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type t
  WHERE t.tenant_id = seed.tenant_id AND t.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
                           dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class,
       seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '仅流程快照' dict_label, 'PROCESS_ONLY' dict_value, 'sys_flow_data_mode' dict_type, 'info' list_class, 'Y' is_default, '只保存流程变量和表单实例快照' remark
  UNION ALL SELECT 1, 2, '业务对象落表', 'BUSINESS_OBJECT', 'sys_flow_data_mode', 'success', 'N', '写入业务对象实际数据表后发起流程'
  UNION ALL SELECT 1, 3, '混合模式', 'HYBRID', 'sys_flow_data_mode', 'warning', 'N', '同时保存业务记录和流程表单快照'
  UNION ALL SELECT 1, 1, '启用', '1', 'sys_flow_entry_status', 'success', 'Y', '入口启用'
  UNION ALL SELECT 1, 2, '禁用', '0', 'sys_flow_entry_status', 'default', 'N', '入口禁用'
  UNION ALL SELECT 1, 1, '草稿', 'DRAFT', 'sys_flow_batch_status', 'default', 'Y', '批次配置草稿'
  UNION ALL SELECT 1, 2, '已发布', 'PUBLISHED', 'sys_flow_batch_status', 'success', 'N', '批次已发布并生成填报明细'
  UNION ALL SELECT 1, 3, '已关闭', 'CLOSED', 'sys_flow_batch_status', 'default', 'N', '批次已关闭'
  UNION ALL SELECT 1, 1, '待填报', 'PENDING', 'sys_flow_fill_status', 'default', 'Y', '待组织负责人填报'
  UNION ALL SELECT 1, 2, '已提交', 'SUBMITTED', 'sys_flow_fill_status', 'info', 'N', '负责人已提交'
  UNION ALL SELECT 1, 3, '审核中', 'RUNNING', 'sys_flow_fill_status', 'warning', 'N', '流程审核中'
  UNION ALL SELECT 1, 4, '已通过', 'APPROVED', 'sys_flow_fill_status', 'success', 'N', '审核通过'
  UNION ALL SELECT 1, 5, '已驳回', 'REJECTED', 'sys_flow_fill_status', 'error', 'N', '审核驳回，可重新提交'
  UNION ALL SELECT 1, 6, '已逾期', 'OVERDUE', 'sys_flow_fill_status', 'error', 'N', '超过截止时间未提交'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

SET @flow_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND (perms = 'flow:model:list' OR path = '/flow/model' OR path = '/flow')
  ORDER BY CASE WHEN path = '/flow' THEN 0 ELSE 1 END, id
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, COALESCE(@flow_menu_id, 0), 2, seed.sort, seed.path, seed.component, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, seed.icon,
       NULL, NULL, 1, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '流程入口' resource_name, 31 sort, '/flow/entry' path, 'flow/entry' component, 'flow:entry:list' perms, 'ionicons5:EnterOutline' icon, '流程表单运行入口管理' remark
  UNION ALL SELECT 1, '组织填报批次', 32, '/flow/fill-batch', 'flow/fillBatch', 'flow:fillBatch:list', 'ionicons5:ClipboardOutline', '组织批量填报任务管理'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = seed.tenant_id
    AND r.resource_type = 2
    AND r.perms = seed.perms
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, COALESCE(@flow_menu_id, 0), 3, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '发布流程表单版本' resource_name, 41 sort, 'flow:form:publish' perms, '发布流程表单不可变版本' remark
  UNION ALL SELECT '维护流程入口', 42, 'flow:entry:edit', '新增或编辑流程入口'
  UNION ALL SELECT '打开流程入口', 43, 'flow:entry:open', '按入口填报并发起流程'
  UNION ALL SELECT '查看流程表单实例', 44, 'flow:runtime:instance', '查看流程表单实例快照'
  UNION ALL SELECT '维护填报批次', 45, 'flow:fillBatch:edit', '新增或编辑组织填报批次'
  UNION ALL SELECT '发布填报批次', 46, 'flow:fillBatch:publish', '发布批次并生成组织填报项'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1
    AND r.resource_type = 3
    AND r.perms = seed.perms
);
