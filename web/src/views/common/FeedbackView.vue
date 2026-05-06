<template>
  <div :class="['feedback-page', userStore.fontSize || 'medium']">
    
    <header class="header-section">
      <h2 class="page-title">反馈与帮助</h2>
      <p class="page-subtitle">您的意见对我们非常重要，我们会认真阅读每一条反馈</p>
    </header>

    <section class="card submit-card">
      <h3 class="card-inner-title">📝 提交新反馈</h3>
      
      <div class="form-grid">
        <div class="form-item">
          <label>问题类型</label>
          <div class="input-wrapper">
            <select v-model="form.feedbackType" class="modern-input">
              <option value="">请选择类型</option>
              <option value="employer_not_pay">雇主未支付</option>
              <option value="task_issue">任务问题</option>
              <option value="elderly_dispute">服务纠纷</option>
              <option value="other">其他问题</option>
            </select>
          </div>
        </div>

        <div class="form-item">
          <label>关联订单号 <span class="optional-hint">(可选)</span></label>
          <div class="input-wrapper">
            <input 
              type="text" 
              v-model="form.relatedOrderId" 
              class="modern-input" 
              placeholder="请输入订单编号"
            />
          </div>
          <p class="field-tip">💡 如果不是针对特定订单的问题，此项可以不填。</p>
        </div>

        <div class="form-item full-width">
          <label>反馈详细内容</label>
          <textarea 
            v-model="form.content" 
            class="modern-input modern-textarea" 
            placeholder="请详细描述您遇到的问题..."
          ></textarea>
        </div>
      </div>

      <button class="btn-primary" :disabled="submitting" @click="submitHandler">
        {{ submitting ? '提交中...' : '提交反馈' }}
      </button>

      <Transition name="fade">
        <p v-if="submitError" class="msg-text error">❌ {{ submitError }}</p>
      </Transition>
      <Transition name="fade">
        <p v-if="submitSuccess" class="msg-text success">✅ {{ submitSuccess }}</p>
      </Transition>
    </section>

    <section class="list-section">
      <div class="section-header">
        <h3 class="card-inner-title">我的反馈记录</h3>
        <span class="count-pill" v-if="feedbackList.length">{{ feedbackList.length }}</span>
      </div>

      <div v-if="loading" class="state-container">正在加载记录...</div>
      <div v-else-if="!feedbackList.length" class="state-container empty">
        <p>暂无反馈记录</p>
      </div>

      <div v-else class="feedback-list">
        <div v-for="item in feedbackList" :key="item.id" class="history-card">
          <div class="item-header">
            <span class="type-badge">{{ feedbackTypeText(item.feedbackType) }}</span>
            <span :class="['status-text', item.status]">
              {{ statusText(item.status) }}
            </span>
          </div>
          
          <div class="item-body">
            <p class="content-text">{{ item.content }}</p>
          </div>

          <div class="item-footer">
            <span class="time-stamp">📅 {{ formatTime(item.createdAt) }}</span>
            <span class="order-link" v-if="item.relatedOrderId">关联订单：{{ item.relatedOrderId }}</span>
          </div>

          <div v-if="item.adminReply" class="admin-reply">
            <div class="reply-label">官方回复：</div>
            <p class="reply-msg">{{ item.adminReply }}</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { submitFeedback, getMyFeedbackList } from "../../api/feedback";
import { useUserStore } from "../../stores/user";

const userStore = useUserStore();

const loading = ref(false);
const feedbackList = ref([]);
const submitting = ref(false);
const submitError = ref("");
const submitSuccess = ref("");

const form = ref({
  feedbackType: "",
  content: "",
  relatedOrderId: "" // 改为字符串，方便输入框使用
});

function feedbackTypeText(type) {
  const map = {
    employer_not_pay: "雇主未支付",
    task_issue: "任务问题",
    elderly_dispute: "服务纠纷",
    other: "其他问题"
  };
  return map[type] || type || "其他问题";
}

function statusText(status) {
  const map = { pending: "待处理", handled: "已回复", resolved: "已解决" };
  return map[status] || status;
}

function formatTime(time) {
  if (!time) return "";
  return new Date(time).toLocaleString("zh-CN", {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  });
}

async function loadData() {
  loading.value = true;
  try {
    const res = await getMyFeedbackList();
    feedbackList.value = res.data || [];
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
}

async function submitHandler() {
  if (!form.value.content.trim()) {
    submitError.value = "请填写反馈内容";
    return;
  }
  submitting.value = true;
  submitError.value = "";
  try {
    await submitFeedback({
      content: form.value.content,
      feedbackType: form.value.feedbackType,
      relatedOrderId: form.value.relatedOrderId || null
    });
    form.value = { feedbackType: "", content: "", relatedOrderId: "" };
    await loadData();
    submitSuccess.value = "反馈已提交";
    setTimeout(() => { submitSuccess.value = ""; }, 3000);
  } catch (e) {
    submitError.value = e.message || "提交失败";
  } finally {
    submitting.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
/* 1. 字体大小阶梯控制 */
.feedback-page.small { --base-font: 14px; --title-font: 18px; }
.feedback-page.medium { --base-font: 16px; --title-font: 20px; }
.feedback-page.large { --base-font: 20px; --title-font: 24px; }
.feedback-page.xlarge { --base-font: 24px; --title-font: 28px; }

.feedback-page {
  padding: 20px;
  background-color: #fcf9f6;
  min-height: 100vh;
  font-size: var(--base-font);
  transition: all 0.2s ease;
}

/* 2. 布局样式 */
.header-section { margin-bottom: 24px; }
.page-title { font-size: var(--title-font); color: #5c4033; margin: 0; font-weight: 800; }
.page-subtitle { font-size: 0.8em; color: #a68d80; margin-top: 6px; }

.card {
  background: #ffffff;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 8px 20px rgba(189, 135, 101, 0.05);
  border: 1px solid #f2e9e1;
}

.submit-card { background: #fffcf9; border-color: #f3d2bd; margin-bottom: 30px; }
.card-inner-title { font-size: 1.1em; color: #5c4033; margin-bottom: 20px; font-weight: 700; }
.section-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.full-width { grid-column: span 2; }

.form-item { display: flex; flex-direction: column; gap: 8px; }
.form-item label { font-weight: 700; color: #8c6a5a; font-size: 0.9em; }
.optional-hint { font-weight: normal; color: #c0b0a8; font-size: 0.8em; }

/* 提示文字样式 */
.field-tip {
  font-size: 0.8em;
  color: #d98a67;
  margin: 0;
  padding-left: 4px;
}

/* 3. 输入框/选择框 */
.modern-input {
  width: 100%;
  padding: 0.8em 1em;
  border: 2px solid #f2e9e1;
  border-radius: 12px;
  background-color: #ffffff;
  color: #5c4033;
  font-size: var(--base-font);
  outline: none;
  box-sizing: border-box;
}
.modern-input:focus { border-color: #d98a67; }
.modern-textarea { min-height: 120px; resize: vertical; }

/* 4. 按钮 */
.btn-primary {
  width: 100%;
  margin-top: 24px;
  background: linear-gradient(135deg, #d98a67, #bc714e);
  color: white;
  border: none;
  padding: 0.9em;
  border-radius: 14px;
  font-size: var(--base-font);
  font-weight: bold;
  cursor: pointer;
  box-shadow: 0 6px 15px rgba(217, 138, 103, 0.3);
}

/* 5. 列表记录 */
.feedback-list { display: flex; flex-direction: column; gap: 16px; }
.history-card {
  background: white;
  border-radius: 18px;
  padding: 20px;
  border: 1px solid #f2e9e1;
}

.item-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.type-badge { background: #fdf2e9; color: #d98a67; padding: 4px 10px; border-radius: 8px; font-size: 0.8em; font-weight: bold; }

.status-text { font-size: 0.85em; font-weight: bold; }
.status-text.pending { color: #f39c12; }
.status-text.resolved { color: #27ae60; }

.item-footer { display: flex; justify-content: space-between; font-size: 0.8em; color: #a68d80; margin-top: 10px; }

/* 6. 管理员回复 */
.admin-reply {
  margin-top: 16px;
  background: #faf8f6;
  border-radius: 12px;
  padding: 14px;
  border-left: 4px solid #d98a67;
}
.reply-label { font-size: 0.85em; font-weight: bold; color: #d98a67; }
.reply-msg { margin-top: 4px; color: #5c4033; font-size: 0.95em; }

/* 7. 提示语 */
.msg-text { margin-top: 12px; text-align: center; font-size: 0.9em; }
.success { color: #27ae60; }
.error { color: #e74c3c; }

.state-container { padding: 40px; text-align: center; color: #a68d80; }
</style>