<template>
  <section class="panel">
    <div class="topbar" style="margin-bottom: 12px">
      <div>
        <h3 class="section-title">任务管理</h3>
      </div>
      <button class="btn-ghost" type="button" :disabled="loading" @click="loadData">刷新</button>
    </div>

    <div class="filter-tabs">
      <button 
        class="filter-tab" 
        :class="{ active: filterStatus === 'waiting' }"
        @click="filterStatus = 'waiting'"
      >
        <img src="../../assets/icons/waiting.svg" class="tab-icon" />
        <span>待接单</span>
        <span v-if="filterStatus === 'waiting'" class="tab-count">{{ total }}</span>
      </button>
      <button 
        class="filter-tab" 
        :class="{ active: filterStatus === 'applying' }"
        @click="filterStatus = 'applying'"
      >
        <img src="../../assets/icons/审核.svg" class="tab-icon" />
        <span>待审核</span>
        <span v-if="filterStatus === 'applying'" class="tab-count">{{ total }}</span>
      </button>
      <button 
        class="filter-tab" 
        :class="{ active: filterStatus === 'working' }"
        @click="filterStatus = 'working'"
      >
        <img src="../../assets/icons/working.svg" class="tab-icon" />
        <span>进行中</span>
        <span v-if="filterStatus === 'working'" class="tab-count">{{ total }}</span>
      </button>
      <button 
        class="filter-tab" 
        :class="{ active: filterStatus === 'pending_payment' }"
        @click="filterStatus = 'pending_payment'"
      >
        <img src="../../assets/icons/pending.svg" class="tab-icon" />
        <span>待支付</span>
        <span v-if="filterStatus === 'pending_payment'" class="tab-count">{{ total }}</span>
      </button>
      <button 
        class="filter-tab" 
        :class="{ active: filterStatus === 'done' }"
        @click="filterStatus = 'done'"
      >
        <img src="../../assets/icons/done.svg" class="tab-icon" />
        <span>已完成</span>
        <span v-if="filterStatus === 'done'" class="tab-count">{{ total }}</span>
      </button>
    </div>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText">{{ successText }}</p>
    <div v-if="!tasks.length" class="empty">暂无已发布任务</div>
    <div v-else class="card-list">
      <article v-for="item in tasks" :key="item.id" class="task-card">
        <div class="topbar" style="margin-bottom: 0">
          <div>
            <h4 class="section-title">{{ item.title }}</h4>
            <div class="meta">
              <span class="task-address">📍 {{ item.address }}</span>
              <span>开始: {{ getStartTime(item) }}</span>
              <span>结束: {{ getEndTime(item) }}</span>
              <span class="task-salary">💰 ¥{{ item.salary }}</span>
              <span>接单人：{{ item.elderly_name || item.elderlyName || "暂无" }}</span>
              <span v-if="item.elderlyMobile">电话：{{ item.elderlyMobile }}</span>
              <span v-if="item.elderlyHealthCondition">状况：{{ healthConditionText(item.elderlyHealthCondition) }}</span>
              <span v-if="item.elderlyCommentCount" class="rating">评分：{{ item.elderlyAvgRating.toFixed(1) }}★ ({{ item.elderlyCommentCount }}条)</span>
              <span v-else class="rating">暂无评分</span>
            </div>
          </div>
          <StatusTag :status="item.status" />
        </div>
        <div class="button-row">
          <button v-if="item.status === 'waiting'" class="btn btn-danger" type="button" :disabled="actionLoading" @click="remove(item.id)">
            删除订单
          </button>
          <button v-else-if="item.status === 'applying'" class="btn" type="button" :disabled="actionLoading" @click="openApproveDialog(item)">
            审核接单
          </button>
          <button v-else-if="item.status === 'pending_payment'" class="btn" type="button" :disabled="actionLoading" @click="showPayDialog(item)">
            模拟支付
          </button>
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

    <div v-if="showDialog" class="modal-mask" @click="showDialog = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3 class="section-title">模拟支付</h3>
          <button class="modal-close" @click="showDialog = false">×</button>
        </div>
        <div class="pay-info">
          <p>订单：{{ payItem?.title }}</p>
          <p>金额：<strong>¥{{ payItem?.salary }}</strong></p>
        </div>
        <div class="button-row">
          <button class="btn btn-success" style="flex: 1" type="button" :disabled="paying" @click="payOrder">
            {{ paying ? '支付中...' : '确认支付' }}
          </button>
          <button class="btn btn-danger" style="flex: 1" type="button" @click="showDialog = false">取消</button>
        </div>
      </div>
    </div>

    <div v-if="showCommentDialog" class="modal-mask" @click="showCommentDialog = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>评价老人</h3>
          <button class="modal-close" @click="showCommentDialog = false">×</button>
        </div>
        <div class="field">
          <label>评分</label>
          <div class="rating-stars">
            <span
              v-for="star in 5"
              :key="star"
              class="star"
              :class="{ active: star <= commentForm.rating }"
              @click="setRating(star)"
            >★</span>
          </div>
          <small class="muted">{{ commentForm.rating }}星</small>
        </div>
        <div class="field">
          <label>评价内容（可选）</label>
          <textarea v-model="commentForm.content" placeholder="写下您的评价..."></textarea>
        </div>
        <div class="button-row">
          <button class="btn btn-success" style="flex: 1" :disabled="commenting" @click="submitComment">提交评价</button>
          <button class="btn btn-danger" style="flex: 1" @click="showCommentDialog = false">跳过</button>
        </div>
      </div>
    </div>

    <div v-if="showApproveDialog" class="modal-mask" @click="showApproveDialog = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3 class="section-title">审核接单申请</h3>
          <button class="modal-close" @click="showApproveDialog = false">×</button>
        </div>
        <div class="pay-info">
          <p>任务：{{ approveItem?.title }}</p>
          <p>申请人：{{ approveItem?.elderlyName }}</p>
          <p v-if="approveItem?.elderlyCommentCount">评分：{{ approveItem?.elderlyAvgRating.toFixed(1) }}★ ({{ approveItem?.elderlyCommentCount }}条评价)</p>
          <p v-else>暂无评分</p>
          <p>健康状况：{{ approveItem?.elderlyHealthCondition || '-' }}</p>
        </div>
        <div class="button-row" style="justify-content: center">
          <button class="btn btn-success" style="flex: 1" type="button" :disabled="approving" @click="approveTask">
            {{ approving ? '处理中...' : '同意接单' }}
          </button>
          <button class="btn btn-danger" style="flex: 1" type="button" :disabled="approving" @click="rejectTask">
            拒绝
          </button>
        </div>
      </div>
    </div>

    <div v-if="showConfirmDialog" class="modal-mask" @click="showConfirmDialog = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3 class="section-title">确认提示</h3>
          <button class="modal-close" @click="showConfirmDialog = false">×</button>
        </div>
        <div class="pay-info">
          <p>{{ confirmAction.value === 'delete' ? '确定要删除该订单吗？' : '请确认操作' }}</p>
        </div>
        <div class="button-row">
          <button class="btn btn-success" style="flex: 1" :disabled="actionLoading" @click="confirmActionHandler">
            {{ actionLoading ? '处理中...' : '确定' }}
          </button>
          <button class="btn btn-danger" style="flex: 1" @click="showConfirmDialog = false">取消</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { addComment } from "../../api/comment";
import { deleteOrder, payOrder as payOrderApi } from "../../api/order";
import { getTaskList, approveTask as approveTaskApi, rejectTask as rejectTaskByIdApi } from "../../api/task";
import AppPagination from "../../components/AppPagination.vue";
import StatusTag from "../../components/StatusTag.vue";

const loading = ref(false);
const actionLoading = ref(false);
const errorText = ref("");
const successText = ref("");
const tasks = ref([]);
const total = ref(0);
const filterStatus = ref("waiting");
const currentPage = ref(1);
const pageSize = ref(10);

const showDialog = ref(false);
const payItem = ref(null);
const paying = ref(false);

const showCommentDialog = ref(false);
const commenting = ref(false);
const commentForm = ref({
  taskId: null,
  revieweeId: null,
  rating: 5,
  content: "",
  commentType: "employer_rate_elderly"
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

const showApproveDialog = ref(false);
const approveItem = ref(null);
const approving = ref(false);

const showConfirmDialog = ref(false);
const confirmItemId = ref(null);
const confirmAction = ref("");

function healthConditionText(condition) {
  const map = { healthy: "健康", fair: "一般" };
  return condition ? (map[condition] || condition) : "-";
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
  const timeParts = parts[1].split("-");
  if (timeParts.length < 2 || !timeParts[1]) return "-";
  const datePart = parts[0].substring(5);
  return `${datePart} ${timeParts[1]}`;
}

async function loadData() {
  loading.value = true;
  errorText.value = "";
  try {
    const res = await getTaskList({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value || undefined
    });
    tasks.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch (error) {
    errorText.value = error.message || "任务列表加载失败";
  } finally {
    loading.value = false;
  }
}

function showPayDialog(item) {
  payItem.value = item;
  showDialog.value = true;
}

function openApproveDialog(item) {
  approveItem.value = item;
  showApproveDialog.value = true;
}

async function approveTask() {
  if (!approveItem.value) return;
  approving.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    await approveTaskApi(approveItem.value.id);
    successText.value = "已同意接单";
    showApproveDialog.value = false;
    await loadData();
  } catch (error) {
    errorText.value = error.message || "操作失败";
  } finally {
    approving.value = false;
  }
}

async function rejectTask() {
  if (!approveItem.value) return;
  approving.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    await rejectTaskByIdApi(approveItem.value.id);
    successText.value = "已拒绝申请";
    showApproveDialog.value = false;
    await loadData();
  } catch (error) {
    errorText.value = error.message || "操作失败";
  } finally {
    approving.value = false;
  }
}

async function payOrder() {
  if (!payItem.value) return;
  paying.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    await payOrderApi(payItem.value.id);
    successText.value = "支付成功";
    showDialog.value = false;
    showCommentDialog.value = true;
    commentForm.value = {
      taskId: payItem.value.id,
      revieweeId: payItem.value.elderlyId,
      rating: 5,
      content: "",
      commentType: "employer_rate_elderly"
    };
    await loadData();
  } catch (error) {
    errorText.value = error.message || "支付失败";
  } finally {
    paying.value = false;
  }
}

function setRating(rating) {
  commentForm.value.rating = rating;
}

async function submitComment() {
  if (commentForm.value.rating < 1 || commentForm.value.rating > 5) {
    errorText.value = "请选择评分";
    return;
  }
  commenting.value = true;
  try {
    await addComment(commentForm.value);
    showCommentDialog.value = false;
    commentForm.value = { taskId: null, revieweeId: null, rating: 5, content: "", commentType: "employer_rate_elderly" };
  } catch (error) {
    errorText.value = error.message || "评价提交失败";
  } finally {
    commenting.value = false;
  }
}

function remove(taskId) {
  confirmItemId.value = taskId;
  confirmAction.value = "delete";
  showConfirmDialog.value = true;
}

async function confirmActionHandler() {
  if (confirmAction.value === "delete") {
    actionLoading.value = true;
    errorText.value = "";
    successText.value = "";
    try {
      const res = await deleteOrder(confirmItemId.value);
      successText.value = res.message || "订单已删除";
      showConfirmDialog.value = false;
      await loadData();
    } catch (error) {
      errorText.value = error.message || "操作失败";
    } finally {
      actionLoading.value = false;
    }
  }
}

onMounted(loadData);

watch(filterStatus, () => {
  currentPage.value = 1;
  loadData();
});
</script>

<style scoped>
.pay-info {
  padding: 16px 0;
  text-align: center;
}
.pay-info p {
  margin: 8px 0;
}
.pay-info strong {
  font-size: 24px;
  color: #d7845c;
}

.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  width: 80%;
  max-width: 320px;
}
.task-address {
  font-weight: 600;
  color: #333;
}
.rating {
  color: #f57c00;
  font-size: 14px;
}
.task-salary {
  font-size: 16px;
  font-weight: 700;
  color: #d7845c;
}

.filter-tabs {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.filter-tab {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 24px;
  border: 1px solid #ddd;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 16px;
  font-weight: 500;
}
.filter-tab:hover {
  border-color: #4a90d9;
}
.filter-tab.active {
  background: #e8f4fc;
  border-color: #4a90d9;
  color: #1976d2;
}
.tab-icon {
  width: 28px;
  height: 28px;
}
.tab-count {
  background: #f0f0f0;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
}
.filter-tab.active .tab-count {
  background: #4a90d9;
  color: #fff;
}

.btn-warning {
  background: #ff9800;
  color: #fff;
}
.btn-warning:hover {
  background: #f57c00;
}
.btn-success {
  background: #4caf50;
  color: #fff;
  text-align: center;
}
.btn-success:hover {
  background: #43a047;
}
.btn-danger {
  text-align: center;
}
.button-col {
  margin-bottom: 8px;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.modal-header .section-title {
  margin: 0;
}
.modal {
  background: radial-gradient(circle at top left, rgba(184, 92, 56, 0.12), transparent 26%),
              radial-gradient(circle at top right, rgba(47, 125, 77, 0.1), transparent 22%),
              linear-gradient(180deg, #f6f1e7, #efe6d8 48%, #f8f5ef);
}
.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0 8px;
}
.modal-close:hover {
  color: #333;
}
.rating-stars {
  display: flex;
  gap: 8px;
  font-size: 32px;
  margin: 8px 0;
}
.star {
  color: #ddd;
  cursor: pointer;
  transition: color 0.2s;
}
.star.active {
  color: #f5b700;
}
</style>
