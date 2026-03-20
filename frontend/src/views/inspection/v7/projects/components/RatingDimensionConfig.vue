<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Trash2, Star, BarChart3, Calculator } from 'lucide-vue-next'
import { inspRatingDimensionApi } from '@/api/insp/project'
import { inspTemplateApi } from '@/api/insp/template'
import type { RatingDimension, TemplateSection, GradeBand } from '@/types/insp/template'

const props = defineProps<{
  projectId: number
}>()

const router = useRouter()

// ========== State ==========
const loading = ref(false)
const dimensions = ref<RatingDimension[]>([])
const sections = ref<TemplateSection[]>([])
const dialogVisible = ref(false)
const editingDimension = ref<RatingDimension | null>(null)
const saving = ref(false)
const calculating = ref<number | null>(null)

const aggregationOptions = [
  { value: 'WEIGHTED_AVERAGE', label: '加权平均' },
  { value: 'SUM', label: '求和' },
  { value: 'AVERAGE', label: '平均值' },
  { value: 'MAX', label: '最大值' },
  { value: 'MIN', label: '最小值' },
]

const form = ref({
  dimensionName: '',
  sectionIds: [] as number[],
  aggregation: 'WEIGHTED_AVERAGE',
  gradeBands: [] as GradeBand[],
  awardName: '',
  rankingEnabled: true,
})

// ========== Computed ==========
const sectionOptions = computed(() =>
  sections.value.map(s => ({
    value: s.id,
    label: s.sectionName,
    targetType: s.targetType,
  }))
)

function getSectionNames(sectionIdsJson: string): string {
  try {
    const ids: number[] = JSON.parse(sectionIdsJson)
    return ids.map(id => {
      const s = sections.value.find(sec => sec.id === id)
      return s ? s.sectionName : `#${id}`
    }).join('、')
  } catch {
    return sectionIdsJson
  }
}

function getGradeBandsPreview(bandsJson: string | null): string {
  if (!bandsJson) return '-'
  try {
    const bands: GradeBand[] = JSON.parse(bandsJson)
    return bands.map(b => `${b.grade}(${b.minScore}-${b.maxScore})`).join(' ')
  } catch {
    return '-'
  }
}

function getAggregationLabel(val: string): string {
  return aggregationOptions.find(o => o.value === val)?.label || val
}

// ========== Load ==========
async function loadDimensions() {
  loading.value = true
  try {
    dimensions.value = await inspRatingDimensionApi.list(props.projectId)
  } catch (e: any) {
    ElMessage.error(e.message || '加载评级维度失败')
  } finally {
    loading.value = false
  }
}

async function loadSections() {
  try {
    const { getProject } = await import('@/api/insp/project')
    const project = await getProject(props.projectId)
    if (project.rootSectionId) {
      const all = await inspTemplateApi.getSections(project.rootSectionId)
      sections.value = all.filter(s => s.parentSectionId === project.rootSectionId)
    }
  } catch { /* ignore */ }
}

// ========== CRUD ==========
function openAddDialog() {
  editingDimension.value = null
  form.value = {
    dimensionName: '',
    sectionIds: [],
    aggregation: 'WEIGHTED_AVERAGE',
    gradeBands: [
      { grade: 'A', label: '优秀', minScore: 90, maxScore: 100, color: '#10b981' },
      { grade: 'B', label: '良好', minScore: 80, maxScore: 89, color: '#3b82f6' },
      { grade: 'C', label: '合格', minScore: 60, maxScore: 79, color: '#f59e0b' },
      { grade: 'D', label: '不合格', minScore: 0, maxScore: 59, color: '#ef4444' },
    ],
    awardName: '',
    rankingEnabled: true,
  }
  dialogVisible.value = true
}

function openEditDialog(dim: RatingDimension) {
  editingDimension.value = dim
  let sectionIds: number[] = []
  try { sectionIds = JSON.parse(dim.sectionIds) } catch {}
  let gradeBands: GradeBand[] = []
  try { if (dim.gradeBands) gradeBands = JSON.parse(dim.gradeBands) } catch {}
  form.value = {
    dimensionName: dim.dimensionName,
    sectionIds,
    aggregation: dim.aggregation,
    gradeBands,
    awardName: dim.awardName || '',
    rankingEnabled: dim.rankingEnabled,
  }
  dialogVisible.value = true
}

function addGradeBand() {
  form.value.gradeBands.push({ grade: '', label: '', minScore: 0, maxScore: 100, color: '#909399' })
}

function removeGradeBand(idx: number) {
  form.value.gradeBands.splice(idx, 1)
}

async function handleSave() {
  if (!form.value.dimensionName.trim()) {
    ElMessage.warning('请输入维度名称')
    return
  }
  saving.value = true
  try {
    const data: Partial<RatingDimension> = {
      projectId: props.projectId,
      dimensionName: form.value.dimensionName,
      sectionIds: JSON.stringify(form.value.sectionIds),
      aggregation: form.value.aggregation,
      gradeBands: form.value.gradeBands.length > 0 ? JSON.stringify(form.value.gradeBands) : null,
      awardName: form.value.awardName || null,
      rankingEnabled: form.value.rankingEnabled,
    }
    if (editingDimension.value) {
      await inspRatingDimensionApi.update(editingDimension.value.id, data)
      ElMessage.success('已更新')
    } else {
      await inspRatingDimensionApi.create(data)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadDimensions()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(dim: RatingDimension) {
  try {
    await ElMessageBox.confirm(`删除维度「${dim.dimensionName}」？`, '确认', { type: 'warning' })
    await inspRatingDimensionApi.delete(dim.id)
    ElMessage.success('已删除')
    loadDimensions()
  } catch {}
}

async function handleCalculate(dim: RatingDimension) {
  calculating.value = dim.id
  try {
    const results = await inspRatingDimensionApi.calculate(dim.id)
    ElMessage.success(`计算完成，共 ${results.length} 条结果`)
  } catch (e: any) {
    ElMessage.error(e.message || '计算失败')
  } finally {
    calculating.value = null
  }
}

function goRankings(dim: RatingDimension) {
  router.push(`/inspection/v7/rating-dimensions/${dim.id}/rankings`)
}

onMounted(() => {
  loadDimensions()
  loadSections()
})
</script>

<template>
  <div v-loading="loading">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <div class="text-sm text-gray-500">配置评级维度，定义分数聚合和等级划分规则</div>
      <el-button type="primary" size="small" @click="openAddDialog">
        <Plus class="w-3.5 h-3.5 mr-1" />添加维度
      </el-button>
    </div>

    <!-- Dimension list -->
    <div v-if="dimensions.length === 0" class="py-16 text-center text-gray-400">
      <Star class="w-12 h-12 mx-auto mb-3 text-gray-300" />
      <div class="text-sm">暂无评级维度</div>
      <div class="text-xs text-gray-300 mt-1">添加维度来定义评级规则</div>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="dim in dimensions"
        :key="dim.id"
        class="bg-white border border-gray-200 rounded-xl p-4 shadow-sm hover:border-gray-300 transition-colors"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-medium text-gray-800">{{ dim.dimensionName }}</span>
              <el-tag size="small" type="primary" round effect="plain">{{ getAggregationLabel(dim.aggregation) }}</el-tag>
              <el-tag v-if="dim.awardName" size="small" type="warning" round effect="plain">{{ dim.awardName }}</el-tag>
              <el-tag v-if="dim.rankingEnabled" size="small" type="success" round effect="plain">排名</el-tag>
            </div>
            <div class="text-xs text-gray-400 mt-1.5 space-y-0.5">
              <div>关联分区: {{ getSectionNames(dim.sectionIds) || '全部' }}</div>
              <div>等级划分: {{ getGradeBandsPreview(dim.gradeBands) }}</div>
            </div>
          </div>
          <div class="flex items-center gap-1.5 shrink-0 ml-3">
            <el-button
              size="small"
              type="primary"
              plain
              :loading="calculating === dim.id"
              @click="handleCalculate(dim)"
            >
              <Calculator class="w-3.5 h-3.5 mr-0.5" />计算
            </el-button>
            <el-button
              v-if="dim.rankingEnabled"
              size="small"
              plain
              @click="goRankings(dim)"
            >
              <BarChart3 class="w-3.5 h-3.5 mr-0.5" />排名
            </el-button>
            <el-button size="small" plain @click="openEditDialog(dim)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(dim)">
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingDimension ? '编辑评级维度' : '添加评级维度'"
      width="640px"
      destroy-on-close
    >
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">维度名称 <span class="text-red-500">*</span></label>
          <el-input v-model="form.dimensionName" placeholder="例如：综合评级" maxlength="100" />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">关联分区</label>
          <el-select v-model="form.sectionIds" multiple class="w-full" placeholder="选择分区（不选则覆盖全部）" collapse-tags collapse-tags-tooltip>
            <el-option v-for="opt in sectionOptions" :key="opt.value" :label="opt.label" :value="opt.value">
              <div class="flex items-center justify-between">
                <span>{{ opt.label }}</span>
                <el-tag v-if="opt.targetType" size="small" type="info" class="ml-2">{{ opt.targetType }}</el-tag>
              </div>
            </el-option>
          </el-select>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">聚合方式</label>
            <el-select v-model="form.aggregation" class="w-full">
              <el-option v-for="opt in aggregationOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">奖项名称</label>
            <el-input v-model="form.awardName" placeholder="例如：流动红旗" />
          </div>
        </div>

        <div>
          <el-checkbox v-model="form.rankingEnabled">启用排名</el-checkbox>
        </div>

        <!-- Grade Bands Editor -->
        <div>
          <div class="flex items-center justify-between mb-2">
            <label class="text-sm font-medium text-gray-700">等级划分</label>
            <el-button size="small" text type="primary" @click="addGradeBand"><Plus class="w-3.5 h-3.5 mr-0.5" />添加</el-button>
          </div>
          <div v-if="form.gradeBands.length === 0" class="text-xs text-gray-400 py-2">暂无等级划分</div>
          <div v-else class="space-y-2">
            <div
              v-for="(band, idx) in form.gradeBands"
              :key="idx"
              class="flex items-center gap-2 bg-gray-50 rounded-lg px-3 py-2"
            >
              <el-color-picker v-model="band.color" size="small" />
              <el-input v-model="band.grade" placeholder="等级" class="!w-16" size="small" />
              <el-input v-model="band.label" placeholder="标签" class="!w-20" size="small" />
              <el-input-number v-model="band.minScore" :min="0" :max="200" size="small" class="!w-24" placeholder="最低分" />
              <span class="text-gray-400 text-xs">~</span>
              <el-input-number v-model="band.maxScore" :min="0" :max="200" size="small" class="!w-24" placeholder="最高分" />
              <el-button size="small" link type="danger" @click="removeGradeBand(idx)"><Trash2 class="w-3.5 h-3.5" /></el-button>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ editingDimension ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
