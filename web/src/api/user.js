import request from "../utils/request";

export function getUserList(params) {
  return request.get("/api/user/list", { params });
}

export function getUserInfo() {
  return request.get("/api/user/info");
}

export function saveUserInfo(data) {
  return request.post("/api/user/save", data);
}

export function setUserEnabled(userId, enabled) {
  return request.post("/api/user/setEnabled", { userId, enabled });
}

export function uploadHealthReport(file) {
  const formData = new FormData();
  formData.append("file", file);
  return request.post("/api/health/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" }
  });
}

export function getHealthReportStatus() {
  return request.get("/api/health/status");
}

export function reviewHealthReport(userId, approved, healthCondition) {
  return request.post("/api/health/review", { userId, enabled: approved, healthCondition });
}

export function setElderlyFontSize(elderlyId, fontSize) {
  return request.post("/api/user/setElderlyFontSize", { elderlyId, fontSize });
}
