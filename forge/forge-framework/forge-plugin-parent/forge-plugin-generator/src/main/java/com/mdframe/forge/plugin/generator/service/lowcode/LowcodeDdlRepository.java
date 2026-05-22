package com.mdframe.forge.plugin.generator.service.lowcode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 低代码受控 DDL 仓储。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LowcodeDdlRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    public Set<String> listColumns(String tableName) {
        List<String> columns = jdbcTemplate.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, String.class, tableName);
        return new HashSet<>(columns);
    }

    public Set<String> listIndexes(String tableName) {
        List<String> indexes = jdbcTemplate.queryForList("""
                SELECT DISTINCT index_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, String.class, tableName);
        return new HashSet<>(indexes);
    }

    public boolean hasAutoIncrementPrimaryId(String tableName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT data_type, column_type, column_key, extra
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = 'id'
                """, tableName);
        if (rows.isEmpty()) {
            return false;
        }
        Map<String, Object> row = rows.get(0);
        String dataType = text(row.get("data_type")).toLowerCase(Locale.ROOT);
        String columnType = text(row.get("column_type")).toLowerCase(Locale.ROOT);
        String columnKey = text(row.get("column_key")).toUpperCase(Locale.ROOT);
        String extra = text(row.get("extra")).toLowerCase(Locale.ROOT);
        return "bigint".equals(dataType)
                && columnType.contains("bigint")
                && "PRI".equals(columnKey)
                && extra.contains("auto_increment");
    }

    public void executeDdl(String ddl) {
        log.info("[LowcodeDdlRepository] 执行低代码受控DDL: {}", ddl);
        jdbcTemplate.execute(ddl);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
