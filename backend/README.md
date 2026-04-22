# 银发零工后端

## Docker 启动

如果使用根目录的 `docker-compose.yml`，先准备环境文件：

```bash
cd backend
cp .env.example .env
```

然后按实际环境修改 `backend/.env`，可参考：

```env
DB_NAME=oldboss
DB_USERNAME=postgres
DB_PASSWORD=YourStrongDbPassword123!
POSTGRES_DB=oldboss
POSTGRES_USER=postgres
POSTGRES_PASSWORD=YourStrongDbPassword123!
ADMIN_USERNAME=admin
ADMIN_PASSWORD=YourStrongAdminPassword123!
ADMIN_NICKNAME=管理员
ADMIN_REAL_NAME=管理员
ADMIN_MOBILE=13600000000
```

变量说明：

1. `DB_USERNAME` / `DB_PASSWORD`：后端程序连接 PostgreSQL 时使用的账号密码。
2. `POSTGRES_USER` / `POSTGRES_PASSWORD`：PostgreSQL 容器首次初始化时创建数据库用户使用的账号密码。
3. `DB_PASSWORD` 和 `POSTGRES_PASSWORD` 通常保持一致，否则后端可能连不上数据库。
4. `ADMIN_USERNAME` / `ADMIN_PASSWORD`：系统初始化管理员账号，登录管理端时使用。

然后回到项目根目录执行：

```bash
docker compose up -d --build
```

说明：

1. PostgreSQL、后端、前端都会一起启动。
2. 体检报告会落到项目根目录 `uploads/healthReports/`。
3. 数据库数据会落到项目根目录 `postgres_data/`。

## 本地数据库初始化

当前项目默认连接本地 PostgreSQL：

```text
jdbc:postgresql://127.0.0.1:5432/oldboss
```

如果不使用 Docker，而是本地 PostgreSQL 单独启动，确保数据库已运行；如果数据库还是空的，请手动执行：

```bash
cd backend
psql -U postgres -d oldboss -f sql/schema.sql
psql -U postgres -d oldboss -f sql/seed.sql
```

## 启动

```bash
cd backend
mvn spring-boot:run
```

环境要求：

```text
JDK 17+
Maven 3.8+
PostgreSQL 14+
```

默认端口：

```text
http://127.0.0.1:8080
```

## 当前真实接口

认证

1. `POST /api/auth/register`
2. `POST /api/auth/login`
3. `POST /api/auth/logout`
4. `GET /api/auth/currentUser`

规则说明：

1. 登录成功后返回 Bearer token。
2. 当前 session 默认 `24` 小时过期，过期后需要重新登录。
3. 被禁用账号的旧 token 会失效。
4. 注册密码长度不能少于 `6` 位。

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

规则说明：

1. 体检报告当前只支持 `PDF` 文件。
2. 文件名无效、无扩展名或类型不合法时，会返回业务错误，不再返回 500。
3. 体检报告上传目录当前通过配置项 `app.upload.health-report-dir` 控制。

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

规则说明：

1. 老人任务大厅只展示 `waiting` 状态的任务。
2. 老人任务大厅会排除已绑定子女发布的待接单任务。
3. `POST /api/task/apply` 前，老人需要体检报告已审核通过。
4. `GET /api/task/list`、`GET /api/task/orders`、`GET /api/task/admin/waiting`、`GET /api/user/list` 支持可选分页参数：`page`、`pageSize`。
5. 不传分页参数时，接口保持原有数组返回；传入分页参数时，返回分页对象。

绑定

1. `POST /api/bind/createCode`
2. `POST /api/bind/confirm`
3. `POST /api/bind/unbind`
   当前版本暂不支持解绑，接口会返回业务错误。
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

规则说明：

1. 当前只允许在任务状态为 `done` 时评价。
2. 只有任务参与方可以评价。
3. 被评价人必须是该任务的另一方，且不能评价自己。
4. 评论中的 `revieweeName` 由后端根据真实任务参与方生成，不信任前端传值。
5. `GET /api/comment/user/{userId}` 和 `GET /api/comment/task/{taskId}` 支持可选分页参数：`page`、`pageSize`。

参数校验说明：

1. 注册、登录、发布任务、绑定确认、留言发送、评价提交、资料保存、字体设置等核心入口已接入标准参数校验。
2. 参数不合法时，后端统一返回 `400` 和明确业务提示。

管理统计

1. `GET /api/admin/stats`

## 数据库配置

默认读取以下环境变量：

```text
DB_USERNAME=postgres
DB_PASSWORD=replace_with_strong_password
POSTGRES_DB=oldboss
POSTGRES_USER=postgres
POSTGRES_PASSWORD=replace_with_strong_password
```

说明：

1. 当前 `application.yml` 默认端口为 `8080`
2. 当前项目不会自动执行 `schema.sql` 和 `seed.sql`
3. `DatabaseInitializer` 只会做少量补丁式表结构修正，不等同于完整建库初始化
4. 当前登录 session 过期时间可通过 `app.auth.session-expire-hours` 配置
5. 当前体检报告上传目录可通过 `app.upload.health-report-dir` 配置

## 当前实现说明

当前后端已接入 PostgreSQL：

1. 使用 Spring Boot 3.3
2. 主要数据访问方式为 MyBatis-Plus Mapper，少量初始化逻辑使用 JdbcTemplate
3. 登录、用户、任务、订单、绑定、留言、会话都落库到 PostgreSQL
4. 登录后返回 Bearer token

## 打包

```bash
cd backend
mvn package -DskipTests
```

## Docker

构建镜像：

```bash
cd backend
mvn package -DskipTests
docker build -t silver-job-backend .
```

运行容器：

```bash
docker run -d --name silver-job-backend -p 8080:8080 silver-job-backend
```
