# Hedwig

血糖数据 API 网关服务，用于把国内 CGM 厂商的数据统一采集并同步到 [Nightscout](http://www.nightscout.info/)。

目前支持：

- 欧泰 (Ottai)
- 硅基轻享 (SiSensing)

## 架构概览

单体发布模式，适合国内 Ubuntu 服务器部署：

- 前端在构建时直接打进 Spring Boot `jar`，单端口提供 API 和前端页面
- 使用 PostgreSQL 数据库
- 支持多个 Nightscout 推送目标（按用户管理，配置存储在数据库中）
- 自适应轮询：根据 CGM 厂商数据上报节奏动态调整采集频率
- 不依赖 Docker、Nginx

推荐流程：本地构建 → 上传发布包 → 服务器运行

## 项目结构

```text
hedwig/
├── backend/                  # Spring Boot 3.2.5 后端 (MyBatis-Plus + FastJSON2)
├── frontend-react/           # React 18 + TypeScript + Ant Design 5 前端 (Vite)
├── build/                    # 本地构建产物 (hedwig.jar)，git 跟踪，CI 直接部署
├── scripts/                  # 构建、启动、systemd 安装脚本
├── release/                  # 完整发布包输出目录
└── doc/                      # 厂商 API 逆向分析文档
```

## 本地开发

### 环境要求

- Java 17+
- Maven 3.6+
- Node.js 18+
- npm 9+
- PostgreSQL（默认连接 `localhost:15432/hedwig`）

### 启动后端

```bash
cd backend
JWT_SECRET=dev-only-secret-change-me mvn spring-boot:run
```

后端默认端口 `3000`（由 `application.yml` 中 `server.port` 决定）。

### 启动前端

```bash
cd frontend-react
npm install
npm run dev
```

前端默认访问 `http://localhost:5173`，并把 `/api/` 代理到 `http://127.0.0.1:3000`。

## 构建与部署

采用**本地构建 + CI 部署**模式：本地执行构建，产物 `build/hedwig.jar` 提交到 git，Woodpecker CI 只负责 SCP 部署到服务器。这样避免了低配服务器上构建 OOM 的问题。

### 工作流

```
本地开发 → ./scripts/build-and-stage.sh → git add build/ → git push
                                                              │
                            Gitea ──Webhook──► Woodpecker (仅部署)
                                                  │ SCP build/hedwig.jar
                                                  │ systemctl restart hedwig
                                                  ▼
                                               阿里云服务器
```

### 一键构建并推送

```bash
# 方式一：使用 git alias（构建 + 暂存 + 推送一步到位）
git bd

# 方式二：手动执行
./scripts/build-and-stage.sh
git add build/
git push
```

`build-and-stage.sh` 会依次执行：前端 `npm ci && npm run build` → 后端 `mvn clean package` → 拷贝 jar 到 `build/hedwig.jar`。

### 完整发布包（可选）

如果需要生成可上传的独立发布包（包含启动脚本、环境变量模板等）：

```bash
./scripts/build-release.sh

# 跳过测试
SKIP_TESTS=1 ./scripts/build-release.sh
```

产出 `release/hedwig/` 目录和 `release/hedwig-YYYYMMDD-HHMMSS.tar.gz` 压缩包。

## 服务器部署

服务器上通过 systemd 管理 Hedwig 服务。CI Pipeline 会自动将 `build/hedwig.jar` SCP 到服务器并重启服务。

### 首次部署

1. 将发布包上传到服务器并解压到 `/opt/hedwig/`
2. 配置环境变量：`cp app.env.example app.env && vim app.env`
3. 安装 systemd 服务：`./install-systemd.sh`

### 常用命令

```bash
sudo systemctl status hedwig     # 查看状态
sudo systemctl restart hedwig    # 重启
sudo systemctl stop hedwig       # 停止
sudo journalctl -u hedwig -f     # 查看日志
```

## 关键环境变量

以下变量在 `app.env` 中配置（或通过系统环境变量覆盖）：

| 变量             | 默认值                                          | 说明                                  |
|------------------|------------------------------------------------|---------------------------------------|
| `SERVER_ADDRESS` | `0.0.0.0`                                      | 监听地址                              |
| `SERVER_PORT`    | `8080`（脚本启动） / `3000`（直接运行 jar）       | HTTP 端口                             |
| `TZ`             | `Asia/Shanghai`                                | 时区                                  |
| `JWT_SECRET`     | 自动生成                                        | JWT 签名密钥，**生产环境务必修改**     |
| `JWT_EXPIRATION` | `86400000`                                     | JWT 过期时间（毫秒），默认 24 小时     |
| `APP_DATA_DIR`   | `./data`                                       | 数据存储目录                          |
| `JAVA_OPTS`      | `-Xms256m -Xmx512m`                           | JVM 参数                              |
| `DB_URL`         | `jdbc:postgresql://localhost:15432/hedwig`      | PostgreSQL JDBC 连接地址              |
| `DB_USERNAME`    | `postgres`                                     | 数据库用户名                          |
| `DB_PASSWORD`    | `postgres`                                     | 数据库密码                            |

## API 接口

### 认证

| 方法 | 路径                   | 说明     | 认证 |
|------|------------------------|----------|------|
| POST | `/api/auth/register`   | 用户注册 | 否   |
| POST | `/api/auth/login`      | 用户登录 | 否   |

### 设备连接管理

| 方法   | 路径                                     | 说明                 |
|--------|------------------------------------------|----------------------|
| GET    | `/api/vendors/connections`               | 获取设备连接列表     |
| POST   | `/api/vendors/connections/token`         | 通过 Token 连接设备  |
| POST   | `/api/vendors/connections/login`         | 通过账号密码连接设备 |
| POST   | `/api/vendors/connections/{id}/refresh`  | 刷新设备 Token       |
| DELETE | `/api/vendors/connections/{id}`          | 断开设备连接         |

### 血糖数据

| 方法 | 路径                                                                    | 说明                     |
|------|-------------------------------------------------------------------------|--------------------------|
| GET  | `/api/glucose/subjects/{subjectId}/readings`                            | 查询血糖历史记录         |
| GET  | `/api/glucose/subjects/{subjectId}/readings/latest`                     | 查询最新血糖数据         |
| POST | `/api/glucose/connections/{connId}/subjects/{subjectId}/fetch/latest`   | 从厂商实时拉取最新数据   |
| POST | `/api/glucose/connections/{connId}/subjects/{subjectId}/fetch/history`  | 从厂商拉取近 24h 历史    |
| POST | `/api/glucose/subjects/{subjectId}/sync`                                | 同步最近历史记录         |

### Nightscout 管理

| 方法   | 路径                                | 说明                         |
|--------|-------------------------------------|------------------------------|
| GET    | `/api/nightscout/targets`           | 获取 Nightscout 推送目标列表 |
| GET    | `/api/nightscout/targets/{id}`      | 获取单个推送目标详情         |
| POST   | `/api/nightscout/targets`           | 创建推送目标                 |
| PUT    | `/api/nightscout/targets/{id}`      | 修改推送目标                 |
| DELETE | `/api/nightscout/targets/{id}`      | 删除推送目标                 |
| POST   | `/api/nightscout/targets/{id}/toggle` | 启用/停用推送目标          |
| POST   | `/api/nightscout/sync`              | 手动触发同步到 Nightscout    |

### 系统状态

| 方法 | 路径           | 说明                                       |
|------|----------------|--------------------------------------------|
| GET  | `/api/status`  | 系统状态（连接数、血糖条数、Nightscout 等） |

## License

MIT
