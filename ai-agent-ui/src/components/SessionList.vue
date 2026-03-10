<template>
  <aside class="session-list">
    <el-button type="primary" @click="create">新建会话</el-button>
    <ul>
      <li v-for="id in store.sessions" :key="id">
        <span @click="store.switchSession(id)">{{ id }}</span>
        <el-button text type="danger" @click="store.deleteSession(id)">删</el-button>
      </li>
    </ul>
  </aside>
</template>

<script setup lang="ts">
import { useSessionStore } from "../stores/session";

const store = useSessionStore();
if (!store.currentSessionId) store.createSession();
const create = () => store.createSession();
</script>
