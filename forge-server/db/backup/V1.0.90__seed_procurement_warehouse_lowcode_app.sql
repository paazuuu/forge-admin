-- Procurement/Warehouse low-code app seed.
-- This migration seeds business assets and runtime tables only. Platform logic stays generic.

CREATE TABLE IF NOT EXISTS `pw_material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `material_code` varchar(64) NOT NULL COMMENT '物料编号',
  `material_name` varchar(128) NOT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `category` varchar(64) DEFAULT NULL COMMENT '物料分类',
  `reference_price_cent` bigint NOT NULL DEFAULT 0 COMMENT '参考单价，单位分',
  `warning_quantity` decimal(18,3) NOT NULL DEFAULT 0 COMMENT '预警数量',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_material_code` (`tenant_id`, `material_code`),
  KEY `idx_pw_material_name` (`tenant_id`, `material_name`),
  KEY `idx_pw_material_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购仓储-物料';

CREATE TABLE IF NOT EXISTS `pw_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `supplier_code` varchar(64) NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_supplier_code` (`tenant_id`, `supplier_code`),
  KEY `idx_pw_supplier_name` (`tenant_id`, `supplier_name`),
  KEY `idx_pw_supplier_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购仓储-供应商';

CREATE TABLE IF NOT EXISTS `pw_supplier_material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_name` varchar(128) DEFAULT NULL COMMENT '供应商名称',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `material_code` varchar(64) DEFAULT NULL COMMENT '物料编号',
  `material_name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `quote_price_cent` bigint NOT NULL DEFAULT 0 COMMENT '当前报价，单位分',
  `last_price_cent` bigint NOT NULL DEFAULT 0 COMMENT '上次报价，单位分',
  `effective_date` date DEFAULT NULL COMMENT '报价有效期',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_supplier_material` (`tenant_id`, `supplier_id`, `material_id`),
  KEY `idx_pw_supplier_material_supplier` (`tenant_id`, `supplier_id`),
  KEY `idx_pw_supplier_material_material` (`tenant_id`, `material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购仓储-供应商物料报价';

CREATE TABLE IF NOT EXISTS `pw_warehouse` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `warehouse_code` varchar(64) NOT NULL COMMENT '仓库编号',
  `warehouse_name` varchar(128) NOT NULL COMMENT '仓库名称',
  `warehouse_type` varchar(32) NOT NULL DEFAULT 'CENTER' COMMENT '仓库类型',
  `project_name` varchar(128) DEFAULT NULL COMMENT '关联项目',
  `location` varchar(255) DEFAULT NULL COMMENT '位置',
  `related_contract_no` varchar(64) DEFAULT NULL COMMENT '关联合同',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_warehouse_code` (`tenant_id`, `warehouse_code`),
  KEY `idx_pw_warehouse_type` (`tenant_id`, `warehouse_type`),
  KEY `idx_pw_warehouse_project` (`tenant_id`, `project_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购仓储-仓库';

CREATE TABLE IF NOT EXISTS `pw_purchase_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `purchase_no` varchar(64) NOT NULL COMMENT '采购单号',
  `project_name` varchar(128) NOT NULL COMMENT '项目名称',
  `delivery_type` varchar(32) NOT NULL DEFAULT 'WAREHOUSE' COMMENT '送达类型',
  `warehouse_id` bigint DEFAULT NULL COMMENT '目标仓库ID',
  `warehouse_name` varchar(128) DEFAULT NULL COMMENT '目标仓库',
  `purchaser_name` varchar(64) DEFAULT NULL COMMENT '采购人',
  `purchase_date` date DEFAULT NULL COMMENT '采购日期',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `supplier_name` varchar(128) DEFAULT NULL COMMENT '供应商',
  `supplier_contact` varchar(64) DEFAULT NULL COMMENT '供应商联系人',
  `supplier_phone` varchar(32) DEFAULT NULL COMMENT '供应商联系电话',
  `purchase_amount_cent` bigint NOT NULL DEFAULT 0 COMMENT '采购金额，单位分',
  `order_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
  `attachment_ids` varchar(500) DEFAULT NULL COMMENT '附件ID集合',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_purchase_no` (`tenant_id`, `purchase_no`),
  KEY `idx_pw_purchase_project` (`tenant_id`, `project_name`),
  KEY `idx_pw_purchase_status` (`tenant_id`, `order_status`),
  KEY `idx_pw_purchase_warehouse` (`tenant_id`, `warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购仓储-采购单';

CREATE TABLE IF NOT EXISTS `pw_purchase_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `purchase_id` bigint NOT NULL COMMENT '采购单ID',
  `material_id` bigint DEFAULT NULL COMMENT '物料ID',
  `material_code` varchar(64) DEFAULT NULL COMMENT '物料编号',
  `material_name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `quantity` decimal(18,3) NOT NULL DEFAULT 0 COMMENT '数量',
  `cost_price_cent` bigint NOT NULL DEFAULT 0 COMMENT '成本单价，单位分',
  `deal_price_cent` bigint NOT NULL DEFAULT 0 COMMENT '成交单价，单位分',
  `amount_cent` bigint NOT NULL DEFAULT 0 COMMENT '金额，单位分',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_pw_purchase_item_order` (`tenant_id`, `purchase_id`),
  KEY `idx_pw_purchase_item_material` (`tenant_id`, `material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购仓储-采购明细';

CREATE TABLE IF NOT EXISTS `pw_outbound_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `outbound_no` varchar(64) NOT NULL COMMENT '出库单号',
  `warehouse_id` bigint NOT NULL COMMENT '所属仓库ID',
  `warehouse_name` varchar(128) DEFAULT NULL COMMENT '所属仓库',
  `applicant_name` varchar(64) DEFAULT NULL COMMENT '申请人',
  `outbound_date` date DEFAULT NULL COMMENT '出库日期',
  `outbound_reason` varchar(255) DEFAULT NULL COMMENT '出库原因',
  `order_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_outbound_no` (`tenant_id`, `outbound_no`),
  KEY `idx_pw_outbound_warehouse` (`tenant_id`, `warehouse_id`),
  KEY `idx_pw_outbound_status` (`tenant_id`, `order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购仓储-出库单';

CREATE TABLE IF NOT EXISTS `pw_outbound_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `outbound_id` bigint NOT NULL COMMENT '出库单ID',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `material_id` bigint DEFAULT NULL COMMENT '物料ID',
  `material_code` varchar(64) DEFAULT NULL COMMENT '物料编号',
  `material_name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `stock_quantity` decimal(18,3) NOT NULL DEFAULT 0 COMMENT '当前库存',
  `outbound_quantity` decimal(18,3) NOT NULL DEFAULT 0 COMMENT '出库数量',
  `lock_code` varchar(128) DEFAULT NULL COMMENT '数量锁定编码',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_pw_outbound_item_order` (`tenant_id`, `outbound_id`),
  KEY `idx_pw_outbound_item_material` (`tenant_id`, `material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购仓储-出库明细';

CREATE TABLE IF NOT EXISTS `pw_transfer_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `transfer_no` varchar(64) NOT NULL COMMENT '调拨单号',
  `from_warehouse_id` bigint NOT NULL COMMENT '调出仓库ID',
  `from_warehouse_name` varchar(128) DEFAULT NULL COMMENT '调出仓库',
  `to_warehouse_id` bigint NOT NULL COMMENT '调入仓库ID',
  `to_warehouse_name` varchar(128) DEFAULT NULL COMMENT '调入仓库',
  `transfer_person_name` varchar(64) DEFAULT NULL COMMENT '调拨人',
  `transfer_date` date DEFAULT NULL COMMENT '调拨日期',
  `transfer_reason` varchar(255) DEFAULT NULL COMMENT '调拨原因',
  `order_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pw_transfer_no` (`tenant_id`, `transfer_no`),
  KEY `idx_pw_transfer_from` (`tenant_id`, `from_warehouse_id`),
  KEY `idx_pw_transfer_to` (`tenant_id`, `to_warehouse_id`),
  KEY `idx_pw_transfer_status` (`tenant_id`, `order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购仓储-调拨单';

CREATE TABLE IF NOT EXISTS `pw_transfer_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `transfer_id` bigint NOT NULL COMMENT '调拨单ID',
  `material_id` bigint DEFAULT NULL COMMENT '物料ID',
  `material_code` varchar(64) DEFAULT NULL COMMENT '物料编号',
  `material_name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `spec_model` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `current_stock_quantity` decimal(18,3) NOT NULL DEFAULT 0 COMMENT '当前库存',
  `transfer_quantity` decimal(18,3) NOT NULL DEFAULT 0 COMMENT '调拨数量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_pw_transfer_item_order` (`tenant_id`, `transfer_id`),
  KEY `idx_pw_transfer_item_material` (`tenant_id`, `material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购仓储-调拨明细';

INSERT INTO ai_lowcode_domain (id, tenant_id, parent_id, domain_code, domain_name, domain_desc, icon, sort, status,
                               menu_parent_id, table_prefix, config_key_prefix, default_app_type,
                               default_layout_type, default_table_mode, domain_schema, create_by, create_time,
                               update_by, update_time, create_dept)
SELECT 1900000000000000901, 1, 0, 'PROCUREMENT_WAREHOUSE', '采购仓储', '采购、仓库、供应商、物料和库存交易闭环低代码业务域',
       'ionicons5:CubeOutline', 30, 'ENABLED', NULL, 'pw_', 'pw_', 'SINGLE',
       'simple-crud', 'EXISTING',
       JSON_OBJECT(
         'aiContext', JSON_OBJECT(
           'description', '采购仓储低代码业务域',
           'terms', JSON_ARRAY('采购单', '仓库', '供应商', '物料', '出库', '调拨', '库存台账'),
           'commonObjects', JSON_ARRAY('PW_MATERIAL', 'PW_SUPPLIER', 'PW_WAREHOUSE', 'PW_PURCHASE_ORDER', 'PW_OUTBOUND_ORDER', 'PW_TRANSFER_ORDER'),
           'fieldNamingPreference', 'lowerCamel'
         ),
         'naming', JSON_OBJECT('tablePrefix', 'pw_', 'configKeyPrefix', 'pw_', 'objectCodeStyle', 'upper_snake'),
         'defaults', JSON_OBJECT('appType', 'SINGLE', 'layoutType', 'simple-crud', 'tableMode', 'EXISTING')
       ),
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM ai_lowcode_domain
  WHERE tenant_id = 1 AND domain_code = 'PROCUREMENT_WAREHOUSE'
);

SET @pw_domain_id := (
  SELECT id FROM ai_lowcode_domain
  WHERE tenant_id = 1 AND domain_code = 'PROCUREMENT_WAREHOUSE'
  LIMIT 1
);

INSERT INTO ai_business_suite (id, tenant_id, parent_id, suite_code, suite_name, icon, description, status,
                               sort_order, options, create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000000901, 1, NULL, 'PROCUREMENT_WAREHOUSE', '采购仓储', 'ionicons5:CubeOutline',
       '完全低代码方式搭建的采购、仓储、供应商和物料管理业务套件', 1, 30,
       JSON_OBJECT('lowcodeApp', true, 'domainCode', 'PROCUREMENT_WAREHOUSE', 'scenario', 'procurement_warehouse'),
       1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_suite
  WHERE tenant_id = 1 AND suite_code = 'PROCUREMENT_WAREHOUSE'
);

INSERT INTO ai_lowcode_model (id, tenant_id, domain_id, domain_code, model_code, model_name, model_desc, status,
                              tenant_enabled, master_data, runtime_table_name, table_mode, model_schema,
                              create_by, create_time, create_dept, update_by, update_time)
SELECT 1920000000000000901, 1, @pw_domain_id, 'PROCUREMENT_WAREHOUSE', 'pw_material', '物料', '物料编号、名称、规格、单位和预警数量',
       'ENABLED', 1, 1, 'pw_material', 'EXISTING',
       JSON_OBJECT('schemaVersion', 2, 'domain', JSON_OBJECT('id', CAST(@pw_domain_id AS CHAR), 'code', 'PROCUREMENT_WAREHOUSE', 'name', '采购仓储'),
         'object', JSON_OBJECT('code', 'PW_MATERIAL', 'name', '物料'), 'appType', 'SINGLE', 'tableMode', 'EXISTING',
         'tableName', 'pw_material', 'businessName', '物料',
         'fields', JSON_ARRAY(
           JSON_OBJECT('field', 'materialCode', 'columnName', 'material_code', 'label', '物料编号', 'dataType', 'varchar', 'length', 64, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 140),
           JSON_OBJECT('field', 'materialName', 'columnName', 'material_name', 'label', '物料名称', 'dataType', 'varchar', 'length', 128, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'specModel', 'columnName', 'spec_model', 'label', '规格型号', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 160),
           JSON_OBJECT('field', 'unit', 'columnName', 'unit', 'label', '单位', 'dataType', 'varchar', 'length', 32, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'width', 80),
           JSON_OBJECT('field', 'referencePriceCent', 'columnName', 'reference_price_cent', 'label', '参考单价(分)', 'dataType', 'bigint', 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 120),
           JSON_OBJECT('field', 'warningQuantity', 'columnName', 'warning_quantity', 'label', '预警数量', 'dataType', 'decimal', 'precision', 3, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 120),
           JSON_OBJECT('field', 'status', 'columnName', 'status', 'label', '状态', 'dataType', 'varchar', 'length', 32, 'required', true, 'defaultValue', 'ENABLED', 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'select', 'queryType', 'eq', 'width', 100),
           JSON_OBJECT('field', 'remark', 'columnName', 'remark', 'label', '备注', 'dataType', 'varchar', 'length', 500, 'required', false, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'textarea', 'width', 220)
         ),
         'policies', JSON_OBJECT('dataScope', 'TENANT', 'auditEnabled', true, 'primaryKeyStrategy', 'AUTO_INCREMENT', 'primaryKeyField', 'id', 'tenantField', 'tenantId', 'tenantColumn', 'tenant_id', 'logicDeleteField', 'delFlag', 'logicDeleteColumn', 'del_flag')
       ),
       1, NOW(), 1, 1, NOW()
WHERE @pw_domain_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ai_lowcode_model WHERE tenant_id = 1 AND domain_id = @pw_domain_id AND model_code = 'pw_material');

INSERT INTO ai_lowcode_model (id, tenant_id, domain_id, domain_code, model_code, model_name, model_desc, status,
                              tenant_enabled, master_data, runtime_table_name, table_mode, model_schema,
                              create_by, create_time, create_dept, update_by, update_time)
SELECT 1920000000000000902, 1, @pw_domain_id, 'PROCUREMENT_WAREHOUSE', 'pw_supplier', '供应商', '供应商基础信息和联系人',
       'ENABLED', 1, 1, 'pw_supplier', 'EXISTING',
       JSON_OBJECT('schemaVersion', 2, 'domain', JSON_OBJECT('id', CAST(@pw_domain_id AS CHAR), 'code', 'PROCUREMENT_WAREHOUSE', 'name', '采购仓储'),
         'object', JSON_OBJECT('code', 'PW_SUPPLIER', 'name', '供应商'), 'appType', 'SINGLE', 'tableMode', 'EXISTING',
         'tableName', 'pw_supplier', 'businessName', '供应商',
         'fields', JSON_ARRAY(
           JSON_OBJECT('field', 'supplierCode', 'columnName', 'supplier_code', 'label', '供应商编号', 'dataType', 'varchar', 'length', 64, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 140),
           JSON_OBJECT('field', 'supplierName', 'columnName', 'supplier_name', 'label', '供应商名称', 'dataType', 'varchar', 'length', 128, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'contactName', 'columnName', 'contact_name', 'label', '联系人', 'dataType', 'varchar', 'length', 64, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'width', 100),
           JSON_OBJECT('field', 'contactPhone', 'columnName', 'contact_phone', 'label', '联系电话', 'dataType', 'varchar', 'length', 32, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'width', 130),
           JSON_OBJECT('field', 'status', 'columnName', 'status', 'label', '状态', 'dataType', 'varchar', 'length', 32, 'required', true, 'defaultValue', 'ENABLED', 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'select', 'queryType', 'eq', 'width', 100),
           JSON_OBJECT('field', 'remark', 'columnName', 'remark', 'label', '备注', 'dataType', 'varchar', 'length', 500, 'required', false, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'textarea', 'width', 220)
         ),
         'policies', JSON_OBJECT('dataScope', 'TENANT', 'auditEnabled', true, 'primaryKeyStrategy', 'AUTO_INCREMENT', 'primaryKeyField', 'id', 'tenantField', 'tenantId', 'tenantColumn', 'tenant_id', 'logicDeleteField', 'delFlag', 'logicDeleteColumn', 'del_flag')
       ),
       1, NOW(), 1, 1, NOW()
WHERE @pw_domain_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ai_lowcode_model WHERE tenant_id = 1 AND domain_id = @pw_domain_id AND model_code = 'pw_supplier');

INSERT INTO ai_lowcode_model (id, tenant_id, domain_id, domain_code, model_code, model_name, model_desc, status,
                              tenant_enabled, master_data, runtime_table_name, table_mode, model_schema,
                              create_by, create_time, create_dept, update_by, update_time)
SELECT 1920000000000000903, 1, @pw_domain_id, 'PROCUREMENT_WAREHOUSE', 'pw_warehouse', '仓库', '中心仓库和项目现场仓',
       'ENABLED', 1, 1, 'pw_warehouse', 'EXISTING',
       JSON_OBJECT('schemaVersion', 2, 'domain', JSON_OBJECT('id', CAST(@pw_domain_id AS CHAR), 'code', 'PROCUREMENT_WAREHOUSE', 'name', '采购仓储'),
         'object', JSON_OBJECT('code', 'PW_WAREHOUSE', 'name', '仓库'), 'appType', 'SINGLE', 'tableMode', 'EXISTING',
         'tableName', 'pw_warehouse', 'businessName', '仓库',
         'fields', JSON_ARRAY(
           JSON_OBJECT('field', 'warehouseCode', 'columnName', 'warehouse_code', 'label', '仓库编号', 'dataType', 'varchar', 'length', 64, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 140),
           JSON_OBJECT('field', 'warehouseName', 'columnName', 'warehouse_name', 'label', '仓库名称/位置', 'dataType', 'varchar', 'length', 128, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 200),
           JSON_OBJECT('field', 'warehouseType', 'columnName', 'warehouse_type', 'label', '类型', 'dataType', 'varchar', 'length', 32, 'required', true, 'defaultValue', 'CENTER', 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'select', 'queryType', 'eq', 'width', 120),
           JSON_OBJECT('field', 'projectName', 'columnName', 'project_name', 'label', '关联项目', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'location', 'columnName', 'location', 'label', '位置', 'dataType', 'varchar', 'length', 255, 'required', false, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'input', 'width', 180),
           JSON_OBJECT('field', 'relatedContractNo', 'columnName', 'related_contract_no', 'label', '关联合同', 'dataType', 'varchar', 'length', 64, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'width', 150),
           JSON_OBJECT('field', 'status', 'columnName', 'status', 'label', '状态', 'dataType', 'varchar', 'length', 32, 'required', true, 'defaultValue', 'ENABLED', 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'select', 'queryType', 'eq', 'width', 100),
           JSON_OBJECT('field', 'remark', 'columnName', 'remark', 'label', '备注', 'dataType', 'varchar', 'length', 500, 'required', false, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'textarea', 'width', 220)
         ),
         'policies', JSON_OBJECT('dataScope', 'TENANT', 'auditEnabled', true, 'primaryKeyStrategy', 'AUTO_INCREMENT', 'primaryKeyField', 'id', 'tenantField', 'tenantId', 'tenantColumn', 'tenant_id', 'logicDeleteField', 'delFlag', 'logicDeleteColumn', 'del_flag')
       ),
       1, NOW(), 1, 1, NOW()
WHERE @pw_domain_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ai_lowcode_model WHERE tenant_id = 1 AND domain_id = @pw_domain_id AND model_code = 'pw_warehouse');

INSERT INTO ai_lowcode_model (id, tenant_id, domain_id, domain_code, model_code, model_name, model_desc, status,
                              tenant_enabled, master_data, runtime_table_name, table_mode, model_schema,
                              create_by, create_time, create_dept, update_by, update_time)
SELECT model_id, 1, @pw_domain_id, 'PROCUREMENT_WAREHOUSE', model_code, model_name, model_desc,
       'ENABLED', 1, 0, table_name, 'EXISTING',
       JSON_OBJECT('schemaVersion', 2, 'domain', JSON_OBJECT('id', CAST(@pw_domain_id AS CHAR), 'code', 'PROCUREMENT_WAREHOUSE', 'name', '采购仓储'),
         'object', JSON_OBJECT('code', object_code, 'name', model_name), 'appType', 'SINGLE', 'tableMode', 'EXISTING',
         'tableName', table_name, 'businessName', model_name,
         'fields', fields_json,
         'policies', JSON_OBJECT('dataScope', 'TENANT', 'auditEnabled', true, 'primaryKeyStrategy', 'AUTO_INCREMENT', 'primaryKeyField', 'id', 'tenantField', 'tenantId', 'tenantColumn', 'tenant_id', 'logicDeleteField', 'delFlag', 'logicDeleteColumn', 'del_flag')
       ),
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1920000000000000904 AS model_id, 'pw_supplier_material' AS model_code, 'PW_SUPPLIER_MATERIAL' AS object_code, '供应商报价' AS model_name, '供应商物料报价维护' AS model_desc, 'pw_supplier_material' AS table_name,
         JSON_ARRAY(
           JSON_OBJECT('field', 'supplierId', 'columnName', 'supplier_id', 'label', '供应商ID', 'dataType', 'bigint', 'required', true, 'searchable', true, 'listVisible', false, 'formVisible', true, 'componentType', 'input-number', 'queryType', 'eq'),
           JSON_OBJECT('field', 'supplierName', 'columnName', 'supplier_name', 'label', '供应商', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'materialId', 'columnName', 'material_id', 'label', '物料ID', 'dataType', 'bigint', 'required', true, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'input-number'),
           JSON_OBJECT('field', 'materialCode', 'columnName', 'material_code', 'label', '物料编号', 'dataType', 'varchar', 'length', 64, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 140),
           JSON_OBJECT('field', 'materialName', 'columnName', 'material_name', 'label', '物料名称', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'quotePriceCent', 'columnName', 'quote_price_cent', 'label', '报价(分)', 'dataType', 'bigint', 'required', true, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 120),
           JSON_OBJECT('field', 'effectiveDate', 'columnName', 'effective_date', 'label', '有效期', 'dataType', 'date', 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'date', 'width', 120),
           JSON_OBJECT('field', 'status', 'columnName', 'status', 'label', '状态', 'dataType', 'varchar', 'length', 32, 'required', true, 'defaultValue', 'ENABLED', 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'select', 'queryType', 'eq', 'width', 100)
         ) AS fields_json
  UNION ALL
  SELECT 1920000000000000905, 'pw_purchase_order', 'PW_PURCHASE_ORDER', '采购单', '采购申请和审批入库单据', 'pw_purchase_order',
         JSON_ARRAY(
           JSON_OBJECT('field', 'purchaseNo', 'columnName', 'purchase_no', 'label', '采购单号', 'dataType', 'varchar', 'length', 64, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 160),
           JSON_OBJECT('field', 'projectName', 'columnName', 'project_name', 'label', '项目名称', 'dataType', 'varchar', 'length', 128, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 200),
           JSON_OBJECT('field', 'warehouseId', 'columnName', 'warehouse_id', 'label', '目标仓库ID', 'dataType', 'bigint', 'required', false, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'input-number'),
           JSON_OBJECT('field', 'warehouseName', 'columnName', 'warehouse_name', 'label', '目标仓库', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 160),
           JSON_OBJECT('field', 'purchaserName', 'columnName', 'purchaser_name', 'label', '采购人', 'dataType', 'varchar', 'length', 64, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'width', 100),
           JSON_OBJECT('field', 'purchaseDate', 'columnName', 'purchase_date', 'label', '采购日期', 'dataType', 'date', 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'date', 'width', 120),
           JSON_OBJECT('field', 'supplierName', 'columnName', 'supplier_name', 'label', '供应商', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'purchaseAmountCent', 'columnName', 'purchase_amount_cent', 'label', '采购金额(分)', 'dataType', 'bigint', 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 130),
           JSON_OBJECT('field', 'orderStatus', 'columnName', 'order_status', 'label', '状态', 'dataType', 'varchar', 'length', 32, 'required', true, 'defaultValue', 'DRAFT', 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'select', 'queryType', 'eq', 'width', 110),
           JSON_OBJECT('field', 'remark', 'columnName', 'remark', 'label', '备注', 'dataType', 'varchar', 'length', 500, 'required', false, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'textarea', 'width', 220)
         )
  UNION ALL
  SELECT 1920000000000000906, 'pw_purchase_order_item', 'PW_PURCHASE_ORDER_ITEM', '采购明细', '采购单物料明细', 'pw_purchase_order_item',
         JSON_ARRAY(
           JSON_OBJECT('field', 'purchaseId', 'columnName', 'purchase_id', 'label', '采购单ID', 'dataType', 'bigint', 'required', true, 'searchable', true, 'listVisible', false, 'formVisible', true, 'componentType', 'input-number', 'queryType', 'eq'),
           JSON_OBJECT('field', 'materialId', 'columnName', 'material_id', 'label', '物料ID', 'dataType', 'bigint', 'required', false, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'input-number'),
           JSON_OBJECT('field', 'materialCode', 'columnName', 'material_code', 'label', '物料编号', 'dataType', 'varchar', 'length', 64, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 140),
           JSON_OBJECT('field', 'materialName', 'columnName', 'material_name', 'label', '物料名称', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'specModel', 'columnName', 'spec_model', 'label', '规格型号', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'width', 160),
           JSON_OBJECT('field', 'unit', 'columnName', 'unit', 'label', '单位', 'dataType', 'varchar', 'length', 32, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'width', 80),
           JSON_OBJECT('field', 'quantity', 'columnName', 'quantity', 'label', '数量', 'dataType', 'decimal', 'precision', 3, 'required', true, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 100),
           JSON_OBJECT('field', 'dealPriceCent', 'columnName', 'deal_price_cent', 'label', '成交单价(分)', 'dataType', 'bigint', 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 120),
           JSON_OBJECT('field', 'amountCent', 'columnName', 'amount_cent', 'label', '金额(分)', 'dataType', 'bigint', 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 120)
         )
  UNION ALL
  SELECT 1920000000000000907, 'pw_outbound_order', 'PW_OUTBOUND_ORDER', '出库单', '仓库出库申请单据', 'pw_outbound_order',
         JSON_ARRAY(
           JSON_OBJECT('field', 'outboundNo', 'columnName', 'outbound_no', 'label', '出库单号', 'dataType', 'varchar', 'length', 64, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 160),
           JSON_OBJECT('field', 'warehouseId', 'columnName', 'warehouse_id', 'label', '仓库ID', 'dataType', 'bigint', 'required', true, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'input-number'),
           JSON_OBJECT('field', 'warehouseName', 'columnName', 'warehouse_name', 'label', '所属仓库', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 200),
           JSON_OBJECT('field', 'applicantName', 'columnName', 'applicant_name', 'label', '申请人', 'dataType', 'varchar', 'length', 64, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'width', 100),
           JSON_OBJECT('field', 'outboundDate', 'columnName', 'outbound_date', 'label', '出库日期', 'dataType', 'date', 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'date', 'width', 120),
           JSON_OBJECT('field', 'outboundReason', 'columnName', 'outbound_reason', 'label', '出库原因', 'dataType', 'varchar', 'length', 255, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'width', 180),
           JSON_OBJECT('field', 'orderStatus', 'columnName', 'order_status', 'label', '状态', 'dataType', 'varchar', 'length', 32, 'required', true, 'defaultValue', 'DRAFT', 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'select', 'queryType', 'eq', 'width', 110)
         )
  UNION ALL
  SELECT 1920000000000000908, 'pw_outbound_order_item', 'PW_OUTBOUND_ORDER_ITEM', '出库明细', '出库物料明细', 'pw_outbound_order_item',
         JSON_ARRAY(
           JSON_OBJECT('field', 'outboundId', 'columnName', 'outbound_id', 'label', '出库单ID', 'dataType', 'bigint', 'required', true, 'searchable', true, 'listVisible', false, 'formVisible', true, 'componentType', 'input-number', 'queryType', 'eq'),
           JSON_OBJECT('field', 'warehouseId', 'columnName', 'warehouse_id', 'label', '仓库ID', 'dataType', 'bigint', 'required', false, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'input-number'),
           JSON_OBJECT('field', 'materialCode', 'columnName', 'material_code', 'label', '物料编号', 'dataType', 'varchar', 'length', 64, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 140),
           JSON_OBJECT('field', 'materialName', 'columnName', 'material_name', 'label', '物料名称', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'stockQuantity', 'columnName', 'stock_quantity', 'label', '库存数量', 'dataType', 'decimal', 'precision', 3, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 100),
           JSON_OBJECT('field', 'outboundQuantity', 'columnName', 'outbound_quantity', 'label', '出库数量', 'dataType', 'decimal', 'precision', 3, 'required', true, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 100)
         )
  UNION ALL
  SELECT 1920000000000000909, 'pw_transfer_order', 'PW_TRANSFER_ORDER', '调拨单', '仓库间物料调拨单据', 'pw_transfer_order',
         JSON_ARRAY(
           JSON_OBJECT('field', 'transferNo', 'columnName', 'transfer_no', 'label', '调拨单号', 'dataType', 'varchar', 'length', 64, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 160),
           JSON_OBJECT('field', 'fromWarehouseName', 'columnName', 'from_warehouse_name', 'label', '调出仓库', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'toWarehouseName', 'columnName', 'to_warehouse_name', 'label', '调入仓库', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'transferPersonName', 'columnName', 'transfer_person_name', 'label', '调拨人', 'dataType', 'varchar', 'length', 64, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'width', 100),
           JSON_OBJECT('field', 'transferDate', 'columnName', 'transfer_date', 'label', '调拨日期', 'dataType', 'date', 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'date', 'width', 120),
           JSON_OBJECT('field', 'orderStatus', 'columnName', 'order_status', 'label', '状态', 'dataType', 'varchar', 'length', 32, 'required', true, 'defaultValue', 'DRAFT', 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'select', 'queryType', 'eq', 'width', 110),
           JSON_OBJECT('field', 'transferReason', 'columnName', 'transfer_reason', 'label', '调拨原因', 'dataType', 'varchar', 'length', 255, 'required', false, 'searchable', false, 'listVisible', false, 'formVisible', true, 'componentType', 'input', 'width', 180)
         )
  UNION ALL
  SELECT 1920000000000000910, 'pw_transfer_order_item', 'PW_TRANSFER_ORDER_ITEM', '调拨明细', '调拨物料明细', 'pw_transfer_order_item',
         JSON_ARRAY(
           JSON_OBJECT('field', 'transferId', 'columnName', 'transfer_id', 'label', '调拨单ID', 'dataType', 'bigint', 'required', true, 'searchable', true, 'listVisible', false, 'formVisible', true, 'componentType', 'input-number', 'queryType', 'eq'),
           JSON_OBJECT('field', 'materialCode', 'columnName', 'material_code', 'label', '物料编号', 'dataType', 'varchar', 'length', 64, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 140),
           JSON_OBJECT('field', 'materialName', 'columnName', 'material_name', 'label', '物料名称', 'dataType', 'varchar', 'length', 128, 'required', false, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'queryType', 'like', 'width', 180),
           JSON_OBJECT('field', 'currentStockQuantity', 'columnName', 'current_stock_quantity', 'label', '当前库存', 'dataType', 'decimal', 'precision', 3, 'required', false, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 100),
           JSON_OBJECT('field', 'transferQuantity', 'columnName', 'transfer_quantity', 'label', '调拨数量', 'dataType', 'decimal', 'precision', 3, 'required', true, 'searchable', false, 'listVisible', true, 'formVisible', true, 'componentType', 'input-number', 'width', 100)
         )
) seed_models
WHERE @pw_domain_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM ai_lowcode_model
    WHERE tenant_id = 1 AND domain_id = @pw_domain_id AND model_code = seed_models.model_code
  );

INSERT INTO ai_business_object (id, tenant_id, suite_code, object_code, object_name, object_type, model_id,
                                model_code, display_field, icon, description, status, sort_order, options,
                                design_status, config_key, last_publish_time, last_publish_version,
                                designer_options, create_by, create_time, create_dept, update_by, update_time)
SELECT object_id, 1, 'PROCUREMENT_WAREHOUSE', object_code, object_name, object_type, model_id,
       model_code, display_field, icon, description, 1, sort_order,
       JSON_OBJECT('lowcodeApp', true, 'domainCode', 'PROCUREMENT_WAREHOUSE'),
       'PUBLISHED', config_key, NOW(), 1,
       JSON_OBJECT('defaultPanel', 'form', 'documentManaged', false),
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000000911 AS object_id, 'PW_MATERIAL' AS object_code, '物料' AS object_name, 'MASTER' AS object_type, 1920000000000000901 AS model_id, 'pw_material' AS model_code, 'materialName' AS display_field, 'ionicons5:CubeOutline' AS icon, '物料基础资料' AS description, 10 AS sort_order, 'pw_material' AS config_key
  UNION ALL SELECT 1910000000000000912, 'PW_SUPPLIER', '供应商', 'MASTER', 1920000000000000902, 'pw_supplier', 'supplierName', 'ionicons5:PeopleOutline', '供应商基础资料', 20, 'pw_supplier'
  UNION ALL SELECT 1910000000000000913, 'PW_SUPPLIER_MATERIAL', '供应商报价', 'DETAIL', 1920000000000000904, 'pw_supplier_material', 'materialName', 'ionicons5:PricetagOutline', '供应商物料报价', 30, NULL
  UNION ALL SELECT 1910000000000000914, 'PW_WAREHOUSE', '仓库', 'MASTER', 1920000000000000903, 'pw_warehouse', 'warehouseName', 'ionicons5:HomeOutline', '中心仓库和项目现场仓', 40, 'pw_warehouse'
  UNION ALL SELECT 1910000000000000915, 'PW_PURCHASE_ORDER', '采购单', 'TRANSACTION', 1920000000000000905, 'pw_purchase_order', 'purchaseNo', 'ionicons5:CartOutline', '采购申请和审批入库单据', 50, 'pw_purchase_order'
  UNION ALL SELECT 1910000000000000916, 'PW_PURCHASE_ORDER_ITEM', '采购明细', 'DETAIL', 1920000000000000906, 'pw_purchase_order_item', 'materialName', 'ionicons5:ListOutline', '采购物料明细', 60, NULL
  UNION ALL SELECT 1910000000000000917, 'PW_OUTBOUND_ORDER', '出库单', 'TRANSACTION', 1920000000000000907, 'pw_outbound_order', 'outboundNo', 'ionicons5:ExitOutline', '仓库出库申请单据', 70, 'pw_outbound_order'
  UNION ALL SELECT 1910000000000000918, 'PW_OUTBOUND_ORDER_ITEM', '出库明细', 'DETAIL', 1920000000000000908, 'pw_outbound_order_item', 'materialName', 'ionicons5:ListOutline', '出库物料明细', 80, NULL
  UNION ALL SELECT 1910000000000000919, 'PW_TRANSFER_ORDER', '调拨单', 'TRANSACTION', 1920000000000000909, 'pw_transfer_order', 'transferNo', 'ionicons5:SwapHorizontalOutline', '仓库间物料调拨单据', 90, 'pw_transfer_order'
  UNION ALL SELECT 1910000000000000920, 'PW_TRANSFER_ORDER_ITEM', '调拨明细', 'DETAIL', 1920000000000000910, 'pw_transfer_order_item', 'materialName', 'ionicons5:ListOutline', '调拨物料明细', 100, NULL
) seed_objects
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_object
  WHERE tenant_id = 1 AND suite_code = 'PROCUREMENT_WAREHOUSE' AND object_code = seed_objects.object_code
);

INSERT INTO ai_crud_config (id, tenant_id, config_key, table_name, table_comment, app_name, search_schema,
                            columns_schema, edit_schema, api_config, options, mode, build_mode, status,
                            publish_status, menu_name, menu_parent_id, menu_sort, menu_resource_id,
                            layout_type, model_schema, page_schema, draft_version, published_version,
                            publish_time, publish_by, domain_id, domain_code, object_code, object_name,
                            runtime_table_name, primary_key_field, primary_key_column, primary_key_type,
                            tenant_strategy, audit_strategy, logic_delete_strategy,
                            create_by, create_time, create_dept, update_by, update_time)
SELECT config_id, 1, config_key, table_name, object_name, app_name,
       search_schema, columns_schema, edit_schema,
       JSON_OBJECT(
         'list', CONCAT('get@/ai/crud/', config_key, '/page'),
         'detail', CONCAT('get@/ai/crud/', config_key, '/:id'),
         'create', CONCAT('post@/ai/crud/', config_key),
         'update', CONCAT('put@/ai/crud/', config_key),
         'delete', CONCAT('delete@/ai/crud/', config_key, '/:id'),
         'import', CONCAT('post@/ai/crud/', config_key, '/import'),
         'export', CONCAT('post@/ai/crud/', config_key, '/export'),
         'importTemplate', CONCAT('get@/ai/crud/', config_key, '/import-template')
       ),
       JSON_OBJECT('layoutType', 'simple-crud', 'rowKey', 'id', 'modalWidth', '1040px', 'editGridCols', 2,
                   'showImport', true, 'showExport', true, 'enableDetail', true),
       'CONFIG', 'LOWCODE', '0', 'PUBLISHED', app_name, NULL, sort_order, NULL,
       'simple-crud', m.model_schema,
       JSON_OBJECT(
         'layoutType', 'simple-crud',
         'primaryModelCode', config_key,
         'zones', JSON_ARRAY(
           JSON_OBJECT('key', 'search', 'type', 'search', 'props', JSON_OBJECT()),
           JSON_OBJECT('key', 'table', 'type', 'table', 'props', JSON_OBJECT()),
           JSON_OBJECT('key', 'edit', 'type', 'form', 'props', JSON_OBJECT('editGridCols', 2)),
           JSON_OBJECT('key', 'detail', 'type', 'detail', 'props', JSON_OBJECT())
         )
       ),
       1, 1, NOW(), 1, @pw_domain_id, 'PROCUREMENT_WAREHOUSE', object_code, object_name,
       table_name, 'id', 'id', 'bigint',
       JSON_OBJECT('enabled', true, 'tenantField', 'tenantId', 'tenantColumn', 'tenant_id'),
       JSON_OBJECT('enabled', true, 'createBy', 'create_by', 'createTime', 'create_time', 'createDept', 'create_dept', 'updateBy', 'update_by', 'updateTime', 'update_time'),
       JSON_OBJECT('enabled', true, 'field', 'delFlag', 'column', 'del_flag', 'notDeletedValue', '0', 'deletedValue', '1'),
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1930000000000000901 AS config_id, 'pw_material' AS config_key, 'pw_material' AS table_name, 'PW_MATERIAL' AS object_code, '物料管理' AS app_name, '物料' AS object_name, 10 AS sort_order,
         JSON_ARRAY(JSON_OBJECT('field', 'materialCode', 'label', '物料编号', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'materialName', 'label', '物料名称', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'specModel', 'label', '规格型号', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'input', 'queryType', 'eq')) AS search_schema,
         JSON_ARRAY(JSON_OBJECT('prop', 'materialCode', 'label', '物料编号', 'width', 140), JSON_OBJECT('prop', 'materialName', 'label', '物料名称', 'width', 180), JSON_OBJECT('prop', 'specModel', 'label', '规格型号', 'width', 160), JSON_OBJECT('prop', 'unit', 'label', '单位', 'width', 80), JSON_OBJECT('prop', 'referencePriceCent', 'label', '参考单价(分)', 'width', 130), JSON_OBJECT('prop', 'warningQuantity', 'label', '预警数量', 'width', 120), JSON_OBJECT('prop', 'status', 'label', '状态', 'width', 100)) AS columns_schema,
         JSON_ARRAY(JSON_OBJECT('field', 'materialCode', 'label', '物料编号', 'type', 'input', 'required', true), JSON_OBJECT('field', 'materialName', 'label', '物料名称', 'type', 'input', 'required', true), JSON_OBJECT('field', 'specModel', 'label', '规格型号', 'type', 'input'), JSON_OBJECT('field', 'unit', 'label', '单位', 'type', 'input'), JSON_OBJECT('field', 'referencePriceCent', 'label', '参考单价(分)', 'type', 'input-number'), JSON_OBJECT('field', 'warningQuantity', 'label', '预警数量', 'type', 'input-number'), JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'input', 'defaultValue', 'ENABLED'), JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea')) AS edit_schema
  UNION ALL
  SELECT 1930000000000000902, 'pw_supplier', 'pw_supplier', 'PW_SUPPLIER', '供应商管理', '供应商', 20,
         JSON_ARRAY(JSON_OBJECT('field', 'supplierCode', 'label', '供应商编号', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'supplierName', 'label', '供应商名称', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'input', 'queryType', 'eq')),
         JSON_ARRAY(JSON_OBJECT('prop', 'supplierCode', 'label', '供应商编号', 'width', 140), JSON_OBJECT('prop', 'supplierName', 'label', '供应商名称', 'width', 200), JSON_OBJECT('prop', 'contactName', 'label', '联系人', 'width', 100), JSON_OBJECT('prop', 'contactPhone', 'label', '联系电话', 'width', 130), JSON_OBJECT('prop', 'status', 'label', '状态', 'width', 100)),
         JSON_ARRAY(JSON_OBJECT('field', 'supplierCode', 'label', '供应商编号', 'type', 'input', 'required', true), JSON_OBJECT('field', 'supplierName', 'label', '供应商名称', 'type', 'input', 'required', true), JSON_OBJECT('field', 'contactName', 'label', '联系人', 'type', 'input'), JSON_OBJECT('field', 'contactPhone', 'label', '联系电话', 'type', 'input'), JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'input', 'defaultValue', 'ENABLED'), JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea'))
  UNION ALL
  SELECT 1930000000000000903, 'pw_warehouse', 'pw_warehouse', 'PW_WAREHOUSE', '仓储管理', '仓库', 30,
         JSON_ARRAY(JSON_OBJECT('field', 'warehouseName', 'label', '仓库名称/位置', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'warehouseType', 'label', '类型', 'type', 'input', 'queryType', 'eq'), JSON_OBJECT('field', 'projectName', 'label', '关联项目', 'type', 'input', 'queryType', 'like')),
         JSON_ARRAY(JSON_OBJECT('prop', 'warehouseName', 'label', '仓库名称/位置', 'width', 220), JSON_OBJECT('prop', 'warehouseType', 'label', '类型', 'width', 120), JSON_OBJECT('prop', 'projectName', 'label', '关联项目', 'width', 180), JSON_OBJECT('prop', 'relatedContractNo', 'label', '关联合同', 'width', 150), JSON_OBJECT('prop', 'status', 'label', '状态', 'width', 100)),
         JSON_ARRAY(JSON_OBJECT('field', 'warehouseCode', 'label', '仓库编号', 'type', 'input', 'required', true), JSON_OBJECT('field', 'warehouseName', 'label', '仓库名称', 'type', 'input', 'required', true), JSON_OBJECT('field', 'warehouseType', 'label', '仓库类型', 'type', 'input', 'defaultValue', 'CENTER'), JSON_OBJECT('field', 'projectName', 'label', '关联项目', 'type', 'input'), JSON_OBJECT('field', 'location', 'label', '位置', 'type', 'input'), JSON_OBJECT('field', 'relatedContractNo', 'label', '关联合同', 'type', 'input'), JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'input', 'defaultValue', 'ENABLED'), JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea'))
  UNION ALL
  SELECT 1930000000000000904, 'pw_purchase_order', 'pw_purchase_order', 'PW_PURCHASE_ORDER', '采购管理', '采购单', 40,
         JSON_ARRAY(JSON_OBJECT('field', 'purchaseNo', 'label', '采购单号', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'projectName', 'label', '项目名称', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'supplierName', 'label', '供应商', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'orderStatus', 'label', '状态', 'type', 'input', 'queryType', 'eq')),
         JSON_ARRAY(JSON_OBJECT('prop', 'purchaseNo', 'label', '采购单号', 'width', 170), JSON_OBJECT('prop', 'projectName', 'label', '项目名称', 'width', 220), JSON_OBJECT('prop', 'supplierName', 'label', '供应商', 'width', 180), JSON_OBJECT('prop', 'purchaseAmountCent', 'label', '采购金额(分)', 'width', 130), JSON_OBJECT('prop', 'purchaserName', 'label', '采购人', 'width', 100), JSON_OBJECT('prop', 'purchaseDate', 'label', '申请日期', 'width', 120), JSON_OBJECT('prop', 'orderStatus', 'label', '状态', 'width', 110)),
         JSON_ARRAY(JSON_OBJECT('field', 'purchaseNo', 'label', '采购单号', 'type', 'input', 'required', true), JSON_OBJECT('field', 'projectName', 'label', '项目名称', 'type', 'input', 'required', true), JSON_OBJECT('field', 'warehouseId', 'label', '目标仓库ID', 'type', 'input-number'), JSON_OBJECT('field', 'warehouseName', 'label', '目标仓库', 'type', 'input'), JSON_OBJECT('field', 'purchaserName', 'label', '采购人', 'type', 'input'), JSON_OBJECT('field', 'purchaseDate', 'label', '采购日期', 'type', 'date'), JSON_OBJECT('field', 'supplierName', 'label', '供应商', 'type', 'input'), JSON_OBJECT('field', 'purchaseAmountCent', 'label', '采购金额(分)', 'type', 'input-number'), JSON_OBJECT('field', 'orderStatus', 'label', '状态', 'type', 'input', 'defaultValue', 'DRAFT'), JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea'))
  UNION ALL
  SELECT 1930000000000000905, 'pw_outbound_order', 'pw_outbound_order', 'PW_OUTBOUND_ORDER', '出库管理', '出库单', 50,
         JSON_ARRAY(JSON_OBJECT('field', 'outboundNo', 'label', '出库单号', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'warehouseName', 'label', '所属仓库', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'orderStatus', 'label', '状态', 'type', 'input', 'queryType', 'eq')),
         JSON_ARRAY(JSON_OBJECT('prop', 'outboundNo', 'label', '出库单号', 'width', 170), JSON_OBJECT('prop', 'warehouseName', 'label', '所属仓库', 'width', 220), JSON_OBJECT('prop', 'applicantName', 'label', '申请人', 'width', 100), JSON_OBJECT('prop', 'outboundDate', 'label', '申请日期', 'width', 120), JSON_OBJECT('prop', 'orderStatus', 'label', '状态', 'width', 110)),
         JSON_ARRAY(JSON_OBJECT('field', 'outboundNo', 'label', '出库单号', 'type', 'input', 'required', true), JSON_OBJECT('field', 'warehouseId', 'label', '仓库ID', 'type', 'input-number', 'required', true), JSON_OBJECT('field', 'warehouseName', 'label', '所属仓库', 'type', 'input'), JSON_OBJECT('field', 'applicantName', 'label', '申请人', 'type', 'input'), JSON_OBJECT('field', 'outboundDate', 'label', '出库日期', 'type', 'date'), JSON_OBJECT('field', 'outboundReason', 'label', '出库原因', 'type', 'input'), JSON_OBJECT('field', 'orderStatus', 'label', '状态', 'type', 'input', 'defaultValue', 'DRAFT'), JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea'))
  UNION ALL
  SELECT 1930000000000000906, 'pw_transfer_order', 'pw_transfer_order', 'PW_TRANSFER_ORDER', '调拨管理', '调拨单', 60,
         JSON_ARRAY(JSON_OBJECT('field', 'transferNo', 'label', '调拨单号', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'fromWarehouseName', 'label', '调出仓库', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'toWarehouseName', 'label', '调入仓库', 'type', 'input', 'queryType', 'like'), JSON_OBJECT('field', 'orderStatus', 'label', '状态', 'type', 'input', 'queryType', 'eq')),
         JSON_ARRAY(JSON_OBJECT('prop', 'transferNo', 'label', '调拨单号', 'width', 170), JSON_OBJECT('prop', 'fromWarehouseName', 'label', '调出仓库', 'width', 180), JSON_OBJECT('prop', 'toWarehouseName', 'label', '调入仓库', 'width', 180), JSON_OBJECT('prop', 'transferDate', 'label', '调拨日期', 'width', 120), JSON_OBJECT('prop', 'transferPersonName', 'label', '调拨人', 'width', 100), JSON_OBJECT('prop', 'orderStatus', 'label', '状态', 'width', 110)),
         JSON_ARRAY(JSON_OBJECT('field', 'transferNo', 'label', '调拨单号', 'type', 'input', 'required', true), JSON_OBJECT('field', 'fromWarehouseId', 'label', '调出仓库ID', 'type', 'input-number', 'required', true), JSON_OBJECT('field', 'fromWarehouseName', 'label', '调出仓库', 'type', 'input'), JSON_OBJECT('field', 'toWarehouseId', 'label', '调入仓库ID', 'type', 'input-number', 'required', true), JSON_OBJECT('field', 'toWarehouseName', 'label', '调入仓库', 'type', 'input'), JSON_OBJECT('field', 'transferPersonName', 'label', '调拨人', 'type', 'input'), JSON_OBJECT('field', 'transferDate', 'label', '调拨日期', 'type', 'date'), JSON_OBJECT('field', 'transferReason', 'label', '调拨原因', 'type', 'input'), JSON_OBJECT('field', 'orderStatus', 'label', '状态', 'type', 'input', 'defaultValue', 'DRAFT'), JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea'))
) cfg
JOIN ai_lowcode_model m
  ON m.tenant_id = 1 AND m.domain_id = @pw_domain_id AND m.model_code = cfg.config_key
WHERE @pw_domain_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM ai_crud_config
    WHERE tenant_id = 1 AND config_key = cfg.config_key
  );

INSERT INTO ai_crud_config (id, tenant_id, config_key, table_name, table_comment, app_name, search_schema,
                            columns_schema, edit_schema, api_config, options, mode, build_mode, status,
                            publish_status, menu_name, menu_parent_id, menu_sort, menu_resource_id,
                            layout_type, model_schema, page_schema, draft_version, published_version,
                            publish_time, publish_by, domain_id, domain_code, object_code, object_name,
                            runtime_table_name, primary_key_field, primary_key_column, primary_key_type,
                            tenant_strategy, audit_strategy, logic_delete_strategy,
                            create_by, create_time, create_dept, update_by, update_time)
SELECT 1930000000000000907, 1, 'pw_supplier_material', 'pw_supplier_material', '供应商报价', '供应商报价',
       JSON_ARRAY(
         JSON_OBJECT('field', 'supplierId', 'label', '供应商ID', 'type', 'number', 'queryType', 'eq'),
         JSON_OBJECT('field', 'supplierName', 'label', '供应商', 'type', 'input', 'queryType', 'like'),
         JSON_OBJECT('field', 'materialCode', 'label', '物料编号', 'type', 'input', 'queryType', 'like'),
         JSON_OBJECT('field', 'materialName', 'label', '物料名称', 'type', 'input', 'queryType', 'like'),
         JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'input', 'queryType', 'eq')
       ),
       JSON_ARRAY(
         JSON_OBJECT('prop', 'supplierName', 'label', '供应商', 'width', 180),
         JSON_OBJECT('prop', 'materialCode', 'label', '物料编号', 'width', 140),
         JSON_OBJECT('prop', 'materialName', 'label', '物料名称', 'width', 180),
         JSON_OBJECT('prop', 'specModel', 'label', '规格型号', 'width', 150),
         JSON_OBJECT('prop', 'unit', 'label', '单位', 'width', 80),
         JSON_OBJECT('prop', 'quotePriceCent', 'label', '报价(分)', 'width', 120),
         JSON_OBJECT('prop', 'effectiveDate', 'label', '有效期', 'width', 120),
         JSON_OBJECT('prop', 'status', 'label', '状态', 'width', 100)
       ),
       JSON_ARRAY(
         JSON_OBJECT('field', 'supplierId', 'label', '供应商', 'type', 'recordSelector', 'required', true,
           'props', JSON_OBJECT(
             'targetLabelField', 'supplierName',
             'recordSelector', JSON_OBJECT(
               'suiteCode', 'PROCUREMENT_WAREHOUSE',
               'objectCode', 'PW_SUPPLIER',
               'title', '选择供应商',
               'displayFields', JSON_ARRAY('supplierCode:供应商编号', 'supplierName:供应商名称', 'contactName:联系人', 'contactPhone:联系电话'),
               'keywordFields', JSON_ARRAY('supplierCode', 'supplierName', 'contactName'),
               'searchParams', JSON_OBJECT('status', 'ENABLED'),
               'fieldMappings', JSON_ARRAY(
                 JSON_OBJECT('sourceField', 'id', 'targetField', 'supplierId'),
                 JSON_OBJECT('sourceField', 'supplierName', 'targetField', 'supplierName')
               ),
               'valueField', 'id',
               'labelField', 'supplierName',
               'targetLabelField', 'supplierName'
             )
           )
         ),
         JSON_OBJECT('field', 'supplierName', 'label', '供应商名称', 'type', 'input'),
         JSON_OBJECT('field', 'materialId', 'label', '物料', 'type', 'recordSelector', 'required', true,
           'props', JSON_OBJECT(
             'targetLabelField', 'materialName',
             'recordSelector', JSON_OBJECT(
               'suiteCode', 'PROCUREMENT_WAREHOUSE',
               'objectCode', 'PW_MATERIAL',
               'title', '选择物料',
               'displayFields', JSON_ARRAY('materialCode:物料编号', 'materialName:物料名称', 'specModel:规格型号', 'unit:单位', 'referencePriceCent:参考单价(分)'),
               'keywordFields', JSON_ARRAY('materialCode', 'materialName', 'specModel'),
               'searchParams', JSON_OBJECT('status', 'ENABLED'),
               'fieldMappings', JSON_ARRAY(
                 JSON_OBJECT('sourceField', 'id', 'targetField', 'materialId'),
                 JSON_OBJECT('sourceField', 'materialCode', 'targetField', 'materialCode'),
                 JSON_OBJECT('sourceField', 'materialName', 'targetField', 'materialName'),
                 JSON_OBJECT('sourceField', 'specModel', 'targetField', 'specModel'),
                 JSON_OBJECT('sourceField', 'unit', 'targetField', 'unit'),
                 JSON_OBJECT('sourceField', 'referencePriceCent', 'targetField', 'quotePriceCent')
               ),
               'valueField', 'id',
               'labelField', 'materialName',
               'targetLabelField', 'materialName'
             )
           )
         ),
         JSON_OBJECT('field', 'materialCode', 'label', '物料编号', 'type', 'input'),
         JSON_OBJECT('field', 'materialName', 'label', '物料名称', 'type', 'input'),
         JSON_OBJECT('field', 'specModel', 'label', '规格型号', 'type', 'input'),
         JSON_OBJECT('field', 'unit', 'label', '单位', 'type', 'input'),
         JSON_OBJECT('field', 'quotePriceCent', 'label', '报价(分)', 'type', 'number', 'required', true),
         JSON_OBJECT('field', 'lastPriceCent', 'label', '上次报价(分)', 'type', 'number'),
         JSON_OBJECT('field', 'effectiveDate', 'label', '有效期', 'type', 'date'),
         JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'input', 'defaultValue', 'ENABLED'),
         JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea')
       ),
       JSON_OBJECT(
         'list', 'get@/ai/crud/pw_supplier_material/page',
         'detail', 'get@/ai/crud/pw_supplier_material/:id',
         'create', 'post@/ai/crud/pw_supplier_material',
         'update', 'put@/ai/crud/pw_supplier_material',
         'delete', 'delete@/ai/crud/pw_supplier_material/:id',
         'import', 'post@/ai/crud/pw_supplier_material/import',
         'export', 'post@/ai/crud/pw_supplier_material/export',
         'importTemplate', 'get@/ai/crud/pw_supplier_material/import-template'
       ),
       JSON_OBJECT('layoutType', 'simple-crud', 'rowKey', 'id', 'modalWidth', '1040px', 'editGridCols', 2,
                   'showImport', true, 'showExport', true, 'enableDetail', true),
       'CONFIG', 'LOWCODE', '0', 'PUBLISHED', '供应商报价', NULL, 25, NULL,
       'simple-crud', m.model_schema,
       JSON_OBJECT(
         'layoutType', 'simple-crud',
         'primaryModelCode', 'pw_supplier_material',
         'zones', JSON_ARRAY(
           JSON_OBJECT('key', 'search', 'type', 'search', 'props', JSON_OBJECT()),
           JSON_OBJECT('key', 'table', 'type', 'table', 'props', JSON_OBJECT()),
           JSON_OBJECT('key', 'edit', 'type', 'form', 'props', JSON_OBJECT('editGridCols', 2)),
           JSON_OBJECT('key', 'detail', 'type', 'detail', 'props', JSON_OBJECT())
         )
       ),
       1, 1, NOW(), 1, @pw_domain_id, 'PROCUREMENT_WAREHOUSE', 'PW_SUPPLIER_MATERIAL', '供应商报价',
       'pw_supplier_material', 'id', 'id', 'bigint',
       JSON_OBJECT('enabled', true, 'tenantField', 'tenantId', 'tenantColumn', 'tenant_id'),
       JSON_OBJECT('enabled', true, 'createBy', 'create_by', 'createTime', 'create_time', 'createDept', 'create_dept', 'updateBy', 'update_by', 'updateTime', 'update_time'),
       JSON_OBJECT('enabled', true, 'field', 'delFlag', 'column', 'del_flag', 'notDeletedValue', '0', 'deletedValue', '1'),
       1, NOW(), 1, 1, NOW()
FROM ai_lowcode_model m
WHERE m.tenant_id = 1
  AND m.domain_id = @pw_domain_id
  AND m.model_code = 'pw_supplier_material'
  AND NOT EXISTS (
    SELECT 1 FROM ai_crud_config
    WHERE tenant_id = 1 AND config_key = 'pw_supplier_material'
  );

UPDATE ai_business_object
SET config_key = 'pw_supplier_material',
    design_status = 'PUBLISHED',
    last_publish_time = NOW(),
    last_publish_version = GREATEST(COALESCE(last_publish_version, 1), 2),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PROCUREMENT_WAREHOUSE'
  AND object_code = 'PW_SUPPLIER_MATERIAL'
  AND (config_key IS NULL OR config_key = '');

UPDATE ai_crud_config c
JOIN (
  SELECT config_key, table_name, object_code, object_name, child_model_code, child_model_name, child_table_name,
         child_fk_field, child_display_field, child_fields, child_selector, master_edit_schema
  FROM (
    SELECT 'pw_supplier' AS config_key, 'pw_supplier' AS table_name, 'PW_SUPPLIER' AS object_code, '供应商' AS object_name,
           'pw_supplier_material' AS child_model_code, '供应商报价' AS child_model_name, 'pw_supplier_material' AS child_table_name,
           'supplierId' AS child_fk_field, 'materialName' AS child_display_field,
           JSON_ARRAY(
             JSON_OBJECT('field', 'materialId', 'sourceField', 'materialId', 'fieldRef', 'pw_supplier_material__materialId', 'columnName', 'material_id', 'label', '物料ID', 'type', 'number', 'componentType', 'number', 'dataType', 'bigint', 'required', true, 'formVisible', true, 'listVisible', false, 'width', 120),
             JSON_OBJECT('field', 'materialCode', 'sourceField', 'materialCode', 'fieldRef', 'pw_supplier_material__materialCode', 'columnName', 'material_code', 'label', '物料编号', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', true, 'formVisible', true, 'listVisible', true, 'width', 140),
             JSON_OBJECT('field', 'materialName', 'sourceField', 'materialName', 'fieldRef', 'pw_supplier_material__materialName', 'columnName', 'material_name', 'label', '物料名称', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', true, 'formVisible', true, 'listVisible', true, 'width', 180),
             JSON_OBJECT('field', 'specModel', 'sourceField', 'specModel', 'fieldRef', 'pw_supplier_material__specModel', 'columnName', 'spec_model', 'label', '规格型号', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 150),
             JSON_OBJECT('field', 'unit', 'sourceField', 'unit', 'fieldRef', 'pw_supplier_material__unit', 'columnName', 'unit', 'label', '单位', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 80),
             JSON_OBJECT('field', 'quotePriceCent', 'sourceField', 'quotePriceCent', 'fieldRef', 'pw_supplier_material__quotePriceCent', 'columnName', 'quote_price_cent', 'label', '报价(分)', 'type', 'number', 'componentType', 'number', 'dataType', 'bigint', 'required', true, 'formVisible', true, 'listVisible', true, 'width', 120),
             JSON_OBJECT('field', 'lastPriceCent', 'sourceField', 'lastPriceCent', 'fieldRef', 'pw_supplier_material__lastPriceCent', 'columnName', 'last_price_cent', 'label', '上次报价(分)', 'type', 'number', 'componentType', 'number', 'dataType', 'bigint', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 120),
             JSON_OBJECT('field', 'effectiveDate', 'sourceField', 'effectiveDate', 'fieldRef', 'pw_supplier_material__effectiveDate', 'columnName', 'effective_date', 'label', '有效期', 'type', 'date', 'componentType', 'date', 'dataType', 'date', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 120),
             JSON_OBJECT('field', 'status', 'sourceField', 'status', 'fieldRef', 'pw_supplier_material__status', 'columnName', 'status', 'label', '状态', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', true, 'defaultValue', 'ENABLED', 'formVisible', true, 'listVisible', true, 'width', 100)
           ) AS child_fields,
           JSON_OBJECT(
             'suiteCode', 'PROCUREMENT_WAREHOUSE',
             'objectCode', 'PW_MATERIAL',
             'title', '选择物料',
             'buttonText', '选择物料',
             'displayFields', JSON_ARRAY('materialCode:物料编号', 'materialName:物料名称', 'specModel:规格型号', 'unit:单位', 'referencePriceCent:参考单价(分)'),
             'keywordFields', JSON_ARRAY('materialCode', 'materialName', 'specModel'),
             'searchParams', JSON_OBJECT('status', 'ENABLED'),
             'fieldMappings', JSON_ARRAY(
               JSON_OBJECT('sourceField', 'id', 'targetField', 'materialId'),
               JSON_OBJECT('sourceField', 'materialCode', 'targetField', 'materialCode'),
               JSON_OBJECT('sourceField', 'materialName', 'targetField', 'materialName'),
               JSON_OBJECT('sourceField', 'specModel', 'targetField', 'specModel'),
               JSON_OBJECT('sourceField', 'unit', 'targetField', 'unit'),
               JSON_OBJECT('sourceField', 'referencePriceCent', 'targetField', 'quotePriceCent')
             )
           ) AS child_selector,
           JSON_ARRAY(JSON_OBJECT('field', 'supplierCode', 'label', '供应商编号', 'type', 'input', 'required', true), JSON_OBJECT('field', 'supplierName', 'label', '供应商名称', 'type', 'input', 'required', true), JSON_OBJECT('field', 'contactName', 'label', '联系人', 'type', 'input'), JSON_OBJECT('field', 'contactPhone', 'label', '联系电话', 'type', 'input'), JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'input', 'defaultValue', 'ENABLED'), JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea')) AS master_edit_schema
    UNION ALL
    SELECT 'pw_purchase_order', 'pw_purchase_order', 'PW_PURCHASE_ORDER', '采购单',
           'pw_purchase_order_item', '采购明细', 'pw_purchase_order_item',
           'purchaseId', 'materialName',
           JSON_ARRAY(
             JSON_OBJECT('field', 'materialId', 'sourceField', 'materialId', 'fieldRef', 'pw_purchase_order_item__materialId', 'columnName', 'material_id', 'label', '物料ID', 'type', 'number', 'componentType', 'number', 'dataType', 'bigint', 'required', true, 'formVisible', true, 'listVisible', false, 'width', 120),
             JSON_OBJECT('field', 'materialCode', 'sourceField', 'materialCode', 'fieldRef', 'pw_purchase_order_item__materialCode', 'columnName', 'material_code', 'label', '物料编号', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', true, 'formVisible', true, 'listVisible', true, 'width', 140),
             JSON_OBJECT('field', 'materialName', 'sourceField', 'materialName', 'fieldRef', 'pw_purchase_order_item__materialName', 'columnName', 'material_name', 'label', '物料名称', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', true, 'formVisible', true, 'listVisible', true, 'width', 180),
             JSON_OBJECT('field', 'specModel', 'sourceField', 'specModel', 'fieldRef', 'pw_purchase_order_item__specModel', 'columnName', 'spec_model', 'label', '规格型号', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 150),
             JSON_OBJECT('field', 'unit', 'sourceField', 'unit', 'fieldRef', 'pw_purchase_order_item__unit', 'columnName', 'unit', 'label', '单位', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 80),
             JSON_OBJECT('field', 'quantity', 'sourceField', 'quantity', 'fieldRef', 'pw_purchase_order_item__quantity', 'columnName', 'quantity', 'label', '数量', 'type', 'number', 'componentType', 'number', 'dataType', 'decimal', 'precision', 3, 'required', true, 'formVisible', true, 'listVisible', true, 'width', 100),
             JSON_OBJECT('field', 'costPriceCent', 'sourceField', 'costPriceCent', 'fieldRef', 'pw_purchase_order_item__costPriceCent', 'columnName', 'cost_price_cent', 'label', '成本单价(分)', 'type', 'number', 'componentType', 'number', 'dataType', 'bigint', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 120),
             JSON_OBJECT('field', 'dealPriceCent', 'sourceField', 'dealPriceCent', 'fieldRef', 'pw_purchase_order_item__dealPriceCent', 'columnName', 'deal_price_cent', 'label', '成交单价(分)', 'type', 'number', 'componentType', 'number', 'dataType', 'bigint', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 120),
             JSON_OBJECT('field', 'amountCent', 'sourceField', 'amountCent', 'fieldRef', 'pw_purchase_order_item__amountCent', 'columnName', 'amount_cent', 'label', '金额(分)', 'type', 'number', 'componentType', 'number', 'dataType', 'bigint', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 120)
           ),
           JSON_OBJECT(
             'suiteCode', 'PROCUREMENT_WAREHOUSE',
             'objectCode', 'PW_SUPPLIER_MATERIAL',
             'title', '选择供应商报价',
             'buttonText', '选择报价',
             'displayFields', JSON_ARRAY('supplierName:供应商', 'materialCode:物料编号', 'materialName:物料名称', 'quotePriceCent:报价(分)', 'effectiveDate:有效期'),
             'keywordFields', JSON_ARRAY('materialCode', 'materialName', 'supplierName'),
             'searchParams', JSON_OBJECT('supplierId', CONCAT('$', '{formData.supplierId}'), 'status', 'ENABLED'),
             'fieldMappings', JSON_ARRAY(
               JSON_OBJECT('sourceField', 'materialId', 'targetField', 'materialId'),
               JSON_OBJECT('sourceField', 'materialCode', 'targetField', 'materialCode'),
               JSON_OBJECT('sourceField', 'materialName', 'targetField', 'materialName'),
               JSON_OBJECT('sourceField', 'specModel', 'targetField', 'specModel'),
               JSON_OBJECT('sourceField', 'unit', 'targetField', 'unit'),
               JSON_OBJECT('sourceField', 'lastPriceCent', 'targetField', 'costPriceCent'),
               JSON_OBJECT('sourceField', 'quotePriceCent', 'targetField', 'dealPriceCent')
             )
           ),
           JSON_ARRAY(
             JSON_OBJECT('field', 'purchaseNo', 'label', '采购单号', 'type', 'input', 'required', true),
             JSON_OBJECT('field', 'projectName', 'label', '项目名称', 'type', 'input', 'required', true),
             JSON_OBJECT('field', 'warehouseId', 'label', '目标仓库', 'type', 'recordSelector', 'props', JSON_OBJECT('targetLabelField', 'warehouseName', 'recordSelector', JSON_OBJECT('suiteCode', 'PROCUREMENT_WAREHOUSE', 'objectCode', 'PW_WAREHOUSE', 'title', '选择仓库', 'displayFields', JSON_ARRAY('warehouseCode:仓库编号', 'warehouseName:仓库名称', 'warehouseType:类型', 'projectName:关联项目'), 'keywordFields', JSON_ARRAY('warehouseCode', 'warehouseName', 'projectName'), 'searchParams', JSON_OBJECT('status', 'ENABLED'), 'fieldMappings', JSON_ARRAY(JSON_OBJECT('sourceField', 'id', 'targetField', 'warehouseId'), JSON_OBJECT('sourceField', 'warehouseName', 'targetField', 'warehouseName')), 'valueField', 'id', 'labelField', 'warehouseName', 'targetLabelField', 'warehouseName'))),
             JSON_OBJECT('field', 'supplierId', 'label', '供应商', 'type', 'recordSelector', 'props', JSON_OBJECT('targetLabelField', 'supplierName', 'recordSelector', JSON_OBJECT('suiteCode', 'PROCUREMENT_WAREHOUSE', 'objectCode', 'PW_SUPPLIER', 'title', '选择供应商', 'displayFields', JSON_ARRAY('supplierCode:供应商编号', 'supplierName:供应商名称', 'contactName:联系人', 'contactPhone:联系电话'), 'keywordFields', JSON_ARRAY('supplierCode', 'supplierName', 'contactName'), 'searchParams', JSON_OBJECT('status', 'ENABLED'), 'fieldMappings', JSON_ARRAY(JSON_OBJECT('sourceField', 'id', 'targetField', 'supplierId'), JSON_OBJECT('sourceField', 'supplierName', 'targetField', 'supplierName'), JSON_OBJECT('sourceField', 'contactName', 'targetField', 'supplierContact'), JSON_OBJECT('sourceField', 'contactPhone', 'targetField', 'supplierPhone')), 'valueField', 'id', 'labelField', 'supplierName', 'targetLabelField', 'supplierName'))),
             JSON_OBJECT('field', 'purchaserName', 'label', '采购人', 'type', 'input'),
             JSON_OBJECT('field', 'purchaseDate', 'label', '采购日期', 'type', 'date'),
             JSON_OBJECT('field', 'supplierContact', 'label', '供应商联系人', 'type', 'input'),
             JSON_OBJECT('field', 'supplierPhone', 'label', '供应商联系电话', 'type', 'input'),
             JSON_OBJECT('field', 'purchaseAmountCent', 'label', '采购金额(分)', 'type', 'number'),
             JSON_OBJECT('field', 'orderStatus', 'label', '状态', 'type', 'input', 'defaultValue', 'DRAFT'),
             JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea')
           )
    UNION ALL
    SELECT 'pw_outbound_order', 'pw_outbound_order', 'PW_OUTBOUND_ORDER', '出库单',
           'pw_outbound_order_item', '出库明细', 'pw_outbound_order_item',
           'outboundId', 'materialName',
           JSON_ARRAY(
             JSON_OBJECT('field', 'warehouseId', 'sourceField', 'warehouseId', 'fieldRef', 'pw_outbound_order_item__warehouseId', 'columnName', 'warehouse_id', 'label', '仓库ID', 'type', 'number', 'componentType', 'number', 'dataType', 'bigint', 'required', false, 'formVisible', false, 'listVisible', false, 'width', 120),
             JSON_OBJECT('field', 'materialId', 'sourceField', 'materialId', 'fieldRef', 'pw_outbound_order_item__materialId', 'columnName', 'material_id', 'label', '物料ID', 'type', 'number', 'componentType', 'number', 'dataType', 'bigint', 'required', true, 'formVisible', true, 'listVisible', false, 'width', 120),
             JSON_OBJECT('field', 'materialCode', 'sourceField', 'materialCode', 'fieldRef', 'pw_outbound_order_item__materialCode', 'columnName', 'material_code', 'label', '物料编号', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', true, 'formVisible', true, 'listVisible', true, 'width', 140),
             JSON_OBJECT('field', 'materialName', 'sourceField', 'materialName', 'fieldRef', 'pw_outbound_order_item__materialName', 'columnName', 'material_name', 'label', '物料名称', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', true, 'formVisible', true, 'listVisible', true, 'width', 180),
             JSON_OBJECT('field', 'specModel', 'sourceField', 'specModel', 'fieldRef', 'pw_outbound_order_item__specModel', 'columnName', 'spec_model', 'label', '规格型号', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 150),
             JSON_OBJECT('field', 'unit', 'sourceField', 'unit', 'fieldRef', 'pw_outbound_order_item__unit', 'columnName', 'unit', 'label', '单位', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 80),
             JSON_OBJECT('field', 'stockQuantity', 'sourceField', 'stockQuantity', 'fieldRef', 'pw_outbound_order_item__stockQuantity', 'columnName', 'stock_quantity', 'label', '库存数量', 'type', 'number', 'componentType', 'number', 'dataType', 'decimal', 'precision', 3, 'required', false, 'formVisible', true, 'listVisible', true, 'width', 100),
             JSON_OBJECT('field', 'outboundQuantity', 'sourceField', 'outboundQuantity', 'fieldRef', 'pw_outbound_order_item__outboundQuantity', 'columnName', 'outbound_quantity', 'label', '出库数量', 'type', 'number', 'componentType', 'number', 'dataType', 'decimal', 'precision', 3, 'required', true, 'formVisible', true, 'listVisible', true, 'width', 100)
           ),
           JSON_OBJECT(
             'suiteCode', 'PROCUREMENT_WAREHOUSE',
             'objectCode', 'PW_MATERIAL',
             'title', '选择出库物料',
             'buttonText', '选择物料',
             'displayFields', JSON_ARRAY('materialCode:物料编号', 'materialName:物料名称', 'specModel:规格型号', 'unit:单位'),
             'keywordFields', JSON_ARRAY('materialCode', 'materialName', 'specModel'),
             'searchParams', JSON_OBJECT('status', 'ENABLED', 'warehouseId', CONCAT('$', '{formData.warehouseId}')),
             'fieldMappings', JSON_ARRAY(
               JSON_OBJECT('sourceField', 'id', 'targetField', 'materialId'),
               JSON_OBJECT('sourceField', 'materialCode', 'targetField', 'materialCode'),
               JSON_OBJECT('sourceField', 'materialName', 'targetField', 'materialName'),
               JSON_OBJECT('sourceField', 'specModel', 'targetField', 'specModel'),
               JSON_OBJECT('sourceField', 'unit', 'targetField', 'unit')
             )
           ),
           JSON_ARRAY(
             JSON_OBJECT('field', 'outboundNo', 'label', '出库单号', 'type', 'input', 'required', true),
             JSON_OBJECT('field', 'warehouseId', 'label', '所属仓库', 'type', 'recordSelector', 'required', true, 'props', JSON_OBJECT('targetLabelField', 'warehouseName', 'recordSelector', JSON_OBJECT('suiteCode', 'PROCUREMENT_WAREHOUSE', 'objectCode', 'PW_WAREHOUSE', 'title', '选择仓库', 'displayFields', JSON_ARRAY('warehouseCode:仓库编号', 'warehouseName:仓库名称', 'warehouseType:类型', 'projectName:关联项目'), 'keywordFields', JSON_ARRAY('warehouseCode', 'warehouseName', 'projectName'), 'searchParams', JSON_OBJECT('status', 'ENABLED'), 'fieldMappings', JSON_ARRAY(JSON_OBJECT('sourceField', 'id', 'targetField', 'warehouseId'), JSON_OBJECT('sourceField', 'warehouseName', 'targetField', 'warehouseName')), 'valueField', 'id', 'labelField', 'warehouseName', 'targetLabelField', 'warehouseName'))),
             JSON_OBJECT('field', 'applicantName', 'label', '申请人', 'type', 'input'),
             JSON_OBJECT('field', 'outboundDate', 'label', '出库日期', 'type', 'date'),
             JSON_OBJECT('field', 'outboundReason', 'label', '出库原因', 'type', 'input'),
             JSON_OBJECT('field', 'orderStatus', 'label', '状态', 'type', 'input', 'defaultValue', 'DRAFT'),
             JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea')
           )
    UNION ALL
    SELECT 'pw_transfer_order', 'pw_transfer_order', 'PW_TRANSFER_ORDER', '调拨单',
           'pw_transfer_order_item', '调拨明细', 'pw_transfer_order_item',
           'transferId', 'materialName',
           JSON_ARRAY(
             JSON_OBJECT('field', 'materialId', 'sourceField', 'materialId', 'fieldRef', 'pw_transfer_order_item__materialId', 'columnName', 'material_id', 'label', '物料ID', 'type', 'number', 'componentType', 'number', 'dataType', 'bigint', 'required', true, 'formVisible', true, 'listVisible', false, 'width', 120),
             JSON_OBJECT('field', 'materialCode', 'sourceField', 'materialCode', 'fieldRef', 'pw_transfer_order_item__materialCode', 'columnName', 'material_code', 'label', '物料编号', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', true, 'formVisible', true, 'listVisible', true, 'width', 140),
             JSON_OBJECT('field', 'materialName', 'sourceField', 'materialName', 'fieldRef', 'pw_transfer_order_item__materialName', 'columnName', 'material_name', 'label', '物料名称', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', true, 'formVisible', true, 'listVisible', true, 'width', 180),
             JSON_OBJECT('field', 'specModel', 'sourceField', 'specModel', 'fieldRef', 'pw_transfer_order_item__specModel', 'columnName', 'spec_model', 'label', '规格型号', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 150),
             JSON_OBJECT('field', 'unit', 'sourceField', 'unit', 'fieldRef', 'pw_transfer_order_item__unit', 'columnName', 'unit', 'label', '单位', 'type', 'input', 'componentType', 'input', 'dataType', 'varchar', 'required', false, 'formVisible', true, 'listVisible', true, 'width', 80),
             JSON_OBJECT('field', 'currentStockQuantity', 'sourceField', 'currentStockQuantity', 'fieldRef', 'pw_transfer_order_item__currentStockQuantity', 'columnName', 'current_stock_quantity', 'label', '当前库存', 'type', 'number', 'componentType', 'number', 'dataType', 'decimal', 'precision', 3, 'required', false, 'formVisible', true, 'listVisible', true, 'width', 100),
             JSON_OBJECT('field', 'transferQuantity', 'sourceField', 'transferQuantity', 'fieldRef', 'pw_transfer_order_item__transferQuantity', 'columnName', 'transfer_quantity', 'label', '调拨数量', 'type', 'number', 'componentType', 'number', 'dataType', 'decimal', 'precision', 3, 'required', true, 'formVisible', true, 'listVisible', true, 'width', 100)
           ),
           JSON_OBJECT(
             'suiteCode', 'PROCUREMENT_WAREHOUSE',
             'objectCode', 'PW_MATERIAL',
             'title', '选择调拨物料',
             'buttonText', '选择物料',
             'displayFields', JSON_ARRAY('materialCode:物料编号', 'materialName:物料名称', 'specModel:规格型号', 'unit:单位'),
             'keywordFields', JSON_ARRAY('materialCode', 'materialName', 'specModel'),
             'searchParams', JSON_OBJECT('status', 'ENABLED', 'fromWarehouseId', CONCAT('$', '{formData.fromWarehouseId}')),
             'fieldMappings', JSON_ARRAY(
               JSON_OBJECT('sourceField', 'id', 'targetField', 'materialId'),
               JSON_OBJECT('sourceField', 'materialCode', 'targetField', 'materialCode'),
               JSON_OBJECT('sourceField', 'materialName', 'targetField', 'materialName'),
               JSON_OBJECT('sourceField', 'specModel', 'targetField', 'specModel'),
               JSON_OBJECT('sourceField', 'unit', 'targetField', 'unit')
             )
           ),
           JSON_ARRAY(
             JSON_OBJECT('field', 'transferNo', 'label', '调拨单号', 'type', 'input', 'required', true),
             JSON_OBJECT('field', 'fromWarehouseId', 'label', '调出仓库', 'type', 'recordSelector', 'required', true, 'props', JSON_OBJECT('targetLabelField', 'fromWarehouseName', 'recordSelector', JSON_OBJECT('suiteCode', 'PROCUREMENT_WAREHOUSE', 'objectCode', 'PW_WAREHOUSE', 'title', '选择调出仓库', 'displayFields', JSON_ARRAY('warehouseCode:仓库编号', 'warehouseName:仓库名称', 'warehouseType:类型', 'projectName:关联项目'), 'keywordFields', JSON_ARRAY('warehouseCode', 'warehouseName', 'projectName'), 'searchParams', JSON_OBJECT('status', 'ENABLED'), 'fieldMappings', JSON_ARRAY(JSON_OBJECT('sourceField', 'id', 'targetField', 'fromWarehouseId'), JSON_OBJECT('sourceField', 'warehouseName', 'targetField', 'fromWarehouseName')), 'valueField', 'id', 'labelField', 'warehouseName', 'targetLabelField', 'fromWarehouseName'))),
             JSON_OBJECT('field', 'toWarehouseId', 'label', '调入仓库', 'type', 'recordSelector', 'required', true, 'props', JSON_OBJECT('targetLabelField', 'toWarehouseName', 'recordSelector', JSON_OBJECT('suiteCode', 'PROCUREMENT_WAREHOUSE', 'objectCode', 'PW_WAREHOUSE', 'title', '选择调入仓库', 'displayFields', JSON_ARRAY('warehouseCode:仓库编号', 'warehouseName:仓库名称', 'warehouseType:类型', 'projectName:关联项目'), 'keywordFields', JSON_ARRAY('warehouseCode', 'warehouseName', 'projectName'), 'searchParams', JSON_OBJECT('status', 'ENABLED'), 'fieldMappings', JSON_ARRAY(JSON_OBJECT('sourceField', 'id', 'targetField', 'toWarehouseId'), JSON_OBJECT('sourceField', 'warehouseName', 'targetField', 'toWarehouseName')), 'valueField', 'id', 'labelField', 'warehouseName', 'targetLabelField', 'toWarehouseName'))),
             JSON_OBJECT('field', 'transferPersonName', 'label', '调拨人', 'type', 'input'),
             JSON_OBJECT('field', 'transferDate', 'label', '调拨日期', 'type', 'date'),
             JSON_OBJECT('field', 'transferReason', 'label', '调拨原因', 'type', 'input'),
             JSON_OBJECT('field', 'orderStatus', 'label', '状态', 'type', 'input', 'defaultValue', 'DRAFT'),
             JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea')
           )
  ) seed_master_detail
) md ON md.config_key = c.config_key AND c.tenant_id = 1
SET c.layout_type = 'master-detail-crud',
    c.edit_schema = md.master_edit_schema,
    c.options = JSON_OBJECT(
      'layoutType', 'master-detail-crud',
      'rowKey', 'id',
      'modalWidth', '1280px',
      'editGridCols', 2,
      'showImport', true,
      'showExport', true,
      'enableDetail', true,
      'masterDetailConfig', JSON_OBJECT(
        'primary', JSON_OBJECT('modelCode', md.config_key, 'modelName', md.object_name, 'tableName', md.table_name, 'keyField', 'id'),
        'children', JSON_ARRAY(
          JSON_OBJECT(
            'key', md.child_model_code,
            'modelCode', md.child_model_code,
            'modelName', md.child_model_name,
            'tableName', md.child_table_name,
            'relationType', 'ONE_TO_MANY',
            'sourceField', md.child_fk_field,
            'targetField', 'id',
            'showInCreate', true,
            'showInEdit', true,
            'showInDetail', true,
            'saveMode', 'merge',
            'tabTitle', md.child_model_name,
            'relationName', md.child_model_name,
            'recordSelector', md.child_selector,
            'fields', md.child_fields
          )
        )
      )
    ),
    c.page_schema = JSON_OBJECT(
      'layoutType', 'master-detail-crud',
      'primaryModelCode', md.config_key,
      'modelRefs', JSON_ARRAY(
        JSON_OBJECT(
          'modelCode', md.config_key,
          'modelName', md.object_name,
          'tableName', md.table_name,
          'primary', true,
          'relations', JSON_ARRAY(
            JSON_OBJECT('relationType', 'ONE_TO_MANY', 'targetObjectCode', md.child_model_code, 'sourceField', 'id', 'targetField', md.child_fk_field, 'displayField', md.child_display_field)
          ),
          'props', JSON_OBJECT(),
          'fields', JSON_ARRAY()
        ),
        JSON_OBJECT(
          'modelCode', md.child_model_code,
          'modelName', md.child_model_name,
          'tableName', md.child_table_name,
          'primary', false,
          'relations', JSON_ARRAY(
            JSON_OBJECT('relationType', 'ONE_TO_MANY', 'targetObjectCode', md.config_key, 'sourceField', md.child_fk_field, 'targetField', 'id', 'displayField', md.child_display_field)
          ),
          'props', JSON_OBJECT('saveMode', 'merge', 'inlineCreateEnabled', true, 'inlineEditEnabled', true, 'showInDetail', true, 'tabTitle', md.child_model_name, 'relationName', md.child_model_name, 'recordSelector', md.child_selector),
          'fields', md.child_fields
        )
      ),
      'zones', JSON_ARRAY(
        JSON_OBJECT('key', 'search', 'type', 'search', 'props', JSON_OBJECT()),
        JSON_OBJECT('key', 'table', 'type', 'table', 'props', JSON_OBJECT()),
        JSON_OBJECT('key', 'edit', 'type', 'form', 'props', JSON_OBJECT('editGridCols', 2)),
        JSON_OBJECT('key', 'detail', 'type', 'detail', 'props', JSON_OBJECT())
      )
    ),
    c.published_version = GREATEST(COALESCE(c.published_version, 1), 2),
    c.publish_time = NOW(),
    c.update_by = 1,
    c.update_time = NOW();

INSERT INTO ai_business_app (id, tenant_id, app_code, app_name, app_type, suite_code, object_code, entry_mode,
                             entry_url, config_key, icon, description, status, sort_order, options,
                             create_by, create_time, create_dept, update_by, update_time)
SELECT app_id, 1, app_code, app_name, 'BUSINESS', 'PROCUREMENT_WAREHOUSE', object_code, 'RUNTIME',
       NULL, config_key, icon, description, 1, sort_order,
       JSON_OBJECT('lowcodeApp', true, 'runtimeOpenMode', 'LIST', 'targetPageKey', 'list'),
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000000941 AS app_id, 'PW_MATERIAL_MANAGE' AS app_code, '物料管理' AS app_name, 'PW_MATERIAL' AS object_code, 'pw_material' AS config_key, 'ionicons5:CubeOutline' AS icon, '物料编号、名称、规格、单位和参考价维护' AS description, 10 AS sort_order
  UNION ALL SELECT 1910000000000000942, 'PW_SUPPLIER_MANAGE', '供应商管理', 'PW_SUPPLIER', 'pw_supplier', 'ionicons5:PeopleOutline', '供应商基础资料和联系人维护', 20
  UNION ALL SELECT 1910000000000000943, 'PW_WAREHOUSE_MANAGE', '仓储管理', 'PW_WAREHOUSE', 'pw_warehouse', 'ionicons5:HomeOutline', '中心仓库和项目现场仓维护', 30
  UNION ALL SELECT 1910000000000000944, 'PW_PURCHASE_MANAGE', '采购管理', 'PW_PURCHASE_ORDER', 'pw_purchase_order', 'ionicons5:CartOutline', '采购单基础 CRUD，后续接入审批和入库动作', 40
  UNION ALL SELECT 1910000000000000945, 'PW_OUTBOUND_MANAGE', '出库管理', 'PW_OUTBOUND_ORDER', 'pw_outbound_order', 'ionicons5:ExitOutline', '出库申请基础 CRUD，后续接入锁定和扣减动作', 50
  UNION ALL SELECT 1910000000000000946, 'PW_TRANSFER_MANAGE', '调拨管理', 'PW_TRANSFER_ORDER', 'pw_transfer_order', 'ionicons5:SwapHorizontalOutline', '调拨申请基础 CRUD，后续接入转移动作', 60
) seed_apps
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_app
  WHERE tenant_id = 1 AND app_code = seed_apps.app_code
);
