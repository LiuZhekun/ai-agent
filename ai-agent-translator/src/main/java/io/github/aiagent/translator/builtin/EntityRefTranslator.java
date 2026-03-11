package io.github.aiagent.translator.builtin;

import io.github.aiagent.translator.FieldTranslator;
import io.github.aiagent.translator.TranslateContext;
import io.github.aiagent.translator.strategy.TranslationPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 实体引用翻译器 —— 将实体名称翻译为对应的主键 ID。
 * <p>
 * 典型场景：用户说"部门为研发部"，本翻译器将 {@code "研发部"} 转换为
 * {@code sys_department} 表中对应记录的 {@code id}。
 * <p>
 * <b>安全设计：表白名单机制</b><br>
 * 为防止通过 {@code target} 参数访问任意数据库表，本翻译器维护了一个静态白名单
 * {@code TABLE_WHITELIST}。只有白名单中的表才允许查询，请求非白名单表时会直接抛出异常。
 * 需要支持新的实体表时，应在白名单中显式注册。
 * <p>
 * 字段名同样做合法标识符校验以防止 SQL 注入。
 *
 * @see FieldTranslator
 * @see TranslationPolicy
 */
@Component
public class EntityRefTranslator implements FieldTranslator {

    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, String> TABLE_WHITELIST = Map.of(
            "sys_department", "sys_department",
            "sys_user", "sys_user"
    );

    public EntityRefTranslator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getType() {
        return "ENTITY_REF";
    }

    @Override
    public Object translate(TranslateContext ctx) {
        String table = TABLE_WHITELIST.get(ctx.getTarget());
        if (table == null) {
            throw new IllegalArgumentException("Target table not allowed: " + ctx.getTarget());
        }
        String lookup = identifierOrDefault(ctx.getLookupField(), "name");
        String result = identifierOrDefault(ctx.getResultField(), "id");
        String sql = "SELECT " + result + " FROM " + table + " WHERE " + lookup + " = ?";
        List<Object> candidates = jdbcTemplate.queryForList(sql, Object.class, String.valueOf(ctx.getSourceValue()));
        if (candidates.isEmpty()) {
            return onNotFound(ctx);
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return onMultiResult(candidates, ctx);
    }

    private String identifierOrDefault(String input, String defaultValue) {
        if (input == null || input.isBlank()) {
            return defaultValue;
        }
        if (!input.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid field identifier: " + input);
        }
        return input;
    }

    private Object onNotFound(TranslateContext ctx) {
        TranslationPolicy.NotFoundPolicy policy = ctx.getNotFoundPolicy();
        if (policy == null || policy == TranslationPolicy.NotFoundPolicy.FAIL) {
            throw new IllegalArgumentException("Entity translation not found");
        }
        if (policy == TranslationPolicy.NotFoundPolicy.USE_ORIGINAL) {
            return ctx.getSourceValue();
        }
        return null;
    }

    private Object onMultiResult(List<Object> candidates, TranslateContext ctx) {
        TranslationPolicy.MultiResultPolicy policy = ctx.getMultiResultPolicy();
        if (policy == null || policy == TranslationPolicy.MultiResultPolicy.FIRST_MATCH) {
            return candidates.get(0);
        }
        if (policy == TranslationPolicy.MultiResultPolicy.USER_SELECT) {
            return candidates;
        }
        throw new IllegalArgumentException("Entity translation ambiguous, candidates=" + candidates.size());
    }
}
