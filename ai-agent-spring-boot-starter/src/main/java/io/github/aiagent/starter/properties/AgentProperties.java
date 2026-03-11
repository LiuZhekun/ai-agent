package io.github.aiagent.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Agent 统一配置属性，绑定前缀 {@code ai.agent.*}。
 * <p>
 * 本类通过嵌套静态类将配置项按功能域分组，每个子属性类对应 YAML 中的一个子节点：
 * <ul>
 *   <li>{@link LlmProperties} — LLM 提供商配置（{@code ai.agent.llm.*}）</li>
 *   <li>{@link MemoryProperties} — 会话记忆策略（{@code ai.agent.memory.*}）</li>
 *   <li>{@link KnowledgeProperties} — 知识库与安全 SQL（{@code ai.agent.knowledge.*}）</li>
 *   <li>{@link ToolProperties} — 工具调用参数（{@code ai.agent.tool.*}）</li>
 *   <li>{@link TranslatorProperties} — 字段翻译（{@code ai.agent.translator.*}）</li>
 *   <li>{@link SafetyProperties} — 安全策略（{@code ai.agent.safety.*}）</li>
 *   <li>{@link DialogueProperties} — 多轮对话与追问（{@code ai.agent.dialogue.*}）</li>
 *   <li>{@link SseProperties} — SSE 推流参数（{@code ai.agent.sse.*}）</li>
 * </ul>
 * 所有属性均有合理默认值，可做到零配置启动。
 */
@ConfigurationProperties(prefix = "ai.agent")
public class AgentProperties {

    private LlmProperties llm = new LlmProperties();
    private MemoryProperties memory = new MemoryProperties();
    private KnowledgeProperties knowledge = new KnowledgeProperties();
    private ToolProperties tool = new ToolProperties();
    private TranslatorProperties translator = new TranslatorProperties();
    private SafetyProperties safety = new SafetyProperties();
    private DialogueProperties dialogue = new DialogueProperties();
    private SseProperties sse = new SseProperties();

    /** LLM 提供商配置。 */
    public static class LlmProperties {
        private String provider = "dashscope";
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
    }

    /** 会话记忆策略配置 —— 控制上下文裁剪、摘要触发及向量检索参数。 */
    public static class MemoryProperties {
        private String strategy = "token-window";
        private int messageWindow = 20;
        private int maxTokens = 8000;
        private int summaryThreshold = 6000;
        private int vectorTopK = 5;
        private double vectorThreshold = 0.7;

        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public int getMessageWindow() { return messageWindow; }
        public void setMessageWindow(int messageWindow) { this.messageWindow = messageWindow; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public int getSummaryThreshold() { return summaryThreshold; }
        public void setSummaryThreshold(int summaryThreshold) { this.summaryThreshold = summaryThreshold; }
        public int getVectorTopK() { return vectorTopK; }
        public void setVectorTopK(int vectorTopK) { this.vectorTopK = vectorTopK; }
        public double getVectorThreshold() { return vectorThreshold; }
        public void setVectorThreshold(double vectorThreshold) { this.vectorThreshold = vectorThreshold; }
    }

    /** 知识库配置 —— 包含 Schema 发现和安全 SQL 执行的子配置。 */
    public static class KnowledgeProperties {
        private boolean enabled = true;
        private SchemaProperties schema = new SchemaProperties();
        private SafeSqlProperties safeSql = new SafeSqlProperties();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public SchemaProperties getSchema() { return schema; }
        public void setSchema(SchemaProperties schema) { this.schema = schema; }
        public SafeSqlProperties getSafeSql() { return safeSql; }
        public void setSafeSql(SafeSqlProperties safeSql) { this.safeSql = safeSql; }

        public static class SchemaProperties {
            private boolean enabled = true;
            private List<String> includeTables = new ArrayList<>();
            private List<String> excludeTables = new ArrayList<>();
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public List<String> getIncludeTables() { return includeTables; }
            public void setIncludeTables(List<String> includeTables) { this.includeTables = includeTables; }
            public List<String> getExcludeTables() { return excludeTables; }
            public void setExcludeTables(List<String> excludeTables) { this.excludeTables = excludeTables; }
        }

        public static class SafeSqlProperties {
            private boolean enabled = true;
            private int maxRows = 100;
            private int rateLimit = 10;
            private int timeoutSeconds = 10;
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public int getMaxRows() { return maxRows; }
            public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
            public int getRateLimit() { return rateLimit; }
            public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }
            public int getTimeoutSeconds() { return timeoutSeconds; }
            public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        }
    }

    /** 工具调用配置 —— 超时、重试、并发及扫描包路径。 */
    public static class ToolProperties {
        private int timeoutSeconds = 10;
        private int maxRetries = 1;
        private int maxConcurrent = 5;
        private List<String> scanPackages = new ArrayList<>();

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public int getMaxConcurrent() { return maxConcurrent; }
        public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
        public List<String> getScanPackages() { return scanPackages; }
        public void setScanPackages(List<String> scanPackages) { this.scanPackages = scanPackages; }
    }

    /** 字段翻译配置 —— 字典表名、列名及冲突策略。 */
    public static class TranslatorProperties {
        private boolean enabled = true;
        private String dictTable = "sys_dict";
        private String dictNameColumn = "name";
        private String dictCodeColumn = "code";
        private String multiMatchStrategy = "user-select";
        private String noMatchStrategy = "fail";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getDictTable() { return dictTable; }
        public void setDictTable(String dictTable) { this.dictTable = dictTable; }
        public String getDictNameColumn() { return dictNameColumn; }
        public void setDictNameColumn(String dictNameColumn) { this.dictNameColumn = dictNameColumn; }
        public String getDictCodeColumn() { return dictCodeColumn; }
        public void setDictCodeColumn(String dictCodeColumn) { this.dictCodeColumn = dictCodeColumn; }
        public String getMultiMatchStrategy() { return multiMatchStrategy; }
        public void setMultiMatchStrategy(String s) { this.multiMatchStrategy = s; }
        public String getNoMatchStrategy() { return noMatchStrategy; }
        public void setNoMatchStrategy(String s) { this.noMatchStrategy = s; }
    }

    /** 安全策略配置 —— 思维链可见性与写操作确认开关。 */
    public static class SafetyProperties {
        private String thinkingVisibility = "summary";
        private boolean writeNeedConfirm = true;

        public String getThinkingVisibility() { return thinkingVisibility; }
        public void setThinkingVisibility(String v) { this.thinkingVisibility = v; }
        public boolean isWriteNeedConfirm() { return writeNeedConfirm; }
        public void setWriteNeedConfirm(boolean writeNeedConfirm) { this.writeNeedConfirm = writeNeedConfirm; }
    }

    /** 多轮对话与追问配置 —— 追问轮次、批量数、超时及心跳间隔。 */
    public static class DialogueProperties {
        private boolean clarificationEnabled = true;
        private int clarificationMaxRounds = 3;
        private int clarificationAskBatchSize = 2;
        private int clarificationTimeoutMinutes = 10;
        private int heartbeatSeconds = 15;

        public boolean isClarificationEnabled() { return clarificationEnabled; }
        public void setClarificationEnabled(boolean e) { this.clarificationEnabled = e; }
        public int getClarificationMaxRounds() { return clarificationMaxRounds; }
        public void setClarificationMaxRounds(int v) { this.clarificationMaxRounds = v; }
        public int getClarificationAskBatchSize() { return clarificationAskBatchSize; }
        public void setClarificationAskBatchSize(int v) { this.clarificationAskBatchSize = v; }
        public int getClarificationTimeoutMinutes() { return clarificationTimeoutMinutes; }
        public void setClarificationTimeoutMinutes(int v) { this.clarificationTimeoutMinutes = v; }
        public int getHeartbeatSeconds() { return heartbeatSeconds; }
        public void setHeartbeatSeconds(int heartbeatSeconds) { this.heartbeatSeconds = heartbeatSeconds; }
    }

    /** SSE 推流配置 —— 心跳间隔与客户端重连延迟。 */
    public static class SseProperties {
        private int heartbeatIntervalSeconds = 15;
        private int reconnectDelayMs = 1000;

        public int getHeartbeatIntervalSeconds() { return heartbeatIntervalSeconds; }
        public void setHeartbeatIntervalSeconds(int v) { this.heartbeatIntervalSeconds = v; }
        public int getReconnectDelayMs() { return reconnectDelayMs; }
        public void setReconnectDelayMs(int reconnectDelayMs) { this.reconnectDelayMs = reconnectDelayMs; }
    }

    // --- Top-level getters/setters ---
    public LlmProperties getLlm() { return llm; }
    public void setLlm(LlmProperties llm) { this.llm = llm; }
    public MemoryProperties getMemory() { return memory; }
    public void setMemory(MemoryProperties memory) { this.memory = memory; }
    public KnowledgeProperties getKnowledge() { return knowledge; }
    public void setKnowledge(KnowledgeProperties knowledge) { this.knowledge = knowledge; }
    public ToolProperties getTool() { return tool; }
    public void setTool(ToolProperties tool) { this.tool = tool; }
    public TranslatorProperties getTranslator() { return translator; }
    public void setTranslator(TranslatorProperties translator) { this.translator = translator; }
    public SafetyProperties getSafety() { return safety; }
    public void setSafety(SafetyProperties safety) { this.safety = safety; }
    public DialogueProperties getDialogue() { return dialogue; }
    public void setDialogue(DialogueProperties dialogue) { this.dialogue = dialogue; }
    public SseProperties getSse() { return sse; }
    public void setSse(SseProperties sse) { this.sse = sse; }
}
