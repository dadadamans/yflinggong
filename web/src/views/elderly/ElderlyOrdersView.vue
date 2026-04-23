<template>
  <div class="split">
    <section class="panel">
      <div class="topbar" style="margin-bottom: 12px">
        <div>
          <h3 class="section-title">我的订单</h3>
        </div>
        <button class="btn-ghost" type="button" :disabled="loading" @click="loadData">刷新</button>
      </div>
      
      <div class="filter-tabs">
        <button 
          class="filter-tab" 
          :class="{ active: filterStatus === 'applying' }"
          @click="filterStatus = 'applying'"
        >
          <img src="../../assets/icons/审核.svg" class="tab-icon" />
          <span>申请中</span>
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
      <div v-if="!orders.length" class="empty">暂无订单</div>
      <div v-else class="card-list">
        <article v-for="item in orders" :key="item.id" class="order-card">
          <div class="topbar" style="margin-bottom: 0">
            <div>
              <h4 class="section-title">{{ item.title }}</h4>
              <div class="meta">
                <span>订单号：{{ item.code || item.id }}</span>
                <span class="task-address">📍 {{ item.address }}</span>
                <span v-if="item.formattedStartTime">开始: {{ item.formattedStartTime }}</span>
                <span v-if="item.formattedFinishTime">完成: {{ item.formattedFinishTime }}</span>
                <span v-else-if="item.timeText">时间：{{ item.timeText }}</span>
                <span class="task-salary">💰 报酬：¥{{ item.displaySalary || item.salary }}</span>
                <span v-if="item.status === 'working' && item.publisherMobile">雇主电话：{{ item.publisherMobile }}</span>
                <span>结算：{{ item.settleText || item.settle_text || "-" }}</span>
              </div>
            </div>
            <StatusTag :status="item.status" />
          </div>
          <div class="button-row">
            <button v-if="item.status === 'working'" class="btn" type="button" :disabled="actionLoading" @click="completeOrder(item)">
              完成订单
            </button>
            <button
              v-else-if="item.status === 'done' && !item.currentUserCommented"
              class="btn btn-success"
              type="button"
              :disabled="commenting"
              @click="openCommentDialog(item)"
            >
              评价雇主
            </button>
            <button class="btn btn-warning" type="button" @click="openFeedbackDialog(item)">
              反馈
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
    </section>

    <section class="grid">
      <article class="panel">
        <h3 class="section-title">绑定信息</h3>
        <div class="meta">
          <span>绑定人：{{ bindInfo?.childName || bindInfo?.code || "-" }}</span>
          <span>绑定状态：{{ bindInfo?.confirmed ? "已绑定" : "待绑定" }}</span>
        </div>
        <div v-if="bindInfo?.healthReportStatus" class="health-status" :class="'health-' + bindInfo.healthReportStatus">
          <span>体检报告：{{ healthStatusText(bindInfo.healthReportStatus) }}</span>
        </div>
      </article>

      <article class="panel">
        <h3 class="section-title">留言回复</h3>
        <p v-if="messageError" class="error-text">{{ messageError }}</p>
        <div class="message-list">
          <div
            v-for="item in messages"
            :key="item.id"
            class="bubble"
            :class="(item.senderRole || item.sender_role) === 'elderly' ? 'self' : 'other'"
          >
            <div class="bubble-header">
              <span class="bubble-role" :class="(item.senderRole || item.sender_role) === 'elderly' ? 'self-role' : 'other-role'">
                {{ (item.senderRole || item.sender_role) === 'elderly' ? '👴 ' + (item.senderName || item.sender_name || '老人') : '👩 ' + (item.senderName || item.sender_name || '子女') }}
              </span>
              <small class="muted">{{ formatTime(item.sentAt || item.sent_at) }}</small>
            </div>
            <div class="bubble-content">{{ item.content }}</div>
          </div>
        </div>
        <div class="field" style="margin-top: 14px">
          <textarea v-model="reply" placeholder="给子女回复消息"></textarea>
        </div>
        <div class="button-row">
          <button class="btn-secondary" type="button" :disabled="sending" @click="sendReplyHandler">
            {{ sending ? "发送中..." : "发送回复" }}
          </button>
        </div>
      </article>
    </section>

    <div v-if="showCommentDialog" class="modal-mask" @click="showCommentDialog = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>评价雇主</h3>
          <button class="modal-close" @click="showCommentDialog = false">×</button>
        </div>
        <p v-if="messageError" class="error-text">{{ messageError }}</p>
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
          <button class="btn btn-danger" style="flex: 1" @click="showCommentDialog = false">取消</button>
        </div>
      </div>
    </div>

    <div v-if="showConfirmDialog" class="modal-mask" @click="showConfirmDialog = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>确认提示</h3>
          <button class="modal-close" @click="showConfirmDialog = false">×</button>
        </div>
        <div class="pay-info">
          <p>确定要完成该订单吗？</p>
        </div>
        <div class="button-row">
          <button class="btn btn-success" style="flex: 1" :disabled="actionLoading" @click="confirmCompleteOrder">
            {{ actionLoading ? '处理中...' : '确定' }}
          </button>
          <button class="btn btn-danger" style="flex: 1" @click="showConfirmDialog = false">取消</button>
        </div>
      </div>
    </div>

    <div v-if="showFeedbackDialog" class="modal-mask" @click="showFeedbackDialog = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>提交反馈</h3>
          <button class="modal-close" @click="showFeedbackDialog = false">×</button>
        </div>
        <p v-if="feedbackError" class="error-text">{{ feedbackError }}</p>
        <div v-if="feedbackSuccess" class="success-text">{{ feedbackSuccess }}</div>
        <div class="field">
          <label>问题类型</label>
          <select v-model="feedbackForm.feedbackType" class="select-input">
            <option value="">请选择</option>
            <option value="employer_not_pay">雇主未支付</option>
            <option value="task_issue">任务问题</option>
            <option value="elderly_dispute">服务纠纷</option>
            <option value="other">其他问题</option>
          </select>
        </div>
        <div class="field">
          <label>反馈内容</label>
          <textarea v-model="feedbackForm.content" placeholder="请详细描述您的问题..."></textarea>
        </div>
        <div class="button-row">
          <button class="btn btn-primary" style="flex: 1" :disabled="submittingFeedback" @click="submitFeedback">
            {{ submittingFeedback ? '提交中...' : '提交反馈' }}
          </button>
          <button class="btn btn-danger" style="flex: 1" @click="showFeedbackDialog = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch } from "vue";
import { useUserStore } from "../../stores/user";
import { addComment } from "../../api/comment";
import { submitFeedback as submitFeedbackApi } from "../../api/feedback";
import { getBindInfo } from "../../api/bind";
import { getMessageList, sendMessage } from "../../api/message";
import { getOrderList, finishOrder } from "../../api/order";
import AppPagination from "../../components/AppPagination.vue";
import StatusTag from "../../components/StatusTag.vue";

const userStore = useUserStore();
const loading = ref(false);
const sending = ref(false);
const actionLoading = ref(false);
const errorText = ref("");
const successText = ref("");
const messageError = ref("");
const reply = ref("");
const orders = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const bindInfo = ref(null);
const messages = ref([]);
const filterStatus = ref("applying");

const showCommentDialog = ref(false);
const commenting = ref(false);
const showConfirmDialog = ref(false);
const confirmItem = ref(null);
const commentForm = ref({
  taskId: null,
  revieweeId: null,
  rating: 5,
  content: "",
  commentType: "elderly_rate_employer"
});

const showFeedbackDialog = ref(false);
const submittingFeedback = ref(false);
const feedbackError = ref("");
const feedbackSuccess = ref("");
const feedbackItem = ref(null);
const feedbackForm = ref({
  feedbackType: "",
  content: "",
  relatedOrderId: null
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

async function loadData() {
  loading.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const orderRes = await getOrderList({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value || undefined
    });
    orders.value = orderRes.data?.list || [];
    total.value = orderRes.data?.total || 0;
  } catch (error) {
    errorText.value = error.message || "订单加载失败";
  } finally {
    loading.value = false;
  }
}

async function loadBindInfo() {
  try {
    const bindRes = await getBindInfo();
    bindInfo.value = bindRes.data?.bindInfo || null;
  } catch (error) {
    messageError.value = error.message || "绑定信息加载失败";
  }
}

const isRefreshing = ref(false);

async function loadMessages() {
  if (isRefreshing.value) return;
  isRefreshing.value = true;
  try {
    const messageRes = await getMessageList();
    messages.value = messageRes.data || [];
  } catch (error) {
    messageError.value = messageError || "留言加载失败";
  } finally {
    setTimeout(() => { isRefreshing.value = false; }, 300);
  }
}

async function completeOrder(item) {
  confirmItem.value = item;
  showConfirmDialog.value = true;
}

async function confirmCompleteOrder() {
  const item = confirmItem.value;
  actionLoading.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    await finishOrder(item.id);
    successText.value = "订单已提交，等待支付";
    showConfirmDialog.value = false;
    confirmItem.value = null;
    await loadData();
  } catch (error) {
    errorText.value = error.message || "操作失败";
  } finally {
    actionLoading.value = false;
  }
}

function openCommentDialog(item) {
  messageError.value = "";
  commentForm.value = {
    taskId: item.id,
    revieweeId: item.publisherId || item.publisher_id,
    rating: 5,
    content: "",
    commentType: "elderly_rate_employer"
  };
  showCommentDialog.value = true;
}

function setRating(rating) {
  commentForm.value.rating = rating;
}

async function submitComment() {
  if (commentForm.value.rating < 1 || commentForm.value.rating > 5) {
    messageError.value = "请选择评分";
    return;
  }
  commenting.value = true;
  try {
    await addComment(commentForm.value);
    showCommentDialog.value = false;
    commentForm.value = { taskId: null, revieweeId: null, rating: 5, content: "", commentType: "elderly_rate_employer" };
    await loadData();
  } catch (error) {
    messageError.value = error.message || "评价提交失败";
  } finally {
    commenting.value = false;
  }
}

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

function healthStatusText(status) {
  const map = { pending: "待审核", approved: "已通过", rejected: "未通过" };
  return map[status] || "待审核";
}

async function sendReplyHandler() {
  if (!reply.value.trim()) {
    messageError.value = "请输入回复内容";
    return;
  }

  sending.value = true;
  messageError.value = "";
  try {
    await sendMessage({ senderRole: "elderly", content: reply.value });
    reply.value = "";
    await loadMessages();
  } catch (error) {
    messageError.value = error.message || "发送失败";
  } finally {
    sending.value = false;
  }
}

function openFeedbackDialog(item) {
  feedbackError.value = "";
  feedbackSuccess.value = "";
  feedbackItem.value = item;
  feedbackForm.value = {
    feedbackType: "",
    content: "",
    relatedOrderId: item.id
  };
  showFeedbackDialog.value = true;
}

async function submitFeedback() {
  if (!feedbackForm.value.content.trim()) {
    feedbackError.value = "请输入反馈内容";
    return;
  }

  submittingFeedback.value = true;
  feedbackError.value = "";
  feedbackSuccess.value = "";
  try {
    await submitFeedbackApi({
      content: feedbackForm.value.content,
      feedbackType: feedbackForm.value.feedbackType,
      relatedOrderId: feedbackForm.value.relatedOrderId
    });
    feedbackSuccess.value = "反馈已提交，管理员会尽快处理";
    setTimeout(() => {
      showFeedbackDialog.value = false;
      feedbackSuccess.value = "";
    }, 1500);
  } catch (error) {
    feedbackError.value = error.message || "反馈提交失败";
  } finally {
    submittingFeedback.value = false;
  }
}

let debounceTimer = null;

function onNewMessage(payload) {
  console.log("老人端收到推送，开始刷新列表...", payload);
  if (payload && payload.type === "new_message") {
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      loadMessages();
      debounceTimer = null;
    }, 300);
  }
}

function onNotification(payload) {
  console.log("老人端收到通知:", payload);
}

onMounted(async () => {
  console.log("老人端组件挂载，正在启动监控...");
  userStore.registerMessageCallback(onNewMessage, onNotification);
  userStore.initWebSocket();
  await Promise.all([loadData(), loadBindInfo(), loadMessages()]);
});

onUnmounted(() => {
  console.log("老人端组件销毁清理回调...");
  userStore.registerMessageCallback(null, null);
});

watch(filterStatus, () => {
  currentPage.value = 1;
  loadData();
});
</script>

<style scoped>
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.modal-header h3 {
  margin: 0;
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
.modal {
  background: radial-gradient(circle at top left, rgba(184, 92, 56, 0.12), transparent 26%),
              radial-gradient(circle at top right, rgba(47, 125, 77, 0.1), transparent 22%),
              linear-gradient(180deg, #f6f1e7, #efe6d8 48%, #f8f5ef);
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
.task-address {
  font-weight: 600;
  color: #333;
}
.task-salary {
  font-size: 16px;
  font-weight: 700;
  color: #d7845c;
}
.health-status {
  margin-top: 8px;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
}
.health-pending {
  background: #fff3e0;
  color: #e65100;
}
.health-approved {
  background: #e8f5e9;
  color: #2e7d32;
}
.health-rejected {
  background: #ffebee;
  color: #c62828;
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
  z-index: 1000;
}
.modal {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
}
.modal h3 {
  margin-bottom: 16px;
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
.btn-warning {
  background: #ff9800;
  color: #fff;
}
.btn-warning:hover {
  background: #f57c00;
}
.btn-primary {
  background: #1976d2;
  color: #fff;
}
.btn-primary:hover {
  background: #1565c0;
}
.success-text {
  color: #4caf50;
  margin-bottom: 12px;
  font-weight: 600;
}
.select-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
}
</style>
