package io.github.aiagent.translator;

import io.github.aiagent.translator.strategy.TranslationPolicy;

import java.util.Map;

/**
 * 翻译上下文 —— 承载单次翻译调用所需的全部参数。
 * <p>
 * 各字段含义：
 * <ul>
 *   <li>{@code sourceValue} — 待翻译的原始值（如用户输入的中文名称）</li>
 *   <li>{@code target} — 翻译目标（字典类型编码 / 实体表名）</li>
 *   <li>{@code lookupField} — 在目标数据源中用于匹配的列名，默认 {@code "name"}</li>
 *   <li>{@code resultField} — 翻译结果所在的列名，默认 {@code "id"}</li>
 *   <li>{@code multiResultPolicy} — 匹配到多条记录时的冲突策略</li>
 *   <li>{@code notFoundPolicy} — 未匹配到记录时的处理策略</li>
 *   <li>{@code metadata} — 可扩展的附加信息（保留给自定义翻译器使用）</li>
 * </ul>
 *
 * @see FieldTranslator#translate(TranslateContext)
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
