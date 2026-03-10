import type { AgentEvent } from "../types/agent";

export interface SendMessageOptions {
  onEvent: (event: AgentEvent) => void;
  onError?: (error: unknown) => void;
}

const processedEventIds = new Set<string>();
const HEARTBEAT_TIMEOUT_MS = 45_000;

/**
 * 向后端发起 SSE 对话请求，通过 ReadableStream 实时消费事件。
 * 支持心跳检测、eventId 去重、409 并发拒绝。
 */
export async function sendMessage(
  sessionId: string,
  message: string,
  options: SendMessageOptions,
  extra?: { slotAnswers?: Record<string, unknown>; approval?: { approvalId: string; approved: boolean } }
): Promise<void> {
  let heartbeatTimer: ReturnType<typeof setTimeout> | null = null;

  const resetHeartbeat = () => {
    if (heartbeatTimer) clearTimeout(heartbeatTimer);
    heartbeatTimer = setTimeout(() => {
      options.onError?.(new Error("心跳超时，连接可能已断开"));
    }, HEARTBEAT_TIMEOUT_MS);
  };

  try {
    const body: Record<string, unknown> = { sessionId, message };
    if (extra?.slotAnswers) body.slotAnswers = extra.slotAnswers;
    if (extra?.approval) body.approval = extra.approval;

    const response = await fetch("/api/agent/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    if (response.status === 409) {
      throw new Error("当前会话正在处理中，请稍后重试");
    }
    if (!response.ok) {
      throw new Error(`请求失败: ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) throw new Error("无法获取响应流");

    const decoder = new TextDecoder();
    let buffer = "";
    resetHeartbeat();

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() ?? "";

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed.startsWith("data:")) continue;
        const payload = trimmed.slice(5).trim();
        if (!payload) continue;

        try {
          const event = JSON.parse(payload) as AgentEvent;
          resetHeartbeat();
          if (processedEventIds.has(event.eventId)) continue;
          processedEventIds.add(event.eventId);
          options.onEvent(event);
        } catch {
          // 跳过无法解析的行
        }
      }
    }
  } catch (error) {
    options.onError?.(error);
  } finally {
    if (heartbeatTimer) clearTimeout(heartbeatTimer);
  }
}
