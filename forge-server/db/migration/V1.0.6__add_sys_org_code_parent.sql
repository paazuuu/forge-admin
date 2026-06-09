ALTER TABLE sys_org ADD org_parent_code varchar(100) NULL COMMENT '父级组织编码';
ALTER TABLE sys_org CHANGE org_parent_code org_parent_code varchar(100) NULL COMMENT '父级组织编码' AFTER org_code;
