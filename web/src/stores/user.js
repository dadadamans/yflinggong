import { computed, ref, watch } from "vue";
import { defineStore } from "pinia";
import { clearSession, getRole, getToken, getUser, setRole, setToken, setUser } from "../utils/storage";
import { getCurrentUser, loginApi, logoutApi } from "../api/auth";
import { connect as wsConnect, disconnect as wsDisconnect } from "../utils/websocket";

const roleLabelMap = {
  elderly: "老人",
  employer: "雇主",
  child: "子女",
  admin: "管理员"
};

function getStoredFontSize() {
  try {
    return localStorage.getItem("fontSize") || "";
  } catch {
    return "";
  }
}

export const useUserStore = defineStore("user", () => {
  const token = ref(getToken());
  const role = ref(getRole() || "elderly");
  const profile = ref(getUser());
  const fontSize = ref(getStoredFontSize() || profile.value?.fontSize || "");
  const onMessageCallback = ref(null);
  const onNotificationCallback = ref(null);

  function registerMessageCallback(onMessage, onNotification) {
    onMessageCallback.value = onMessage;
    onNotificationCallback.value = onNotification;
  }

  function initWebSocket() {
    if (!token.value) {
      return;
    }
    wsConnect(
      (data) => {
        console.log("Store 接收到消息分发中:", data);
        if (onMessageCallback.value) {
          onMessageCallback.value(data);
        }
      },
      (noti) => {
        if (onNotificationCallback.value) {
          onNotificationCallback.value(noti);
        }
      }
    );
  }

  watch(profile, (newProfile) => {
    fontSize.value = newProfile?.fontSize || "";
  });

  const isLoggedIn = computed(() => Boolean(token.value));
  const roleLabel = computed(() => roleLabelMap[role.value] || "用户");

  function saveSession(payload = {}) {
    token.value = payload.token || "";
    role.value = payload.currentRole || payload.roleType || role.value || "elderly";
    const nextProfile = payload.userInfo || payload.profile || null;
    profile.value = nextProfile;
    setToken(token.value);
    setRole(role.value);
    if (nextProfile) {
      setUser(profile.value);
      fontSize.value = nextProfile.fontSize || "";
      return;
    }

    clearSession();
    setToken(token.value);
    setRole(role.value);
    fontSize.value = "";
  }

function setFontSize(size) {
  fontSize.value = size;
  try {
    localStorage.setItem("fontSize", size);
  } catch {}
}

  async function login(form) {
    const res = await loginApi(form);
    saveSession(res.data || {});
    return res;
  }

  async function fetchCurrentUser() {
    const res = await getCurrentUser();
    const data = res.data || {};
    saveSession({
      token: token.value,
      currentRole: data.currentRole || data.roleType || role.value,
      userInfo: data.userInfo || data.profile || data
    });
    return res;
  }

  async function logout() {
    try {
      await logoutApi();
    } finally {
      wsDisconnect();
      token.value = "";
      role.value = "elderly";
      profile.value = null;
      fontSize.value = "";
      onMessageCallback.value = null;
      onNotificationCallback.value = null;
      clearSession();
    }
  }

  return {
    token,
    role,
    roleLabel,
    profile,
    fontSize,
    isLoggedIn,
    saveSession,
    setFontSize,
    login,
    fetchCurrentUser,
    logout,
    registerMessageCallback,
    initWebSocket
  };
});
