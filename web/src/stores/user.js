import { computed, ref, watch } from "vue";
import { defineStore } from "pinia";
import { clearSession, getRole, getToken, getUser, setRole, setToken, setUser } from "../utils/storage";
import { getCurrentUser, loginApi, logoutApi } from "../api/auth";

export const useUserStore = defineStore("user", () => {
  const token = ref(getToken());
  const role = ref(getRole() || "elderly");
  const profile = ref(getUser());
  const fontSize = ref(profile.value?.fontSize || "");

  // Update fontSize when profile changes
  watch(profile, (newProfile) => {
    fontSize.value = newProfile?.fontSize || "";
  });

  const isLoggedIn = computed(() => Boolean(token.value));

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
      token.value = "";
      role.value = "elderly";
      profile.value = null;
      fontSize.value = "";
      clearSession();
    }
  }

  return {
    token,
    role,
    profile,
    fontSize,
    isLoggedIn,
    saveSession,
    setFontSize,
    login,
    fetchCurrentUser,
    logout
  };
});
