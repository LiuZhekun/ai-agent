<!--
  ChartRenderer — ECharts 图表渲染组件
  接收已解析的 ECharts option 对象（由 ChatWindow 从 __CHART__ 消息中解析），
  使用 echarts.setOption() 渲染图表。
  注意：接收的是 JS 对象而非 JSON 字符串，避免重复序列化/反序列化。
-->
<template>
  <div class="chart-card">
    <div class="chart-actions">
      <el-button size="small" @click="downloadPng">下载 PNG</el-button>
      <el-button size="small" @click="downloadJson">下载 JSON</el-button>
    </div>
    <div ref="el" class="chart"></div>
  </div>
</template>

<script setup lang="ts">
import * as echarts from "echarts";
import { ElMessage } from "element-plus";
import { onMounted, onBeforeUnmount, ref, watch } from "vue";

const props = defineProps<{ option: Record<string, any> }>();
const el = ref<HTMLDivElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

const render = () => {
  if (!el.value) return;
  if (!chartInstance) {
    chartInstance = echarts.init(el.value);
  }
  chartInstance.setOption(props.option || {});
};

const resize = () => {
  chartInstance?.resize();
};

const createFileName = (suffix: string) => {
  const titleText =
    (props.option?.title as { text?: string } | undefined)?.text?.trim() || "chart";
  const safeTitle = titleText.replace(/[\\/:*?"<>|]/g, "_");
  return `${safeTitle}-${new Date().toISOString().replace(/[:.]/g, "-")}.${suffix}`;
};

const triggerDownload = (url: string, filename: string) => {
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
};

const downloadPng = () => {
  if (!chartInstance) {
    ElMessage.warning("图表尚未渲染完成");
    return;
  }
  const dataUrl = chartInstance.getDataURL({
    type: "png",
    pixelRatio: 2,
    backgroundColor: "#fff",
  });
  triggerDownload(dataUrl, createFileName("png"));
};

const downloadJson = () => {
  const json = JSON.stringify(props.option || {}, null, 2);
  const blob = new Blob([json], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  triggerDownload(url, createFileName("json"));
  URL.revokeObjectURL(url);
};

onMounted(() => {
  render();
  window.addEventListener("resize", resize);
});
watch(() => props.option, render, { deep: true });

onBeforeUnmount(() => {
  window.removeEventListener("resize", resize);
  chartInstance?.dispose();
  chartInstance = null;
});
</script>

<style scoped>
.chart-card {
  margin: 10px 0;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.chart-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
