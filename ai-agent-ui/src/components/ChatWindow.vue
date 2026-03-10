<template>
  <main class="chat-window">
    <div class="messages">
      <MessageBubble v-for="(m, i) in chat.messages" :key="i" :message="m" />
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

    <div class="input-area" v-if="!chat.clarificationState">
      <el-input v-model="input" placeholder="请输入消息" @keyup.enter="submit" />
      <el-button type="primary" @click="submit" :loading="chat.isLoading">发送</el-button>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from "vue";
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

const chartPayloads = computed(() =>
  chat.messages
    .filter((m) => m.content.startsWith("__CHART__"))
    .map((m) => {
      try { return JSON.parse(m.content.slice(9)); } catch { return null; }
    })
    .filter(Boolean)
);

const submit = async () => {
  if (!input.value.trim()) return;
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
</script>

<style scoped>
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
