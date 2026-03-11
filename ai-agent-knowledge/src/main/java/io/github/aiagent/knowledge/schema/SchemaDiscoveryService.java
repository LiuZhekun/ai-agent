package io.github.aiagent.knowledge.schema;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * L1 Schema 自动发现服务 —— 从 MySQL information_schema 拉取表/字段元数据。
 * <p>
 * 发现的 Schema 信息有两个用途：
 * <ol>
 *   <li>经 {@link SchemaPromptGenerator} 转换为自然语言后注入 Prompt，
 *       帮助 LLM 理解数据库结构</li>
 *   <li>供 SQL 安全校验组件（如表白名单）复用，避免重复查询</li>
 * </ol>
 * <p>
 * 内部使用 {@link java.util.concurrent.atomic.AtomicReference} 做线程安全缓存，
 * 首次调用 {@link #getCachedOrLoad()} 时自动触发 {@link #discover()} 扫描，
 * 后续直接返回缓存。可通过再次调用 {@code discover()} 手动刷新。
 *
 * @see SchemaPromptGenerator
 * @see TableSchema
 */
@Component
public class SchemaDiscoveryService {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicReference<List<TableSchema>> cache = new AtomicReference<>(List.of());

    public SchemaDiscoveryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 实时扫描当前数据库的表结构并刷新内存缓存。
     * <p>
     * 通过 {@code information_schema.tables} 和 {@code information_schema.columns}
     * 两次查询获取所有表的名称、注释、列名、列类型和列注释。
     *
     * @return 当前数据库所有表的 Schema 列表
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
     * 优先返回缓存的 Schema，缓存为空时自动触发一次 {@link #discover()} 扫描。
     *
     * @return 缓存或新扫描的 Schema 列表，不为 null
     */
    public List<TableSchema> getCachedOrLoad() {
        List<TableSchema> current = cache.get();
        return current.isEmpty() ? discover() : current;
    }
}
