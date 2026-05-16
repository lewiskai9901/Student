#!/usr/bin/env bash
# ESLint error 基线门禁 — 不允许新增 lint error.
#
# 工作机制:
#   - 跑 eslint, 数 error 数 (不算 warning)
#   - 和 .lint-baseline 比较
#   - error 数 > baseline → fail (有人引入新 lint 错误)
#   - error 数 < baseline → warn (该收紧基线) + pass
#   - error 数 == baseline → pass
#
# 2026-05-16 起 baseline 0 — 历史 9 个 lint error 已根因修复, 之后任何 error 都阻断.
#
# 如何降基线 (理论上):
#   1. 修一批 error 后, 用 `npm run lint:check` 看新 error 数
#   2. 改 .lint-baseline 里的数字
#   3. commit 时 CI 会 pass

set -euo pipefail

cd "$(dirname "$0")/.."

BASELINE_FILE=".lint-baseline"
if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "[lint-ceiling] $BASELINE_FILE not found"
  exit 2
fi
CEILING=$(cat "$BASELINE_FILE")

# 用 --no-fix 跑, 不自动改文件 (避免污染工作树)
# eslint 报告里每行一个 problem, "error" 关键字在 severity 列
set +e
OUT=$(npx eslint . --ext .vue,.js,.jsx,.cjs,.mjs,.ts,.tsx,.cts,.mts --no-fix 2>&1)
STATUS=$?
set -e

# 数最后一行 "✖ N problems (X errors, Y warnings)" 里的 X
ERRORS=$(echo "$OUT" | grep -oE "[0-9]+ error" | head -1 | grep -oE "[0-9]+" || echo "0")
ERRORS=${ERRORS:-0}

echo "[lint-ceiling] errors: $ERRORS / ceiling: $CEILING"

if [[ "$ERRORS" -gt "$CEILING" ]]; then
  echo ""
  echo "❌ REGRESSION: 新引入的 lint error 超过基线 $CEILING"
  echo ""
  echo "$OUT" | grep -E "  error " | head -20
  exit 1
fi

if [[ "$ERRORS" -lt "$CEILING" ]]; then
  echo ""
  echo "✅ IMPROVED: error 数少于基线 ($ERRORS < $CEILING)"
  echo "   考虑把 $BASELINE_FILE 里的上限从 $CEILING 改为 $ERRORS."
fi

exit 0
