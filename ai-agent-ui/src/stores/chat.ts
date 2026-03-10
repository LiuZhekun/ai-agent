import { defineStore } from "pinia";
import { sendMessage } from "../api/agent";
import type { AgentEvent, ChatMessage } from "../types/agent";

// 聊天状态中心：统一管理消息列表、加载态与澄清/确认状态。
export const useChatStore = defineStore("chat", {
  state: () => ({
    messages: [] as ChatMessage[],
    isLoading: false,
    clarificationState: null as null | "waiting" | "confirming"
  }),
  actions: {
    async sendMessage(sessionId: string, message: string) {
      // 先乐观写入用户消息，再消费后端 SSE 事件流。
      this.messages.push({ role: "USER", content: message });
      this.isLoading = true;
      await sendMessage(sessionId, message, {
        onEvent: (e) => this.dispatchEvent(e),
        onError: (e) =>
          this.messages.push({ role: "SYSTEM", content: String(e) })
      });
      this.isLoading = false;
    },
    submitSlotAnswers() {
      this.clarificationState = null;
    },
    submitApproval() {
      this.clarificationState = null;
    },
    dispatchEvent(event: AgentEvent) {
      // 事件分发：按事件类型驱动 UI 状态更新。
      if (event.type === "FINAL_ANSWER") {
        this.messages.push({ role: "ASSISTANT", content: String(event.payload ?? "") });
      } else if (event.type === "CLARIFICATION_REQUIRED") {
        this.clarificationState = "waiting";
      } else if (event.type === "EXECUTION_CONFIRM_REQUIRED") {
        this.clarificationState = "confirming";
      }
    }
  }
});
