#!/usr/bin/env bash
# as-any 基线门禁 (L5, 2026-05-19) — 防 `as any` 总数继续涨.
#
# `as any` 是绕 TS 类型检查的逃生口, 大量出现 = 类型系统失效, 通常是
# 复制粘贴 + 没找到正确类型. 不要在新代码引入; 旧代码慢慢清.
#
# 工作机制:
#   - 统计 src/**.{vue,ts} 内 `\bas any\b` 出现总次数 (-c 累计)
#   - 与 .as-any-baseline 对比, 超 → fail
set -euo pipefail

cd "$(dirname "$0")/.."

BASELINE_FILE=".as-any-baseline"
if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "[as-any-ceiling] $BASELINE_FILE not found"
  exit 2
fi
CEILING=$(cat "$BASELINE_FILE")

COUNT=$(grep -rcE "\bas any\b" --include='*.vue' --include='*.ts' src/ 2>/dev/null \
  | awk -F: '{s+=$2} END{print s+0}')

echo "[as-any-ceiling] as-any occurrences: $COUNT / ceiling: $CEILING"

if [[ "$COUNT" -gt "$CEILING" ]]; then
  echo ""
  echo "❌ REGRESSION: as any 总数超过基线 $CEILING"
  echo "   新代码请用 unknown / 正经类型 / as never 收口, 不用 as any."
  echo ""
  echo "Top 文件:"
  grep -rcE "\bas any\b" --include='*.vue' --include='*.ts' src/ 2>/dev/null \
    | awk -F: '$2>0' | sort -t: -k2 -rn | head -10
  exit 1
fi

if [[ "$COUNT" -lt "$CEILING" ]]; then
  echo ""
  echo "✅ IMPROVED: $COUNT < $CEILING"
  echo "   更新 $BASELINE_FILE 收紧为 $COUNT."
fi

exit 0
