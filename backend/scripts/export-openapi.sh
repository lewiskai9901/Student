#!/usr/bin/env bash
# 导出 OpenAPI spec 到 backend/openapi.json 入 git, 作为前端 SDK 生成的 source of truth.
#
# 用法:
#   bash backend/scripts/export-openapi.sh           # 从 localhost:8080 拉取
#   BACKEND_URL=http://staging.example.com/api  bash backend/scripts/export-openapi.sh
#
# 退出码: 0=成功 / 1=backend 不可达 / 2=spec 无效
#
# 用法约定: 后端改完接口后跑一次, openapi.json diff 入同一个 commit. CI 跑相同命令
# 比对 git 里的版本, 不一致就 fail.

set -euo pipefail

BACKEND="${BACKEND_URL:-http://localhost:8080/api}"
TARGET="${TARGET:-backend/openapi.json}"

echo "==> Exporting OpenAPI from $BACKEND/v3/api-docs"

if ! curl -fsS "$BACKEND/actuator/health" >/dev/null 2>&1; then
  echo "ERROR: backend $BACKEND/actuator/health 不可达"
  echo "请先启动 backend: cd backend && mvn spring-boot:run -DskipTests -Dspring-boot.run.profiles=dev"
  exit 1
fi

# 拉 + 排序键 (deterministic diff) + UTF-8
curl -fsS "$BACKEND/v3/api-docs" | \
  python -c "import json, sys; d=json.load(sys.stdin); print(json.dumps(d, indent=2, ensure_ascii=False, sort_keys=True))" \
  > "$TARGET.tmp"

# Validation: 必须有 paths 和 components.schemas
PATHS=$(python -c "import json; d=json.load(open('$TARGET.tmp', encoding='utf-8')); print(len(d.get('paths',{})))")
SCHEMAS=$(python -c "import json; d=json.load(open('$TARGET.tmp', encoding='utf-8')); print(len(d.get('components',{}).get('schemas',{})))")

if [ "$PATHS" -lt 100 ]; then
  echo "ERROR: spec 中只有 $PATHS paths, 疑似失败"
  rm "$TARGET.tmp"
  exit 2
fi

mv "$TARGET.tmp" "$TARGET"
echo "==> Wrote $TARGET ($PATHS paths / $SCHEMAS schemas)"
