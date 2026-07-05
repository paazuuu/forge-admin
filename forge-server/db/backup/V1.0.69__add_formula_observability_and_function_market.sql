-- ============================================================
-- V1.0.69: 公式可观测性与函数市场底座
-- ============================================================

CREATE TABLE IF NOT EXISTS `ai_formula_execution_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '公式执行链路ID',
  `object_code` varchar(64) NOT NULL COMMENT '业务对象编码',
  `record_id` varchar(64) DEFAULT NULL COMMENT '业务记录ID',
  `field_code` varchar(64) NOT NULL COMMENT '公式字段编码',
  `formula_type` varchar(32) DEFAULT NULL COMMENT '公式类型：CALC/AGGREGATE/CONDITIONAL/LOOKUP',
  `formula_mode` varchar(32) DEFAULT NULL COMMENT '计算模式：STORED/VIRTUAL',
  `expression` text DEFAULT NULL COMMENT '表达式或配置摘要',
  `input_snapshot` json DEFAULT NULL COMMENT '输入快照，脱敏后存储',
  `output_value` text DEFAULT NULL COMMENT '输出值摘要，脱敏后存储',
  `success` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否执行成功：1成功 0失败',
  `error_message` text DEFAULT NULL COMMENT '错误信息',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_formula_log_trace` (`tenant_id`, `trace_id`),
  KEY `idx_ai_formula_log_record` (`tenant_id`, `object_code`, `record_id`, `create_time`),
  KEY `idx_ai_formula_log_field` (`tenant_id`, `field_code`, `create_time`),
  KEY `idx_ai_formula_log_success` (`tenant_id`, `success`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-公式执行日志表';

CREATE TABLE IF NOT EXISTS `ai_formula_function` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `function_code` varchar(64) NOT NULL COMMENT '函数编码',
  `display_name` varchar(128) NOT NULL COMMENT '展示名称',
  `category` varchar(64) NOT NULL COMMENT '函数分类',
  `description` varchar(500) DEFAULT NULL COMMENT '函数说明',
  `source_type` varchar(32) NOT NULL DEFAULT 'BUILTIN' COMMENT '来源类型：BUILTIN/SYSTEM/TENANT/MARKET',
  `argument_schema` json DEFAULT NULL COMMENT '参数Schema',
  `return_type` varchar(32) DEFAULT NULL COMMENT '返回类型',
  `example` text DEFAULT NULL COMMENT '示例',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `current_version` varchar(32) DEFAULT NULL COMMENT '当前版本',
  `latest_version` varchar(32) DEFAULT NULL COMMENT '最新版本',
  `builtin` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否内置函数',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_formula_function_code` (`tenant_id`, `function_code`),
  KEY `idx_ai_formula_function_query` (`tenant_id`, `category`, `source_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-公式函数注册表';

CREATE TABLE IF NOT EXISTS `ai_formula_function_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `function_code` varchar(64) NOT NULL COMMENT '函数编码',
  `version` varchar(32) NOT NULL COMMENT '函数版本',
  `implementation_type` varchar(32) NOT NULL DEFAULT 'JAVA_BEAN' COMMENT '实现类型',
  `bean_name` varchar(128) DEFAULT NULL COMMENT 'Spring Bean名称',
  `method_name` varchar(128) DEFAULT NULL COMMENT '方法名称',
  `argument_schema` json DEFAULT NULL COMMENT '参数Schema',
  `return_type` varchar(32) DEFAULT NULL COMMENT '返回类型',
  `example` text DEFAULT NULL COMMENT '示例',
  `release_note` varchar(500) DEFAULT NULL COMMENT '版本说明',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED/DEPRECATED',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_formula_function_version` (`tenant_id`, `function_code`, `version`),
  KEY `idx_ai_formula_function_version_code` (`tenant_id`, `function_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-公式函数版本表';

CREATE TABLE IF NOT EXISTS `ai_formula_function_install` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `function_code` varchar(64) NOT NULL COMMENT '函数编码',
  `installed_version` varchar(32) NOT NULL COMMENT '已安装版本',
  `install_status` varchar(32) NOT NULL DEFAULT 'INSTALLED' COMMENT '安装状态：INSTALLED/UNINSTALLED',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `source_type` varchar(32) NOT NULL DEFAULT 'BUILTIN' COMMENT '来源类型：BUILTIN/SYSTEM/TENANT/MARKET',
  `installed_by` bigint DEFAULT NULL COMMENT '安装人',
  `installed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_formula_function_install` (`tenant_id`, `function_code`),
  KEY `idx_ai_formula_function_install_status` (`tenant_id`, `install_status`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-公式函数安装表';

INSERT INTO `ai_formula_function` (
  id, tenant_id, function_code, display_name, category, description, source_type,
  argument_schema, return_type, example, status, current_version, latest_version,
  builtin, sort_order, create_by, create_time, create_dept, update_by, update_time
)
SELECT seed.id, 1, seed.function_code, seed.display_name, seed.category, seed.description, 'BUILTIN',
       seed.argument_schema, seed.return_type, seed.example, 'ENABLED', '1.0.0', '1.0.0',
       1, seed.sort_order, NULL, NOW(), NULL, NULL, NOW()
FROM (
  SELECT 690001001 AS id, 'math.abs' AS function_code, '绝对值' AS display_name, 'Math' AS category, '返回数字绝对值' AS description, JSON_ARRAY(JSON_OBJECT('name','value','type','NUMBER','required',true)) AS argument_schema, 'NUMBER' AS return_type, 'math.abs(-5) => 5' AS example, 10 AS sort_order
  UNION ALL SELECT 690001002, 'math.round', '四舍五入', 'Math', '返回最接近的整数', JSON_ARRAY(JSON_OBJECT('name','value','type','NUMBER','required',true)), 'NUMBER', 'math.round(3.6) => 4', 20
  UNION ALL SELECT 690001010, 'math.floor', '向下取整', 'Math', '返回不大于入参的最大整数', JSON_ARRAY(JSON_OBJECT('name','value','type','NUMBER','required',true)), 'NUMBER', 'math.floor(3.9) => 3', 25
  UNION ALL SELECT 690001011, 'math.ceil', '向上取整', 'Math', '返回不小于入参的最小整数', JSON_ARRAY(JSON_OBJECT('name','value','type','NUMBER','required',true)), 'NUMBER', 'math.ceil(3.1) => 4', 26
  UNION ALL SELECT 690001003, 'math.max', '最大值', 'Math', '返回两个数字中的最大值', JSON_ARRAY(JSON_OBJECT('name','left','type','NUMBER','required',true), JSON_OBJECT('name','right','type','NUMBER','required',true)), 'NUMBER', 'math.max(3, 7) => 7', 30
  UNION ALL SELECT 690001004, 'math.min', '最小值', 'Math', '返回两个数字中的最小值', JSON_ARRAY(JSON_OBJECT('name','left','type','NUMBER','required',true), JSON_OBJECT('name','right','type','NUMBER','required',true)), 'NUMBER', 'math.min(3, 7) => 3', 40
  UNION ALL SELECT 690001012, 'math.pow', '幂运算', 'Math', '返回 left 的 right 次方', JSON_ARRAY(JSON_OBJECT('name','left','type','NUMBER','required',true), JSON_OBJECT('name','right','type','NUMBER','required',true)), 'NUMBER', 'math.pow(2, 3) => 8', 50
  UNION ALL SELECT 690001013, 'math.sqrt', '平方根', 'Math', '返回数字平方根', JSON_ARRAY(JSON_OBJECT('name','value','type','NUMBER','required',true)), 'NUMBER', 'math.sqrt(16) => 4', 60
  UNION ALL SELECT 690001005, 'string.length', '字符串长度', 'String', '返回字符串长度', JSON_ARRAY(JSON_OBJECT('name','value','type','STRING','required',true)), 'NUMBER', 'string.length(''hello'') => 5', 110
  UNION ALL SELECT 690001006, 'string.contains', '包含判断', 'String', '判断字符串是否包含子串', JSON_ARRAY(JSON_OBJECT('name','value','type','STRING','required',true), JSON_OBJECT('name','keyword','type','STRING','required',true)), 'BOOLEAN', 'string.contains(''hello'',''ll'') => true', 120
  UNION ALL SELECT 690001007, 'string.startsWith', '前缀判断', 'String', '判断字符串前缀', JSON_ARRAY(JSON_OBJECT('name','value','type','STRING','required',true), JSON_OBJECT('name','prefix','type','STRING','required',true)), 'BOOLEAN', 'string.startsWith(''hello'',''he'') => true', 130
  UNION ALL SELECT 690001008, 'string.endsWith', '后缀判断', 'String', '判断字符串后缀', JSON_ARRAY(JSON_OBJECT('name','value','type','STRING','required',true), JSON_OBJECT('name','suffix','type','STRING','required',true)), 'BOOLEAN', 'string.endsWith(''hello'',''lo'') => true', 140
  UNION ALL SELECT 690001014, 'string.indexOf', '子串位置', 'String', '返回子串首次出现位置', JSON_ARRAY(JSON_OBJECT('name','value','type','STRING','required',true), JSON_OBJECT('name','keyword','type','STRING','required',true)), 'NUMBER', 'string.indexOf(''hello'',''l'') => 2', 150
  UNION ALL SELECT 690001015, 'string.replace', '字符串替换', 'String', '替换字符串片段', JSON_ARRAY(JSON_OBJECT('name','value','type','STRING','required',true), JSON_OBJECT('name','target','type','STRING','required',true), JSON_OBJECT('name','replacement','type','STRING','required',true)), 'STRING', 'string.replace(''hello'',''l'',''x'') => ''hexxo''', 160
  UNION ALL SELECT 690001016, 'seq.list', '创建列表', 'Collection', '按入参顺序创建列表', JSON_ARRAY(), 'COLLECTION', 'seq.list(1,2,3) => [1,2,3]', 310
  UNION ALL SELECT 690001017, 'seq.set', '创建集合', 'Collection', '按入参顺序创建去重集合', JSON_ARRAY(), 'COLLECTION', 'seq.set(1,2,3) => [1,2,3]', 320
  UNION ALL SELECT 690001018, 'seq.map', '创建映射', 'Collection', '按 key/value 入参创建映射', JSON_ARRAY(), 'MAP', 'seq.map(''a'',1,''b'',2) => {a:1,b:2}', 330
  UNION ALL SELECT 690001009, 'date_to_string', '日期格式化', 'Date', '将日期格式化为字符串', JSON_ARRAY(JSON_OBJECT('name','date','type','DATE','required',true), JSON_OBJECT('name','pattern','type','STRING','required',true)), 'STRING', 'date_to_string(date, ''yyyy-MM-dd'')', 210
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM `ai_formula_function` existing
  WHERE existing.tenant_id = 1
    AND existing.function_code = seed.function_code
);

INSERT INTO `ai_formula_function_version` (
  id, tenant_id, function_code, version, implementation_type, bean_name, method_name,
  argument_schema, return_type, example, release_note, status,
  create_by, create_time, create_dept, update_by, update_time
)
SELECT seed.id, 1, seed.function_code, '1.0.0', 'JAVA_BEAN', seed.bean_name, seed.method_name,
       f.argument_schema, f.return_type, f.example, '内置函数初始版本', 'ENABLED',
       NULL, NOW(), NULL, NULL, NOW()
FROM (
  SELECT 690002001 AS id, 'math.abs' AS function_code, 'formulaBuiltinFunctionProvider' AS bean_name, 'mathAbs' AS method_name
  UNION ALL SELECT 690002002, 'math.round', 'formulaBuiltinFunctionProvider', 'mathRound'
  UNION ALL SELECT 690002010, 'math.floor', 'formulaBuiltinFunctionProvider', 'mathFloor'
  UNION ALL SELECT 690002011, 'math.ceil', 'formulaBuiltinFunctionProvider', 'mathCeil'
  UNION ALL SELECT 690002003, 'math.max', 'formulaBuiltinFunctionProvider', 'mathMax'
  UNION ALL SELECT 690002004, 'math.min', 'formulaBuiltinFunctionProvider', 'mathMin'
  UNION ALL SELECT 690002012, 'math.pow', 'formulaBuiltinFunctionProvider', 'mathPow'
  UNION ALL SELECT 690002013, 'math.sqrt', 'formulaBuiltinFunctionProvider', 'mathSqrt'
  UNION ALL SELECT 690002005, 'string.length', 'formulaBuiltinFunctionProvider', 'stringLength'
  UNION ALL SELECT 690002006, 'string.contains', 'formulaBuiltinFunctionProvider', 'stringContains'
  UNION ALL SELECT 690002007, 'string.startsWith', 'formulaBuiltinFunctionProvider', 'stringStartsWith'
  UNION ALL SELECT 690002008, 'string.endsWith', 'formulaBuiltinFunctionProvider', 'stringEndsWith'
  UNION ALL SELECT 690002014, 'string.indexOf', 'formulaBuiltinFunctionProvider', 'stringIndexOf'
  UNION ALL SELECT 690002015, 'string.replace', 'formulaBuiltinFunctionProvider', 'stringReplace'
  UNION ALL SELECT 690002016, 'seq.list', 'formulaBuiltinFunctionProvider', 'seqList'
  UNION ALL SELECT 690002017, 'seq.set', 'formulaBuiltinFunctionProvider', 'seqSet'
  UNION ALL SELECT 690002018, 'seq.map', 'formulaBuiltinFunctionProvider', 'seqMap'
  UNION ALL SELECT 690002009, 'date_to_string', 'formulaBuiltinFunctionProvider', 'dateToString'
) seed
JOIN `ai_formula_function` f
  ON f.tenant_id = 1
 AND f.function_code = seed.function_code
WHERE NOT EXISTS (
  SELECT 1 FROM `ai_formula_function_version` existing
  WHERE existing.tenant_id = 1
    AND existing.function_code = seed.function_code
    AND existing.version = '1.0.0'
);

INSERT INTO `ai_formula_function_install` (
  id, tenant_id, function_code, installed_version, install_status, enabled, source_type,
  installed_by, installed_time, create_by, create_time, create_dept, update_by, update_time
)
SELECT seed.id, 1, seed.function_code, '1.0.0', 'INSTALLED', 1, 'BUILTIN',
       NULL, NOW(), NULL, NOW(), NULL, NULL, NOW()
FROM (
  SELECT 690003001 AS id, 'math.abs' AS function_code
  UNION ALL SELECT 690003002, 'math.round'
  UNION ALL SELECT 690003010, 'math.floor'
  UNION ALL SELECT 690003011, 'math.ceil'
  UNION ALL SELECT 690003003, 'math.max'
  UNION ALL SELECT 690003004, 'math.min'
  UNION ALL SELECT 690003012, 'math.pow'
  UNION ALL SELECT 690003013, 'math.sqrt'
  UNION ALL SELECT 690003005, 'string.length'
  UNION ALL SELECT 690003006, 'string.contains'
  UNION ALL SELECT 690003007, 'string.startsWith'
  UNION ALL SELECT 690003008, 'string.endsWith'
  UNION ALL SELECT 690003014, 'string.indexOf'
  UNION ALL SELECT 690003015, 'string.replace'
  UNION ALL SELECT 690003016, 'seq.list'
  UNION ALL SELECT 690003017, 'seq.set'
  UNION ALL SELECT 690003018, 'seq.map'
  UNION ALL SELECT 690003009, 'date_to_string'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM `ai_formula_function_install` existing
  WHERE existing.tenant_id = 1
    AND existing.function_code = seed.function_code
);
