import axios from "axios";
import { clearSession, getToken } from "./storage";

const envBaseUrl = import.meta.env.VITE_API_BASE_URL || "";
const baseURL = /^https?:\/\//i.test(envBaseUrl) ? envBaseUrl : "";

const service = axios.create({
  baseURL,
  timeout: 10000
});

service.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

service.interceptors.response.use(
  (response) => {
    const payload = response.data;

    if (payload && typeof payload === "object" && "code" in payload) {
      if (payload.code === 200 || payload.code === 0) {
        return payload;
      }

      if (payload.code === 401) {
        clearSession();
      }

      return Promise.reject(new Error(payload.message || "请求失败"));
    }

    return {
      code: 200,
      data: payload,
      message: "ok"
    };
  },
  (error) => {
    const message =
      error?.response?.data?.message ||
      error?.message ||
      "网络异常，请检查后端服务是否启动";

    if (error?.response?.status === 401) {
      clearSession();
    }

    return Promise.reject(new Error(message));
  }
);

export default service;
