<script setup lang="ts">
/**
 * 评级结果 Tab — Phase 5 评级引擎完美架构.
 *
 * 列出指定 indicator 下的全部评级结果 (含 DRAFT/PUBLISHED/SUPERSEDED 三态),
 * 支持发布 / 查看修订链 / 手动评估.
 */
import type { LongId } from '@/types/common'
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Send, History, Play } from 'lucide-vue-next'
import { getIndicators, indicatorResultApi } from '@/api/inspection/indicator'
import type {
  Indicator, IndicatorResult, ResultStatus,
} from '@/types/insp/indicator'
import { RESULT_STATUS_OPTIONS } from '@/types/insp/indicator'

const props = defineProps<{
  projectId: LongId
  initialIndicatorId?: LongId | null
}>()

// ============== State ==============
const loading = ref(false)
const indicators = ref<Indicator[]>([])
const results = ref<IndicatorResult[]>([])
const selectedIndicatorId = ref<LongId | null>(null)
const statusFilter = ref<ResultStatus | ''>('')

const leafIndicators = computed(() => indicators.value.filter(i => i.indicatorType === 'LEAF'))

const filteredResults = computed(() => {
  if (!statusFilter.value) return results.value
  return results.value.filter(r => r.status === statusFilter.value)
})

const statusCounts = computed(() => {
  const map: Record<ResultStatus, number> = { DRAFT: 0, PUBLISHED: 0, SUPERSEDED: 0 }
  for (const r of results.value) map[r.status]++
  return map
})

function statusTone(s: ResultStatus): string {
  return RESULT_STATUS_OPTIONS.find(o => o.value === s)?.tone || 'pending'
}
function statusLabel(s: ResultStatus): string {
  return RESULT_STATUS_OPTIONS.find(o => o.value === s)?.label || s
}

// ============== Load ==============
async function loadIndicators() {
  try {
    indicators.value = await getIndicators(props.projectId)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载指标失败')
  }
}

async function loadResults() {
  if (!selectedIndicatorId.value) {
    results.value = []
    return
  }
  loading.value = true
  try {
    results.value = await indicatorResultApi.list({
      indicatorId: selectedIndicatorId.value,
    })
    // 按 computedAt DESC + revision DESC 排, PUBLISHED 优先视觉权重
    results.value.sort((a, b) => {
      const t = (b.computedAt || '').localeCompare(a.computedAt || '')
      if (t !== 0) return t
      return (b.revision || 0) - (a.revision || 0)
    })
  } catch (e: any) {
    ElMessage.error(e?.message || '加载结果失败')
  } finally {
    loading.value = false
  }
}

watch(selectedIndicatorId, () => { loadResults() })

// ============== Actions ==============
const publishing = ref<LongId | null>(null)
async function handlePublish(r: IndicatorResult) {
  try {
    await ElMessageBox.confirm(
      `发布该结果 (target=${r.targetName || r.targetId}, period=${r.periodKey})？发布后此版本即为权威值, 后续重算会替代它.`,
      '确认发布', { type: 'warning' },
    )
    publishing.value = r.id
    await indicatorResultApi.publish(r.id)
    ElMessage.success('已发布')
    await loadResults()
  } catch (e: any) {
    if (e === 'cancel' || e?.toString?.() === 'cancel') return
    ElMessage.error(e?.message || '发布失败')
  } finally {
    publishing.value = null
  }
}

// ============== 修订链 Dialog ==============
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyAnchor = ref<IndicatorResult | null>(null)
const historyList = ref<IndicatorResult[]>([])

async function openHistory(r: IndicatorResult) {
  historyAnchor.value = r
  historyVisible.value = true
  historyLoading.value = true
  historyList.value = []
  try {
    const chain = await indicatorResultApi.history(r.id)
    // 时间倒序: 最新在上
    historyList.value = chain.slice().sort((a, b) =>
      (b.computedAt || '').localeCompare(a.computedAt || ''),
    )
  } catch (e: any) {
    ElMessage.error(e?.message || '加载修订链失败')
  } finally {
    historyLoading.value = false
  }
}

// ============== 手动评估 Dialog ==============
const manualVisible = ref(false)
const manualSaving = ref(false)
const manualForm = ref<{
  indicatorId: LongId | null
  startDate: string
  endDate: string
}>({
  indicatorId: null,
  startDate: '',
  endDate: '',
})

// 只有 MANUAL trigger 的 indicator 才适合手动评估; 其他模式也允许 (强制重算)
const manualIndicatorOptions = computed(() => leafIndicators.value)

function openManual() {
  manualForm.value = {
    indicatorId: selectedIndicatorId.value,
    startDate: '',
    endDate: '',
  }
  manualVisible.value = true
}

async function handleManualEvaluate() {
  const f = manualForm.value
  if (!f.indicatorId) { ElMessage.warning('请选择指标'); return }
  if (!f.startDate || !f.endDate) { ElMessage.warning('请选择日期范围'); return }
  if (f.endDate < f.startDate) { ElMessage.warning('结束日期不能早于开始日期'); return }
  manualSaving.value = true
  try {
    const drafts = await indicatorResultApi.manualEvaluate({
      indicatorId: f.indicatorId,
      startDate: f.startDate,
      endDate: f.endDate,
    })
    ElMessage.success(`已生成 ${drafts.length} 条 DRAFT 结果, 请审核后发布`)
    manualVisible.value = false
    // 自动切换到刚评估的指标
    selectedIndicatorId.value = f.indicatorId
    await loadResults()
  } catch (e: any) {
    ElMessage.error(e?.message || '手动评估失败')
  } finally {
    manualSaving.value = false
  }
}

// ============== Format helpers ==============
function fmtNum(v: number | null | undefined, digits = 2): string {
  if (v == null || Number.isNaN(v)) return '—'
  return Number(v).toFixed(digits)
}
function fmtRank(r: IndicatorResult): string {
  if (r.rankPosition == null) return '—'
  return r.totalRanked ? `${r.rankPosition}/${r.totalRanked}` : `${r.rankPosition}`
}
function fmtDate(s: string | null | undefined): string {
  if (!s) return '—'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

// ============== Init ==============
onMounted(async () => {
  await loadIndicators()
  if (props.initialIndicatorId) {
    selectedIndicatorId.value = props.initialIndicatorId
  } else if (leafIndicators.value.length > 0) {
    selectedIndicatorId.value = leafIndicators.value[0].id
  }
})

watch(() => props.initialIndicatorId, (v) => {
  if (v) selectedIndicatorId.value = v
})
</script>

<template>
  <div class="evr">
    <!-- Filter bar -->
    <div class="evr-filter">
      <div class="evr-filter-grp">
        <label class="evr-flt-lbl">评级指标</label>
        <el-select v-model="selectedIndicatorId" filterable size="small"
                   placeholder="选择指标" style="width: 240px">
          <el-option v-for="ind in leafIndicators" :key="ind.id" :label="ind.name" :value="ind.id" />
        </el-select>
      </div>

      <div class="evr-status-tabs">
        <button class="evr-st-tab" :class="{ on: statusFilter === '' }" @click="statusFilter = ''">
          全部 <span class="evr-st-cnt">{{ results.length }}</span>
        </button>
        <button class="evr-st-tab" :class="{ on: statusFilter === 'PUBLISHED' }" @click="statusFilter = 'PUBLISHED'">
          已发布 <span class="evr-st-cnt">{{ statusCounts.PUBLISHED }}</span>
        </button>
        <button class="evr-st-tab" :class="{ on: statusFilter === 'DRAFT' }" @click="statusFilter = 'DRAFT'">
          草稿 <span class="evr-st-cnt">{{ statusCounts.DRAFT }}</span>
        </button>
        <button class="evr-st-tab" :class="{ on: statusFilter === 'SUPERSEDED' }" @click="statusFilter = 'SUPERSEDED'">
          已替代 <span class="evr-st-cnt">{{ statusCounts.SUPERSEDED }}</span>
        </button>
      </div>

      <div class="evr-filter-ops">
        <el-button size="small" type="primary" @click="openManual" round>
          <Play class="w-3.5 h-3.5 mr-1" />手动评估
        </el-button>
      </div>
    </div>

    <!-- Body -->
    <div v-if="!selectedIndicatorId" class="evr-state evr-empty">
      请先选择一个评级指标
    </div>
    <div v-else-if="loading" class="evr-state">加载中...</div>
    <div v-else-if="filteredResults.length === 0" class="evr-state evr-empty">
      暂无评级结果 · 可点击"手动评估"生成
    </div>
    <div v-else class="evr-table-wrap">
      <table class="evr-table">
        <thead>
          <tr>
            <th>目标</th>
            <th>周期</th>
            <th class="evr-num-col">分数</th>
            <th class="evr-num-col">排名</th>
            <th>等级</th>
            <th>状态</th>
            <th>修订</th>
            <th>计算时间</th>
            <th class="evr-op-col">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in filteredResults" :key="r.id"
              :class="{ 'evr-row-published': r.status === 'PUBLISHED', 'evr-row-superseded': r.status === 'SUPERSEDED' }">
            <td>
              <div class="evr-target-name">{{ r.targetName || `#${r.targetId}` }}</div>
              <div v-if="r.targetType" class="evr-target-type">{{ r.targetType }}</div>
            </td>
            <td class="evr-mono">{{ r.periodKey }}</td>
            <td class="evr-num-col evr-mono">{{ fmtNum(r.value) }}</td>
            <td class="evr-num-col evr-mono">{{ fmtRank(r) }}</td>
            <td>
              <span v-if="r.gradeName" class="evr-grade-chip"
                    :style="{ background: r.gradeColor || '#e5e7eb' }">
                {{ r.gradeName }}
              </span>
              <span v-else class="evr-muted">—</span>
            </td>
            <td>
              <span class="evr-chip" :class="`evr-chip--${statusTone(r.status)}`">
                {{ statusLabel(r.status) }}
              </span>
            </td>
            <td class="evr-mono">v{{ r.revision }}</td>
            <td class="evr-mono evr-tiny">{{ fmtDate(r.computedAt) }}</td>
            <td class="evr-op-col">
              <div class="evr-row-ops">
                <el-button v-if="r.status === 'DRAFT'" size="small" type="primary"
                           :loading="publishing === r.id" @click="handlePublish(r)">
                  <Send class="w-3.5 h-3.5 mr-0.5" />发布
                </el-button>
                <el-button size="small" link type="primary" @click="openHistory(r)">
                  <History class="w-3.5 h-3.5 mr-0.5" />修订链
                </el-button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ===== 修订链 Dialog ===== -->
    <el-dialog v-model="historyVisible" width="560px"
               :title="historyAnchor ? `修订链: ${historyAnchor.targetName || '#'+historyAnchor.targetId} · ${historyAnchor.periodKey}` : '修订链'">
      <div v-if="historyLoading" class="evr-state">加载中...</div>
      <div v-else-if="historyList.length === 0" class="evr-state evr-empty">暂无版本</div>
      <div v-else class="evr-timeline">
        <div v-for="(h, i) in historyList" :key="h.id" class="evr-tl-item"
             :class="{ 'evr-tl-current': h.status === 'PUBLISHED' }">
          <div class="evr-tl-dot" :class="`evr-tl-dot--${statusTone(h.status)}`" />
          <div v-if="i < historyList.length - 1" class="evr-tl-line" />
          <div class="evr-tl-body">
            <div class="evr-tl-head">
              <span class="evr-tl-rev">v{{ h.revision }}</span>
              <span class="evr-chip" :class="`evr-chip--${statusTone(h.status)}`">{{ statusLabel(h.status) }}</span>
              <span class="evr-tl-time">{{ fmtDate(h.computedAt) }}</span>
            </div>
            <div class="evr-tl-stats">
              <span>分数 <b>{{ fmtNum(h.value) }}</b></span>
              <span class="evr-tl-sep">·</span>
              <span>排名 <b>{{ fmtRank(h) }}</b></span>
              <template v-if="h.gradeName">
                <span class="evr-tl-sep">·</span>
                <span class="evr-grade-chip" :style="{ background: h.gradeColor || '#e5e7eb' }">{{ h.gradeName }}</span>
              </template>
            </div>
            <div v-if="h.publishedAt" class="evr-tl-pub">发布于 {{ fmtDate(h.publishedAt) }}</div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- ===== 手动评估 Dialog ===== -->
    <el-dialog v-model="manualVisible" title="手动评估" width="480px">
      <div class="evc-form">
        <div class="evc-field">
          <label class="evc-lbl">评级指标 <span class="evc-req">*</span></label>
          <el-select v-model="manualForm.indicatorId" filterable size="default" style="width: 100%"
                     placeholder="选择指标">
            <el-option v-for="ind in manualIndicatorOptions" :key="ind.id" :label="ind.name" :value="ind.id" />
          </el-select>
          <div class="evc-hint">触发模式为「手动」的指标专属;
            其他模式的指标也可用此入口强制重算指定日期范围.
          </div>
        </div>
        <div class="evc-row2">
          <div class="evc-field">
            <label class="evc-lbl">开始日期 <span class="evc-req">*</span></label>
            <el-date-picker v-model="manualForm.startDate" type="date" value-format="YYYY-MM-DD"
                            placeholder="开始日期" style="width: 100%" />
          </div>
          <div class="evc-field">
            <label class="evc-lbl">结束日期 <span class="evc-req">*</span></label>
            <el-date-picker v-model="manualForm.endDate" type="date" value-format="YYYY-MM-DD"
                            placeholder="结束日期" style="width: 100%" />
          </div>
        </div>
        <div class="evr-tip">
          评估生成的结果为 DRAFT, 需在列表中手动「发布」才生效.
        </div>
      </div>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" :loading="manualSaving" @click="handleManualEvaluate">开始评估</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.evr { display: flex; flex-direction: column; gap: 10px; }

.evr-filter {
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
  padding: 8px 10px;
  background: var(--insp-bg-subtle, #fafbfc);
  border: 1px solid var(--insp-border-subtle, #e5e7eb);
  border-radius: 6px;
}
.evr-filter-grp { display: inline-flex; align-items: center; gap: 6px; }
.evr-flt-lbl {
  font-size: 12px; color: var(--insp-ink-tertiary, #6b7280);
}
.evr-filter-ops { margin-left: auto; }

.evr-status-tabs { display: inline-flex; align-items: center; gap: 4px; }
.evr-st-tab {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px; border-radius: 99px;
  background: transparent; border: 1px solid transparent;
  color: var(--insp-ink-tertiary, #6b7280);
  font-size: 12px; cursor: pointer;
  transition: all 0.15s;
}
.evr-st-tab:hover { background: var(--insp-bg-surface, #fff); }
.evr-st-tab.on {
  background: var(--insp-accent-paler, #eff6ff);
  border-color: var(--insp-accent, #1a6dff);
  color: var(--insp-accent, #1a6dff);
  font-weight: 600;
}
.evr-st-cnt { font-variant-numeric: tabular-nums; opacity: 0.7; }

.evr-state {
  padding: 36px 0; text-align: center;
  font-size: 12px; color: var(--insp-ink-quaternary, #9ca3af);
}
.evr-empty { padding: 48px 0; }

/* ===== Table ===== */
.evr-table-wrap {
  border: 1px solid var(--insp-border-subtle, #e5e7eb);
  border-radius: 6px; overflow: auto;
  background: var(--insp-bg-surface, #fff);
}
.evr-table {
  width: 100%; border-collapse: collapse;
  font-size: 12px;
}
.evr-table th {
  text-align: left; padding: 8px 12px;
  background: var(--insp-bg-subtle, #fafbfc);
  border-bottom: 1px solid var(--insp-border-subtle, #e5e7eb);
  color: var(--insp-ink-tertiary, #6b7280);
  font-weight: 500;
}
.evr-table td {
  padding: 8px 12px;
  border-bottom: 1px solid var(--insp-border-subtle, #f1f3f5);
  color: var(--insp-ink-primary, #111827);
  vertical-align: middle;
}
.evr-table tr:last-child td { border-bottom: 0; }
.evr-table tr.evr-row-published { background: rgba(16, 185, 129, 0.04); }
.evr-table tr.evr-row-superseded td { color: var(--insp-ink-quaternary, #9ca3af); }

.evr-num-col { text-align: right; }
.evr-op-col { width: 200px; }
.evr-mono { font-variant-numeric: tabular-nums; font-family: var(--insp-font-mono, monospace); }
.evr-tiny { font-size: 11px; color: var(--insp-ink-tertiary, #6b7280); }
.evr-muted { color: var(--insp-ink-quaternary, #9ca3af); }

.evr-target-name { font-weight: 500; }
.evr-target-type {
  font-size: 10px; color: var(--insp-ink-quaternary, #9ca3af);
  margin-top: 1px;
}

.evr-grade-chip {
  display: inline-block; padding: 1px 8px; border-radius: 99px;
  font-size: 10px; font-weight: 600; color: #fff;
}

.evr-chip {
  display: inline-block; padding: 1px 7px; border-radius: 3px;
  font-size: 10px; font-weight: 600;
}
.evr-chip--pass { background: #d1fae5; color: #065f46; }
.evr-chip--warn { background: #fef3c7; color: #92400e; }
.evr-chip--pending { background: #f3f4f6; color: #6b7280; }
.evr-chip--info { background: #dbeafe; color: #1e40af; }

.evr-row-ops { display: inline-flex; align-items: center; gap: 4px; }

/* ===== Timeline ===== */
.evr-timeline {
  display: flex; flex-direction: column; gap: 16px;
  padding: 12px 8px;
}
.evr-tl-item {
  position: relative;
  display: flex; align-items: flex-start; gap: 12px;
  padding-left: 24px;
}
.evr-tl-dot {
  position: absolute; left: 6px; top: 4px;
  width: 12px; height: 12px; border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px var(--insp-border-default, #d1d5db);
}
.evr-tl-dot--pass { background: #10b981; box-shadow: 0 0 0 2px #10b981; }
.evr-tl-dot--warn { background: #f59e0b; }
.evr-tl-dot--pending { background: #9ca3af; }
.evr-tl-line {
  position: absolute; left: 11px; top: 18px; bottom: -18px;
  width: 2px; background: var(--insp-border-subtle, #e5e7eb);
}
.evr-tl-body {
  flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px;
}
.evr-tl-head {
  display: inline-flex; align-items: center; gap: 8px;
}
.evr-tl-rev {
  font-family: var(--insp-font-mono, monospace);
  font-size: 12px; font-weight: 600;
  color: var(--insp-ink-primary, #111827);
}
.evr-tl-time {
  font-size: 11px; color: var(--insp-ink-tertiary, #6b7280);
  margin-left: auto;
}
.evr-tl-stats {
  display: inline-flex; align-items: center; flex-wrap: wrap; gap: 6px;
  font-size: 12px; color: var(--insp-ink-secondary, #374151);
}
.evr-tl-stats b { color: var(--insp-ink-primary, #111827); }
.evr-tl-sep { color: var(--insp-ink-quaternary, #d1d5db); }
.evr-tl-pub {
  font-size: 11px; color: var(--insp-pass, #10b981);
}

/* ===== Form (shared w/ EvaluationConfigView) ===== */
.evc-form { display: flex; flex-direction: column; gap: 14px; }
.evc-field { display: flex; flex-direction: column; gap: 5px; }
.evc-lbl {
  font-size: 12px; font-weight: 500;
  color: var(--insp-ink-secondary, #374151);
}
.evc-req { color: #ef4444; }
.evc-hint { font-size: 11px; color: var(--insp-ink-tertiary, #6b7280); }
.evc-row2 {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
}

.evr-tip {
  padding: 8px 10px; border-radius: 6px;
  background: var(--insp-accent-paler, #eff6ff);
  color: var(--insp-accent, #1a6dff);
  font-size: 11px;
}
</style>
