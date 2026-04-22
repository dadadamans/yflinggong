<template>
  <div v-if="total > 0" class="pagination-bar">
    <div class="pagination-summary">
      共 {{ total }} 条，第 {{ page }} / {{ totalPages || 1 }} 页
    </div>

    <div class="pagination-controls">
      <label class="page-size-label">
        每页
        <select :value="pageSize" class="page-size-select" @change="handlePageSizeChange">
          <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
        </select>
        条
      </label>

      <button class="page-btn" :disabled="page <= 1" @click="emit('update:page', page - 1)">上一页</button>
      <button class="page-btn" :disabled="page >= totalPages" @click="emit('update:page', page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  page: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  total: { type: Number, required: true },
  pageSizeOptions: {
    type: Array,
    default: () => [5, 10, 20]
  }
});

const emit = defineEmits(["update:page", "update:pageSize"]);

const totalPages = computed(() => {
  if (!props.total) return 0;
  return Math.ceil(props.total / props.pageSize);
});

function handlePageSizeChange(event) {
  emit("update:pageSize", Number(event.target.value));
}
</script>

<style scoped>
.pagination-bar {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.pagination-summary {
  color: #6a645a;
  font-size: 14px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.page-size-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #5b5348;
  font-size: 14px;
}

.page-size-select {
  padding: 8px 10px;
  border: 1px solid #d7c9b6;
  border-radius: 8px;
  background: #fffaf2;
  color: #3f3a34;
}

.page-btn {
  padding: 8px 14px;
  border: 1px solid #d7c9b6;
  border-radius: 999px;
  background: #fffaf2;
  color: #5b5348;
  cursor: pointer;
  font-size: 14px;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
