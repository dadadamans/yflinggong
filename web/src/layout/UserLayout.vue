<template>
  <div class="shell">
    <aside class="side">
      <div class="brand">
        <h1>{{ titleMap[userStore.role] }}</h1>
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
          <h2 class="section-title">{{ route.meta.title || "页面" }}</h2>
          <div class="muted">当前身份：{{ userStore.roleLabel }}{{ userStore.profile?.nickname ? ` / ${userStore.profile.nickname}` : "" }}</div>
        </div>
        <div class="button-row">
          <button class="btn-ghost" type="button" @click="router.push('/home')">返回首页</button>
          <button class="btn-danger" type="button" @click="handleLogout">退出登录</button>
        </div>
      </header>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useUserStore } from "../stores/user";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const navMap = {
  elderly: [
    { label: "首页", to: "/home" },
    { label: "任务大厅", to: "/elderly/tasks" },
    { label: "我的订单", to: "/elderly/orders" },
    { label: "个人中心", to: "/elderly/profile" },
    { label: "子女绑定", to: "/elderly/bind" }
  ],
  employer: [
    { label: "首页", to: "/home" },
    { label: "发布任务", to: "/employer/publish" },
    { label: "任务管理", to: "/employer/tasks" },
    { label: "个人中心", to: "/employer/profile" }
  ],
  child: [
    { label: "首页", to: "/home" },
    { label: "绑定老人", to: "/child/bind" },
    { label: "发布任务", to: "/child/publish" },
    { label: "任务管理", to: "/child/tasks" },
    { label: "老人资料", to: "/child/elderly" },
    { label: "留言沟通", to: "/child/messages" },
    { label: "个人中心", to: "/child/profile" }
  ]
};

const titleMap = {
  elderly: "老人工作台",
  employer: "雇主工作台",
  child: "子女工作台"
};

const navItems = computed(() => navMap[userStore.role] || navMap.elderly);

async function handleLogout() {
  await userStore.logout();
  router.replace("/login");
}
</script>
