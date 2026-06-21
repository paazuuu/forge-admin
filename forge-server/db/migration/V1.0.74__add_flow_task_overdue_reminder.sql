-- Add flow task overdue reminder configuration and idempotent send records.

CREATE TABLE IF NOT EXISTS `sys_flow_overdue_reminder_record` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    `task_id` varchar(64) NOT NULL COMMENT 'Flowable任务ID',
    `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
    `process_def_key` varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
    `task_def_key` varchar(100) DEFAULT NULL COMMENT '任务定义Key',
    `reminder_key` varchar(160) NOT NULL COMMENT '提醒批次键',
    `channel` varchar(20) NOT NULL COMMENT '推送渠道',
    `template_code` varchar(50) DEFAULT NULL COMMENT '消息模板编码',
    `receiver_user_ids` text COMMENT '接收人用户ID集合',
    `message_id` bigint DEFAULT NULL COMMENT '消息ID',
    `send_status` tinyint NOT NULL DEFAULT 0 COMMENT '发送状态：0-待发送/1-成功/2-失败',
    `send_time` datetime DEFAULT NULL COMMENT '发送时间',
    `error_message` varchar(1000) DEFAULT NULL COMMENT '失败原因',
    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
    `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_overdue_reminder` (`tenant_id`, `reminder_key`, `channel`),
    KEY `idx_flow_overdue_task` (`tenant_id`, `task_id`, `send_time`),
    KEY `idx_flow_overdue_status` (`tenant_id`, `send_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务逾期提醒记录';

SET @node_config_table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_node_config'
);

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_node_config'
    AND column_name = 'overdue_reminder_enabled'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_enabled tinyint NOT NULL DEFAULT 0 COMMENT ''是否启用逾期提醒''',
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
    AND column_name = 'overdue_reminder_template_code'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_template_code varchar(50) DEFAULT NULL COMMENT ''逾期提醒消息模板编码''',
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
    AND column_name = 'overdue_reminder_channels'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_channels varchar(200) DEFAULT NULL COMMENT ''逾期提醒推送渠道，逗号分隔''',
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
    AND column_name = 'overdue_reminder_repeat_mode'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_repeat_mode varchar(32) NOT NULL DEFAULT ''once'' COMMENT ''逾期提醒重复策略：once/interval''',
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
    AND column_name = 'overdue_reminder_interval_minutes'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_interval_minutes int NOT NULL DEFAULT 1440 COMMENT ''重复提醒间隔分钟''',
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
    AND column_name = 'overdue_reminder_max_times'
);
SET @ddl := IF(@node_config_table_exists > 0 AND @column_exists = 0,
  'ALTER TABLE sys_flow_node_config ADD COLUMN overdue_reminder_max_times int NOT NULL DEFAULT 1 COMMENT ''最大提醒次数''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, '流程逾期提醒重复策略', 'sys_flow_overdue_repeat_mode', 1, '审批任务逾期提醒重复发送策略', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type
  WHERE tenant_id = 1
    AND dict_type = 'sys_flow_overdue_repeat_mode'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default,
                           dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, 'sys_flow_overdue_repeat_mode',
       NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '仅一次' dict_label, 'once' dict_value, 'info' list_class, 'Y' is_default, '逾期后仅提醒一次' remark
  UNION ALL SELECT 1, 2, '按间隔重复', 'interval', 'warning', 'N', '逾期后按配置间隔重复提醒'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = 'sys_flow_overdue_repeat_mode'
    AND d.dict_value = seed.dict_value
);

INSERT INTO sys_message_template (tenant_id, template_code, template_name, type, title_template,
                                  content_template, default_channel, enabled, remark,
                                  create_by, create_time, update_by, update_time)
SELECT 1, 'FLOW_TASK_OVERDUE', '流程任务逾期提醒', 'SYSTEM',
       CONCAT('流程任务逾期：', '$', '{taskName}'),
       CONCAT('流程「', '$', '{processName}', '」的任务「', '$', '{taskName}',
              '」已逾期，截止时间：', '$', '{dueDate}', '，逾期 ', '$', '{overdueMinutes}',
              ' 分钟。请及时处理：', '$', '{jumpUrl}'),
       'WEB', 1, '审批任务逾期后提醒当前审批人', 1, NOW(), 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_message_template
  WHERE tenant_id = 1
    AND template_code = 'FLOW_TASK_OVERDUE'
);

INSERT INTO sys_message_biz_type (tenant_id, biz_type, biz_name, jump_url, jump_target, icon,
                                  sort, enabled, remark, create_by, create_time, update_by, update_time)
SELECT 1, 'FLOW_TASK_OVERDUE', '流程任务逾期提醒', CONCAT('/flow/todo?taskId=', '$', '{taskId}'), '_self',
       'ionicons5:AlarmOutline', 30, 1, '流程审批任务逾期提醒消息', 1, NOW(), 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_message_biz_type
  WHERE tenant_id = 1
    AND biz_type = 'FLOW_TASK_OVERDUE'
);
