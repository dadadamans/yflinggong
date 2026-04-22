<template>
  <div class="split">
    <section class="panel">
      <h3 class="section-title">老人资料</h3>
      <p v-if="errorText" class="error-text">{{ errorText }}</p>
      <div v-if="loading" class="empty">正在加载老人资料...</div>
      <div v-else-if="!data?.elderlyProfile" class="empty">暂无老人资料，请先完成绑定</div>
      <div v-else class="card-list">
        <article class="task-card">
          <div class="meta">
            <span>姓名：{{ data.elderlyProfile.realName || data.elderlyProfile.nickname }}</span>
            <span>年龄：{{ data.elderlyProfile.age }}</span>
            <span>性别：{{ data.elderlyProfile.gender }}</span>
          </div>
          <p>健康情况：{{ data.elderlyProfile.healthDesc }}</p>
          <p>技能标签：{{ data.elderlyProfile.skillText || data.elderlyProfile.skillTags }}</p>
          <p>紧急联系人：{{ data.elderlyProfile.emergencyContact }} / {{ data.elderlyProfile.emergencyMobile }}</p>
          
          <div class="font-setting">
            <label>字体大小：</label>
            <select v-model="currentFontSize" @change="handleFontSizeChange">
              <option value="small">小</option>
              <option value="medium">中</option>
              <option value="large">大</option>
              <option value="xlarge">特大</option>
            </select>
          </div>
        </article>
      </div>
    </section>

    <section class="panel">
      <h3 class="section-title">老人当前订单</h3>
      <div v-if="!data?.currentOrder" class="empty">当前没有进行中的订单</div>
      <article v-else class="order-card">
        <div class="topbar" style="margin-bottom: 0">
          <div>
            <h4 class="section-title">{{ data.currentOrder.title }}</h4>
            <div class="meta">
              <span>{{ data.currentOrder.address }}</span>
              <span>{{ data.currentOrder.startTime }}</span>
              <span>¥{{ data.currentOrder.salary }}</span>
            </div>
          </div>
          <StatusTag :status="data.currentOrder.status" />
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { getBindInfo } from "../../api/bind";
import { setElderlyFontSize } from "../../api/user";
import StatusTag from "../../components/StatusTag.vue";

const loading = ref(false);
const errorText = ref("");
const data = ref(null);
const currentFontSize = ref("medium");

const fontSizeOptions = [
  { value: "small", label: "小 (12px)" },
  { value: "medium", label: "中 (14px)" },
  { value: "large", label: "大 (18px)" },
  { value: "xlarge", label: "特大 (22px)" }
];

async function loadData() {
  loading.value = true;
  errorText.value = "";
  try {
    const res = await getBindInfo();
    data.value = res.data || null;
    if (res.data?.elderlyProfile?.fontSize) {
      currentFontSize.value = res.data.elderlyProfile.fontSize;
    }
  } catch (error) {
    errorText.value = error.message || "老人资料加载失败";
  } finally {
    loading.value = false;
  }
}

async function handleFontSizeChange() {
  if (!data.value?.elderlyProfile?.id) return;
  try {
    await setElderlyFontSize(data.value.elderlyProfile.id, currentFontSize.value);
  } catch (error) {
    alert("字体大小设置失败: " + (error.message || "请重试"));
    currentFontSize.value = data.value.elderlyProfile.fontSize || "medium";
  }
}

onMounted(loadData);
</script>

<style scoped>
.font-setting {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
}
.font-setting label {
  font-weight: 500;
  color: #333;
}
.font-setting select {
  padding: 6px 12px;
  font-size: 14px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-left: 8px;
  background: #fff;
  cursor: pointer;
}
.font-setting select:hover {
  border-color: #1890ff;
}
</style>
