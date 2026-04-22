<template>
  <div class="shell">
    <aside class="side">
      <div class="brand">
        <h1>管理后台</h1>
      </div>
      <nav class="nav">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>
    <main class="main">
      <header class="topbar panel">
        <div>
          <h2 class="section-title">{{ route.meta.title || "后台页面" }}</h2>
        </div>
        <button class="btn-danger" type="button" @click="handleLogout">退出登录</button>
      </header>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useUserStore } from "../stores/user";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const navItems = [
  { label: "数据概览", to: "/admin/dashboard" },
  { label: "用户管理", to: "/admin/users" },
  { label: "任务管理", to: "/admin/tasks" },
  { label: "订单管理", to: "/admin/orders" },
  { label: "绑定关系", to: "/admin/binds" }
];

async function handleLogout() {
  await userStore.logout();
  router.replace("/login");
}
</script>
