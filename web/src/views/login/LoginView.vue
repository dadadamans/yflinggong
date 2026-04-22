<template>
  <div class="login-wrap">
    <div class="login-box">
      <div class="brand-center">
        <h1>银发零工</h1>
        <p>让老年生活更充实</p>
      </div>

      <div class="tabs">
        <button
          class="tab"
          :class="{ active: activeTab === 'login' }"
          type="button"
          @click="activeTab = 'login'"
        >
          登录
        </button>
        <button
          class="tab"
          :class="{ active: activeTab === 'register' }"
          type="button"
          @click="activeTab = 'register'"
        >
          注册
        </button>
      </div>

      <div v-if="activeTab === 'login'" class="panel">
        <h2 class="section-title">欢迎回来</h2>
        <div class="form-grid">
          <div class="field field-full">
            <label>账号</label>
            <input v-model="loginForm.username" placeholder="请输入账号" />
          </div>
          <div class="field field-full">
            <label>密码</label>
            <input v-model="loginForm.password" type="password" placeholder="请输入密码" />
          </div>
        </div>
        <p v-if="loginError" class="error-text">{{ loginError }}</p>
        <div class="button-row">
          <button class="btn btn-full" type="button" :disabled="logging" @click="handleLogin">
            {{ logging ? "登录中..." : "登录" }}
          </button>
        </div>
      </div>

      <div v-if="activeTab === 'register'" class="panel">
        <h2 class="section-title">创建新账号</h2>
        <div class="form-grid">
          <div class="field field-full">
            <label>账号</label>
            <input v-model="registerForm.username" placeholder="请输入账号" />
          </div>
          <div class="field field-full">
            <label>密码</label>
            <input v-model="registerForm.password" type="password" placeholder="请输入密码" />
          </div>
          <div class="field field-full">
            <label>确认密码</label>
            <input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" />
          </div>
          <div class="field field-full">
            <label>手机号</label>
            <input v-model="registerForm.mobile" placeholder="请输入手机号" />
          </div>
          <div class="field field-full">
            <label>选择身份</label>
            <div class="role-options">
              <button
                v-for="item in roles"
                :key="item.value"
                class="role-btn"
                :class="{ active: registerForm.roleType === item.value }"
                type="button"
                @click="registerForm.roleType = item.value"
              >
                {{ item.title }}
              </button>
            </div>
          </div>
          <div v-if="registerForm.roleType === 'elderly'" class="field field-full">
            <label>真实姓名</label>
            <input v-model="registerForm.realName" placeholder="请输入真实姓名" />
          </div>
          <div v-if="registerForm.roleType === 'employer'" class="field field-full">
            <label>真实姓名</label>
            <input v-model="registerForm.realName" placeholder="请输入真实姓名" />
          </div>
          <div v-if="registerForm.roleType === 'child'" class="field field-full">
            <label>真实姓名</label>
            <input v-model="registerForm.realName" placeholder="请输入真实姓名" />
          </div>
          <div v-if="registerForm.roleType === 'child'" class="field field-full">
            <label>与老人关系</label>
            <input v-model="registerForm.relation" placeholder="如：子女、亲属" />
          </div>
          <div v-if="registerForm.roleType === 'child'" class="field field-full">
            <label>老人绑定码</label>
            <input v-model="registerForm.bindCode" placeholder="请输入老人绑定码" />
          </div>
        </div>
        <p v-if="registerError" class="error-text">{{ registerError }}</p>
        <div class="button-row">
          <button class="btn btn-full" type="button" :disabled="registering" @click="handleRegister">
            {{ registering ? "注册中..." : "注册" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "../../stores/user";
import { registerApi } from "../../api/auth";
import { setToken, setRole } from "../../utils/storage";

const router = useRouter();
const userStore = useUserStore();

const activeTab = ref("login");
const logging = ref(false);
const registering = ref(false);
const loginError = ref("");
const registerError = ref("");

const roles = [
  { value: "elderly", title: "老人" },
  { value: "employer", title: "雇主" },
  { value: "child", title: "子女" }
];

const loginForm = reactive({
  username: "",
  password: ""
});

const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: "",
  mobile: "",
  roleType: "elderly",
  realName: "",
  relation: "",
  bindCode: ""
});

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    loginError.value = "请输入账号和密码";
    return;
  }

  logging.value = true;
  loginError.value = "";

  try {
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password
    });
    if (userStore.role === "admin") {
      router.replace("/admin/dashboard");
    } else {
      router.replace("/home");
    }
  } catch (error) {
    loginError.value = error.message || "登录失败，请检查账号密码";
  } finally {
    logging.value = false;
  }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.password) {
    registerError.value = "请输入账号和密码";
    return;
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    registerError.value = "两次输入的密码不一致";
    return;
  }

  if (!registerForm.mobile) {
    registerError.value = "请输入手机号";
    return;
  }

  if (registerForm.roleType === "elderly" && !registerForm.realName) {
    registerError.value = "请输入真实姓名";
    return;
  }

  if (registerForm.roleType === "child" && !registerForm.relation) {
    registerError.value = "请输入与老人关系";
    return;
  }

  if (registerForm.roleType === "child" && !registerForm.bindCode) {
    registerError.value = "请输入老人绑定码";
    return;
  }

  registering.value = true;
  registerError.value = "";

  try {
    const res = await registerApi({
      username: registerForm.username,
      password: registerForm.password,
      roleType: registerForm.roleType,
      mobile: registerForm.mobile,
      realName: registerForm.realName,
      relation: registerForm.roleType === "child" ? registerForm.relation : "",
      bindCode: registerForm.roleType === "child" ? registerForm.bindCode : ""
    });

    const token = res.data?.token;
    const role = res.data?.currentRole;
    if (token) {
      setToken(token);
      setRole(role || registerForm.roleType);
    }

    activeTab.value = "login";
    loginForm.username = registerForm.username;
    loginForm.password = registerForm.password;
    registerError.value = "";
    alert(registerForm.bindCode ? "注册成功，已自动绑定老人" : "注册成功，请登录");
  } catch (error) {
    registerError.value = error.message || "注册失败";
  } finally {
    registering.value = false;
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-box {
  width: min(420px, 100%);
}

.brand-center {
  text-align: center;
  margin-bottom: 32px;
}

.brand-center h1 {
  margin: 0 0 8px;
  font-size: 36px;
  color: var(--primary);
}

.brand-center p {
  margin: 0;
  color: var(--text-muted);
}

.tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  border-radius: 14px;
  overflow: hidden;
  background: var(--bg-muted);
}

.tab {
  flex: 1;
  padding: 14px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-muted);
}

.tab.active {
  background: var(--primary);
  color: #fff;
}

.panel {
  background: rgba(255, 250, 242, 0.96);
  border: 1px solid rgba(221, 207, 184, 0.85);
  border-radius: var(--radius);
  padding: 28px;
  box-shadow: var(--shadow);
}

.section-title {
  margin: 0 0 24px;
  text-align: center;
}

.form-grid {
  display: grid;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-full {
  grid-column: 1 / -1;
}

.field label {
  font-weight: 600;
  font-size: 14px;
}

.field input {
  width: 100%;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 16px;
}

.field input:focus {
  outline: none;
  border-color: var(--primary);
}

.role-options {
  display: flex;
  gap: 12px;
}

.role-btn {
  flex: 1;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s;
}

.role-btn.active {
  border-color: var(--primary);
  background: linear-gradient(160deg, rgba(184, 92, 56, 0.14), rgba(255, 250, 242, 0.95));
  color: var(--primary);
}

.button-row {
  margin-top: 24px;
}

.btn-full {
  width: 100%;
}

.error-text {
  color: var(--danger);
  text-align: center;
  margin: 16px 0 0;
}

.hint {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  margin: 16px 0 0;
}
</style>
