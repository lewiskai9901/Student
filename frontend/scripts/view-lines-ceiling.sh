#!/usr/bin/env bash
# view-lines 基线门禁 (L5, 2026-05-19) — 防 mega-view 继续膨胀.
#
# 6 个 mega-view 已是技术债天花板, 不允许再涨; 每个 view 都有独立 baseline.
# 拆出子组件 / composable / style → 行数减 → 须同步更新 baseline.
#
# 守护逻辑:
#   - 任一 view 行数 > 它的 baseline → fail (有 PR 让它更胖)
#   - 全部 < baseline → warn + pass (该降基线)
#   - 相等 → pass
set -euo pipefail

cd "$(dirname "$0")/.."

# 格式: <baseline> <path>
declare -a TARGETS=(
  "1752 src/views/inspection/projects/ProjectDetailView.vue"
  "1727 src/views/inspection/tasks/TaskExecutionView.vue"
  "1708 src/views/place/components/FloorPlanEditor.vue"
  "1591 src/views/place/UniversalPlaceManagement.vue"
  "1399 src/views/inspection/projects/components/SectionConfigView.vue"
  "1171 src/views/inspection/config/InspectionConfigView.vue"
)

REGRESSED=0
IMPROVED=0
for entry in "${TARGETS[@]}"; do
  CEILING="${entry%% *}"
  FILE="${entry#* }"

  if [[ ! -f "$FILE" ]]; then
    echo "[view-lines-ceiling] WARN: $FILE not found (renamed?)"
    continue
  fi

  CUR=$(wc -l < "$FILE" | tr -d ' ')
  echo "[view-lines-ceiling] $FILE: $CUR / $CEILING"

  if [[ "$CUR" -gt "$CEILING" ]]; then
    echo "  ❌ REGRESSION: $((CUR - CEILING)) lines added since baseline"
    REGRESSED=1
  elif [[ "$CUR" -lt "$CEILING" ]]; then
    echo "  ✅ IMPROVED: $((CEILING - CUR)) lines smaller — 收紧基线为 $CUR"
    IMPROVED=1
  fi
done

if [[ "$REGRESSED" == 1 ]]; then
  echo ""
  echo "❌ 至少一个 mega-view 比基线大. 拆子组件 / composable / style 减回."
  exit 1
fi

if [[ "$IMPROVED" == 1 ]]; then
  echo ""
  echo "✅ 有 view 缩小 — 更新 scripts/view-lines-ceiling.sh 里的 baseline."
fi

exit 0
