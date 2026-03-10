package io.github.aiagent.translator.strategy;

import io.github.aiagent.translator.TranslateContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationPolicyTest {
    @Test
    void shouldUseDefaultPoliciesInContext() {
        TranslateContext context = new TranslateContext();
        assertEquals(TranslationPolicy.MultiResultPolicy.FIRST_MATCH, context.getMultiResultPolicy());
        assertEquals(TranslationPolicy.NotFoundPolicy.FAIL, context.getNotFoundPolicy());
    }
}
