<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Trash2, Pencil, Layers, BarChart3 } from 'lucide-vue-next'
import {
  getIndicators, createLeafIndicator, createCompositeIndicator,
  updateIndicator, deleteIndicator,
} from '@/api/insp/indicator'
import { getSections } from '@/api/insp/template'
import { getGradeSchemes } from '@/api/insp/gradeScheme'
import type { Indicator } from '@/types/insp/indicator'
import type { TemplateSection } from '@/types/insp/template'
import type { GradeScheme } from '@/types/insp/gradeScheme'
import {
  SOURCE_AGG_OPTIONS, COMPOSITE_AGG_OPTIONS,
  MISSING_POLICY_OPTIONS, EVAL_PERIOD_OPTIONS, NORMALIZATION_OPTIONS,
  EVALUATION_METHOD_OPTIONS,
} from '@/types/insp/indicator'

const props = defineProps<{ projectId: number; rootSectionId: number | null }>()

// ── State ──
const loading = ref(false)
const indicators = ref<Indicator[]>([])
const sections = ref<TemplateSection[]>([])
const gradeSchemes = ref<GradeScheme[]>([])
const dialogVisible = ref(false)
const editingIndicator = ref<Indicator | null>(null)
const saving = ref(false)

// Dialog mode: 'indicator' (leaf) or 'group' (composite)
const dialogMode = ref<'indicator' | 'group'>('indicator')
const dialogParentId = ref<number | null>(null)

const form = ref({
  name: '',
  evaluationPeriod: 'PER_TASK',
  gradeSchemeId: null as number | null,
  evaluationMethod: 'PERCENT_RANGE',
  gradeThresholds: null as string | null,
  sourceSectionId: null as number | null,
  sourceAggregation: 'AVG',
  compositeAggregation: 'WEIGHTED_AVG',
  missingPolicy: 'SKIP',
  normalization: 'NONE',
  sortOrder: 0,
})
const normConfig = ref({ countType: 'USER', relation: 'member', value: 50 })
const thresholdMap = ref<Record<string, number>>({})
const minScoreMap = ref<Record<string, number | null>>({})

const selectedSchemeGrades = computed(() => {
  if (!form.value.gradeSchemeId) return []
  const scheme = gradeSchemes.value.find(s => s.id === form.value.gradeSchemeId)
  return scheme?.grades || []
})

// ── Computed ──
const rootIndicators = computed(() =>
  indicators.value.filter(i => !i.parentIndicatorId).sort((a, b) => a.sortOrder - b.sortOrder)
)

const standaloneIndicators = computed(() =>
  indicators.value.filter(i => !i.parentIndicatorId && i.indicatorType === 'LEAF').sort((a, b) => a.sortOrder - b.sortOrder)
)

function getChildren(parentId: number): Indicator[] {
  return indicators.value.filter(i => i.parentIndicatorId === parentId).sort((a, b) => a.sortOrder - b.sortOrder)
}

function getSectionName(id: number | null): string {
  if (!id) return '-'
  return sections.value.find(s => s.id === id)?.sectionName || `#${id}`
}

function getSchemeDisplayName(id: number | null): string {
  if (!id) return ''
  return gradeSchemes.value.find(s => s.id === id)?.displayName || ''
}

function periodLabel(p: string): string {
  return EVAL_PERIOD_OPTIONS.find(o => o.value === p)?.label || p
}

function aggLabel(type: string, agg: string | null): string {
  if (type === 'LEAF') return SOURCE_AGG_OPTIONS.find(o => o.value === agg)?.label || agg || ''
  return COMPOSITE_AGG_OPTIONS.find(o => o.value === agg)?.label || agg || ''
}

function normLabel(n: string | null): string {
  if (!n || n === 'NONE') return ''
  return NORMALIZATION_OPTIONS.find(o => o.value === n)?.label || n
}

// ── Load ──
async function loadAll() {
  loading.value = true
  try {
    indicators.value = await getIndicators(props.projectId)
    if (props.rootSectionId) {
      sections.value = await getSections(props.rootSectionId)
    }
    gradeSchemes.value = await getGradeSchemes()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// ── Dialog ──
function openAdd(parentId: number | null, mode: 'indicator' | 'group') {
  editingIndicator.value = null
  dialogMode.value = mode
  dialogParentId.value = parentId
  form.value = {
    name: '', evaluationPeriod: mode === 'group' ? 'WEEKLY' : 'PER_TASK',
    gradeSchemeId: null, evaluationMethod: 'PERCENT_RANGE', gradeThresholds: null,
    sourceSectionId: null, sourceAggregation: 'AVG',
    compositeAggregation: 'WEIGHTED_AVG', missingPolicy: 'SKIP',
    normalization: 'NONE', sortOrder: 0,
  }
  normConfig.value = { countType: 'USER', relation: 'member', value: 50 }
  thresholdMap.value = {}
  minScoreMap.value = {}
  // auto sortOrder
  const siblings = parentId ? getChildren(parentId) : rootIndicators.value
  form.value.sortOrder = siblings.length > 0 ? Math.max(...siblings.map(s => s.sortOrder)) + 1 : 0
  dialogVisible.value = true
}

function openEdit(ind: Indicator) {
  editingIndicator.value = ind
  dialogMode.value = ind.indicatorType === 'COMPOSITE' ? 'group' : 'indicator'
  dialogParentId.value = ind.parentIndicatorId
  form.value = {
    name: ind.name,
    evaluationPeriod: ind.evaluationPeriod,
    gradeSchemeId: ind.gradeSchemeId,
    evaluationMethod: ind.evaluationMethod || 'PERCENT_RANGE',
    gradeThresholds: ind.gradeThresholds,
    sourceSectionId: ind.sourceSectionId,
    sourceAggregation: ind.sourceAggregation || 'AVG',
    compositeAggregation: ind.compositeAggregation || 'WEIGHTED_AVG',
    missingPolicy: ind.missingPolicy || 'SKIP',
    normalization: ind.normalization || 'NONE',
    sortOrder: ind.sortOrder,
  }
  normConfig.value = { countType: 'USER', relation: 'member', value: 50 }
  if (ind.normalizationConfig) {
    try { Object.assign(normConfig.value, JSON.parse(ind.normalizationConfig)) } catch {}
  }
  thresholdMap.value = {}
  minScoreMap.value = {}
  if (ind.gradeThresholds) {
    try {
      const arr = JSON.parse(ind.gradeThresholds) as Array<{gradeCode: string; value: number; minScore?: number}>
      for (const t of arr) {
        thresholdMap.value[t.gradeCode] = t.value
        if (t.minScore != null) minScoreMap.value[t.gradeCode] = t.minScore
      }
    } catch {}
  }
  dialogVisible.value = true
}

// ── Save ──
async function handleSave() {
  if (!form.value.name.trim()) { ElMessage.warning('请填写名称'); return }
  const normCfg = form.value.normalization !== 'NONE' ? JSON.stringify(normConfig.value) : undefined
  const serializedThresholds = form.value.gradeSchemeId
    ? JSON.stringify(selectedSchemeGrades.value.map(g => ({
        gradeCode: g.code,
        value: thresholdMap.value[g.code] || 0,
        ...(minScoreMap.value[g.code] != null ? { minScore: minScoreMap.value[g.code] } : {}),
      })))
    : undefined
  saving.value = true
  try {
    if (editingIndicator.value) {
      await updateIndicator(editingIndicator.value.id, {
        name: form.value.name,
        evaluationPeriod: form.value.evaluationPeriod, gradeSchemeId: form.value.gradeSchemeId,
        evaluationMethod: form.value.evaluationMethod, gradeThresholds: serializedThresholds,
        normalization: form.value.normalization, normalizationConfig: normCfg,
        sortOrder: form.value.sortOrder,
        ...(dialogMode.value === 'indicator'
          ? { sourceSectionId: form.value.sourceSectionId, sourceAggregation: form.value.sourceAggregation }
          : { compositeAggregation: form.value.compositeAggregation, missingPolicy: form.value.missingPolicy }),
      })
    } else if (dialogMode.value === 'indicator') {
      await createLeafIndicator({
        projectId: props.projectId, parentIndicatorId: dialogParentId.value,
        name: form.value.name, sourceSectionId: form.value.sourceSectionId,
        sourceAggregation: form.value.sourceAggregation, evaluationPeriod: form.value.evaluationPeriod,
        gradeSchemeId: form.value.gradeSchemeId,
        evaluationMethod: form.value.evaluationMethod, gradeThresholds: serializedThresholds,
        normalization: form.value.normalization, normalizationConfig: normCfg,
        sortOrder: form.value.sortOrder,
      })
    } else {
      await createCompositeIndicator({
        projectId: props.projectId, parentIndicatorId: dialogParentId.value,
        name: form.value.name, compositeAggregation: form.value.compositeAggregation,
        missingPolicy: form.value.missingPolicy, evaluationPeriod: form.value.evaluationPeriod,
        gradeSchemeId: form.value.gradeSchemeId,
        evaluationMethod: form.value.evaluationMethod, gradeThresholds: serializedThresholds,
        normalization: form.value.normalization, normalizationConfig: normCfg,
        sortOrder: form.value.sortOrder,
      })
    }
    ElMessage.success(editingIndicator.value ? '已更新' : '已添加')
    dialogVisible.value = false
    await loadAll()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
  finally { saving.value = false }
}

// ── Delete ──
async function handleDelete(ind: Indicator) {
  const n = getChildren(ind.id).length
  const msg = n > 0 ? `「${ind.name}」包含 ${n} 个子指标，一并删除？` : `删除「${ind.name}」？`
  try {
    await ElMessageBox.confirm(msg, '确认删除', { type: 'warning' })
    await deleteIndicator(ind.id)
    ElMessage.success('已删除')
    await loadAll()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

const leafSections = computed(() => sections.value.filter(s => s.parentSectionId !== null))

onMounted(() => loadAll())
</script>

<template>
  <div class="ite" v-loading="loading">
    <!-- Header -->
    <div class="ite-head">
      <div class="ite-head-left">
        <Layers class="w-4 h-4" style="color: #6366f1" />
        <span class="ite-head-title">评价指标</span>
        <span class="ite-head-count" v-if="indicators.length">{{ indicators.length }} 项</span>
      </div>
      <div class="ite-head-actions">
        <button class="ite-btn secondary" @click="openAdd(null, 'indicator')">
          <Plus class="w-3.5 h-3.5" /> 添加指标
        </button>
        <button class="ite-btn primary" @click="openAdd(null, 'group')">
          <Plus class="w-3.5 h-3.5" /> 添加分组
        </button>
      </div>
    </div>

    <!-- Empty -->
    <div v-if="indicators.length === 0 && !loading" class="ite-empty">
      <div class="ite-empty-icon">
        <BarChart3 class="w-8 h-8" />
      </div>
      <div class="ite-empty-title">尚未配置评价指标</div>
      <div class="ite-empty-desc">添加分组来组织评价维度，然后在分组下添加具体的检查指标</div>
      <button class="ite-btn primary lg" @click="openAdd(null, 'group')">
        <Plus class="w-4 h-4" /> 创建第一个分组
      </button>
    </div>

    <!-- Tree -->
    <div class="ite-tree">
      <div v-for="root in rootIndicators" :key="root.id" class="ite-group">
        <!-- Group Header -->
        <div class="ite-group-head">
          <div class="ite-group-left">
            <div class="ite-group-dot" />
            <span class="ite-group-name">{{ root.name }}</span>
            <span class="ite-pill purple">{{ periodLabel(root.evaluationPeriod) }}</span>
            <span class="ite-pill blue" v-if="aggLabel('COMPOSITE', root.compositeAggregation)">
              {{ aggLabel('COMPOSITE', root.compositeAggregation) }}
            </span>
            <span class="ite-pill amber" v-if="root.gradeSchemeId">
              {{ getSchemeDisplayName(root.gradeSchemeId) }}
            </span>
            <span class="ite-pill sky" v-if="normLabel(root.normalization)">
              {{ normLabel(root.normalization) }}
            </span>
          </div>
          <div class="ite-group-actions">
            <button class="ite-icon-btn" @click="openEdit(root)" title="编辑"><Pencil class="w-3.5 h-3.5" /></button>
            <button class="ite-icon-btn danger" @click="handleDelete(root)" title="删除"><Trash2 class="w-3.5 h-3.5" /></button>
          </div>
        </div>

        <!-- Children -->
        <div class="ite-items">
          <div v-for="child in getChildren(root.id)" :key="child.id" class="ite-item">
            <div class="ite-item-bar" />
            <div class="ite-item-body">
              <div class="ite-item-top">
                <span class="ite-item-name">{{ child.name }}</span>
                <div class="ite-item-pills">
                  <span class="ite-pill green" v-if="child.sourceSectionId">{{ getSectionName(child.sourceSectionId) }}</span>
                  <span class="ite-pill purple">{{ periodLabel(child.evaluationPeriod) }}</span>
                  <span class="ite-pill blue">{{ aggLabel('LEAF', child.sourceAggregation) }}</span>
                  <span class="ite-pill amber" v-if="child.gradeSchemeId">{{ getSchemeDisplayName(child.gradeSchemeId) }}</span>
                  <span class="ite-pill sky" v-if="normLabel(child.normalization)">{{ normLabel(child.normalization) }}</span>
                </div>
              </div>
            </div>
            <div class="ite-item-actions">
              <button class="ite-icon-btn" @click="openEdit(child)" title="编辑"><Pencil class="w-3.5 h-3.5" /></button>
              <button class="ite-icon-btn danger" @click="handleDelete(child)" title="删除"><Trash2 class="w-3.5 h-3.5" /></button>
            </div>
          </div>

          <!-- Add indicator -->
          <button class="ite-add-child" @click="openAdd(root.id, 'indicator')">
            <Plus class="w-3.5 h-3.5" /> 添加指标
          </button>
        </div>
      </div>
    </div>

    <!-- Standalone indicators (not in any group) -->
    <div v-if="standaloneIndicators.length > 0" class="ite-standalone">
      <div v-for="ind in standaloneIndicators" :key="ind.id" class="ite-item standalone">
        <div class="ite-item-bar" />
        <div class="ite-item-body">
          <div class="ite-item-top">
            <span class="ite-item-name">{{ ind.name }}</span>
            <div class="ite-item-pills">
              <span class="ite-pill green" v-if="ind.sourceSectionId">{{ getSectionName(ind.sourceSectionId) }}</span>
              <span class="ite-pill purple">{{ periodLabel(ind.evaluationPeriod) }}</span>
              <span class="ite-pill blue">{{ aggLabel('LEAF', ind.sourceAggregation) }}</span>
              <span class="ite-pill amber" v-if="ind.gradeSchemeId">{{ getSchemeDisplayName(ind.gradeSchemeId) }}</span>
              <span class="ite-pill sky" v-if="normLabel(ind.normalization)">{{ normLabel(ind.normalization) }}</span>
            </div>
          </div>
        </div>
        <div class="ite-item-actions" style="opacity:1">
          <button class="ite-icon-btn" @click="openEdit(ind)"><Pencil class="w-3.5 h-3.5" /></button>
          <button class="ite-icon-btn danger" @click="handleDelete(ind)"><Trash2 class="w-3.5 h-3.5" /></button>
        </div>
      </div>
    </div>

    <!-- ─── Dialog ─── -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingIndicator ? '编辑' + (dialogMode === 'group' ? '分组' : '指标') : '添加' + (dialogMode === 'group' ? '分组' : '指标')"
      width="520px" :close-on-click-modal="false"
      class="ite-dialog"
    >
      <div class="dlg">
        <!-- Name -->
        <div class="dlg-field">
          <label class="dlg-label">名称 <span class="dlg-req">*</span></label>
          <input v-model="form.name" class="dlg-input" :placeholder="dialogMode === 'group' ? '如：综合评价' : '如：卫生指标'" />
        </div>

        <!-- Indicator: source section -->
        <template v-if="dialogMode === 'indicator'">
          <div class="dlg-field">
            <label class="dlg-label">数据来源分区</label>
            <el-select v-model="form.sourceSectionId" placeholder="选择检查分区" style="width:100%" filterable clearable size="default">
              <el-option v-for="s in leafSections" :key="s.id" :label="s.sectionName" :value="s.id" />
            </el-select>
          </div>
          <div class="dlg-row">
            <div class="dlg-field">
              <label class="dlg-label">多次检查合并</label>
              <el-select v-model="form.sourceAggregation" style="width:100%" size="default">
                <el-option v-for="o in SOURCE_AGG_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </div>
            <div class="dlg-field">
              <label class="dlg-label">评估周期</label>
              <el-select v-model="form.evaluationPeriod" style="width:100%" size="default">
                <el-option v-for="o in EVAL_PERIOD_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </div>
          </div>
        </template>

        <!-- Group: aggregation -->
        <template v-if="dialogMode === 'group'">
          <div class="dlg-row">
            <div class="dlg-field">
              <label class="dlg-label">汇总方式</label>
              <el-select v-model="form.compositeAggregation" style="width:100%" size="default">
                <el-option v-for="o in COMPOSITE_AGG_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </div>
            <div class="dlg-field">
              <label class="dlg-label">评估周期</label>
              <el-select v-model="form.evaluationPeriod" style="width:100%" size="default">
                <el-option v-for="o in EVAL_PERIOD_OPTIONS.filter(o => o.value !== 'PER_TASK')" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </div>
          </div>
          <div class="dlg-field">
            <label class="dlg-label">数据缺失时</label>
            <el-select v-model="form.missingPolicy" style="width:100%" size="default">
              <el-option v-for="o in MISSING_POLICY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </div>
        </template>

        <!-- Grade scheme -->
        <div class="dlg-field">
          <label class="dlg-label">等级方案 <a href="/inspection/v7/grade-schemes" target="_blank" class="dlg-link">管理方案</a></label>
          <el-select v-model="form.gradeSchemeId" clearable placeholder="不映射等级" style="width:100%" size="default">
            <el-option v-for="s in gradeSchemes" :key="s.id" :label="s.displayName" :value="s.id" />
          </el-select>
        </div>

        <!-- Evaluation Method (only when grade scheme selected) -->
        <template v-if="form.gradeSchemeId">
          <div class="dlg-field">
            <label class="dlg-label">评价方式</label>
            <el-select v-model="form.evaluationMethod" style="width:100%" size="default">
              <el-option v-for="o in EVALUATION_METHOD_OPTIONS" :key="o.value" :label="o.label" :value="o.value">
                <div><span class="font-medium">{{ o.label }}</span><span class="text-xs text-gray-400 ml-2">{{ o.description }}</span></div>
              </el-option>
            </el-select>
          </div>

          <!-- Grade thresholds -->
          <div class="dlg-field" v-if="selectedSchemeGrades.length > 0">
            <label class="dlg-label">阈值设置</label>
            <div class="thresh-list">
              <div v-for="g in selectedSchemeGrades" :key="g.code" class="thresh-row">
                <span class="thresh-grade" :style="{ color: g.color }">{{ g.name }}</span>
                <span class="thresh-op">{{ form.evaluationMethod === 'RANK_COUNT' ? '前' : form.evaluationMethod === 'RANK_PERCENT' ? '前' : '≥' }}</span>
                <input v-model.number="thresholdMap[g.code]" type="number" class="thresh-input" />
                <span class="thresh-unit">{{ form.evaluationMethod === 'RANK_COUNT' ? '名' : form.evaluationMethod === 'RANK_PERCENT' ? '%' : form.evaluationMethod === 'PERCENT_RANGE' ? '%' : '分' }}</span>
                <template v-if="form.evaluationMethod === 'RANK_COUNT' || form.evaluationMethod === 'RANK_PERCENT'">
                  <span class="thresh-op" style="margin-left:4px">且≥</span>
                  <input v-model.number="minScoreMap[g.code]" type="number" class="thresh-input" placeholder="不限" />
                  <span class="thresh-unit">分</span>
                </template>
              </div>
            </div>
          </div>
        </template>

        <!-- Normalization -->
        <div class="dlg-divider">归一化</div>
        <div class="norm-grid">
          <div v-for="opt in NORMALIZATION_OPTIONS" :key="opt.value"
            class="norm-opt" :class="{ active: form.normalization === opt.value }"
            @click="form.normalization = opt.value">
            <div class="norm-opt-title">{{ opt.label }}</div>
            <div class="norm-opt-desc">{{ opt.description }}</div>
          </div>
        </div>

        <!-- Normalization sub-config -->
        <div v-if="form.normalization === 'RELATION_COUNT'" class="norm-detail">
          <div class="norm-detail-row">
            <label>统计类型</label>
            <el-select v-model="normConfig.countType" size="small" style="width:130px">
              <el-option value="USER" label="用户" />
              <el-option value="ORG" label="组织" />
              <el-option value="PLACE" label="场所" />
            </el-select>
          </div>
          <div class="norm-detail-row">
            <label>关系类型</label>
            <el-input v-model="normConfig.relation" size="small" style="width:130px" placeholder="member" />
          </div>
          <div class="norm-formula">
            原始分 ÷ 关联{{ normConfig.countType === 'USER' ? '用户' : normConfig.countType === 'ORG' ? '组织' : '场所' }}数 = 归一化分
          </div>
        </div>
        <div v-if="form.normalization === 'FIXED_VALUE'" class="norm-detail">
          <div class="norm-detail-row">
            <label>除数</label>
            <el-input-number v-model="normConfig.value" :min="1" size="small" style="width:130px" />
          </div>
          <div class="norm-formula">原始分 ÷ {{ normConfig.value || 1 }} = 归一化分</div>
        </div>
        <div v-if="form.normalization === 'PERCENTAGE'" class="norm-detail">
          <div class="norm-formula">原始分 ÷ 满分 × 100 = 百分比</div>
        </div>
      </div>

      <template #footer>
        <div class="dlg-footer">
          <button class="dlg-btn cancel" @click="dialogVisible = false">取消</button>
          <button class="dlg-btn confirm" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : (editingIndicator ? '更新' : '添加') }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ── Base ── */
.ite { min-height: 200px; }

/* ── Header ── */
.ite-head {
  display: flex; align-items: center; justify-content: space-between;
  padding-bottom: 14px; border-bottom: 1px solid #f0f0f3; margin-bottom: 16px;
}
.ite-head-left { display: flex; align-items: center; gap: 8px; }
.ite-head-title { font-size: 15px; font-weight: 700; color: #1e1b4b; letter-spacing: -0.3px; }
.ite-head-count { font-size: 11px; color: #a5b4fc; background: #eef2ff; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
.ite-head-actions { display: flex; gap: 6px; }

/* ── Buttons ── */
.ite-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 14px; border-radius: 8px; font-size: 12px; font-weight: 600;
  border: none; cursor: pointer; transition: all 0.2s;
}
.ite-btn.secondary { background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }
.ite-btn.secondary:hover { background: #dcfce7; }
.ite-btn.primary { background: #6366f1; color: #fff; }
.ite-btn.primary:hover { background: #4f46e5; }
.ite-btn.lg { padding: 10px 22px; font-size: 13px; border-radius: 10px; }

/* ── Empty ── */
.ite-empty { text-align: center; padding: 48px 20px; }
.ite-empty-icon { color: #c7d2fe; margin-bottom: 12px; display: flex; justify-content: center; }
.ite-empty-title { font-size: 14px; font-weight: 600; color: #4b5563; margin-bottom: 4px; }
.ite-empty-desc { font-size: 12px; color: #9ca3af; margin-bottom: 16px; max-width: 280px; margin-left: auto; margin-right: auto; }

/* ── Tree ── */
.ite-tree { display: flex; flex-direction: column; gap: 12px; }

/* ── Group (root composite) ── */
.ite-group {
  border: 1px solid #e5e7eb; border-radius: 12px; background: #fff;
  overflow: hidden; transition: box-shadow 0.2s;
}
.ite-group:hover { box-shadow: 0 2px 12px rgba(99,102,241,0.06); }

.ite-group-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; background: linear-gradient(135deg, #fafbff 0%, #f5f3ff 100%);
  border-bottom: 1px solid #f0eef5;
}
.ite-group-left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ite-group-dot { width: 8px; height: 8px; border-radius: 50%; background: #6366f1; flex-shrink: 0; }
.ite-group-name { font-size: 14px; font-weight: 700; color: #1e1b4b; }
.ite-group-actions { display: flex; gap: 2px; }

/* ── Icon button ── */
.ite-icon-btn {
  width: 28px; height: 28px; border: none; border-radius: 6px;
  background: transparent; color: #9ca3af; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.ite-icon-btn:hover { background: #f3f4f6; color: #374151; }
.ite-icon-btn.danger:hover { background: #fef2f2; color: #ef4444; }

/* ── Pills ── */
.ite-pill {
  font-size: 10px; font-weight: 600; padding: 2px 7px; border-radius: 5px; white-space: nowrap;
}
.ite-pill.purple { background: #f3f0ff; color: #7c3aed; }
.ite-pill.blue { background: #eff6ff; color: #3b82f6; }
.ite-pill.green { background: #f0fdf4; color: #16a34a; }
.ite-pill.amber { background: #fffbeb; color: #d97706; }
.ite-pill.sky { background: #f0f9ff; color: #0284c7; }

/* ── Items (leaf children) ── */
.ite-items { padding: 8px 16px 12px 16px; }

.ite-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 0; border-bottom: 1px solid #f9fafb;
  transition: background 0.1s;
}
.ite-item:last-of-type { border-bottom: none; }
.ite-item:hover { background: #fafbff; margin: 0 -8px; padding: 8px 8px; border-radius: 8px; }

.ite-item-bar { width: 3px; height: 24px; border-radius: 2px; background: #22c55e; flex-shrink: 0; }
.ite-item-body { flex: 1; min-width: 0; }
.ite-item-top { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ite-item-name { font-size: 13px; font-weight: 600; color: #1f2937; }
.ite-item-pills { display: flex; gap: 4px; flex-wrap: wrap; }
.ite-item-actions { display: flex; gap: 2px; flex-shrink: 0; opacity: 0; transition: opacity 0.15s; }
.ite-item:hover .ite-item-actions { opacity: 1; }

/* ── Add child ── */
.ite-add-child {
  display: inline-flex; align-items: center; gap: 4px;
  margin-top: 6px; padding: 5px 12px; border-radius: 7px;
  font-size: 12px; font-weight: 500; color: #9ca3af;
  background: none; border: 1px dashed #e5e7eb; cursor: pointer;
  transition: all 0.15s;
}
.ite-add-child:hover { color: #6366f1; border-color: #a5b4fc; background: #f5f3ff; }

/* ── Standalone indicators ── */
.ite-standalone { margin-top: 8px; }
.ite-item.standalone { padding: 10px 12px; border: 1px solid #e5e7eb; border-radius: 10px; margin-bottom: 6px; background: #fff; }

/* ── Dialog ── */
.dlg { display: flex; flex-direction: column; gap: 14px; }
.dlg-field { display: flex; flex-direction: column; gap: 4px; }
.dlg-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.dlg-label { font-size: 12px; font-weight: 600; color: #4b5563; display: flex; align-items: center; justify-content: space-between; }
.dlg-link { font-size: 11px; font-weight: 500; color: #6366f1; text-decoration: none; }
.dlg-link:hover { text-decoration: underline; }
.dlg-req { color: #ef4444; }
.dlg-input {
  width: 100%; padding: 8px 12px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 13px; color: #1f2937; outline: none; transition: border-color 0.2s;
}
.dlg-input:focus { border-color: #6366f1; }
.dlg-input::placeholder { color: #d1d5db; }

.dlg-divider {
  font-size: 11px; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.5px;
  padding-top: 6px; border-top: 1px solid #f3f4f6; margin-top: 2px;
}

.dlg-footer { display: flex; justify-content: flex-end; gap: 8px; }
.dlg-btn {
  padding: 8px 20px; border-radius: 8px; font-size: 13px; font-weight: 600;
  border: none; cursor: pointer; transition: all 0.15s;
}
.dlg-btn.cancel { background: #f3f4f6; color: #6b7280; }
.dlg-btn.cancel:hover { background: #e5e7eb; }
.dlg-btn.confirm { background: #6366f1; color: #fff; }
.dlg-btn.confirm:hover { background: #4f46e5; }
.dlg-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ── Normalization ── */
.norm-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.norm-opt {
  padding: 8px 10px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  cursor: pointer; transition: all 0.15s;
}
.norm-opt:hover { border-color: #a5b4fc; }
.norm-opt.active { border-color: #6366f1; background: #f5f3ff; }
.norm-opt-title { font-size: 12px; font-weight: 600; color: #1e1b4b; }
.norm-opt-desc { font-size: 10px; color: #9ca3af; margin-top: 1px; }

.norm-detail { padding: 10px 12px; background: #fafbff; border-radius: 8px; margin-top: 6px; }
.norm-detail-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.norm-detail-row label { font-size: 12px; color: #6b7280; min-width: 56px; }
.norm-formula {
  font-size: 11px; color: #6366f1; background: #eef2ff;
  padding: 6px 10px; border-radius: 6px; margin-top: 4px;
}

/* ── Threshold editor ── */
.thresh-list { display: flex; flex-direction: column; gap: 6px; }
.thresh-row { display: flex; align-items: center; gap: 6px; }
.thresh-grade { font-size: 12px; font-weight: 600; min-width: 50px; }
.thresh-op { font-size: 11px; color: #9ca3af; }
.thresh-input { width: 60px; padding: 4px 8px; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 12px; text-align: center; outline: none; }
.thresh-input:focus { border-color: #6366f1; }
.thresh-unit { font-size: 11px; color: #9ca3af; }
</style>
