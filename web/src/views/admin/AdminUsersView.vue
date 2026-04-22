<template>
  <section class="panel">
    <h3 class="section-title">用户管理</h3>
    
    <div class="filter-row">
      <div class="filter-item">
        <img src="../../assets/icons/身份证.svg" class="filter-icon" />
        <select v-model="filterRole" class="filter-select">
          <option value="">全部身份</option>
          <option value="elderly">老人</option>
          <option value="employer">雇主</option>
          <option value="child">子女</option>
          <option value="admin">管理员</option>
        </select>
      </div>
      <div class="filter-item">
        <img src="../../assets/icons/状态.svg" class="filter-icon" />
        <select v-model="filterEnabled" class="filter-select">
          <option value="">全部状态</option>
          <option value="true">正常</option>
          <option value="false">已禁用</option>
        </select>
      </div>
      <div class="filter-item">
        <img src="../../assets/icons/体检报告.svg" class="filter-icon" />
        <select v-model="filterHealth" class="filter-select">
          <option value="">全部体检</option>
          <option value="pending">待审核</option>
          <option value="approved">已通过</option>
          <option value="rejected">未通过</option>
        </select>
      </div>
    </div>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText" class="success-text">{{ successText }}</p>
    <table v-if="users.length" class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>账号</th>
          <th>身份</th>
          <th>昵称</th>
          <th>手机号</th>
          <th>体检报告</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in users" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.username }}</td>
          <td>{{ roleText(item.role_type) }}</td>
          <td>{{ item.nickname || '-' }}</td>
          <td>{{ item.mobile || '-' }}</td>
          <td>
            <span v-if="item.role_type === 'elderly'" class="health-status" :class="'health-' + (item.health_report_status || 'none')">
              {{ healthStatusText(item.health_report_status) }}
            </span>
            <a v-if="item.role_type === 'elderly' && item.health_report_url" :href="getFullUrl(item.health_report_url)" target="_blank" class="report-link">查看报告</a>
            <span v-else class="muted">-</span>
          </td>
          <td>
            <span class="status-badge" :class="item.enabled ? 'status-active' : 'status-disabled'">
              {{ item.enabled ? '正常' : '已禁用' }}
            </span>
          </td>
          <td>
            <button 
              v-if="item.role_type === 'elderly' && item.health_report_status === 'pending'" 
              class="action-btn review-btn"
              :disabled="actionLoading"
              @click="reviewHealth(item)"
            >
              审核
            </button>
            <button 
              v-if="item.role_type !== 'admin'" 
              class="action-btn" 
              :style="{ background: item.enabled ? '#dc3545' : '#28a745' }"
              :disabled="actionLoading || item.enabled === undefined"
              @click="toggleEnabled(item)"
            >
              {{ item.enabled ? '禁用' : '启用' }}
            </button>
            <span v-if="item.role_type === 'admin'" class="muted">-</span>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty">暂无用户数据</div>
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
        <h3 class="section-title">审核体检报告</h3>
        <button class="modal-close" @click="showDialog = false">×</button>
      </div>
      <p class="modal-info">请选择该老人的身体状况：</p>
      <div class="condition-options">
        <button
          v-for="c in conditions"
          :key="c.value"
          class="condition-btn"
          :class="{ active: selectedCondition === c.value }"
          @click="selectedCondition = c.value"
        >
          {{ c.label }}
        </button>
      </div>
      <p v-if="dialogError" class="error-text">{{ dialogError }}</p>
      <div class="button-row">
        <button class="btn btn-success" style="flex: 1" type="button" :disabled="reviewing" @click="confirmReview(true)">
          {{ reviewing ? '审核中...' : '通过' }}
        </button>
        <button class="btn btn-danger" style="flex: 1" type="button" :disabled="reviewing" @click="confirmReview(false)">
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
          <p>确定要{{ confirmActionName }}该用户吗？</p>
        </div>
        <div class="button-row">
          <button class="btn btn-success" style="flex: 1" :disabled="actionLoading" @click="confirmToggleAction">
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
import { getUserList, setUserEnabled, reviewHealthReport } from "../../api/user";
import AppPagination from "../../components/AppPagination.vue";

const users = ref([]);
const total = ref(0);
const errorText = ref("");
const successText = ref("");
const actionLoading = ref(false);

const filterRole = ref("");
const filterEnabled = ref("");
const filterHealth = ref("");
const currentPage = ref(1);
const pageSize = ref(10);

const showDialog = ref(false);
const reviewing = ref(false);
const selectedUser = ref(null);
const selectedCondition = ref("");
const dialogError = ref("");

const showConfirmDialog = ref(false);
const confirmItemId = ref(null);
const confirmActionName = ref("");
const tempItem = ref(null);

const conditions = [
  { value: "healthy", label: "健康" },
  { value: "fair", label: "一般" }
];

function getFullUrl(url) {
  if (!url) return "";
  if (url.startsWith("http")) return url;
  if (url.startsWith("/")) {
    return `${window.location.origin}${url}`;
  }
  return `${window.location.origin}/${url}`;
}

function roleText(role) {
  const map = { elderly: "老人", employer: "雇主", child: "子女", admin: "管理员" };
  return map[role] || role;
}

function healthStatusText(status) {
  const map = { pending: "待审核", approved: "已通过", rejected: "未通过" };
  return status ? (map[status] || "待审核") : "未上传";
}

async function loadData() {
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      roleType: filterRole.value || undefined,
      enabled: filterEnabled.value === "" ? undefined : filterEnabled.value === "true",
      healthStatus: filterHealth.value || undefined
    };
    const res = await getUserList(params);
    users.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch (error) {
    errorText.value = error.message || "用户数据加载失败";
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

async function reviewHealth(item) {
  selectedUser.value = item;
  selectedCondition.value = "";
  dialogError.value = "";
  showDialog.value = true;
}

async function confirmReview(approved) {
  if (approved && !selectedCondition.value) {
    dialogError.value = "请选择身体状况";
    return;
  }
  reviewing.value = true;
  dialogError.value = "";
  errorText.value = "";
  successText.value = "";
  try {
    const res = await reviewHealthReport(selectedUser.value.id, approved, selectedCondition.value);
    successText.value = res.message;
    showDialog.value = false;
    await loadData();
  } catch (error) {
    dialogError.value = error.message || "操作失败";
  } finally {
    reviewing.value = false;
  }
}

async function toggleEnabled(item) {
  const newEnabled = item.enabled;
  const action = newEnabled ? "禁用" : "启用";
  
  confirmItemId.value = item.id;
  confirmActionName.value = action;
  tempItem.value = item;
  showConfirmDialog.value = true;
}

async function confirmToggleAction() {
  const item = tempItem.value;
  const newEnabled = item.enabled;
  
  actionLoading.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const res = await setUserEnabled(item.id, !newEnabled);
    successText.value = res.message;
    showConfirmDialog.value = false;
    await loadData();
  } catch (error) {
    errorText.value = error.message || "操作失败";
  } finally {
    actionLoading.value = false;
  }
}

onMounted(loadData);

watch([filterRole, filterEnabled, filterHealth], () => {
  currentPage.value = 1;
  loadData();
});
</script>

<style scoped>
.filter-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-icon {
  width: 24px;
  height: 24px;
}
.filter-select {
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  background: #fff;
  cursor: pointer;
  min-width: 140px;
}
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 700;
}
.status-active {
  color: #19613d;
  background: #dcf4e4;
}
.status-disabled {
  color: #7a2c2c;
  background: #f8dfdf;
}
.health-status {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 700;
}
.health-pending {
  color: #8a5a00;
  background: #fff0ce;
}
.health-approved {
  color: #19613d;
  background: #dcf4e4;
}
.health-rejected {
  color: #7a2c2c;
  background: #f8dfdf;
}
.health-none {
  color: #666;
  background: #f5f5f5;
}
.review-btn {
  background: #2196f3;
  color: #fff;
  margin-right: 8px;
}
.action-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.action-btn:first-of-type {
  background: #dc3545;
  color: #fff;
}
.action-btn:last-of-type {
  background: #28a745;
  color: #fff;
}
.action-btn:hover {
  opacity: 0.85;
}
.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.muted {
  color: #999;
}
.success-text {
  color: #28a745;
  margin-bottom: 12px;
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
  background: radial-gradient(circle at top left, rgba(184, 92, 56, 0.12), transparent 26%),
              radial-gradient(circle at top right, rgba(47, 125, 77, 0.1), transparent 22%),
              linear-gradient(180deg, #f6f1e7, #efe6d8 48%, #f8f5ef);
  padding: 20px;
  border-radius: 8px;
  width: 80%;
  max-width: 360px;
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
.modal-info {
  margin: 0 0 16px;
  text-align: center;
}

.condition-options {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.condition-btn {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
}

.condition-btn.active {
  border-color: #4a90d9;
  background: #e8f4fc;
  color: #1976d2;
}
</style>
