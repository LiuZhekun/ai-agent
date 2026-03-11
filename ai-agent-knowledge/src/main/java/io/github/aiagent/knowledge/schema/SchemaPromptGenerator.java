package io.github.aiagent.knowledge.schema;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Schema → Prompt 文本生成器 —— 将数据库表结构转换为 LLM 可理解的自然语言描述。
 * <p>
 * 输出格式为每张表一行：{@code 表名(表注释): 列名(列类型,列注释), ...}，
 * 供 {@link io.github.aiagent.knowledge.KnowledgeManager} 拼接进系统 Prompt。
 * <p>
 * 紧凑的单行格式是为了在有限的 token 预算内尽可能多地传递 Schema 信息。
 *
 * @see SchemaDiscoveryService
 * @see TableSchema
 */
@Component
public class SchemaPromptGenerator {

    /**
     * 将 Schema 列表转换为可注入 Prompt 的文本。
     *
     * @param schemas 由 {@link SchemaDiscoveryService} 发现的表结构列表
     * @return 每张表一行的紧凑描述文本；空列表返回空字符串
     */
    public String generate(List<TableSchema> schemas) {
        return schemas.stream().map(schema -> {
            String columns = schema.getColumns().stream()
                    .map(c -> c.getName() + "(" + c.getType() + "," + c.getComment() + ")")
                    .collect(Collectors.joining(", "));
            return schema.getTableName() + "(" + schema.getComment() + "): " + columns;
        }).collect(Collectors.joining("\n"));
    }
}
