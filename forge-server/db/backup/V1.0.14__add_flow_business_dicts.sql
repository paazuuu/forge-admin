-- P1 流程模块业务枚举字典。
-- 流程模型、表单、模板、任务、实例监控等页面统一从字典读取选项和标签。

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, seed.dict_status, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, '流程表单类型' dict_name, 'flow_form_type' dict_type, 1 dict_status, '流程表单管理使用的表单类型' remark
  UNION ALL SELECT 1, '流程定义表单类型', 'flow_process_form_type', 1, '流程模型和模板使用的表单类型'
  UNION ALL SELECT 1, '流程模型状态', 'flow_model_status', 1, '流程模型生命周期状态'
  UNION ALL SELECT 1, 'SPEL 模板分类', 'flow_spel_category', 1, '审批人 SPEL 模板分类'
  UNION ALL SELECT 1, '流程版本标记', 'flow_version_tag', 1, '流程模型版本标记'
  UNION ALL SELECT 1, '流程待办状态', 'flow_todo_status', 1, '我的待办任务状态'
  UNION ALL SELECT 1, '流程已办状态', 'flow_done_status', 1, '我的已办审批结果'
  UNION ALL SELECT 1, '我发起的流程状态', 'flow_started_status', 1, '我发起的流程状态'
  UNION ALL SELECT 1, '流程实例状态', 'flow_instance_status', 1, '流程监控实例状态'
  UNION ALL SELECT 1, '流程错误日志状态', 'flow_error_log_status', 1, '流程监控错误日志处理状态'
  UNION ALL SELECT 1, '流程阅读状态', 'flow_read_status', 1, '流程抄送阅读状态'
  UNION ALL SELECT 1, '流程优先级', 'flow_priority', 1, '流程任务优先级'
  UNION ALL SELECT 1, '流程条件规则类型', 'flow_rule_type', 1, '流程条件规则业务类型'
  UNION ALL SELECT 1, '流程条件运算符', 'flow_rule_operator', 1, '流程条件规则运算符'
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
  SELECT 1 tenant_id, 1 dict_sort, '动态表单' dict_label, 'dynamic' dict_value, 'flow_form_type' dict_type, 'info' list_class, 'Y' is_default, '动态表单' remark
  UNION ALL SELECT 1, 2, '外置表单', 'external', 'flow_form_type', 'warning', 'N', '外置表单'
  UNION ALL SELECT 1, 3, '内置表单', 'builtin', 'flow_form_type', 'success', 'N', '内置表单'
  UNION ALL SELECT 1, 1, '动态表单', 'dynamic', 'flow_process_form_type', 'info', 'Y', '动态表单'
  UNION ALL SELECT 1, 2, '外置表单', 'external', 'flow_process_form_type', 'warning', 'N', '外置表单'
  UNION ALL SELECT 1, 3, '无表单', 'none', 'flow_process_form_type', 'default', 'N', '无表单'
  UNION ALL SELECT 1, 1, '设计中', '0', 'flow_model_status', 'info', 'Y', '设计中'
  UNION ALL SELECT 1, 2, '已部署', '1', 'flow_model_status', 'success', 'N', '已部署'
  UNION ALL SELECT 1, 3, '已挂起', '2', 'flow_model_status', 'warning', 'N', '已挂起'
  UNION ALL SELECT 1, 4, '已禁用', '3', 'flow_model_status', 'error', 'N', '已禁用'
  UNION ALL SELECT 1, 1, '通用', 'general', 'flow_spel_category', 'default', 'Y', '通用'
  UNION ALL SELECT 1, 2, '部门', 'dept', 'flow_spel_category', 'info', 'N', '部门'
  UNION ALL SELECT 1, 3, '角色', 'role', 'flow_spel_category', 'success', 'N', '角色'
  UNION ALL SELECT 1, 4, '行政区划', 'region', 'flow_spel_category', 'warning', 'N', '行政区划'
  UNION ALL SELECT 1, 5, '自定义', 'custom', 'flow_spel_category', 'default', 'N', '自定义'
  UNION ALL SELECT 1, 1, '草稿', 'draft', 'flow_version_tag', 'default', 'Y', '草稿版本'
  UNION ALL SELECT 1, 2, '测试', 'test', 'flow_version_tag', 'info', 'N', '测试版本'
  UNION ALL SELECT 1, 3, '正式发布', 'release', 'flow_version_tag', 'success', 'N', '正式发布版本'
  UNION ALL SELECT 1, 4, '已废弃', 'deprecated', 'flow_version_tag', 'warning', 'N', '已废弃版本'
  UNION ALL SELECT 1, 1, '待办', '0', 'flow_todo_status', 'warning', 'Y', '待办'
  UNION ALL SELECT 1, 2, '已签收', '1', 'flow_todo_status', 'info', 'N', '已签收'
  UNION ALL SELECT 1, 1, '已通过', '2', 'flow_done_status', 'success', 'Y', '已通过'
  UNION ALL SELECT 1, 2, '已驳回', '3', 'flow_done_status', 'error', 'N', '已驳回'
  UNION ALL SELECT 1, 3, '已转办', '4', 'flow_done_status', 'warning', 'N', '已转办'
  UNION ALL SELECT 1, 4, '已委派', '5', 'flow_done_status', 'info', 'N', '已委派'
  UNION ALL SELECT 1, 5, '已撤回', '6', 'flow_done_status', 'default', 'N', '已撤回'
  UNION ALL SELECT 1, 1, '审批中', '0', 'flow_started_status', 'warning', 'Y', '审批中'
  UNION ALL SELECT 1, 2, '已签收', '1', 'flow_started_status', 'info', 'N', '已签收'
  UNION ALL SELECT 1, 3, '已通过', '2', 'flow_started_status', 'success', 'N', '已通过'
  UNION ALL SELECT 1, 4, '已驳回', '3', 'flow_started_status', 'error', 'N', '已驳回'
  UNION ALL SELECT 1, 5, '已转办', '4', 'flow_started_status', 'warning', 'N', '已转办'
  UNION ALL SELECT 1, 6, '已委派', '5', 'flow_started_status', 'info', 'N', '已委派'
  UNION ALL SELECT 1, 7, '已撤回', '6', 'flow_started_status', 'default', 'N', '已撤回'
  UNION ALL SELECT 1, 1, '运行中', 'running', 'flow_instance_status', 'success', 'Y', '运行中'
  UNION ALL SELECT 1, 2, '已挂起', 'suspended', 'flow_instance_status', 'warning', 'N', '已挂起'
  UNION ALL SELECT 1, 3, '已通过', 'approved', 'flow_instance_status', 'success', 'N', '已通过'
  UNION ALL SELECT 1, 4, '已驳回', 'rejected', 'flow_instance_status', 'error', 'N', '已驳回'
  UNION ALL SELECT 1, 5, '已取消', 'canceled', 'flow_instance_status', 'default', 'N', '已取消'
  UNION ALL SELECT 1, 6, '已终止', 'terminated', 'flow_instance_status', 'error', 'N', '已终止'
  UNION ALL SELECT 1, 1, '未处理', '0', 'flow_error_log_status', 'error', 'Y', '未处理'
  UNION ALL SELECT 1, 2, '已重试', '1', 'flow_error_log_status', 'success', 'N', '已重试'
  UNION ALL SELECT 1, 3, '已解决', '2', 'flow_error_log_status', 'default', 'N', '已解决'
  UNION ALL SELECT 1, 4, '重试失败', '3', 'flow_error_log_status', 'warning', 'N', '重试失败'
  UNION ALL SELECT 1, 1, '未读', '0', 'flow_read_status', 'error', 'Y', '未读'
  UNION ALL SELECT 1, 2, '已读', '1', 'flow_read_status', 'success', 'N', '已读'
  UNION ALL SELECT 1, 1, '低', '0', 'flow_priority', 'default', 'N', '低优先级'
  UNION ALL SELECT 1, 2, '普通', '1', 'flow_priority', 'default', 'Y', '普通优先级'
  UNION ALL SELECT 1, 3, '高', '2', 'flow_priority', 'warning', 'N', '高优先级'
  UNION ALL SELECT 1, 4, '紧急', '3', 'flow_priority', 'error', 'N', '紧急优先级'
  UNION ALL SELECT 1, 1, '处理条件', 'approval', 'flow_rule_type', 'info', 'Y', '处理条件'
  UNION ALL SELECT 1, 2, '流转条件', 'flow', 'flow_rule_type', 'success', 'N', '流转条件'
  UNION ALL SELECT 1, 3, '通知条件', 'notify', 'flow_rule_type', 'warning', 'N', '通知条件'
  UNION ALL SELECT 1, 1, '等于', 'eq', 'flow_rule_operator', 'default', 'Y', '等于'
  UNION ALL SELECT 1, 2, '不等于', 'ne', 'flow_rule_operator', 'default', 'N', '不等于'
  UNION ALL SELECT 1, 3, '大于', 'gt', 'flow_rule_operator', 'default', 'N', '大于'
  UNION ALL SELECT 1, 4, '大于等于', 'ge', 'flow_rule_operator', 'default', 'N', '大于等于'
  UNION ALL SELECT 1, 5, '小于', 'lt', 'flow_rule_operator', 'default', 'N', '小于'
  UNION ALL SELECT 1, 6, '小于等于', 'le', 'flow_rule_operator', 'default', 'N', '小于等于'
  UNION ALL SELECT 1, 7, '包含', 'contains', 'flow_rule_operator', 'default', 'N', '包含'
  UNION ALL SELECT 1, 8, '为空', 'empty', 'flow_rule_operator', 'default', 'N', '为空'
  UNION ALL SELECT 1, 9, '不为空', 'notEmpty', 'flow_rule_operator', 'default', 'N', '不为空'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);
