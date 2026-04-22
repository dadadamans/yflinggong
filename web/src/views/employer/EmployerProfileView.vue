<template>
  <section class="panel">
    <h3 class="section-title">雇主资料</h3>
    <div class="rating-summary">
      <span v-if="ratingStats.avgRating" class="rating-score">{{ ratingStats.avgRating.toFixed(1) }}</span>
      <span v-else class="rating-score">暂无评分</span>
      <span class="rating-stars">★</span>
      <span class="rating-count"> ({{ ratingStats.commentCount || 0 }} 条评价)</span>
    </div>
    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText">{{ successText }}</p>
    <div v-if="loading" class="empty">正在加载资料...</div>
    <div v-else class="form-grid">
      <div class="field"><label>昵称</label><input v-model="form.nickname" /></div>
      <div class="field"><label>姓名</label><input v-model="form.name" /></div>
      <div class="field"><label>手机号</label><input v-model="form.mobile" /></div>
      <div class="field field-full"><label>备注</label><textarea v-model="form.remark" /></div>
    </div>
    <div class="button-row" style="margin-top: 18px">
      <button class="btn" type="button" :disabled="saving" @click="save">{{ saving ? "保存中..." : "保存资料" }}</button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { getUserInfo, saveUserInfo } from "../../api/user";

const loading = ref(false);
const saving = ref(false);
const errorText = ref("");
const successText = ref("");

const form = reactive({
  nickname: "",
  name: "",
  mobile: "",
  remark: ""
});

const ratingStats = reactive({
  avgRating: 0,
  commentCount: 0
});

async function loadData() {
  loading.value = true;
  errorText.value = "";
  try {
    const res = await getUserInfo();
    Object.assign(form, res.data?.profile || res.data || {});
    const profile = res.data?.profile || res.data || {};
    ratingStats.avgRating = profile.avgRating || 0;
    ratingStats.commentCount = profile.commentCount || 0;
  } catch (error) {
    errorText.value = error.message || "资料加载失败";
  } finally {
    loading.value = false;
  }
}

async function save() {
  saving.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const res = await saveUserInfo({ ...form });
    successText.value = res.message || "资料已保存";
  } catch (error) {
    errorText.value = error.message || "保存失败";
  } finally {
    saving.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
.rating-summary {
  margin-bottom: 16px;
  padding: 12px;
  background: #fff8e1;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.rating-score {
  font-size: 24px;
  font-weight: 700;
  color: #f57c00;
}
.rating-stars {
  font-size: 20px;
  color: #f5b700;
}
.rating-count {
  color: #666;
  font-size: 14px;
}
</style>
