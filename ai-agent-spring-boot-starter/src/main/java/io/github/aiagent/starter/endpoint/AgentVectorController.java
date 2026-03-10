package io.github.aiagent.starter.endpoint;

import io.github.aiagent.vectorizer.VectorSyncScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 向量同步管理接口。
 */
@RestController
@RequestMapping("/api/agent/vector")
@ConditionalOnClass(name = "io.github.aiagent.vectorizer.VectorSyncService")
public class AgentVectorController {

    private final VectorSyncScheduler scheduler;

    public AgentVectorController(VectorSyncScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/sync")
    public Map<String, Object> sync() {
        scheduler.sync();
        return Map.of("status", "ok");
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("lastSyncTime", scheduler.getLastSyncTime().toString());
    }
}
