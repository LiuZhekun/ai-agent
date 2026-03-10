package io.github.aiagent.vectorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 向量增量同步调度器。
 * 负责按 cron 触发同步任务，并记录最近一次触发时间供运维接口查询。
 */
@Component
public class VectorSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(VectorSyncScheduler.class);

    private final VectorSyncService vectorSyncService;
    private final VectorSyncProperties vectorSyncProperties;
    private final ObjectProvider<VectorSyncEntityProvider> entityProviders;
    private Instant lastSyncTime = Instant.now().minusSeconds(3600);

    public VectorSyncScheduler(
            VectorSyncService vectorSyncService,
            VectorSyncProperties vectorSyncProperties,
            ObjectProvider<VectorSyncEntityProvider> entityProviders) {
        this.vectorSyncService = vectorSyncService;
        this.vectorSyncProperties = vectorSyncProperties;
        this.entityProviders = entityProviders;
    }

    /**
     * 定时触发入口。
     * 当前模板默认只更新时间，业务项目可在这里注册并触发具体实体的同步。
     */
    @Scheduled(cron = "${ai.agent.vector.sync.cron-expression:0 */5 * * * *}")
    public void sync() {
        if (!vectorSyncProperties.isEnabled()) {
            log.debug("Skip vector sync scheduler because ai.agent.vector.sync.enabled=false");
            return;
        }
        Instant syncStart = Instant.now();
        Instant since = lastSyncTime;
        List<VectorSyncEntityProvider> providers = entityProviders.orderedStream().toList();
        if (providers.isEmpty()) {
            log.debug("Skip vector sync because no VectorSyncEntityProvider found");
            lastSyncTime = syncStart;
            return;
        }
        for (VectorSyncEntityProvider provider : providers) {
            for (Class<?> entityClass : provider.entities()) {
                vectorSyncService.syncIncremental(entityClass, since);
            }
        }
        lastSyncTime = Instant.now();
    }

    public Instant getLastSyncTime() {
        return lastSyncTime;
    }
}
