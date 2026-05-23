<script setup lang="ts">
/**
 * 评级配置 Tab — Phase 5 评级引擎完美架构.
 *
 * 列出项目的全部评级指标 (Indicator), 提供新建/编辑/删除入口.
 * 表单字段对齐后端 Phase 1-4 落地的 7 个新字段:
 *   sourceSectionIds[] / triggerMode / countThreshold / weightsBySection /
 *   rankDirection / missingPolicy(enum) / latePolicy / submissionDateField
 */
import type { LongId } from '@/types/common'
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2, GitBranch } from 'lucide-vue-next'
import {
  getIndicators, createLeafIndicator, updateIndicator, deleteIndicator,
} from '@/api/inspection/indicator'
import { getGradeSchemes } from '@/api/inspection/gradeScheme'
import type {
  Indicator, CreateLeafIndicatorRequest, UpdateIndicatorRequest,
  TriggerMode, RankDirection, MissingPolicyEnum, LatePolicy, SubmissionDateField,
} from '@/types/insp/indicator'
import {
  SOURCE_AGG_OPTIONS, EVAL_PERIOD_OPTIONS,
  TRIGGER_MODE_OPTIONS, RANK_DIRECTION_OPTIONS,
  MISSING_POLICY_ENUM_OPTIONS, LATE_POLICY_OPTIONS, SUBMISSION_DATE_FIELD_OPTIONS,
} from '@/types/insp/indicator'
import type { GradeScheme } from '@/types/insp/gradeScheme'

const props = defineProps<{
  projectId: LongId
  sections: Array<{ id: LongId; sectionName: string; targetType?: string }>
}>()

const emit = defineEmits<{
  'view-results': [indicatorId: LongId]
}>()

// ============== State ==============
const loading = ref(false)
const indicators = ref<Indicator[]>([])
const gradeSchemes = ref<GradeScheme[]>([])

// 仅显示 LEAF 指标 — composite 在分区配置页管理
const leafIndicators = computed(() => indicators.value.filter(i => i.indicatorType === 'LEAF'))

const sectionNameMap = computed(() => {
  const m = new Map<string, string>()
  for (const s of props.sections) m.set(String(s.id), s.sectionName)
  return m
})

function sectionsLabel(ids: LongId[] | null | undefined, fallbackId: LongId | null | undefined): string {
  const list = (ids && ids.length > 0) ? ids : (fallbackId != null ? [fallbackId] : [])
  if (list.length === 0) return '—'
  return list.map(id => sectionNameMap.value.get(String(id)) || `#${id}`).join(' / ')
}

function gradeSchemeName(id: LongId | null | undefined): string {
  if (id == null) return '—'
  const g = gradeSchemes.value.find(x => String(x.id) === String(id))
  return g?.displayName || `#${id}`
}

const TRIGGER_TONE: Record<TriggerMode, string> = {
  TIME_WINDOW: 'pass',
  COUNT: 'info',
  MANUAL: 'pending',
}
function triggerLabel(mode: TriggerMode | null | undefined): string {
  if (!mode) return '—'
  return TRIGGER_MODE_OPTIONS.find(o => o.value === mode)?.label || mode
}

// ============== Dialog ==============
const dialogVisible = ref(false)
const editingId = ref<LongId | null>(null)
const saving = ref(false)

interface FormShape {
  name: string
  sourceSectionIds: LongId[]
  sourceAggregation: string
  triggerMode: TriggerMode
  countThreshold: number
  weightsBySection: Record<string, number>
  rankDirection: RankDirection
  missingPolicy: MissingPolicyEnum
  latePolicy: LatePolicy
  submissionDateField: SubmissionDateField
  evaluationPeriod: string
  gradeSchemeId: LongId | null
}

function blankForm(): FormShape {
  return {
    name: '',
    sourceSectionIds: [],
    sourceAggregation: 'AVG',
    triggerMode: 'TIME_WINDOW',
    countThreshold: 5,
    weightsBySection: {},
    rankDirection: 'DESC',
    missingPolicy: 'IGNORE',
    latePolicy: 'REVISE_ORIGINAL',
    submissionDateField: 'TASK_DATE',
    evaluationPeriod: 'WEEKLY',
    gradeSchemeId: null,
  }
}

const form = ref<FormShape>(blankForm())

// 多分区时显示权重 — sourceSectionIds 长度 > 1
const showWeights = computed(() => form.value.sourceSectionIds.length > 1)
// 时间窗口模式才显示评估周期
const showPeriod = computed(() => form.value.triggerMode === 'TIME_WINDOW')
// 次数模式才显示 countThreshold
const showThreshold = computed(() => form.value.triggerMode === 'COUNT')

// 当 sourceSectionIds 变化, 清理 weightsBySection 里不在列表里的 key
watch(() => form.value.sourceSectionIds, (ids) => {
  const allowed = new Set(ids.map(String))
  const next: Record<string, number> = {}
  for (const k of Object.keys(form.value.weightsBySection)) {
    if (allowed.has(k)) next[k] = form.value.weightsBySection[k]
  }
  // 新加入的 key 默认权重 1 (用户可调)
  for (const id of ids) {
    if (!(String(id) in next)) next[String(id)] = 1
  }
  form.value.weightsBySection = next
})

function openCreate() {
  editingId.value = null
  form.value = blankForm()
  dialogVisible.value = true
}

function openEdit(ind: Indicator) {
  editingId.value = ind.id
  const ids = (ind.sourceSectionIds && ind.sourceSectionIds.length > 0)
    ? [...ind.sourceSectionIds]
    : (ind.sourceSectionId != null ? [ind.sourceSectionId] : [])
  form.value = {
    name: ind.name,
    sourceSectionIds: ids,
    sourceAggregation: ind.sourceAggregation || 'AVG',
    triggerMode: (ind.triggerMode || 'TIME_WINDOW') as TriggerMode,
    countThreshold: ind.countThreshold ?? 5,
    weightsBySection: ind.weightsBySection
      ? Object.fromEntries(Object.entries(ind.weightsBySection).map(([k, v]) => [k, Number(v)]))
      : {},
    rankDirection: (ind.rankDirection || 'DESC') as RankDirection,
    missingPolicy: (ind.missingPolicy as MissingPolicyEnum) || 'IGNORE',
    latePolicy: (ind.latePolicy || 'REVISE_ORIGINAL') as LatePolicy,
    submissionDateField: (ind.submissionDateField || 'TASK_DATE') as SubmissionDateField,
    evaluationPeriod: ind.evaluationPeriod || 'WEEKLY',
    gradeSchemeId: ind.gradeSchemeId ?? null,
  }
  dialogVisible.value = true
}

function validate(): string | null {
  const f = form.value
  if (!f.name.trim()) return '请输入指标名称'
  if (f.sourceSectionIds.length === 0) return '请至少选择一个数据来源分区'
  if (f.triggerMode === 'COUNT' && (!f.countThreshold || f.countThreshold < 1)) {
    return '次数触发模式下, 次数阈值需 ≥ 1'
  }
  return null
}

async function handleSave() {
  const err = validate()
  if (err) { ElMessage.warning(err); return }
  saving.value = true
  try {
    const f = form.value
    // 权重转 BigDecimal 兼容 (后端 Map<Long, BigDecimal>) — 这里送 number, Jackson 会解析
    const weightsBySection: Record<string, number> | undefined = showWeights.value
      ? f.weightsBySection
      : undefined
    if (editingId.value) {
      const payload: UpdateIndicatorRequest = {
        name: f.name.trim(),
        sourceSectionIds: f.sourceSectionIds,
        sourceAggregation: f.sourceAggregation,
        triggerMode: f.triggerMode,
        countThreshold: f.triggerMode === 'COUNT' ? f.countThreshold : undefined,
        weightsBySection,
        rankDirection: f.rankDirection,
        missingPolicy: f.missingPolicy,
        latePolicy: f.latePolicy,
        submissionDateField: f.submissionDateField,
        evaluationPeriod: showPeriod.value ? f.evaluationPeriod : undefined,
        gradeSchemeId: f.gradeSchemeId,
      }
      await updateIndicator(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      const payload: CreateLeafIndicatorRequest = {
        projectId: props.projectId,
        parentIndicatorId: null,
        name: f.name.trim(),
        sourceSectionIds: f.sourceSectionIds,
        sourceAggregation: f.sourceAggregation,
        triggerMode: f.triggerMode,
        countThreshold: f.triggerMode === 'COUNT' ? f.countThreshold : undefined,
        weightsBySection,
        rankDirection: f.rankDirection,
        missingPolicy: f.missingPolicy,
        latePolicy: f.latePolicy,
        submissionDateField: f.submissionDateField,
        // TIME_WINDOW 用配置周期, 非时间窗口模式 (COUNT/MANUAL) 服务端忽略, 此处给个占位避免类型必填
        evaluationPeriod: f.evaluationPeriod,
        gradeSchemeId: f.gradeSchemeId,
      }
      await createLeafIndicator(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await loadAll()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(ind: Indicator) {
  try {
    await ElMessageBox.confirm(`确定删除评级指标「${ind.name}」？`, '确认', { type: 'warning' })
    await deleteIndicator(ind.id)
    ElMessage.success('已删除')
    await loadAll()
  } catch (e: any) {
    if (e === 'cancel' || e?.toString?.() === 'cancel') return
    ElMessage.error(e?.message || '删除失败')
  }
}

// ============== Load ==============
async function loadAll() {
  loading.value = true
  try {
    const [inds, gs] = await Promise.all([
      getIndicators(props.projectId),
      getGradeSchemes(),
    ])
    indicators.value = inds
    gradeSchemes.value = gs
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.projectId, (v) => { if (v) loadAll() })
onMounted(() => { if (props.projectId) loadAll() })
</script>

<template>
  <div class="evc">
    <!-- Header -->
    <div class="evc-head">
      <div class="evc-head-left">
        <div class="evc-title">评级指标</div>
        <div class="evc-sub">
          按指定数据源 + 触发模式自动计算评级结果. 已配置
          <span class="evc-num">{{ leafIndicators.length }}</span> 个指标.
        </div>
      </div>
      <div class="evc-head-ops">
        <el-button size="small" type="primary" @click="openCreate" round>
          <Plus class="w-3.5 h-3.5 mr-1" />新建评级指标
        </el-button>
      </div>
    </div>

    <!-- List -->
    <div v-if="loading" class="evc-state">加载中...</div>
    <div v-else-if="leafIndicators.length === 0" class="evc-state evc-empty">
      暂无评级指标 · 点击右上角新建
    </div>
    <div v-else class="evc-list">
      <div v-for="ind in leafIndicators" :key="ind.id" class="evc-row">
        <div class="evc-row-main">
          <div class="evc-row-name">{{ ind.name }}</div>
          <div class="evc-row-meta">
            <span class="evc-chip" :class="`evc-chip--${TRIGGER_TONE[(ind.triggerMode || 'TIME_WINDOW') as TriggerMode]}`">
              {{ triggerLabel(ind.triggerMode as TriggerMode) }}
            </span>
            <span class="evc-meta-sep">·</span>
            <span class="evc-meta-label">来源</span>
            <span class="evc-meta-val">{{ sectionsLabel(ind.sourceSectionIds, ind.sourceSectionId) }}</span>
            <template v-if="ind.gradeSchemeId">
              <span class="evc-meta-sep">·</span>
              <span class="evc-meta-label">等级方案</span>
              <span class="evc-meta-val">{{ gradeSchemeName(ind.gradeSchemeId) }}</span>
            </template>
            <template v-if="ind.triggerMode === 'TIME_WINDOW' && ind.evaluationPeriod">
              <span class="evc-meta-sep">·</span>
              <span class="evc-meta-val">{{ EVAL_PERIOD_OPTIONS.find(o => o.value === ind.evaluationPeriod)?.label || ind.evaluationPeriod }}</span>
            </template>
            <template v-if="ind.triggerMode === 'COUNT' && ind.countThreshold">
              <span class="evc-meta-sep">·</span>
              <span class="evc-meta-val">{{ ind.countThreshold }} 次触发</span>
            </template>
          </div>
        </div>
        <div class="evc-row-ops">
          <el-button size="small" link type="primary" @click="emit('view-results', ind.id)">
            <GitBranch class="w-3.5 h-3.5 mr-0.5" />查看结果
          </el-button>
          <el-button size="small" link type="primary" @click="openEdit(ind)">
            <Pencil class="w-3.5 h-3.5 mr-0.5" />编辑
          </el-button>
          <el-button size="small" link type="danger" @click="handleDelete(ind)">
            <Trash2 class="w-3.5 h-3.5" />
          </el-button>
        </div>
      </div>
    </div>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑评级指标' : '新建评级指标'"
      width="640px" :close-on-click-modal="false">
      <div class="evc-form">
        <div class="evc-field">
          <label class="evc-lbl">指标名称 <span class="evc-req">*</span></label>
          <el-input v-model="form.name" placeholder="如: 班级综合评比 / 个人月度排名" size="default" />
        </div>

        <div class="evc-field">
          <label class="evc-lbl">数据来源分区 <span class="evc-req">*</span></label>
          <el-select v-model="form.sourceSectionIds" multiple filterable size="default"
                     placeholder="选择 1 个或多个分区" style="width: 100%">
            <el-option v-for="s in props.sections" :key="s.id" :label="s.sectionName" :value="s.id" />
          </el-select>
          <div class="evc-hint">跨分区时, 下方需为每个分区配置权重</div>
        </div>

        <!-- 权重 (多分区时显示) -->
        <div v-if="showWeights" class="evc-field">
          <label class="evc-lbl">分区权重</label>
          <div class="evc-weight-grid">
            <div v-for="id in form.sourceSectionIds" :key="String(id)" class="evc-weight-row">
              <span class="evc-weight-name">{{ sectionNameMap.get(String(id)) || `#${id}` }}</span>
              <el-input-number
                v-model="form.weightsBySection[String(id)]"
                :min="0" :step="0.1" :precision="2" size="small" style="width: 110px"
                placeholder="权重"
              />
            </div>
          </div>
        </div>

        <div class="evc-field">
          <label class="evc-lbl">触发模式</label>
          <el-radio-group v-model="form.triggerMode">
            <el-radio v-for="o in TRIGGER_MODE_OPTIONS" :key="o.value" :value="o.value">
              {{ o.label }}
            </el-radio>
          </el-radio-group>
          <div class="evc-hint">
            {{ TRIGGER_MODE_OPTIONS.find(o => o.value === form.triggerMode)?.description }}
          </div>
        </div>

        <!-- 评估周期 (TIME_WINDOW) -->
        <div v-if="showPeriod" class="evc-field">
          <label class="evc-lbl">评估周期</label>
          <el-radio-group v-model="form.evaluationPeriod">
            <el-radio v-for="o in EVAL_PERIOD_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </div>

        <!-- 次数阈值 (COUNT) -->
        <div v-if="showThreshold" class="evc-field">
          <label class="evc-lbl">次数阈值 <span class="evc-req">*</span></label>
          <el-input-number v-model="form.countThreshold" :min="1" :step="1" size="small" style="width: 140px" />
          <div class="evc-hint">同目标累计提交达到此次数时触发一次评估</div>
        </div>

        <div class="evc-row2">
          <div class="evc-field">
            <label class="evc-lbl">多次合并 (聚合方式)</label>
            <el-select v-model="form.sourceAggregation" size="default" style="width: 100%">
              <el-option v-for="o in SOURCE_AGG_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </div>
          <div class="evc-field">
            <label class="evc-lbl">排序方向</label>
            <el-select v-model="form.rankDirection" size="default" style="width: 100%">
              <el-option v-for="o in RANK_DIRECTION_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </div>
        </div>

        <div class="evc-row2">
          <div class="evc-field">
            <label class="evc-lbl">缺失数据策略</label>
            <el-select v-model="form.missingPolicy" size="default" style="width: 100%">
              <el-option v-for="o in MISSING_POLICY_ENUM_OPTIONS" :key="o.value" :label="o.label" :value="o.value">
                <div style="display:flex; flex-direction:column">
                  <span>{{ o.label }}</span>
                  <span style="font-size:11px; color:#9ca3af">{{ o.description }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
          <div class="evc-field">
            <label class="evc-lbl">迟到数据策略</label>
            <el-select v-model="form.latePolicy" size="default" style="width: 100%">
              <el-option v-for="o in LATE_POLICY_OPTIONS" :key="o.value" :label="o.label" :value="o.value">
                <div style="display:flex; flex-direction:column">
                  <span>{{ o.label }}</span>
                  <span style="font-size:11px; color:#9ca3af">{{ o.description }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
        </div>

        <div class="evc-row2">
          <div class="evc-field">
            <label class="evc-lbl">日期取值字段</label>
            <el-select v-model="form.submissionDateField" size="default" style="width: 100%">
              <el-option v-for="o in SUBMISSION_DATE_FIELD_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <div class="evc-hint">决定本期窗口按"任务日"还是"完成时间"切片</div>
          </div>
          <div class="evc-field">
            <label class="evc-lbl">等级方案 (可选)</label>
            <el-select v-model="form.gradeSchemeId" clearable filterable size="default"
                       placeholder="不绑定则只算分不评级" style="width: 100%">
              <el-option v-for="g in gradeSchemes" :key="g.id" :label="g.displayName" :value="g.id" />
            </el-select>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ editingId ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.evc { display: flex; flex-direction: column; gap: 12px; }

.evc-head {
  display: flex; align-items: flex-end; justify-content: space-between;
  padding-bottom: 4px;
}
.evc-head-left { display: flex; flex-direction: column; gap: 3px; }
.evc-title { font-size: 14px; font-weight: 600; color: var(--insp-ink-primary, #111827); }
.evc-sub { font-size: 12px; color: var(--insp-ink-tertiary, #6b7280); }
.evc-num { font-weight: 600; color: var(--insp-accent, #1a6dff); }

.evc-state {
  padding: 24px 0; text-align: center; font-size: 12px;
  color: var(--insp-ink-quaternary, #9ca3af);
}
.evc-empty { padding: 36px 0; }

.evc-list {
  display: flex; flex-direction: column;
  border: 1px solid var(--insp-border-subtle, #e5e7eb);
  border-radius: 6px; overflow: hidden;
  background: var(--insp-bg-surface, #fff);
}
.evc-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--insp-border-subtle, #f1f3f5);
  transition: background 0.15s;
}
.evc-row:last-child { border-bottom: 0; }
.evc-row:hover { background: var(--insp-bg-subtle, #fafbfc); }

.evc-row-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.evc-row-name {
  font-size: 13px; font-weight: 600;
  color: var(--insp-ink-primary, #111827);
}
.evc-row-meta {
  display: inline-flex; align-items: center; flex-wrap: wrap; gap: 6px;
  font-size: 11px; color: var(--insp-ink-tertiary, #6b7280);
}
.evc-meta-label { color: var(--insp-ink-quaternary, #9ca3af); }
.evc-meta-val { color: var(--insp-ink-secondary, #374151); }
.evc-meta-sep { color: var(--insp-ink-quaternary, #d1d5db); }

.evc-chip {
  padding: 1px 7px; border-radius: 3px; font-size: 10px; font-weight: 600;
}
.evc-chip--pass { background: #d1fae5; color: #065f46; }
.evc-chip--info { background: #dbeafe; color: #1e40af; }
.evc-chip--pending { background: #f3f4f6; color: #4b5563; }

.evc-row-ops {
  display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0;
}

/* ===== Form ===== */
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

.evc-weight-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  padding: 8px;
  background: var(--insp-bg-subtle, #fafbfc);
  border-radius: 6px;
}
.evc-weight-row {
  display: flex; align-items: center; justify-content: space-between;
  gap: 8px;
}
.evc-weight-name {
  font-size: 12px; color: var(--insp-ink-secondary, #374151);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
</style>
