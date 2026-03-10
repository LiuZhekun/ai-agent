package io.github.aiagent.knowledge.sql;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * EXPLAIN 风险检查器。
 */
@Component
public class SqlExplainChecker {

    private final JdbcTemplate jdbcTemplate;
    private final SqlQueryProperties properties;

    public SqlExplainChecker(JdbcTemplate jdbcTemplate, SqlQueryProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public boolean check(String sql) {
        if (!properties.isExplainCheck()) {
            return true;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("EXPLAIN " + sql);
        for (Map<String, Object> row : rows) {
            Object scanned = row.get("rows");
            if (scanned instanceof Number num && num.longValue() > properties.getMaxScanRows()) {
                return false;
            }
        }
        return true;
    }
}
