<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  getOptionProjects,
  getOptionSections,
  getOptionGradeBands,
  getOptionEventTypes,
} from '@/api/evalCenter'
import type {
  EvalCondition,
  EvalCampaign,
  ConditionSourceType,
  ConditionMetric,
  InspProject,
  InspSection,
  GradeBand,
  EventType,
} from '@/types/evalCenter'
import {
  INSPECTION_METRICS,
  EVENT_METRICS,
  HISTORY_METRICS,
  SOURCE_METRICS,
  ConditionSourceConfig,
  ConditionScopeConfig,
  OperatorLabels,
} from '@/types/evalCenter'

// ==================== Props / Emits ====================
const props = defineProps<{
  visible: boolean
  initial?: EvalCondition | null
  campaignTargetType?: string
  campaignLevels?: { levelNum: number; levelName: string }[]
  campaignList?: { id: number; campaignName: string }[]
}>()

const emit = defineEmits<{
  (e: 'save', condition: EvalCondition): void
  (e: 'close'): void
}>()

// ==================== Internal State ====================
const sourceType = ref<ConditionSourceType>('INSPECTION')

// INSPECTION params
const selectedProjectId = ref<number | null>(null)
const selectedSectionId = ref<number | null>(null)
const selectedProjectName = ref('')
const selectedSectionName = ref('')

// EVENT params
const selectedEventType = ref('')
const selectedEventTypeName = ref('')

// HISTORY params
const selectedCampaignId = ref<number | null>(null)
const selectedCampaignName = ref('')

// Common condition fields
const metric = ref<ConditionMetric>('SCORE_AVG')
const operator = ref('>=')
const thresholdNum = ref<number | ''>('')
const thresholdGrades = ref<string[]>([])  // for GRADE_EVERY (IN operator)
const thresholdLevelNum = ref<number | ''>('')  // for PREV_LEVEL
const scope = ref<'SELF' | 'MEMBERS' | 'SPECIFIC_ROLE'>('SELF')
const scopeRole = ref('')
const timeRange = ref<'CYCLE' | 'CUSTOM'>('CYCLE')
const timeRangeDays = ref<number | ''>('')

// Options data
const projects = ref<InspProject[]>([])
const sections = ref<InspSection[]>([])
const gradeBands = ref<GradeBand[]>([])
const eventTypes = ref<EventType[]>([])

const optLoading = ref(false)

// ==================== Computed ====================
const currentMetrics = computed(() => {
  return SOURCE_METRICS[sourceType.value] ?? {}
})

const currentMetricConfig = computed(() => {
  const m = currentMetrics.value
  return m[metric.value] ?? null
})

const availableOperators = computed(() => {
  return currentMetricConfig.value?.supportedOperators ?? ['>=']
})

const thresholdType = computed(() => {
  return currentMetricConfig.value?.thresholdType ?? 'number'
})

// Natural language preview
const conditionPreview = computed(() => {
  return generateDescription()
})

// ==================== Watchers ====================
watch(() => props.visible, async (v) => {
  if (v) {
    await loadBaseOptions()
    if (props.initial) {
      fillFromCondition(props.initial)
    } else {
      resetForm()
    }
  }
})

watch(sourceType, () => {
  metric.value = getDefaultMetric()
  operator.value = availableOperators.value[0] ?? '>='
  thresholdNum.value = ''
  thresholdGrades.value = []
  thresholdLevelNum.value = ''
})

watch(metric, () => {
  const ops = availableOperators.value
  if (!ops.includes(operator.value)) {
    operator.value = ops[0] ?? '>='
  }
})

watch(selectedSectionId, async (id) => {
  gradeBands.value = []
  if (id) {
    try {
      gradeBands.value = await getOptionGradeBands(id)
    } catch { /* ignore */ }
  }
})

watch(selectedProjectId, async (id) => {
  sections.value = []
  selectedSectionId.value = null
  selectedSectionName.value = ''
  try {
    sections.value = await getOptionSections(id ?? undefined)
  } catch { /* ignore */ }
})

// ==================== Load options ====================
async function loadBaseOptions() {
  optLoading.value = true
  try {
    const [projs, evts, sects] = await Promise.all([
      getOptionProjects().catch(() => [] as InspProject[]),
      getOptionEventTypes().catch(() => [] as EventType[]),
      getOptionSections().catch(() => [] as InspSection[]),
    ])
    projects.value = projs
    eventTypes.value = evts
    sections.value = sects
  } finally {
    optLoading.value = false
  }
}

// ==================== Fill / Reset ====================
function fillFromCondition(c: EvalCondition) {
  sourceType.value = c.sourceType
  metric.value = c.metric as ConditionMetric
  operator.value = c.operator
  scope.value = c.scope
  scopeRole.value = c.scopeRole ?? ''
  timeRange.value = c.timeRange
  timeRangeDays.value = c.timeRangeDays ?? ''

  // Parse threshold
  const thresh = c.threshold
  if (thresholdType.value === 'grade_list') {
    try {
      thresholdGrades.value = JSON.parse(thresh)
    } catch { thresholdGrades.value = [] }
  } else if (thresholdType.value === 'level_num') {
    thresholdLevelNum.value = Number(thresh) || ''
  } else {
    thresholdNum.value = Number(thresh) || ''
  }

  // Parse sourceConfig
  const sc = typeof c.sourceConfig === 'string'
    ? (() => { try { return JSON.parse(c.sourceConfig as string) } catch { return {} } })()
    : c.sourceConfig

  if (c.sourceType === 'INSPECTION') {
    selectedProjectId.value = (sc as any)?.projectId ?? null
    selectedProjectName.value = (sc as any)?.projectName ?? ''
    selectedSectionId.value = (sc as any)?.sectionId ?? null
    selectedSectionName.value = (sc as any)?.sectionName ?? ''
  } else if (c.sourceType === 'EVENT') {
    selectedEventType.value = (sc as any)?.eventType ?? ''
    selectedEventTypeName.value = (sc as any)?.eventTypeName ?? ''
  } else if (c.sourceType === 'HISTORY') {
    selectedCampaignId.value = (sc as any)?.campaignId ?? null
    selectedCampaignName.value = (sc as any)?.campaignName ?? ''
  }
}

function resetForm() {
  sourceType.value = 'INSPECTION'
  selectedProjectId.value = null
  selectedSectionId.value = null
  selectedProjectName.value = ''
  selectedSectionName.value = ''
  selectedEventType.value = ''
  selectedEventTypeName.value = ''
  selectedCampaignId.value = null
  selectedCampaignName.value = ''
  metric.value = 'SCORE_AVG'
  operator.value = '>='
  thresholdNum.value = ''
  thresholdGrades.value = []
  thresholdLevelNum.value = ''
  scope.value = 'SELF'
  scopeRole.value = ''
  timeRange.value = 'CYCLE'
  timeRangeDays.value = ''
}

function getDefaultMetric(): ConditionMetric {
  const keys = Object.keys(currentMetrics.value)
  return (keys[0] ?? 'SCORE_AVG') as ConditionMetric
}

// ==================== Description generation ====================
function generateDescription(): string {
  const metricLabel = currentMetricConfig.value?.label ?? metric.value
  const opLabel = OperatorLabels[operator.value] ?? operator.value
  let thresholdStr = ''

  if (thresholdType.value === 'grade_list') {
    thresholdStr = thresholdGrades.value.join('、') || '?'
  } else if (thresholdType.value === 'level_num') {
    const levelNum = Number(thresholdLevelNum.value)
    const levelName = props.campaignLevels?.find(l => l.levelNum === levelNum)?.levelName
    thresholdStr = levelName ? `${levelName}级（第${levelNum}级）` : (thresholdLevelNum.value !== '' ? String(thresholdLevelNum.value) : '?')
  } else if (thresholdType.value === 'percent') {
    thresholdStr = thresholdNum.value !== '' ? `前${thresholdNum.value}%` : '?%'
  } else {
    thresholdStr = thresholdNum.value !== '' ? String(thresholdNum.value) : '?'
  }

  let sourceDesc = ''
  if (sourceType.value === 'INSPECTION') {
    const sectionLabel = selectedSectionName.value || (selectedSectionId.value ? `分区${selectedSectionId.value}` : '全部分区')
    sourceDesc = sectionLabel
  } else if (sourceType.value === 'EVENT') {
    sourceDesc = selectedEventTypeName.value || selectedEventType.value || '事件'
  } else if (sourceType.value === 'HISTORY') {
    sourceDesc = selectedCampaignName.value || (selectedCampaignId.value ? `活动${selectedCampaignId.value}` : '本活动')
  }

  const scopeLabel = ConditionScopeConfig[scope.value]?.label ?? scope.value
  const timeLabel = timeRange.value === 'CYCLE' ? '评选周期内' : `最近${timeRangeDays.value || '?'}天`

  if (sourceType.value === 'INSPECTION') {
    if (metric.value === 'GRADE_EVERY') {
      return `【${scopeLabel}】${timeLabel}，${sourceDesc} 每次评级都达到 ${thresholdStr}`
    }
    return `【${scopeLabel}】${timeLabel}，${sourceDesc} ${metricLabel} ${opLabel} ${thresholdStr}`
  } else if (sourceType.value === 'EVENT') {
    return `【${scopeLabel}】${timeLabel}，${sourceDesc} ${metricLabel} ${opLabel} ${thresholdStr}`
  } else {
    if (metric.value === 'CONSECUTIVE') {
      return `连续 ${thresholdStr} 期达到指定级别`
    }
    return `${sourceDesc} ${metricLabel} ${opLabel} ${thresholdStr}`
  }
}

// ==================== Save ====================
function handleSave() {
  // Build sourceConfig
  let sc: any = {}
  if (sourceType.value === 'INSPECTION') {
    sc = { projectId: selectedProjectId.value, sectionId: selectedSectionId.value, sectionName: selectedSectionName.value }
  } else if (sourceType.value === 'EVENT') {
    sc = { eventType: selectedEventType.value, eventTypeName: selectedEventTypeName.value }
  } else {
    sc = { campaignId: selectedCampaignId.value, campaignName: selectedCampaignName.value }
  }

  // Build threshold
  let threshStr = ''
  if (thresholdType.value === 'grade_list') {
    threshStr = JSON.stringify(thresholdGrades.value)
  } else if (thresholdType.value === 'level_num') {
    threshStr = String(thresholdLevelNum.value)
  } else {
    threshStr = String(thresholdNum.value)
  }

  const condition: EvalCondition = {
    sourceType: sourceType.value,
    sourceConfig: JSON.stringify(sc),
    metric: metric.value,
    operator: operator.value,
    threshold: threshStr,
    scope: scope.value,
    scopeRole: scope.value === 'SPECIFIC_ROLE' ? scopeRole.value : null,
    timeRange: timeRange.value,
    timeRangeDays: timeRange.value === 'CUSTOM' ? Number(timeRangeDays.value) || null : null,
    description: generateDescription(),
  }

  emit('save', condition)
}

// ==================== Helpers ====================
function selectProject(p: InspProject | null) {
  selectedProjectId.value = p?.id ?? null
  selectedProjectName.value = p?.name ?? ''
}

function selectSection(s: InspSection | null) {
  selectedSectionId.value = s?.id ?? null
  selectedSectionName.value = s?.sectionName ?? ''
}

function selectEventType(et: EventType) {
  selectedEventType.value = et.typeCode
  selectedEventTypeName.value = et.typeName
}

function toggleGrade(code: string) {
  const idx = thresholdGrades.value.indexOf(code)
  if (idx >= 0) {
    thresholdGrades.value.splice(idx, 1)
  } else {
    thresholdGrades.value.push(code)
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="ce-mask" @click.self="emit('close')">
        <div class="ce-box">
          <!-- Header -->
          <div class="ce-head">
            <h3>{{ initial ? '编辑条件' : '添加条件' }}</h3>
            <button class="ce-close" @click="emit('close')">&times;</button>
          </div>

          <!-- Body -->
          <div class="ce-body">
            <!-- Step 1: Source type -->
            <div class="ce-section">
              <div class="ce-section-title">数据源</div>
              <div class="source-tabs">
                <button
                  v-for="(cfg, key) in ConditionSourceConfig"
                  :key="key"
                  class="source-tab"
                  :class="{ active: sourceType === key }"
                  @click="sourceType = key as ConditionSourceType"
                >
                  <span class="source-tab-label">{{ cfg.label }}</span>
                  <span class="source-tab-desc">{{ cfg.description }}</span>
                </button>
              </div>
            </div>

            <!-- Step 2: Source-specific params -->
            <div class="ce-section">
              <div class="ce-section-title">数据源参数</div>

              <!-- INSPECTION -->
              <template v-if="sourceType === 'INSPECTION'">
                <div class="param-row">
                  <label class="param-label">检查项目（可选）</label>
                  <select
                    class="param-select"
                    :value="selectedProjectId ?? ''"
                    @change="selectProject(projects.find(p => p.id === Number(($event.target as HTMLSelectElement).value)) ?? null)"
                  >
                    <option value="">全部项目</option>
                    <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.name }}</option>
                  </select>
                </div>
                <div class="param-row">
                  <label class="param-label">检查分区 <span class="req">*</span></label>
                  <select
                    class="param-select"
                    :value="selectedSectionId ?? ''"
                    @change="selectSection(sections.find(s => s.id === Number(($event.target as HTMLSelectElement).value)) ?? null)"
                  >
                    <option value="">请选择分区</option>
                    <option v-for="s in sections" :key="s.id" :value="s.id">{{ s.sectionName }}</option>
                  </select>
                </div>
              </template>

              <!-- EVENT -->
              <template v-else-if="sourceType === 'EVENT'">
                <div class="param-row">
                  <label class="param-label">事件类型 <span class="req">*</span></label>
                  <select
                    class="param-select"
                    :value="selectedEventType"
                    @change="selectEventType(eventTypes.find(et => et.typeCode === ($event.target as HTMLSelectElement).value) ?? { typeCode: ($event.target as HTMLSelectElement).value, typeName: ($event.target as HTMLSelectElement).value })"
                  >
                    <option value="">请选择事件类型</option>
                    <option v-for="et in eventTypes" :key="et.typeCode" :value="et.typeCode">{{ et.typeName }}</option>
                  </select>
                </div>
              </template>

              <!-- HISTORY -->
              <template v-else-if="sourceType === 'HISTORY'">
                <div class="param-row">
                  <label class="param-label">评选活动（空=本活动）</label>
                  <select
                    class="param-select"
                    :value="selectedCampaignId ?? ''"
                    @change="() => {
                      const val = ($event.target as HTMLSelectElement).value
                      const camp = props.campaignList?.find(c => c.id === Number(val))
                      selectedCampaignId = camp?.id ?? null
                      selectedCampaignName = camp?.campaignName ?? ''
                    }"
                  >
                    <option value="">本活动</option>
                    <option v-for="c in campaignList" :key="c.id" :value="c.id">{{ c.campaignName }}</option>
                  </select>
                </div>
              </template>
            </div>

            <!-- Step 3: Metric -->
            <div class="ce-section">
              <div class="ce-section-title">指标</div>
              <div class="metric-grid">
                <button
                  v-for="(cfg, key) in currentMetrics"
                  :key="key"
                  class="metric-btn"
                  :class="{ active: metric === key }"
                  @click="metric = key as ConditionMetric"
                >
                  <span class="metric-label">{{ cfg.label }}</span>
                  <span class="metric-desc">{{ cfg.description }}</span>
                </button>
              </div>
            </div>

            <!-- Step 4: Operator + Threshold -->
            <div class="ce-section">
              <div class="ce-section-title">判定规则</div>
              <div class="rule-row">
                <!-- Operator -->
                <select v-model="operator" class="param-select op-select">
                  <option v-for="op in availableOperators" :key="op" :value="op">
                    {{ OperatorLabels[op] ?? op }}
                  </option>
                </select>

                <!-- Threshold -->
                <template v-if="thresholdType === 'number' || thresholdType === 'percent'">
                  <input
                    v-model.number="thresholdNum"
                    type="number"
                    class="param-input"
                    :placeholder="thresholdType === 'percent' ? '百分比（如 20 = 前20%）' : '输入数值'"
                    min="0"
                  />
                  <span v-if="thresholdType === 'percent'" class="threshold-unit">%</span>
                </template>

                <template v-else-if="thresholdType === 'level_num'">
                  <select v-model.number="thresholdLevelNum" class="param-select">
                    <option value="">请选择级别</option>
                    <option
                      v-for="lv in campaignLevels"
                      :key="lv.levelNum"
                      :value="lv.levelNum"
                    >
                      {{ lv.levelName }}（第{{ lv.levelNum }}级）
                    </option>
                  </select>
                </template>

                <template v-else-if="thresholdType === 'grade_list'">
                  <div class="grade-checkboxes">
                    <label
                      v-for="gb in gradeBands"
                      :key="gb.gradeCode"
                      class="grade-check"
                      :class="{ checked: thresholdGrades.includes(gb.gradeCode) }"
                    >
                      <input
                        type="checkbox"
                        :checked="thresholdGrades.includes(gb.gradeCode)"
                        @change="toggleGrade(gb.gradeCode)"
                      />
                      {{ gb.gradeName }}
                    </label>
                    <span v-if="gradeBands.length === 0" class="grade-empty">
                      请先选择分区以加载等级
                    </span>
                  </div>
                </template>
              </div>
            </div>

            <!-- Step 5: Scope -->
            <div class="ce-section">
              <div class="ce-section-title">数据范围</div>
              <div class="scope-tabs">
                <button
                  v-for="(cfg, key) in ConditionScopeConfig"
                  :key="key"
                  class="scope-tab"
                  :class="{ active: scope === key }"
                  @click="scope = key as 'SELF' | 'MEMBERS' | 'SPECIFIC_ROLE'"
                >
                  {{ cfg.label }}
                </button>
              </div>
              <div v-if="scope === 'SPECIFIC_ROLE'" class="param-row" style="margin-top: 10px;">
                <label class="param-label">角色编码</label>
                <input v-model="scopeRole" type="text" class="param-input" placeholder="如：HEAD_TEACHER" />
              </div>
            </div>

            <!-- Step 6: Time range -->
            <div class="ce-section">
              <div class="ce-section-title">时间范围</div>
              <div class="time-tabs">
                <button
                  class="time-tab"
                  :class="{ active: timeRange === 'CYCLE' }"
                  @click="timeRange = 'CYCLE'"
                >
                  评选周期内
                </button>
                <button
                  class="time-tab"
                  :class="{ active: timeRange === 'CUSTOM' }"
                  @click="timeRange = 'CUSTOM'"
                >
                  自定义天数
                </button>
              </div>
              <div v-if="timeRange === 'CUSTOM'" class="param-row" style="margin-top: 10px;">
                <label class="param-label">天数</label>
                <input
                  v-model.number="timeRangeDays"
                  type="number"
                  class="param-input"
                  placeholder="如：30"
                  min="1"
                  style="width: 120px;"
                />
                <span class="threshold-unit">天</span>
              </div>
            </div>

            <!-- Preview -->
            <div class="ce-preview">
              <div class="preview-label">条件预览</div>
              <div class="preview-text">{{ conditionPreview || '请填写条件参数' }}</div>
            </div>
          </div>

          <!-- Footer -->
          <div class="ce-foot">
            <button class="btn-ghost" @click="emit('close')">取消</button>
            <button class="btn-primary" @click="handleSave">保存条件</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.ce-mask {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(2px);
  padding: 20px;
}
.ce-box {
  width: 640px;
  max-height: 90vh;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.ce-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.ce-head h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}
.ce-close {
  background: none;
  border: none;
  font-size: 22px;
  color: #b8c0cc;
  cursor: pointer;
  line-height: 1;
  padding: 0 4px;
}
.ce-close:hover { color: #5a6474; }

.ce-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.ce-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #5a6474;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

/* Source type tabs */
.source-tabs {
  display: flex;
  gap: 10px;
}
.source-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 12px 14px;
  border: 2px solid #e8ecf0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.source-tab:hover { border-color: #c8d4e3; background: #f8fafc; }
.source-tab.active { border-color: #1a6dff; background: #e8f0ff; }
.source-tab-label {
  font-size: 13px;
  font-weight: 600;
  color: #1e2a3a;
}
.source-tab.active .source-tab-label { color: #1a6dff; }
.source-tab-desc {
  font-size: 11px;
  color: #8c95a3;
  margin-top: 3px;
  text-align: left;
  line-height: 1.4;
}

/* Param rows */
.param-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.param-label {
  font-size: 12px;
  font-weight: 500;
  color: #5a6474;
  width: 120px;
  flex-shrink: 0;
}
.param-select {
  flex: 1;
  height: 34px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 0 10px;
  font-size: 13px;
  outline: none;
  background: #fff;
  color: #3d4757;
  cursor: pointer;
  font-family: inherit;
}
.param-select:focus { border-color: #7aadff; box-shadow: 0 0 0 3px rgba(26,109,255,0.08); }
.param-input {
  flex: 1;
  height: 34px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 13px;
  outline: none;
  background: #fff;
  color: #3d4757;
  font-family: inherit;
}
.param-input:focus { border-color: #7aadff; box-shadow: 0 0 0 3px rgba(26,109,255,0.08); }
.threshold-unit {
  font-size: 12px;
  color: #8c95a3;
  white-space: nowrap;
}

/* Metric grid */
.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 8px;
}
.metric-btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 10px 12px;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.metric-btn:hover { border-color: #c8d4e3; background: #f8fafc; }
.metric-btn.active { border-color: #1a6dff; background: #e8f0ff; }
.metric-label {
  font-size: 13px;
  font-weight: 600;
  color: #1e2a3a;
}
.metric-btn.active .metric-label { color: #1a6dff; }
.metric-desc {
  font-size: 11px;
  color: #8c95a3;
  margin-top: 2px;
  text-align: left;
}

/* Rule row */
.rule-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.op-select {
  flex: none;
  width: 160px;
}

/* Grade checkboxes */
.grade-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.grade-check {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border: 1px solid #e8ecf0;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  color: #5a6474;
}
.grade-check input { display: none; }
.grade-check.checked {
  background: #e8f0ff;
  border-color: #1a6dff;
  color: #1a6dff;
  font-weight: 600;
}
.grade-empty {
  font-size: 12px;
  color: #b8c0cc;
}

/* Scope tabs */
.scope-tabs, .time-tabs {
  display: flex;
  gap: 8px;
}
.scope-tab, .time-tab {
  padding: 6px 16px;
  border: 1px solid #e8ecf0;
  border-radius: 20px;
  background: #fff;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.scope-tab:hover, .time-tab:hover { border-color: #c8d4e3; }
.scope-tab.active, .time-tab.active {
  background: #1a6dff;
  border-color: #1a6dff;
  color: #fff;
  font-weight: 500;
}

/* Preview */
.ce-preview {
  background: #f4f6f9;
  border-radius: 8px;
  padding: 12px 16px;
}
.preview-label {
  font-size: 11px;
  font-weight: 600;
  color: #8c95a3;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}
.preview-text {
  font-size: 13px;
  color: #1e2a3a;
  line-height: 1.6;
  font-style: italic;
}

/* Footer */
.ce-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid #e8ecf0;
  flex-shrink: 0;
}

/* Buttons */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 20px;
  background: #1a6dff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}
.btn-primary:hover { background: #1558d6; }
.btn-ghost {
  padding: 8px 20px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  font-family: inherit;
}
.btn-ghost:hover { background: #f4f6f9; }
.req { color: #d93025; }

.modal-enter-active { transition: all 0.2s ease-out; }
.modal-leave-active { transition: all 0.15s ease-in; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .ce-box { transform: translateY(16px) scale(0.97); }
.modal-leave-to { opacity: 0; }
.modal-leave-to .ce-box { transform: translateY(-8px) scale(0.98); }
</style>
