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
 * Agent 工具回调提供者 —— 自动发现、注册并装饰所有 Agent 工具。
 *
 * <h3>核心流程（构造时一次性完成）</h3>
 * <ol>
 *   <li><b>扫描</b> —— 从 Spring {@link ApplicationContext} 中查找所有标注了
 *       {@link AgentTool @AgentTool} 的 Bean；</li>
 *   <li><b>提取</b> —— 对每个 Bean 使用 Spring AI 的
 *       {@link MethodToolCallbackProvider} 提取其 {@code @Tool} 方法，
 *       生成原始 {@link ToolCallback}；</li>
 *   <li><b>装饰</b> —— 将每个原始 Callback 包装为 {@link ToolCallbackDecorator}，
 *       统一注入限流（{@link java.util.concurrent.Semaphore}）、超时、重试、
 *       拦截器链（{@link ToolCallbackInterceptor}）和结果格式化能力；</li>
 *   <li><b>元数据</b> —— 同步构建 {@link ToolMetadata} 列表，
 *       供 {@link io.github.aiagent.core.agent.advisor.ClarificationAdvisor}
 *       和 {@link io.github.aiagent.core.agent.advisor.PlanningAdvisor} 使用。</li>
 * </ol>
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>标记 {@code @Primary} 以覆盖 Spring AI 默认的 ToolCallbackProvider，
 *       确保 {@link io.github.aiagent.core.agent.AgentEngine} 注入的是经过装饰的版本；</li>
 *   <li>并发上限硬编码为 5，后续可提取为配置项。</li>
 * </ul>
 *
 * @see AgentTool
 * @see ToolCallbackDecorator
 * @see ToolCallbackInterceptor
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

    /**
     * 返回经过装饰的所有工具回调数组，供 {@link io.github.aiagent.core.agent.AgentEngine}
     * 传递给 Spring AI {@code ChatClient}。
     *
     * <p>数组在构造时已完全初始化，调用此方法不会触发额外的扫描或实例化。</p>
     *
     * @return 装饰后的 {@link ToolCallback} 数组
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        return callbacks;
    }

    /**
     * 返回所有已注册工具的元数据列表，供 Advisor（如澄清、规划）进行参数检查和任务分解。
     *
     * @return 工具元数据列表（与 {@link #getToolCallbacks()} 中的工具一一对应）
     */
    public List<ToolMetadata> getToolMetadatas() {
        return toolMetadatas;
    }
}
