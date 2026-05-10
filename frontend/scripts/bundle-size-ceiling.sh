#!/usr/bin/env bash
# bundle size 上限门禁 — 防 dist/ 体积失控.
#
# 工作机制:
#   - 统计 dist/static/js/*.js 总字节数 + 最大单块
#   - 和 .bundle-size-baseline.json 里的 ceiling 比较
#   - 任一超过 → fail
#   - 如新增功能合理超出, 更新 baseline 文件 (实际值 + 5% 余量)
#
# 使用:
#   npm run build             # 必须先 build
#   bash scripts/bundle-size-ceiling.sh
#   或一键: npm run build:check-size
set -euo pipefail

cd "$(dirname "$0")/.."

JS_DIR="dist/static/js"
if [[ ! -d "$JS_DIR" ]]; then
  echo "[bundle-size-ceiling] $JS_DIR 不存在, 先 npm run build"
  exit 2
fi

BASELINE_FILE=".bundle-size-baseline.json"
if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "[bundle-size-ceiling] $BASELINE_FILE not found"
  exit 2
fi

CEILING_TOTAL=$(node -p "require('./.bundle-size-baseline.json').totalBytes")
CEILING_LARGEST=$(node -p "require('./.bundle-size-baseline.json').largestChunkBytes")

# stat -c%s 在 Git Bash for Windows / Linux 都可用
ACTUAL_TOTAL=$(find "$JS_DIR" -name "*.js" -exec stat -c%s {} \; | awk '{s+=$1} END {print s+0}')
ACTUAL_LARGEST=$(find "$JS_DIR" -name "*.js" -exec stat -c%s {} \; | sort -nr | head -1)

# 兜底: 如 stat -c%s 无法工作, 切到 wc -c
if [[ -z "$ACTUAL_TOTAL" || "$ACTUAL_TOTAL" == "0" ]]; then
  ACTUAL_TOTAL=$(find "$JS_DIR" -name "*.js" -exec wc -c < {} \; | awk '{s+=$1} END {print s+0}')
  ACTUAL_LARGEST=$(find "$JS_DIR" -name "*.js" -exec wc -c < {} \; | sort -nr | head -1)
fi

echo "[bundle-size-ceiling] total JS:       $ACTUAL_TOTAL bytes (ceiling: $CEILING_TOTAL)"
echo "[bundle-size-ceiling] largest chunk:  $ACTUAL_LARGEST bytes (ceiling: $CEILING_LARGEST)"

FAIL=0

if [[ "$ACTUAL_TOTAL" -gt "$CEILING_TOTAL" ]]; then
  DIFF=$((ACTUAL_TOTAL - CEILING_TOTAL))
  echo ""
  echo "❌ 总 JS 超过上限 (+${DIFF} bytes)"
  echo "   分析: du -ah $JS_DIR/*.js | sort -rh | head -10"
  echo "   如确认合理, 更新 $BASELINE_FILE 里的 totalBytes (实际值 + 5%)"
  FAIL=1
fi

if [[ "$ACTUAL_LARGEST" -gt "$CEILING_LARGEST" ]]; then
  DIFF=$((ACTUAL_LARGEST - CEILING_LARGEST))
  echo ""
  echo "❌ 最大 chunk 超过上限 (+${DIFF} bytes)"
  echo "   单块膨胀通常意味着引入了大依赖或破坏了 manualChunks 切分"
  echo "   如确认合理, 更新 $BASELINE_FILE 里的 largestChunkBytes (实际值 + 5%)"
  FAIL=1
fi

if [[ "$FAIL" == "1" ]]; then
  exit 1
fi

# 提示收紧
SLACK_TOTAL=$((CEILING_TOTAL - ACTUAL_TOTAL))
if [[ "$SLACK_TOTAL" -gt $((CEILING_TOTAL / 10)) ]]; then
  echo ""
  echo "ℹ️  当前总 JS 距 ceiling 富余 ${SLACK_TOTAL} bytes (>10%), 可考虑收紧 baseline."
fi

echo "✅ Bundle size within ceiling"
exit 0
