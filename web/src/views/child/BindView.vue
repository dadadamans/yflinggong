<template>
  <div class="split">
    <section class="panel">
      <h3 class="section-title">{{ isChild ? "输入绑定码" : "生成绑定码" }}</h3>
      <p v-if="errorText" class="error-text">{{ errorText }}</p>
      <p v-if="successText">{{ successText }}</p>
      <div v-if="loading" class="empty">正在加载绑定信息...</div>
      <div v-else class="grid">
        <article class="task-card">
          <h4 class="section-title">当前状态</h4>
          <div class="meta">
            <span>绑定码：{{ info?.bindInfo?.code || info?.code || "-" }}</span>
            <span>状态：{{ info?.bindInfo?.confirmed || info?.confirmed ? "已绑定" : "待确认" }}</span>
          </div>
        </article>

        <template v-if="!isChild">
          <div class="button-row">
            <button class="btn" type="button" :disabled="operating" @click="refreshCode">
              {{ operating ? "处理中..." : "重新生成绑定码" }}
            </button>
          </div>
        </template>

        <template v-else>
          <div class="field">
            <label>绑定码</label>
            <input v-model="inputCode" maxlength="6" placeholder="请输入老人提供的绑定码" />
          </div>
          <div class="button-row">
            <button class="btn-secondary" type="button" :disabled="operating" @click="submitBind">
              {{ operating ? "处理中..." : "确认绑定" }}
            </button>
          </div>
        </template>
      </div>
    </section>

    <section class="panel">
      <h3 class="section-title">说明</h3>
      <div class="card-list">
        <article class="message-card">老人端负责生成绑定码。</article>
        <article class="message-card">子女端输入绑定码后建立关系。</article>
        <article class="message-card">绑定成功后，子女可查看老人资料、订单和留言板。</article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { createBindCode, confirmBind, getBindInfo } from "../../api/bind";
import { useUserStore } from "../../stores/user";

const userStore = useUserStore();
const loading = ref(false);
const operating = ref(false);
const errorText = ref("");
const successText = ref("");
const info = ref(null);
const inputCode = ref("");

const isChild = computed(() => userStore.role === "child");

async function loadData() {
  loading.value = true;
  errorText.value = "";
  try {
    const res = await getBindInfo();
    info.value = res.data || null;
  } catch (error) {
    errorText.value = error.message || "绑定信息加载失败";
  } finally {
    loading.value = false;
  }
}

async function refreshCode() {
  operating.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const res = await createBindCode();
    info.value = res.data || null;
    inputCode.value = "";
    successText.value = res.message || "已生成新绑定码";
  } catch (error) {
    errorText.value = error.message || "生成失败";
  } finally {
    operating.value = false;
  }
}

async function submitBind() {
  operating.value = true;
  errorText.value = "";
  successText.value = "";
  try {
    const res = await confirmBind(inputCode.value);
    successText.value = res.message || "绑定成功";
    await loadData();
  } catch (error) {
    errorText.value = error.message || "绑定失败";
  } finally {
    operating.value = false;
  }
}

onMounted(loadData);
</script>
