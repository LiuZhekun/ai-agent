import { defineStore } from "pinia";
import { sendMessage } from "../api/agent";
import type { AgentEvent, ChatMessage, ToolCallInfo, SlotState } from "../types/agent";

type ClarificationQuestion = {
  slotName: string;
  question: string;
  options?: string[];
  required: boolean;
};

export const useChatStore = defineStore("chat", {
  state: () => ({
    sessionMessages: {} as Record<string, ChatMessage[]>,
    currentSessionId: "",
    isLoading: false,
    thinkingSummary: "" as string,
    toolTraces: [] as ToolCallInfo[],
    clarificationState: null as null | "waiting" | "confirming",
    clarificationQuestions: [] as ClarificationQuestion[],
    slotStates: [] as SlotState[],
    confirmAction: null as null | { approvalId: string; actionSummary: string; riskLevel: string },
  }),
  getters: {
    messages(state): ChatMessage[] {
      if (!state.currentSessionId) return [];
      return state.sessionMessages[state.currentSessionId] ?? [];
    },
  },
  actions: {
    setActiveSession(sessionId: string) {
      this.currentSessionId = sessionId;
      this.resetRuntimeState();
      if (!sessionId) return;
      this.ensureSessionMessages(sessionId);
    },

    removeSession(sessionId: string) {
      delete this.sessionMessages[sessionId];
      if (this.currentSessionId === sessionId) {
        this.setActiveSession("");
      }
    },

    async sendUserMessage(sessionId: string, message: string) {
      this.setActiveSession(sessionId);
      const messages = this.ensureSessionMessages(sessionId);
      messages.push({ role: "USER", content: message });
      this.isLoading = true;
      this.thinkingSummary = "";
      this.toolTraces = [];
      await sendMessage(sessionId, message, {
        onEvent: (e) => this.dispatchEvent(e),
        onError: (e) =>
          this.messages.push({ role: "SYSTEM", content: String(e) }),
      });
      this.isLoading = false;
    },

    async submitSlotAnswers(sessionId: string, answers: Record<string, unknown>) {
      this.setActiveSession(sessionId);
      this.clarificationState = null;
      this.isLoading = true;
      await sendMessage(sessionId, "", {
        onEvent: (e) => this.dispatchEvent(e),
        onError: (e) =>
          this.messages.push({ role: "SYSTEM", content: String(e) }),
      }, { slotAnswers: answers });
      this.isLoading = false;
    },

    async submitApproval(sessionId: string, approvalId: string, approved: boolean) {
      this.setActiveSession(sessionId);
      this.clarificationState = null;
      this.isLoading = true;
      await sendMessage(sessionId, "", {
        onEvent: (e) => this.dispatchEvent(e),
        onError: (e) =>
          this.messages.push({ role: "SYSTEM", content: String(e) }),
      }, { approval: { approvalId, approved } });
      this.isLoading = false;
    },

    dispatchEvent(event: AgentEvent) {
      const messages = this.currentSessionId ? this.ensureSessionMessages(this.currentSessionId) : [];
      switch (event.type) {
        case "HEARTBEAT":
          break;
        case "THINKING_SUMMARY":
          this.thinkingSummary = typeof event.payload === "object" && event.payload !== null
            ? (event.payload as Record<string, unknown>).summary as string ?? ""
            : String(event.payload ?? "");
          break;
        case "TOOL_TRACE":
          if (event.payload) this.toolTraces.push(event.payload as ToolCallInfo);
          break;
        case "CLARIFICATION_REQUIRED":
          this.clarificationState = "waiting";
          if (event.payload && typeof event.payload === "object") {
            const p = event.payload as Record<string, unknown>;
            this.clarificationQuestions = (p.questions as typeof this.clarificationQuestions) ?? [];
          }
          break;
        case "SLOT_UPDATE":
          if (event.payload) this.slotStates = event.payload as SlotState[];
          break;
        case "EXECUTION_CONFIRM_REQUIRED":
          this.clarificationState = "confirming";
          if (event.payload && typeof event.payload === "object") {
            this.confirmAction = event.payload as typeof this.confirmAction;
          }
          break;
        case "CHART_PAYLOAD":
          messages.push({ role: "ASSISTANT", content: "__CHART__" + JSON.stringify(event.payload) });
          break;
        case "FINAL_ANSWER":
          messages.push({ role: "ASSISTANT", content: String(event.payload ?? "") });
          break;
        case "ERROR":
          messages.push({
            role: "SYSTEM",
            content: typeof event.payload === "object" && event.payload !== null
              ? (event.payload as Record<string, unknown>).message as string ?? "未知错误"
              : String(event.payload ?? "未知错误"),
          });
          break;
        case "COMPLETED":
          break;
      }
    },

    ensureSessionMessages(sessionId: string) {
      if (!this.sessionMessages[sessionId]) {
        this.sessionMessages[sessionId] = [];
      }
      return this.sessionMessages[sessionId];
    },

    resetRuntimeState() {
      this.isLoading = false;
      this.thinkingSummary = "";
      this.toolTraces = [];
      this.clarificationState = null;
      this.clarificationQuestions = [];
      this.slotStates = [];
      this.confirmAction = null;
    },
  },
});
