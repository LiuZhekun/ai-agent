<!--
  ChartRenderer — ECharts 图表渲染组件
  接收已解析的 ECharts option 对象（由 ChatWindow 从 __CHART__ 消息中解析），
  使用 echarts.setOption() 渲染图表。
  注意：接收的是 JS 对象而非 JSON 字符串，避免重复序列化/反序列化。
-->
<template>
  <div ref="el" class="chart"></div>
</template>

<script setup lang="ts">
import * as echarts from "echarts";
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

onMounted(render);
watch(() => props.option, render, { deep: true });

onBeforeUnmount(() => {
  chartInstance?.dispose();
  chartInstance = null;
});
</script>
