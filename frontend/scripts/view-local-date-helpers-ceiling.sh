#!/usr/bin/env bash
# view-local-date-helpers 基线门禁 (L4, 2026-05-19) —
# 防 view/component 内自己写 const formatDate / function formatDate / formatTime / formatDateTime.
#
# 工作机制:
#   - 统计 src/views/**.vue + src/components/**.vue 内
#     `^(const|function) format(Date|Time|DateTime)` 出现的文件数
#   - 与基线对比, 超过 → fail
#
# 反模式: View 内自己写 formatDate → 重复 + 格式不统一. 使用 @/utils/date 即可,
#         缺的格式 (MM/DD, zh-CN 等) 应该 add 到 utils/date.ts.
set -euo pipefail

cd "$(dirname "$0")/.."

BASELINE_FILE=".view-local-date-helpers-baseline"
if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "[view-local-date-helpers-ceiling] $BASELINE_FILE not found"
  exit 2
fi
CEILING=$(cat "$BASELINE_FILE")

# 文件级计数 (一个文件即使有 2 个 const formatDate 也只算 1)
COUNT=$(grep -rcE "^(const|function) format(Date|Time|DateTime)\b" \
  --include='*.vue' src/views src/components 2>/dev/null \
  | grep -v ':0$' | wc -l | tr -d ' ')

echo "[view-local-date-helpers-ceiling] files with local date formatter: $COUNT / ceiling: $CEILING"

if [[ "$COUNT" -gt "$CEILING" ]]; then
  echo ""
  echo "❌ REGRESSION: view/component 内本地 formatDate 文件数超过基线 $CEILING"
  echo "   新代码应该 import { formatDate / formatDateTime / formatMMDD / formatDateZh } from '@/utils/date'"
  echo "   若 utils/date.ts 缺需要的格式, 先加 helper + 单测, 再用."
  echo ""
  echo "当前违规文件:"
  grep -rlE "^(const|function) format(Date|Time|DateTime)\b" --include='*.vue' src/views src/components 2>/dev/null
  exit 1
fi

if [[ "$COUNT" -lt "$CEILING" ]]; then
  echo ""
  echo "✅ IMPROVED: $COUNT < $CEILING"
  echo "   考虑把 $BASELINE_FILE 里的上限改为 $COUNT, 避免回退."
fi

exit 0
