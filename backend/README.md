# 银发零工后端

## 启动

```bash
cd backend
mvn spring-boot:run
```

环境要求：

```text
JDK 17+
Maven 3.9+
PostgreSQL 14+
```

默认端口：

```text
http://127.0.0.1:8080
```

## 已实现接口

1. `POST /api/auth/login`
2. `POST /api/auth/logout`
3. `GET /api/auth/currentUser`
4. `GET /api/user/info`
5. `POST /api/user/save`
6. `GET /api/task/list`
7. `GET /api/task/detail?id={id}`
8. `POST /api/task/add`
9. `POST /api/task/grab`
10. `GET /api/order/list`
11. `POST /api/order/finish`
12. `POST /api/order/cancel`
13. `POST /api/bind/createCode`
14. `POST /api/bind/confirm`
15. `GET /api/bind/elderlyInfo`
16. `GET /api/bind/orderList`
17. `GET /api/message/list`
18. `POST /api/message/send`

## 数据库配置

默认读取以下环境变量：

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/oldboss
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

应用启动时会自动执行：

```text
backend/sql/schema.sql
backend/sql/seed.sql
```

## 当前实现说明

当前后端已接入 PostgreSQL：

1. 使用 Spring Boot 3.3 + JdbcTemplate
2. 登录、用户、任务、订单、绑定、留言、会话都落库到 PostgreSQL
3. 登录后返回 Bearer token
4. 首次启动会自动导入演示数据

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
