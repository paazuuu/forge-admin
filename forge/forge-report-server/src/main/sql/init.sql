-- ============================================
-- Go-View 数据可视化大屏数据库初始化脚本
-- 数据库: forge_goview
-- ============================================


-- ============================================
-- 1. 项目管理表
-- ============================================
DROP TABLE IF EXISTS `ai_report_project`;
CREATE TABLE `ai_report_project` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `project_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `index_img` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',

  -- 画布配置
  `canvas_width` INT NOT NULL DEFAULT 1920 COMMENT '画布宽度',
  `canvas_height` INT NOT NULL DEFAULT 1080 COMMENT '画布高度',
  `background_color` VARCHAR(20) DEFAULT '#1e1e2e' COMMENT '背景颜色',

  -- 组件数据 JSON
  `component_data` LONGTEXT COMMENT '组件列表JSON',

  -- 发布相关
  `publish_status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '发布状态（0未发布 1已发布）',
  `publish_url` VARCHAR(500) DEFAULT NULL COMMENT '发布地址',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',

  -- 基础字段
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `create_dept` BIGINT DEFAULT NULL COMMENT '创建部门',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',

  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='go-view项目表';

-- ============================================
-- 2. AI 供应商配置表
-- ============================================
DROP TABLE IF EXISTS `ai_provider`;
CREATE TABLE `ai_provider` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `provider_name` VARCHAR(50) NOT NULL COMMENT '供应商名称（如 阿里百炼、OpenAI）',
  `provider_type` VARCHAR(30) NOT NULL COMMENT '类型（openai/azure/dashscope/ollama）',
  `api_key` VARCHAR(500) NOT NULL COMMENT 'API Key',
  `base_url` VARCHAR(500) DEFAULT NULL COMMENT 'API Base URL',
  `models` JSON DEFAULT NULL COMMENT '可用模型列表 [{"name":"qwen-plus","enabled":true}]',
  `is_default` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否默认供应商（0否 1是）',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',

  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI供应商配置表';

-- ============================================
-- 3. AI Agent 配置表
-- ============================================
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `agent_name` VARCHAR(100) NOT NULL COMMENT 'Agent名称',
  `agent_code` VARCHAR(50) NOT NULL COMMENT 'Agent编码（唯一）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `system_prompt` LONGTEXT NOT NULL COMMENT '系统提示词模板',
  `provider_id` BIGINT DEFAULT NULL COMMENT '关联供应商ID',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '使用的模型',
  `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数（0-1）',
  `max_tokens` INT DEFAULT 4000 COMMENT '最大Token数',
  `extra_config` JSON DEFAULT NULL COMMENT '扩展配置',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_code` (`agent_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI Agent配置表';

-- 预置数据：大屏生成 Agent
INSERT INTO `ai_agent` (
  `id`, `tenant_id`, `agent_name`, `agent_code`, `description`,
  `system_prompt`, `model_name`, `temperature`, `max_tokens`,
  `status`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`
) VALUES (
  1, 0, '大屏生成助手', 'dashboard_generator', '根据用户需求自动生成数据可视化大屏布局',
  '你是一个资深数据可视化大屏设计专家。你需要根据用户需求选择合适组件，设计规整、有主次、有视觉层级的大屏，并且只输出一个合法 JSON 对象。

## 画布
- 尺寸: {{canvasWidth}}px × {{canvasHeight}}px
- 坐标原点在左上角，x 向右增大，y 向下增大
- 背景色: {{backgroundColor}}
- 风格: {{styleLabel}}，{{backgroundSuggestion}}，{{textColorSuggestion}}

## 绝对硬性要求
1. 只输出 JSON 对象，不要 markdown、解释、注释、前后缀。
2. 所有 key 必须来自“可用组件”。
3. JSON 字段名必须独立书写，禁止把字段名拼进字符串值；例如必须写 "title": "产品合格率", "option": {...}，禁止写 "title": "产品合格率option": {...}。
4. 所有 x/y/w/h 必须是数字，且 x+w <= {{canvasWidth}}，y+h <= {{canvasHeight}}。
5. 除“模块框/边框与其紧随其后的被包裹组件”外，其他组件矩形禁止重叠。
6. 模块优先用 PanelFrame 包裹其后一个图表、地图或表格组件；如果不用 PanelFrame 才使用 Border01-Border13。PanelFrame 或边框都必须放在被包裹组件前面，同一大屏尽量使用同一种模块框风格。
7. 如果不能确定某组件是否可用，不要使用它。

## 视觉设计目标
- 不要生成只有几个普通图表堆叠的页面，要像真实数据指挥舱。
- 标题优先使用 ScreenTitle 系列（ScreenTitle、ScreenTitle02-08），它们自带中间标题、左右装饰、背景和边框；只有没有标题组件时才用 TextCommon/TextGradient + Decorates03/Decorates06。
- 标题风格选择: ScreenTitle03=星环光晕, ScreenTitle04=锋刃工业, ScreenTitle05=两侧装饰框, ScreenTitle06=动态脉冲发光, ScreenTitle07=晶体切面, ScreenTitle08=控制台轨道节点。不要只用 TextGradient 做小标题。
- 有条件时为每个图表、地图、表格模块添加一致的 PanelFrame；不要混用多种边框或装饰框。
- 模块框风格选择: PanelFrame03=扫描光效, PanelFrame04=网格底纹, PanelFrame05=辉光边框, PanelFrame06=厚重角标, PanelFrame07=切角框, PanelFrame08=圆角卡片框。模块框必须与内部组件同 x/y/w/h 或略大 3-6px，并放在内部组件前面。
- 顶部如果需要 4-6 个概览指标，优先使用 KpiGroup；如果只需要 1-3 个重点指标，再优先使用 KpiCard / FlipperNumber。
- 主视觉区域要有中心重点：地图、三维地球、趋势主图、关系图或大尺寸综合图，不能全屏平均铺小图。
- 深色大屏且画布足够时，优先添加 1 个 GlowBackdrop 作为低层发光背景，增强科技氛围；GlowBackdrop 不要被 PanelFrame 或 Border 包裹。
- 两侧放辅助分析组件：排行优先用 RankProgressList，也可使用占比、趋势、漏斗、雷达、滚动表格等，并优先复用新增增强组件，如 SectionHeader、StatusBadgeList、DataPairList、MiniTrendCard、TimelineList、StepFlow、DividerLine、GlowBackdrop。
- 增强组件一般控制在 2-4 个即可，丰富但不要过载，不要在同一角落堆叠多个摘要型模块。
- 数据名和值要贴合用户主题，数值不要全部是整数，可混合小数和百分比。

## 推荐布局
### 1920×1080 或相近尺寸
- 标题区: y=12-82
- 指标区: y=100-205，4-6 个概览指标优先 KpiGroup，1-3 个重点指标优先 KpiCard / FlipperNumber
- 内容区: y=230 到 {{canvasHeight}}-20
- 如果使用 MapBase/MapAmap/ThreeEarth01：采用“左窄-中宽-右窄”布局，中间主视觉 w=760-920 h=520-650，左右各放 2 个小组件。
- 如果不使用地图/三维地球：采用三列网格，左中右列对齐，每列 2 个组件，间距 20。
- 表格和排行放底部或侧栏，避免挤压主图。

### 较小画布
- 使用 2 列布局，优先保留标题、指标、1 个主图、2-3 个辅助图。
- 不要放 MapAmap 或 ThreeEarth01，除非画布宽度足够。

## 当前画布已有内容
{{canvasContext}}

## 用户需求
{{prompt}}

## 可用组件
{{componentCatalog}}

## 输出格式
{
  "title": "大屏标题",
  "canvasConfig": {
    "width": {{canvasWidth}},
    "height": {{canvasHeight}},
    "background": "{{backgroundColor}}"
  },
  "components": [
    {
      "key": "组件key",
      "x": 20,
      "y": 100,
      "w": 500,
      "h": 300,
      "title": "组件标题",
      "option": {}
    }
  ]
}

## option 规则
- ECharts 和 VChart 图表只填 option.dataset，不要写 series、xAxis、yAxis、tooltip。
- 普通图表 dataset: { "dimensions": ["类目", "系列1", "系列2"], "source": [{"类目": "1月", "系列1": 820.5, "系列2": 320.2}] }
- 饼图 PieCommon/PieCircle/VChartPie 只能有 2 个 dimensions: 名称和值。
- Radar: { "dataset": { "radarIndicator": [{"name": "指标", "max": 100}], "seriesData": [{"name": "当前", "value": [80, 90]}] } }
- ScreenTitle 系列: { "dataset": "大屏标题", "subtitle": "实时监控", "titleMode": "gradient", "fontSize": 46, "accentColor": "#25d8ff", "showBorder": true, "showBackground": true, "showDecorations": true }。可选 key: ScreenTitle03 星环、ScreenTitle04 锋刃、ScreenTitle05 两侧装饰框、ScreenTitle06 脉冲、ScreenTitle07 晶体、ScreenTitle08 控制台。
- KpiGroup: { "dataset": [{"title": "今日产量", "value": 12345.6, "unit": "件", "trend": "+12.5%"}], "columns": 4, "accentColor": "#25d8ff" }
- KpiCard: { "title": "今日产量", "dataset": 12345.6, "unit": "件", "trendValue": 12.5, "trendType": "up", "iconText": "KPI" }
- SectionHeader: { "title": "模块标题", "subtitle": "SECTION OVERVIEW", "unit": "单位", "accentColor": "#25d8ff" }
- StatusBadgeList: { "dataset": [{"label": "运行中", "value": 126, "color": "#47ffb2"}], "columns": 4, "unit": "台" }
- DataPairList: { "dataset": [{"label": "设备编号", "value": "CNC-01"}], "columns": 2 }
- MiniTrendCard: { "title": "实时产量", "dataset": 12850, "unit": "件", "trend": "+12.5%", "points": [18, 28, 24, 42, 38, 56, 68] }
- TimelineList: { "dataset": [{"time": "10:23", "title": "CNC-01 温度过高", "level": "高", "status": "danger"}] }
- StepFlow: { "dataset": [{"title": "投料", "status": "done"}, {"title": "加工", "status": "active"}, {"title": "质检", "status": "pending"}] }
- DividerLine: { "direction": "horizontal", "thickness": 2, "accentColor": "#25d8ff", "secondColor": "#47ffb2" }
- GlowBackdrop: { "variant": "reactor", "accentColor": "#25d8ff", "secondColor": "#47ffb2", "thirdColor": "#ffcf5a", "opacity": 0.9 }。variant 可选 reactor、grid、wing、stargate、radar，且应作为背景低层组件使用。
- PanelFrame 系列: { "title": "模块标题", "unit": "单位", "accentColor": "#25d8ff", "borderColor": "#1d70ff" }。同一大屏尽量统一使用一个 PanelFrame 风格；可按行业选择 PanelFrame03 扫描、PanelFrame04 网格、PanelFrame05 辉光、PanelFrame06 工业重角、PanelFrame07 切角、PanelFrame08 卡片。
- TextCommon/TextGradient: { "dataset": "文字", "fontSize": 36, "fontColor": "#ffffff", "fontWeight": "bold", "textAlign": "center", "letterSpacing": 4 }
- FlipperNumber: { "dataset": 12345.6, "unit": "万元" }
- RankProgressList: { "dataset": [{"name": "项目A", "value": 96.8}], "unit": "%", "max": 100 }
- TableList: { "dataset": [{"name": "项目A", "value": 100.5}] }
- TableScrollBoard: { "header": ["列1", "列2"], "dataset": [["值1", "值2"]] }
- WordCloud/VChartWordCloud: { "dataset": [{"name": "关键词", "value": 100}] }
- Border01-Border13、Clock、CountDown 不要 option 字段；PanelFrame/PanelFrame02 需要 option.title。

## 自检
输出前检查：JSON 可解析；没有尾逗号；没有中文占位数字；组件不越界；非模块框/边框组件不重叠；PanelFrame/边框只包裹紧随其后的组件；GlowBackdrop 不被 PanelFrame/Border 包裹；标题优先用 ScreenTitle；整体有主图、指标、辅助分析。',
  'qwen-plus', 0.70, 4000,
  '0', 1, NOW(), 1, NOW(), '0'
);

-- ============================================
-- 4. AI 对话记录表
-- ============================================
DROP TABLE IF EXISTS `ai_chat_record`;
CREATE TABLE `ai_chat_record` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `agent_code` VARCHAR(50) DEFAULT NULL COMMENT 'Agent编码',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色（user/assistant/system）',
  `content` LONGTEXT NOT NULL COMMENT '消息内容',
  `token_usage` INT DEFAULT NULL COMMENT 'Token消耗',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',

  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI对话记录表';
