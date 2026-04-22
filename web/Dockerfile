# 构建阶段：在镜像内安装依赖并产出前端静态资源。
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# 运行阶段：使用 Nginx 仅托管构建后的静态文件。
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

# 前台运行 Nginx，保持容器存活。
CMD ["nginx", "-g", "daemon off;"]
