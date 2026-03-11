package io.github.aiagent.knowledge.sql;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 静态安全校验器 —— {@link SafeSqlQueryTool} 多层防御的第一道关卡。
 * <p>
 * 校验规则按以下顺序执行，任一不通过即拒绝：
 * <ol>
 *   <li><b>语句类型</b> —— 仅允许 {@code SELECT} 开头的查询语句</li>
 *   <li><b>危险函数黑名单</b> —— 拦截 {@code SLEEP()}、{@code LOAD_FILE()} 等</li>
 *   <li><b>表黑名单</b> —— 拦截 {@link SqlQueryProperties#getDeniedTables()} 中配置的表</li>
 *   <li><b>表白名单</b> —— 当 {@link SqlQueryProperties#getAllowedTables()} 非空时，
 *       SQL 中引用的所有表都必须在白名单内</li>
 * </ol>
 *
 * @see SafeSqlQueryTool
 * @see SqlQueryProperties
 */
@Component
public class SqlValidator {

    /** 匹配 SQL 中 FROM / JOIN 关键字后面的表名。 */
    private static final Pattern TABLE_REF_PATTERN =
            Pattern.compile("(?:from|join)\\s+([a-z_][a-z0-9_]*)", Pattern.CASE_INSENSITIVE);

    private final SqlQueryProperties properties;

    public SqlValidator(SqlQueryProperties properties) {
        this.properties = properties;
    }

    /**
     * 对 SQL 语句执行静态安全校验。
     *
     * @param sql 待校验的 SQL 语句
     * @return 校验结果，{@link ValidationResult#isAllowed()} 为 false 时包含拒绝原因
     */
    public ValidationResult validate(String sql) {
        String normalized = sql == null ? "" : sql.trim().toLowerCase();
        if (!normalized.startsWith("select")) {
            return ValidationResult.reject("Only SELECT is allowed");
        }
        for (String denied : properties.getDeniedFunctions()) {
            if (normalized.contains(denied.toLowerCase() + "(")) {
                return ValidationResult.reject("Denied function: " + denied);
            }
        }
        for (String deniedTable : properties.getDeniedTables()) {
            if (normalized.contains(deniedTable.toLowerCase())) {
                return ValidationResult.reject("Denied table: " + deniedTable);
            }
        }
        List<String> allowedTables = properties.getAllowedTables();
        if (allowedTables != null && !allowedTables.isEmpty()) {
            Set<String> allowedLower = new HashSet<>();
            for (String t : allowedTables) {
                allowedLower.add(t.toLowerCase());
            }
            Matcher matcher = TABLE_REF_PATTERN.matcher(normalized);
            while (matcher.find()) {
                String table = matcher.group(1);
                if (!allowedLower.contains(table)) {
                    return ValidationResult.reject("Table not in whitelist: " + table);
                }
            }
        }
        return ValidationResult.allow();
    }

    /**
     * 校验结果值对象。
     */
    public static class ValidationResult {
        private final boolean allowed;
        private final String reason;

        private ValidationResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public static ValidationResult allow() { return new ValidationResult(true, ""); }
        public static ValidationResult reject(String reason) { return new ValidationResult(false, reason); }
        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
    }
}
