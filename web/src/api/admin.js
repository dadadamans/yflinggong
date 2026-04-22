import request from "../utils/request";

export function getAdminStats() {
  return request.get("/api/admin/stats");
}