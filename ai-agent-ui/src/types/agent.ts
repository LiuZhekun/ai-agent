export type AgentEventType =
  | "HEARTBEAT"
  | "THINKING_SUMMARY"
  | "CLARIFICATION_REQUIRED"
  | "SLOT_UPDATE"
  | "EXECUTION_CONFIRM_REQUIRED"
  | "TOOL_TRACE"
  | "CHART_PAYLOAD"
  | "FINAL_ANSWER"
  | "ERROR"
  | "COMPLETED";

export interface AgentEvent {
  eventId: string;
  sessionId: string;
  type: AgentEventType;
  timestamp: string;
  payload: unknown;
  traceId?: string;
}

export interface ChatMessage {
  role: "USER" | "ASSISTANT" | "SYSTEM";
  content: string;
  timestamp?: string;
}

export interface ToolCallInfo {
  toolName: string;
  toolGroup: string;
  parameters: Record<string, unknown>;
  result?: unknown;
  status: "STARTED" | "SUCCESS" | "FAILED";
  durationMs?: number;
  errorMessage?: string;
}

export interface ClarificationQuestion {
  slotName: string;
  question: string;
  options?: string[];
  required: boolean;
}

export interface SlotState {
  slotName: string;
  required: boolean;
  value?: unknown;
  confidence: number;
  status: "FILLED" | "MISSING" | "AMBIGUOUS";
}

export interface ExecutionConfirmation {
  approvalId: string;
  approved: boolean;
  comment?: string;
}
