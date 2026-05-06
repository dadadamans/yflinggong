没问题，这是为您重构的订单管理页面。

订单管理通常涉及金钱和状态流转，因此我在 UI 上特别强化了金额的展示和状态的视觉区分。延续了前几个页面的米褐色调和毛玻璃质感，让管理后台看起来像是一个精致的现代应用。

优化后的订单管理页面代码
代码段

<template>
  <section class="panel">
    <div class="topbar-modern">
      <div class="title-group">
        <h3 class="section-title">订单调度中心</h3>
        <p class="section-subtitle">监控全平台服务订单流向，处理异常及结算单据</p>
      </div>
      <button class="btn-refresh" type="button" @click="loadData">
        <span class="refresh-icon">↻</span> 刷新列表
      </button>
    </div>

    <div class="filter-tabs-container">
      <div 
        class="filter-tab-v2" 
        :class="{ active: filterStatus === '' }"
        @click="filterStatus = ''"
      >
        <div class="tab-main">
          <span class="tab-label">全部订单</span>
          <span class="tab-count">{{ totalCount }}</span>
        </div>
      </div>

      <div 
        class="filter-tab-v2" 
        :class="{ active: filterStatus === 'waiting' }"
        @click="filterStatus = 'waiting'"
      >
        <div class="tab-icon-box waiting"><img src="../../assets/icons/waiting.svg" /></div>
        <div class="tab-main">
          <span class="tab-label">待接单</span>
          <span class="tab-count highlight">{{ waitingCount }}</span>
        </div>
      </div>

      <div 
        class="filter-tab-v2" 
        :class="{ active: filterStatus === 'working' }"
        @click="filterStatus = 'working'"
      >
        <div class="tab-icon-box working"><img src="../../assets/icons/working.svg" /></div>
        <div class="tab-main">
          <span class="tab-label">进行中</span>
          <span class="tab-count">{{ workingCount }}</span>
        </div>
      </div>

      <div 
        class="filter-tab-v2" 
        :class="{ active: filterStatus === 'pending_payment' }"
        @click="filterStatus = 'pending_payment'"
      >
        <div class="tab-icon-box pending"><img src="../../assets/icons/pending.svg" /></div>
        <div class="tab-main">
          <span class="tab-label">待支付</span>
          <span class="tab-count highlight-warn">{{ pendingPaymentCount }}</span>
        </div>
      </div>

      <div 
        class="filter-tab-v2" 
        :class="{ active: filterStatus === 'done' }"
        @click="filterStatus = 'done'"
      >
        <div class="tab-icon-box done"><img src="../../assets/icons/done.svg" /></div>
        <div class="tab-main">
          <span class="tab-label">已完成</span>
          <span class="tab-count">{{ doneCount }}</span>
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
      <table v-if="orders.length" class="modern-table">
        <thead>
          <tr>
            <th>订单编号</th>
            <th>服务项目</th>
            <th>服务人员 (老人)</th>
            <th>预约时间</th>
            <th>服务报酬</th>
            <th>当前状态</th>
            <th class="text-center">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in orders" :key="item.id">
            <td class="id-cell">#{{ item.code || item.id }}</td>
            <td class="title-cell">
              <span class="order-title">{{ item.title }}</span>
            </td>
            <td>
              <div class="user-chip">
                <span class="chip-avatar">👴</span>
                <span>{{ item.elderly_name || item.elderlyName || "-" }}</span>
              </div>
            </td>
            <td class="time-cell">{{ item.time_text || item.timeText || "-" }}</td>
            <td>
              <span class="salary-text">¥{{ item.salary }}</span>
            </td>
            <td><StatusTag :status="item.status" /></td>
            <td class="text-center">
              <button class="btn-action-delete" @click="deleteOrder(item)">
                <span class="icon">🗑️</span> 删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <div v-else class="empty-placeholder">
        <div class="empty-icon">📄</div>
        <p>未发现相关订单记录</p>
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
      <div v-if="showConfirmDialog" class="modal-overlay" @click="showConfirmDialog = false">
        <div class="modal-card delete-confirm-modal" @click.stop>
          <div class="modal-body text-center">
            <div class="warning-animation">⚠️</div>
            <h3 class="modal-title">高危操作提示</h3>
            <div class="warning-box">
              <p>确定要彻底删除订单 <strong>#{{ confirmItemId }}</strong> 吗？</p>
              <p class="sub-tip">此操作将同步从用户记录中抹除，且不可撤销。</p>
            </div>
            <div class="button-row-v2">
              <button class="btn-minimal" @click="showConfirmDialog = false">取消</button>
              <button class="btn-danger-gradient" @click="confirmDeleteOrder">确认删除</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </section>
</template>
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { getAdminStats } from "../../api/admin";
import { getOrderList, deleteOrder as deleteOrderApi } from "../../api/order";
import AppPagination from "../../components/AppPagination.vue";
import StatusTag from "../../components/StatusTag.vue";

const orders = ref([]);
const total = ref(0);
const errorText = ref("");
const successText = ref("");

const filterStatus = ref("");
const currentPage = ref(1);
const pageSize = ref(10);
const totalCount = ref(0);
const waitingCount = ref(0);
const workingCount = ref(0);
const pendingPaymentCount = ref(0);
const doneCount = ref(0);

const showConfirmDialog = ref(false);
const confirmItemId = ref(null);

const loading = ref(false);

async function loadData() {
  try {
    const [orderRes, statsRes] = await Promise.all([
      getOrderList({
        page: currentPage.value,
        pageSize: pageSize.value,
        status: filterStatus.value || undefined
      }),
      getAdminStats()
    ]);
    orders.value = orderRes.data?.list || [];
    total.value = orderRes.data?.total || 0;
    const stats = statsRes.data || {};
    waitingCount.value = stats.waitingCount || 0;
    workingCount.value = stats.workingCount || 0;
    pendingPaymentCount.value = stats.pendingPaymentCount || 0;
    doneCount.value = stats.doneCount || 0;
    totalCount.value = stats.taskCount || 0;
  } catch (error) {
    errorText.value = error.message || "订单数据加载失败";
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

function deleteOrder(item) {
  confirmItemId.value = item.taskId || item.task_id || item.id;
  showConfirmDialog.value = true;
}

async function confirmDeleteOrder() {
  try {
    const taskId = confirmItemId.value;
    const res = await deleteOrderApi(taskId);
    successText.value = res.message;
    showConfirmDialog.value = false;
    await loadData();
  } catch (error) {
    errorText.value = error.message || "删除失败";
  }
}

onMounted(loadData);

watch(filterStatus, () => {
  currentPage.value = 1;
  loadData();
});
</script>

<style scoped>
/* --- 布局与顶部 --- */
.panel { padding: 24px; background-color: #fcf9f6; min-height: 100vh; }
.topbar-modern { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.section-title { font-size: 22px; color: #5c4033; margin: 0; font-weight: 700; }
.section-subtitle { font-size: 13px; color: #a68d80; margin-top: 4px; }

.btn-refresh {
  background: #fff; border: 1px solid #f2e9e1; padding: 10px 18px; border-radius: 12px;
  color: #8c6a5a; cursor: pointer; transition: all 0.3s; display: flex; align-items: center; gap: 8px; font-weight: 600;
}

/* --- 状态切换卡片 --- */
.filter-tabs-container { display: flex; gap: 12px; margin-bottom: 24px; overflow-x: auto; padding-bottom: 8px; }
.filter-tab-v2 {
  flex: 1; min-width: 140px; background: #fff; border: 2px solid transparent; border-radius: 20px;
  padding: 16px; display: flex; align-items: center; gap: 12px; cursor: pointer; transition: all 0.3s;
  box-shadow: 0 4px 10px rgba(0,0,0,0.02);
}
.filter-tab-v2:hover { transform: translateY(-4px); box-shadow: 0 10px 20px rgba(189,135,101,0.08); }
.filter-tab-v2.active { border-color: #d98a67; background: #fffcf9; }

.tab-icon-box { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; }
.tab-icon-box img { width: 20px; height: 20px; }
.tab-icon-box.waiting { background: #fff7ed; }
.tab-icon-box.working { background: #f0fdf4; }
.tab-icon-box.pending { background: #fef2f2; }
.tab-icon-box.done { background: #eff6ff; }

.tab-main { display: flex; flex-direction: column; }
.tab-label { font-size: 12px; color: #a68d80; font-weight: 600; }
.tab-count { font-size: 20px; font-weight: 800; color: #5c4033; }
.tab-count.highlight { color: #d98a67; }
.tab-count.highlight-warn { color: #b91c1c; }

/* --- 表格样式 --- */
.table-container {
  background: #fff; border-radius: 24px; padding: 24px; border: 1px solid #f2e9e1;
  box-shadow: 0 4px 20px rgba(92, 64, 51, 0.05);
}
.modern-table { width: 100%; border-collapse: collapse; }
.modern-table th { text-align: left; padding: 16px; color: #a68d80; border-bottom: 2px solid #fcf9f6; font-size: 14px; }
.modern-table td { padding: 18px 16px; border-bottom: 1px solid #faf8f6; font-size: 14px; color: #5c4033; }

.id-cell { font-family: monospace; color: #a68d80; font-weight: bold; }
.order-title { font-weight: 700; color: #5c4033; }
.salary-text { color: #d98a67; font-weight: 800; font-size: 16px; }

.user-chip { display: flex; align-items: center; gap: 8px; background: #fcf9f6; padding: 4px 12px; border-radius: 20px; width: fit-content; }
.chip-avatar { font-size: 14px; }

/* --- 按钮与操作 --- */
.btn-action-delete {
  background: #fef2f2; color: #b91c1c; border: 1px solid #fee2e2; padding: 8px 16px;
  border-radius: 10px; cursor: pointer; transition: all 0.2s; font-weight: 600; font-size: 13px;
}
.btn-action-delete:hover { background: #b91c1c; color: #fff; }

/* --- 弹窗样式 --- */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(60, 42, 33, 0.4); backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.delete-confirm-modal { background: #fff; padding: 40px; border-radius: 30px; width: 400px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); }
.warning-animation { font-size: 50px; margin-bottom: 20px; animation: pulse 2s infinite; }
@keyframes pulse { 0% { transform: scale(1); } 50% { transform: scale(1.1); } 100% { transform: scale(1); } }

.modal-title { font-size: 20px; color: #5c4033; margin-bottom: 16px; }
.warning-box { background: #fcf9f6; padding: 16px; border-radius: 16px; margin-bottom: 24px; }
.sub-tip { font-size: 12px; color: #a68d80; margin-top: 8px; }

.button-row-v2 { display: flex; gap: 12px; justify-content: center; }
.btn-minimal { background: none; border: none; color: #a68d80; font-weight: 600; cursor: pointer; padding: 0 20px; }
.btn-danger-gradient {
  background: linear-gradient(135deg, #ef4444 0%, #b91c1c 100%); color: #fff; border: none;
  padding: 12px 28px; border-radius: 12px; font-weight: 700; cursor: pointer; box-shadow: 0 4px 12px rgba(185, 28, 28, 0.2);
}

/* --- 其他 --- */
.alert { padding: 12px 20px; border-radius: 12px; margin-bottom: 20px; }
.alert-success { background: #f0fdf4; color: #15803d; border: 1px solid #dcfce7; }
.alert-error { background: #fef2f2; color: #b91c1c; border: 1px solid #fee2e2; }

.empty-placeholder { padding: 60px; text-align: center; color: #a68d80; }
.empty-icon { font-size: 40px; margin-bottom: 10px; }

.pagination-wrapper { margin-top: 24px; }
</style>