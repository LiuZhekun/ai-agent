package io.github.aiagent.knowledge.sql;

import io.github.aiagent.core.tool.annotation.AgentTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * L2 安全 SQL 工具。
 * <p>
 * 执行顺序：语法/权限校验 -> Explain 扫描评估 -> 频控 -> 自动附加 LIMIT -> 脱敏返回。
 */
@AgentTool(name = "safe-sql", description = "安全 SQL 查询工具", riskLevel = AgentTool.RiskLevel.HIGH)
public class SafeSqlQueryTool {

    private final SqlValidator validator;
    private final SqlExplainChecker explainChecker;
    private final SqlRateLimiter rateLimiter;
    private final SqlDataMasker masker;
    private final SqlQueryProperties properties;
    private final JdbcTemplate jdbcTemplate;

    public SafeSqlQueryTool(
            SqlValidator validator,
            SqlExplainChecker explainChecker,
            SqlRateLimiter rateLimiter,
            SqlDataMasker masker,
            SqlQueryProperties properties,
            JdbcTemplate jdbcTemplate) {
        this.validator = validator;
        this.explainChecker = explainChecker;
        this.rateLimiter = rateLimiter;
        this.masker = masker;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Tool(description = "执行安全 SQL 查询")
    public List<Map<String, Object>> executeSql(String sql) {
        // 1) 静态规则校验（禁用语句、表白名单等）
        SqlValidator.ValidationResult check = validator.validate(sql);
        if (!check.isAllowed()) {
            throw new IllegalArgumentException(check.getReason());
        }
        // 2) Explain 守卫，避免全表大扫描
        if (!explainChecker.check(sql)) {
            throw new IllegalArgumentException("SQL scan rows too large");
        }
        // 3) 按会话维度限流，防止高频压库
        if (!rateLimiter.allow("default")) {
            throw new IllegalStateException("SQL rate limit exceeded");
        }
        // 4) 强制追加最大返回行数并执行
        String safeSql = sql + " LIMIT " + properties.getMaxRows();
        // 5) 对敏感字段脱敏后再返回给上层
        return masker.mask(jdbcTemplate.queryForList(safeSql));
    }
}
