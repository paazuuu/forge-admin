package com.mdframe.forge.business.core.datasource;

import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;

/**
 * forge-business 显式业务数据源上下文。
 */
public final class BusinessDataSourceContextHolder {

    private static final ThreadLocal<LowcodeRuntimeDataSourceContext> CONTEXT = new ThreadLocal<>();

    private BusinessDataSourceContextHolder() {
    }

    public static LowcodeRuntimeDataSourceContext get() {
        return CONTEXT.get();
    }

    public static Scope use(LowcodeRuntimeDataSourceContext context) {
        LowcodeRuntimeDataSourceContext previous = CONTEXT.get();
        CONTEXT.set(context);
        return () -> {
            if (previous == null) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(previous);
            }
        };
    }

    public interface Scope extends AutoCloseable {

        @Override
        void close();
    }
}
