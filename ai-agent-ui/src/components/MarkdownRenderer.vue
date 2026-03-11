<!--
  MarkdownRenderer — Markdown 渲染组件
  使用 marked 将 Markdown 文本转为 HTML，并通过基础标签白名单过滤防御 XSS。
  生产环境建议额外引入 DOMPurify（npm install dompurify）替换 sanitizeHtml 获得更完善防护。
-->
<template>
  <div class="markdown-body" v-html="safeHtml"></div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { marked } from "marked";

const props = defineProps<{ content: string }>();

const ALLOWED_TAGS = new Set([
  "p", "br", "strong", "em", "b", "i", "u", "s", "del",
  "h1", "h2", "h3", "h4", "h5", "h6",
  "ul", "ol", "li", "blockquote", "pre", "code",
  "table", "thead", "tbody", "tr", "th", "td",
  "a", "img", "hr", "span", "div", "sup", "sub",
]);

/**
 * 基础 HTML 标签白名单过滤：
 * - 移除不在白名单中的标签（如 script、iframe、style、object 等）
 * - 移除所有标签上的事件处理属性（on* 系列）
 * - 移除 javascript: 协议链接
 */
function sanitizeHtml(html: string): string {
  return html
    .replace(/<\/?([a-zA-Z][a-zA-Z0-9]*)\b[^>]*>/g, (match, tag) => {
      if (!ALLOWED_TAGS.has(tag.toLowerCase())) return "";
      return match
        .replace(/\s+on\w+\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)/gi, "")
        .replace(/href\s*=\s*["']?\s*javascript:/gi, 'href="');
    });
}

const safeHtml = computed(() => {
  const raw = marked.parse(props.content || "") as string;
  return sanitizeHtml(raw);
});
</script>
