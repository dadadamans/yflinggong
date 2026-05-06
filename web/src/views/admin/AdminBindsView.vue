<template>
  <section class="panel">
    <div class="topbar-modern">
      <div class="title-group">
        <h3 class="section-title">绑定关系管理</h3>
        <p class="section-subtitle">监控老人与子女的账号关联状态及绑定码使用情况</p>
      </div>
      <button class="btn-refresh" type="button" :disabled="loading" @click="loadData">
        <span class="refresh-icon">↻</span> 刷新数据
      </button>
    </div>

    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon">🔗</div>
        <div class="stat-info">
          <span class="stat-label">总绑定数</span>
          <span class="stat-value">{{ total }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon highlight">⏳</div>
        <div class="stat-info">
          <span class="stat-label">待确认关联</span>
          <span class="stat-value">{{ binds.filter(i => !i.confirmed).length }}</span>
        </div>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="errorText" class="alert alert-error">{{ errorText }}</div>
    </Transition>

    <div class="table-container">
      <table v-if="binds.length" class="modern-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>唯一绑定码</th>
            <th>家庭成员关系 (老人 ↔ 子女)</th>
            <th>称呼关系</th>
            <th>状态</th>
            <th>建立时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in binds" :key="item.id">
            <td class="id-cell">#{{ item.id }}</td>
            <td>
              <code class="bind-code">{{ item.bind_code }}</code>
            </td>
            <td>
              <div class="connection-display">
                <div class="member elder">
                  <span class="m-icon">👴</span>
                  <span class="m-name">{{ item.elderly_real_name || item.elderly_nickname || '未知' }}</span>
                </div>
                <div class="connector">
                  <div class="line"></div>
                  <div class="arrow">✦</div>
                </div>
                <div class="member child">
                  <span class="m-icon">🧒</span>
                  <span class="m-name">{{ item.child_real_name || item.child_nickname || '未关联' }}</span>
                </div>
              </div>
            </td>
            <td>
              <span class="relation-tag">{{ item.relation || '未设置' }}</span>
            </td>
            <td>
              <span class="status-pill" :class="item.confirmed ? 'status-confirmed' : 'status-waiting'">
                {{ item.confirmed ? '已激活绑定' : '等待确认中' }}
              </span>
            </td>
            <td class="time-cell">{{ formatDate(item.created_at) }}</td>
          </tr>
        </tbody>
      </table>
      
      <div v-else class="empty-placeholder">
        <div class="empty-icon">⛓️</div>
        <p>暂无账号绑定关系记录</p>
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
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { getBindList } from "../../api/bind";
import AppPagination from "../../components/AppPagination.vue";

const binds = ref([]);
const total = ref(0);
const errorText = ref("");
const currentPage = ref(1);
const pageSize = ref(10);

function formatDate(dateStr) {
  if (!dateStr) return "-";
  return dateStr.substring(0, 19).replace("T", " ");
}

async function loadData() {
  try {
    const res = await getBindList({ page: currentPage.value, pageSize: pageSize.value });
    binds.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch (error) {
    errorText.value = error.message || "绑定关系加载失败";
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
/* --- 全局布局 --- */
.panel {
  padding: 24px;
  background-color: #fcf9f6;
  min-height: 100vh;
}

.topbar-modern {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title { font-size: 22px; color: #5c4033; margin: 0; font-weight: 700; }
.section-subtitle { font-size: 13px; color: #a68d80; margin-top: 4px; }

.btn-refresh {
  background: #fff; border: 1px solid #f2e9e1; padding: 10px 18px; border-radius: 12px;
  color: #8c6a5a; cursor: pointer; transition: all 0.3s; display: flex; align-items: center; gap: 8px; font-weight: 600;
}
.btn-refresh:hover { background: #f2e9e1; }

/* --- 统计卡片 --- */
.stat-cards { display: flex; gap: 20px; margin-bottom: 30px; }
.stat-card {
  flex: 1; background: #fff; border-radius: 20px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.02);
}
.stat-icon {
  width: 48px; height: 48px; background: #faf8f6; border-radius: 14px;
  display: flex; align-items: center; justify-content: center; font-size: 20px;
}
.stat-icon.highlight { background: #fff7ed; }
.stat-label { display: block; font-size: 13px; color: #a68d80; font-weight: 600; }
.stat-value { font-size: 24px; font-weight: 800; color: #5c4033; }

/* --- 表格设计 --- */
.table-container {
  background: #fff; border-radius: 24px; padding: 24px; border: 1px solid #f2e9e1;
  box-shadow: 0 4px 20px rgba(92, 64, 51, 0.05);
}

.modern-table { width: 100%; border-collapse: collapse; }
.modern-table th {
  text-align: left; padding: 16px; color: #a68d80; border-bottom: 2px solid #fcf9f6;
  font-size: 14px; font-weight: 600;
}
.modern-table td { padding: 20px 16px; border-bottom: 1px solid #faf8f6; font-size: 14px; color: #5c4033; }

.id-cell { font-family: monospace; color: #a68d80; font-weight: bold; }

/* 绑定码样式 */
.bind-code {
  background: #faf8f6; padding: 4px 8px; border-radius: 6px;
  border: 1px solid #f2e9e1; color: #d98a67; font-weight: 700;
}

/* 核心：连接关系展示 */
.connection-display { display: flex; align-items: center; gap: 12px; }
.member { display: flex; align-items: center; gap: 8px; background: #fcf9f6; padding: 6px 12px; border-radius: 12px; }
.m-name { font-weight: 700; color: #5c4033; }
.m-icon { font-size: 16px; }

.connector { display: flex; align-items: center; gap: 4px; }
.line { width: 30px; height: 2px; background: #f2e9e1; position: relative; }
.arrow { color: #d98a67; font-size: 12px; margin-left: -5px; }

/* 关系标签 */
.relation-tag {
  background: #fff7ed; color: #d98a67; padding: 4px 12px; border-radius: 8px;
  font-size: 12px; font-weight: 700; border: 1px solid #ffedd5;
}

/* 状态药丸 */
.status-pill { padding: 4px 12px; border-radius: 10px; font-size: 12px; font-weight: 600; }
.status-confirmed { background: #f0fdf4; color: #15803d; border: 1px solid #dcfce7; }
.status-waiting { background: #fff7ed; color: #c2410c; border: 1px solid #ffedd5; }

.time-cell { color: #a68d80; font-size: 13px; }

/* --- 提示与空状态 --- */
.alert { padding: 12px 20px; border-radius: 12px; margin-bottom: 20px; font-size: 14px; }
.alert-error { background: #fef2f2; color: #b91c1c; border: 1px solid #fee2e2; }

.empty-placeholder { padding: 60px; text-align: center; color: #a68d80; }
.empty-icon { font-size: 40px; margin-bottom: 10px; }

.pagination-wrapper { margin-top: 24px; }

/* 动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.5s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
