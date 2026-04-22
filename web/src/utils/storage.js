const TOKEN_KEY = "silver-job-token";
const ROLE_KEY = "silver-job-role";
const USER_KEY = "silver-job-user";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token || "");
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function getRole() {
  return localStorage.getItem(ROLE_KEY) || "";
}

export function setRole(role) {
  localStorage.setItem(ROLE_KEY, role || "");
}

export function clearRole() {
  localStorage.removeItem(ROLE_KEY);
}

export function getUser() {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user || {}));
}

export function clearUser() {
  localStorage.removeItem(USER_KEY);
}

export function clearSession() {
  clearToken();
  clearRole();
  clearUser();
}
