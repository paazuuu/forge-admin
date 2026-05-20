package com.mdframe.forge.plugin.generator.service.lowcode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
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

    public void executeDdl(String ddl) {
        log.info("[LowcodeDdlRepository] 执行低代码受控DDL: {}", ddl);
        jdbcTemplate.execute(ddl);
    }
}
