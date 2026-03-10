package io.github.aiagent.core.tool;

import io.github.aiagent.core.metrics.AgentMetrics;
import io.github.aiagent.core.tool.annotation.AgentTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Agent 工具回调 Provider。
 * 自动扫描 @AgentTool 标注的 Bean，提取其 @Tool 方法注册为 ToolCallback，
 * 并通过 ToolCallbackDecorator 统一包装超时/限流/拦截/审计能力。
 */
@Component
@Primary
public class AgentToolCallbackProvider implements ToolCallbackProvider {

    private final List<ToolMetadata> toolMetadatas = new ArrayList<>();
    private final ToolCallback[] callbacks;

    public AgentToolCallbackProvider(
            ApplicationContext applicationContext,
            List<ToolCallbackInterceptor> interceptors,
            ToolResultFormatter formatter,
            @Nullable AgentMetrics metrics) {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(AgentTool.class);
        List<ToolCallback> callbackList = new ArrayList<>();
        Semaphore semaphore = new Semaphore(5);

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
            metadata.setToolDescription(annotation.description());
            toolMetadatas.add(metadata);

            ToolCallbackProvider methodProvider = MethodToolCallbackProvider.builder()
                    .toolObjects(bean)
                    .build();
            for (ToolCallback cb : methodProvider.getToolCallbacks()) {
                callbackList.add(new ToolCallbackDecorator(
                        cb, interceptors, metrics, 10, 1, semaphore, formatter));
            }
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
}
