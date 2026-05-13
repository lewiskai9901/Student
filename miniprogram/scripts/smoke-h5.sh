#!/usr/bin/env bash
# Tier 6 Block B-7 — 小程序 h5 端到后端的烟测脚本
#
# 用途:
#   - 一次性验证 backend 起来后, 小程序登录 + 拉任务的契约还能通
#   - 不真启 dev:h5 (那是长进程, CI 不友好)
#   - 调 backend-contract.test.ts vitest 套件 (真打 8080)
#
# 用法:
#   bash scripts/smoke-h5.sh
#   BACKEND_URL=http://staging.example.com/api bash scripts/smoke-h5.sh
#
# 退出码: 0=全过 / 1=backend down / 2=契约测失败

set -euo pipefail

BACKEND="${BACKEND_URL:-http://localhost:8080/api}"
echo "==> Tier 6 H5 smoke vs $BACKEND"

# 1. backend 健康
echo "==> [1/3] 检查 backend 健康"
if ! curl -fsS "$BACKEND/actuator/health" >/dev/null 2>&1; then
  echo "ERROR: backend $BACKEND/actuator/health 不可达"
  echo "请先启动 backend: cd backend && mvn spring-boot:run -DskipTests -Dspring-boot.run.profiles=dev"
  exit 1
fi
echo "  backend UP"

# 2. 验登录 + admin/admin123
echo "==> [2/3] 验证 /auth/login 工作"
TOK=$(curl -fsS -X POST "$BACKEND/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
if [ -z "$TOK" ]; then
  echo "ERROR: 无法获取 admin token"
  exit 2
fi
echo "  /auth/login 返回 token (前 20 字符: ${TOK:0:20}...)"

# 3. 跑契约 vitest (login + my-tasks + my-appeals + /auth/me)
echo "==> [3/3] 跑 backend-contract.test.ts"
BACKEND_URL="$BACKEND" npx vitest run src/__integration__/backend-contract.test.ts --reporter=basic

echo ""
echo "==> Smoke 通过. 手动验证 H5 UI 可执行:"
echo "   npm run dev:h5      # 起 http://localhost:5173/h5 (uni-app dev)"
echo "   # 浏览器开 → 登录 admin/admin123 → 看检查任务列表"
