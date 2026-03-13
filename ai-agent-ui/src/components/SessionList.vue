<template>
  <aside class="session-list">
    <el-button type="primary" @click="create">新建会话</el-button>
    <ul>
      <li v-for="id in store.sessions" :key="id">
        <span @click="switchTo(id)">{{ id }}</span>
        <el-button text type="danger" @click="remove(id)">删</el-button>
      </li>
    </ul>
  </aside>
</template>

<script setup lang="ts">
import { useSessionStore } from "../stores/session";
import { useChatStore } from "../stores/chat";

const store = useSessionStore();
const chat = useChatStore();

if (!store.currentSessionId) {
  const id = store.createSession();
  chat.setActiveSession(id);
}

const create = () => {
  const id = store.createSession();
  chat.setActiveSession(id);
};

const switchTo = (id: string) => {
  store.switchSession(id);
  chat.setActiveSession(id);
};

const remove = (id: string) => {
  store.deleteSession(id);
  chat.removeSession(id);
  if (store.currentSessionId) {
    chat.setActiveSession(store.currentSessionId);
  }
};
</script>
