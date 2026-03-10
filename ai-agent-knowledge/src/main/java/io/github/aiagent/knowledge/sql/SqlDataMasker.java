package io.github.aiagent.knowledge.sql;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询结果脱敏器。
 */
@Component
public class SqlDataMasker {

    private final SqlQueryProperties properties;

    public SqlDataMasker(SqlQueryProperties properties) {
        this.properties = properties;
    }

    public List<Map<String, Object>> mask(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> copy = new java.util.LinkedHashMap<>(row);
            for (String column : properties.getMaskColumns()) {
                if (copy.containsKey(column) && copy.get(column) != null) {
                    copy.put(column, maskValue(String.valueOf(copy.get(column))));
                }
            }
            result.add(copy);
        }
        return result;
    }

    private String maskValue(String value) {
        String masked = value.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        masked = masked.replaceAll("(^\\w{2})\\w+(\\w{2}@)", "$1***$2");
        masked = masked.replaceAll("(\\d{6})\\d{8}(\\w{4})", "$1********$2");
        return masked;
    }
}
