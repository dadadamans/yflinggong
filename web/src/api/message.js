import request from "../utils/request";

export function getMessageList(params) {
  return request.get("/api/message/list", { params });
}

export function sendMessage(data) {
  return request.post("/api/message/send", data);
}
