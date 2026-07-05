-- 补充低代码业务系统生成 Agent 的单领域和表名前缀规则。

INSERT INTO ai_context_config (tenant_id, agent_code, config_name, config_content, config_type, sort, status,
                               create_by, create_time, create_dept, update_by, update_time)
SELECT 1,
       'lowcode_system_generator',
       '低代码单领域和命名规则',
       '## 单领域和命名规则
- 当用户描述中出现“一个业务领域”“同一个业务领域”“不要分开”“不要拆分”等表达时，必须只输出一个业务领域。
- 单领域场景下，多个业务对象可以生成多个模型和应用，但 domains 数组只能有一个元素，所有 modelSchema.domain 和 app.domainCode 必须一致。
- 当用户指定表名前缀时，例如“表名以 tf_f_ 开头”，所有 modelSchema.tableName 必须使用该前缀。
- 商品基本信息和商品分类管理属于同一商品管理业务能力时，不要拆分成供应链和运营管理两个领域。
- 如果当前领域已有数据模型上下文，优先复用已有模型生成应用页面；只有已有模型不能覆盖需求时才补充新模型。',
       'RULE',
       16,
       '0',
       1,
       NOW(),
       1,
       1,
       NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_context_config
    WHERE tenant_id = 1
      AND agent_code = 'lowcode_system_generator'
      AND config_name = '低代码单领域和命名规则'
);
