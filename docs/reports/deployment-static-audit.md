# 部署栈静态审计报告

**Repo**: `D:\学生管理系统` @ master `6d5b41a0`
**审计日期**: 2026-05-10
**审计员**: 静态代码审计 (无 Docker 运行时)
**总评**: **C 级 — 当前部署栈不能开箱即用,至少 4 个 P0 阻塞 + 多个 P1 安全/规范问题**

---

## 1. Surface-by-surface 表

### A. Dockerfile

| 检查项 | 状态 | 证据 |
|---|---|---|
| 多阶段 base 镜像版本 | ✅ | `maven:3.9-eclipse-temurin-17` / `node:18-alpine` / `eclipse-temurin:17-jre-alpine` (`Dockerfile:4,17,30`) |
| `mvn dependency:go-offline` 缓存 | ✅ | `Dockerfile:10` |
| `npm ci` 而非 `npm install` | ✅ | `Dockerfile:23` |
| 非 root 用户 1001 | ✅ | `Dockerfile:37-38,53` |
| HEALTHCHECK 端点鉴权 | ⚠️ | `wget /api/actuator/health` (`Dockerfile:60`); SecurityConfig 已放行 `/actuator/health` (`SecurityConfig.java:129`) — context-path `/api` 会被 Spring 剥离,**实际匹配 OK**,但配置者依赖隐含规则 |
| `SPRING_PROFILES_ACTIVE=prod` | ✅ | `Dockerfile:71` |
| `application-prod.yml` 存在 | ✅ | `backend/src/main/resources/application-prod.yml` |
| 挂载点 logs/uploads/config | ✅ | `Dockerfile:49` |
| 多余 port | ✅ | 仅 8080 |
| JVM `MaxRAMPercentage=75` | ✅ | `Dockerfile:64` (依赖 compose 设 mem_limit;**当前 compose 没设**) |

### B. `docker-compose.yml`

| 检查项 | 状态 | 证据 |
|---|---|---|
| depends_on healthcheck 链 | ✅ | `docker-compose.yml:23-27` |
| **MySQL 初始化挂载** | ❌ **P0** | `./database/init:/docker-entrypoint-initdb.d:ro` (`:54`) — MySQL 容器**只能跑 .sql,不会跑 init-all.sh**;且 `init/` 仅含 task/seed sql,**完全没有 schema/V*.sql + migrations/V*.sql,启动后只有 7 个增量 sql 跑,没有 baseline、没有 88 张 V2 表、没有 V3-V104 + V20260419+ migrations** — 容器起来后是**空库** |
| `init-all.sh` 引用 `init_data.sql` | ❌ **P0** | `database/scripts/init-all.sh:51` 引用 `database/init/init_data.sql`,但**该文件不存在**,即便手动跑 init-all.sh 也会在最后一步失败 |
| character-set utf8mb4_unicode_ci | ✅ | `:63` |
| Redis 密码必填 | ⚠️ | `${REDIS_PASSWORD}` 无默认 (`:69`),空值会让 redis 拒启 — 是好事,但 .env.production 提供了默认占位符 |
| nginx profile 默认不启动 | ✅ | `:98-99` `profiles: [with-nginx]` |
| **nginx volume 路径错误** | ❌ **P0** | `:91` 挂 `./deploy/nginx/nginx.conf` 但实际文件在 `./deploy/nginx.conf` (无 `nginx/` 子目录),且 `./deploy/nginx/ssl` 整个目录不存在 — `--profile with-nginx` 启动必失败 |
| networks 一致 | ✅ | `app-network` |
| **缺 mem/cpu limit** | ⚠️ P1 | Dockerfile JAVA_OPTS `MaxRAMPercentage=75` 在容器无 mem_limit 时取宿主机内存,生产可吃光宿主机 |
| monitoring 集成 | ⚠️ P2 | 主 compose 不含 prometheus/grafana,与 `monitoring/docker-compose.yml` 分离,跨网络无 service discovery |

### C. `monitoring/`

| 检查项 | 状态 | 证据 |
|---|---|---|
| prometheus scrape 跨网络 | ⚠️ P1 | `monitoring/docker-compose.yml:11` 用 `host.docker.internal:host-gateway` + `prometheus.yml:12` 抓 `host.docker.internal:8080` — 依赖 backend 端口暴露到 host,**当前 docker-compose.yml `:11` 暴露 8080 OK**,但生产若改 internal-only 会断 |
| metrics_path 自定义 | ✅ | `prometheus.yml:10` `/api/actuator/prometheus` (Spring context-path 是 `/api`) |
| grafana datasource UID | ❌ P1 | dashboard JSON 没有定义 `datasource.uid` 字段 (4 个 dashboard 全部),`provisioning/datasources/prometheus.yml` 也没设 `uid` — Grafana 10.4 import 后 panel 多半会报 "datasource not found" 需手动重选 |
| **grafana admin 密码硬编码** | ❌ P1 | `monitoring/docker-compose.yml:18-19` `admin/admin` — 任何能访问 3001 端口的人接管所有看板和数据源 |

### D. `deploy/nginx.conf`

| 检查项 | 状态 | 证据 |
|---|---|---|
| upstream 指向 | ❌ P1 | `:29` 写死 `127.0.0.1:8080` — 这只对**裸机/宝塔单机部署**对,docker-compose `with-nginx` 模式下应是 `http://app:8080` 容器名,所以即使修复 P0 #3 nginx 也连不上 backend |
| location 路由 | ✅ | `/api/` 代理 + 静态 fallback |
| WebSocket 头 | ❌ P1 | 无 `proxy_set_header Upgrade` / `Connection "upgrade"` — Flowable / SSE / 通知 push 走 WS 时挂 |
| gzip/cache header | ⚠️ | 静态资源有 `expires 7d` 但**无 gzip 配置** |
| HTTPS/SSL | ❌ P1 | 文件**只有 `listen 80`**,无 SSL block,但 docker-compose 又挂了 `./deploy/nginx/ssl:/etc/nginx/ssl` — 配置与挂载意图不一致 |
| CORS 双重 | ⚠️ P2 | 无 nginx 层 CORS,Spring 已配 — OK |

### E. `.env*`

| 检查项 | 状态 | 证据 |
|---|---|---|
| 字段覆盖 compose `${VAR}` | ❌ P0 | `docker-compose.yml` 引用 `DB_PASSWORD / MYSQL_ROOT_PASSWORD / REDIS_PASSWORD / JWT_SECRET`,但 `.env.production.example` **没有 `MYSQL_ROOT_PASSWORD`** (`:13-23`) — `docker compose up` 会因 mysql container 无 root 密码崩 |
| `frontend/.env.production` `VITE_API_BASE_URL` | ✅ | `=/api` 相对路径,反代后正确 |
| 敏感默认值 | ⚠️ P1 | `.env.production` (实际值) 含 `JWT_SECRET=B4S64/...` 真密钥 — `.gitignore:49` 已排除,但**已检入 commit `6d5b41a0`?** 需 `git ls-files .env.production` 校验 (静态审计时不能确认) |
| `.gitignore` 排除 `.env*` | ✅ | `.gitignore:46-50` |

### F. `.github/workflows/`

| 检查项 | 状态 | 证据 |
|---|---|---|
| `ci.yml` 全套 | ✅ | mvn test + spotbugs + checkstyle (warn) + npm lint + type-check:ceiling + vitest + build + Playwright E2E + Trivy |
| **CI E2E 不跑 init-all.sh** | ⚠️ P2 | `ci.yml:249-268` 自定义初始化路径 (schema_v2 + migrations + 5 个 init/*.sql + ci_e2e_seed.sql) — **与 init-all.sh 漂移**;真生产部署用什么尚未一致化 |
| `deploy.yml` trigger | ✅ | `release published` + `workflow_dispatch` |
| `deploy.yml` 实际部署命令 | ❌ P1 | `:106-110, :122-127` 全是 `echo` 占位符,**未真正部署任何东西**,只 build + push image。release tag 后**生产服务器啥也不会动** |
| `actions/upload-artifact@v3` | ⚠️ P2 | v3 已弃用 (2025-Q1 EOL),`ci.yml:80,144,193,323,331` + `api-docs.yml:84,90` 6 处需升 v4 |
| Java/Node 版本一致 | ✅ | 17/18 与 pom.xml/package.json 一致 |
| GHCR 推送 | ✅ | `deploy.yml:66-92` |

### G. `docs/deployment/`

| 检查项 | 状态 | 证据 |
|---|---|---|
| 部署步骤指南 | ⚠️ | 仅 `DEPLOYMENT_ISSUES.md` (问题集),**无完整部署 SOP**;另有 `deploy/DEPLOY-GUIDE.md` + `deploy/DEPLOY-BAOTA.md` 但都针对宝塔/裸机,无 docker-compose 路径文档 |
| `application-prod.yml` 配置说明 | ⚠️ | `DEPLOYMENT_ISSUES.md` 提了占位符问题但未系统化 |
| 备份恢复 | ⚠️ P2 | 仅 `scripts\backup-database.bat` (Windows),容器/Linux 环境无对应 |
| init-all.sh 与 docker-entrypoint-initdb.d 冲突说明 | ❌ P1 | 两条初始化路径**互不兼容**,无任何文档说明 docker 部署时应该用哪条 |

---

## 2. P0 列表 (阻塞,docker-compose up 起不来)

| # | Bug | 修复建议 |
|---|---|---|
| **P0-1** | `database/init/` 挂入 mysql `docker-entrypoint-initdb.d` 仅含 task/seed sql,**完全缺 baseline + 100+ migrations**,容器初始化后是空库 | 改 compose 为 init container 模式: 先跑 `mysql:8.0` 起库,再用 `app` 容器或独立 init job 执行 `database/scripts/init-all.sh`;或把 `complete_schema_v2.sql` + 全部 migrations 拼成单一 `00-schema.sql` 也挂入 initdb.d (但顺序需确认) |
| **P0-2** | `init-all.sh:51` 引用不存在的 `database/init/init_data.sql` — 即手动初始化也最后一步炸 | 要么补该文件,要么改脚本指向真实存在的 seed (例如 `task_*.sql` 组合) |
| **P0-3** | `docker-compose.yml:91-92` 挂 `./deploy/nginx/nginx.conf` 和 `./deploy/nginx/ssl/`,但**`deploy/nginx/` 目录不存在** (实际是 `deploy/nginx.conf` 单文件) | `mkdir deploy/nginx && mv deploy/nginx.conf deploy/nginx/`,或改 compose 路径为 `./deploy/nginx.conf` (ssl 部分单独建空目录) |
| **P0-4** | `.env.production.example` 缺 `MYSQL_ROOT_PASSWORD`,而 `docker-compose.yml:46` 强引用 — `compose up` 时 mysql 无 root 密码会拒启 (或随机生成致 app_user 创建失败) | example 加 `MYSQL_ROOT_PASSWORD=...`,与 `DB_PASSWORD` 区分 |

## 3. P1 列表 (能起,不安全/不规范)

- **P1-1**: nginx upstream 写死 `127.0.0.1:8080`,`with-nginx` 模式下应改 `http://app:8080`
- **P1-2**: nginx 无 SSL server block,但 compose 又挂了 ssl 目录 — 拿不到 https
- **P1-3**: nginx 无 WebSocket Upgrade 头转发 — Flowable WS / SSE 通知会断
- **P1-4**: Grafana admin/admin 硬编码,生产暴露 3001 等于裸奔
- **P1-5**: Grafana dashboard JSON 缺 `datasource.uid` — provisioning 后 panel 数据源未绑定
- **P1-6**: `deploy.yml` 部署 job 全是 echo 占位符,release 后无实际部署动作
- **P1-7**: `.env.production` 实际密钥已存在文件中 — 需 `git ls-files .env.production` 验证未误入仓库 (审计时无法运行 git 查询历史)
- **P1-8**: docker-compose 缺 `mem_limit / cpus` — JVM `MaxRAMPercentage=75` 取宿主机全部内存,易 OOM 宿主
- **P1-9**: docs 无 docker-compose 部署 SOP,init-all.sh 与 initdb.d 冲突无说明

## 4. P2 follow-up

- `actions/upload-artifact@v3` → v4 (CI 7 处)
- monitoring stack 与 app stack 网络分离,prometheus 走 host.docker.internal — 生产托管时要用 service discovery
- CI E2E 初始化路径与 init-all.sh 漂移,需统一为单一 source of truth
- nginx 配置无 gzip
- `backup-database.bat` 仅 Windows,缺 Linux/容器版

## 5. 建议修复顺序

1. **commit 1 (P0-1 + P0-2)**: 修 DB 初始化 — 补 `init_data.sql` 或拼出 `bootstrap.sql` 挂 `docker-entrypoint-initdb.d`,同时修 `init-all.sh` 路径
2. **commit 2 (P0-3 + P0-4)**: 修 compose 路径错配 + `.env.production.example` 补 `MYSQL_ROOT_PASSWORD`
3. **commit 3 (P1-1 ~ P1-3)**: 修 nginx (容器名 upstream + WS Upgrade 头);若需 SSL 加 server block,否则去掉 ssl 挂载
4. **commit 4 (P1-4 + P1-5)**: monitoring 安全 — Grafana admin 密码走 env,dashboard datasource UID 补齐
5. **commit 5 (P1-6)**: `deploy.yml` 接 SSH/k8s,移除 echo 占位
6. **commit 6 (P1-8 + P2)**: compose 加资源限制,actions/upload-artifact 升 v4
7. **commit 7 (docs)**: 补 docker-compose 部署 SOP,统一初始化路径文档

---

**审计员注**: P0-1 / P0-3 / P0-4 三个问题任意一个都会让首次 `docker compose up -d` 失败;P0-2 让 init-all.sh 路径同样失败。当前部署栈实质上未在 docker 路径下被验证过,文档中存在的两条初始化路径互相不兼容。建议在修复 P0 后跑一次完整 `docker compose down -v && up -d` 端到端冒烟验证。
