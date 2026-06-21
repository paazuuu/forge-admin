package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL 运行时方言。
 */
@Component
public class PostgreSqlRuntimeDatabaseDialect implements RuntimeDatabaseDialect {

    @Override
    public String dbType() {
        return "PostgreSQL";
    }

    @Override
    public boolean supports(String dbType) {
        return StringUtils.equalsAnyIgnoreCase(dbType, "postgresql", "postgres");
    }

    @Override
    public String quote(String identifier) {
        return "\"" + identifier + "\"";
    }

    @Override
    public String tableExistsSql() {
        return """
            SELECT COUNT(1)
            FROM information_schema.tables
            WHERE table_schema = current_schema()
              AND table_name = ?
            """;
    }

    @Override
    public String listColumnsSql() {
        return """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = ?
            ORDER BY ordinal_position
            """;
    }

    @Override
    public String listColumnMetadataSql() {
        return """
            SELECT column_name, data_type AS column_type, is_nullable, column_default, '' AS extra,
                   COALESCE(col_description((quote_ident(table_schema) || '.' || quote_ident(table_name))::regclass::oid, ordinal_position), '') AS column_comment,
                   '' AS generation_expression
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = ?
            ORDER BY ordinal_position
            """;
    }

    @Override
    public String listIndexesSql() {
        return """
            SELECT indexname
            FROM pg_indexes
            WHERE schemaname = current_schema()
              AND tablename = ?
            """;
    }

    @Override
    public String primaryKeyMetadataSql() {
        return """
            SELECT kcu.column_name, c.data_type, COALESCE(c.column_default, '') AS extra
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
             AND tc.table_schema = kcu.table_schema
            JOIN information_schema.columns c
              ON c.table_schema = kcu.table_schema
             AND c.table_name = kcu.table_name
             AND c.column_name = kcu.column_name
            WHERE tc.constraint_type = 'PRIMARY KEY'
              AND tc.table_schema = current_schema()
              AND tc.table_name = ?
            ORDER BY kcu.ordinal_position
            """;
    }

    @Override
    public String paginate(String sql, long offset, long limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }
}
