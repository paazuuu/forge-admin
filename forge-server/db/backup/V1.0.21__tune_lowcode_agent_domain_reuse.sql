-- 补充低代码业务系统生成 Agent 的已有领域复用规则。

INSERT INTO ai_context_config (tenant_id, agent_code, config_name, config_content, config_type, sort, status,
                               create_by, create_time, create_dept, update_by, update_time)
SELECT 1,
       'lowcode_system_generator',
       '低代码已有领域复用规则',
       '## 已有业务领域复用规则
- 当用户指定目标业务领域时，必须优先在该已有领域中生成模型和应用。
- 输出 domains[0].existingDomainId 必须等于用户指定的 domainId。
- 输出 domains[0].domainCode、domainName 必须与用户指定领域保持一致。
- 除非用户需求明确要求跨领域拆分，否则不要新建业务领域。
- 模型 modelSchema.domain 和应用 domainCode/domainName 需要与目标领域一致。',
       'RULE',
       15,
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
      AND config_name = '低代码已有领域复用规则'
);
