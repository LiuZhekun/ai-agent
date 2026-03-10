package io.github.aiagent.knowledge.sql;

import org.springframework.stereotype.Component;

/**
 * SQL 安全校验器。
 */
@Component
public class SqlValidator {

    private final SqlQueryProperties properties;

    public SqlValidator(SqlQueryProperties properties) {
        this.properties = properties;
    }

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
        return ValidationResult.allow();
    }

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
