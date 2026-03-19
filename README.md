# Hedwig

血糖数据 API 网关服务，用于把国内 CGM 厂商的数据统一采集并同步到 [Nightscout](http://www.nightscout.info/)。

目前支持：

- 欧泰 (Ottai)
- 硅基轻享 (SiSensing)

## 当前部署方式

项目已经改成更适合国内 Ubuntu 服务器的单体发布模式：

- 前端在构建时直接打进 Spring Boot `jar`
- 服务默认走 HTTP 单端口，无需证书也能先跑起来
- 默认使用内置 H2 文件数据库，单机可直接启动
- 如需 PostgreSQL，只需要改环境变量
- 不再依赖 Docker、Nginx 和镜像拉取

推荐流程：

1. 在本地或能正常联网的机器执行构建
2. 上传生成好的发布包到 Ubuntu
3. 在服务器直接运行 `jar`

## 项目结构

```text
hedwig/
├── backend/                  # Spring Boot 后端
├── frontend/                 # Rust Leptos WASM 前端
├── deploy/                   # 部署环境变量模板
├── scripts/                  # 构建、启动、systemd 安装脚本
└── doc/                      # 厂商 API 相关文档
```

## 本地开发

### 环境要求

- Java 17+
- Maven 3.9+
- Rust stable
- Trunk
- wasm32-unknown-unknown 目标

### 启动后端

```bash
cd backend
JWT_SECRET=dev-only-secret-change-me SERVER_PORT=3000 mvn spring-boot:run
```

### 启动前端

```bash
cd frontend
trunk serve
```

前端默认访问 `http://localhost:8080`，并把 `/api/` 代理到 `http://127.0.0.1:3000`。

## 一键构建发布包

在仓库根目录执行：

```bash
./scripts/build-release.sh
```

构建完成后会产出：

- `release/hedwig/`：可直接上传的发布目录
- `release/hedwig-YYYYMMDD-HHMMSS.tar.gz`：压缩包

如果只想打包不跑测试：

```bash
SKIP_TESTS=1 ./scripts/build-release.sh
```

## Ubuntu 部署

### 1. 上传并解压发布包

```bash
scp release/hedwig-*.tar.gz user@your-server:/opt/
ssh user@your-server
cd /opt
tar -xzf hedwig-*.tar.gz
cd hedwig
```

### 2. 首次启动

```bash
./run-http.sh start
```

脚本会自动：

- 复制 `app.env.example` 为 `app.env`
- 自动生成一个 JWT 密钥
- 创建 `data/` 和 `logs/` 目录
- 以后台模式启动服务

默认访问地址：

- `http://服务器IP:8080`

### 3. 常用命令

```bash
./run-http.sh status
./run-http.sh logs
./run-http.sh restart
./run-http.sh stop
```

### 4. 安装为 systemd 服务

```bash
sudo ./install-systemd.sh
```

安装后服务名默认为 `hedwig`。

## 关键环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SERVER_ADDRESS` | `0.0.0.0` | 监听地址 |
| `SERVER_PORT` | `8080` | HTTP 端口 |
| `TZ` | `Asia/Shanghai` | 时区 |
| `APP_DATA_DIR` | `./data` | 内置数据库和运行数据目录 |
| `JWT_SECRET` | 自动生成/示例值 | JWT 密钥，生产环境务必修改 |
| `JWT_EXPIRATION` | `86400000` | JWT 过期时间（毫秒） |
| `NIGHTSCOUT_ENABLED` | `false` | 是否启用 Nightscout |
| `NIGHTSCOUT_URL` | 空 | Nightscout 地址 |
| `NIGHTSCOUT_API_SECRET` | 空 | Nightscout API Secret |
| `DB_URL` | 内置 H2 文件库 | 可选改为 PostgreSQL |
| `DB_DRIVER_CLASS_NAME` | `org.h2.Driver` | JDBC 驱动类 |
| `DB_USERNAME` | `sa` | 数据库用户名 |
| `DB_PASSWORD` | 空 | 数据库密码 |
| `JPA_DIALECT` | `org.hibernate.dialect.H2Dialect` | Hibernate 方言 |
| `DDL_AUTO` | `update` | 表结构策略 |

## PostgreSQL 可选切换

如果你想改用 PostgreSQL，只需要在 `app.env` 中配置：

```bash
DB_URL=jdbc:postgresql://127.0.0.1:5432/hedwig
DB_DRIVER_CLASS_NAME=org.postgresql.Driver
DB_USERNAME=postgres
DB_PASSWORD=change-me
JPA_DIALECT=org.hibernate.dialect.PostgreSQLDialect
```

## API 接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 用户注册 | 否 |
| POST | `/api/auth/login` | 用户登录 | 否 |
| GET | `/api/vendors/connections` | 获取设备连接列表 | 是 |
| POST | `/api/vendors/connections/token` | 通过 Token 连接设备 | 是 |
| POST | `/api/vendors/connections/login` | 通过账号密码连接设备 | 是 |
| POST | `/api/vendors/connections/{id}/refresh` | 刷新设备 Token | 是 |
| DELETE | `/api/vendors/connections/{id}` | 断开设备连接 | 是 |
| GET | `/api/glucose/subjects/{id}/readings` | 查询血糖历史记录 | 是 |
| GET | `/api/glucose/subjects/{id}/readings/latest` | 查询最新血糖数据 | 是 |
| POST | `/api/glucose/subjects/{id}/sync` | 同步最近历史记录 | 是 |
| POST | `/api/nightscout/sync` | 手动同步至 Nightscout | 是 |
| GET | `/api/status` | 系统状态 | 是 |

## License

MIT
