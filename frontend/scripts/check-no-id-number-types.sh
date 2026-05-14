#!/usr/bin/env bash
# 守护脚本: types/ 里禁止 `(xxxId|id): number` — 应改用 LongId.
#
# 工作机制:
#   - grep 所有 src/types/**/*.ts 里形如 `id: number` / `xxxId: number` 的字段
#   - 排除真 number 字段 (count/score/total/pageNum/expiresIn etc.) 的白名单
#   - 实际匹配数 > 基线 → fail (有人引入了新的 id:number)
#   - 实际匹配数 < 基线 → warn (该收紧)
#   - 等于 → pass
#
# 基线: frontend/.id-number-baseline (一行整数)
# 目标基线: 0 (Phase 2 跑完后)
set -euo pipefail

cd "$(dirname "$0")/.."

BASELINE_FILE=".id-number-baseline"
if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "[id-number-check] $BASELINE_FILE not found (默认基线 0)"
  CEILING=0
else
  CEILING=$(cat "$BASELINE_FILE")
fi

# ID-like 字段名 (后端 Long, 该是 LongId 而不是 number).
# 注: count/score/total/weight 等真 number 字段不在列内.
PATTERN='\b(id|userId|projectId|templateId|taskId|orgUnitId|tenantId|roleId|classId|placeId|sectionId|itemId|caseId|submissionId|submissionDetailId|inspectorId|reviewerId|targetId|recordId|assigneeId|appealId|parentId|messageId|alertId|jobId|eventId|warningId|attachmentId|evidenceId|profileId|libraryItemId|gradeSchemeId|responseSetId|categoryId|fileId|positionId|departmentId|gradeId|majorId|cohortId|courseId|moduleId|holidayId|planId|policyId|customScopeId|notificationId|subscriptionId|relationId|warningTypeId|userTypeId|caseSubjectId|fromId|toId|sourceId|destId|ownerId|createdById|updatedById|deletedById|tagId|labelId|groupId|dictionaryId|configId|menuId|permissionId|stationId|sensorId|deviceId|nodeId|edgeId|pluginId|extensionId|chainId|stepId|nodeId|workflowId|processId|formId|instanceId)\?\s*:\s*number\b'

set +e
HITS=$(grep -rEn "$PATTERN" src/types/ 2>/dev/null | grep -v "^Binary" | wc -l | tr -d ' ')
set -e

echo "[id-number-check] 实际 = $HITS, 基线 = $CEILING"

if (( HITS > CEILING )); then
  echo ""
  echo "❌ 新增 'xxxId: number' 字段 ($HITS > $CEILING):"
  grep -rEn "$PATTERN" src/types/ 2>/dev/null | head -30
  echo ""
  echo "💡 应改用 LongId — 后端 Long 字段实际是 string (Jackson 全局策略)."
  echo "   import type { LongId } from '@/types/common'"
  echo "   修完后更新 $BASELINE_FILE 基线."
  exit 1
fi

if (( HITS < CEILING )); then
  echo "ℹ️  改进了 $((CEILING - HITS)) 处, 该收紧基线: 把 $BASELINE_FILE 写为 $HITS"
fi

exit 0
