CREATE TABLE IF NOT EXISTS `ai_crud_export_task`
(
    `id`             bigint       NOT NULL COMMENT '任务ID',
    `tenant_id`      bigint       NOT NULL DEFAULT 1 COMMENT '租户编号',
    `config_key`     varchar(64)  NOT NULL COMMENT 'CRUD配置键',
    `export_name`    varchar(128)          DEFAULT NULL COMMENT '导出名称',
    `file_name`      varchar(255)          DEFAULT NULL COMMENT '导出文件名',
    `file_id`        varchar(64)           DEFAULT NULL COMMENT '文件中心ID',
    `file_size`      bigint                DEFAULT NULL COMMENT '文件大小(字节)',
    `status`         varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '任务状态:PENDING/RUNNING/SUCCESS/FAILED',
    `total_count`    bigint       NOT NULL DEFAULT 0 COMMENT '总数据量',
    `exported_count` bigint       NOT NULL DEFAULT 0 COMMENT '已导出数据量',
    `progress`       int          NOT NULL DEFAULT 0 COMMENT '导出进度百分比',
    `query_params`   json                  DEFAULT NULL COMMENT '导出查询参数快照',
    `error_message`  varchar(1000)         DEFAULT NULL COMMENT '失败原因',
    `finish_time`    datetime              DEFAULT NULL COMMENT '完成时间',
    `expire_time`    datetime              DEFAULT NULL COMMENT '文件过期时间',
    `create_by`      bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_dept`    bigint                DEFAULT NULL COMMENT '创建组织ID',
    `update_by`      bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_crud_export_task_user` (`tenant_id`, `create_by`, `config_key`, `create_time`),
    KEY `idx_ai_crud_export_task_status` (`tenant_id`, `status`, `expire_time`),
    KEY `idx_ai_crud_export_task_file` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用CRUD异步导出任务表';

INSERT INTO sys_config (tenant_id, config_name, config_key, config_value, config_type, config_desc, sort,
                        create_by, create_time, update_by, update_time, create_dept)
SELECT 1,
       '通用导出异步阈值',
       'sys.export.async.threshold',
       '5000',
       'Y',
       '通用CRUD导出数据量超过该阈值时自动转为异步导出',
       30,
       1,
       NOW(),
       1,
       NOW(),
       1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE tenant_id = 1 AND config_key = 'sys.export.async.threshold'
);

INSERT INTO sys_config (tenant_id, config_name, config_key, config_value, config_type, config_desc, sort,
                        create_by, create_time, update_by, update_time, create_dept)
SELECT 1,
       '通用导出批量大小',
       'sys.export.batch.size',
       '1000',
       'Y',
       '通用CRUD异步导出每批分页查询和写入Excel的行数',
       31,
       1,
       NOW(),
       1,
       NOW(),
       1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE tenant_id = 1 AND config_key = 'sys.export.batch.size'
);

INSERT INTO sys_config (tenant_id, config_name, config_key, config_value, config_type, config_desc, sort,
                        create_by, create_time, update_by, update_time, create_dept)
SELECT 1,
       '通用导出文件保留小时',
       'sys.export.file.keepHours',
       '24',
       'Y',
       '通用CRUD异步导出文件在任务中的默认保留小时数',
       32,
       1,
       NOW(),
       1,
       NOW(),
       1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE tenant_id = 1 AND config_key = 'sys.export.file.keepHours'
);
