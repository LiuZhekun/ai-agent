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

class EntityRefTranslatorTest {

    @Test
    void shouldReturnOriginalWhenNotFoundPolicyIsUseOriginal() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Object.class), eq("未知部门")))
                .thenReturn(List.of());
        EntityRefTranslator translator = new EntityRefTranslator(jdbcTemplate);

        TranslateContext context = context("sys_department", "未知部门");
        context.setNotFoundPolicy(TranslationPolicy.NotFoundPolicy.USE_ORIGINAL);

        Object result = translator.translate(context);
        assertEquals("未知部门", result);
    }

    @Test
    void shouldReturnCandidatesWhenMultiResultPolicyIsUserSelect() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Object.class), eq("研发部")))
                .thenReturn(List.of(1L, 2L));
        EntityRefTranslator translator = new EntityRefTranslator(jdbcTemplate);

        TranslateContext context = context("sys_department", "研发部");
        context.setMultiResultPolicy(TranslationPolicy.MultiResultPolicy.USER_SELECT);

        Object result = translator.translate(context);
        assertEquals(List.of(1L, 2L), result);
    }

    @Test
    void shouldReturnNullWhenNotFoundPolicyIsSkip() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Object.class), eq("未知部门")))
                .thenReturn(List.of());
        EntityRefTranslator translator = new EntityRefTranslator(jdbcTemplate);

        TranslateContext context = context("sys_department", "未知部门");
        context.setNotFoundPolicy(TranslationPolicy.NotFoundPolicy.SKIP);

        Object result = translator.translate(context);
        assertNull(result);
    }

    @Test
    void shouldThrowWhenTableNotAllowed() {
        EntityRefTranslator translator = new EntityRefTranslator(mock(JdbcTemplate.class));
        TranslateContext context = context("sys_unknown", "foo");
        assertThrows(IllegalArgumentException.class, () -> translator.translate(context));
    }

    private TranslateContext context(String target, Object sourceValue) {
        TranslateContext context = new TranslateContext();
        context.setTarget(target);
        context.setSourceValue(sourceValue);
        return context;
    }
}
