<template>
  <section class="panel">
    <div class="topbar-modern">
      <div class="title-group">
        <h3 class="section-title">用户管理中心</h3>
        <p class="section-subtitle">管理系统所有角色的账号状态、身份审核及体检信息</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" type="button" @click="loadData">
          <span class="refresh-icon">↻</span> 刷新列表
        </button>
      </div>
    </div>

    <div class="filter-card-group">
      <div class="filter-card">
        <div class="filter-content">
          <div class="filter-label-group">
            <img src="../../assets/icons/身份证.svg" class="filter-icon" />
            <span class="filter-label">账号身份</span>
          </div>
          <select v-model="filterRole" class="modern-select">
            <option value="">全部身份</option>
            <option value="elderly">老人</option>
            <option value="employer">雇主</option>
            <option value="child">子女</option>
            <option value="admin">管理员</option>
          </select>
        </div>
      </div>

      <div class="filter-card">
        <div class="filter-content">
          <div class="filter-label-group">
            <img src="../../assets/icons/状态.svg" class="filter-icon" />
            <span class="filter-label">账号状态</span>
          </div>
          <select v-model="filterEnabled" class="modern-select">
            <option value="">全部状态</option>
            <option value="true">正常使用</option>
            <option value="false">锁定禁用</option>
          </select>
        </div>
      </div>

      <div class="filter-card">
        <div class="filter-content">
          <div class="filter-label-group">
            <img src="../../assets/icons/体检报告.svg" class="filter-icon" />
            <span class="filter-label">体检审核</span>
          </div>
          <select v-model="filterHealth" class="modern-select">
            <option value="">全部体检</option>
            <option value="pending">待处理</option>
            <option value="approved">已通过</option>
            <option value="rejected">未通过</option>
          </select>
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
      <table v-if="users.length" class="modern-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>基本信息</th>
            <th>身份角色</th>
            <th>联系电话</th>
            <th>体检报告状态</th>
            <th>账号状态</th>
            <th class="text-center">操作管理</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in users" :key="item.id">
            <td class="id-cell">#{{ item.id }}</td>
            <td class="user-info-cell">
              <div class="user-meta">
                <span class="user-username">{{ item.username }}</span>
                <span class="user-nickname">{{ item.nickname || '未设置昵称' }}</span>
              </div>
            </td>
            <td>
              <span class="role-tag" :class="'role-' + item.role_type">
                {{ roleText(item.role_type) }}
              </span>
            </td>
            <td class="mobile-cell">{{ item.mobile || '-' }}</td>
            <td>
              <div v-if="item.role_type === 'elderly'" class="health-box">
                <span class="status-pill" :class="'health-' + (item.health_report_status || 'none')">
                  {{ healthStatusText(item.health_report_status) }}
                </span>
                <a v-if="item.health_report_url" :href="getFullUrl(item.health_report_url)" target="_blank" class="report-link">预览报告</a>
              </div>
              <span v-else class="muted">无需体检</span>
            </td>
            <td>
              <span class="status-dot-wrapper" :class="item.enabled ? 'active' : 'disabled'">
                <span class="dot"></span>
                {{ item.enabled ? '正常' : '已禁用' }}
              </span>
            </td>
            <td class="text-center">
              <div class="action-group">
                <button 
                  v-if="item.role_type === 'elderly' && item.health_report_status === 'pending'" 
                  class="btn-action btn-action-review"
                  :disabled="actionLoading"
                  @click="reviewHealth(item)"
                >
                  审核报告
                </button>
                <button 
                  v-if="item.role_type !== 'admin'" 
                  class="btn-action" 
                  :class="item.enabled ? 'btn-action-danger' : 'btn-action-success'"
                  :disabled="actionLoading || item.enabled === undefined"
                  @click="toggleEnabled(item)"
                >
                  {{ item.enabled ? '禁用账号' : '解封账号' }}
                </button>
                <span v-if="item.role_type === 'admin'" class="muted">系统预留</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-placeholder">
        <div class="empty-icon">👥</div>
        <p>暂无符合条件的用户数据记录</p>
      </div>
    </div>

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

    <Transition name="modal-fade">
      <div v-if="showDialog" class="modal-overlay" @click="showDialog = false">
        <div class="modal-card modern-modal" @click.stop>
          <div class="modal-decoration"></div>
          <div class="modal-header">
            <div class="header-main">
              <div class="icon-circle">📋</div>
              <div>
                <h3>健康报告审核</h3>
                <p>正在审核老人：{{ selectedUser?.nickname || selectedUser?.username }}</p>
              </div>
            </div>
            <button class="btn-close-circle" @click="showDialog = false">×</button>
          </div>
          
          <div class="modal-body">
            <div class="info-bubble">
              <p>请仔细核对体检报告附件，并对老人的健康等级进行初步判定：</p>
            </div>
            <div class="condition-grid">
              <button
                v-for="c in conditions"
                :key="c.value"
                class="condition-card"
                :class="{ active: selectedCondition === c.value }"
                @click="selectedCondition = c.value"
              >
                <span class="check-mark" v-if="selectedCondition === c.value">✓</span>
                {{ c.label }}
              </button>
            </div>
            <p v-if="dialogError" class="modal-error-text">{{ dialogError }}</p>
          </div>

          <div class="modal-footer-v2">
            <button class="btn-minimal" @click="showDialog = false">取消</button>
            <div class="footer-actions">
              <button class="btn-danger-outline" :disabled="reviewing" @click="confirmReview(false)">拒绝通过</button>
              <button class="btn-gradient" :disabled="reviewing" @click="confirmReview(true)">
                {{ reviewing ? '处理中...' : '审核通过' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="modal-fade">
      <div v-if="showConfirmDialog" class="modal-overlay" @click="showConfirmDialog = false">
        <div class="modal-card confirm-modal" @click.stop>
          <div class="modal-body text-center">
            <div class="warning-icon">⚠️</div>
            <h3 class="confirm-title">确认操作</h3>
            <p class="confirm-desc">您确定要 <span class="highlight">{{ confirmActionName }}</span> 用户 <span class="highlight">#{{ confirmItemId }}</span> 吗？</p>
            <div class="button-row-v2">
              <button class="btn-minimal" @click="showConfirmDialog = false">点错了</button>
              <button class="btn-gradient-confirm" :disabled="actionLoading" @click="confirmToggleAction">
                {{ actionLoading ? '执行中...' : '确定执行' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
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
/* --- 全局容器与标题 --- */
.panel { padding: 24px; background-color: #fcf9f6; min-height: 100vh; }
.topbar-modern { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.section-title { font-size: 22px; color: #5c4033; margin: 0; font-weight: 700; }
.section-subtitle { font-size: 13px; color: #a68d80; margin-top: 4px; }

.btn-refresh {
  background: #fff; border: 1px solid #f2e9e1; padding: 10px 18px; border-radius: 12px;
  color: #8c6a5a; cursor: pointer; transition: all 0.3s; display: flex; align-items: center; gap: 8px; font-weight: 600;
}
.btn-refresh:hover { background: #f2e9e1; }

/* --- 筛选统计卡片 --- */
.filter-card-group { display: flex; gap: 20px; margin-bottom: 30px; }
.filter-card {
  flex: 1; background: #fff; border-radius: 24px; padding: 16px 20px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.02); transition: all 0.3s;
}
.filter-card:hover { transform: translateY(-4px); box-shadow: 0 10px 20px rgba(189,135,101,0.08); }
.filter-label-group { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.filter-label { font-size: 13px; color: #a68d80; font-weight: 700; }
.filter-icon { width: 18px; height: 18px; opacity: 0.7; }
.modern-select {
  width: 100%; border: 1px solid #f2e9e1; border-radius: 12px; padding: 10px;
  font-size: 15px; color: #5c4033; background-color: #faf8f6; cursor: pointer; outline: none;
}
.modern-select:focus { border-color: #d98a67; background-color: #fff; }

/* --- 表格样式 --- */
.table-container {
  background: #fff; border-radius: 24px; padding: 24px; border: 1px solid #f2e9e1;
  box-shadow: 0 4px 20px rgba(92, 64, 51, 0.05);
}
.modern-table { width: 100%; border-collapse: collapse; }
.modern-table th {
  text-align: left; padding: 16px; color: #a68d80; border-bottom: 2px solid #fcf9f6;
  font-size: 14px; font-weight: 600;
}
.modern-table td { padding: 18px 16px; border-bottom: 1px solid #faf8f6; font-size: 14px; color: #5c4033; }

.id-cell { font-family: monospace; color: #a68d80; font-weight: bold; }
.user-info-cell .user-meta { display: flex; flex-direction: column; }
.user-username { font-weight: 700; color: #5c4033; }
.user-nickname { font-size: 12px; color: #a68d80; margin-top: 2px; }

/* 身份标签 */
.role-tag { padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 600; color: white; }
.role-elderly { background: #d98a67; }
.role-employer { background: #8c6a5a; }
.role-child { background: #6b8e23; }
.role-admin { background: #5c4033; }

/* 状态药丸 */
.status-pill { padding: 4px 12px; border-radius: 10px; font-size: 12px; font-weight: 600; margin-right: 8px; }
.health-pending { background: #fff7ed; color: #c2410c; border: 1px solid #ffedd5; }
.health-approved { background: #f0fdf4; color: #15803d; border: 1px solid #dcfce7; }
.health-rejected { background: #fef2f2; color: #b91c1c; border: 1px solid #fee2e2; }
.health-none { background: #f5f5f5; color: #999; }

.report-link { font-size: 12px; color: #d98a67; text-decoration: none; font-weight: 600; }
.report-link:hover { text-decoration: underline; }

/* 状态小圆点 */
.status-dot-wrapper { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 13px; }
.status-dot-wrapper .dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot-wrapper.active { color: #28a745; }
.status-dot-wrapper.active .dot { background: #28a745; box-shadow: 0 0 8px rgba(40, 167, 69, 0.4); }
.status-dot-wrapper.disabled { color: #dc3545; }
.status-dot-wrapper.disabled .dot { background: #dc3545; }

/* 按钮组 */
.action-group { display: flex; gap: 8px; justify-content: center; }
.btn-action {
  padding: 8px 14px; border-radius: 10px; border: none; cursor: pointer; font-weight: 600;
  transition: all 0.2s; font-size: 12px;
}
.btn-action-review { background: #d98a67; color: white; }
.btn-action-success { background: #f0fdf4; color: #15803d; border: 1px solid #dcfce7; }
.btn-action-danger { background: #fef2f2; color: #b91c1c; border: 1px solid #fee2e2; }
.btn-action:hover:not(:disabled) { transform: scale(1.05); }

/* --- 弹窗样式重构 --- */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(60, 42, 33, 0.4); backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modern-modal {
  position: relative; background: #ffffff; border-radius: 30px;
  width: 480px; box-shadow: 0 25px 50px rgba(92, 64, 51, 0.2); overflow: hidden;
}
.modal-decoration { height: 8px; background: linear-gradient(90deg, #d98a67, #f2e9e1, #d98a67); }
.modal-header { padding: 30px; display: flex; justify-content: space-between; }
.header-main { display: flex; gap: 16px; align-items: center; }
.icon-circle {
  width: 44px; height: 44px; background: #fff7ed; display: flex; align-items: center;
  justify-content: center; border-radius: 14px; font-size: 20px;
}
.modal-body { padding: 0 30px 20px; }
.info-bubble { background: #faf8f6; padding: 15px; border-radius: 16px; margin-bottom: 20px; font-size: 14px; color: #8c6a5a; }
.condition-grid { display: flex; gap: 12px; }
.condition-card {
  flex: 1; padding: 15px; border: 2px solid #f2e9e1; border-radius: 16px; background: #fff;
  cursor: pointer; transition: all 0.3s; text-align: center; font-weight: 700; color: #5c4033; position: relative;
}
.condition-card.active { border-color: #d98a67; background: #fffcf9; color: #d98a67; }
.check-mark { position: absolute; top: -5px; right: -5px; background: #d98a67; color: white; width: 20px; height: 20px; border-radius: 50%; font-size: 12px; line-height: 20px; }

.modal-footer-v2 { padding: 20px 30px 30px; display: flex; justify-content: space-between; align-items: center; background: #fcf9f6; }
.footer-actions { display: flex; gap: 10px; }
.btn-minimal { background: transparent; border: none; color: #a68d80; font-weight: 700; cursor: pointer; }
.btn-danger-outline { background: #fff; border: 1px solid #fee2e2; color: #b91c1c; padding: 10px 20px; border-radius: 12px; cursor: pointer; font-weight: 600; }
.btn-gradient {
  background: linear-gradient(135deg, #d98a67 0%, #bf7a5a 100%); color: white; border: none;
  padding: 12px 24px; border-radius: 12px; font-weight: 700; cursor: pointer;
  box-shadow: 0 6px 15px rgba(217, 138, 103, 0.3);
}

/* 确认弹窗微调 */
.confirm-modal { padding: 40px; text-align: center; width: 400px; border-radius: 30px; background: #fff; }
.warning-icon { font-size: 48px; margin-bottom: 16px; }
.confirm-title { font-size: 20px; color: #5c4033; margin-bottom: 10px; }
.confirm-desc { color: #a68d80; line-height: 1.6; }
.highlight { color: #d98a67; font-weight: 800; }
.button-row-v2 { display: flex; justify-content: center; gap: 20px; margin-top: 30px; }
.btn-gradient-confirm {
  background: #5c4033; color: white; border: none; padding: 12px 30px; border-radius: 12px; font-weight: 700; cursor: pointer;
}

/* --- 其他 --- */
.alert { padding: 12px 20px; border-radius: 12px; margin-bottom: 20px; font-size: 14px; }
.alert-success { background: #f0fdf4; color: #15803d; border: 1px solid #dcfce7; }
.alert-error { background: #fef2f2; color: #b91c1c; border: 1px solid #fee2e2; }
.empty-placeholder { padding: 60px; text-align: center; color: #a68d80; }
.empty-icon { font-size: 40px; margin-bottom: 10px; }

/* 动画 */
.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.3s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.9); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.5s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
