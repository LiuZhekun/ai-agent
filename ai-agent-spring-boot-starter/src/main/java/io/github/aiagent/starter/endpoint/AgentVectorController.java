package io.github.aiagent.starter.endpoint;

import io.github.aiagent.vectorizer.VectorSyncScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 向量同步管理 REST 接口。
 * <p>
 * 仅在 classpath 中存在 {@code VectorSyncService} 时激活（即引入了 ai-agent-vectorizer 模块）。
 * <p>
 * 提供两个端点：
 * <ul>
 *   <li>{@code POST /api/agent/vector/sync} — 手动触发一轮增量同步</li>
 *   <li>{@code GET /api/agent/vector/status} — 查询最近一次同步的完成时间</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/agent/vector")
@ConditionalOnClass(name = "io.github.aiagent.vectorizer.VectorSyncService")
public class AgentVectorController {

    private final VectorSyncScheduler scheduler;

    public AgentVectorController(VectorSyncScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 手动触发一轮向量同步。
     * 遍历所有已注册实体执行增量同步，适用于首次接入或数据修复后的即时同步。
     * 同步为同步调用，大数据量时可能耗时较长。
     *
     * @return {@code {"status": "ok"}} 表示同步完成
     */
    @PostMapping("/sync")
    public Map<String, Object> sync() {
        scheduler.sync();
        return Map.of("status", "ok");
    }

    /**
     * 查询向量同步状态。
     *
     * @return 包含 lastSyncTime 的 JSON，格式为 ISO-8601 时间戳
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("lastSyncTime", scheduler.getLastSyncTime().toString());
    }
}
