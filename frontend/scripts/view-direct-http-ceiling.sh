#!/usr/bin/env bash
# view-direct-http 基线门禁 (L4, 2026-05-19) — 防 view/component 直 import @/utils/request.
#
# 工作机制:
#   - 统计 src/views/**.vue + src/components/**.vue 内 `from '@/utils/request'` 出现的文件数
#   - 和 .view-direct-http-baseline 里记录的上限比较
#   - 超过 → fail (有 PR 又新增 view 直连 http)
#   - 少于 → warn (该收紧基线)
#
# 反模式: View 直 import request → 业务逻辑漏到 view, 不可复用, 不可测试
#         应迁移到 src/api/ 模块, view 调 api 函数.
set -euo pipefail

cd "$(dirname "$0")/.."

BASELINE_FILE=".view-direct-http-baseline"
if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "[view-direct-http-ceiling] $BASELINE_FILE not found"
  exit 2
fi
CEILING=$(cat "$BASELINE_FILE")

COUNT=$(grep -rl "from '@/utils/request'" src/views src/components 2>/dev/null | wc -l | tr -d ' ')

echo "[view-direct-http-ceiling] view+component direct-http files: $COUNT / ceiling: $CEILING"

if [[ "$COUNT" -gt "$CEILING" ]]; then
  echo ""
  echo "❌ REGRESSION: view/component 直 import @/utils/request 数量超过基线 $CEILING"
  echo "   新代码应该把业务调用放到 src/api/ 模块, view 调 api 函数."
  echo ""
  echo "当前违规文件:"
  grep -rl "from '@/utils/request'" src/views src/components 2>/dev/null
  exit 1
fi

if [[ "$COUNT" -lt "$CEILING" ]]; then
  echo ""
  echo "✅ IMPROVED: $COUNT < $CEILING"
  echo "   考虑把 $BASELINE_FILE 里的上限改为 $COUNT, 避免回退."
fi

exit 0
