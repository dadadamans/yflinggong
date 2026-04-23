<template>
  <div class="grid">
    <section class="panel hero-section">
      <img src="../../assets/icons/hero-banner.png" alt="首页Banner" class="hero-banner" />
      <h3 class="hero-title">欢迎回来，{{ userStore.profile?.nickname || userStore.profile?.name || "演示用户" }}</h3>
      <div class="button-row">
        <button v-for="item in quickLinks" :key="item.to" class="btn" type="button" @click="router.push(item.to)">
          {{ item.label }}
        </button>
      </div>
    </section>

    <section class="grid grid-4">
      <article v-for="item in stats" :key="item.label" class="stat-card">
        <strong>{{ item.value }}</strong>
        <span class="muted">{{ item.label }}</span>
      </article>
    </section>

    <section class="panel">
      <div class="topbar" style="margin-bottom: 12px">
        <div>
          <h3 class="section-title">最近任务</h3>
        </div>
        <button class="btn-ghost" type="button" :disabled="loading" @click="loadData">刷新</button>
      </div>

      <p v-if="errorText" class="error-text">{{ errorText }}</p>
      <div v-else-if="!tasks.length" class="empty">暂无任务数据</div>
      <div v-else class="card-list">
        <article v-for="item in tasks.slice(0, 3)" :key="item.id" class="task-card">
          <div class="topbar" style="margin-bottom: 0">
            <div>
              <h4 class="section-title">{{ item.title }}</h4>
              <div class="meta">
                <span>{{ item.address }}</span>
                <span>开始: {{ getStartTime(item) }}</span>
                <span>结束: {{ getEndTime(item) }}</span>
                <span>¥{{ item.displaySalary || item.salary }}</span>
              </div>
            </div>
            <StatusTag :status="item.status" />
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { getBindInfo, getBindOrderList } from "../../api/bind";
import { getOrderList } from "../../api/order";
import { getTaskList } from "../../api/task";
import { useUserStore } from "../../stores/user";
import StatusTag from "../../components/StatusTag.vue";

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const errorText = ref("");
const tasks = ref([]);
const orders = ref([]);
const bindInfo = ref(null);

const quickLinks = computed(() => {
  const map = {
    elderly: [
      { label: "任务大厅", to: "/elderly/tasks" },
      { label: "我的订单", to: "/elderly/orders" },
      { label: "子女绑定", to: "/elderly/bind" }
    ],
    employer: [
      { label: "发布任务", to: "/employer/publish" },
      { label: "任务管理", to: "/employer/tasks" }
    ],
    child: [
      { label: "绑定老人", to: "/child/bind" },
      { label: "老人资料", to: "/child/elderly" },
      { label: "留言沟通", to: "/child/messages" }
    ],
    admin: [{ label: "进入后台", to: "/admin/users" }]
  };

  return map[userStore.role] || map.elderly;
});

const stats = computed(() => {
  const waiting = tasks.value.filter((item) => item.status === "waiting").length;
  const working = orders.value.filter((item) => item.status === "working").length;
  const done = orders.value.filter((item) => item.status === "done").length;
  
  const statsList = [
    { label: "待接任务", value: waiting },
    { label: "进行中订单", value: working },
    { label: "已完成订单", value: done }
  ];
  
  if (userStore.role === "elderly" || userStore.role === "child") {
    const hasBind = bindInfo.value?.bindInfo?.confirmed === true;
    const bind = hasBind ? "已绑定" : "待绑定";
    statsList.push({ label: "家属绑定", value: bind });
  }
  
  return statsList;
});

async function loadData() {
  loading.value = true;
  errorText.value = "";

  try {
    if (userStore.role === "elderly") {
      const taskRes = await getTaskList();
      tasks.value = taskRes.data || [];
    } else if (userStore.role === "employer") {
      const taskRes = await getTaskList();
      tasks.value = taskRes.data || [];
    } else if (userStore.role === "child") {
      const taskRes = await getTaskList();
      tasks.value = taskRes.data || [];
    }

    if (userStore.role === "elderly" || userStore.role === "employer") {
      const orderRes = await getOrderList();
      orders.value = orderRes.data || [];
    } else if (userStore.role === "child") {
      const bindOrderRes = await getBindOrderList();
      orders.value = bindOrderRes.data || [];
    }

    if (userStore.role === "elderly" || userStore.role === "child") {
      const bindRes = await getBindInfo();
      bindInfo.value = bindRes.data || null;
    }
  } catch (error) {
    errorText.value = error.message || "首页加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);

function getStartTime(item) {
  const timeText = item.timeText || item.time_text || "";
  if (!timeText) return "-";
  const parts = timeText.split(" ");
  return parts.length > 1 ? parts[1].split("-")[0] : "-";
}

function getEndTime(item) {
  const timeText = item.timeText || item.time_text || "";
  if (!timeText) return "-";
  const parts = timeText.split(" ");
  return parts.length > 1 ? parts[1].split("-")[1] : "-";
}
</script>

<style scoped>
.hero-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hero-banner {
  width: 100%;
  max-width: 600px;
  height: auto;
  border-radius: 8px;
  margin-bottom: 16px;
}
</style>

<style scoped>
.hero-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hero-banner {
  width: 100%;
  max-width: 600px;
  height: auto;
  border-radius: 8px;
  margin-bottom: 16px;
}
</style>
