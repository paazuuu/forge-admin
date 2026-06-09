-- 客户端验证码覆盖配置。为空时继承系统登录配置。

SET @column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_client'
    AND COLUMN_NAME = 'captcha_type'
);

SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE `sys_client` ADD COLUMN `captcha_type` varchar(32) DEFAULT NULL COMMENT ''验证码覆盖类型：graphical/slider/sms，空表示继承全局登录配置'' AFTER `auth_types`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
