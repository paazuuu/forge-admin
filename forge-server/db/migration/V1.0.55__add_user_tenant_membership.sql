CREATE TABLE IF NOT EXISTS sys_user_tenant
(
    id           bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id    bigint      NOT NULL COMMENT '租户ID',
    user_id      bigint      NOT NULL COMMENT '用户ID',
    member_type  tinyint     NOT NULL DEFAULT 2 COMMENT '成员类型（1-租户管理员，2-普通成员）',
    is_default   tinyint     NOT NULL DEFAULT 0 COMMENT '是否默认租户（0-否，1-是）',
    status       tinyint     NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-正常）',
    create_by    bigint               DEFAULT NULL COMMENT '创建者',
    create_time  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept  bigint               DEFAULT NULL COMMENT '创建部门',
    update_by    bigint               DEFAULT NULL COMMENT '更新者',
    update_time  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_tenant (tenant_id, user_id),
    KEY idx_user_tenant_user (user_id, status),
    KEY idx_user_tenant_default (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-租户成员关系表';

UPDATE sys_user
SET tenant_id = 1
WHERE tenant_id IS NULL
   OR tenant_id = 0;

INSERT INTO sys_user_tenant (tenant_id, user_id, member_type, is_default, status, create_by, create_time, update_by, update_time)
SELECT u.tenant_id,
       u.id,
       CASE WHEN u.user_type = 1 THEN 1 ELSE 2 END,
       1,
       1,
       u.create_by,
       COALESCE(u.create_time, NOW()),
       u.update_by,
       COALESCE(u.update_time, NOW())
FROM sys_user u
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_tenant sut
    WHERE sut.tenant_id = u.tenant_id
      AND sut.user_id = u.id
);
