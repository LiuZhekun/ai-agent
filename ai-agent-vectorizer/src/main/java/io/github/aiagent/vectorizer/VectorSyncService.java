package io.github.aiagent.vectorizer;

import io.github.aiagent.vectorizer.annotation.VectorIndexed;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Instant;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 向量同步服务。
 * <p>
 * 新手阅读建议：
 * 1) 从 syncAll/syncIncremental 看入口；
 * 2) 再看 queryRows + persistAsVectors 了解数据流；
 * 3) 最后看反射兼容方法（buildDocument/invokeVectorStoreAdd）。
 */
@Component
public class VectorSyncService {

    private static final Logger log = LoggerFactory.getLogger(VectorSyncService.class);
    private static final String DEFAULT_ID_COLUMN = "id";
    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{([a-zA-Z0-9_]+)}");
    private static final List<String> UPDATE_TIME_CANDIDATES = List.of(
            "updated_at", "update_time", "gmt_modified", "modified_at", "updatedAt", "updateTime");

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;

    public VectorSyncService(VectorStore vectorStore, JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 全量同步：把实体对应表中的文本字段全部写入向量库。
     */
    public void syncAll(Class<?> entityClass) {
        VectorIndexed config = entityClass.getAnnotation(VectorIndexed.class);
        if (config == null) {
            log.debug("Skip vector sync, @VectorIndexed missing for {}", entityClass.getName());
            return;
        }
        String table = toSnakeCase(entityClass.getSimpleName());
        TableSchema schema = loadSchema(table);
        String idColumn = resolveColumn(schema.columns, DEFAULT_ID_COLUMN);
        if (idColumn == null) {
            throw new IllegalStateException("Missing id column for table: " + table);
        }
        List<String> reflected = filterByEntityReflection(entityClass, config.fields());
        List<String> textColumns = resolveTextColumns(schema.columns, reflected.toArray(String[]::new));
        if (textColumns.isEmpty()) {
            log.warn("No vector text columns found for table {}, fields={}", table, Arrays.toString(config.fields()));
            return;
        }
        List<Map<String, Object>> rows = queryRows(table, idColumn, textColumns, null, null);
        persistAsVectors(entityClass, table, config.collection(), idColumn, textColumns, config.textTemplate(), rows);
    }

    /**
     * 增量同步：优先按更新时间字段过滤，缺失条件时自动退化为全量同步。
     */
    public void syncIncremental(Class<?> entityClass, Instant since) {
        VectorIndexed config = entityClass.getAnnotation(VectorIndexed.class);
        if (config == null) {
            log.debug("Skip incremental sync, @VectorIndexed missing for {}", entityClass.getName());
            return;
        }
        String table = toSnakeCase(entityClass.getSimpleName());
        TableSchema schema = loadSchema(table);
        String idColumn = resolveColumn(schema.columns, DEFAULT_ID_COLUMN);
        if (idColumn == null) {
            throw new IllegalStateException("Missing id column for table: " + table);
        }
        List<String> reflected = filterByEntityReflection(entityClass, config.fields());
        List<String> textColumns = resolveTextColumns(schema.columns, reflected.toArray(String[]::new));
        if (textColumns.isEmpty()) {
            log.warn("No vector text columns found for table {}, fields={}", table, Arrays.toString(config.fields()));
            return;
        }
        String updateColumn = resolveUpdateColumn(schema.columns);
        if (updateColumn == null || since == null) {
            log.info("Fallback to full sync for table {} (update column or since missing)", table);
            List<Map<String, Object>> rows = queryRows(table, idColumn, textColumns, null, null);
            persistAsVectors(entityClass, table, config.collection(), idColumn, textColumns, config.textTemplate(), rows);
            return;
        }
        List<Map<String, Object>> rows = queryRows(table, idColumn, textColumns, updateColumn, since);
        persistAsVectors(entityClass, table, config.collection(), idColumn, textColumns, config.textTemplate(), rows);
    }

    private TableSchema loadSchema(String table) {
        List<Map<String, Object>> sample = jdbcTemplate.queryForList("SELECT * FROM " + table + " LIMIT 1");
        List<String> columns = new ArrayList<>();
        if (!sample.isEmpty()) {
            columns.addAll(sample.get(0).keySet());
        }
        if (columns.isEmpty()) {
            columns = jdbcTemplate.query("SELECT * FROM " + table + " LIMIT 1",
                    rs -> {
                        int count = rs.getMetaData().getColumnCount();
                        List<String> names = new ArrayList<>(count);
                        for (int i = 1; i <= count; i++) {
                            names.add(rs.getMetaData().getColumnLabel(i));
                        }
                        return names;
                    });
        }
        return new TableSchema(columns);
    }

    private List<String> filterByEntityReflection(Class<?> entityClass, String[] configuredFields) {
        Set<String> entityFields = new LinkedHashSet<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                entityFields.add(f.getName().toLowerCase(Locale.ROOT));
            }
            current = current.getSuperclass();
        }
        List<String> accepted = new ArrayList<>();
        for (String field : configuredFields) {
            if (field != null && entityFields.contains(field.toLowerCase(Locale.ROOT))) {
                accepted.add(field);
            } else {
                log.debug("Skip non-reflective vector field {} on {}", field, entityClass.getName());
            }
        }
        return accepted;
    }

    private List<String> resolveTextColumns(List<String> columns, String[] requestedFields) {
        Set<String> selected = new LinkedHashSet<>();
        for (String requested : requestedFields) {
            String found = resolveColumn(columns, requested);
            if (found != null) {
                selected.add(found);
            }
        }
        return new ArrayList<>(selected);
    }

    private String resolveUpdateColumn(List<String> columns) {
        for (String c : UPDATE_TIME_CANDIDATES) {
            String found = resolveColumn(columns, c);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private String resolveColumn(List<String> columns, String target) {
        if (target == null || target.isBlank()) {
            return null;
        }
        for (String column : columns) {
            if (column.equals(target) || column.equalsIgnoreCase(target)) {
                return column;
            }
        }
        return null;
    }

    private List<Map<String, Object>> queryRows(
            String table,
            String idColumn,
            List<String> textColumns,
            String updateColumn,
            Instant since) {
        List<String> selected = new ArrayList<>();
        selected.add(idColumn);
        selected.addAll(textColumns);
        if (updateColumn != null) {
            selected.add(updateColumn);
        }
        String columnClause = String.join(", ", selected);
        String sql = "SELECT " + columnClause + " FROM " + table;
        if (updateColumn != null && since != null) {
            sql += " WHERE " + updateColumn + " >= ?";
            return jdbcTemplate.queryForList(sql, since);
        }
        return jdbcTemplate.queryForList(sql);
    }

    private void persistAsVectors(
            Class<?> entityClass,
            String table,
            String collection,
            String idColumn,
            List<String> textColumns,
            String textTemplate,
            List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            log.info("No rows to sync for table {}", table);
            return;
        }
        List<Object> docs = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalized = normalizeRow(row);
            String content = renderContent(normalized, textColumns, textTemplate);
            if (content.isBlank()) {
                continue;
            }
            // 预计算 embedding：用于提前暴露模型能力问题，实际持久化仍由 VectorStore 接管。
            ensureEmbedding(content);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("entityClass", entityClass.getName());
            metadata.put("table", table);
            metadata.put("collection", collection);
            metadata.put("rowId", String.valueOf(normalized.get(idColumn.toLowerCase(Locale.ROOT))));
            docs.add(buildDocument(
                    String.valueOf(normalized.get(idColumn.toLowerCase(Locale.ROOT))),
                    content,
                    metadata));
        }
        if (docs.isEmpty()) {
            log.info("No valid documents generated for table {}", table);
            return;
        }
        invokeVectorStoreAdd(docs);
        log.info("Vector sync finished for table {}, documents={}", table, docs.size());
    }

    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return normalized;
    }

    private String renderContent(Map<String, Object> row, List<String> textColumns, String textTemplate) {
        if (textTemplate != null && !textTemplate.isBlank()) {
            Matcher matcher = TEMPLATE_TOKEN.matcher(textTemplate);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String key = matcher.group(1).toLowerCase(Locale.ROOT);
                Object value = row.get(key);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
            }
            matcher.appendTail(sb);
            return sb.toString().trim();
        }
        StringBuilder builder = new StringBuilder();
        for (String column : textColumns) {
            Object value = row.get(column.toLowerCase(Locale.ROOT));
            if (value == null) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(column).append(": ").append(value);
        }
        return builder.toString().trim();
    }

    private void ensureEmbedding(String content) {
        try {
            Method embedString = embeddingModel.getClass().getMethod("embed", String.class);
            embedString.invoke(embeddingModel, content);
            return;
        } catch (Exception ignored) {
            // 兼容不同 Spring AI 版本的 EmbeddingModel API。
        }
        try {
            Method embedBatch = embeddingModel.getClass().getMethod("embed", List.class);
            embedBatch.invoke(embeddingModel, List.of(content));
        } catch (Exception ex) {
            log.debug("Embedding pre-check skipped: {}", ex.getMessage());
        }
    }

    private Object buildDocument(String id, String content, Map<String, Object> metadata) {
        try {
            Class<?> docClass = Class.forName("org.springframework.ai.document.Document");
            try {
                Constructor<?> ctor = docClass.getConstructor(String.class, String.class, Map.class);
                return ctor.newInstance(id, content, metadata);
            } catch (NoSuchMethodException ignored) {
                // 老版本无此构造方法，继续尝试其他构造方式。
            }
            try {
                Constructor<?> ctor = docClass.getConstructor(String.class, Map.class);
                return ctor.newInstance(content, metadata);
            } catch (NoSuchMethodException ignored) {
                // 老版本无此构造方法，继续尝试 builder。
            }
            Object builder = docClass.getMethod("builder").invoke(null);
            invokeIfExists(builder, "id", id);
            invokeIfExists(builder, "text", content);
            invokeIfExists(builder, "metadata", metadata);
            Method build = builder.getClass().getMethod("build");
            return build.invoke(builder);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to construct Spring AI Document", ex);
        }
    }

    private void invokeIfExists(Object target, String method, Object arg) {
        try {
            for (Method m : target.getClass().getMethods()) {
                if (!m.getName().equals(method) || m.getParameterCount() != 1) {
                    continue;
                }
                Class<?> paramType = m.getParameterTypes()[0];
                if (arg == null || paramType.isAssignableFrom(arg.getClass())) {
                    m.invoke(target, arg);
                    return;
                }
            }
        } catch (Exception ignored) {
            // builder method may not exist in some versions.
        }
    }

    @SuppressWarnings("unchecked")
    private void invokeVectorStoreAdd(List<Object> docs) {
        try {
            Method add = vectorStore.getClass().getMethod("add", List.class);
            add.invoke(vectorStore, docs);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorStore.add invocation failed", ex);
        }
    }

    private String toSnakeCase(String name) {
        StringBuilder sb = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(ch));
        }
        return sb.toString();
    }

    private static class TableSchema {
        private final List<String> columns;

        private TableSchema(List<String> columns) {
            this.columns = columns;
        }
    }
}
