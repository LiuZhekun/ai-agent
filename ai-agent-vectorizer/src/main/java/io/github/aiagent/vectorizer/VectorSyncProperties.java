package io.github.aiagent.vectorizer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量同步配置。
 */
@Component
@ConfigurationProperties(prefix = "ai.agent.vector.sync")
public class VectorSyncProperties {
    private String cronExpression = "0 */5 * * * *";
    private int batchSize = 200;
    private boolean enabled = true;

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
