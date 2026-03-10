package io.github.aiagent.translator.builtin;

import io.github.aiagent.translator.TranslateContext;
import io.github.aiagent.translator.strategy.TranslationPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DictTranslatorTest {

    @Test
    void shouldReturnFirstWhenMultiResultPolicyIsFirstMatch() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Object.class), eq("gender"), eq("男")))
                .thenReturn(List.of("M", "MALE"));
        DictTranslator translator = new DictTranslator(jdbcTemplate);

        TranslateContext context = context("gender", "男");
        context.setMultiResultPolicy(TranslationPolicy.MultiResultPolicy.FIRST_MATCH);

        Object result = translator.translate(context);
        assertEquals("M", result);
    }

    @Test
    void shouldReturnCandidatesWhenMultiResultPolicyIsUserSelect() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Object.class), eq("gender"), eq("男")))
                .thenReturn(List.of("M", "MALE"));
        DictTranslator translator = new DictTranslator(jdbcTemplate);

        TranslateContext context = context("gender", "男");
        context.setMultiResultPolicy(TranslationPolicy.MultiResultPolicy.USER_SELECT);

        Object result = translator.translate(context);
        assertEquals(List.of("M", "MALE"), result);
    }

    @Test
    void shouldReturnNullWhenNotFoundPolicyIsSkip() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Object.class), eq("gender"), eq("未知")))
                .thenReturn(List.of());
        DictTranslator translator = new DictTranslator(jdbcTemplate);

        TranslateContext context = context("gender", "未知");
        context.setNotFoundPolicy(TranslationPolicy.NotFoundPolicy.SKIP);

        Object result = translator.translate(context);
        assertNull(result);
    }

    @Test
    void shouldThrowWhenMultiResultPolicyIsFail() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Object.class), eq("gender"), eq("男")))
                .thenReturn(List.of("M", "MALE"));
        DictTranslator translator = new DictTranslator(jdbcTemplate);

        TranslateContext context = context("gender", "男");
        context.setMultiResultPolicy(TranslationPolicy.MultiResultPolicy.FAIL);

        assertThrows(IllegalArgumentException.class, () -> translator.translate(context));
    }

    private TranslateContext context(String target, Object sourceValue) {
        TranslateContext context = new TranslateContext();
        context.setTarget(target);
        context.setSourceValue(sourceValue);
        return context;
    }
}
