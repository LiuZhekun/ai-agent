package io.github.aiagent.translator.builtin;

import io.github.aiagent.translator.FieldTranslator;
import io.github.aiagent.translator.TranslateContext;
import io.github.aiagent.translator.strategy.TranslationPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典翻译器 —— 查询 {@code sys_dict} 表将字典名称翻译为对应的字典编码。
 * <p>
 * 典型场景：用户说"状态为启用"，本翻译器将 {@code "启用"} 转换为字典 code（如 {@code 1}）。
 * <p>
 * 查询逻辑：{@code SELECT <resultField> FROM sys_dict WHERE type=<target> AND <lookupField>=<sourceValue>}
 * <ul>
 *   <li>{@code lookupField} 默认 {@code "name"}，{@code resultField} 默认 {@code "code"}</li>
 *   <li>字段名会做合法标识符校验，防止 SQL 注入</li>
 *   <li>多条匹配 / 未匹配时按 {@link TranslateContext} 中的策略处理</li>
 * </ul>
 *
 * @see FieldTranslator
 * @see TranslationPolicy
 */
@Component
public class DictTranslator implements FieldTranslator {

    private final JdbcTemplate jdbcTemplate;

    public DictTranslator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getType() {
        return "DICT";
    }

    @Override
    public Object translate(TranslateContext ctx) {
        String lookupField = identifierOrDefault(ctx.getLookupField(), "name");
        String resultField = identifierOrDefault(ctx.getResultField(), "code");
        String sql = "SELECT " + resultField + " FROM sys_dict WHERE type=? AND " + lookupField + "=?";
        List<Object> candidates = jdbcTemplate.queryForList(
                sql,
                Object.class,
                ctx.getTarget(),
                String.valueOf(ctx.getSourceValue()));
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
            throw new IllegalArgumentException("Dict translation not found");
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
        throw new IllegalArgumentException("Dict translation ambiguous, candidates=" + candidates.size());
    }
}
