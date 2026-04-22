<template>
  <section class="panel">
    <h3 class="section-title">订单管理</h3>
    
    <div class="filter-tabs">
      <button 
        class="filter-tab" 
        :class="{ active: filterStatus === '' }"
        @click="filterStatus = ''"
      >
        <img src="../../assets/icons/状态.svg" class="tab-icon" />
        <span>全部</span>
        <span class="tab-count">{{ totalCount }}</span>
      </button>
      <button 
        class="filter-tab" 
        :class="{ active: filterStatus === 'waiting' }"
        @click="filterStatus = 'waiting'"
      >
        <img src="../../assets/icons/waiting.svg" class="tab-icon" />
        <span>待接单</span>
        <span class="tab-count">{{ waitingCount }}</span>
      </button>
      <button 
        class="filter-tab" 
        :class="{ active: filterStatus === 'working' }"
        @click="filterStatus = 'working'"
      >
        <img src="../../assets/icons/working.svg" class="tab-icon" />
        <span>进行中</span>
        <span class="tab-count">{{ workingCount }}</span>
      </button>
      <button 
        class="filter-tab" 
        :class="{ active: filterStatus === 'pending_payment' }"
        @click="filterStatus = 'pending_payment'"
      >
        <img src="../../assets/icons/pending.svg" class="tab-icon" />
        <span>待支付</span>
        <span class="tab-count">{{ pendingPaymentCount }}</span>
      </button>
      <button 
        class="filter-tab" 
        :class="{ active: filterStatus === 'done' }"
        @click="filterStatus = 'done'"
      >
        <img src="../../assets/icons/done.svg" class="tab-icon" />
        <span>已完成</span>
        <span class="tab-count">{{ doneCount }}</span>
      </button>
    </div>

    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText">{{ successText }}</p>
    <table v-if="orders.length" class="table">
      <thead>
        <tr>
          <th>订单号</th>
          <th>标题</th>
          <th>接单人</th>
          <th>时间</th>
          <th>报酬</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in orders" :key="item.id">
          <td>{{ item.code || item.id }}</td>
          <td>{{ item.title }}</td>
          <td>{{ item.elderly_name || item.elderlyName || "-" }}</td>
          <td>{{ item.time_text || item.timeText || "-" }}</td>
          <td>¥{{ item.salary }}</td>
          <td><StatusTag :status="item.status" /></td>
          <td>
            <button class="action-btn" @click="deleteOrder(item)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty">暂无订单数据</div>
    <AppPagination
      v-if="total > 0"
      :page="currentPage"
      :page-size="pageSize"
      :total="total"
      @update:page="handlePageChange"
      @update:pageSize="handlePageSizeChange"
    />
  </section>

  <div v-if="showConfirmDialog" class="modal-mask" @click="showConfirmDialog = false">
    <div class="modal" @click.stop>
      <div class="modal-header">
        <h3 class="section-title">确认删除</h3>
        <button class="modal-close" @click="showConfirmDialog = false">×</button>
      </div>
      <div class="pay-info">
        <p>确定要彻底删除该订单吗？删除后数据将无法恢复！</p>
      </div>
      <div class="button-row">
        <button class="btn btn-success" style="flex: 1" @click="confirmDeleteOrder">确定删除</button>
        <button class="btn btn-danger" style="flex: 1" @click="showConfirmDialog = false">取消</button>
      </div>
    </div>
  </div>
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
.action-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  background: #dc3545;
  color: #fff;
}
.action-btn:hover {
  opacity: 0.85;
}
.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.filter-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: #fff;
  cursor: pointer;
  font-size: 16px;
  min-height: 44px;
}
.filter-tab.active {
  background: #4a90d9;
  color: #fff;
  border-color: #4a90d9;
}
.tab-icon {
  width: 20px;
  height: 20px;
}
.tab-count {
  background: rgba(0,0,0,0.1);
  padding: 3px 8px;
  border-radius: 10px;
  font-size: 14px;
}
.filter-tab.active .tab-count {
  background: rgba(255,255,255,0.2);
}
.success-text {
  color: #28a745;
  margin-bottom: 12px;
}
.empty {
  padding: 40px;
  text-align: center;
  color: #999;
}
.error-text {
  color: #f56c6c;
}
.btn-success {
  background: #4caf50;
  color: #fff;
  text-align: center;
}
.btn-danger {
  background: #dc3545;
  color: #fff;
  text-align: center;
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
.pay-info {
  margin-bottom: 16px;
}
.pay-info p {
  margin: 8px 0;
}
</style>
