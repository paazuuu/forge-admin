-- P0 业务枚举字典。
-- 前端用户、角色、岗位、租户、通知公告、消息中心等页面统一从字典读取选项和标签。

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, seed.dict_status, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, '通用正常禁用' dict_name, 'sys_normal_disable' dict_type, 1 dict_status, '通用正常/禁用状态' remark
  UNION ALL SELECT 1, '通用启用禁用', 'sys_enable_disable', 1, '通用启用/禁用状态'
  UNION ALL SELECT 1, '通用是否', 'sys_yes_no', 1, '通用是否选项'
  UNION ALL SELECT 1, '用户类型', 'sys_user_type', 1, '系统用户类型'
  UNION ALL SELECT 1, '用户状态', 'sys_user_status', 1, '系统用户状态'
  UNION ALL SELECT 1, '用户性别', 'sys_user_sex', 1, '系统用户性别'
  UNION ALL SELECT 1, '角色数据范围', 'sys_role_data_scope', 1, '角色数据权限范围'
  UNION ALL SELECT 1, '岗位类型', 'sys_post_type', 1, '系统岗位类型'
  UNION ALL SELECT 1, '公告类型', 'sys_notice_type', 1, '通知公告类型'
  UNION ALL SELECT 1, '公告发布状态', 'sys_notice_status', 1, '通知公告发布状态'
  UNION ALL SELECT 1, '消息类型', 'sys_message_type', 1, '消息中心消息类型'
  UNION ALL SELECT 1, '消息渠道', 'sys_message_channel', 1, '消息中心发送渠道'
  UNION ALL SELECT 1, '消息发送范围', 'sys_message_send_scope', 1, '消息中心发送范围'
  UNION ALL SELECT 1, '消息发送状态', 'sys_message_send_status', 1, '消息中心发送状态'
  UNION ALL SELECT 1, '消息阅读状态', 'sys_message_read_status', 1, '消息中心阅读状态'
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
  SELECT 1 tenant_id, 1 dict_sort, '正常' dict_label, '1' dict_value, 'sys_normal_disable' dict_type, 'success' list_class, 'Y' is_default, '正常状态' remark
  UNION ALL SELECT 1, 2, '禁用', '0', 'sys_normal_disable', 'error', 'N', '禁用状态'
  UNION ALL SELECT 1, 1, '启用', '1', 'sys_enable_disable', 'success', 'Y', '启用状态'
  UNION ALL SELECT 1, 2, '禁用', '0', 'sys_enable_disable', 'error', 'N', '禁用状态'
  UNION ALL SELECT 1, 1, '是', '1', 'sys_yes_no', 'success', 'N', '是'
  UNION ALL SELECT 1, 2, '否', '0', 'sys_yes_no', 'default', 'Y', '否'
  UNION ALL SELECT 1, 1, '系统管理员', '0', 'sys_user_type', 'warning', 'N', '系统管理员'
  UNION ALL SELECT 1, 2, '租户管理员', '1', 'sys_user_type', 'info', 'Y', '租户管理员'
  UNION ALL SELECT 1, 3, '普通用户', '2', 'sys_user_type', 'default', 'N', '普通用户'
  UNION ALL SELECT 1, 1, '禁用', '0', 'sys_user_status', 'error', 'N', '用户禁用'
  UNION ALL SELECT 1, 2, '正常', '1', 'sys_user_status', 'success', 'Y', '用户正常'
  UNION ALL SELECT 1, 3, '锁定', '2', 'sys_user_status', 'warning', 'N', '用户锁定'
  UNION ALL SELECT 1, 1, '未知', '0', 'sys_user_sex', 'default', 'Y', '性别未知'
  UNION ALL SELECT 1, 2, '男', '1', 'sys_user_sex', 'info', 'N', '男'
  UNION ALL SELECT 1, 3, '女', '2', 'sys_user_sex', 'error', 'N', '女'
  UNION ALL SELECT 1, 1, '全部数据', '1', 'sys_role_data_scope', 'success', 'N', '全部数据权限'
  UNION ALL SELECT 1, 2, '本租户数据', '2', 'sys_role_data_scope', 'info', 'Y', '本租户数据'
  UNION ALL SELECT 1, 3, '本组织数据', '3', 'sys_role_data_scope', 'warning', 'N', '本组织数据'
  UNION ALL SELECT 1, 4, '本组织及子组织', '4', 'sys_role_data_scope', 'warning', 'N', '本组织及子组织'
  UNION ALL SELECT 1, 5, '个人数据', '5', 'sys_role_data_scope', 'default', 'N', '个人数据'
  UNION ALL SELECT 1, 7, '本行政区划数据', '7', 'sys_role_data_scope', 'primary', 'N', '本行政区划数据'
  UNION ALL SELECT 1, 1, '管理岗', '1', 'sys_post_type', 'success', 'N', '管理岗位'
  UNION ALL SELECT 1, 2, '技术岗', '2', 'sys_post_type', 'info', 'N', '技术岗位'
  UNION ALL SELECT 1, 3, '业务岗', '3', 'sys_post_type', 'warning', 'N', '业务岗位'
  UNION ALL SELECT 1, 4, '其他', '4', 'sys_post_type', 'default', 'Y', '其他岗位'
  UNION ALL SELECT 1, 1, '通知公告', 'NOTICE', 'sys_notice_type', 'info', 'Y', '普通通知公告'
  UNION ALL SELECT 1, 2, '系统公告', 'ANNOUNCEMENT', 'sys_notice_type', 'warning', 'N', '系统级公告'
  UNION ALL SELECT 1, 3, '新闻动态', 'NEWS', 'sys_notice_type', 'success', 'N', '新闻动态'
  UNION ALL SELECT 1, 1, '草稿', '0', 'sys_notice_status', 'default', 'Y', '草稿状态'
  UNION ALL SELECT 1, 2, '已发布', '1', 'sys_notice_status', 'success', 'N', '已发布状态'
  UNION ALL SELECT 1, 3, '已撤回', '2', 'sys_notice_status', 'warning', 'N', '已撤回状态'
  UNION ALL SELECT 1, 1, '系统消息', 'SYSTEM', 'sys_message_type', 'info', 'Y', '系统消息'
  UNION ALL SELECT 1, 2, '短信', 'SMS', 'sys_message_type', 'warning', 'N', '短信消息'
  UNION ALL SELECT 1, 3, '邮件', 'EMAIL', 'sys_message_type', 'success', 'N', '邮件消息'
  UNION ALL SELECT 1, 4, '自定义', 'CUSTOM', 'sys_message_type', 'default', 'N', '自定义消息'
  UNION ALL SELECT 1, 1, '站内信', 'WEB', 'sys_message_channel', 'default', 'Y', '站内信渠道'
  UNION ALL SELECT 1, 2, '短信', 'SMS', 'sys_message_channel', 'warning', 'N', '短信渠道'
  UNION ALL SELECT 1, 3, '邮件', 'EMAIL', 'sys_message_channel', 'success', 'N', '邮件渠道'
  UNION ALL SELECT 1, 4, '推送', 'PUSH', 'sys_message_channel', 'info', 'N', '推送渠道'
  UNION ALL SELECT 1, 1, '指定人员', 'USERS', 'sys_message_send_scope', 'info', 'Y', '发送给指定人员'
  UNION ALL SELECT 1, 2, '指定组织', 'ORG', 'sys_message_send_scope', 'warning', 'N', '发送给指定组织'
  UNION ALL SELECT 1, 3, '全员', 'ALL', 'sys_message_send_scope', 'success', 'N', '发送给全员'
  UNION ALL SELECT 1, 1, '草稿', '0', 'sys_message_send_status', 'default', 'Y', '草稿'
  UNION ALL SELECT 1, 2, '已发送', '1', 'sys_message_send_status', 'success', 'N', '已发送'
  UNION ALL SELECT 1, 3, '发送失败', '2', 'sys_message_send_status', 'error', 'N', '发送失败'
  UNION ALL SELECT 1, 1, '未读', '0', 'sys_message_read_status', 'error', 'Y', '未读'
  UNION ALL SELECT 1, 2, '已读', '1', 'sys_message_read_status', 'success', 'N', '已读'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);
