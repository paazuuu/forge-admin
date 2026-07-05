-- P2 系统管理模块字典类型
-- 包含：登录日志、操作日志、定时任务、API配置、菜单管理、参数配置、文件管理等页面所需字典

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, seed.dict_status, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, '登录类型' dict_name, 'sys_login_type' dict_type, 1 dict_status, '登录行为类型' remark
  UNION ALL SELECT 1, '客户端类型', 'sys_client_type', 1, '登录客户端类型'
  UNION ALL SELECT 1, '通用成功失败', 'sys_common_status', 1, '通用成功/失败状态'
  UNION ALL SELECT 1, '操作类型', 'sys_operation_type', 1, '操作日志类型'
  UNION ALL SELECT 1, '任务状态', 'sys_job_status', 1, '定时任务运行状态'
  UNION ALL SELECT 1, '任务执行模式', 'sys_job_run_mode', 1, '定时任务执行模式'
  UNION ALL SELECT 1, '请求方法', 'sys_req_method', 1, 'HTTP请求方法'
  UNION ALL SELECT 1, '资源类型', 'sys_resource_type', 1, '菜单资源类型'
  UNION ALL SELECT 1, '显示状态', 'sys_show_hide', 1, '菜单显示/隐藏'
  UNION ALL SELECT 1, '链接打开方式', 'sys_link_open_target', 1, '链接打开目标'
  UNION ALL SELECT 1, '参数内置类型', 'sys_config_type', 1, '系统参数是否内置'
  UNION ALL SELECT 1, '文件分组类型', 'sys_file_group_type', 1, '文件分组类型'
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
  -- sys_login_type: 登录类型
  SELECT 1 tenant_id, 1 dict_sort, '登录' dict_label, 'LOGIN' dict_value, 'sys_login_type' dict_type, 'success' list_class, 'Y' is_default, '用户登录' remark
  UNION ALL SELECT 1, 2, '登出', 'LOGOUT', 'sys_login_type', 'default', 'N', '用户登出'
  UNION ALL SELECT 1, 3, '注册', 'REGISTER', 'sys_login_type', 'info', 'N', '用户注册'
  UNION ALL SELECT 1, 4, '被踢下线', 'KICKOUT', 'sys_login_type', 'error', 'N', '被踢下线'
  UNION ALL SELECT 1, 5, '被顶下线', 'REPLACED', 'sys_login_type', 'warning', 'N', '被顶下线'
  
  -- sys_client_type: 客户端类型
  UNION ALL SELECT 1, 1, '桌面端', 'pc', 'sys_client_type', 'info', 'Y', 'PC桌面端'
  UNION ALL SELECT 1, 2, '移动APP', 'app', 'sys_client_type', 'success', 'N', '移动应用'
  UNION ALL SELECT 1, 3, '网页H5', 'h5', 'sys_client_type', 'warning', 'N', 'H5网页'
  UNION ALL SELECT 1, 4, '微信', 'wechat', 'sys_client_type', 'error', 'N', '微信客户端'
  
  -- sys_common_status: 通用成功失败
  UNION ALL SELECT 1, 1, '成功', '1', 'sys_common_status', 'success', 'Y', '成功状态'
  UNION ALL SELECT 1, 2, '失败', '0', 'sys_common_status', 'error', 'N', '失败状态'
  
  -- sys_operation_type: 操作类型
  UNION ALL SELECT 1, 1, '查询', 'QUERY', 'sys_operation_type', 'info', 'Y', '查询操作'
  UNION ALL SELECT 1, 2, '新增', 'INSERT', 'sys_operation_type', 'success', 'N', '新增操作'
  UNION ALL SELECT 1, 3, '更新', 'UPDATE', 'sys_operation_type', 'warning', 'N', '更新操作'
  UNION ALL SELECT 1, 4, '删除', 'DELETE', 'sys_operation_type', 'error', 'N', '删除操作'
  UNION ALL SELECT 1, 5, '导入', 'IMPORT', 'sys_operation_type', 'info', 'N', '导入操作'
  UNION ALL SELECT 1, 6, '导出', 'EXPORT', 'sys_operation_type', 'success', 'N', '导出操作'
  UNION ALL SELECT 1, 7, '其他', 'OTHER', 'sys_operation_type', 'default', 'N', '其他操作'
  
  -- sys_job_status: 任务状态
  UNION ALL SELECT 1, 1, '停止', '0', 'sys_job_status', 'default', 'Y', '任务停止'
  UNION ALL SELECT 1, 2, '运行中', '1', 'sys_job_status', 'success', 'N', '任务运行'
  
  -- sys_job_run_mode: 任务执行模式
  UNION ALL SELECT 1, 1, 'Bean模式（本地调用）', 'BEAN', 'sys_job_run_mode', 'info', 'Y', 'Bean模式'
  UNION ALL SELECT 1, 2, 'Handler模式（扩展调用）', 'HANDLER', 'sys_job_run_mode', 'success', 'N', 'Handler模式'
  
  -- sys_req_method: 请求方法
  UNION ALL SELECT 1, 1, 'GET', 'GET', 'sys_req_method', 'info', 'Y', 'GET请求'
  UNION ALL SELECT 1, 2, 'POST', 'POST', 'sys_req_method', 'success', 'N', 'POST请求'
  UNION ALL SELECT 1, 3, 'PUT', 'PUT', 'sys_req_method', 'warning', 'N', 'PUT请求'
  UNION ALL SELECT 1, 4, 'DELETE', 'DELETE', 'sys_req_method', 'error', 'N', 'DELETE请求'
  UNION ALL SELECT 1, 5, 'ALL', 'ALL', 'sys_req_method', 'default', 'N', '所有方法'
  
  -- sys_resource_type: 资源类型
  UNION ALL SELECT 1, 1, '目录', '1', 'sys_resource_type', 'info', 'Y', '目录类型'
  UNION ALL SELECT 1, 2, '菜单', '2', 'sys_resource_type', 'success', 'N', '菜单类型'
  UNION ALL SELECT 1, 3, '按钮', '3', 'sys_resource_type', 'warning', 'N', '按钮类型'
  UNION ALL SELECT 1, 4, 'API接口', '4', 'sys_resource_type', 'error', 'N', 'API接口类型'
  
  -- sys_show_hide: 显示状态
  UNION ALL SELECT 1, 1, '显示', '1', 'sys_show_hide', 'success', 'Y', '显示状态'
  UNION ALL SELECT 1, 2, '隐藏', '0', 'sys_show_hide', 'default', 'N', '隐藏状态'
  
  -- sys_link_open_target: 链接打开方式
  UNION ALL SELECT 1, 1, '当前页', '_self', 'sys_link_open_target', 'info', 'Y', '当前页面打开'
  UNION ALL SELECT 1, 2, '新窗口', '_blank', 'sys_link_open_target', 'success', 'N', '新窗口打开'
  
  -- sys_config_type: 参数内置类型（Y/N）
  UNION ALL SELECT 1, 1, '是', 'Y', 'sys_config_type', 'success', 'N', '系统内置参数'
  UNION ALL SELECT 1, 2, '否', 'N', 'sys_config_type', 'default', 'Y', '非内置参数'
  
  -- sys_file_group_type: 文件分组类型
  UNION ALL SELECT 1, 1, '默认', 'default', 'sys_file_group_type', 'default', 'Y', '默认分组'
  UNION ALL SELECT 1, 2, '文档', 'document', 'sys_file_group_type', 'info', 'N', '文档分组'
  UNION ALL SELECT 1, 3, '图片', 'image', 'sys_file_group_type', 'success', 'N', '图片分组'
  UNION ALL SELECT 1, 4, '视频', 'video', 'sys_file_group_type', 'warning', 'N', '视频分组'
  UNION ALL SELECT 1, 5, '音频', 'audio', 'sys_file_group_type', 'info', 'N', '音频分组'
  UNION ALL SELECT 1, 6, '压缩包', 'archive', 'sys_file_group_type', 'default', 'N', '压缩包分组'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);