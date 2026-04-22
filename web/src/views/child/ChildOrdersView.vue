<template>
  <section class="panel">
    <div class="topbar" style="margin-bottom: 12px">
      <div>
        <h3 class="section-title">老人订单查看</h3>
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
    <div v-if="!orders.length" class="empty">暂无订单数据</div>
    <div v-else class="card-list">
      <article v-for="item in orders" :key="item.id" class="order-card">
        <div class="topbar" style="margin-bottom: 0">
          <div>
            <h4 class="section-title">{{ item.title }}</h4>
            <div class="meta">
              <span>订单号：{{ item.code || item.id }}</span>
              <span>地址：{{ item.address }}</span>
              <span>时间：{{ item.timeText || '-' }}</span>
              <span>接单人：{{ item.elderly_name || item.elderlyName || '-' }}</span>
              <span>报酬：¥{{ item.salary }}</span>
              <span>结算：{{ item.settleText || item.settle_text || '-' }}</span>
            </div>
          </div>
          <StatusTag :status="item.status" />
        </div>
        <div class="button-row">
          <button v-if="item.status === 'pending_payment'" class="btn" type="button" :disabled="actionLoading" @click="showPayDialog(item)">
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

    </section>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { getBindOrderList } from "../../api/bind";
import { payOrder as payOrderApi } from "../../api/order";
import AppPagination from "../../components/AppPagination.vue";
import StatusTag from "../../components/StatusTag.vue";

const loading = ref(false);
const actionLoading = ref(false);
const errorText = ref("");
const successText = ref("");
const orders = ref([]);
const total = ref(0);
const filterStatus = ref("waiting");
const currentPage = ref(1);
const pageSize = ref(10);

const showDialog = ref(false);
const payItem = ref(null);
const paying = ref(false);

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
  try {
    const res = await getBindOrderList({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value || undefined
    });
    orders.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch (error) {
    errorText.value = error.message || "订单加载失败";
  } finally {
    loading.value = false;
  }
}

function showPayDialog(item) {
  payItem.value = item;
  showDialog.value = true;
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
    await loadData();
  } catch (error) {
    errorText.value = error.message || "支付失败";
  } finally {
    paying.value = false;
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
  background: radial-gradient(circle at top left, rgba(184, 92, 56, 0.12), transparent 26%),
              radial-gradient(circle at top right, rgba(47, 125, 77, 0.1), transparent 22%),
              linear-gradient(180deg, #f6f1e7, #efe6d8 48%, #f8f5ef);
  padding: 20px;
  border-radius: 8px;
  width: 80%;
  max-width: 320px;
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
</style>
