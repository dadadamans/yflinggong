import request from "../utils/request";

export function getOrderList(params) {
  return request.get("/api/task/orders", { params });
}

export function finishOrder(taskId) {
  return request.post("/api/task/order/finish", { id: taskId });
}

export function cancelOrder(taskId) {
  return request.post("/api/task/order/cancel", { id: taskId });
}

export function deleteOrder(taskId) {
  return request.post("/api/task/order/delete", { id: taskId });
}

export function payOrder(taskId) {
  return request.post("/api/task/order/pay", { id: taskId });
}