<template>
  <section class="panel">
    <h3 class="section-title">绑定关系</h3>
    <p v-if="errorText" class="error-text">{{ errorText }}</p>
    <table v-if="binds.length" class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>绑定码</th>
          <th>老人</th>
          <th>子女</th>
          <th>关系</th>
          <th>状态</th>
          <th>创建时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in binds" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.bind_code }}</td>
          <td>{{ item.elderly_real_name || item.elderly_nickname || '-' }}</td>
          <td>{{ item.child_real_name || item.child_nickname || '-' }}</td>
          <td>{{ item.relation || '-' }}</td>
          <td>{{ item.confirmed ? '已绑定' : '待确认' }}</td>
          <td>{{ formatDate(item.created_at) }}</td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty">暂无绑定关系数据</div>
    <AppPagination
      v-if="total > 0"
      :page="currentPage"
      :page-size="pageSize"
      :total="total"
      @update:page="handlePageChange"
      @update:pageSize="handlePageSizeChange"
    />
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
