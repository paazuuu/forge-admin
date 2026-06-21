package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Oracle 运行时方言。
 */
@Component
public class OracleRuntimeDatabaseDialect implements RuntimeDatabaseDialect {

    @Override
    public String dbType() {
        return "Oracle";
    }

    @Override
    public boolean supports(String dbType) {
        return StringUtils.equalsAnyIgnoreCase(dbType, "oracle", "oracle12c");
    }

    @Override
    public String quote(String identifier) {
        return "\"" + identifier.toUpperCase() + "\"";
    }

    @Override
    public String tableExistsSql() {
        return """
            SELECT COUNT(1)
            FROM user_tables
            WHERE table_name = UPPER(?)
            """;
    }

    @Override
    public String listColumnsSql() {
        return """
            SELECT column_name
            FROM user_tab_columns
            WHERE table_name = UPPER(?)
            ORDER BY column_id
            """;
    }

    @Override
    public String listColumnMetadataSql() {
        return """
            SELECT column_name,
                   data_type AS column_type,
                   nullable AS is_nullable,
                   data_default AS column_default,
                   identity_column AS extra,
                   '' AS column_comment,
                   '' AS generation_expression
            FROM user_tab_columns
            WHERE table_name = UPPER(?)
            ORDER BY column_id
            """;
    }

    @Override
    public String listIndexesSql() {
        return """
            SELECT index_name
            FROM user_indexes
            WHERE table_name = UPPER(?)
            """;
    }

    @Override
    public String primaryKeyMetadataSql() {
        return """
            SELECT cols.column_name, tab_cols.data_type, tab_cols.identity_column AS extra
            FROM user_constraints cons
            JOIN user_cons_columns cols
              ON cons.constraint_name = cols.constraint_name
            JOIN user_tab_columns tab_cols
              ON tab_cols.table_name = cols.table_name
             AND tab_cols.column_name = cols.column_name
            WHERE cons.constraint_type = 'P'
              AND cons.table_name = UPPER(?)
            ORDER BY cols.position
            """;
    }

    @Override
    public String paginate(String sql, long offset, long limit) {
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }
}
