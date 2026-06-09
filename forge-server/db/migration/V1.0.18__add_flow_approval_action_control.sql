-- 流程审批动作权限与办理要求控制

SET @node_config_table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_node_config'
);

SET @flow_task_table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_task'
);

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_node_config'
    AND column_name = 'allow_approve'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN allow_approve tinyint DEFAULT 1 COMMENT ''允许通过'' AFTER timeout_action',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_node_config'
    AND column_name = 'allow_return'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN allow_return tinyint DEFAULT 0 COMMENT ''允许退回上一审批节点'' AFTER allow_counter_sign',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_node_config'
    AND column_name = 'allow_terminate'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN allow_terminate tinyint DEFAULT 0 COMMENT ''允许终结流程'' AFTER allow_return',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_node_config'
    AND column_name = 'require_signature'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN require_signature tinyint DEFAULT 0 COMMENT ''办理时需要签名'' AFTER allow_terminate',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_node_config'
    AND column_name = 'require_comment'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN require_comment tinyint DEFAULT 1 COMMENT ''办理时需要审批意见'' AFTER require_signature',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_task'
    AND column_name = 'signature'
);
SET @ddl := IF(@flow_task_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_task ADD COLUMN signature text COMMENT ''审批签名'' AFTER comment',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, 'N', 1, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, 8 dict_sort, '已退回' dict_label, '7' dict_value, 'flow_todo_status' dict_type, 'warning' list_class, '审批任务已退回上一节点' remark
  UNION ALL SELECT 1, 9, '已终结', '8', 'flow_todo_status', 'error', '审批人终结流程'
  UNION ALL SELECT 1, 6, '已退回', '7', 'flow_done_status', 'warning', '审批任务已退回上一节点'
  UNION ALL SELECT 1, 7, '已终结', '8', 'flow_done_status', 'error', '审批人终结流程'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);
