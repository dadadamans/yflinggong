<template>
  <section class="panel">
    <div class="topbar" style="margin-bottom: 12px">
      <div>
        <h3 class="section-title">用户反馈管理</h3>
      </div>
      <button class="btn-ghost" type="button" :disabled="loading" @click="loadData">刷新</button>
    </div>

    <div class="filter-tabs">
      <button class="filter-tab" :class="{ active: filterStatus === '' }" @click="filterStatus = ''">
        <span>全部</span>
        <span class="tab-count">{{ totalCount }}</span>
      </button>
      <button class="filter-tab" :class="{ active: filterStatus === 'pending' }" @click="filterStatus = 'pending'">
        <span>待处理</span>
        <span class="tab-count">{{ pendingCount }}</span>
      </button>
      <button class="filter-tab" :class="{ active: filterStatus === 'resolved' }" @click="filterStatus = 'resolved'">
        <span>已处理</span>
        <span class="tab-count">{{ resolvedCount }}</span>
      </button>
    </div>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText" class="success-text">{{ successText }}</p>

    <table v-if="feedbackList.length" class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>用户</th>
          <th>角色</th>
          <th>问题类型</th>
          <th>关联订单</th>
          <th>反馈内容</th>
          <th>状态</th>
          <th>时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in feedbackList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.userName || '-' }}</td>
          <td>
            <span class="role-badge" :class="'role-' + item.role">{{ roleText(item.role) }}</span>
          </td>
          <td>{{ feedbackTypeText(item.feedbackType) }}</td>
          <td>
            <span v-if="item.orderTitle">{{ item.orderTitle }}</span>
            <span v-else class="muted">-</span>
          </td>
          <td class="content-cell">{{ item.content }}</td>
          <td>
            <span class="status-badge" :class="'status-' + item.status">
              {{ item.status === 'pending' ? '待处理' : '已处理' }}
            </span>
          </td>
          <td>{{ formatDate(item.createdAt) }}</td>
          <td>
            <button
              v-if="item.status === 'pending'"
              class="action-btn action-btn-success"
              @click="resolveFeedback(item)"
            >
              标记已处理
            </button>
            <span v-else class="muted">-</span>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty">暂无反馈数据</div>

    <AppPagination
      v-if="total > 0"
      :page="currentPage"
      :page-size="pageSize"
      :total="total"
      @update:page="handlePageChange"
      @update:pageSize="handlePageSizeChange"
    />
  </section>
</template>

<script setup>
import { onMounted, ref, computed } from "vue";
import { getAdminFeedbackList, updateFeedbackStatus } from "../../api/feedback";
import AppPagination from "../../components/AppPagination.vue";

const loading = ref(false);
const errorText = ref("");
const successText = ref("");
const feedbackList = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const filterStatus = ref("");

const totalCount = computed(() => feedbackList.value.length);
const pendingCount = ref(0);
const resolvedCount = ref(0);

function formatDate(dateStr) {
  if (!dateStr) return "-";
  if (typeof dateStr === "string" && dateStr.includes("T")) {
    return dateStr.substring(0, 19).replace("T", " ");
  }
  return dateStr;
}

function roleText(role) {
  const map = { elderly: "老人", employer: "雇主", child: "子女" };
  return map[role] || role || "-";
}

function feedbackTypeText(type) {
  const map = {
    employer_not_pay: "雇主未支付",
    task_issue: "任务问题",
    elderly_dispute: "服务纠纷",
    elderly_not_finish: "老人未完成任务",
    health_issue: "健康问题",
    payment_issue: "支付问题",
    task_exception: "任务异常",
    elderly_issue: "老人问题",
    other: "其他问题"
  };
  return map[type] || type || "-";
}

async function loadData() {
  loading.value = true;
  errorText.value = "";
  try {
    const res = await getAdminFeedbackList();
    let list = res.data || [];

    if (filterStatus.value) {
      list = list.filter(item => item.status === filterStatus.value);
    }

    feedbackList.value = list;
    total.value = list.length;

    pendingCount.value = list.filter(item => item.status === "pending").length;
    resolvedCount.value = list.filter(item => item.status === "resolved").length;
  } catch (error) {
    errorText.value = error.message || "反馈列表加载失败";
  } finally {
    loading.value = false;
  }
}

async function resolveFeedback(item) {
  try {
    await updateFeedbackStatus(item.id, "resolved");
    successText.value = "已标记为已处理";
    await loadData();
  } catch (error) {
    errorText.value = error.message || "操作失败";
  }
}

function handlePageChange(page) {
  currentPage.value = page;
  loadData();
}

function handlePageSizeChange(size) {
  pageSize.value = size;
  currentPage.value = 1;
  loadData();
}

onMounted(loadData);
</script>

<style scoped>
.filter-tabs {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.filter-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
}
.filter-tab.active {
  background: #4a90d9;
  color: #fff;
  border-color: #4a90d9;
}
.tab-count {
  background: rgba(0,0,0,0.1);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}
.filter-tab.active .tab-count {
  background: rgba(255,255,255,0.2);
}
.role-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}
.role-elderly { background: #d7845c; }
.role-employer { background: #4a90d9; }
.role-child { background: #4da46d; }
.status-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.status-pending { background: #fff3e0; color: #e65100; }
.status-resolved { background: #e8f5e9; color: #2e7d32; }
.content-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}
.action-btn-success {
  background: #4caf50;
  color: #fff;
}
.action-btn-success:hover {
  background: #43a047;
}
.success-text {
  color: #4caf50;
  margin-bottom: 12px;
}
.error-text {
  color: #f56c6c;
  margin-bottom: 12px;
}
.muted {
  color: #999;
}
.empty {
  padding: 40px;
  text-align: center;
  color: #999;
}
</style>