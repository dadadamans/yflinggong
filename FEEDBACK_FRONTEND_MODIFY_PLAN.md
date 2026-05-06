# 前端 Feedback 功能完整修改方案

---

## 一、API 层 (src/api/feedback.js)

保持不变，当前 API 已适配：
- `submitFeedback` POST /api/feedback
- `getMyFeedbackList` GET /api/feedback
- `getAdminFeedbackList` GET /api/feedback/admin
- `updateFeedbackStatus` PUT /api/feedback/{id}

---

## 二、管理端 - AdminFeedbackView.vue

**修改说明：** 将"标记已处理"改为"回复处理"弹窗，支持管理员输入回复内容

### 完整代码

```vue
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
          <th>管理员回复</th>
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
          <td class="reply-cell">
            <span v-if="item.adminReply" class="reply-preview">{{ item.adminReply.substring(0, 20) }}...</span>
            <span v-else class="muted">-</span>
          </td>
          <td>
            <span class="status-badge" :class="'status-' + item.status">
              {{ item.status === 'pending' ? '待处理' : '已处理' }}
            </span>
          </td>
          <td>{{ formatDate(item.createdAt) }}</td>
          <td>
            <button
              class="action-btn"
              :class="item.status === 'pending' ? 'action-btn-primary' : 'action-btn-secondary'"
              @click="openReplyModal(item)"
            >
              {{ item.status === 'pending' ? '回复处理' : '查看/修改回复' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty">暂无反馈数据</div>

    <!-- 回复弹窗 -->
    <div v-if="showReplyModal" class="modal-mask" @click="showReplyModal = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>反馈处理回信</h3>
          <button class="modal-close" @click="showReplyModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="info-group">
            <label>用户信息：</label>
            <p class="info-text">
              <span class="role-badge" :class="'role-' + activeFeedback.role">{{ roleText(activeFeedback.role) }}</span>
              {{ activeFeedback.userName || '匿名用户' }}
            </p>
          </div>
          <div class="info-group">
            <label>问题类型：</label>
            <p class="info-text">{{ feedbackTypeText(activeFeedback.feedbackType) }}</p>
          </div>
          <div class="info-group">
            <label>用户反馈内容：</label>
            <p class="user-content">{{ activeFeedback.content }}</p>
          </div>
          <div class="field">
            <label>回复内容：</label>
            <textarea
              v-model="replyForm.reply"
              placeholder="请输入回复给用户的详细内容..."
              rows="4"
            ></textarea>
          </div>
        </div>
        <div class="button-row">
          <button class="btn btn-primary" :disabled="submitting" @click="submitReply">
            {{ submitting ? '提交中...' : '确认回信' }}
          </button>
          <button class="btn btn-danger" @click="showReplyModal = false">取消</button>
        </div>
      </div>
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
</template>

<script setup>
import { onMounted, ref, computed } from "vue";
import { getAdminFeedbackList, updateFeedbackStatus } from "../../api/feedback";
import AppPagination from "../../components/AppPagination.vue";

const loading = ref(false);
const submitting = ref(false);
const errorText = ref("");
const successText = ref("");
const feedbackList = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const filterStatus = ref("");

// 弹窗相关
const showReplyModal = ref(false);
const activeFeedback = ref({});
const replyForm = ref({ status: 'resolved', reply: '' });

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
    suggestion: "意见建议",
    complaint: "投诉",
    other: "其他"
  };
  return map[type] || type || "-";
}

async function loadData() {
  loading.value = true;
  errorText.value = "";
  successText.value = "";
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

function openReplyModal(item) {
  activeFeedback.value = { ...item };
  replyForm.value = {
    status: 'resolved',
    reply: item.adminReply || ""
  };
  showReplyModal.value = true;
}

async function submitReply() {
  if (!replyForm.value.reply.trim()) {
    errorText.value = "回复内容不能为空";
    return;
  }
  submitting.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    await updateFeedbackStatus(activeFeedback.value.id, replyForm.value);
    successText.value = "处理成功，已回信给用户";
    showReplyModal.value = false;
    await loadData();
  } catch (error) {
    errorText.value = error.message || "操作失败";
  } finally {
    submitting.value = false;
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
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reply-cell {
  max-width: 120px;
  font-size: 12px;
}
.reply-preview {
  color: #4a90d9;
}
.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}
.action-btn-primary {
  background: #4a90d9;
  color: #fff;
}
.action-btn-primary:hover {
  background: #357abd;
}
.action-btn-secondary {
  background: #e8f5e9;
  color: #2e7d32;
}
.action-btn-secondary:hover {
  background: #c8e6c9;
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

/* 弹窗样式 */
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
  max-width: 500px;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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
}
.modal-close:hover {
  color: #333;
}
.modal-body {
  margin-bottom: 16px;
}
.info-group {
  margin-bottom: 12px;
}
.info-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 4px;
  color: #666;
}
.info-text {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-content {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 8px;
  line-height: 1.5;
  margin: 0;
}
.field {
  margin-top: 12px;
}
.field label {
  display: block;
  font-weight: 600;
  margin-bottom: 4px;
  color: #666;
}
.field textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
}
.field textarea:focus {
  outline: none;
  border-color: #4a90d9;
}
.button-row {
  display: flex;
  gap: 12px;
}
.btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}
.btn-primary {
  background: #4a90d9;
  color: #fff;
}
.btn-primary:hover {
  background: #357abd;
}
.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.btn-danger {
  background: #f5f5f5;
  color: #666;
}
.btn-danger:hover {
  background: #e0e0e0;
}
</style>
```

---

## 三、用户端 - ElderlyOrdersView.vue

**修改说明：** 复用现有的反馈弹窗逻辑，提交成功后可以查看自己的反馈列表及管理员回复

### 需要修改的部分

1. 在页面顶部添加"我的反馈"模块
2. 复用现有的 `showFeedbackDialog` 弹窗
3. 加载时获取用户的反馈列表并展示

### 完整修改方案

在 ElderlyOrdersView.vue 的 template 中，在 `<div class="split">` 之前添加：

```vue
<!-- 我的反馈中心模块 -->
<section class="panel" style="margin-bottom: 16px">
  <div class="topbar" style="margin-bottom: 12px">
    <div>
      <h3 class="section-title">我的反馈</h3>
    </div>
    <button class="btn-ghost" type="button" @click="loadMyFeedback">刷��</button>
  </div>

  <div v-if="myFeedbackList.length" class="feedback-list">
    <div v-for="item in myFeedbackList" :key="item.id" class="feedback-item">
      <div class="feedback-header">
        <span class="f-type">{{ feedbackTypeText(item.feedbackType) }}</span>
        <span class="status-badge" :class="'status-' + item.status">
          {{ item.status === 'pending' ? '待处理' : '已处理' }}
        </span>
      </div>
      <p class="f-content">{{ item.content }}</p>
      <div v-if="item.adminReply" class="f-reply">
        <span class="reply-label">系统回复：</span>
        <p class="reply-text">{{ item.adminReply }}</p>
      </div>
      <small class="f-time">提交于：{{ formatTime(item.createdAt) }}</small>
    </div>
  </div>
  <div v-else class="empty">暂无反馈记录</div>
</section>
```

在 script 中添加：

```javascript
// 反馈列表相关
const myFeedbackList = ref([]);

async function loadMyFeedback() {
  try {
    const res = await getMyFeedbackList();
    myFeedbackList.value = res.data || [];
  } catch (error) {
    console.error("反馈列表加载失败", error);
  }
}

// onMounted 中添加 loadMyFeedback()
onMounted(async () => {
  console.log("老人端组件挂载，正在启动监控...");
  userStore.registerMessageCallback(onNewMessage, onNotification);
  userStore.initWebSocket();
  await Promise.all([loadData(), loadBindInfo(), loadMessages(), loadMyFeedback()]);
});
```

---

## 四、路由配置 (src/router/index.js)

如需新建独立的反馈中心页面：

```javascript
// 新增路由
const FeedbackCenterView = () => import("../views/elderly/FeedbackCenterView.vue");

// 在 elderly children 中添加
{ path: "elderly/feedback", component: FeedbackCenterView, meta: { title: "反馈中心", roles: ["elderly", "employer", "child"] } }
```

同时在 UserLayout.vue 的 navMap 中添加对应导航项。

---

## 五、执行顺序

| 步骤 | 文件 | 操作 |
|------|------|------|
| 1 | api/feedback.js | 保持不变 |
| 2 | views/admin/AdminFeedbackView.vue | 完全替换 |
| 3 | views/elderly/ElderlyOrdersView.vue | 添加反馈列表模块 |
| 4 | router/index.js | 如需独立页面则添加路由 |
| 5 | layout/UserLayout.vue | 如需独立页面则添加导航项 |