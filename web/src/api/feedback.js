import request from "../utils/request";

export function submitFeedback(data) {
  return request.post("/api/feedback", data);
}

export function getMyFeedbackList(params) {
  return request.get("/api/feedback", { params });
}

export function getAdminFeedbackList(params) {
  return request.get("/api/feedback/admin", { params });
}

export function updateFeedbackStatus(id, status) {
  return request.put(`/api/feedback/${id}`, { status });
}