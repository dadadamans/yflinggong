<template>
  <section class="panel">
    <h3 class="section-title">{{ roleText }}发布任务</h3>
    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <p v-if="successText">{{ successText }}</p>
    <div class="form-grid">
      <div class="field"><label>任务标题</label><input v-model="form.title" /></div>
      <div class="field"><label>服务地址</label><input v-model="form.address" /></div>
      <div class="field field-half">
        <label>任务类型</label>
        <select v-model="form.category" @change="form.type = ''">
          <option value="">选择大类</option>
          <option v-for="cat in taskCategories" :key="cat.value" :value="cat.value">{{ cat.label }}</option>
        </select>
      </div>
      <div class="field field-half">
        <label>具体类型</label>
        <select v-model="form.type" :disabled="!form.category">
          <option value="">选择小类</option>
          <option v-for="sub in currentSubTypes" :key="sub.value" :value="sub.value">{{ sub.label }}</option>
        </select>
      </div>
      <div class="field field-half">
        <label>服务日期</label>
        <select v-model="form.taskDate">
          <option v-for="date in dateOptions" :key="date.value" :value="date.value">{{ date.label }}</option>
        </select>
      </div>
      <div class="field field-half">
        <label>服务时间</label>
        <select v-model="form.taskTimeSlot">
          <option v-for="slot in timeSlots" :key="slot" :value="slot">{{ slot }}</option>
        </select>
      </div>
      <div class="field field-full"><label>报酬金额</label><input v-model="form.salary" type="number" /></div>
      <div class="field field-full"><label>任务内容</label><textarea v-model="form.content" /></div>
    </div>
    <div class="button-row" style="margin-top: 18px">
      <button class="btn" type="button" :disabled="saving" @click="submit">{{ saving ? "发布中..." : "发布任务" }}</button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { addTask, getTaskCategories } from "../../api/task";
import { useUserStore } from "../../stores/user";

const router = useRouter();
const userStore = useUserStore();
const saving = ref(false);
const errorText = ref("");
const successText = ref("");
const taskCategories = ref([]);

const timeSlots = [
  "08:00-10:00",
  "10:00-12:00",
  "12:00-14:00",
  "14:00-16:00",
  "16:00-18:00",
  "18:00-20:00",
  "20:00-22:00"
];

const defaultForm = () => {
  const today = new Date();
  const d = new Date(today);
  const defaultDate = d.toISOString().split("T")[0];
  return {
    title: "陪老人去医院复查",
    category: "skill",
    type: "escort",
    address: "红谷滩区梅岭大道 1999号",
    salary: 150,
    taskDate: defaultDate,
    taskTimeSlot: "14:00-16:00",
    timeText: ""
  };
};

const form = reactive(defaultForm());

const currentSubTypes = computed(() => {
  if (!form.category) return [];
  const cat = taskCategories.value.find(c => c.value === form.category);
  return cat ? cat.children : [];
});

const dateOptions = computed(() => {
  const dates = [];
  const today = new Date();
  for (let i = 0; i < 7; i++) {
    const d = new Date(today);
    d.setDate(today.getDate() + i);
    const label = i === 0 ? "今天" : i === 1 ? "明天" : i === 2 ? "后天" : `${i}天后`;
    dates.push({
      label: `${label} (${d.getMonth() + 1}/${d.getDate()})`,
      value: d.toISOString().split("T")[0]
    });
  }
  return dates;
});

const roleText = computed(() => (userStore.role === "child" ? "子女" : "雇主"));

onMounted(async () => {
  try {
    const res = await getTaskCategories();
    taskCategories.value = res.data || [];
  } catch (e) {
    console.error("加载分类失败", e);
  }
});

async function submit() {
  saving.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    if (!form.category || !form.type) {
      throw new Error("请选择任务类型");
    }
    const timeText = `${form.taskDate} ${form.taskTimeSlot}`;
    const res = await addTask({ 
      title: form.title,
      category: form.category,
      type: form.type,
      address: form.address,
      salary: form.salary,
      timeText,
      content: form.content
    });
    successText.value = res.message || "任务已发布";
    Object.assign(form, defaultForm());
    router.push(userStore.role === "child" ? "/child/tasks" : "/employer/tasks");
  } catch (error) {
    errorText.value = error.message || "发布失败";
  } finally {
    saving.value = false;
  }
}
</script>