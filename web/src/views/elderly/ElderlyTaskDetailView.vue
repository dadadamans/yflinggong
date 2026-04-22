<template>
  <section class="panel">
    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText">{{ successText }}</p>
    <div v-if="loading" class="empty">正在加载任务详情...</div>
    <div v-else-if="!task" class="empty">任务不存在</div>
    <div v-else class="grid">
      <div class="topbar" style="margin-bottom: 0">
        <div>
          <h3 class="section-title" @click="speakTitle">{{ task.title }} 🔊</h3>
          <div class="meta">
            <span class="task-type">{{ task.taskType || task.task_type || task.type || "通用任务" }}</span>
            <span class="task-address">📍 {{ task.address }}</span>
            <span class="task-time">开始: {{ taskStartTime }}</span>
            <span class="task-time">结束: {{ taskEndTime }}</span>
            <span class="task-salary">💰 ¥{{ task.displaySalary || task.salary }}</span>
          </div>
        </div>
        <StatusTag :status="task.status" />
      </div>

      <article class="task-card">
        <h4 class="section-title">任务说明</h4>
        <p>{{ task.content }}</p>
        <button class="btn-speak" type="button" @click="speak(task.content)">
          🔊 播报任务说明
        </button>
        <div class="meta">
          <span>发布人：{{ task.publisherName || task.publisherUserName || task.publisher_name || "-" }}</span>
          <span v-if="task.publisherAvgRating" class="rating-info">
            评分：{{ task.publisherAvgRating.toFixed(1) }}★ ({{ task.publisherCommentCount || 0 }}条评价)
          </span>
          <span>当前接单人：{{ task.elderly_name || "暂无" }}</span>
        </div>
      </article>

      <article v-if="task.order" class="order-card">
        <h4 class="section-title">关联订单</h4>
        <div class="meta">
          <span>订单号：{{ task.order.code || task.order.id }}</span>
          <span>开始时间：{{ task.order.startTime || task.order.createTime || "-" }}</span>
          <span>完成时间：{{ task.order.finishTime || "-" }}</span>
        </div>
      </article>

      <div class="button-row">
        <button class="btn" type="button" :disabled="actionLoading" @click="handleAction">
          {{ actionLoading ? "处理中..." : actionText }}
        </button>
        <button class="btn-ghost" type="button" @click="router.push('/elderly/tasks')">返回列表</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getTaskDetail, applyTask } from "../../api/task";
import { speak } from "../../utils/speech";
import StatusTag from "../../components/StatusTag.vue";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const actionLoading = ref(false);
const errorText = ref("");
const successText = ref("");
const task = ref(null);

const actionText = computed(() => (task.value?.status === "waiting" ? "申请接单" : "查看我的订单"));

const taskStartTime = computed(() => {
  const timeText = task.value?.timeText || task.value?.time_text || "";
  if (!timeText) return "-";
  const parts = timeText.split(" ");
  if (parts.length < 2) return "-";
  const datePart = parts[0].substring(5);
  return `${datePart} ${parts[1].split("-")[0]}`;
});

const taskEndTime = computed(() => {
  const timeText = task.value?.timeText || task.value?.time_text || "";
  if (!timeText) return "-";
  const parts = timeText.split(" ");
  if (parts.length < 2) return "-";
  const datePart = parts[0].substring(5);
  return `${datePart} ${parts[1].split("-")[1]}`;
});

function speakTitle() {
  if (!task.value) return;
  const t = task.value;
  const type = t.taskType || t.task_type || t.type || "通用任务";
  const salary = t.salary || t.displaySalary || "面议";
  const startTime = taskStartTime.value === "-" ? "待定" : taskStartTime.value;
  const endTime = taskEndTime.value === "-" ? "待定" : taskEndTime.value;
  speak(`任务：${t.title || "暂无"}，任务类型${type}，地址在${t.address || "暂无"}，工资${salary}元，时间${startTime}到${endTime}`);
}

async function loadDetail() {
  loading.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const res = await getTaskDetail(route.params.id);
    task.value = res.data || null;
  } catch (error) {
    errorText.value = error.message || "任务详情加载失败";
  } finally {
    loading.value = false;
  }
}

async function handleAction() {
  if (!task.value) {
    return;
  }

  if (task.value.status !== "waiting") {
    router.push("/elderly/orders");
    return;
  }

  actionLoading.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const res = await applyTask(task.value.id);
    successText.value = res.message || "申请已提交，等待审核";
    await loadDetail();
  } catch (error) {
    errorText.value = error.message || "申请失败";
  } finally {
    actionLoading.value = false;
  }
}

onMounted(loadDetail);
</script>

<style scoped>
.btn-speak {
  margin-top: 8px;
  padding: 8px 16px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 18px;
  cursor: pointer;
  color: #333;
}
.btn-speak:active {
  background: #e8e8e8;
}
.task-type {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  background: transparent;
  padding: 0;
}
.task-time {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}
.task-address {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}
.task-salary {
  font-size: 24px;
  font-weight: 700;
  color: #d7845c;
}
</style>
