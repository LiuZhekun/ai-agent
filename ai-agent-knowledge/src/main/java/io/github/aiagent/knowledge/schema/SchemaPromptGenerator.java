package io.github.aiagent.knowledge.schema;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Schema 转 Prompt 文本生成器。
 */
@Component
public class SchemaPromptGenerator {

    public String generate(List<TableSchema> schemas) {
        return schemas.stream().map(schema -> {
            String columns = schema.getColumns().stream()
                    .map(c -> c.getName() + "(" + c.getType() + "," + c.getComment() + ")")
                    .collect(Collectors.joining(", "));
            return schema.getTableName() + "(" + schema.getComment() + "): " + columns;
        }).collect(Collectors.joining("\n"));
    }
}
