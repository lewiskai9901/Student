#!/usr/bin/env bash
# 手写 API 导入基线门禁 — 防止 OpenAPI SDK 化倒退.
#
# 工作机制:
#   - 数 src/**/*.{ts,vue} 里 `from '@/api/xxx'` 的总数 (不含 @/api-generated)
#   - 和 .handwritten-api-baseline 比较
#   - 数量 > 基线 → fail (有人引入新的手写 API 调用)
#   - 数量 < 基线 → warn (该收紧基线) + pass
#   - 数量 == 基线 → pass
#
# 为什么这样: OpenAPI 自动同步 SDK 已就绪 (api-generated/sdk.gen.ts),
# 任何新代码应该用 SDK, 不再写新的 axios 调用. 既有手写 api 文件不强迫立刻迁,
# 但 ceiling 防止它们继续蔓延.
#
# 如何降基线:
#   1. 把某个 view 从 import { foo } from '@/api/bar' 改为
#      import { foo } from '@/api-generated/sdk.gen'
#   2. npm run handwritten-api-check 看新数量
#   3. 更新 .handwritten-api-baseline
#
# 目标基线 0 — 全部走生成的 SDK.
set -euo pipefail

cd "$(dirname "$0")/.."

BASELINE_FILE=".handwritten-api-baseline"
if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "[handwritten-api-ceiling] $BASELINE_FILE not found"
  exit 2
fi
CEILING=$(cat "$BASELINE_FILE")

# 数总数 (含 vue 单文件组件)
COUNT=$(grep -rEh "from '@/api/[^']+'" src --include='*.ts' --include='*.vue' 2>/dev/null \
        | grep -v "api-generated" \
        | wc -l \
        | tr -d ' ')

echo "[handwritten-api-ceiling] handwritten api imports: $COUNT / ceiling: $CEILING"

if [[ "$COUNT" -gt "$CEILING" ]]; then
  echo ""
  echo "❌ REGRESSION: 新增了手写 API 导入 ($COUNT > $CEILING)"
  echo "   新代码应该用 '@/api-generated/sdk.gen' 生成的 SDK 函数."
  echo "   参考: docs/development/openapi-sdk.md"
  echo ""
  echo "   最近被加 import 的文件:"
  grep -rEln "from '@/api/[^']+'" src --include='*.ts' --include='*.vue' 2>/dev/null \
    | grep -v "api-generated" \
    | xargs -I{} stat -c '%Y {}' {} 2>/dev/null \
    | sort -rn | head -5 | awk '{print "     " $2}'
  exit 1
fi

if [[ "$COUNT" -lt "$CEILING" ]]; then
  echo ""
  echo "✅ IMPROVED: 手写 API 导入数少于基线 ($COUNT < $CEILING)"
  echo "   建议把 $BASELINE_FILE 从 $CEILING 改为 $COUNT, 避免回退."
fi

exit 0
