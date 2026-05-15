#!/usr/bin/env bash
# CI 守护脚本: 比对当前 backend 生成的 openapi.json 与 git 里 commit 的版本.
# 如果有差异, 说明 backend 接口改了但开发者忘了 npm run openapi:export.
#
# 用法 (本地):
#   bash backend/scripts/check-openapi-drift.sh
# 用法 (CI):
#   CI=true bash backend/scripts/check-openapi-drift.sh
#
# 退出码: 0=同步 / 1=drift 检测到 / 2=backend 不可达

set -euo pipefail

BACKEND="${BACKEND_URL:-http://localhost:8080/api}"
COMMITTED="${COMMITTED:-backend/openapi.json}"
TMP="$(mktemp -t openapi-drift-XXX.json)"

echo "==> Generating current openapi from $BACKEND"

if ! curl -fsS "$BACKEND/actuator/health" >/dev/null 2>&1; then
  echo "ERROR: backend $BACKEND/actuator/health 不可达"
  exit 2
fi

curl -fsS "$BACKEND/v3/api-docs" | \
  python -c "import json, sys; d=json.load(sys.stdin); print(json.dumps(d, indent=2, ensure_ascii=False, sort_keys=True))" \
  > "$TMP"

echo "==> Comparing to $COMMITTED"

if diff -u "$COMMITTED" "$TMP" > /dev/null 2>&1; then
  echo "✓ OpenAPI spec 同步"
  rm "$TMP"
  exit 0
else
  echo "❌ DRIFT: backend 生成的 OpenAPI 与 $COMMITTED 不一致"
  echo ""
  echo "Backend 接口或 DTO 改了但 openapi.json 没更新. 修复方法:"
  echo "   bash backend/scripts/export-openapi.sh"
  echo "   git add $COMMITTED && git commit"
  echo ""
  echo "Diff (前 50 行):"
  diff -u "$COMMITTED" "$TMP" | head -50
  rm "$TMP"
  exit 1
fi
