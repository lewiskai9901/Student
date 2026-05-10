# Docker Compose 部署 SOP (生产单机)

适用场景: 中小规模单机部署 (1-2 台服务器, 自维基础设施)。
不适用: K8s 集群 (用 helm chart, 待实现) / 大流量 (需要专业 DBA + 横向扩展)。

## 1. 前置环境

| 组件 | 最低版本 | 备注 |
|------|---------|------|
| Docker Engine | 20.10+ | Docker Desktop 或 Linux server |
| Docker Compose | v2.20+ | `docker compose` (空格) 不是 `docker-compose` |
| 服务器 | 4 核 8G | backend 限 2G, mysql + redis 各 1-2G |
| 磁盘 | ≥ 50G | mysql data + uploads + logs |
| 操作系统 | Linux 任一 / Win Docker Desktop | 生产推荐 Ubuntu 22.04 LTS |

## 2. 初次部署

```bash
# 1. 克隆代码到目标服务器
git clone <repo-url> /opt/student-management
cd /opt/student-management

# 2. 配置环境变量
cp .env.production.example .env.production
vim .env.production
# 关键字段:
#   MYSQL_ROOT_PASSWORD       MySQL root 密码 (compose 强引用)
#   DB_PASSWORD               app_user 密码
#   REDIS_PASSWORD            Redis auth
#   JWT_SECRET                ≥ 64 字节, 用于 HS512
#   GRAFANA_ADMIN_PASSWORD    Grafana 管理密码 (启 monitoring 才需)

# 3. 起核心服务 (app + mysql + redis)
docker compose --env-file .env.production up -d

# 4. 等待初始化完成 (首次启动 ~3-5 min, mysql 跑 baseline + 198 migrations + seed)
docker compose logs -f mysql | grep "ready for connections"

# 5. 健康检查
curl -fsS http://localhost:8080/api/actuator/health | jq
# 期望: {"status":"UP","components":{"db":"UP","redis":"UP",...}}

# 6. 默认登录
# admin / admin123 (首次登录后立即改密码)
```

## 3. 启用 nginx 反代 (可选)

```bash
# 默认不启动 nginx, 通过 profile 加载
docker compose --env-file .env.production --profile with-nginx up -d

# nginx 监听 80, 转发 /api/* → app:8080, 静态资源 → app /app/static
# HTTPS: 在 deploy/nginx/ssl/ 放 cert.pem 和 key.pem,
#        然后取消注释 deploy/nginx/nginx.conf 中的 HTTPS server block
```

## 4. 启用监控栈 (可选)

```bash
cd monitoring
docker compose --env-file ../.env.production up -d
# Prometheus → http://localhost:9090
# Grafana    → http://localhost:3001  (登录用 .env 中 GRAFANA_ADMIN_USER/PASSWORD)
# 4 个 dashboard 自动 provision (jvm / http / inspection / plugin)
```

## 5. 数据库初始化路径

部署栈支持两条**不互通**的初始化路径,需选一:

### 路径 A: Docker (默认, 自动)
首次 `docker compose up` 时, mysql 容器执行 `database/docker-init/00-bootstrap.sh`,
顺序: complete_schema_v2.sql → schema/V*.sql (语义版本) → migrations/V*.sql (日期版本) → init/*.sql (跳过 verify_*).

### 路径 B: 裸机 / 已有 MySQL 实例
```bash
DB_PASSWORD=your-pw bash database/scripts/init-all.sh
# 与路径 A 等价, 但脚本运行在 host 上, 用 mysql client 连本地或远端 MySQL.
```

⚠️ **不要混用** — 容器路径和裸机路径生成相同表结构, 互不兼容(不会合并)。

## 6. 升级部署

新版代码到来时:
```bash
git pull
# 重新构建 image 并滚动更新 (mysql/redis 不重启, 数据保留)
docker compose --env-file .env.production up -d --build app
# 数据库 migration 通过应用启动时 ContributionDispatcher 自动 upsert (大部分),
# 真有 schema 变更时, 手动 apply 新增的 V*.sql 文件 (memory 提及的写法).
```

## 7. 备份恢复

### 备份 (cron 推荐每日)
```bash
docker compose exec mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} \
  --single-transaction --quick student_management \
  > backup-$(date +%Y%m%d).sql
gzip backup-$(date +%Y%m%d).sql

# uploads 目录单独备份
docker compose run --rm -v $(pwd)/backups:/bak app \
  tar czf /bak/uploads-$(date +%Y%m%d).tar.gz /app/uploads
```

### 恢复
```bash
# 停 app (避免写入)
docker compose stop app

# 还原 SQL
gunzip -c backup-20260511.sql.gz | \
  docker compose exec -T mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} student_management

# 还原 uploads
docker compose run --rm -v $(pwd)/backups:/bak app \
  tar xzf /bak/uploads-20260511.tar.gz -C /

# 重启
docker compose start app
```

## 8. 监控指标

| 指标 | 来源 | 说明 |
|------|------|------|
| `jvm_memory_used_bytes` | actuator/prometheus | JVM 各 region 内存 |
| `http_server_requests_seconds` | actuator/prometheus | HTTP 端点延迟分位数 |
| `inspection_task_created_total` | 业务自定义 | 检查任务创建数 (Phase 8) |
| MySQL slow log | mysql 容器 | 长查询 |

## 9. 常见运维操作

```bash
# 查看 backend 日志
docker compose logs -f --tail=100 app

# 进 backend 容器
docker compose exec app sh

# 重启某服务
docker compose restart app

# 完全清空数据重来 (危险!)
docker compose down -v   # -v 会删 volumes (mysql + uploads)

# 查 mysql
docker compose exec mysql mysql -u app_user -p${DB_PASSWORD} student_management
```

## 10. 已知问题与限制

- `deploy.yml` GitHub Actions 部署 step 仍是 echo 占位, release 后只 push GHCR 镜像, 不真触发服务器拉取 (TODO P1-6)
- 备份脚本仅 Windows .bat (`scripts/backup-database.bat`) 与上述命令重叠但语法不同, Linux 用上方命令
- nginx HTTPS server block 默认注释, 启用前要放 cert.pem/key.pem 到 `deploy/nginx/ssl/`
- monitoring stack 与 app stack 网络隔离, prometheus 通过 `host.docker.internal:8080` 抓 — 8080 必须暴露到 host
- 所有 secrets (DB_PASSWORD/REDIS_PASSWORD/JWT_SECRET/GRAFANA_ADMIN_PASSWORD/MYSQL_ROOT_PASSWORD) 写在 `.env.production`, 该文件 `.gitignore` 已排, 切勿误入仓库

## 11. 故障排查 速查

| 现象 | 检查 |
|------|------|
| app 容器 unhealthy / Restart loop | `docker compose logs app` 找 Spring Boot 异常 |
| `Could not resolve placeholder 'jwt.secret-key'` | `.env.production` 没有 JWT_SECRET 或没用 `--env-file` |
| MySQL 启动后表全空 | 检查 `database/docker-init/00-bootstrap.sh` 是否被 mysql 容器读到 (`docker compose logs mysql | grep docker-init`) |
| 调 `/api/workflow/process-definitions` 500 | Flowable 没启用 — application-prod.yml 检查 是否 exclude Flowable |
| 调 `/api/workflow/*` 403 | DB 缺 workflow:* 权限 seed (V20260510_1__workflow_permissions_seed.sql 没 apply) |
