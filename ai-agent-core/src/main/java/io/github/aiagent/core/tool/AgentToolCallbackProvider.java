package io.github.aiagent.core.tool;

import io.github.aiagent.core.tool.annotation.AgentTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Agent 工具回调 Provider。
 */
@Component
public class AgentToolCallbackProvider implements ToolCallbackProvider {

    private final List<ToolMetadata> toolMetadatas = new ArrayList<>();
    private final ToolCallback[] callbacks;

    public AgentToolCallbackProvider(
            ApplicationContext applicationContext,
            List<ToolCallbackInterceptor> interceptors,
            ToolResultFormatter formatter) {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(AgentTool.class);
        List<ToolCallback> callbackList = new ArrayList<>();
        for (Object bean : beans.values()) {
            AgentTool annotation = AnnotatedElementUtils.findMergedAnnotation(bean.getClass(), AgentTool.class);
            if (annotation == null) {
                continue;
            }
            ToolMetadata metadata = new ToolMetadata();
            metadata.setGroupName(annotation.name());
            metadata.setGroupDescription(annotation.description());
            metadata.setRiskLevel(annotation.riskLevel());
            metadata.setBeanClass(bean.getClass());
            metadata.setToolName(bean.getClass().getSimpleName());
            metadata.setToolDescription("Auto-discovered tool bean");
            toolMetadatas.add(metadata);
        }
        this.callbacks = callbackList.toArray(new ToolCallback[0]);
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return callbacks;
    }

    public List<ToolMetadata> getToolMetadatas() {
        return toolMetadatas;
    }

    public ToolCallback decorate(
            ToolCallback delegate,
            List<ToolCallbackInterceptor> interceptors,
            io.github.aiagent.core.metrics.AgentMetrics metrics,
            int timeoutSeconds,
            int maxRetries,
            int maxConcurrency,
            ToolResultFormatter formatter) {
        return new ToolCallbackDecorator(
                delegate,
                interceptors == null ? List.of() : interceptors,
                metrics,
                timeoutSeconds,
                maxRetries,
                new Semaphore(maxConcurrency),
                formatter);
    }
}
