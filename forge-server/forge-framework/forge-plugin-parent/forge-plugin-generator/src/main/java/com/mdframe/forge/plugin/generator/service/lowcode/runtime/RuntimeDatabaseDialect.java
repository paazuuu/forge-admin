package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

/**
 * 低代码运行时数据库方言。
 */
public interface RuntimeDatabaseDialect {

    String dbType();

    boolean supports(String dbType);

    String quote(String identifier);

    String tableExistsSql();

    String listColumnsSql();

    String listColumnMetadataSql();

    String listIndexesSql();

    String primaryKeyMetadataSql();

    String paginate(String sql, long offset, long limit);

    default boolean supportsDdl() {
        return true;
    }
}
