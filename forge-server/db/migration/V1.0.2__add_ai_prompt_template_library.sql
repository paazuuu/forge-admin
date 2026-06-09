-- AI 提示词模板库

CREATE TABLE IF NOT EXISTS `ai_prompt_template` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `template_code` varchar(80) DEFAULT NULL COMMENT '模板编码',
  `usage_scene` varchar(64) NOT NULL DEFAULT 'dashboard_generate' COMMENT '使用场景：dashboard_generate大屏生成等',
  `business_category` varchar(64) DEFAULT NULL COMMENT '业务分类',
  `domain_category` varchar(64) DEFAULT NULL COMMENT '领域分类',
  `template_tags` varchar(255) DEFAULT NULL COMMENT '模板标签，逗号分隔',
  `description` varchar(500) DEFAULT NULL COMMENT '模板说明',
  `prompt_content` longtext NOT NULL COMMENT '提示词内容',
  `example_input` text COMMENT '示例输入',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
  `is_recommended` char(1) NOT NULL DEFAULT '0' COMMENT '是否推荐：0否 1是',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `use_count` int NOT NULL DEFAULT 0 COMMENT '使用次数',
  `test_count` int NOT NULL DEFAULT 0 COMMENT '测试次数',
  `download_count` int NOT NULL DEFAULT 0 COMMENT '下载次数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_prompt_template_code` (`tenant_id`, `template_code`),
  KEY `idx_ai_prompt_template_scene` (`tenant_id`, `usage_scene`, `status`, `sort_order`),
  KEY `idx_ai_prompt_template_category` (`tenant_id`, `business_category`, `domain_category`),
  KEY `idx_ai_prompt_template_recommend` (`tenant_id`, `is_recommended`, `use_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI提示词模板库';

INSERT INTO ai_prompt_template (
  id, tenant_id, template_name, template_code, usage_scene, business_category, domain_category,
  template_tags, description, prompt_content, example_input, status, is_recommended, sort_order,
  use_count, test_count, download_count, remark, create_by, create_time, create_dept, update_by, update_time
)
SELECT 1910000000000000001, 1, '数据驱动大屏生成通用模板', 'dashboard_generate_default', 'dashboard_generate',
       '经营分析', '数据大屏', '数据绑定,经营监控,可视化',
       '适用于基于业务定义和数据集生成可落地的大屏，强调动态数据绑定和组件布局。',
       '请基于当前业务定义、数据集字段和运行时样例生成数据大屏。要求：\n1. 优先使用真实 datasetId 生成动态数据组件，指标、趋势、排行、表格、地图尽量输出 request.datasetId、request.datasetFields 和 request.datasetMapping。\n2. 先规划顶部 KPI、核心趋势、分类排行、明细/告警等区域，再选择合适组件，避免只生成装饰元素。\n3. 字段只能使用上下文中出现的 fieldName，不要使用字段中文名作为 request 字段。\n4. 敏感或隐藏字段按上下文安全规则处理，不能展示原始敏感值。\n5. 如果数据集不可用，可以静态兜底，但标题、说明和组件含义必须贴合业务目标。\n6. 输出必须是系统要求的合法 JSON，不要添加 Markdown 包裹或额外解释。',
       '生成经营监控大屏，突出核心指标、趋势分析和异常预警。',
       '0', '1', 1, 0, 0, 0, '系统内置最佳实践模板', 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_prompt_template
  WHERE tenant_id = 1
    AND template_code = 'dashboard_generate_default'
);

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
SELECT 1, '提示词模板库', @ai_root_id, 2, 5, '/ai/prompt-template', 'ai/prompt-template', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:prompt-template:list', 'ionicons5:DocumentTextOutline',
       NULL, NULL, 1, 0, NULL, 'AI提示词模板库', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @ai_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'ai:prompt-template:list'
  );

SET @prompt_template_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:prompt-template:list'
  LIMIT 1
);
