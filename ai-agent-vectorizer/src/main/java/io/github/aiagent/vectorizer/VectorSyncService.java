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
 * 向量同步服务 —— 将关系型数据库中的业务数据向量化并写入 Milvus。
 * <p>
 * <b>核心数据流：</b>
 * <pre>
 *   MySQL 业务表
 *     ──▶ queryRows()           按 @VectorIndexed 配置的字段从数据库查询数据行
 *     ──▶ renderContent()       根据 textTemplate 或默认规则将行数据拼装为自然语言文本
 *     ──▶ ensureEmbedding()     预调用 EmbeddingModel 验证向量化能力（仅首行）
 *     ──▶ buildDocument()       构造 Spring AI Document 对象（反射兼容多版本）
 *     ──▶ invokeVectorStoreAdd() 分批写入 VectorStore（Milvus）
 * </pre>
 * <p>
 * <b>表名映射：</b>优先读取实体上的 {@code @TableName} 注解值；若无则将类名按驼峰转下划线。
 * <p>
 * <b>增量策略：</b>自动探测表中的更新时间列（{@code update_time}、{@code updated_at} 等），
 * 若探测不到则退化为全量同步。
 *
 * @see VectorIndexed
 * @see VectorSyncScheduler
 */
@Component
public class VectorSyncService {

    private static final Logger log = LoggerFactory.getLogger(VectorSyncService.class);

    /** 默认主键列名，用于定位每条业务数据的唯一标识。 */
    private static final String DEFAULT_ID_COLUMN = "id";

    /** 文本模板中 {fieldName} 占位符的匹配正则。 */
    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

    /**
     * 增量同步时自动探测的更新时间列名候选列表（按优先级排序）。
     * 匹配到第一个存在的列即用于 WHERE 过滤。
     */
    private static final List<String> UPDATE_TIME_CANDIDATES = List.of(
            "updated_at", "update_time", "gmt_modified", "modified_at", "updatedAt", "updateTime");

    /** MyBatis-Plus @TableName 注解全限定名，通过反射读取以避免硬依赖。 */
    private static final String TABLE_NAME_ANNOTATION = "com.baomidou.mybatisplus.annotation.TableName";

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;
    private final VectorSyncProperties syncProperties;

    public VectorSyncService(VectorStore vectorStore,
                             JdbcTemplate jdbcTemplate,
                             EmbeddingModel embeddingModel,
                             VectorSyncProperties syncProperties) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
        this.syncProperties = syncProperties;
    }

    // ==================== 公开同步入口 ====================

    /**
     * 全量同步：将实体对应表中所有行的指定字段向量化后写入 Milvus。
     * <p>
     * 适用于首次接入或数据修复场景。数据量较大时建议在低峰期执行。
     *
     * @param entityClass 标注了 {@link VectorIndexed} 的实体类
     * @throws IllegalStateException 当表缺少 id 列时抛出
     */
    public void syncAll(Class<?> entityClass) {
        VectorIndexed config = entityClass.getAnnotation(VectorIndexed.class);
        if (config == null) {
            log.debug("Skip vector sync, @VectorIndexed missing for {}", entityClass.getName());
            return;
        }
        String table = resolveTableName(entityClass);
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
     * 增量同步：仅同步 {@code since} 之后更新的数据行。
     * <p>
     * 当表中不存在可识别的更新时间列或 {@code since} 为 null 时，自动退化为全量同步并输出 INFO 日志。
     *
     * @param entityClass 标注了 {@link VectorIndexed} 的实体类
     * @param since       增量起始时间（含），通常为上一次同步的时间戳
     */
    public void syncIncremental(Class<?> entityClass, Instant since) {
        VectorIndexed config = entityClass.getAnnotation(VectorIndexed.class);
        if (config == null) {
            log.debug("Skip incremental sync, @VectorIndexed missing for {}", entityClass.getName());
            return;
        }
        String table = resolveTableName(entityClass);
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
            log.info("Fallback to full sync for table {} (updateColumn={}, since={})", table, updateColumn, since);
            List<Map<String, Object>> rows = queryRows(table, idColumn, textColumns, null, null);
            persistAsVectors(entityClass, table, config.collection(), idColumn, textColumns, config.textTemplate(), rows);
            return;
        }
        List<Map<String, Object>> rows = queryRows(table, idColumn, textColumns, updateColumn, since);
        persistAsVectors(entityClass, table, config.collection(), idColumn, textColumns, config.textTemplate(), rows);
    }

    // ==================== 表名解析 ====================

    /**
     * 解析实体类对应的数据库表名。
     * 优先读取 MyBatis-Plus 的 @TableName 注解值，不存在时按类名驼峰转下划线。
     */
    private String resolveTableName(Class<?> entityClass) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends java.lang.annotation.Annotation> tableNameAnno =
                    (Class<? extends java.lang.annotation.Annotation>) Class.forName(TABLE_NAME_ANNOTATION);
            java.lang.annotation.Annotation annotation = entityClass.getAnnotation(tableNameAnno);
            if (annotation != null) {
                Method valueMethod = tableNameAnno.getMethod("value");
                String value = (String) valueMethod.invoke(annotation);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        } catch (ClassNotFoundException ignored) {
            // MyBatis-Plus 不在 classpath 中，跳过
        } catch (Exception ex) {
            log.debug("Failed to read @TableName from {}: {}", entityClass.getName(), ex.getMessage());
        }
        return toSnakeCase(entityClass.getSimpleName());
    }

    // ==================== Schema 与列解析 ====================

    /**
     * 通过采样查询获取表的列名列表。
     * 先尝试从查询结果的 keySet 获取；空表时回退到 ResultSetMetaData。
     */
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

    /**
     * 通过反射校验 @VectorIndexed.fields 中声明的字段是否在实体类中真实存在。
     * 遍历实体类及其父类的所有声明字段，不区分大小写匹配。
     * 不存在的字段会被跳过并输出 DEBUG 日志，防止配置笔误导致静默丢失数据。
     */
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

    /**
     * 将 @VectorIndexed.fields 映射到数据库中实际存在的列名（不区分大小写匹配）。
     */
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

    /**
     * 从候选列表中探测表的更新时间列，用于增量同步 WHERE 过滤。
     *
     * @return 找到的列名，或 null 表示该表无可用的更新时间列
     */
    private String resolveUpdateColumn(List<String> columns) {
        for (String c : UPDATE_TIME_CANDIDATES) {
            String found = resolveColumn(columns, c);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 不区分大小写地在列名列表中查找目标列。
     * 优先精确匹配，其次忽略大小写匹配。
     */
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

    // ==================== 数据查询 ====================

    /**
     * 从业务表中查询需要向量化的数据行。
     * 仅 SELECT 必要的列（id + 文本字段 + 更新时间），减少网络传输和内存开销。
     *
     * @param updateColumn 更新时间列名，为 null 时不加 WHERE 条件（全量查询）
     * @param since        增量起始时间，与 updateColumn 配合使用
     */
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

    // ==================== 向量持久化 ====================

    /**
     * 将数据行转换为 Document 并分批写入 VectorStore。
     * <p>
     * 每条 Document 的 metadata 包含：entityClass、table、collection、rowId，
     * 便于后续按来源过滤或溯源。
     * <p>
     * 写入按 {@link VectorSyncProperties#getBatchSize()} 分批执行，
     * 避免大表一次性写入导致内存溢出或请求超时。
     */
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

        int batchSize = syncProperties.getBatchSize();
        boolean embeddingVerified = false;
        List<Object> batch = new ArrayList<>(batchSize);
        int totalDocs = 0;

        for (Map<String, Object> row : rows) {
            Map<String, Object> normalized = normalizeRow(row);
            String content = renderContent(normalized, textColumns, textTemplate);
            if (content.isBlank()) {
                continue;
            }
            // 仅对首条数据预调用 Embedding，验证模型可用性；后续由 VectorStore 内部处理
            if (!embeddingVerified) {
                ensureEmbedding(content);
                embeddingVerified = true;
            }
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("entityClass", entityClass.getName());
            metadata.put("table", table);
            metadata.put("collection", collection);
            metadata.put("rowId", String.valueOf(normalized.get(idColumn.toLowerCase(Locale.ROOT))));
            batch.add(buildDocument(
                    String.valueOf(normalized.get(idColumn.toLowerCase(Locale.ROOT))),
                    content,
                    metadata));

            if (batch.size() >= batchSize) {
                invokeVectorStoreAdd(batch);
                totalDocs += batch.size();
                log.debug("Vector batch written for table {}, batchDocs={}, totalDocs={}", table, batch.size(), totalDocs);
                batch = new ArrayList<>(batchSize);
            }
        }
        // 写入剩余不满一批的数据
        if (!batch.isEmpty()) {
            invokeVectorStoreAdd(batch);
            totalDocs += batch.size();
        }
        if (totalDocs == 0) {
            log.info("No valid documents generated for table {}", table);
            return;
        }
        log.info("Vector sync finished for table {}, documents={}", table, totalDocs);
    }

    // ==================== 文本处理 ====================

    /**
     * 将数据库行的 key 统一转为小写，消除不同数据库/驱动返回列名大小写不一致的问题。
     */
    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return normalized;
    }

    /**
     * 将一行数据渲染为向量化文本。
     * <ul>
     *   <li>若 textTemplate 非空，按模板替换 {@code {fieldName}} 占位符</li>
     *   <li>否则按 {@code 列名: 值} 格式逐字段拼接，跳过 null 值</li>
     * </ul>
     *
     * @param row         已经过 normalizeRow 处理的小写 key 行数据
     * @param textColumns 参与拼接的列名列表
     * @param textTemplate 文本模板，为空则使用默认拼接
     * @return 拼装后的自然语言文本
     */
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

    // ==================== Embedding 预检 ====================

    /**
     * 预调用 EmbeddingModel 验证向量化能力是否可用。
     * <p>
     * 仅在每次同步任务的第一条数据上执行，目的是在批量写入前提前暴露
     * API Key 失效、模型不可用等问题，避免大量数据处理完成后才发现写入失败。
     * <p>
     * 通过反射调用以兼容不同版本的 Spring AI EmbeddingModel API。
     */
    private void ensureEmbedding(String content) {
        try {
            Method embedString = embeddingModel.getClass().getMethod("embed", String.class);
            embedString.invoke(embeddingModel, content);
            return;
        } catch (Exception ignored) {
        }
        try {
            Method embedBatch = embeddingModel.getClass().getMethod("embed", List.class);
            embedBatch.invoke(embeddingModel, List.of(content));
        } catch (Exception ex) {
            log.debug("Embedding pre-check skipped: {}", ex.getMessage());
        }
    }

    // ==================== Spring AI Document 构造（反射兼容） ====================

    /**
     * 通过反射构造 Spring AI {@code Document} 对象。
     * <p>
     * Spring AI 不同版本的 Document 构造方式差异较大，按以下优先级尝试：
     * <ol>
     *   <li>{@code new Document(id, content, metadata)} — 新版三参构造</li>
     *   <li>{@code new Document(content, metadata)} — 旧版二参构造（不支持自定义 id）</li>
     *   <li>{@code Document.builder().id(id).text(content).metadata(metadata).build()} — Builder 模式</li>
     * </ol>
     */
    private Object buildDocument(String id, String content, Map<String, Object> metadata) {
        try {
            Class<?> docClass = Class.forName("org.springframework.ai.document.Document");
            try {
                Constructor<?> ctor = docClass.getConstructor(String.class, String.class, Map.class);
                return ctor.newInstance(id, content, metadata);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                Constructor<?> ctor = docClass.getConstructor(String.class, Map.class);
                return ctor.newInstance(content, metadata);
            } catch (NoSuchMethodException ignored) {
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

    /** 反射调用 builder 的 setter 方法，方法不存在时静默跳过。 */
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
        }
    }

    /** 反射调用 {@code VectorStore.add(List)} 写入向量数据。 */
    @SuppressWarnings("unchecked")
    private void invokeVectorStoreAdd(List<Object> docs) {
        try {
            Method add = vectorStore.getClass().getMethod("add", List.class);
            add.invoke(vectorStore, docs);
        } catch (Exception ex) {
            throw new IllegalStateException("VectorStore.add invocation failed", ex);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 驼峰命名转下划线命名。
     * 例：{@code SysUser → sys_user}，{@code ProductCategory → product_category}
     */
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

    /** 封装表的列名信息，用于在方法间传递 schema 元数据。 */
    private static class TableSchema {
        private final List<String> columns;

        private TableSchema(List<String> columns) {
            this.columns = columns;
        }
    }
}
