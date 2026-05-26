<template>
  <div class="pcs-root">
    <!-- Header: 统计 + 搜索 + 全部展开/折叠 -->
    <div class="pcs-header">
      <div class="pcs-stat">
        <span class="pcs-stat-num">{{ enabledCount }}</span>
        <span class="pcs-stat-divider">/</span>
        <span class="pcs-stat-total">{{ items.length }}</span>
        <span class="pcs-stat-label">题已开启整改</span>
      </div>
      <el-input
        v-model="search"
        size="small"
        placeholder="搜索题目..."
        clearable
        style="width: 240px"
      />
      <el-radio-group v-model="filter" size="small">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="enabled">已开启</el-radio-button>
        <el-radio-button label="disabled">未开启</el-radio-button>
      </el-radio-group>
    </div>

    <div class="pcs-tip">
      默认<b>不开启整改</b> — 检查员提交时, 关闭的题不会建整改单. 开启后可按本题量表自定义触发阈值与时限.
    </div>

    <div class="pcs-items-area">
    <div v-if="loading" class="pcs-empty">加载中...</div>
    <div v-else-if="items.length === 0" class="pcs-empty">
      本项目模板无评分题 (或未关联模板).
    </div>
    <div v-else-if="filteredItems.length === 0" class="pcs-empty">
      无匹配题目.
    </div>

    <!-- Item 列表 — 按 section 分组 -->
    <div v-for="(group, sectionName) in groupedItems" :key="sectionName" class="pcs-section">
      <div class="pcs-section-head">
        <span class="pcs-section-name">{{ sectionName }}</span>
        <span class="pcs-section-count">{{ group.length }} 题</span>
      </div>
      <div class="pcs-section-items">
        <div v-for="item in group" :key="item.itemId"
             :class="['pcs-item', { 'pcs-item--enabled': isEnabled(item), 'pcs-item--expanded': expandedItemId === item.itemId }]">
          <!-- Row: header (single line) -->
          <div class="pcs-item-row" @click="toggleExpand(item)">
            <span :class="['pcs-item-dot', isEnabled(item) ? 'pcs-item-dot--on' : 'pcs-item-dot--off']" />
            <span class="pcs-item-name">{{ item.itemName }}</span>
            <span class="pcs-item-mode">{{ modeLabel(item.scoringMode) }}</span>
            <span v-if="isEnabled(item) && ruleSummary(item)" class="pcs-item-summary">
              {{ ruleSummary(item) }}
            </span>
            <span class="pcs-item-actions" @click.stop>
              <el-switch :model-value="isEnabled(item)" @update:model-value="(v) => onToggle(item, Boolean(v))" size="small" />
            </span>
          </div>
          <!-- Expanded: 阈值编辑器 -->
          <div v-if="isEnabled(item) && expandedItemId === item.itemId" class="pcs-item-detail">
            <ModeRuleEditor
              :scoring-mode="item.scoringMode"
              :max-score="item.maxScore"
              :discrete-options="defaultDiscreteOptions(item.scoringMode)"
              :risk-levels="['L', 'M', 'H', 'VH']"
              :rule-json="item.overrideRuleJson || ''"
              :enabled-mode="true"
              @change="(json) => onSaveItemRule(item, json)"
            />
          </div>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import ModeRuleEditor from './ModeRuleEditor.vue'

interface Props { projectId: LongId | null }
const props = defineProps<Props>()

interface ProjectItem {
  itemId: string
  itemName: string
  sectionName: string
  scoringMode: string
  maxScore?: number
  overrideRuleJson?: string
}

const items = ref<ProjectItem[]>([])
const loading = ref(false)
const search = ref('')
const filter = ref<'all' | 'enabled' | 'disabled'>('all')
const expandedItemId = ref<string | null>(null)

const ALL_MODES = [
  { value: 'PASS_FAIL', label: '通过/不通过' },
  { value: 'RATING_SCALE', label: '星级评分' },
  { value: 'DIRECT', label: '直接打分' },
  { value: 'DEDUCTION', label: '扣分制' },
  { value: 'ADDITION', label: '加分制' },
  { value: 'CUMULATIVE', label: '累计计次' },
  { value: 'LEVEL', label: '等级评分' },
  { value: 'SCORE_TABLE', label: '评分标准表' },
  { value: 'TIERED_DEDUCTION', label: '分档扣分' },
  { value: 'WEIGHTED_MULTI', label: '多维加权' },
  { value: 'RISK_MATRIX', label: '风险矩阵' },
  { value: 'THRESHOLD', label: '阈值判定' },
  { value: 'FORMULA', label: '公式计算' },
]
function modeLabel(mode: string): string {
  return ALL_MODES.find(m => m.value === mode)?.label || mode || '未配置'
}

function defaultDiscreteOptions(mode: string): string[] {
  switch (mode) {
    case 'LEVEL':            return ['A', 'B', 'C', 'D']
    case 'SCORE_TABLE':      return ['优', '良', '中', '差']
    case 'TIERED_DEDUCTION': return ['一级', '二级', '三级']
    case 'THRESHOLD':        return ['正常', '偏低', '异常']
    default: return []
  }
}

function isEnabled(item: ProjectItem): boolean {
  return !!item.overrideRuleJson && item.overrideRuleJson.trim() !== ''
}

const enabledCount = computed(() => items.value.filter(isEnabled).length)

const filteredItems = computed(() => {
  let arr = items.value
  if (filter.value === 'enabled') arr = arr.filter(isEnabled)
  else if (filter.value === 'disabled') arr = arr.filter(i => !isEnabled(i))
  if (search.value.trim()) {
    const kw = search.value.trim().toLowerCase()
    arr = arr.filter(i =>
      i.itemName.toLowerCase().includes(kw) ||
      i.sectionName.toLowerCase().includes(kw)
    )
  }
  return arr
})

const groupedItems = computed(() => {
  const grouped: Record<string, ProjectItem[]> = {}
  for (const i of filteredItems.value) {
    const k = i.sectionName || '未命名分区'
    if (!grouped[k]) grouped[k] = []
    grouped[k].push(i)
  }
  return grouped
})

/** 解析规则 JSON 给出简短摘要 (一行显示在 enabled item 后) */
function ruleSummary(item: ProjectItem): string {
  if (!item.overrideRuleJson) return ''
  try {
    const r = JSON.parse(item.overrideRuleJson)
    if (r._pending) return '待配置'
    const SEV: Record<string, string> = { HIGH: '严重', MEDIUM: '中度', LOW: '轻微' }
    const days = r.deadlineOverrideDays ? ` · ${r.deadlineOverrideDays} 天` : ''
    // PASS_FAIL
    if (r.baseSeverityMap?.FAIL) return `不通过 → ${SEV[r.baseSeverityMap.FAIL] || r.baseSeverityMap.FAIL}${days}`
    // 离散
    if (r.baseSeverityMap) {
      const entries = Object.entries(r.baseSeverityMap)
      if (entries.length > 0) {
        return entries.map(([k, v]) => `${k}→${SEV[v as string] || v}`).join(', ') + days
      }
    }
    // 连续
    if (r.singleThreshold?.sevThreshold != null) {
      const sev = r.singleThreshold.sevThreshold
      const max = item.maxScore || 10
      let val: string
      if (['DEDUCTION', 'CUMULATIVE'].includes(item.scoringMode)) {
        val = `≥ ${(sev * max).toFixed(1)}`
      } else {
        val = `≤ ${(max * (1 - sev)).toFixed(1)}`
      }
      return `${val} → ${SEV[r.singleThreshold.triggerSeverity] || r.singleThreshold.triggerSeverity}${days}`
    }
    return ''
  } catch { return '' }
}

async function loadItems() {
  if (!props.projectId) return
  loading.value = true
  try {
    const data = await http.get<ProjectItem[]>(`/inspection/corrective/projects/${props.projectId}/template-items`)
    items.value = Array.isArray(data) ? data : []
  } catch (e: any) {
    ElMessage.error('加载题目失败: ' + (e?.message || ''))
  } finally {
    loading.value = false
  }
}

function toggleExpand(item: ProjectItem) {
  if (!isEnabled(item)) return  // 未开启不展开
  expandedItemId.value = expandedItemId.value === item.itemId ? null : item.itemId
}

async function onToggle(item: ProjectItem, on: boolean) {
  if (!props.projectId) return
  if (on) {
    try {
      await http.put(
        `/inspection/corrective/projects/${props.projectId}/item-overrides/${item.itemId}`,
        { _pending: true }
      )
      item.overrideRuleJson = JSON.stringify({ _pending: true })
      expandedItemId.value = item.itemId
      ElMessage.success('已开启此题整改, 请在下方配置阈值')
    } catch (e: any) {
      ElMessage.error('开启失败: ' + (e?.message || ''))
    }
  } else {
    try {
      await http.delete(`/inspection/corrective/projects/${props.projectId}/item-overrides/${item.itemId}`)
      item.overrideRuleJson = undefined
      if (expandedItemId.value === item.itemId) expandedItemId.value = null
      ElMessage.success('已关闭此题整改')
    } catch (e: any) {
      ElMessage.error('关闭失败: ' + (e?.message || ''))
    }
  }
}

async function onSaveItemRule(item: ProjectItem, json: string) {
  if (!props.projectId) return
  try {
    const body = JSON.parse(json)
    await http.put(
      `/inspection/corrective/projects/${props.projectId}/item-overrides/${item.itemId}`,
      body
    )
    item.overrideRuleJson = json
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e?.message || ''))
  }
}

watch(() => props.projectId, () => loadItems(), { immediate: true })
</script>

<style scoped>
.pcs-root { display: flex; flex-direction: column; gap: 12px; }

/* Header */
.pcs-header {
  display: flex; align-items: center; gap: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0e7ff 100%);
  border: 1px solid #c7d2fe;
  border-radius: 8px;
}
.pcs-stat { display: flex; align-items: baseline; gap: 4px; flex: 1; }
.pcs-stat-num { font-size: 22px; font-weight: 700; color: #1e40af; }
.pcs-stat-divider { color: #94a3b8; }
.pcs-stat-total { font-size: 16px; color: #64748b; }
.pcs-stat-label { font-size: 12px; color: #475569; margin-left: 8px; }

.pcs-tip {
  padding: 8px 14px;
  background: #fef3c7;
  border: 1px solid #fde68a;
  border-radius: 4px;
  font-size: 12px; color: #78350f; line-height: 1.5;
}
.pcs-tip b { color: #92400e; }

.pcs-empty {
  padding: 32px; text-align: center; color: #94a3b8; font-size: 13px;
  background: #fafafa; border-radius: 6px;
}

/* 防 filter 切换抖动: items 区域固定 min-height, 内容少时不收缩 */
.pcs-items-area {
  min-height: 600px;
  display: flex; flex-direction: column; gap: 12px;
}

/* Section group */
.pcs-section {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}
.pcs-section-head {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
}
.pcs-section-name { font-weight: 600; color: #1f2937; flex: 1; }
.pcs-section-count { color: #6b7280; font-size: 11px; }

.pcs-section-items { display: flex; flex-direction: column; }

/* Item row */
.pcs-item {
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.12s;
}
.pcs-item:last-child { border-bottom: none; }
.pcs-item--enabled { background: #f0fdf4; }
.pcs-item--enabled.pcs-item--expanded { background: #ecfdf5; }
.pcs-item:hover { background: #f9fafb; }
.pcs-item--enabled:hover { background: #dcfce7; }

.pcs-item-row {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px;
  cursor: default;
  min-height: 38px;
}
.pcs-item--enabled .pcs-item-row { cursor: pointer; }
.pcs-item-dot {
  width: 7px; height: 7px; border-radius: 50%;
  flex-shrink: 0;
}
.pcs-item-dot--on { background: #16a34a; box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.15); }
.pcs-item-dot--off { background: #cbd5e1; }
.pcs-item-name {
  font-size: 13px; color: #1f2937; font-weight: 500;
  min-width: 100px;
}
.pcs-item-mode {
  font-size: 11px; color: #64748b;
  padding: 1px 8px;
  background: #f1f5f9; border-radius: 3px;
}
.pcs-item-summary {
  flex: 1;
  font-size: 12px; color: #166534;
  font-family: ui-monospace, monospace;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.pcs-item-actions { margin-left: auto; flex-shrink: 0; }

.pcs-item-detail {
  padding: 12px 14px 14px 32px;
  background: #fff;
  border-top: 1px dashed #d1d5db;
}
</style>
