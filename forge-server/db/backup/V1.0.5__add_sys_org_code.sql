ALTER TABLE sys_org ADD org_code varchar(100) NULL COMMENT '组织编码';
ALTER TABLE sys_org CHANGE org_code org_code varchar(100) NULL COMMENT '组织编码' AFTER tenant_id;
