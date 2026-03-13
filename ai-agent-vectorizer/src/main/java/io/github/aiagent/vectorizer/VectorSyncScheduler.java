package io.github.aiagent.vectorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 向量增量同步调度器。
 * <p>
 * 按 cron 表达式定时触发，遍历所有 {@link VectorSyncEntityProvider} 注册的实体类，
 * 对每个实体执行增量同步。
 * <p>
 * <b>调度策略：</b>
 * <ul>
 *   <li>每次调度以上一次同步的完成时间作为增量起点</li>
 *   <li>首次启动时默认从 1 小时前开始同步，确保最近变更不遗漏</li>
 *   <li>单个实体同步失败不会阻断其他实体，异常被隔离并记录日志</li>
 * </ul>
 * <p>
 * <b>手动触发：</b>可通过 {@code POST /api/agent/vector/sync} 接口调用 {@link #sync()} 方法。
 *
 * @see VectorSyncService
 * @see VectorSyncEntityProvider
 */
@Component
public class VectorSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(VectorSyncScheduler.class);

    private final VectorSyncService vectorSyncService;
    private final VectorSyncProperties vectorSyncProperties;
    /** 通过 ObjectProvider 延迟获取，支持零个或多个 Provider 共存。 */
    private final ObjectProvider<VectorSyncEntityProvider> entityProviders;
    /** 上一次同步完成时间，初始值为启动前 1 小时，确保首次同步能覆盖最近变更。 */
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
     * 定时同步入口，由 Spring @Scheduled 驱动或 REST 接口手动调用。
     * <p>
     * 执行流程：
     * <ol>
     *   <li>检查同步开关 {@code ai.agent.vector.sync.enabled}</li>
     *   <li>收集所有 VectorSyncEntityProvider 注册的实体类</li>
     *   <li>逐个实体调用增量同步，单个失败不影响其他实体</li>
     *   <li>更新 lastSyncTime 供状态查询接口使用</li>
     * </ol>
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
        int entityCount = 0;
        int failCount = 0;
        for (VectorSyncEntityProvider provider : providers) {
            for (Class<?> entityClass : provider.entities()) {
                entityCount++;
                try {
                    vectorSyncService.syncIncremental(entityClass, since);
                } catch (Exception ex) {
                    failCount++;
                    log.error("Vector sync failed for entity {}: {}", entityClass.getName(), ex.getMessage(), ex);
                }
            }
        }
        lastSyncTime = Instant.now();
        Duration elapsed = Duration.between(syncStart, lastSyncTime);
        log.info("Vector sync round completed: entities={}, failed={}, elapsed={}ms",
                entityCount, failCount, elapsed.toMillis());
    }

    /**
     * 应用就绪后执行一次全量同步，确保 Milvus 在首次检索前完成首批数据写入与索引创建。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialSyncOnStartup() {
        if (!vectorSyncProperties.isEnabled()) {
            return;
        }
        List<VectorSyncEntityProvider> providers = entityProviders.orderedStream().toList();
        if (providers.isEmpty()) {
            return;
        }
        int entityCount = 0;
        int failCount = 0;
        Instant start = Instant.now();
        for (VectorSyncEntityProvider provider : providers) {
            for (Class<?> entityClass : provider.entities()) {
                entityCount++;
                try {
                    vectorSyncService.syncAll(entityClass);
                } catch (Exception ex) {
                    failCount++;
                    log.error("Initial vector sync failed for entity {}: {}", entityClass.getName(), ex.getMessage(), ex);
                }
            }
        }
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        log.info("Initial vector sync completed: entities={}, failed={}, elapsed={}ms",
                entityCount, failCount, elapsedMs);
    }

    /**
     * 获取最近一次同步完成时间，供运维状态接口查询。
     */
    public Instant getLastSyncTime() {
        return lastSyncTime;
    }
}
