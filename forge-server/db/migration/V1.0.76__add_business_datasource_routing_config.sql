INSERT INTO sys_config (tenant_id, config_name, config_key, config_value, config_type, config_desc, sort,
                        create_by, create_time, update_by, update_time, create_dept)
SELECT 1,
       '业务租户数据源路由开关',
       'business.datasource.tenant-routing-enabled',
       'false',
       'Y',
       '控制 forge-business 是否按当前租户默认业务数据源切换 MyBatis-Plus ORM 数据源',
       40,
       1,
       NOW(),
       1,
       NOW(),
       1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE tenant_id = 1 AND config_key = 'business.datasource.tenant-routing-enabled'
);
