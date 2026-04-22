import request from "../utils/request";

export function loginApi(data) {
  return request.post("/api/auth/login", data);
}

export function registerApi(data) {
  return request.post("/api/auth/register", data);
}

export function logoutApi() {
  return request.post("/api/auth/logout");
}

export function getCurrentUser() {
  return request.get("/api/auth/currentUser");
}
