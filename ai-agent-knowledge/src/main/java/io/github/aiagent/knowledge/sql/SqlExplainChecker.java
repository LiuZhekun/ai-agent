package io.github.aiagent.knowledge.sql;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * EXPLAIN 风险检查器 —— {@link SafeSqlQueryTool} 多层防御的第二道关卡。
 * <p>
 * 在实际执行 SQL 之前，先通过 {@code EXPLAIN} 获取查询计划，
 * 检查预估扫描行数是否超过阈值 {@link SqlQueryProperties#getMaxScanRows()}。
 * 当任一步骤的扫描行数超限时拒绝执行，防止全表扫描拖垮数据库。
 * <p>
 * 可通过 {@link SqlQueryProperties#isExplainCheck()} 配置项全局关闭此检查。
 *
 * @see SafeSqlQueryTool
 * @see SqlQueryProperties
 */
@Component
public class SqlExplainChecker {

    private final JdbcTemplate jdbcTemplate;
    private final SqlQueryProperties properties;

    public SqlExplainChecker(JdbcTemplate jdbcTemplate, SqlQueryProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /**
     * 对 SQL 执行 EXPLAIN 检查，判断扫描行数是否在安全范围内。
     *
     * @param sql 待检查的 SQL 语句
     * @return {@code true} 表示通过检查可以执行，{@code false} 表示扫描行数过大
     */
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
