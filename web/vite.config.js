import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  return {
    plugins: [vue()],
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes("node_modules")) {
              return;
            }

            if (id.includes("element-plus")) {
              return "element-plus";
            }

            if (id.includes("echarts") || id.includes("zrender")) {
              return "echarts";
            }

            if (
              id.includes("/vue/") ||
              id.includes("/vue-router/") ||
              id.includes("/pinia/")
            ) {
              return "vue-vendor";
            }

            return "vendor";
          }
        }
      }
    },
    server: {
      port: 5173,
      host: "0.0.0.0",
      open: 'chrome', 
      proxy: {
        "/api": {
          target: env.VITE_API_BASE_URL || "http://127.0.0.1:8080",
          changeOrigin: true
        }
      }
    }
  };
});
