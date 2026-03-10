<template>
  <div class="app-layout">
    <SessionList />
    <ChatWindow />
  </div>
</template>

<script setup lang="ts">
import SessionList from "./components/SessionList.vue";
import ChatWindow from "./components/ChatWindow.vue";
</script>
