<template>
  <div class="grid">
    <section class="grid grid-3">
      <article class="stat-card">
        <strong>{{ profile.realName || profile.nickname || "老人用户" }}</strong>
        <span class="muted">当前接单人</span>
      </article>
      <article class="stat-card">
        <strong>{{ total }}</strong>
        <span class="muted">待接任务</span>
      </article>
      <article class="stat-card">
        <strong>{{ workingTotal }}</strong>
        <span class="muted">进行中任务</span>
      </article>
    </section>

    <section class="panel">
      <div class="topbar" style="margin-bottom: 12px">
        <div>
          <h3 class="section-title">任务大厅</h3>
        </div>
        <button class="btn-ghost" type="button" :disabled="loading" @click="loadData">刷新</button>
      </div>
      <p v-if="errorText" class="error-text">{{ errorText }}</p>
      <div v-else-if="!tasks.length" class="empty">暂无可查看任务</div>
      <div v-else class="card-list">
        <article v-for="item in tasks" :key="item.id" class="task-card">
          <div class="topbar" style="margin-bottom: 0">
            <div>
              <h4 class="section-title" @click="speakTitle(item)">{{ item.title }} 🔊</h4>
              <div class="meta">
                <span class="task-type">{{ item.taskType || item.task_type || item.type || "通用任务" }}</span>
                <span class="task-address">📍 {{ item.address }}</span>
                <span class="task-time">开始: {{ getStartTime(item) }}</span>
                <span class="task-time">结束: {{ getEndTime(item) }}</span>
                <span class="task-salary">💰 ¥{{ item.displaySalary || item.salary }}</span>
              </div>
              <div class="publisher-info" :style="{ fontSize: publisherFontSize + 'px' }">
                <span>发布人：{{ item.publisherName }}</span>
                <span v-if="item.publisherCommentCount" class="rating">评分：{{ item.publisherAvgRating.toFixed(1) }}★ ({{ item.publisherCommentCount }}条)</span>
                <span v-else class="rating">暂无评分</span>
              </div>
              <p class="muted">{{ item.content }}</p>
            </div>
            <StatusTag :status="item.status" />
          </div>
          <div class="button-row">
            <button class="btn" type="button" @click="router.push(`/elderly/task/${item.id}`)">查看详情</button>
          </div>
        </article>
      </div>
      <AppPagination
        v-if="total > 0"
        :page="currentPage"
        :page-size="pageSize"
        :total="total"
        @update:page="handlePageChange"
        @update:pageSize="handlePageSizeChange"
      />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { getTaskList } from "../../api/task";
import { getOrderList } from "../../api/order";
import { getUserInfo } from "../../api/user";
import { useUserStore } from "../../stores/user";
import { speak } from "../../utils/speech";
import AppPagination from "../../components/AppPagination.vue";
import StatusTag from "../../components/StatusTag.vue";

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const errorText = ref("");
const profile = ref({});
const tasks = ref([]);
const total = ref(0);
const workingTotal = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const fontSizeMap = { small: 14, medium: 16, large: 20, xlarge: 24 };
const publisherFontSize = computed(() => {
  const size = userStore.fontSize || "medium";
  return fontSizeMap[size] || 16;
});

function handlePageChange(page) {
  currentPage.value = page;
  loadData();
}

function handlePageSizeChange(size) {
  pageSize.value = size;
  currentPage.value = 1;
  loadData();
}

function getStartTime(item) {
  const timeText = item.timeText || item.time_text || "";
  if (!timeText) return "-";
  const parts = timeText.split(" ");
  if (parts.length < 2) return "-";
  const datePart = parts[0].substring(5);
  return `${datePart} ${parts[1].split("-")[0]}`;
}

function getEndTime(item) {
  const timeText = item.timeText || item.time_text || "";
  if (!timeText) return "-";
  const parts = timeText.split(" ");
  if (parts.length < 2) return "-";
  const datePart = parts[0].substring(5);
  return `${datePart} ${parts[1].split("-")[1]}`;
}

function speakTitle(item) {
  if (!item) return;
  const title = item.title || "暂无标题";
  const type = item.taskType || item.task_type || item.type || "通用任务";
  const address = item.address || "暂无地址";
  const salary = item.displaySalary || item.salary || "面议";
  speak(`${title}，任务类型${type}，地址${address}，工资${salary}元`);
}

async function loadData() {
  loading.value = true;
  errorText.value = "";
  try {
    const [userRes, taskRes, orderRes] = await Promise.all([
      getUserInfo(),
      getTaskList({ page: currentPage.value, pageSize: pageSize.value }),
      getOrderList({ page: 1, pageSize: 1, status: "working" })
    ]);
    profile.value = userRes.data?.profile || userRes.data || {};
    tasks.value = taskRes.data?.list || [];
    total.value = taskRes.data?.total || 0;
    workingTotal.value = orderRes.data?.total || 0;
  } catch (error) {
    errorText.value = error.message || "任务加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
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
  font-size: 22px;
  font-weight: 700;
  color: #d7845c;
}
.publisher-info {
  color: #666;
  margin-top: 4px;
  font-weight: 500;
}
.publisher-info .rating {
  color: #f57c00;
  margin-left: 8px;
  font-weight: 600;
}
.rating-info {
  color: #f57c00;
}
</style>
