package io.github.aiagent.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent 统一配置属性。
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

    public static class LlmProperties { private String provider = "dashscope"; public String getProvider(){return provider;} public void setProvider(String provider){this.provider=provider;} }
    public static class MemoryProperties { private String strategy = "MESSAGE_WINDOW"; public String getStrategy(){return strategy;} public void setStrategy(String strategy){this.strategy=strategy;} }
    public static class KnowledgeProperties { private boolean enabled = true; public boolean isEnabled(){return enabled;} public void setEnabled(boolean enabled){this.enabled=enabled;} }
    public static class ToolProperties { private int timeoutSeconds = 10; public int getTimeoutSeconds(){return timeoutSeconds;} public void setTimeoutSeconds(int timeoutSeconds){this.timeoutSeconds=timeoutSeconds;} }
    public static class TranslatorProperties { private boolean enabled = true; public boolean isEnabled(){return enabled;} public void setEnabled(boolean enabled){this.enabled=enabled;} }
    public static class SafetyProperties { private boolean writeNeedConfirm = true; public boolean isWriteNeedConfirm(){return writeNeedConfirm;} public void setWriteNeedConfirm(boolean writeNeedConfirm){this.writeNeedConfirm=writeNeedConfirm;} }
    public static class DialogueProperties { private int heartbeatSeconds = 15; public int getHeartbeatSeconds(){return heartbeatSeconds;} public void setHeartbeatSeconds(int heartbeatSeconds){this.heartbeatSeconds=heartbeatSeconds;} }
    public static class SseProperties { private int reconnectDelayMs = 1000; public int getReconnectDelayMs(){return reconnectDelayMs;} public void setReconnectDelayMs(int reconnectDelayMs){this.reconnectDelayMs=reconnectDelayMs;} }

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
