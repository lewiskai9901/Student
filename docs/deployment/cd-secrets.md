# CD 部署 secrets 配置 (区块3-4)

> `.github/workflows/deploy.yml` 的镜像 build/push 到 GHCR **已是真的**(用内置 `GITHUB_TOKEN`,开箱即用)。
> deploy-staging / deploy-production 已写成**真实 SSH 部署**(`docker compose pull && up -d`),但
> **默认 inert**:未配下列 secret 时优雅跳过(workflow warning,不失败)。配齐即自动启用真部署。

## 假设的部署形态
默认方案 = **SSH 到服务器 + docker compose**(贴合本仓 `docker-compose.yml`,服务名 `app`,部署路径
`/opt/student-management`)。若你用 k8s / watchtower / 其它,把 deploy.yml 里 `appleboy/ssh-action`
步替换成对应命令即可(其余 build/push/guard 不变)。

## 需要在 GitHub 配置的 secret
位置:repo **Settings → Environments → `staging` / `production`**(分环境配,production 建议挂
required reviewers 做发布审批)。

| 环境 | secret | 说明 | 必填 |
|---|---|---|---|
| staging | `STAGING_HOST` | 服务器地址 (配了才启用 staging 部署) | 启用即必填 |
| staging | `STAGING_USER` | SSH 用户名 | 是 |
| staging | `STAGING_SSH_KEY` | SSH 私钥 (PEM) | 是 |
| production | `PRODUCTION_HOST` | 生产服务器地址 (配了才启用 production 部署) | 启用即必填 |
| production | `PRODUCTION_USER` | SSH 用户名 | 是 |
| production | `PRODUCTION_SSH_KEY` | SSH 私钥 (PEM) | 是 |
| production | `PRODUCTION_HEALTH_URL` | 健康检查 URL (如 `https://prod/api/actuator/health`),返回含 `"status":"UP"` | 否(未配则跳过健康检查) |

> ⚠ 凭据由你在 GitHub UI 自行添加(我不接触私钥/密码)。服务器侧需预置 `/opt/student-management/docker-compose.yml`
> 并已 `docker login ghcr.io`(或 compose 用公开镜像)。

## 部署路径前置
- 服务器 `/opt/student-management/docker-compose.yml` 的 `app` 服务镜像应指向 GHCR
  (`ghcr.io/<owner>/<repo>:latest` 或具体 tag);CD 在 release 后 `pull` 最新再 `up -d`。
- 触发:GitHub **Release published**(自动 staging→production)或 **Actions→Deploy→workflow_dispatch**(手动选环境)。

## 当前状态
- ✅ 镜像 build + push GHCR — 真,无需配置。
- ✅ SSH 部署步 — 真,**待配上表 secret 激活**(未配=优雅跳过)。
- ✅ 健康检查 + 失败回滚 — 真(回滚为 best-effort,失败仍需人工核实生产)。
