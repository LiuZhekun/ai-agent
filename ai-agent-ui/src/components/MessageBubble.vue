<template>
  <div class="bubble" :class="message.role.toLowerCase()">
    <MarkdownRenderer v-if="message.role !== 'USER'" :content="message.content" />
    <span v-else>{{ message.content }}</span>
  </div>
</template>

<script setup lang="ts">
import type { ChatMessage } from "../types/agent";
import MarkdownRenderer from "./MarkdownRenderer.vue";

defineProps<{ message: ChatMessage }>();
</script>
