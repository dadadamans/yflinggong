# 银发零工网页端

## 开发启动

```bash
cd web
npm install
cp .env.example .env
npm run dev
```

开发环境默认前端地址：

```text
http://127.0.0.1:5173
```

开发环境默认后端地址：

```text
http://127.0.0.1:8080
```

如果后端地址不同，请修改 `.env` 中的 `VITE_API_BASE_URL`。

开发环境下：

1. 前端浏览器访问 `5173` 端口。
2. Vite 会把 `/api` 请求代理到 `.env` 里的后端地址。

生产环境下：

1. 前端默认打包为静态文件。
2. 运行在 Nginx 容器中。
3. 前端接口默认访问相对路径 `/api`。
4. `/api` 由 Nginx 反向代理到后端容器 `backend:8080`。

## 已实现页面

1. 登录页
2. 首页
3. 老人端：任务大厅、任务详情、我的订单、个人中心、子女绑定
4. 雇主端：发布任务、我的任务、订单管理、个人中心
5. 子女端：绑定老人、发布任务、老人资料、订单查看、留言沟通、个人中心
6. 管理端：数据概览、用户管理、任务管理、订单管理、绑定关系

## 生产部署文件

当前前端工程已补充：

1. [Dockerfile](/home/bigold/code/oldBoss/web/Dockerfile)
2. [nginx.conf](/home/bigold/code/oldBoss/web/nginx.conf)
3. [.dockerignore](/home/bigold/code/oldBoss/web/.dockerignore)
4. [.env.production](/home/bigold/code/oldBoss/web/.env.production)
5. 根目录 [docker-compose.yml](/home/bigold/code/oldBoss/docker-compose.yml)

## Docker 部署

单独构建前端镜像：

```bash
cd web
docker build -t silver-job-web .
docker run -d --name silver-job-web -p 80:80 silver-job-web
```

如果前后端都已经准备好镜像，可在项目根目录执行：

```bash
docker compose up -d --build
```

说明：

1. 根目录 `docker-compose.yml` 负责同时编排数据库、后端、前端。
2. 当前 `nginx.conf` 假设后端服务名为 `backend`，端口为 `8080`。
3. 如果你的后端容器名或端口不同，需要同步修改 [nginx.conf](/home/bigold/code/oldBoss/web/nginx.conf)。

## 当前真实接口

前端已按以下接口进行联调封装：

认证

1. `POST /api/auth/register`
2. `POST /api/auth/login`
3. `POST /api/auth/logout`
4. `GET /api/auth/currentUser`

前端联调规则：

1. 登录成功后后端返回 Bearer token。
2. 当前 session 默认 `24` 小时过期，过期后前端需要重新登录。
3. 被禁用账号的旧 token 会失效。
4. 注册密码长度至少为 `6` 位。

用户

1. `GET /api/user/list`
2. `GET /api/user/info`
3. `POST /api/user/save`
4. `POST /api/user/setEnabled`
5. `POST /api/user/setElderlyFontSize`

体检报告

1. `POST /api/health/upload`
2. `POST /api/health/review`
3. `GET /api/health/status`

前端联调规则：

1. 体检报告上传当前只支持 `PDF`。
2. 文件名无效、无扩展名或类型不合法时，后端会返回业务错误。

任务与订单

1. `GET /api/task/categories`
2. `GET /api/task/list`
3. `GET /api/task/detail?id={id}`
4. `POST /api/task/add`
5. `POST /api/task/apply`
6. `POST /api/task/approve`
7. `POST /api/task/reject`
8. `GET /api/task/admin/waiting`
9. `GET /api/task/orders`
10. `POST /api/task/order/finish`
11. `POST /api/task/order/pay`
12. `POST /api/task/order/cancel`
13. `POST /api/task/order/delete`

前端联调规则：

1. 老人任务大厅只展示 `waiting` 状态任务。
2. 老人任务大厅会自动排除已绑定子女发布的待接单任务。
3. 老人申请接单前，体检报告必须已审核通过。
4. `GET /api/task/list`、`GET /api/task/orders`、`GET /api/task/admin/waiting`、`GET /api/user/list` 支持可选分页参数 `page`、`pageSize`。
5. 不传分页参数时仍返回数组；传入分页参数时返回分页对象。

绑定

1. `POST /api/bind/createCode`
2. `POST /api/bind/confirm`
3. `POST /api/bind/unbind`
   当前版本暂不支持解绑，前端不提供解绑入口。
4. `GET /api/bind/elderlyInfo`
5. `GET /api/bind/orderList`
6. `GET /api/bind/list`

留言

1. `GET /api/message/list`
2. `POST /api/message/send`

评价

1. `POST /api/comment/add`
2. `GET /api/comment/user/{userId}`
3. `GET /api/comment/user/{userId}/stats`
4. `GET /api/comment/task/{taskId}`

前端联调规则：

1. 当前只允许在任务状态为 `done` 时评价。
2. 只有任务参与方可以评价任务另一方。
3. 不能评价自己。
4. 被评价人显示名由后端根据真实参与方生成，不使用前端传入的 `revieweeName`。
5. `GET /api/comment/user/{userId}` 和 `GET /api/comment/task/{taskId}` 支持可选分页参数 `page`、`pageSize`。

参数校验说明：

1. 注册、登录、发布任务、绑定确认、留言发送、评价提交、资料保存、字体设置等核心请求已接入后端标准参数校验。
2. 前端提交空值或超长字段时，后端会返回 `400` 和明确提示。

管理统计

1. `GET /api/admin/stats`

## 返回结构建议

建议后端统一返回：

```json
{
  "code": 200,
  "message": "ok",
  "data": {}
}
```

前端当前已兼容两种情况：

1. 标准 `code/data/message` 包装结构
2. 直接返回业务对象

## 登录接口示例字段

```json
{
  "username": "demo_user",
  "password": "123456"
}
```

## 最低联调要求

如果你希望当前页面全部顺畅联调，后端至少需要保证：

1. 登录接口返回 `token`
2. `currentUser` 能返回当前角色和用户资料
3. 任务、订单、绑定、留言、体检报告接口字段与当前代码保持一致

## 生产环境联调要求

如果你希望项目部署到服务器后可以直接访问，后端还需要额外保证：

1. 允许被 Nginx 反向代理访问。
2. 登录态建议使用 JWT 或 token 方案。
3. 接口前缀统一为 `/api`。
4. 上传图片后的访问地址可被公网访问。
5. 如果做跨域，优先由 Nginx 统一代理，尽量不要直接暴露多个端口给前端。

## 构建验证

本地已执行：

```bash
npm run build
```

构建通过。
