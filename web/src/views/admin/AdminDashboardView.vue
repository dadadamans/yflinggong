<template>
  <div class="dashboard">
    <section class="panel chart-section">
      <h3 class="section-title">数据分布</h3>
      <div class="charts-row">
        <div class="chart-box">
          <h4>订单状态分布</h4>
          <div ref="orderStatusChart" class="echart-container"></div>
        </div>
        
        <div class="chart-box">
          <h4>任务类型分布</h4>
          <div ref="taskTypeChart" class="echart-container"></div>
        </div>
      </div>
    </section>
    
    <section class="panel chart-section" style="margin-top: 20px;">
      <h3 class="section-title">趋势分析</h3>
      <div class="charts-row">
        <div class="chart-box">
          <h4>用户注册趋势（过去7天）</h4>
          <div ref="userTrendChart" class="echart-container"></div>
        </div>
        
        <div class="chart-box">
          <h4>任务发布趋势（过去7天）</h4>
          <div ref="taskTrendChart" class="echart-container"></div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from "vue";
import * as echarts from "echarts";
import { getAdminStats } from "../../api/admin";

const stats = ref({});
const loading = ref(false);

const orderStatusChart = ref(null);
const taskTypeChart = ref(null);
const userTrendChart = ref(null);
const taskTrendChart = ref(null);

let orderChartInstance = null;
let taskTypeChartInstance = null;
let userTrendChartInstance = null;
let taskTrendChartInstance = null;

const orderStatusDist = computed(() => {
  return stats.value.orderStatusDist || [];
});

const taskTypeDist = computed(() => {
  return stats.value.taskTypeDist || [];
});

function statusText(status) {
  const map = {
    waiting: "待接单",
    applying: "待审核",
    working: "进行中",
    pending_payment: "待支付",
    done: "已完成",
    cancelled: "已取消"
  };
  return map[status] || status;
}

function typeText(type) {
  if (!type) return "未分类";
  if (type.startsWith("skill:")) return "技能型";
  if (type.startsWith("experience:")) return "体验型";
  if (type.startsWith("mutual:")) return "互助型";
  return type;
}

function initOrderStatusChart(dist) {
  if (!orderStatusChart.value) return;
  
  if (orderChartInstance) {
    orderChartInstance.dispose();
  }
  
  const data = dist.map(item => ({
    name: statusText(item.status),
    value: item.count
  }));
  
  orderChartInstance = echarts.init(orderStatusChart.value);
  orderChartInstance.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
    legend: { bottom: "0%", left: "center" },
    series: [{
      type: "pie",
      radius: ["40%", "70%"],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: "#fff",
        borderWidth: 2
      },
      label: { show: true, formatter: "{b}" },
      data: data.length ? data : [{ name: "无数据", value: 0 }]
    }]
  });
}

function initTaskTypeChart(dist) {
  if (!taskTypeChart.value) return;
  
  if (taskTypeChartInstance) {
    taskTypeChartInstance.dispose();
  }
  
  const data = dist.map(item => ({
    name: typeText(item.type),
    value: item.count
  }));
  
  taskTypeChartInstance = echarts.init(taskTypeChart.value);
  taskTypeChartInstance.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
    legend: { bottom: "0%", left: "center" },
    series: [{
      type: "pie",
      radius: ["40%", "70%"],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: "#fff",
        borderWidth: 2
      },
      label: { show: true, formatter: "{b}" },
      data: data.length ? data : [{ name: "无数据", value: 0 }]
    }]
  });
}

function initUserTrendChart(trend) {
  if (!userTrendChart.value) return;
  
  if (userTrendChartInstance) {
    userTrendChartInstance.dispose();
  }
  
  const dates = trend.map(item => item.date);
  const counts = trend.map(item => item.count);
  
  userTrendChartInstance = echarts.init(userTrendChart.value);
  userTrendChartInstance.setOption({
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: dates },
    yAxis: { type: "value" },
    series: [{
      type: "line",
      data: counts,
      smooth: true,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: "#5470c6" }
    }]
  });
}

function initTaskTrendChart(trend) {
  if (!taskTrendChart.value) return;
  
  if (taskTrendChartInstance) {
    taskTrendChartInstance.dispose();
  }
  
  const dates = trend.map(item => item.date);
  const counts = trend.map(item => item.count);
  
  taskTrendChartInstance = echarts.init(taskTrendChart.value);
  taskTrendChartInstance.setOption({
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: dates },
    yAxis: { type: "value" },
    series: [{
      type: "line",
      data: counts,
      smooth: true,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: "#91cc75" }
    }]
  });
}

async function loadData() {
  loading.value = true;
  try {
    const res = await getAdminStats();
    stats.value = res.data || {};
    
    setTimeout(() => {
      initOrderStatusChart(orderStatusDist.value);
      initTaskTypeChart(taskTypeDist.value);
      initUserTrendChart(stats.value.userTrend || []);
      initTaskTrendChart(stats.value.taskTrend || []);
    }, 100);
  } catch (error) {
    console.error("加载统计数据失败", error);
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.stat-icon {
  font-size: 36px;
  margin-right: 16px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.chart-section {
  padding: 20px;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-top: 16px;
}

.chart-box {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.chart-box h4 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 16px;
  text-align: center;
}

.echart-container {
  width: 100%;
  height: 300px;
}

@media (max-width: 1200px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>