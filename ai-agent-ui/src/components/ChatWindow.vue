<template>
  <main class="chat-window">
    <div class="messages">
      <MessageBubble v-for="(m, i) in chat.messages" :key="i" :message="m" />
    </div>
    <ThinkingPanel />
    <ToolTraceTimeline />
    <div class="input-area">
      <el-input v-model="input" placeholder="请输入消息" @keyup.enter="submit" />
      <el-button type="primary" @click="submit" :loading="chat.isLoading">发送</el-button>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useChatStore } from "../stores/chat";
import { useSessionStore } from "../stores/session";
import MessageBubble from "./MessageBubble.vue";
import ThinkingPanel from "./ThinkingPanel.vue";
import ToolTraceTimeline from "./ToolTraceTimeline.vue";

const input = ref("");
const chat = useChatStore();
const session = useSessionStore();

const submit = async () => {
  if (!input.value.trim()) return;
  await chat.sendMessage(session.currentSessionId, input.value);
  input.value = "";
};
</script>
