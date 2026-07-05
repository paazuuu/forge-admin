-- Harden sensitive data exposure, default-password handling, and upload type controls.

SET @col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'force_password_change'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE sys_user ADD COLUMN force_password_change TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否必须修改密码'' AFTER login_count',
  'SELECT ''Column force_password_change already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Existing users are forced to rotate passwords after the shared default-password leak.
UPDATE sys_user
SET force_password_change = 1,
    update_time = CURRENT_TIMESTAMP
WHERE user_status = 1
  AND force_password_change = 0;

-- Stop storing a reusable default password in system configuration.
UPDATE sys_config
SET config_value = '',
    config_type = 'N',
    config_desc = CASE
      WHEN config_desc LIKE '%已因安全加固停用%' THEN config_desc
      ELSE CONCAT(
        COALESCE(NULLIF(config_desc, ''), '用户初始密码配置'),
        '；已因安全加固停用，新增/重置用户必须首次登录修改密码'
      )
    END,
    update_time = CURRENT_TIMESTAMP
WHERE config_key = 'sys.user.initPassword';

-- Ensure upload storage configs have a safe whitelist and do not advertise active-content types.
UPDATE sys_file_storage_config
SET allowed_types = 'jpg,jpeg,png,gif,webp,pdf,doc,docx,xls,xlsx,txt,csv,zip,rar,mp4,mp3',
    update_time = CURRENT_TIMESTAMP
WHERE allowed_types IS NULL
   OR TRIM(allowed_types) = ''
   OR LOWER(CONCAT(',', REPLACE(allowed_types, ' ', ''), ',')) REGEXP ',(svg|html|htm|js|mjs|ts|vue|jsp|jspx|php|asp|aspx|sh|bash|bat|cmd|ps1|exe|dll|jar|war|ear|sql|md),';
