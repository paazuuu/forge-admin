-- P1 AI / 低代码模块业务枚举字典。
-- 提示词模板、大屏生成记录、低代码应用、低代码模型、上下文配置等页面统一从字典读取选项和标签。

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, seed.dict_status, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, '提示词适用场景' dict_name, 'ai_prompt_usage_scene' dict_type, 1 dict_status, 'AI提示词模板适用场景' remark
  UNION ALL SELECT 1, '提示词状态', 'ai_prompt_status', 1, 'AI提示词模板状态'
  UNION ALL SELECT 1, '提示词是否推荐', 'ai_prompt_recommended', 1, 'AI提示词模板推荐标记'
  UNION ALL SELECT 1, '大屏生成状态', 'ai_dashboard_generate_status', 1, 'AI大屏生成记录状态'
  UNION ALL SELECT 1, '低代码应用发布状态', 'lowcode_app_publish_status', 1, '低代码应用发布生命周期状态'
  UNION ALL SELECT 1, '低代码模型状态', 'lowcode_model_status', 1, '低代码数据模型状态'
  UNION ALL SELECT 1, 'AI上下文类型', 'ai_context_type', 1, 'AI上下文配置类型'
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
  SELECT 1 tenant_id, 1 dict_sort, 'AI大屏生成' dict_label, 'dashboard_generate' dict_value, 'ai_prompt_usage_scene' dict_type, 'success' list_class, 'Y' is_default, 'AI大屏生成场景' remark
  UNION ALL SELECT 1, 2, '通用对话', 'chat', 'ai_prompt_usage_scene', 'info', 'N', '通用对话场景'
  UNION ALL SELECT 1, 3, '代码生成', 'code_generate', 'ai_prompt_usage_scene', 'warning', 'N', '代码生成场景'
  UNION ALL SELECT 1, 4, '流程编排', 'flow_design', 'ai_prompt_usage_scene', 'primary', 'N', '流程编排场景'
  UNION ALL SELECT 1, 1, '启用', '0', 'ai_prompt_status', 'success', 'Y', '启用状态' remark
  UNION ALL SELECT 1, 2, '停用', '1', 'ai_prompt_status', 'error', 'N', '停用状态'
  UNION ALL SELECT 1, 1, '推荐', '1', 'ai_prompt_recommended', 'success', 'N', '推荐模板' remark
  UNION ALL SELECT 1, 2, '普通', '0', 'ai_prompt_recommended', 'default', 'Y', '普通模板'
  UNION ALL SELECT 1, 1, '成功', 'success', 'ai_dashboard_generate_status', 'success', 'Y', '生成成功' remark
  UNION ALL SELECT 1, 2, '失败', 'failed', 'ai_dashboard_generate_status', 'error', 'N', '生成失败'
  UNION ALL SELECT 1, 3, '解析失败', 'parse_failed', 'ai_dashboard_generate_status', 'warning', 'N', '解析失败'
  UNION ALL SELECT 1, 4, '已停止', 'stopped', 'ai_dashboard_generate_status', 'default', 'N', '已停止'
  UNION ALL SELECT 1, 1, '草稿', 'DRAFT', 'lowcode_app_publish_status', 'default', 'Y', '草稿状态' remark
  UNION ALL SELECT 1, 2, '已发布', 'PUBLISHED', 'lowcode_app_publish_status', 'success', 'N', '已发布状态'
  UNION ALL SELECT 1, 3, '已停用', 'STOPPED', 'lowcode_app_publish_status', 'warning', 'N', '已停用状态'
  UNION ALL SELECT 1, 1, '启用', 'ENABLED', 'lowcode_model_status', 'success', 'Y', '启用状态' remark
  UNION ALL SELECT 1, 2, '停用', 'DISABLED', 'lowcode_model_status', 'warning', 'N', '停用状态'
  UNION ALL SELECT 1, 1, 'SPEC', 'SPEC', 'ai_context_type', 'info', 'Y', 'SPEC类型' remark
  UNION ALL SELECT 1, 2, 'SAMPLE', 'SAMPLE', 'ai_context_type', 'success', 'N', 'SAMPLE类型'
  UNION ALL SELECT 1, 3, 'RULE', 'RULE', 'ai_context_type', 'warning', 'N', 'RULE类型'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);