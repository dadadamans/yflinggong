import request from "../utils/request";

export function getTaskList(params) {
  return request.get("/api/task/list", { params });
}

export function getTaskDetail(id) {
  return request.get("/api/task/detail", { params: { id } });
}

export function addTask(data) {
  return request.post("/api/task/add", data);
}

export function applyTask(id) {
  return request.post("/api/task/apply", { id });
}

export function approveTask(id) {
  return request.post("/api/task/approve", { id });
}

export function rejectTask(id) {
  return request.post("/api/task/reject", { id });
}

export function getTaskCategories() {
  return request.get("/api/task/categories");
}

export function getAdminWaitingTasks() {
  return request.get("/api/task/admin/waiting");
}
