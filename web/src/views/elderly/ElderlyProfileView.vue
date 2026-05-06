<template>
  <section class="panel">
    <h3 class="section-title">个人资料</h3>
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
      <div class="field"><label>真实姓名</label><input v-model="form.realName" /></div>
      <div class="field"><label>手机号</label><input v-model="form.mobile" /></div>
      <div class="field"><label>年龄</label><input v-model="form.age" /></div>
      <div class="field"><label>性别</label><input v-model="form.gender" /></div>
      <div class="field field-full">
        <label>技能标签</label>
        <div class="tag-selector">
          <div 
            v-for="tag in availableTags" 
            :key="tag" 
            class="tag-option" 
            :class="{ selected: form.skillTags.includes(tag) }"
            @click="toggleTag(tag)"
          >
            {{ tag }}
          </div>
        </div>
      </div>
      <div class="field field-full"><label>健康描述</label><textarea v-model="form.healthDesc" /></div>
      <div class="field"><label>紧急联系人</label><input v-model="form.emergencyContact" /></div>
      <div class="field"><label>紧急联系电话</label><input v-model="form.emergencyMobile" /></div>
      
      <div class="field field-full">
        <label>字体大小</label>
        <select v-model="form.fontSize">
          <option value="small">小 (12px)</option>
          <option value="medium">中 (14px)</option>
          <option value="large">大 (18px)</option>
          <option value="xlarge">特大 (22px)</option>
        </select>
      </div>
      
      <div class="field field-full">
        <label>体检报告</label>
        <div class="health-report">
          <div v-if="healthReportStatus === 'pending' && healthReportUrl" class="status-pending">
            <span>已上传体检报告，等待审核中（可重新上传）</span>
          </div>
          <div v-else-if="healthReportStatus === 'approved'" class="status-approved">
            <span>✅ 已通过审核</span>
          </div>
          <div v-else-if="healthReportStatus === 'rejected'" class="status-rejected">
            <span>❌ 审核未通过，请重新上传</span>
          </div>
          <div v-else class="status-none">
            <span>请上传体检报告</span>
          </div>
          
          <input type="file" accept="image/*,.pdf" @change="handleFileChange" :disabled="uploading" />
          <button class="btn btn-secondary" type="button" :disabled="uploading" @click="uploadReport">
            {{ uploading ? '上传中...' : uploadButtonText }}
          </button>
          <p v-if="healthReportUrl" class="report-link">
            <a :href="fullHealthReportUrl" target="_blank">查看已上传的体检报告</a>
          </p>
        </div>
      </div>
    </div>
    <div class="button-row" style="margin-top: 18px">
      <button class="btn" type="button" :disabled="saving" @click="save">{{ saving ? "保存中..." : "保存资料" }}</button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { getUserInfo, saveUserInfo, getHealthReportStatus, uploadHealthReport, setElderlyFontSize } from "../../api/user";
import { useUserStore } from "../../stores/user";

const userStore = useUserStore();

const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
const errorText = ref("");
const successText = ref("");

const healthReportUrl = ref("");
const healthReportStatus = ref("");
const selectedFile = ref(null);

const availableTags = ["陪诊", "做饭", "买菜", "陪伴聊天", "简单家务", "陪护", "代买药", "代办事务"];

const ratingStats = reactive({
  avgRating: 0,
  totalScore: 0,
  commentCount: 0
});

const fullHealthReportUrl = computed(() => {
  if (!healthReportUrl.value) return "";
  if (healthReportUrl.value.startsWith("http")) return healthReportUrl.value;
  if (healthReportUrl.value.startsWith("/")) {
    return `${window.location.origin}${healthReportUrl.value}`;
  }
  return `${window.location.origin}/${healthReportUrl.value}`;
});

const uploadButtonText = computed(() => {
  if (!healthReportUrl.value) {
    return '上传体检报告';
  }
  if (healthReportStatus.value === 'approved') return '重新上传';
  if (healthReportStatus.value === 'rejected') return '重新上传';
  if (healthReportStatus.value === 'pending') return '重新上传体检报告';
  return '重新上传';
});

const form = reactive({
  id: "",
  nickname: "",
  realName: "",
  mobile: "",
  age: "",
  gender: "",
  healthDesc: "",
  skillTags: [],
  emergencyContact: "",
  emergencyMobile: "",
  fontSize: "medium"
});

async function loadData() {
  loading.value = true;
  errorText.value = "";
  try {
    const [userRes, healthRes] = await Promise.all([getUserInfo(), getHealthReportStatus()]);
    const profileData = userRes.data?.profile || userRes.data || {};
    if (profileData.skillTags) {
      form.skillTags = profileData.skillTags.split(",").map(t => t.trim()).filter(t => t);
    }
    Object.assign(form, profileData);
    if (!Array.isArray(form.skillTags)) {
      form.skillTags = form.skillTags ? form.skillTags.split(",").map(t => t.trim()).filter(t => t) : [];
    }

    if (form.fontSize) {
      userStore.setFontSize(form.fontSize);
    }

    ratingStats.avgRating = profileData.avgRating || 0;
    ratingStats.totalScore = profileData.totalScore || 0;
    ratingStats.commentCount = profileData.commentCount || 0;
    
    healthReportUrl.value = healthRes.data?.healthReportUrl || "";
    healthReportStatus.value = healthRes.data?.healthReportStatus || "";
  } catch (error) {
    errorText.value = error.message || "资料加载失败";
  } finally {
    loading.value = false;
  }
}

function handleFileChange(e) {
  const file = e.target.files[0];
  if (file) {
    selectedFile.value = file;
  }
}

async function uploadReport() {
  if (!selectedFile.value) {
    errorText.value = "请先选择文件";
    return;
  }
  uploading.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    await uploadHealthReport(selectedFile.value);
    successText.value = "体检报告已上传，等待审核";
    await loadData();
    selectedFile.value = null;
  } catch (error) {
    errorText.value = error.message || "上传失败";
  } finally {
    uploading.value = false;
  }
}

function toggleTag(tag) {
  const idx = form.skillTags.indexOf(tag);
  if (idx === -1) {
    form.skillTags.push(tag);
  } else {
    form.skillTags.splice(idx, 1);
  }
}

async function save() {
  saving.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const data = { ...form, skillTags: form.skillTags.join(",") };
    const res = await saveUserInfo(data);
    userStore.setFontSize(form.fontSize);
    if (userStore.role === 'child') {
      await setElderlyFontSize(form.id, form.fontSize);
    }
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
.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag-option {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
}
.tag-option:hover {
  border-color: #4a90d9;
}
.tag-option.selected {
  background: #e8f4fc;
  border-color: #4a90d9;
  color: #2a6099;
}
.tag-option input {
  display: none;
}

.health-report {
  padding: 12px 0;
}
.health-report input[type="file"] {
  margin: 8px 0;
}
.health-report .btn-secondary {
  margin-top: 8px;
}
.status-pending {
  color: #f57c00;
  font-size: 14px;
}
.status-approved {
  color: #4caf50;
  font-size: 14px;
}
.status-rejected {
  color: #f44336;
  font-size: 14px;
}
.status-none {
  color: #666;
  font-size: 14px;
}
.report-link {
  margin-top: 8px;
}
.report-link a {
  color: #4a90d9;
}
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
