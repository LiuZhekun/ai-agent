import type { AgentEvent } from "../types/agent";

export interface SendMessageOptions {
  onEvent: (event: AgentEvent) => void;
  onError?: (error: unknown) => void;
}

// 简单幂等集合：防止网络重试/重复解析导致 UI 重复渲染同一事件。
const processedEventIds = new Set<string>();

export async function sendMessage(
  sessionId: string,
  message: string,
  options: SendMessageOptions
): Promise<void> {
  try {
    // 当前实现直接读取完整 SSE 文本，再按 data: 行解析事件。
    const response = await fetch("/api/agent/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ sessionId, message })
    });
    if (response.status === 409) {
      throw new Error("会话忙，请稍后重试");
    }
    const text = await response.text();
    const lines = text.split("\n").filter((v) => v.startsWith("data:"));
    for (const line of lines) {
      const payload = line.slice(5).trim();
      if (!payload) continue;
      const event = JSON.parse(payload) as AgentEvent;
      if (processedEventIds.has(event.eventId)) continue;
      processedEventIds.add(event.eventId);
      options.onEvent(event);
    }
  } catch (error) {
    options.onError?.(error);
  }
}
