#!/usr/bin/env bash
# type-check 基线门禁 — 允许既有错误, 禁止新增.
#
# 工作机制:
#   - 运行 vue-tsc --noEmit 统计 error TS 行数
#   - 和 .typecheck-baseline 里记录的上限比较
#   - 错误数 > 上限 → fail (有人引入了新 TS 错误)
#   - 错误数 < 上限 → warn (该收紧基线) + pass
#   - 错误数 == 上限 → pass (无变化)
#
# 如何降基线:
#   1. 本地修好几个错误, npm run type-check 确认减少了 N 个
#   2. 更新 frontend/.typecheck-baseline 里的数字 (旧值 - N)
#   3. commit 时 CI 会 pass (当前 <= 新基线)
#
# 目标: 基线 0 (对应 A+ 目标).
set -euo pipefail

cd "$(dirname "$0")/.."

BASELINE_FILE=".typecheck-baseline"
if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "[type-check-ceiling] $BASELINE_FILE not found"
  exit 2
fi
CEILING=$(cat "$BASELINE_FILE")

# 运行 vue-tsc, 忽略本命令 grep 的 pipefail 失败
set +e
OUT=$(npx vue-tsc --noEmit 2>&1)
STATUS=$?
set -e

ERRORS=$(echo "$OUT" | grep -cE "error TS" || true)

echo "[type-check-ceiling] errors: $ERRORS / ceiling: $CEILING"

if [[ "$ERRORS" -gt "$CEILING" ]]; then
  echo ""
  echo "❌ REGRESSION: 新引入的 TS 错误超过基线 $CEILING"
  echo "   修复新增错误, 或若是合理新错误, 考虑收紧基线后再 push."
  echo ""
  echo "$OUT" | grep -E "error TS" | head -30
  exit 1
fi

if [[ "$ERRORS" -lt "$CEILING" ]]; then
  echo ""
  echo "✅ IMPROVED: 错误数少于基线 ($ERRORS < $CEILING)"
  echo "   考虑把 $BASELINE_FILE 里的上限从 $CEILING 改为 $ERRORS, 避免回退."
fi

exit 0
