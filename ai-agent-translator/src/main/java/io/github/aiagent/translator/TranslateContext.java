package io.github.aiagent.translator;

import io.github.aiagent.translator.strategy.TranslationPolicy;

import java.util.Map;

/**
 * 翻译上下文。
 */
public class TranslateContext {
    private Object sourceValue;
    private String target;
    private String lookupField;
    private String resultField;
    private TranslationPolicy.MultiResultPolicy multiResultPolicy = TranslationPolicy.MultiResultPolicy.FIRST_MATCH;
    private TranslationPolicy.NotFoundPolicy notFoundPolicy = TranslationPolicy.NotFoundPolicy.FAIL;
    private Map<String, Object> metadata;

    public Object getSourceValue() { return sourceValue; }
    public void setSourceValue(Object sourceValue) { this.sourceValue = sourceValue; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getLookupField() { return lookupField; }
    public void setLookupField(String lookupField) { this.lookupField = lookupField; }
    public String getResultField() { return resultField; }
    public void setResultField(String resultField) { this.resultField = resultField; }
    public TranslationPolicy.MultiResultPolicy getMultiResultPolicy() { return multiResultPolicy; }
    public void setMultiResultPolicy(TranslationPolicy.MultiResultPolicy multiResultPolicy) { this.multiResultPolicy = multiResultPolicy; }
    public TranslationPolicy.NotFoundPolicy getNotFoundPolicy() { return notFoundPolicy; }
    public void setNotFoundPolicy(TranslationPolicy.NotFoundPolicy notFoundPolicy) { this.notFoundPolicy = notFoundPolicy; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
