-- P1 数据模块业务枚举字典。
-- 数据连接、业务定义、数据集分类、维度、数据集等页面统一从字典读取选项和标签。

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, seed.dict_status, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, '数据库类型' dict_name, 'data_db_type' dict_type, 1 dict_status, '数据连接支持的数据库类型' remark
  UNION ALL SELECT 1, '维度来源类型', 'data_dimension_source_type', 1, '维度值录入方式'
  UNION ALL SELECT 1, '数据集类型', 'data_dataset_type', 1, '数据集建模方式'
  UNION ALL SELECT 1, '数据集发布状态', 'data_dataset_publish_status', 1, '数据集生命周期发布状态'
  UNION ALL SELECT 1, '数据集访问模式', 'data_dataset_access_mode', 1, '数据集访问权限范围'
  UNION ALL SELECT 1, '授权主体类型', 'data_acl_subject_type', 1, '数据集授权主体类型'
  UNION ALL SELECT 1, '访问级别', 'data_acl_access_level', 1, '数据集授权访问级别'
  UNION ALL SELECT 1, '字段角色', 'data_field_role', 1, '数据集字段角色类型'
  UNION ALL SELECT 1, '敏感级别', 'data_field_sensitive_level', 1, '数据集字段敏感级别'
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
  SELECT 1 tenant_id, 1 dict_sort, 'MySQL' dict_label, 'MYSQL' dict_value, 'data_db_type' dict_type, 'success' list_class, 'Y' is_default, 'MySQL数据库' remark
  UNION ALL SELECT 1, 2, 'Oracle', 'ORACLE', 'data_db_type', 'default', 'N', 'Oracle数据库'
  UNION ALL SELECT 1, 3, 'PostgreSQL', 'POSTGRESQL', 'data_db_type', 'info', 'N', 'PostgreSQL数据库'
  UNION ALL SELECT 1, 4, 'SQLServer', 'SQLSERVER', 'data_db_type', 'warning', 'N', 'SQLServer数据库'
  UNION ALL SELECT 1, 1, '手动维护', 'MANUAL' dict_value, 'data_dimension_source_type' dict_type, 'info' list_class, 'Y' is_default, '手动维护维度值' remark
  UNION ALL SELECT 1, 2, 'SQL同步', 'SQL', 'data_dimension_source_type', 'warning', 'N', '从数据源同步维度值'
  UNION ALL SELECT 1, 1, '单表数据集', 'TABLE' dict_value, 'data_dataset_type' dict_type, 'info' list_class, 'Y' is_default, '单表数据集' remark
  UNION ALL SELECT 1, 2, 'SQL数据集', 'SQL', 'data_dataset_type', 'warning', 'N', 'SQL数据集'
  UNION ALL SELECT 1, 1, '未发布', '0' dict_value, 'data_dataset_publish_status' dict_type, 'default' list_class, 'Y' is_default, '草稿状态' remark
  UNION ALL SELECT 1, 2, '已发布', '1', 'data_dataset_publish_status', 'success', 'N', '已发布状态'
  UNION ALL SELECT 1, 3, '已下架', '2', 'data_dataset_publish_status', 'warning', 'N', '已下架状态'
  UNION ALL SELECT 1, 1, '公开', 'PUBLIC' dict_value, 'data_dataset_access_mode' dict_type, 'success' list_class, 'Y' is_default, '公开访问' remark
  UNION ALL SELECT 1, 2, '私有', 'PRIVATE', 'data_dataset_access_mode', 'warning', 'N', '私有访问'
  UNION ALL SELECT 1, 1, '角色', 'ROLE' dict_value, 'data_acl_subject_type' dict_type, 'info' list_class, 'Y' is_default, '角色授权' remark
  UNION ALL SELECT 1, 2, '用户', 'USER', 'data_acl_subject_type', 'success', 'N', '用户授权'
  UNION ALL SELECT 1, 3, '组织', 'ORG', 'data_acl_subject_type', 'warning', 'N', '组织授权'
  UNION ALL SELECT 1, 1, '查看', 'VIEW' dict_value, 'data_acl_access_level' dict_type, 'info' list_class, 'Y' is_default, '查看权限' remark
  UNION ALL SELECT 1, 2, '查询', 'QUERY', 'data_acl_access_level', 'success', 'N', '查询权限'
  UNION ALL SELECT 1, 3, '管理', 'MANAGE', 'data_acl_access_level', 'warning', 'N', '管理权限'
  UNION ALL SELECT 1, 1, '维度', 'DIMENSION' dict_value, 'data_field_role' dict_type, 'info' list_class, 'Y' is_default, '维度字段' remark
  UNION ALL SELECT 1, 2, '指标', 'MEASURE', 'data_field_role', 'success', 'N', '指标字段'
  UNION ALL SELECT 1, 1, '不脱敏', 'NONE' dict_value, 'data_field_sensitive_level' dict_type, 'default' list_class, 'Y' is_default, '不脱敏' remark
  UNION ALL SELECT 1, 2, '脱敏展示', 'MASK', 'data_field_sensitive_level', 'warning', 'N', '脱敏展示'
  UNION ALL SELECT 1, 3, '隐藏字段', 'HIDDEN', 'data_field_sensitive_level', 'error', 'N', '隐藏字段'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);