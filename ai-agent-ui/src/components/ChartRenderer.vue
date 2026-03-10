<template>
  <div ref="el" class="chart"></div>
</template>

<script setup lang="ts">
import * as echarts from "echarts";
import { onMounted, ref, watch } from "vue";

const props = defineProps<{ optionJson: string }>();
const el = ref<HTMLDivElement | null>(null);

const render = () => {
  if (!el.value) return;
  const chart = echarts.init(el.value);
  chart.setOption(JSON.parse(props.optionJson || "{}"));
};

onMounted(render);
watch(() => props.optionJson, render);
</script>
