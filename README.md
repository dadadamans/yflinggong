# 银发零工 (Silver Job)

一个面向老年群体的零工接单平台，支持老人接单、雇主/子女发单、子女绑定管理等功能。

## 功能特点

- **多角色系统**：老人、雇主、子女、管理员四种角色
- **任务管理**：发布、浏览、接单、订单全流程
- **子女绑定**：老人发起绑定，子女输入绑定码关联
- **即时沟通**：子女与老人之间可发送留言
- **健康审核**：老人上传体检报告，管理员审核后，方可接单
- **数据看板**：管理员查看平台统计数据

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 17 + Spring Boot 3.3 + PostgreSQL + MyBatis-Plus |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router |
| 部署 | Docker + Docker Compose + Nginx |

## 环境要求

在开始之前，请确保你的开发环境已安装以下工具：

| 工具 | 最低版本 | 备注 |
|---|---|---|
| Node.js | v18.0.0+ | 推荐使用 LTS 版本 (如 v20) |
| Package Manager | npm 9+ 或 pnpm 8+ | 推荐使用 pnpm 以获得更快的安装速度 |
| Java SDK | 17 | 后端运行必需 |
| PostgreSQL | 14+ | 本地开发时需要 |
| Docker | 24.0+ | 仅在使用 Docker 部署时需要 |

## 项目结构

```
.
├── backend/              # Spring Boot 后端
│   ├── src/
│   ├── sql/              # 数据库脚本
│   ├── pom.xml
│   └── Dockerfile
├── web/                  # Vue 前端
│   ├── src/
│   ├── package.json
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml     # 一键部署编排
└── uploads/               # 文件上传目录
```

## 快速启动

### Docker Compose 部署（推荐）

#### 前端环境变量 (web/.env)

> Docker 部署时可选，前端代码已内置默认配置。如需自定义可提前修改。

| 变量名 | 说明 | 默认值 |
|---|---|---|
| `VITE_API_BASE_URL` | 后端 API 基地址，构建时硬编码进 JS | http://127.0.0.1:8080 |
| `VITE_APP_TITLE` | 网页标题 | 银发零工网页端 |

#### 部署步骤

```bash
# 1. 准备后端环境变量
cd backend
cp .env.example .env
# ⚠️ 必须编辑 .env，将以下占位符替换为实际值，否则后端无法启动
#   - DB_PASSWORD
#   - POSTGRES_PASSWORD
#   - ADMIN_PASSWORD

# 2. 编译后端 jar 包（确保已安装 Maven）
mvn clean package -DskipTests
# 编译成功后生成 target/silver-job-backend-1.0.0.jar

# 3. 返回根目录启动
cd ..
mkdir -p uploads
docker compose up -d --build
```

> **注意**：
> - 后端 Dockerfile 只负责运行 jar，不含 Maven，必须先在本地编译
> - `.env` 中的 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD` 无默认值，不修改会导致后端启动失败
> - Docker 启动时会自动执行 `schema.sql` 初始化数据库（仅首次启动时），不会自动导入测试数据

#### 后端环境变量说明 (backend/.env)

| 变量名 | 说明 |
|---|---|
| `DB_NAME` | 数据库名称 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `POSTGRES_DB` | Postgres 数据库名 |
| `POSTGRES_USER` | Postgres 用户名 |
| `POSTGRES_PASSWORD` | Postgres 密码 |
| `ADMIN_USERNAME` | 管理员用户名 |
| `ADMIN_PASSWORD` | 管理员密码 |
| `ADMIN_NICKNAME` | 管理员昵称 |
| `ADMIN_REAL_NAME` | 管理员真实姓名 |
| `ADMIN_MOBILE` | 管理员手机号 |

（可选）导入测试数据以便快速体验：

```bash
# 等待容器启动完成后执行
docker exec -i silver-job-db psql -U postgres -d oldboss < backend/sql/seed.sql
```

访问：

- 前端：http://127.0.0.1:3000
- 后端 API：http://127.0.0.1:8080

## 角色说明

| 角色 | 说明 |
|---|---|
| 老人 | 浏览任务大厅、查看任务详情、接单、查看我的订单、上传体检报告 |
| 雇主 | 发布任务、查看我的任务、管理订单 |
| 子女 | 绑定老人、替家庭发布任务、查看老人资料和订单、与老人留言沟通 |
| 管理员 | 数据概览、用户管理、任务审核、订单管理、绑定关系管理 |

## API 概览

### 认证

- `POST /api/auth/register` - 注册
- `POST /api/auth/login` - 登录
- `POST /api/auth/logout` - 登出
- `GET /api/auth/currentUser` - 获取当前用户

### 用户

- `GET /api/user/list` - 用户列表
- `GET /api/user/info` - 用户资料
- `POST /api/user/save` - 保存资料

### 任务

- `GET /api/task/categories` - 任务分类
- `GET /api/task/list` - 任务列表
- `GET /api/task/detail` - 任务详情
- `POST /api/task/add` - 发布任务
- `POST /api/task/apply` - 老人接单
- `POST /api/task/approve` - 审核通过
- `POST /api/task/reject` - 审核拒绝
- `GET /api/task/orders` - 我的订单
- `POST /api/task/order/finish` - 完成任务
- `POST /api/task/order/pay` - 模拟支付
- `POST /api/task/order/cancel` - 取消订单

### 绑定

- `POST /api/bind/createCode` - 生成绑定码
- `POST /api/bind/confirm` - 确认绑定
- `GET /api/bind/list` - 绑定列表
- `GET /api/bind/elderlyInfo` - 查看老人资料

### 留言

- `GET /api/message/list` - 留言列表
- `POST /api/message/send` - 发送留言

### 评价

- `POST /api/comment/add` - 提交评价
- `GET /api/comment/task/{taskId}` - 任务评价

### 体检报告

- `POST /api/health/upload` - 上传体检报告
- `POST /api/health/review` - 管理员审核
- `GET /api/health/status` - 查看审核状态

### 管理统计

- `GET /api/admin/stats` - 平台数据统计


## 许可证

[MIT License](LICENSE)
