package com.mdframe.forge.business.core.datasource;

import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * forge-business 当前业务数据源 JDBC 模板提供器。
 */
@Component
@RequiredArgsConstructor
public class BusinessJdbcTemplateProvider {

    private final BusinessTenantDataSourceResolver dataSourceResolver;
    private final RuntimeJdbcTemplateProvider runtimeJdbcTemplateProvider;

    public JdbcTemplate jdbcTemplate() {
        return jdbcTemplate(dataSourceResolver.resolve());
    }

    public JdbcTemplate jdbcTemplate(LowcodeRuntimeDataSourceContext context) {
        return runtimeJdbcTemplateProvider.jdbcTemplate(context);
    }

    public NamedParameterJdbcTemplate namedJdbcTemplate() {
        return namedJdbcTemplate(dataSourceResolver.resolve());
    }

    public NamedParameterJdbcTemplate namedJdbcTemplate(LowcodeRuntimeDataSourceContext context) {
        return runtimeJdbcTemplateProvider.namedJdbcTemplate(context);
    }
}
