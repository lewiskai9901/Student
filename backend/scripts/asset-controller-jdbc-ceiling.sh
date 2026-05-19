#!/usr/bin/env bash
# asset-controller-jdbc 基线门禁 (M1, 2026-05-20) —
# 防 interfaces/rest/asset/* 内 jdbc 直访继续涨.
#
# 当前 101 处反 DDD jdbc 调用 (L3 模式), 需要按域抽 ApplicationService.
# 由于工作量大 (~3-5 天), 用 baseline + ratchet 模式: 允许既有, 禁止新增,
# 后续 PR 抽一个 controller 就降 baseline 一截.
#
# 工作机制:
#   - 统计 src/main/java/.../interfaces/rest/asset/*.java 内
#     `jdbc.(query|update|execute|batchUpdate|queryFor*)` 调用总数
#   - 与 baseline 比, 超 → fail
set -euo pipefail

cd "$(dirname "$0")/.."

BASELINE_FILE=".asset-controller-jdbc-baseline"
if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "[asset-controller-jdbc-ceiling] $BASELINE_FILE not found"
  exit 2
fi
CEILING=$(cat "$BASELINE_FILE")

COUNT=$(grep -rcE "jdbc\.(query|update|execute|batchUpdate|queryFor)" \
  src/main/java/com/school/management/interfaces/rest/asset/ 2>/dev/null \
  | awk -F: '{s+=$2} END{print s+0}')

echo "[asset-controller-jdbc-ceiling] jdbc calls in interfaces/rest/asset: $COUNT / ceiling: $CEILING"

if [[ "$COUNT" -gt "$CEILING" ]]; then
  echo ""
  echo "❌ REGRESSION: 资产 Controller 内 jdbc 调用数超过基线 $CEILING"
  echo "   新代码应该走 application/asset/*ApplicationService, 不要在 Controller 直访 jdbc."
  echo "   参考 L3 模式 (application/event/EventConfigApplicationService)."
  echo ""
  echo "Per-file:"
  grep -rcE "jdbc\.(query|update|execute|batchUpdate|queryFor)" \
    src/main/java/com/school/management/interfaces/rest/asset/ 2>/dev/null \
    | awk -F: '$2>0' | sort -t: -k2 -rn
  exit 1
fi

if [[ "$COUNT" -lt "$CEILING" ]]; then
  echo ""
  echo "✅ IMPROVED: $COUNT < $CEILING"
  echo "   更新 $BASELINE_FILE 收紧为 $COUNT."
fi

exit 0
