<template>
  <section class="panel">
    <h3 class="section-title">{{ title }}</h3>
    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <div v-if="loading" class="empty">正在加载留言...</div>
    <div v-else class="message-list">
      <div
        v-for="item in messages"
        :key="item.id"
        class="bubble"
        :class="(item.senderRole || item.sender_role) === senderRole ? 'self' : 'other'"
      >
        <div class="bubble-header">
          <span class="bubble-role" :class="(item.senderRole || item.sender_role) === senderRole ? 'self-role' : 'other-role'">
            {{ (item.senderRole || item.sender_role) === 'elderly' ? '👴 ' + (item.senderName || item.sender_name || '老人') : '👩 ' + (item.senderName || item.sender_name || '子女') }}
          </span>
          <small class="muted">{{ formatTime(item.sentAt || item.sent_at) }}</small>
        </div>
        <div class="bubble-content">{{ item.content }}</div>
      </div>
    </div>
    <div class="field" style="margin-top: 16px">
      <textarea v-model="text" placeholder="输入留言内容" />
    </div>
    <div class="button-row">
      <button class="btn-secondary" type="button" :disabled="sending" @click="submit">
        {{ sending ? "发送中..." : "发送留言" }}
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { getMessageList, sendMessage } from "../../api/message";
import { useUserStore } from "../../stores/user";

const userStore = useUserStore();
const loading = ref(false);
const sending = ref(false);
const errorText = ref("");
const text = ref("");
const messages = ref([]);

const senderRole = computed(() => (userStore.role === "elderly" ? "elderly" : "child"));
const title = computed(() => (userStore.role === "elderly" ? "给子女回复留言" : "给老人发送留言"));


function formatTime(time) {
  if (!time) return "";
  if (typeof time === "string") {
    let date;
    if (time.includes("T")) {
      date = new Date(time);
    } else {
      date = new Date(time.replace(" ", "T"));
    }
    if (!isNaN(date.getTime())) {
      date.setHours(date.getHours() + 8);
      const pad = (n) => String(n).padStart(2, "0");
      return `${pad(date.getFullYear())}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }
    return time.substring(0, 16);
  }
  return "";
}

const isDataLoading = ref(false);

async function loadData() {
  if (isDataLoading.value) return;
  isDataLoading.value = true;
  errorText.value = "";
  try {
    const res = await getMessageList();
    messages.value = res.data || [];
    console.log("列表已刷新，当前留言数:", messages.value.length);
  } catch (error) {
    errorText.value = error.message || "留言加载失败";
  } finally {
    setTimeout(() => { isDataLoading.value = false; }, 300);
    loading.value = false;
  }
}

async function submit() {
  if (!text.value.trim()) {
    errorText.value = "请输入留言内容";
    return;
  }

  sending.value = true;
  errorText.value = "";
  try {
    await sendMessage({ senderRole: senderRole.value, content: text.value });
    text.value = "";
    await loadData();
  } catch (error) {
    errorText.value = error.message || "发送失败";
  } finally {
    sending.value = false;
  }
}

let debounceTimer = null;

function onNewMessage(payload) {
  console.log("组件级回调触发，收到载荷:", payload);
  if (payload && payload.type === "new_message") {
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      loadData();
      debounceTimer = null;
    }, 300);
  }
}

function onNotification(payload) {
  console.log("收到系统通知:", payload);
}

onMounted(() => {
  console.log("组件挂载正在注册 WebSocket 回调...");
  userStore.registerMessageCallback(onNewMessage, onNotification);
  userStore.initWebSocket();
  loadData();
});

onUnmounted(() => {
  console.log("组件销毁，清理回调...");
  userStore.registerMessageCallback(null, null);
});
</script>

<style scoped>
.bubble-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.bubble-role {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.bubble-role.self-role {
  background: #d7845c;
  color: #fff;
}

.bubble-role.other-role {
  background: #4da46d;
  color: #fff;
}

.bubble-content {
  line-height: 1.5;
}
</style>
