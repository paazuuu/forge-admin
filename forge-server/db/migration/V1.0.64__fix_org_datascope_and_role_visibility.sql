-- Fix system organization data-scope configuration.
-- SELF scope for organization queries must map the current user to sys_user_org,
-- otherwise the data-scope interceptor may generate invalid SQL or skip filtering.

UPDATE sys_data_scope_config
SET user_id_column = '<sql>id IN (SELECT org_id FROM sys_user_org WHERE user_id = #{userId} AND tenant_id = #{tenantId})</sql>',
    table_alias = '',
    update_time = NOW()
WHERE mapper_method IN (
    'com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgPage',
    'com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgList'
);

UPDATE sys_data_scope_config
SET user_id_column = '<sql>o.id IN (SELECT org_id FROM sys_user_org WHERE user_id = #{userId} AND tenant_id = #{tenantId})</sql>',
    table_alias = 'o',
    update_time = NOW()
WHERE mapper_method = 'com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgLazyTree';

INSERT INTO sys_data_scope_config (
    tenant_id,
    resource_code,
    resource_name,
    mapper_method,
    table_alias,
    user_id_column,
    org_id_column,
    tenant_id_column,
    region_code_column,
    user_region_column,
    user_table_alias,
    enabled,
    remark,
    create_by,
    create_time,
    update_by,
    update_time,
    create_dept
)
SELECT
    1,
    'system:org:children',
    '组织子节点查询',
    'com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgChildrenByParentId',
    'o',
    '<sql>o.id IN (SELECT org_id FROM sys_user_org WHERE user_id = #{userId} AND tenant_id = #{tenantId})</sql>',
    'id',
    'tenant_id',
    'region_code',
    NULL,
    NULL,
    1,
    '组织懒加载子节点数据权限配置',
    NULL,
    NOW(),
    NULL,
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_data_scope_config
    WHERE mapper_method = 'com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgChildrenByParentId'
);
