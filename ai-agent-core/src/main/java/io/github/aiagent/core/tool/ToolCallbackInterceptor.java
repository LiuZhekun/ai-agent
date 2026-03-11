package io.github.aiagent.core.tool;

/**
 * 工具调用拦截器 —— SPI 扩展点，用于在工具调用前后注入自定义逻辑。
 *
 * <h3>扩展方式</h3>
 * <p>实现此接口并注册为 Spring Bean 即可自动生效。
 * {@link AgentToolCallbackProvider} 会在构造时收集所有
 * {@code ToolCallbackInterceptor} Bean，并将它们注入到每个
 * {@link ToolCallbackDecorator} 中。多个拦截器按 Spring 的默认排序执行。</p>
 *
 * <h3>典型用途</h3>
 * <ul>
 *   <li>参数翻译 —— 在 {@link #beforeCall} 中将自然语言值替换为系统存储值
 *       （如 "技术部" → departmentId=3），返回修改后的 JSON；</li>
 *   <li>日志审计 —— 在 {@link #beforeCall} 中记录调用入参；</li>
 *   <li>参数脱敏 —— 在 {@link #beforeCall} 中对敏感字段加密；</li>
 *   <li>结果改写 —— 在 {@link #afterCall} 中对输出做后处理或脱敏；</li>
 *   <li>访问控制 —— 在 {@link #beforeCall} 中检查权限，不满足时抛出异常中断调用。</li>
 * </ul>
 *
 * @see ToolCallbackDecorator
 */
public interface ToolCallbackInterceptor {

    /**
     * 工具调用前置拦截。
     *
     * <p>可用于参数翻译、日志记录、参数校验或访问控制。
     * 返回值为（可能被修改的）工具输入 JSON，将替换原始输入传递给后续拦截器和工具执行。
     * 若不需要修改输入，直接返回原 {@code toolInput}；若需中断调用，直接抛出运行时异常。</p>
     *
     * @param toolName  被调用的工具名称
     * @param toolInput JSON 格式的工具输入参数
     * @return 处理后的工具输入 JSON（可直接返回原 {@code toolInput} 不做修改）
     */
    String beforeCall(String toolName, String toolInput);

    /**
     * 工具调用后置拦截。
     *
     * <p>可对输出结果进行改写、脱敏或追加信息。返回值将替换原始输出继续传递给后续拦截器和格式化器。</p>
     *
     * @param toolName   被调用的工具名称
     * @param toolInput  JSON 格式的工具输入参数
     * @param toolOutput 工具执行的原始输出
     * @return 处理后的输出结果（可直接返回原 {@code toolOutput} 不做修改）
     */
    String afterCall(String toolName, String toolInput, String toolOutput);
}
