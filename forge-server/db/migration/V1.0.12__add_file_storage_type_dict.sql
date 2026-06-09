-- 文件存储类型字典。
-- 前端存储配置、文件列表等页面通过 sys_file_storage_type 读取展示名称和标签样式。

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT 1, '文件存储类型', 'sys_file_storage_type', 1, '文件存储配置的存储类型选项', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type
  WHERE tenant_id = 1
    AND dict_type = 'sys_file_storage_type'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
SELECT 1, 1, '本地存储', 'local', 'sys_file_storage_type', NULL, 'default', 'Y', 1, '本地磁盘文件存储', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data
  WHERE tenant_id = 1 AND dict_type = 'sys_file_storage_type' AND dict_value = 'local'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
SELECT 1, 2, 'RustFS存储', 'rustfs', 'sys_file_storage_type', NULL, 'success', 'N', 1, 'RustFS 对象存储', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data
  WHERE tenant_id = 1 AND dict_type = 'sys_file_storage_type' AND dict_value = 'rustfs'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
SELECT 1, 3, 'MinIO', 'minio', 'sys_file_storage_type', NULL, 'info', 'N', 1, 'MinIO 对象存储', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data
  WHERE tenant_id = 1 AND dict_type = 'sys_file_storage_type' AND dict_value = 'minio'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
SELECT 1, 4, '阿里云OSS', 'aliyun', 'sys_file_storage_type', NULL, 'warning', 'N', 1, '阿里云 OSS 对象存储', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data
  WHERE tenant_id = 1 AND dict_type = 'sys_file_storage_type' AND dict_value = 'aliyun'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
SELECT 1, 5, '腾讯云COS', 'tencent', 'sys_file_storage_type', NULL, 'success', 'N', 1, '腾讯云 COS 对象存储', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data
  WHERE tenant_id = 1 AND dict_type = 'sys_file_storage_type' AND dict_value = 'tencent'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
SELECT 1, 6, '七牛云', 'qiniu', 'sys_file_storage_type', NULL, 'error', 'N', 1, '七牛云对象存储', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data
  WHERE tenant_id = 1 AND dict_type = 'sys_file_storage_type' AND dict_value = 'qiniu'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
SELECT 1, 7, 'AWS S3', 's3', 'sys_file_storage_type', NULL, 'primary', 'N', 1, 'AWS S3 对象存储', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data
  WHERE tenant_id = 1 AND dict_type = 'sys_file_storage_type' AND dict_value = 's3'
);
