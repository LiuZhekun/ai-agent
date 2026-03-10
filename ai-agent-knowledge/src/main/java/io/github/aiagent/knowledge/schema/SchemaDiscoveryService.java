package io.github.aiagent.knowledge.schema;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * L1 Schema 自动发现服务。
 * 从 information_schema 拉取表/字段元数据并缓存，供 Prompt 组装与 SQL 约束复用。
 */
@Component
public class SchemaDiscoveryService {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicReference<List<TableSchema>> cache = new AtomicReference<>(List.of());

    public SchemaDiscoveryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 实时扫描当前数据库结构并刷新内存缓存。
     */
    public List<TableSchema> discover() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT table_name, table_comment FROM information_schema.tables WHERE table_schema = DATABASE()");
        List<TableSchema> result = new ArrayList<>();
        for (Map<String, Object> table : tables) {
            TableSchema schema = new TableSchema();
            schema.setTableName(String.valueOf(table.get("table_name")));
            schema.setComment(String.valueOf(table.get("table_comment")));
            List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                    "SELECT column_name, column_type, column_comment FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?",
                    schema.getTableName());
            for (Map<String, Object> col : cols) {
                TableSchema.ColumnSchema c = new TableSchema.ColumnSchema();
                c.setName(String.valueOf(col.get("column_name")));
                c.setType(String.valueOf(col.get("column_type")));
                c.setComment(String.valueOf(col.get("column_comment")));
                schema.getColumns().add(c);
            }
            result.add(schema);
        }
        cache.set(result);
        return result;
    }

    /**
     * 优先返回缓存，缓存为空时自动触发一次 discover。
     */
    public List<TableSchema> getCachedOrLoad() {
        List<TableSchema> current = cache.get();
        return current.isEmpty() ? discover() : current;
    }
}
