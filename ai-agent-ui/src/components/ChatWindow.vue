<template>
  <main class="chat-window">
    <div ref="conversationRef" class="conversation-scroll">
      <div class="messages">
        <MessageBubble v-for="(m, i) in visibleMessages" :key="i" :message="m" />
      </div>
      <ThinkingPanel />
      <ToolTraceTimeline />
      <ChartRenderer
        v-for="(c, i) in chartPayloads"
        :key="'chart-' + i"
        :option="c"
      />

      <!-- 澄清态：展示追问 -->
      <div v-if="chat.clarificationState === 'waiting'" class="clarification-panel">
        <p>请补充以下信息：</p>
        <div v-for="q in chat.clarificationQuestions" :key="q.slotName" class="slot-question">
          <label>{{ q.question }}</label>
          <el-input v-model="slotAnswerMap[q.slotName]" :placeholder="q.slotName" />
        </div>
        <el-button type="primary" @click="submitSlots">提交</el-button>
      </div>

      <!-- 确认态：写操作确认 -->
      <div v-if="chat.clarificationState === 'confirming' && chat.confirmAction" class="confirm-panel">
        <p>{{ chat.confirmAction.actionSummary }}</p>
        <el-button type="primary" @click="approve(true)">确认执行</el-button>
        <el-button @click="approve(false)">取消</el-button>
      </div>
    </div>

    <div class="input-area" v-if="!chat.clarificationState">
      <el-input v-model="input" placeholder="请输入消息" @keyup.enter="submit" />
      <el-button type="primary" @click="submit" :loading="chat.isLoading">发送</el-button>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from "vue";
import { useChatStore } from "../stores/chat";
import { useSessionStore } from "../stores/session";
import MessageBubble from "./MessageBubble.vue";
import ThinkingPanel from "./ThinkingPanel.vue";
import ToolTraceTimeline from "./ToolTraceTimeline.vue";
import ChartRenderer from "./ChartRenderer.vue";

const input = ref("");
const chat = useChatStore();
const session = useSessionStore();
const slotAnswerMap = reactive<Record<string, string>>({});
const conversationRef = ref<HTMLElement | null>(null);

const visibleMessages = computed(() =>
  chat.messages.filter((m) => !m.content.startsWith("__CHART__"))
);

const CHART_JSON_HINTS = ["series", "xAxis", "yAxis", "legend", "tooltip", "title"];

const safeJsonParse = (text: string): Record<string, unknown> | null => {
  if (!text.trim()) return null;
  const normalized = text
    .replace(/[\u201C\u201D]/g, "\"")
    .replace(/[；]/g, ";")
    .replace(/,\s*([}\]])/g, "$1");
  try {
    const parsed = JSON.parse(normalized);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return null;
    return parsed as Record<string, unknown>;
  } catch {
    return null;
  }
};

const looksLikeEchartOption = (obj: Record<string, unknown>) =>
  CHART_JSON_HINTS.some((k) => k in obj);

const extractChartFromMarkdown = (content: string): Record<string, unknown> | null => {
  const markdownJsonCodeBlock = content.match(/```(?:json)?\s*([\s\S]*?)```/i);
  if (markdownJsonCodeBlock?.[1]) {
    const parsed = safeJsonParse(markdownJsonCodeBlock[1]);
    if (parsed && looksLikeEchartOption(parsed)) return parsed;
  }

  const start = content.indexOf("{");
  const end = content.lastIndexOf("}");
  if (start >= 0 && end > start) {
    const possibleJson = content.slice(start, end + 1);
    const parsed = safeJsonParse(possibleJson);
    if (parsed && looksLikeEchartOption(parsed)) return parsed;
  }

  return null;
};

const chartPayloads = computed(() => {
  const fromChartEvent = chat.messages
    .filter((m) => m.content.startsWith("__CHART__"))
    .map((m) => safeJsonParse(m.content.slice(9)))
    .filter((v): v is Record<string, unknown> => !!v);

  const fromAssistantText = chat.messages
    .filter((m) => m.role === "ASSISTANT" && !m.content.startsWith("__CHART__"))
    .map((m) => extractChartFromMarkdown(m.content))
    .filter((v): v is Record<string, unknown> => !!v);

  return [...fromChartEvent, ...fromAssistantText];
});

const submit = async () => {
  if (!input.value.trim()) return;
  if (!session.currentSessionId) {
    session.createSession();
  }
  await chat.sendUserMessage(session.currentSessionId, input.value);
  input.value = "";
};

const submitSlots = () => {
  chat.submitSlotAnswers(session.currentSessionId, { ...slotAnswerMap });
};

const approve = (approved: boolean) => {
  if (!chat.confirmAction) return;
  chat.submitApproval(session.currentSessionId, chat.confirmAction.approvalId, approved);
};

const scrollToBottom = () => {
  if (!conversationRef.value) return;
  conversationRef.value.scrollTop = conversationRef.value.scrollHeight;
};

watch(
  () => session.currentSessionId,
  (sessionId) => {
    chat.setActiveSession(sessionId);
    nextTick(scrollToBottom);
  },
  { immediate: true },
);

watch(
  () => [
    chat.messages.length,
    chat.thinkingSummary,
    chat.toolTraces.length,
    chat.clarificationState,
    chartPayloads.value.length,
  ],
  () => nextTick(scrollToBottom),
);
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.conversation-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}

.input-area {
  display: flex;
  gap: 8px;
  padding-top: 12px;
}

.clarification-panel,
.confirm-panel {
  padding: 12px;
  margin: 8px 0;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}
.slot-question {
  margin: 8px 0;
}
.slot-question label {
  display: block;
  margin-bottom: 4px;
  font-size: 13px;
  color: #333;
}
</style>
