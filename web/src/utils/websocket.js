import { getToken } from "./storage";

/**
 * WebSocket 连接管理器
 * 使用原生 WebSocket API
 */

let ws = null;
let isConnected = false;
let messageCallback = null;
let notificationCallback = null;
let reconnectTimer = null;

const WS_URL = (() => {
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL || "";
  console.log("WebSocket: VITE_API_BASE_URL =", envBaseUrl);
  if (/^https?:\/\//i.test(envBaseUrl)) {
    const url = envBaseUrl.replace(/^http/i, "ws") + "/ws/connect";
    console.log("WebSocket: calculated URL =", url);
    return url;
  }
  const url = "ws://127.0.0.1:8080/ws/connect";
  console.log("WebSocket: fallback URL =", url);
  return url;
})();

function getWebSocketUrl() {
  return WS_URL;
}

function connect(onMessage, onNotification) {
  if (isConnected && ws) {
    console.log("WebSocket: already connected");
    return;
  }

  messageCallback = onMessage;
  notificationCallback = onNotification;

  const token = getToken();
  if (!token) {
    console.warn("WebSocket: no token, skip connection");
    return;
  }

  const wsUrl = getWebSocketUrl() + "?token=" + token;
  console.log("WebSocket: connecting to", wsUrl);

  try {
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log("WebSocket: connected!");
      isConnected = true;

      ws.send(JSON.stringify({
        type: "subscribe",
        destination: "/queue/message"
      }));
      console.log("WebSocket: subscribed to /queue/message");
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log("WebSocket: received:", data);

        if (data.type === "new_message" && messageCallback) {
          messageCallback(data);
        } else if (data.type === "notification" && notificationCallback) {
          notificationCallback(data);
        }
      } catch (e) {
        console.error("WebSocket: parse error:", e);
      }
    };

    ws.onerror = (error) => {
      console.error("WebSocket: error:", error);
    };

    ws.onclose = (event) => {
      console.log("WebSocket: closed, code=", event.code, "reason=", event.reason);
      isConnected = false;
      ws = null;

      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
      }
      reconnectTimer = setTimeout(() => {
        console.log("WebSocket: reconnecting...");
        connect(messageCallback, notificationCallback);
      }, 5000);
    };
  } catch (e) {
    console.error("WebSocket: connection error:", e);
  }
}

function disconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  if (ws) {
    ws.close();
    ws = null;
  }

  isConnected = false;
  messageCallback = null;
  notificationCallback = null;
}

function getConnectionStatus() {
  return isConnected;
}

export {
  connect,
  disconnect,
  getConnectionStatus,
  getWebSocketUrl
};