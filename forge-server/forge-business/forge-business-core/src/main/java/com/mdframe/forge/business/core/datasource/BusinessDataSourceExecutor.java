package com.mdframe.forge.business.core.datasource;

import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * forge-business 业务数据源作用域执行器。
 */
@Component
@RequiredArgsConstructor
public class BusinessDataSourceExecutor {

    private final BusinessTenantDataSourceResolver dataSourceResolver;

    public void runWithTenantDefault(Runnable action) {
        executeWithTenantDefault(() -> {
            action.run();
            return null;
        });
    }

    public <T> T executeWithTenantDefault(Supplier<T> action) {
        LowcodeRuntimeDataSourceContext context = dataSourceResolver.resolve();
        try (BusinessDataSourceContextHolder.Scope ignored = dataSourceResolver.use(context)) {
            return action.get();
        }
    }

    public <T> T executeWithContext(LowcodeRuntimeDataSourceContext context, Supplier<T> action) {
        try (BusinessDataSourceContextHolder.Scope ignored = dataSourceResolver.use(context)) {
            return action.get();
        }
    }
}
