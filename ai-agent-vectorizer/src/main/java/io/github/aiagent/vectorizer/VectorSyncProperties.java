package io.github.aiagent.vectorizer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量同步配置属性，对应配置前缀 {@code ai.agent.vector.sync}。
 * <p>
 * 示例：
 * <pre>
 * ai:
 *   agent:
 *     vector:
 *       sync:
 *         enabled: true
 *         cron-expression: "0 *&#47;5 * * * *"
 *         batch-size: 200
 * </pre>
 *
 * @see VectorSyncScheduler
 * @see VectorSyncService
 */
@Component
@ConfigurationProperties(prefix = "ai.agent.vector.sync")
public class VectorSyncProperties {

    /**
     * 增量同步定时任务的 cron 表达式。
     * 默认每 5 分钟执行一次。
     */
    private String cronExpression = "0 */5 * * * *";

    /**
     * 每批次写入向量库的文档数量。
     * 过大可能导致单次写入超时或内存压力；过小则增加网络往返次数。
     */
    private int batchSize = 200;

    /**
     * 是否启用向量同步功能。
     * 设为 false 时定时任务和手动触发均不执行同步。
     */
    private boolean enabled = true;

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
