package io.github.aiagent.knowledge.sql;

import io.github.aiagent.core.tool.annotation.AgentTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * L2 安全 SQL 查询工具 —— 通过多层防御策略让 LLM 安全地查询业务数据库。
 * <p>
 * 该工具注册为 Agent 可调用的高风险工具（{@code riskLevel=HIGH}），
 * 在 LLM 生成的 SQL 到达数据库之前依次经过五道安全关卡：
 * <ol>
 *   <li><b>静态规则校验</b>（{@link SqlValidator}）—— 仅允许 SELECT，拦截危险函数和受限表</li>
 *   <li><b>EXPLAIN 扫描评估</b>（{@link SqlExplainChecker}）—— 防止全表大扫描拖垮数据库</li>
 *   <li><b>会话级频率限制</b>（{@link SqlRateLimiter}）—— 防止 LLM 循环调用导致高频压库</li>
 *   <li><b>强制 LIMIT</b> —— 自动追加最大返回行数，避免超大结果集</li>
 *   <li><b>数据脱敏</b>（{@link SqlDataMasker}）—— 对手机号、邮箱、身份证等敏感字段脱敏后再返回</li>
 * </ol>
 * <p>
 * 建议搭配只读数据源使用，从连接层面杜绝写操作风险。
 *
 * @see SqlValidator
 * @see SqlExplainChecker
 * @see SqlRateLimiter
 * @see SqlDataMasker
 * @see SqlQueryProperties
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

    /**
     * 执行安全 SQL 查询。
     * <p>
     * 依次经过静态校验 → EXPLAIN 守卫 → 频率限制 → 强制 LIMIT → 结果脱敏，
     * 任一环节不通过则抛出异常，不会实际执行 SQL。
     *
     * @param sql LLM 生成的 SQL 语句（仅允许 SELECT）
     * @return 脱敏后的查询结果
     * @throws IllegalArgumentException  SQL 校验不通过或扫描行数过大
     * @throws IllegalStateException     超出频率限制
     */
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
