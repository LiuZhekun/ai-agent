package io.github.aiagent.core.agent.advisor;

import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.agent.ExecutionTrace;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * 轨迹采集 Advisor —— 为每次工具调用生成结构化的执行轨迹。
 *
 * <h3>在 Advisor 链中的位置</h3>
 * <p>{@code @Order(40)}，作为最后执行的 Advisor，在规划/澄清等业务逻辑之后采集轨迹，
 * 避免在被中断的请求中产生无意义的轨迹记录。</p>
 *
 * <h3>使用方式</h3>
 * <p>在工具调用前调用 {@link #startTrace} 创建初始轨迹并挂载到 Session，
 * 在工具调用后（无论成功或失败）调用 {@link #finishTrace} 填充执行结果和耗时。
 * 采集到的轨迹通过 {@link AgentSession#getExecutionTraces()} 对外暴露。</p>
 *
 * @see ExecutionTrace
 * @see AgentSession#getExecutionTraces()
 */
@Component
@Order(40)
public class TraceAdvisor {

    /**
     * 在工具调用开始前创建一条新的执行轨迹并挂载到会话中。
     *
     * @param session   当前会话上下文
     * @param toolName  被调用的工具名称
     * @param toolGroup 工具所属的分组名称（对应 {@link io.github.aiagent.core.tool.annotation.AgentTool#name()}）
     * @return 新创建的轨迹对象，状态为 {@link ExecutionTrace.TraceStatus#STARTED}
     */
    public ExecutionTrace startTrace(AgentSession session, String toolName, String toolGroup) {
        ExecutionTrace trace = new ExecutionTrace();
        trace.setTraceId(UUID.randomUUID().toString());
        trace.setToolName(toolName);
        trace.setToolGroup(toolGroup);
        trace.setStatus(ExecutionTrace.TraceStatus.STARTED);
        trace.setStartTime(Instant.now());
        session.getExecutionTraces().add(trace);
        return trace;
    }

    /**
     * 标记轨迹执行结束，填充状态、耗时和可能的错误信息。
     *
     * @param trace        要完善的轨迹对象（必须已通过 {@link #startTrace} 创建）
     * @param success      执行是否成功
     * @param errorMessage 失败时的错误描述；成功时可传 {@code null}
     */
    public void finishTrace(ExecutionTrace trace, boolean success, String errorMessage) {
        trace.setStatus(success ? ExecutionTrace.TraceStatus.SUCCESS : ExecutionTrace.TraceStatus.FAILED);
        long duration = Instant.now().toEpochMilli() - trace.getStartTime().toEpochMilli();
        trace.setDurationMs(Math.max(duration, 0));
        trace.setErrorMessage(errorMessage);
    }
}
