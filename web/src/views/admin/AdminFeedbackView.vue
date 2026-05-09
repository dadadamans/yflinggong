<template>
  <section class="panel">
    <div class="topbar-modern">
      <div class="title-group">
        <h3 class="section-title">用户反馈管理</h3>
        <p class="section-subtitle">查看并回复所有身份用户的反馈建议</p>
      </div>
      <button class="btn-refresh" type="button" :disabled="loading" @click="loadData">
        <span class="refresh-icon">↻</span> 刷新数据
      </button>
    </div>

    <div class="filter-tabs">
      <div class="filter-tab" :class="{ active: filterStatus === '' }" @click="handleFilterChange('')">
        <div class="tab-content">
          <span class="tab-label">全部反馈</span>
          <span class="tab-count">{{ totalCount }}</span>
        </div>
      </div>

      <div class="filter-tab" :class="{ active: filterStatus === 'pending' }" @click="handleFilterChange('pending')">
        <div class="tab-icon-wrapper pending">
          <img src="../../assets/icons/首页统计-反馈待处理.svg" class="tab-icon" />
        </div>
        <div class="tab-content">
          <span class="tab-label">待处理</span>
          <span class="tab-count highlight">{{ pendingCount }}</span>
        </div>
      </div>

      <div class="filter-tab" :class="{ active: filterStatus === 'handled' }" @click="handleFilterChange('handled')">
        <div class="tab-icon-wrapper resolved">
          <img src="../../assets/icons/done.svg" class="tab-icon" />
        </div>
        <div class="tab-content">
          <span class="tab-label">已回复</span>
          <span class="tab-count">{{ handledCount }}</span>
        </div>
      </div>

      <div class="filter-tab" :class="{ active: filterStatus === 'resolved' }" @click="handleFilterChange('resolved')">
        <div class="tab-icon-wrapper resolved">
          <img src="../../assets/icons/已处理.svg" class="tab-icon" />
        </div>
        <div class="tab-content">
          <span class="tab-label">已处理</span>
          <span class="tab-count">{{ resolvedCount }}</span>
        </div>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="errorText" class="alert alert-error">{{ errorText }}</div>
    </Transition>
    <Transition name="fade">
      <div v-if="successText" class="alert alert-success">{{ successText }}</div>
    </Transition>

    <div class="table-container">
      <table v-if="feedbackList.length" class="modern-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>反馈用户</th>
            <th>身份</th>
            <th>问题类型</th>
            <th>反馈内容</th>
            <th>关联订单</th>
            <th>状态</th>
            <th>提交时间</th>
            <th class="text-center">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in feedbackList" :key="item.id">
            <td class="id-cell">#{{ item.id }}</td>
            <td class="user-cell">
              <span class="user-name">{{ item.userName || '未知用户' }}</span>
            </td>
            <td>
              <span class="role-tag" :class="'role-' + item.role">{{ roleText(item.role) }}</span>
            </td>
            <td><span class="type-text">{{ feedbackTypeText(item.feedbackType) }}</span></td>
            <td class="content-cell">
              <div class="content-tooltip" :title="item.content">{{ item.content }}</div>
            </td>
            <td>
              <span v-if="item.relatedOrderId" class="order-tag">#{{ item.relatedOrderId }}</span>
              <span v-else class="text-muted">-</span>
            </td>
            <td>
              <span class="status-pill" :class="'status-' + item.status">
                {{ item.status === 'pending' ? '待处理' : item.status === 'resolved' ? '已解决' : '已回复' }}
              </span>
            </td>
            <td class="time-cell">{{ formatDate(item.createdAt) }}</td>
            <td class="text-center">
              <button
                class="btn-action"
                :class="item.status === 'pending' ? 'btn-action-primary' : 'btn-action-view'"
                @click="openReplyModal(item)"
              >
                {{ item.status === 'pending' ? '回复处理' : '查看/详情' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <div v-else class="empty-placeholder">
        <div class="empty-icon">📂</div>
        <p>暂无反馈数据记录</p>
      </div>
    </div>

    <Transition name="modal-fade">
      <div v-if="showReplyModal" class="modal-overlay" @click="showReplyModal = false">
        <div class="modal-card modern-reply-modal" @click.stop>
          <div class="modal-decoration"></div>
          
          <div class="modal-header">
            <div class="header-main">
              <div class="icon-circle">✉</div>
              <div>
                <h3>反馈处理回信</h3>
                <p>工单编号：#{{ activeFeedback.id }}</p>
              </div>
            </div>
            <button class="btn-close-circle" @click="showReplyModal = false">×</button>
          </div>

          <div class="modal-body">
            <div class="info-section">
              <div class="user-profile">
                <div class="avatar-placeholder">{{ activeFeedback.userName?.charAt(0) || 'U' }}</div>
                <div class="user-meta">
                  <span class="u-name">{{ activeFeedback.userName }}</span>
                  <span class="role-tag-small" :class="'role-' + activeFeedback.role">
                    {{ roleText(activeFeedback.role) }}
                  </span>
                </div>
                <span class="time-stamp">{{ formatDate(activeFeedback.createdAt) }}</span>
              </div>
              
              <div v-if="activeFeedback.relatedOrderId" class="related-order-info">
                <span class="info-icon">📋</span>
                <span>关联订单：#{{ activeFeedback.relatedOrderId }}</span>
              </div>
              
              <div class="content-bubble">
                <div class="bubble-label">反馈描述</div>
                <p class="user-msg-text">{{ activeFeedback.content }}</p>
              </div>
            </div>

            <div class="reply-section">
              <div class="field-label">
                <span class="dot"></span> 官方回复处理
              </div>
              <div class="textarea-wrapper">
                <textarea
                  v-model="replyForm.reply"
                  class="modern-textarea-v2"
                  placeholder="请在此输入您的回复内容，建议详细说明处理方案..."
                  rows="5"
                ></textarea>
                <div class="char-count">{{ replyForm.reply.length }} / 500</div>
              </div>
            </div>
          </div>

          <div class="modal-footer-v2">
            <button class="btn-minimal" @click="showReplyModal = false">取消</button>
            <button 
              class="btn-gradient" 
              :disabled="submitting || !replyForm.reply.trim()" 
              @click="submitReply"
            >
              <span v-if="!submitting">确认发送回信</span>
              <span v-else class="loading-spinner"></span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <div class="pagination-wrapper">
      <AppPagination
        v-if="total > 0"
        :page="currentPage"
        :page-size="pageSize"
        :total="total"
        @update:page="handlePageChange"
        @update:pageSize="handlePageSizeChange"
      />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
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
const showReplyModal = ref(false);
const activeFeedback = ref({});
const replyForm = ref({ status: 'handled', reply: '' });

const totalCount = ref(0);
const pendingCount = ref(0);
const handledCount = ref(0);
const resolvedCount = ref(0);

function formatDate(dateStr) {
  if (!dateStr) return "-";
  return dateStr.substring(0, 16).replace("T", " ");
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
    other: "其他"
  };
  return map[type] || type || "一般反馈";
}

async function loadData() {
  loading.value = true;
  try {
    const res = await getAdminFeedbackList();
    const allData = res.data || [];
    
    totalCount.value = allData.length;
    pendingCount.value = allData.filter(i => i.status === "pending").length;
    handledCount.value = allData.filter(i => i.status === "handled").length;
    resolvedCount.value = allData.filter(i => i.status === "resolved").length;

    let filtered = allData;
    if (filterStatus.value) {
      filtered = allData.filter(item => item.status === filterStatus.value);
    }
    
    feedbackList.value = filtered;
    total.value = filtered.length;
  } catch (error) {
    errorText.value = error.message;
  } finally {
    loading.value = false;
  }
}

function handleFilterChange(status) {
  filterStatus.value = status;
  loadData();
}

function openReplyModal(item) {
  activeFeedback.value = { ...item };
  replyForm.value = { status: 'handled', reply: item.adminReply || "" };
  showReplyModal.value = true;
}

async function submitReply() {
  if (!replyForm.value.reply.trim()) return;
  submitting.value = true;
  try {
    await updateFeedbackStatus(activeFeedback.value.id, {
      ...replyForm.value,
      status: 'resolved' 
    });
    successText.value = "回复处理成功！";
    showReplyModal.value = false;
    setTimeout(() => successText.value = "", 3000);
    await loadData();
  } catch (error) {
    errorText.value = error.message;
    setTimeout(() => errorText.value = "", 3000);
  } finally {
    submitting.value = false;
  }
}

function handlePageChange(page) { currentPage.value = page; loadData(); }
function handlePageSizeChange(size) { pageSize.value = size; loadData(); }

onMounted(loadData);
</script>

<style scoped>
/* 核心容器 */
.panel {
  padding: 24px;
  background-color: #fcf9f6;
  min-height: 100vh;
}

/* 顶部栏 */
.topbar-modern {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.section-title { font-size: 22px; color: #5c4033; margin: 0; font-weight: 700; }
.section-subtitle { font-size: 13px; color: #a68d80; margin-top: 4px; }

.btn-refresh {
  background: #fff;
  border: 1px solid #f2e9e1;
  padding: 10px 18px;
  border-radius: 12px;
  color: #8c6a5a;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.btn-refresh:hover { background: #f2e9e1; }

/* 统计卡片 */
.filter-tabs {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
}
.filter-tab {
  flex: 1;
  background: #fff;
  border: 2px solid transparent;
  border-radius: 24px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(0,0,0,0.02);
}
.filter-tab:hover { transform: translateY(-4px); box-shadow: 0 10px 20px rgba(189,135,101,0.08); }
.filter-tab.active { border-color: #d98a67; background: #fffcf9; }

.tab-icon-wrapper {
  width: 50px;
  height: 50px;
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tab-icon-wrapper.pending { background: #fff7ed; }
.tab-icon-wrapper.resolved { background: #f0fdf4; }
.tab-icon { width: 28px; height: 28px; }

.tab-content { display: flex; flex-direction: column; }
.tab-label { font-size: 15px; color: #a68d80; font-weight: 600; }
.tab-count { font-size: 28px; font-weight: 800; color: #5c4033; line-height: 1.2; }
.tab-count.highlight { color: #d98a67; }

/* 表格样式 */
.table-container {
  background: #fff;
  border-radius: 24px;
  padding: 24px;
  border: 1px solid #f2e9e1;
  box-shadow: 0 4px 20px rgba(92, 64, 51, 0.05);
}
.modern-table { width: 100%; border-collapse: collapse; }
.modern-table th {
  text-align: left;
  padding: 18px 16px;
  font-size: 17px;
  font-weight: 600;
}
.modern-table td { padding: 18px 16px; border-bottom: 1px solid #faf8f6; font-size: 16px; color: #5c4033; }

.role-tag {
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: white;
}
.role-tag-small {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  color: white;
}
.role-elderly { background: #d98a67; }
.role-employer { background: #8c6a5a; }
.role-child { background: #6b8e23; }

.status-pill {
  padding: 4px 12px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}
.status-pending { background: #fff7ed; color: #c2410c; border: 1px solid #ffedd5; }
.status-resolved { background: #f0fdf4; color: #15803d; border: 1px solid #dcfce7; }

.content-cell { max-width: 180px; }
.content-tooltip { 
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; 
  color: #8c6a5a; font-style: italic;
}

.order-tag {
  background: #fff7ed;
  color: #c2410c;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  display: inline-block;
}
.text-muted { color: #a68d80; }

.related-order-info {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff7ed;
  border: 1px solid #ffedd5;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 15px;
  color: #c2410c;
  margin-top: 12px;
}
.info-icon { font-size: 14px; }

.btn-action {
  padding: 8px 16px;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}
.btn-action-primary { background: #d98a67; color: white; }
.btn-action-primary:hover { background: #bf7a5a; transform: scale(1.05); }
.btn-action-view { background: #fcf9f6; color: #a68d80; border: 1px solid #f2e9e1; }

/* --- 重新设计的弹窗样式 --- */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(60, 42, 33, 0.4);
  backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}

.modern-reply-modal {
  position: relative;
  background: #ffffff;
  border-radius: 30px;
  width: 540px;
  max-width: 95vw;
  box-shadow: 0 25px 50px rgba(92, 64, 51, 0.2);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.modal-decoration {
  height: 8px;
  background: linear-gradient(90deg, #d98a67, #f2e9e1, #d98a67);
}

.modal-header {
  padding: 30px 30px 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.header-main { display: flex; gap: 16px; align-items: center; }

.icon-circle {
  width: 48px; height: 48px; background: #fff7ed; color: #d98a67;
  display: flex; align-items: center; justify-content: center;
  border-radius: 16px; font-size: 24px;
}

.modal-header h3 { font-size: 20px; color: #5c4033; margin: 0; font-weight: 800; }
.modal-header p { font-size: 12px; color: #a68d80; margin: 4px 0 0; }

.btn-close-circle {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: #fcf9f6; color: #a68d80; cursor: pointer;
  transition: all 0.3s; font-size: 22px;
  display: flex; align-items: center; justify-content: center;
}
.btn-close-circle:hover { background: #f2e9e1; color: #5c4033; transform: rotate(90deg); }

.modal-body { padding: 0 30px; }

.info-section {
  background: #faf8f6;
  padding: 20px;
  border-radius: 22px;
  margin-bottom: 24px;
  border: 1px dashed #e5ddd5;
}

.user-profile { display: flex; align-items: center; margin-bottom: 14px; }
.avatar-placeholder {
  width: 36px; height: 36px; background: #d98a67; color: white;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-weight: bold; margin-right: 12px; border: 2px solid #fff;
}

.user-meta { flex: 1; display: flex; align-items: center; gap: 10px; }
.u-name { font-weight: 700; color: #5c4033; font-size: 15px; }
.time-stamp { font-size: 12px; color: #a68d80; }

/* 反馈内容气泡样式优化 */
.content-bubble { 
  background: #ffffff; 
  padding: 16px; 
  border-radius: 16px; 
  box-shadow: 0 4px 10px rgba(0,0,0,0.02);
  /* 新增：限制最大高度并允许内容内部滚动 */
  max-height: 180px;
  display: flex;
  flex-direction: column;
}

.bubble-label { font-size: 11px; color: #a68d80; font-weight: 800; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 1px; }

.user-msg-text { 
  margin: 0; 
  font-size: 14px; 
  line-height: 1.7; 
  color: #5c4033; 
  /* 核心修复： */
  overflow-y: auto;          /* 纵向溢出自动显示滚动条 */
  word-break: break-all;     /* 强制长字符串换行 */
  white-space: pre-wrap;     /* 识别换行符并保留 */
  padding-right: 8px;        /* 为滚动条留空 */
}

/* 美化气泡内的滚动条 */
.user-msg-text::-webkit-scrollbar {
  width: 4px;
}
.user-msg-text::-webkit-scrollbar-thumb {
  background: #e5ddd5;
  border-radius: 10px;
}
.user-msg-text::-webkit-scrollbar-track {
  background: transparent;
}

.reply-section { margin-bottom: 24px; }
.field-label { display: flex; align-items: center; gap: 10px; font-size: 15px; font-weight: 700; color: #5c4033; margin-bottom: 15px; }
.field-label .dot { width: 8px; height: 8px; background: #d98a67; border-radius: 50%; box-shadow: 0 0 8px rgba(217,138,103,0.5); }

.textarea-wrapper { position: relative; }
.modern-textarea-v2 {
  width: 100%; border: 2px solid #f2e9e1; border-radius: 20px;
  padding: 18px; font-size: 14px; color: #5c4033; transition: all 0.3s;
  box-sizing: border-box; resize: none; background: #fff;
}
.modern-textarea-v2:focus { outline: none; border-color: #d98a67; box-shadow: 0 8px 20px rgba(217,138,103,0.1); }

.char-count { position: absolute; bottom: 15px; right: 20px; font-size: 12px; color: #a68d80; opacity: 0.7; }

.modal-footer-v2 {
  padding: 20px 30px 30px;
  display: flex; justify-content: flex-end; gap: 15px;
  background: #fcf9f6;
}

.btn-minimal { background: transparent; border: none; color: #a68d80; font-weight: 700; cursor: pointer; padding: 10px 20px; }

.btn-gradient {
  background: linear-gradient(135deg, #d98a67 0%, #bf7a5a 100%);
  color: white; border: none; padding: 14px 32px; border-radius: 16px;
  font-weight: 700; cursor: pointer; transition: all 0.3s;
  box-shadow: 0 6px 15px rgba(217, 138, 103, 0.3);
}
.btn-gradient:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 10px 20px rgba(217, 138, 103, 0.4); }
.btn-gradient:disabled { filter: grayscale(0.8); cursor: not-allowed; opacity: 0.7; }

/* 动画效果 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.5s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.modal-fade-enter-active { transition: all 0.3s ease-out; }
.modal-fade-leave-active { transition: all 0.2s ease-in; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.9) translateY(20px); }

.alert {
  padding: 12px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}
.alert-success { background: #f0fdf4; color: #15803d; border: 1px solid #dcfce7; }
.alert-error { background: #fef2f2; color: #b91c1c; border: 1px solid #fee2e2; }

.empty-placeholder { padding: 60px; text-align: center; color: #a68d80; }
.empty-icon { font-size: 40px; margin-bottom: 10px; }
</style>