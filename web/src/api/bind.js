import request from "../utils/request";

export function createBindCode() {
  return request.post("/api/bind/createCode");
}

export function confirmBind(code) {
  return request.post("/api/bind/confirm", { code });
}

export function getBindInfo() {
  return request.get("/api/bind/elderlyInfo");
}

export function getBindOrderList(params) {
  return request.get("/api/bind/orderList", { params });
}

export function getBindList(params) {
  return request.get("/api/bind/list", { params });
}

export function unbindBind() {
  return request.post("/api/bind/unbind");
}
