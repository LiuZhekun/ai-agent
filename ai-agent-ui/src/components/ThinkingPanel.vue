<template>
  <el-collapse v-if="chat.thinkingSummary">
    <el-collapse-item title="思考摘要">
      <div class="thinking-content">{{ chat.thinkingSummary }}</div>
    </el-collapse-item>
  </el-collapse>
</template>

<script setup lang="ts">
import { useChatStore } from "../stores/chat";
const chat = useChatStore();
</script>

<style scoped>
.thinking-content {
  white-space: pre-wrap;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
}
</style>
