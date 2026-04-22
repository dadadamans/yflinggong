import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "../stores/user";
import LoginView from "../views/login/LoginView.vue";
import HomeView from "../views/home/HomeView.vue";
import UserLayout from "../layout/UserLayout.vue";
import AdminLayout from "../layout/AdminLayout.vue";
import ElderlyTasksView from "../views/elderly/ElderlyTasksView.vue";
import ElderlyTaskDetailView from "../views/elderly/ElderlyTaskDetailView.vue";
import ElderlyOrdersView from "../views/elderly/ElderlyOrdersView.vue";
import ElderlyProfileView from "../views/elderly/ElderlyProfileView.vue";
import BindView from "../views/child/BindView.vue";
import EmployerPublishView from "../views/employer/EmployerPublishView.vue";
import EmployerTasksView from "../views/employer/EmployerTasksView.vue";
import EmployerProfileView from "../views/employer/EmployerProfileView.vue";
import ChildElderlyView from "../views/child/ChildElderlyView.vue";
import ChildOrdersView from "../views/child/ChildOrdersView.vue";
import ChildMessagesView from "../views/child/ChildMessagesView.vue";
import ChildProfileView from "../views/child/ChildProfileView.vue";
import AdminDashboardView from "../views/admin/AdminDashboardView.vue";
import AdminUsersView from "../views/admin/AdminUsersView.vue";
import AdminTasksView from "../views/admin/AdminTasksView.vue";
import AdminOrdersView from "../views/admin/AdminOrdersView.vue";
import AdminBindsView from "../views/admin/AdminBindsView.vue";

const routes = [
  {
    path: "/login",
    name: "login",
    component: LoginView,
    meta: { public: true, title: "登录" }
  },
  {
    path: "/",
    redirect: "/home"
  },
  {
    path: "/",
    component: UserLayout,
    meta: { requiresAuth: true },
    children: [
      { path: "home", component: HomeView, meta: { title: "首页" } },
      { path: "elderly/tasks", component: ElderlyTasksView, meta: { title: "任务大厅", roles: ["elderly"] } },
      { path: "elderly/task/:id", component: ElderlyTaskDetailView, meta: { title: "任务详情", roles: ["elderly"] } },
      { path: "elderly/orders", component: ElderlyOrdersView, meta: { title: "我的订单", roles: ["elderly"] } },
      { path: "elderly/profile", component: ElderlyProfileView, meta: { title: "个人中心", roles: ["elderly"] } },
      { path: "elderly/bind", component: BindView, meta: { title: "子女绑定", roles: ["elderly"] } },
      { path: "employer/publish", component: EmployerPublishView, meta: { title: "发布任务", roles: ["employer", "child"] } },
      { path: "employer/tasks", component: EmployerTasksView, meta: { title: "任务管理", roles: ["employer", "child"] } },
      { path: "employer/profile", component: EmployerProfileView, meta: { title: "个人中心", roles: ["employer"] } },
      { path: "child/bind", component: BindView, meta: { title: "绑定老人", roles: ["child"] } },
      { path: "child/publish", component: EmployerPublishView, meta: { title: "发布任务", roles: ["child"] } },
      { path: "child/tasks", component: EmployerTasksView, meta: { title: "任务管理", roles: ["child"] } },
      { path: "child/elderly", component: ChildElderlyView, meta: { title: "老人资料", roles: ["child"] } },
      { path: "child/messages", component: ChildMessagesView, meta: { title: "留言沟通", roles: ["child", "elderly"] } },
      { path: "child/profile", component: ChildProfileView, meta: { title: "个人中心", roles: ["child"] } }
    ]
  },
  {
    path: "/admin",
    component: AdminLayout,
    meta: { requiresAuth: true, roles: ["admin"] },
    children: [
      { path: "", redirect: "/admin/dashboard" },
      { path: "dashboard", component: AdminDashboardView, meta: { title: "数据概览", roles: ["admin"] } },
      { path: "users", component: AdminUsersView, meta: { title: "用户管理", roles: ["admin"] } },
      { path: "tasks", component: AdminTasksView, meta: { title: "任务管理", roles: ["admin"] } },
      { path: "orders", component: AdminOrdersView, meta: { title: "订单管理", roles: ["admin"] } },
      { path: "binds", component: AdminBindsView, meta: { title: "绑定关系", roles: ["admin"] } }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to, from) => {
  const userStore = useUserStore();

  if (to.meta.public) {
    return true;
  }

  if (!userStore.isLoggedIn) {
    return "/login";
  }

  if (!userStore.profile && userStore.token) {
    try {
      await userStore.fetchCurrentUser();
    } catch {
      userStore.logout();
      return "/login";
    }
  }

  if (userStore.role === "admin") {
    if (to.path === "/admin" || to.path === "/admin/") {
      return "/admin/dashboard";
    }
    if (!to.path.startsWith("/admin")) {
      return "/admin/dashboard";
    }
    return true;
  }

  const roles = to.meta.roles;
  if (roles && !roles.includes(userStore.role)) {
    return "/home";
  }

  return true;
});

export default router;
