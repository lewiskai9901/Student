<template>
  <div class="task-banners">
    <!-- V110: 项目策略提示 banner -->
    <div
      v-if="policyStrictness"
      class="policy-banner"
      :class="`policy-${policyStrictness.toLowerCase()}`"
    >
      <span class="policy-icon">●</span>
      <span class="policy-text">
        本项目整改策略:
        <strong>{{ policyLabel }}</strong>
        ·
        {{ policyHint }}
      </span>
    </div>

    <!-- 上次同类问题提示 -->
    <div v-if="prevIssues.length" class="prev-issues-banner">
      <span class="prev-icon">!</span>
      <span class="prev-text">
        上次检查 ({{ prevIssuesDate }}) 在该目标存在 {{ prevIssues.length }} 个扣分项, 优先关注:
        <span v-for="(p, i) in prevIssues.slice(0, 3)" :key="i" class="prev-pill">
          {{ p.itemName }}
        </span>
      </span>
      <button class="prev-dismiss" @click="$emit('dismiss-prev-issues')">×</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface PrevIssue { itemName: string; itemCode: string; sectionName: string }

const props = defineProps<{
  policyStrictness: string | null
  prevIssues: PrevIssue[]
  prevIssuesDate: string
}>()

defineEmits<{
  (e: 'dismiss-prev-issues'): void
}>()

const policyLabel = computed(() => {
  switch (props.policyStrictness) {
    case 'STRICT':  return '严格 (自动建单)'
    case 'NORMAL':  return '标准 (引擎建议+确认)'
    case 'LENIENT': return '宽松 (仅严重)'
    case 'OFF':     return '关闭 (人工)'
    default:        return ''
  }
})

const policyHint = computed(() => {
  switch (props.policyStrictness) {
    case 'STRICT':  return '提交后任何不达标都会自动建立整改单'
    case 'NORMAL':  return '提交后会弹出整改候选确认对话框, 你可勾选/跳过'
    case 'LENIENT': return '只有 HIGH 级别问题会建议建单'
    case 'OFF':     return '不会自动判定, 整改单需手动建立'
    default:        return ''
  }
})
</script>

<style scoped>
/* V110 项目策略 banner */
.policy-banner {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 16px;
  border-bottom: 1px solid;
  font-size: 12px;
}
.policy-strict  { background: #fef2f2; color: #991b1b; border-color: #fecaca; }
.policy-normal  { background: #faf5ff; color: #6d28d9; border-color: #ddd6fe; }
.policy-lenient { background: #f0fdf4; color: #047857; border-color: #bbf7d0; }
.policy-off     { background: #f9fafb; color: #6b7280; border-color: #e5e7eb; }
.policy-icon { font-size: 8px; }
.policy-text strong { font-weight: 600; }

.prev-issues-banner {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 16px;
  background: linear-gradient(90deg, #fffbeb, #fef3c7);
  border-bottom: 1px solid #fcd34d;
  font-size: 13px; color: #92400e;
}
.prev-icon {
  font-size: 16px;
  font-weight: 700;
  width: 22px; height: 22px;
  display: inline-flex; align-items: center; justify-content: center;
  background: #f59e0b; color: #fff;
  border-radius: 50%;
  flex-shrink: 0;
}
.prev-text { flex: 1; line-height: 1.5; }
.prev-pill {
  display: inline-block;
  padding: 1px 8px;
  margin: 0 4px;
  background: #fff;
  border: 1px solid #fcd34d;
  border-radius: 3px;
  font-size: 12px;
  color: #92400e;
}
.prev-dismiss {
  background: none; border: none; cursor: pointer;
  font-size: 18px; color: #92400e;
  padding: 0 4px;
}
.prev-dismiss:hover { color: #b45309; }
</style>
