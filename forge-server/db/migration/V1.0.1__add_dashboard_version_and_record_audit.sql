-- 大屏发布版本与生成记录稽核入口

CREATE TABLE IF NOT EXISTS `ai_report_project_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` bigint NOT NULL COMMENT '大屏项目ID',
  `project_name` varchar(100) NOT NULL COMMENT '大屏项目名称',
  `version_no` int NOT NULL COMMENT '版本号',
  `version_name` varchar(32) NOT NULL COMMENT '版本名称',
  `operation_type` varchar(20) NOT NULL DEFAULT 'publish' COMMENT '操作类型：publish发布 rollback回退',
  `source_version_id` bigint DEFAULT NULL COMMENT '回退来源版本ID',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人ID',
  `publisher_name` varchar(100) DEFAULT NULL COMMENT '发布人名称',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `publish_url` varchar(500) DEFAULT NULL COMMENT '发布地址',
  `index_img` varchar(500) DEFAULT NULL COMMENT '封面图文件ID',
  `canvas_width` int DEFAULT NULL COMMENT '画布宽度',
  `canvas_height` int DEFAULT NULL COMMENT '画布高度',
  `background_color` varchar(20) DEFAULT NULL COMMENT '背景颜色',
  `component_data` longtext COMMENT '组件配置JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_project_version_no` (`tenant_id`, `project_id`, `version_no`),
  KEY `idx_report_project_version_project` (`tenant_id`, `project_id`, `create_time`),
  KEY `idx_report_project_version_publisher` (`tenant_id`, `publisher_id`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大屏项目发布版本表';

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
SELECT 1, '大屏生成记录', @ai_root_id, 2, 4, '/ai/dashboard-generate-record', 'ai/dashboard-generate-record', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:dashboard-record:list', 'ionicons5:ReceiptOutline',
       NULL, NULL, 1, 0, NULL, 'AI大屏生成记录稽核', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @ai_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'ai:dashboard-record:list'
  );

SET @dashboard_record_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:dashboard-record:list'
  LIMIT 1
);
