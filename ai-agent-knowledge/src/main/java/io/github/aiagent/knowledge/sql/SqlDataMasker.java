package io.github.aiagent.knowledge.sql;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询结果脱敏器 —— {@link SafeSqlQueryTool} 多层防御的最后一道关卡。
 * <p>
 * 对 {@link SqlQueryProperties#getMaskColumns()} 中配置的敏感列进行正则脱敏：
 * <ul>
 *   <li>手机号 —— 中间 4 位替换为 {@code ****}（如 138****1234）</li>
 *   <li>邮箱 —— 用户名中间部分替换为 {@code ***}</li>
 *   <li>身份证号 —— 中间 8 位替换为 {@code ********}</li>
 * </ul>
 * <p>
 * 脱敏在内存中对查询结果的副本操作，不影响原始数据。
 *
 * @see SafeSqlQueryTool
 * @see SqlQueryProperties
 */
@Component
public class SqlDataMasker {

    private final SqlQueryProperties properties;

    public SqlDataMasker(SqlQueryProperties properties) {
        this.properties = properties;
    }

    /**
     * 对查询结果中的敏感列执行脱敏处理。
     *
     * @param rows 原始查询结果
     * @return 脱敏后的结果副本（原始数据不被修改）；入参为 null 或空时直接返回
     */
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
