import request from "../utils/request";

export function addComment(data) {
  return request.post("/api/comment/add", data);
}

export function getUserComments(userId, params) {
  return request.get(`/api/comment/user/${userId}`, { params });
}

export function getUserCommentStats(userId) {
  return request.get(`/api/comment/user/${userId}/stats`);
}

export function getTaskComments(taskId, params) {
  return request.get(`/api/comment/task/${taskId}`, { params });
}
