import { defineStore } from "pinia";

export const useSessionStore = defineStore("session", {
  state: () => ({
    sessions: [] as string[],
    currentSessionId: ""
  }),
  actions: {
    createSession() {
      const id = crypto.randomUUID();
      this.sessions.push(id);
      this.currentSessionId = id;
      return id;
    },
    switchSession(id: string) {
      this.currentSessionId = id;
    },
    deleteSession(id: string) {
      this.sessions = this.sessions.filter((s) => s !== id);
      if (this.currentSessionId === id) {
        this.currentSessionId = this.sessions[0] ?? "";
      }
    }
  }
});
