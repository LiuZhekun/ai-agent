<template>
  <el-timeline v-if="chat.toolTraces.length > 0">
    <el-timeline-item
      v-for="(trace, i) in chat.toolTraces"
      :key="i"
      :type="statusColor(trace.status)"
      :timestamp="trace.durationMs ? trace.durationMs + 'ms' : ''"
      placement="top"
    >
      <div class="trace-header">
        <strong>{{ trace.toolName }}</strong>
        <el-tag :type="statusTag(trace.status)" size="small">{{ trace.status }}</el-tag>
      </div>
      <div v-if="trace.parameters" class="trace-params">
        参数: <code>{{ JSON.stringify(trace.parameters) }}</code>
      </div>
      <div v-if="trace.result" class="trace-result">
        结果: <code>{{ typeof trace.result === 'string' ? trace.result : JSON.stringify(trace.result) }}</code>
      </div>
      <div v-if="trace.errorMessage" class="trace-error">
        错误: {{ trace.errorMessage }}
      </div>
    </el-timeline-item>
  </el-timeline>
</template>

<script setup lang="ts">
import { useChatStore } from "../stores/chat";

const chat = useChatStore();

const statusColor = (s: string) =>
  s === "SUCCESS" ? "success" : s === "FAILED" ? "danger" : "primary";
const statusTag = (s: string) =>
  s === "SUCCESS" ? "success" : s === "FAILED" ? "danger" : "info";
</script>

<style scoped>
.trace-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.trace-params,
.trace-result {
  margin-top: 4px;
  font-size: 12px;
  color: #666;
}
.trace-result code,
.trace-params code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  word-break: break-all;
}
.trace-error {
  margin-top: 4px;
  color: #f56c6c;
  font-size: 12px;
}
</style>
