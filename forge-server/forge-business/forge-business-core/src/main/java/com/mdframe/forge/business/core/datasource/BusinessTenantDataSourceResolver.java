package com.mdframe.forge.business.core.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * forge-business 租户默认业务数据源解析器。
 */
@Component
@RequiredArgsConstructor
public class BusinessTenantDataSourceResolver {

    private static final String DEFAULT_DB_TYPE = "MySQL";

    private final SysTenantMapper tenantMapper;
    private final GenDatasourceMapper datasourceMapper;

    public LowcodeRuntimeDataSourceContext resolve() {
        LowcodeRuntimeDataSourceContext explicitContext = BusinessDataSourceContextHolder.get();
        if (explicitContext != null) {
            return explicitContext;
        }
        Long tenantId = resolveTenantId();
        if (tenantId == null) {
            return master();
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || (tenant.getDefaultBusinessDatasourceId() == null
                && StringUtils.isBlank(tenant.getDefaultBusinessDatasourceCode()))) {
            return master();
        }
        GenDatasource datasource = findDatasource(tenant);
        if (datasource == null) {
            throw new BusinessException("租户默认业务数据源不存在");
        }
        if (Integer.valueOf(0).equals(datasource.getIsEnabled())) {
            throw new BusinessException("租户默认业务数据源已禁用: " + datasource.getDatasourceName());
        }
        return fromDatasource(datasource);
    }

    public BusinessDataSourceContextHolder.Scope use(LowcodeRuntimeDataSourceContext context) {
        return BusinessDataSourceContextHolder.use(context == null ? master() : context);
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return SessionHelper.getTenantId();
    }

    private GenDatasource findDatasource(SysTenant tenant) {
        if (tenant.getDefaultBusinessDatasourceId() != null) {
            return datasourceMapper.selectById(tenant.getDefaultBusinessDatasourceId());
        }
        return datasourceMapper.selectOne(new LambdaQueryWrapper<GenDatasource>()
                .eq(GenDatasource::getDatasourceCode, tenant.getDefaultBusinessDatasourceCode())
                .last("LIMIT 1"));
    }

    private LowcodeRuntimeDataSourceContext master() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContext.master(null);
        context.setTableMode("BUSINESS");
        return context;
    }

    private LowcodeRuntimeDataSourceContext fromDatasource(GenDatasource datasource) {
        LowcodeRuntimeDataSourceContext context = new LowcodeRuntimeDataSourceContext();
        context.setMaster(false);
        context.setDatasourceId(datasource.getDatasourceId());
        context.setDatasourceCode(datasource.getDatasourceCode());
        context.setDatasourceName(datasource.getDatasourceName());
        context.setDbType(StringUtils.defaultIfBlank(datasource.getDbType(), DEFAULT_DB_TYPE));
        context.setTableMode("BUSINESS");
        context.setReadonly(Integer.valueOf(1).equals(datasource.getReadonly()));
        context.setAllowWrite(!context.isReadonly() && Integer.valueOf(1).equals(datasource.getAllowRuntimeWrite()));
        context.setAllowDdl(false);
        context.setRiskLevel(datasource.getRiskLevel());
        return context;
    }
}
