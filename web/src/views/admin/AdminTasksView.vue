<template>
  <section class="panel">
    <h3 class="section-title">任务管理</h3>
    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText">{{ successText }}</p>
    
    <table v-if="tasks.length" class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>标题</th>
          <th>地址</th>
          <th>时间</th>
          <th>报酬</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in tasks" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.title }}</td>
          <td>{{ item.address }}</td>
          <td>{{ getStartTime(item) }}-{{ getEndTime(item) }}</td>
          <td>¥{{ item.salary }}</td>
          <td><StatusTag :status="item.status" /></td>
          <td>
            <button class="action-btn" @click="deleteTask(item)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty">暂无待接单任务</div>
  </section>

  <div v-if="showConfirmDialog" class="modal-mask" @click="showConfirmDialog = false">
    <div class="modal" @click.stop>
      <div class="modal-header">
        <h3 class="section-title">确认删除</h3>
        <button class="modal-close" @click="showConfirmDialog = false">×</button>
      </div>
      <div class="pay-info">
        <p>确定要彻底删除该任务吗？删除后数据将无法恢复！</p>
      </div>
      <div class="button-row">
        <button class="btn btn-success" style="flex: 1" @click="confirmDeleteTask">确定删除</button>
        <button class="btn btn-danger" style="flex: 1" @click="showConfirmDialog = false">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { getAdminWaitingTasks } from "../../api/task";
import { deleteOrder as deleteOrderApi } from "../../api/order";
import StatusTag from "../../components/StatusTag.vue";

const tasks = ref([]);
const errorText = ref("");
const successText = ref("");

const showConfirmDialog = ref(false);
const confirmItemId = ref(null);

function getStartTime(item) {
  const timeText = item.timeText || item.time_text || "";
  if (!timeText) return "-";
  const parts = timeText.split(" ");
  return parts.length > 1 ? parts[1].split("-")[0] : "-";
}

function getEndTime(item) {
  const timeText = item.timeText || item.time_text || "";
  if (!timeText) return "-";
  const parts = timeText.split(" ");
  return parts.length > 1 ? parts[1].split("-")[1] : "-";
}

async function loadData() {
  try {
    const res = await getAdminWaitingTasks();
    tasks.value = res.data || [];
  } catch (error) {
    errorText.value = error.message || "数据加载失败";
  }
}

function deleteTask(item) {
  confirmItemId.value = item.id;
  showConfirmDialog.value = true;
}

async function confirmDeleteTask() {
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