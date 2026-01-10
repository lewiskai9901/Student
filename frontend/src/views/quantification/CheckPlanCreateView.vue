<template>
  <div class="page-container">
    <!-- 顶部栏 -->
    <header class="top-bar">
      <div class="top-left">
        <button class="btn-back" @click="goBack">
          <ArrowLeft :size="18" />
        </button>
        <div class="page-title">
          <h1>新建检查计划</h1>
          <span class="breadcrumb">量化管理 / 检查计划 / 新建</span>
        </div>
      </div>
      <div class="top-right">
        <button class="btn-cancel" @click="goBack">取消</button>
        <button class="btn-submit" :disabled="!canSubmit || submitting" @click="handleSubmit">
          <Loader2 v-if="submitting" :size="16" class="spin" />
          <Check v-else :size="16" />
          {{ submitting ? '创建中...' : '创建计划' }}
        </button>
      </div>
    </header>

    <!-- 基本信息 -->
    <section class="info-bar">
      <div class="info-field flex-2">
        <label><FileText :size="12" /> 计划名称 <em>*</em></label>
        <input v-model="form.planName" type="text" placeholder="如：第1周量化检查" />
      </div>
      <div class="info-field flex-1">
        <label><CalendarDays :size="12" /> 开始日期 <em>*</em></label>
        <input v-model="form.startDate" type="date" />
      </div>
      <div class="info-field flex-1">
        <label><CalendarDays :size="12" /> 结束日期 <em>*</em></label>
        <input v-model="form.endDate" type="date" :min="form.startDate" />
      </div>
      <div class="info-field flex-2">
        <label><MessageSquare :size="12" /> 备注</label>
        <input v-model="form.description" type="text" placeholder="可选" />
      </div>
    </section>

    <!-- 目标范围选择 -->
    <section class="target-scope-bar">
      <div class="scope-header">
        <Users :size="14" />
        <span class="scope-title">检查目标范围</span>
        <span class="scope-hint">（指定本计划覆盖的检查对象范围）</span>
      </div>
      <div class="scope-content">
        <div class="scope-type-group">
          <label
            v-for="opt in scopeTypeOptions"
            :key="opt.value"
            class="scope-type-item"
            :class="{ active: targetScopeType === opt.value }"
          >
            <input
              type="radio"
              :value="opt.value"
              v-model="targetScopeType"
              @change="onTargetScopeTypeChange"
            />
            <component :is="opt.icon" :size="14" />
            <span>{{ opt.label }}</span>
          </label>
        </div>

        <!-- 按院系 -->
        <div v-if="targetScopeType === 'department'" class="scope-select-area">
          <div class="select-label">
            <Building2 :size="12" />
            选择院系
          </div>
          <div class="select-chips">
            <div
              v-for="dept in flatDepartments"
              :key="dept.id"
              class="select-chip"
              :class="{ selected: selectedDepartmentIds.includes(dept.id) }"
              @click="toggleDepartment(dept.id)"
            >
              <CheckCircle v-if="selectedDepartmentIds.includes(dept.id)" :size="12" />
              <span>{{ dept.deptName }}</span>
            </div>
          </div>
          <div class="select-info" v-if="selectedDepartmentIds.length > 0">
            已选 {{ selectedDepartmentIds.length }} 个院系，涵盖 {{ filteredClasses.length }} 个班级
          </div>
        </div>

        <!-- 按年级 -->
        <div v-if="targetScopeType === 'grade'" class="scope-select-area">
          <div class="select-label">
            <GraduationCap :size="12" />
            选择年级
          </div>
          <div class="select-chips">
            <div
              v-for="grade in grades"
              :key="grade.id"
              class="select-chip"
              :class="{ selected: selectedGradeIds.includes(grade.id) }"
              @click="toggleGrade(grade.id)"
            >
              <CheckCircle v-if="selectedGradeIds.includes(grade.id)" :size="12" />
              <span>{{ grade.gradeName }}</span>
            </div>
          </div>
          <div class="select-info" v-if="selectedGradeIds.length > 0">
            已选 {{ selectedGradeIds.length }} 个年级，涵盖 {{ filteredClasses.length }} 个班级
          </div>
        </div>

        <!-- 自定义 -->
        <div v-if="targetScopeType === 'custom'" class="scope-select-area">
          <div class="select-row">
            <div class="select-col">
              <div class="select-label">
                <Building2 :size="12" />
                按院系筛选（可选）
              </div>
              <div class="select-chips small">
                <div
                  v-for="dept in flatDepartments"
                  :key="dept.id"
                  class="select-chip small"
                  :class="{ selected: customFilterDeptIds.includes(dept.id) }"
                  @click="toggleCustomFilterDept(dept.id)"
                >
                  <span>{{ dept.deptName }}</span>
                </div>
              </div>
            </div>
            <div class="select-col">
              <div class="select-label">
                <GraduationCap :size="12" />
                按年级筛选（可选）
              </div>
              <div class="select-chips small">
                <div
                  v-for="grade in grades"
                  :key="grade.id"
                  class="select-chip small"
                  :class="{ selected: customFilterGradeIds.includes(grade.id) }"
                  @click="toggleCustomFilterGrade(grade.id)"
                >
                  <span>{{ grade.gradeName }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="select-label" style="margin-top: 12px;">
            <Users :size="12" />
            选择班级
            <button class="btn-select-all" @click="selectAllFilteredClasses">全选</button>
            <button class="btn-select-all" @click="clearSelectedClasses">清空</button>
          </div>
          <div class="class-select-area">
            <div
              v-for="cls in customFilteredClasses"
              :key="cls.id"
              class="class-chip"
              :class="{ selected: selectedClassIds.includes(cls.id) }"
              @click="toggleClass(cls.id)"
            >
              <CheckCircle v-if="selectedClassIds.includes(cls.id)" :size="10" />
              <span>{{ cls.className }}</span>
            </div>
            <div v-if="customFilteredClasses.length === 0" class="empty-classes">
              暂无匹配的班级
            </div>
          </div>
          <div class="select-info" v-if="selectedClassIds.length > 0">
            已选 {{ selectedClassIds.length }} 个班级
          </div>
        </div>

        <!-- 排除班级 -->
        <div v-if="targetScopeType !== 'all' && targetScopeType !== 'custom' && filteredClasses.length > 0" class="exclude-area">
          <div class="select-label">
            <XCircle :size="12" />
            排除班级（可选）
          </div>
          <div class="select-chips small">
            <div
              v-for="cls in filteredClasses"
              :key="cls.id"
              class="select-chip small exclude"
              :class="{ selected: excludeClassIds.includes(cls.id) }"
              @click="toggleExcludeClass(cls.id)"
            >
              <XCircle v-if="excludeClassIds.includes(cls.id)" :size="10" />
              <span>{{ cls.className }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 左侧：模板选择与结构 -->
      <section class="left-panel">
        <!-- 模板选择 -->
        <div class="panel-section">
          <div class="section-header">
            <span class="section-title"><LayoutTemplate :size="14" /> 检查模板</span>
            <div class="search-box" v-if="templates.length > 5">
              <Search :size="12" />
              <input v-model="templateSearch" type="text" placeholder="搜索模板..." />
            </div>
          </div>
          <div class="template-grid">
            <div v-if="loadingTemplates" class="loading-hint">
              <Loader2 :size="16" class="spin" /> 加载中...
            </div>
            <div v-else-if="filteredTemplates.length === 0" class="empty-hint">
              <Inbox :size="20" /> 暂无模板
            </div>
            <template v-else>
              <div
                v-for="t in filteredTemplates"
                :key="t.id"
                class="template-chip"
                :class="{ active: form.templateId === t.id }"
                @click="selectTemplate(t)"
              >
                <CheckCircle v-if="form.templateId === t.id" :size="12" class="chip-check" />
                <span class="chip-name">{{ t.templateName }}</span>
                <span class="chip-meta">{{ getItemCount(t) }}项</span>
              </div>
            </template>
          </div>
        </div>

        <!-- 模板结构 -->
        <div class="panel-section structure-section" v-if="selectedTemplate">
          <div class="section-header">
            <span class="section-title"><FolderTree :size="14" /> 模板结构</span>
            <span class="section-badge">{{ getTotalItems() }}项</span>
          </div>

          <!-- 计划级加权设置 -->
          <div
            class="plan-weight-row"
            :class="{ 'has-weight': planWeightConfig, 'drag-over': dragOverTarget === 'plan-plan' }"
            @dragover.prevent="onDragOver($event, 'plan')"
            @dragleave="onDragLeave"
            @drop="onDrop($event, 'plan')"
          >
            <div class="pw-left">
              <Settings :size="14" />
              <span>计划默认加权</span>
            </div>
            <div class="pw-right">
              <div
                v-if="planWeightConfig"
                class="weight-tag"
                :style="{ background: planWeightConfig.color }"
              >
                {{ planWeightConfig.configName }}
                <button class="tag-remove" @click.stop="removePlanWeight">
                  <X :size="10" />
                </button>
              </div>
              <span v-else class="pw-hint">拖拽方案到此处</span>
            </div>
          </div>

          <!-- 类别和扣分项树 -->
          <div class="structure-tree">
            <div v-for="cat in selectedTemplate.categories" :key="cat.categoryId" class="tree-category">
              <!-- 类别行 -->
              <div
                class="category-row"
                :class="{
                  'has-weight': getCategoryWeight(cat.categoryId),
                  'drag-over': dragOverTarget === `category-${cat.categoryId}`
                }"
                @click="toggleCategory(cat.categoryId)"
                @dragover.prevent="onDragOver($event, 'category', cat.categoryId)"
                @dragleave="onDragLeave"
                @drop="onDrop($event, 'category', cat.categoryId)"
              >
                <div class="cat-left">
                  <ChevronRight
                    :size="12"
                    class="expand-icon"
                    :class="{ expanded: expandedCategories.has(cat.categoryId) }"
                  />
                  <FolderOpen :size="14" class="folder-icon" />
                  <span class="cat-name">{{ cat.categoryName }}</span>
                  <span v-if="cat.isRequired === 1" class="tag-required">必检</span>
                </div>
                <div class="cat-right">
                  <span class="cat-count">{{ cat.deductionItems?.length || 0 }}</span>
                  <div
                    v-if="getCategoryWeight(cat.categoryId)"
                    class="weight-tag small"
                    :style="{ background: getCategoryWeight(cat.categoryId)?.color }"
                    @click.stop
                  >
                    {{ getCategoryWeight(cat.categoryId)?.configName }}
                    <button class="tag-remove" @click.stop="removeCategoryWeight(cat.categoryId)">
                      <X :size="8" />
                    </button>
                  </div>
                  <span v-else-if="planWeightConfig" class="inherit-hint">继承</span>
                </div>
              </div>

              <!-- 扣分项卡片列表 -->
              <div v-show="expandedCategories.has(cat.categoryId)" class="items-grid">
                <div
                  v-for="item in cat.deductionItems"
                  :key="item.id"
                  class="item-card"
                  :class="{
                    'has-weight': getEffectiveWeight(item.id, cat.categoryId),
                    'drag-over': dragOverTarget === `item-${item.id}`
                  }"
                  :style="getItemCardStyle(item.id, cat.categoryId)"
                  @dragover.prevent="onDragOver($event, 'item', item.id)"
                  @dragleave="onDragLeave"
                  @drop="onDrop($event, 'item', item.id, cat.categoryId)"
                >
                  <div class="item-card-header" :style="getItemHeaderStyle(item.id, cat.categoryId)">
                    <span class="item-card-name">{{ item.itemName }}</span>
                    <span class="item-score-badge" :class="getScoreType(item)">{{ formatScore(item) }}</span>
                  </div>
                  <div class="item-card-footer">
                    <!-- 扣分项自身设置的加权 -->
                    <div
                      v-if="getItemWeight(item.id)"
                      class="item-weight-tag"
                      :style="{ background: getItemWeight(item.id)?.color }"
                    >
                      <Gauge :size="10" />
                      {{ getItemWeight(item.id)?.configName }}
                      <button class="item-tag-remove" @click.stop="removeItemWeight(item.id)">
                        <X :size="8" />
                      </button>
                    </div>
                    <!-- 扣分项被标记为不继承 -->
                    <div
                      v-else-if="isItemNoWeight(item.id)"
                      class="item-no-weight-tag"
                      @click.stop="restoreItemInherit(item.id)"
                    >
                      <XCircle :size="10" />
                      不加权
                      <span class="restore-hint">点击恢复</span>
                    </div>
                    <!-- 继承自类别 -->
                    <div
                      v-else-if="getCategoryWeight(cat.categoryId)"
                      class="item-weight-tag inherited"
                      :style="{ background: getCategoryWeight(cat.categoryId)?.color }"
                    >
                      <ArrowDown :size="10" />
                      {{ getCategoryWeight(cat.categoryId)?.configName }}
                      <button class="item-tag-remove" @click.stop="cancelItemInherit(item.id)" title="取消继承">
                        <X :size="8" />
                      </button>
                    </div>
                    <!-- 继承自计划 -->
                    <div
                      v-else-if="planWeightConfig"
                      class="item-weight-tag inherited"
                      :style="{ background: planWeightConfig.color }"
                    >
                      <ArrowDown :size="10" />
                      {{ planWeightConfig.configName }}
                      <button class="item-tag-remove" @click.stop="cancelItemInherit(item.id)" title="取消继承">
                        <X :size="8" />
                      </button>
                    </div>
                    <!-- 未配置 -->
                    <div v-else class="item-empty-tag">
                      <Target :size="10" />
                      未配置
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 未选择模板 -->
        <div v-else class="panel-section empty-section">
          <MousePointer :size="28" />
          <span>请先选择检查模板</span>
        </div>
      </section>

      <!-- 右侧：加权方案工作区 -->
      <section class="right-panel">
        <div class="panel-section">
          <div class="section-header">
            <span class="section-title"><Scale :size="14" /> 加权方案</span>
            <button class="btn-add-scheme" @click="showSchemeSelector = true">
              <Plus :size="14" /> 添加方案
            </button>
          </div>

          <!-- 已添加的方案（工作区） -->
          <div class="workspace-schemes">
            <div v-if="workspaceSchemes.length === 0" class="empty-workspace">
              <Scale :size="24" />
              <span>点击上方按钮添加加权方案</span>
              <span class="hint">添加后可拖拽到左侧进行配置</span>
            </div>
            <div
              v-for="scheme in workspaceSchemes"
              :key="scheme.id"
              class="scheme-card"
              :style="{ borderColor: scheme.color }"
              draggable="true"
              @dragstart="onDragStart($event, scheme)"
              @dragend="onDragEnd"
            >
              <div class="scheme-header" :style="{ background: scheme.color }">
                <Gauge :size="14" />
                <span>{{ scheme.configName }}</span>
              </div>
              <div class="scheme-body">
                <div class="scheme-info">
                  <span class="info-label">模式</span>
                  <span class="info-value">{{ getModeLabel(scheme.weightMode) }}</span>
                </div>
                <div class="scheme-info">
                  <span class="info-label">标准人数</span>
                  <span v-if="scheme.standardSizeMode === 'TARGET_AVERAGE'" class="info-value text-blue-500">动态计算</span>
                  <span v-else-if="scheme.standardSize" class="info-value">{{ scheme.standardSize }}人</span>
                  <span v-else class="info-value">-</span>
                </div>
                <div v-if="scheme.minWeight" class="scheme-info">
                  <span class="info-label">权重范围</span>
                  <span class="info-value">{{ scheme.minWeight }}~{{ scheme.maxWeight }}</span>
                </div>
              </div>
              <div class="scheme-footer">
                <span class="drag-hint"><GripVertical :size="12" /> 拖拽配置</span>
                <button class="btn-remove-scheme" @click="removeFromWorkspace(scheme.id)">
                  <Trash2 :size="12" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 配置统计 -->
        <div class="panel-section stats-section" v-if="selectedTemplate">
          <div class="section-header">
            <span class="section-title"><BarChart3 :size="14" /> 配置统计</span>
          </div>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-value">{{ getTotalItems() }}</span>
              <span class="stat-label">扣分项</span>
            </div>
            <div class="stat-item">
              <span class="stat-value highlight">{{ getConfiguredCount() }}</span>
              <span class="stat-label">已配置</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ getInheritCount() }}</span>
              <span class="stat-label">继承</span>
            </div>
          </div>
          <div class="stats-bar">
            <div class="bar-fill" :style="{ width: getConfigPercent() + '%' }"></div>
          </div>
          <span class="stats-percent">{{ getConfigPercent() }}% 已配置加权</span>
        </div>
      </section>
    </main>

    <!-- 方案选择弹窗 -->
    <div v-if="showSchemeSelector" class="modal-overlay" @click.self="showSchemeSelector = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3><Scale :size="16" /> 选择加权方案</h3>
          <button class="btn-close" @click="showSchemeSelector = false">
            <X :size="18" />
          </button>
        </div>
        <div class="modal-body">
          <div class="scheme-search" v-if="allWeightConfigs.length > 5">
            <Search :size="14" />
            <input v-model="schemeSearch" type="text" placeholder="搜索方案..." />
          </div>
          <div class="scheme-list">
            <div v-if="allWeightConfigs.length === 0" class="empty-hint">
              <Scale :size="24" />
              <span>暂无可用加权方案</span>
            </div>
            <div
              v-for="cfg in filteredSchemes"
              :key="cfg.id"
              class="scheme-option"
              :class="{ selected: isInWorkspace(cfg.id), disabled: isInWorkspace(cfg.id) }"
              @click="addToWorkspace(cfg)"
            >
              <div class="opt-color" :style="{ background: getSchemeColor(cfg.id) }"></div>
              <div class="opt-info">
                <span class="opt-name">{{ cfg.configName }}</span>
                <span class="opt-mode">{{ getModeLabel(cfg.weightMode) }}</span>
              </div>
              <div class="opt-check">
                <CheckCircle v-if="isInWorkspace(cfg.id)" :size="16" />
                <Plus v-else :size="16" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Check, Loader2, FileText, CalendarDays, MessageSquare,
  LayoutTemplate, ClipboardList, ChevronRight, FolderOpen, FolderTree,
  Scale, MousePointer, Plus, X, Gauge, GripVertical, Trash2,
  BarChart3, Search, Inbox, Settings, CheckCircle, ArrowDown, Target,
  Users, Building2, GraduationCap, XCircle, Globe, Layers
} from 'lucide-vue-next'
import { createCheckPlan } from '@/api/v2/quantification'
import type { CheckPlanCreateRequest, TargetScopeConfig } from '@/api/v2/quantification'
import { getAllCheckTemplates } from '@/api/v2/quantification'
import { getDepartmentTree } from '@/api/v2/organization'
import { getGradePage } from '@/api/v2/organization'
import { getClassList } from '@/api/v2/organization'
import { http } from '@/utils/request'

// 颜色池
const SCHEME_COLORS = [
  '#6366f1', '#8b5cf6', '#ec4899', '#f43f5e', '#f97316',
  '#eab308', '#22c55e', '#14b8a6', '#06b6d4', '#3b82f6'
]

// 目标范围类型选项
const scopeTypeOptions = [
  { value: 'all', label: '全部班级', icon: Globe },
  { value: 'department', label: '按院系', icon: Building2 },
  { value: 'grade', label: '按年级', icon: GraduationCap },
  { value: 'custom', label: '自定义', icon: Layers }
]

interface DeductionItem {
  id: string | number
  itemName: string
  deductMode: number
  fixedScore?: number
  baseScore?: number
  perPersonScore?: number
}

interface Category {
  categoryId: string | number
  categoryName: string
  isRequired?: number
  deductionItems?: DeductionItem[]
}

interface Template {
  id: string | number
  templateName: string
  status?: number
  categories?: Category[]
}

interface WeightConfig {
  id: string | number
  configName: string
  weightMode?: string
  standardSizeMode?: string
  standardSize?: number
  minWeight?: number
  maxWeight?: number
  color?: string
}

const router = useRouter()

// 状态
const loadingTemplates = ref(false)
const templates = ref<Template[]>([])
const allWeightConfigs = ref<WeightConfig[]>([])
const workspaceSchemes = ref<WeightConfig[]>([])
const expandedCategories = ref<Set<string | number>>(new Set())
const submitting = ref(false)
const showSchemeSelector = ref(false)
const templateSearch = ref('')
const schemeSearch = ref('')

// 目标范围相关
const targetScopeType = ref('all')  // all/department/grade/custom
const departments = ref<any[]>([])
const grades = ref<any[]>([])
const classes = ref<any[]>([])
const selectedDepartmentIds = ref<(string | number)[]>([])
const selectedGradeIds = ref<(string | number)[]>([])
const selectedClassIds = ref<(string | number)[]>([])
const excludeClassIds = ref<(string | number)[]>([])
const loadingTargetData = ref(false)
// 自定义模式下的筛选条件
const customFilterDeptIds = ref<(string | number)[]>([])
const customFilterGradeIds = ref<(string | number)[]>([])

// 加权配置映射
const planWeightConfig = ref<WeightConfig | null>(null)
const categoryWeightMap = ref<Map<string | number, WeightConfig>>(new Map())
const itemWeightMap = ref<Map<string | number, WeightConfig>>(new Map())
// 不继承加权的扣分项ID集合（用于取消继承）
const noWeightItemIds = ref<Set<string | number>>(new Set())

// 拖拽状态
const draggedScheme = ref<WeightConfig | null>(null)
const dragOverTarget = ref<string | null>(null)

// 表单
const form = reactive<CheckPlanCreateRequest>({
  planName: '',
  description: '',
  templateId: '',
  startDate: '',
  endDate: ''
})

// 计算属性
const selectedTemplate = computed(() => templates.value.find(t => t.id === form.templateId))

const canSubmit = computed(() => {
  return form.templateId && form.planName && form.startDate && form.endDate && form.startDate <= form.endDate
})

const filteredTemplates = computed(() => {
  if (!templateSearch.value) return templates.value
  const kw = templateSearch.value.toLowerCase()
  return templates.value.filter(t => t.templateName.toLowerCase().includes(kw))
})

const filteredSchemes = computed(() => {
  if (!schemeSearch.value) return allWeightConfigs.value
  const kw = schemeSearch.value.toLowerCase()
  return allWeightConfigs.value.filter(c => c.configName.toLowerCase().includes(kw))
})

// 方法
const loadTemplates = async () => {
  loadingTemplates.value = true
  try {
    const res = await getAllCheckTemplates()
    templates.value = ((res || []) as Template[]).filter(t => t.status === 1)
  } catch (e) {
    console.error(e)
  } finally {
    loadingTemplates.value = false
  }
}

const loadWeightConfigs = async () => {
  try {
    const res = await http.get<any>('/quantification/weight-config/configs', {
      params: { pageNum: 1, pageSize: 100, enableWeight: 1 }
    })
    allWeightConfigs.value = (res?.records || []).map((cfg: any, idx: number) => ({
      ...cfg,
      color: SCHEME_COLORS[idx % SCHEME_COLORS.length]
    }))
  } catch (e) {
    console.error(e)
  }
}

// 加载目标范围数据（院系、年级、班级）
const loadTargetData = async () => {
  loadingTargetData.value = true
  try {
    const [deptRes, gradeRes, classRes] = await Promise.all([
      getDepartmentTree(),
      getGradePage({ pageNum: 1, pageSize: 100 }),
      getClassList({ pageNum: 1, pageSize: 1000 })
    ])
    // API响应拦截器已经返回data，不需要再访问.data
    departments.value = deptRes || []
    grades.value = gradeRes?.records || []
    classes.value = classRes?.records || []
  } catch (e) {
    console.error('加载目标数据失败:', e)
  } finally {
    loadingTargetData.value = false
  }
}

// 递归获取部门及其所有子部门ID
const getAllDescendantDeptIds = (deptId: string | number, deptTree: any[]): (string | number)[] => {
  const result: (string | number)[] = [deptId]
  const findAndCollect = (items: any[]) => {
    for (const item of items) {
      if (item.id === deptId) {
        // 找到目标部门，收集所有子部门ID
        const collectChildren = (children: any[]) => {
          children?.forEach(child => {
            result.push(child.id)
            if (child.children?.length) {
              collectChildren(child.children)
            }
          })
        }
        collectChildren(item.children || [])
        return true
      }
      if (item.children?.length && findAndCollect(item.children)) {
        return true
      }
    }
    return false
  }
  findAndCollect(deptTree)
  return result
}

// 获取所有选中部门及其子部门的ID集合
const allSelectedDeptIds = computed(() => {
  const allIds = new Set<string | number>()
  selectedDepartmentIds.value.forEach(deptId => {
    getAllDescendantDeptIds(deptId, departments.value).forEach(id => allIds.add(id))
  })
  return allIds
})

// 获取过滤后的班级列表（根据选择的院系和年级，包含子部门）
const filteredClasses = computed(() => {
  let result = classes.value
  if (targetScopeType.value === 'department' && selectedDepartmentIds.value.length > 0) {
    // 使用包含子部门的ID集合进行过滤
    result = result.filter(c => allSelectedDeptIds.value.has(c.departmentId))
  }
  if (targetScopeType.value === 'grade' && selectedGradeIds.value.length > 0) {
    result = result.filter(c => selectedGradeIds.value.includes(c.gradeId))
  }
  return result
})

// 扁平化的部门列表（用于选择）
const flatDepartments = computed(() => {
  const result: any[] = []
  const flatten = (items: any[], level = 0) => {
    items.forEach(item => {
      result.push({ ...item, level })
      if (item.children?.length) {
        flatten(item.children, level + 1)
      }
    })
  }
  flatten(departments.value)
  return result
})

// 自定义模式下筛选后的班级列表
const customFilteredClasses = computed(() => {
  let result = classes.value
  if (customFilterDeptIds.value.length > 0) {
    result = result.filter(c => customFilterDeptIds.value.includes(c.departmentId))
  }
  if (customFilterGradeIds.value.length > 0) {
    result = result.filter(c => customFilterGradeIds.value.includes(c.gradeId))
  }
  return result
})

// 目标范围类型变更
const onTargetScopeTypeChange = () => {
  selectedDepartmentIds.value = []
  selectedGradeIds.value = []
  selectedClassIds.value = []
  excludeClassIds.value = []
  customFilterDeptIds.value = []
  customFilterGradeIds.value = []
}

// 切换院系选择
const toggleDepartment = (id: string | number) => {
  const idx = selectedDepartmentIds.value.indexOf(id)
  if (idx >= 0) {
    selectedDepartmentIds.value.splice(idx, 1)
  } else {
    selectedDepartmentIds.value.push(id)
  }
  excludeClassIds.value = []
}

// 切换年级选择
const toggleGrade = (id: string | number) => {
  const idx = selectedGradeIds.value.indexOf(id)
  if (idx >= 0) {
    selectedGradeIds.value.splice(idx, 1)
  } else {
    selectedGradeIds.value.push(id)
  }
  excludeClassIds.value = []
}

// 切换班级选择
const toggleClass = (id: string | number) => {
  const idx = selectedClassIds.value.indexOf(id)
  if (idx >= 0) {
    selectedClassIds.value.splice(idx, 1)
  } else {
    selectedClassIds.value.push(id)
  }
}

// 切换排除班级
const toggleExcludeClass = (id: string | number) => {
  const idx = excludeClassIds.value.indexOf(id)
  if (idx >= 0) {
    excludeClassIds.value.splice(idx, 1)
  } else {
    excludeClassIds.value.push(id)
  }
}

// 自定义模式-切换院系筛选
const toggleCustomFilterDept = (id: string | number) => {
  const idx = customFilterDeptIds.value.indexOf(id)
  if (idx >= 0) {
    customFilterDeptIds.value.splice(idx, 1)
  } else {
    customFilterDeptIds.value.push(id)
  }
}

// 自定义模式-切换年级筛选
const toggleCustomFilterGrade = (id: string | number) => {
  const idx = customFilterGradeIds.value.indexOf(id)
  if (idx >= 0) {
    customFilterGradeIds.value.splice(idx, 1)
  } else {
    customFilterGradeIds.value.push(id)
  }
}

// 全选筛选后的班级
const selectAllFilteredClasses = () => {
  selectedClassIds.value = customFilteredClasses.value.map(c => c.id)
}

// 清空班级选择
const clearSelectedClasses = () => {
  selectedClassIds.value = []
}

const selectTemplate = (t: Template) => {
  form.templateId = t.id as string
  expandedCategories.value.clear()
  categoryWeightMap.value.clear()
  itemWeightMap.value.clear()
  noWeightItemIds.value.clear()
  t.categories?.forEach(c => expandedCategories.value.add(c.categoryId))
}

const toggleCategory = (id: string | number) => {
  expandedCategories.value.has(id)
    ? expandedCategories.value.delete(id)
    : expandedCategories.value.add(id)
}

const getItemCount = (t: Template) => {
  return t.categories?.reduce((s, c) => s + (c.deductionItems?.length || 0), 0) || 0
}

const getTotalItems = () => {
  return selectedTemplate.value?.categories?.reduce((s, c) => s + (c.deductionItems?.length || 0), 0) || 0
}

const getScoreType = (item: DeductionItem) => {
  return { fixed: item.deductMode === 1, dynamic: item.deductMode === 2, range: item.deductMode === 3 }
}

const formatScore = (item: DeductionItem) => {
  if (item.deductMode === 1) return `-${item.fixedScore || 0}`
  if (item.deductMode === 2) return `${item.baseScore || 0}+${item.perPersonScore || 0}/人`
  return '区间'
}

const getModeLabel = (mode?: string) => {
  const map: Record<string, string> = {
    'STANDARD': '标准折算', 'PER_CAPITA': '人均', 'SEGMENT': '分段', 'NONE': '无'
  }
  return map[mode || ''] || '标准'
}

const getSchemeColor = (id: string | number) => {
  const cfg = allWeightConfigs.value.find(c => c.id === id)
  return cfg?.color || SCHEME_COLORS[0]
}

const isInWorkspace = (id: string | number) => {
  return workspaceSchemes.value.some(s => s.id === id)
}

const addToWorkspace = (cfg: WeightConfig) => {
  if (isInWorkspace(cfg.id)) return
  workspaceSchemes.value.push({ ...cfg, color: getSchemeColor(cfg.id) })
}

const removeFromWorkspace = (id: string | number) => {
  workspaceSchemes.value = workspaceSchemes.value.filter(s => s.id !== id)
  // 清除使用该方案的配置
  if (planWeightConfig.value?.id === id) planWeightConfig.value = null
  categoryWeightMap.value.forEach((v, k) => { if (v.id === id) categoryWeightMap.value.delete(k) })
  itemWeightMap.value.forEach((v, k) => { if (v.id === id) itemWeightMap.value.delete(k) })
}

// 拖拽处理
const onDragStart = (e: DragEvent, scheme: WeightConfig) => {
  draggedScheme.value = scheme
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'copy'
    e.dataTransfer.setData('text/plain', String(scheme.id))
  }
}

const onDragEnd = () => {
  draggedScheme.value = null
  dragOverTarget.value = null
}

const onDragOver = (e: DragEvent, type: string, id?: string | number) => {
  e.preventDefault()
  dragOverTarget.value = `${type}-${id || 'plan'}`
}

const onDragLeave = () => {
  dragOverTarget.value = null
}

const onDrop = (e: DragEvent, type: string, id?: string | number, categoryId?: string | number) => {
  e.preventDefault()
  if (!draggedScheme.value) return

  if (type === 'plan') {
    planWeightConfig.value = draggedScheme.value
    // 清除所有类别和扣分项的单独设置，改为继承计划
    categoryWeightMap.value.clear()
    itemWeightMap.value.clear()
  } else if (type === 'category' && id) {
    categoryWeightMap.value.set(id, draggedScheme.value)
    // 清除该类别下所有扣分项的单独设置，改为继承类别
    const cat = selectedTemplate.value?.categories?.find(c => c.categoryId === id)
    cat?.deductionItems?.forEach(item => {
      itemWeightMap.value.delete(item.id)
    })
  } else if (type === 'item' && id) {
    itemWeightMap.value.set(id, draggedScheme.value)
  }

  dragOverTarget.value = null
}

// 获取加权配置
const getCategoryWeight = (catId: string | number) => categoryWeightMap.value.get(catId)
const getItemWeight = (itemId: string | number) => itemWeightMap.value.get(itemId)
const isItemNoWeight = (itemId: string | number) => noWeightItemIds.value.has(itemId)

// 获取有效的加权方案（考虑继承和取消继承）
const getEffectiveWeight = (itemId: string | number, catId: string | number) => {
  // 如果该扣分项设置为不继承，返回null
  if (noWeightItemIds.value.has(itemId)) return null
  return getItemWeight(itemId) || getCategoryWeight(catId) || planWeightConfig.value
}

// 获取卡片样式（根据加权方案颜色）
const getItemCardStyle = (itemId: string | number, catId: string | number) => {
  const weight = getEffectiveWeight(itemId, catId)
  if (weight?.color) {
    return {
      borderColor: weight.color,
      borderLeftWidth: '3px'
    }
  }
  return {}
}

// 获取卡片头部样式
const getItemHeaderStyle = (itemId: string | number, catId: string | number) => {
  const weight = getEffectiveWeight(itemId, catId)
  if (weight?.color) {
    return {
      background: `${weight.color}15`,
      borderBottom: `1px solid ${weight.color}30`
    }
  }
  return {}
}

// 移除加权配置
const removePlanWeight = () => { planWeightConfig.value = null }
const removeCategoryWeight = (catId: string | number) => { categoryWeightMap.value.delete(catId) }
const removeItemWeight = (itemId: string | number) => {
  itemWeightMap.value.delete(itemId)
  noWeightItemIds.value.delete(itemId)
}
// 取消继承（标记扣分项为不继承加权）
const cancelItemInherit = (itemId: string | number) => {
  itemWeightMap.value.delete(itemId)
  noWeightItemIds.value.add(itemId)
}
// 恢复继承
const restoreItemInherit = (itemId: string | number) => {
  noWeightItemIds.value.delete(itemId)
}

// 统计
const getConfiguredCount = () => {
  let count = 0
  selectedTemplate.value?.categories?.forEach(cat => {
    cat.deductionItems?.forEach(item => {
      if (itemWeightMap.value.has(item.id)) count++
    })
  })
  return count
}

const getInheritCount = () => {
  let count = 0
  selectedTemplate.value?.categories?.forEach(cat => {
    const catWeight = categoryWeightMap.value.has(cat.categoryId)
    cat.deductionItems?.forEach(item => {
      // 排除被标记为不继承的扣分项
      if (noWeightItemIds.value.has(item.id)) return
      if (!itemWeightMap.value.has(item.id) && (catWeight || planWeightConfig.value)) count++
    })
  })
  return count
}

const getConfigPercent = () => {
  const total = getTotalItems()
  if (!total) return 0
  const configured = getConfiguredCount() + getInheritCount()
  return Math.round((configured / total) * 100)
}

const goBack = () => router.push('/quantification/check-plan')

const handleSubmit = async () => {
  if (submitting.value || !canSubmit.value) return
  if (form.startDate > form.endDate) {
    ElMessage.warning('日期范围错误')
    return
  }

  submitting.value = true
  try {
    // 构建加权配置（排除被标记为不继承的扣分项）
    const itemConfigs: any[] = []
    selectedTemplate.value?.categories?.forEach(cat => {
      const catWeight = categoryWeightMap.value.get(cat.categoryId)
      cat.deductionItems?.forEach(item => {
        // 如果该扣分项被标记为不继承，跳过
        if (noWeightItemIds.value.has(item.id)) return
        const itemWeight = itemWeightMap.value.get(item.id)
        const effectiveWeight = itemWeight || catWeight || planWeightConfig.value
        if (effectiveWeight) {
          itemConfigs.push({
            itemId: item.id,
            categoryId: cat.categoryId,
            weightConfigId: effectiveWeight.id
          })
        }
      })
    })

    // 构建目标范围配置
    let scopeConfig: TargetScopeConfig | undefined
    if (targetScopeType.value === 'department' && selectedDepartmentIds.value.length > 0) {
      scopeConfig = {
        departmentIds: selectedDepartmentIds.value,
        excludeClassIds: excludeClassIds.value.length > 0 ? excludeClassIds.value : undefined
      }
    } else if (targetScopeType.value === 'grade' && selectedGradeIds.value.length > 0) {
      scopeConfig = {
        gradeIds: selectedGradeIds.value,
        excludeClassIds: excludeClassIds.value.length > 0 ? excludeClassIds.value : undefined
      }
    } else if (targetScopeType.value === 'custom' && selectedClassIds.value.length > 0) {
      scopeConfig = {
        classIds: selectedClassIds.value
      }
    }

    const res = await createCheckPlan({
      planName: form.planName,
      description: form.description || undefined,
      templateId: form.templateId,
      startDate: form.startDate,
      endDate: form.endDate,
      enableWeight: itemConfigs.length > 0 ? 1 : 0,
      weightConfigId: planWeightConfig.value?.id,
      itemWeightConfigs: itemConfigs.length > 0 ? itemConfigs : undefined,
      targetScopeType: targetScopeType.value,
      targetScopeConfig: scopeConfig
    })

    ElMessage.success('创建成功')
    router.push(res?.id ? `/quantification/check-plan/${res.id}` : '/quantification/check-plan')
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadTemplates()
  loadWeightConfigs()
  loadTargetData()
  const today = new Date()
  form.startDate = today.toISOString().split('T')[0]
  form.endDate = new Date(today.getTime() + 7 * 86400000).toISOString().split('T')[0]
})
</script>

<style scoped>
* { box-sizing: border-box; }

.page-container {
  min-height: 100vh;
  background: #f1f5f9;
  display: flex;
  flex-direction: column;
}

/* 顶部栏 */
.top-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.top-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: #f1f5f9;
  border-radius: 6px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-back:hover {
  background: #6366f1;
  color: #fff;
}

.page-title h1 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.breadcrumb {
  font-size: 11px;
  color: #94a3b8;
}

.top-right {
  display: flex;
  gap: 10px;
}

.btn-cancel {
  padding: 6px 14px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-cancel:hover {
  border-color: #94a3b8;
}

.btn-submit {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 16px;
  border: none;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 基本信息栏 */
.info-bar {
  display: flex;
  gap: 12px;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.info-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-field.flex-1 { flex: 1; }
.info-field.flex-2 { flex: 2; }

.info-field label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
  text-transform: uppercase;
}

.info-field label em {
  color: #ef4444;
  font-style: normal;
}

.info-field input {
  padding: 7px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  color: #1e293b;
  background: #f8fafc;
  transition: all 0.15s;
}

.info-field input:focus {
  outline: none;
  border-color: #6366f1;
  background: #fff;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

/* 目标范围选择栏 */
.target-scope-bar {
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  padding: 12px 20px;
}

.scope-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.scope-header svg {
  color: #6366f1;
}

.scope-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.scope-hint {
  font-size: 11px;
  color: #94a3b8;
}

.scope-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scope-type-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.scope-type-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.15s;
  font-size: 12px;
  color: #64748b;
}

.scope-type-item:hover {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.scope-type-item.active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-color: transparent;
  color: #fff;
}

.scope-type-item input {
  display: none;
}

.scope-select-area {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e2e8f0;
}

.select-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
  margin-bottom: 8px;
  text-transform: uppercase;
}

.select-label svg {
  color: #6366f1;
}

.select-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 120px;
  overflow-y: auto;
}

.select-chips.small {
  max-height: 80px;
}

.select-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.15s;
  font-size: 12px;
  color: #334155;
}

.select-chip:hover {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.select-chip.selected {
  background: #6366f1;
  border-color: #6366f1;
  color: #fff;
}

.select-chip.small {
  padding: 3px 8px;
  font-size: 11px;
}

.select-chip.exclude:hover {
  background: #fef2f2;
  border-color: #fecaca;
}

.select-chip.exclude.selected {
  background: #ef4444;
  border-color: #ef4444;
  color: #fff;
}

.select-info {
  margin-top: 8px;
  font-size: 11px;
  color: #22c55e;
  font-weight: 500;
}

.select-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.select-col {
  display: flex;
  flex-direction: column;
}

.class-select-area {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 150px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px;
}

.class-chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s;
  font-size: 11px;
  color: #334155;
}

.class-chip:hover {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.class-chip.selected {
  background: #6366f1;
  border-color: #6366f1;
  color: #fff;
}

.empty-classes {
  width: 100%;
  text-align: center;
  padding: 20px;
  color: #94a3b8;
  font-size: 12px;
}

.btn-select-all {
  margin-left: 8px;
  padding: 2px 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 4px;
  font-size: 10px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-select-all:hover {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
}

.exclude-area {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
  padding: 16px 20px;
  overflow: hidden;
}

/* 面板 */
.left-panel, .right-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.panel-section {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.section-title svg {
  color: #6366f1;
}

.section-badge {
  font-size: 11px;
  color: #64748b;
  background: #e2e8f0;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 搜索框 */
.search-box {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.search-box svg {
  color: #94a3b8;
}

.search-box input {
  border: none;
  outline: none;
  font-size: 12px;
  width: 120px;
  color: #1e293b;
}

/* 模板网格 */
.template-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  max-height: 120px;
  overflow-y: auto;
}

.loading-hint, .empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  color: #94a3b8;
  font-size: 12px;
  width: 100%;
}

.template-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.15s;
}

.template-chip:hover {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.template-chip.active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-color: transparent;
  color: #fff;
}

.template-chip.active .chip-name,
.template-chip.active .chip-meta {
  color: #fff;
}

.template-chip.active .chip-check {
  color: #fff;
}

.chip-check {
  color: #fff;
  flex-shrink: 0;
}

.chip-name {
  font-size: 12px;
  font-weight: 500;
  color: #334155;
  max-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chip-meta {
  font-size: 10px;
  color: #94a3b8;
  flex-shrink: 0;
}

/* 结构区 */
.structure-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 计划级加权 */
.plan-weight-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  transition: all 0.15s;
}

.plan-weight-row.has-weight {
  background: #fef3c7;
}

.plan-weight-row.drag-over {
  background: #dbeafe;
  border: 2px dashed #3b82f6;
}

.pw-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.pw-left svg {
  color: #f59e0b;
}

.pw-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pw-hint {
  font-size: 11px;
  color: #94a3b8;
  font-style: italic;
}

/* 加权标签 */
.weight-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  color: #fff;
}

.weight-tag.small {
  padding: 2px 6px;
  font-size: 10px;
}

.weight-tag.tiny {
  padding: 1px 5px;
  font-size: 9px;
}

.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border: none;
  background: rgba(255,255,255,0.3);
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  margin-left: 2px;
}

.tag-remove:hover {
  background: rgba(255,255,255,0.5);
}

/* 树结构 */
.structure-tree {
  position: relative;
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tree-category {
  position: relative;
  margin-bottom: 4px;
}

.category-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px dashed transparent;
}

.category-row:hover {
  background: #f8fafc;
}

.category-row.has-weight {
  background: #f0fdf4;
  border-color: #86efac;
}

.category-row.drag-over {
  background: #dbeafe;
  border: 2px dashed #3b82f6;
}

.cat-left {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.expand-icon {
  color: #94a3b8;
  transition: transform 0.15s;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.folder-icon {
  color: #f59e0b;
}

.cat-name {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-required {
  font-size: 9px;
  font-weight: 600;
  color: #ef4444;
  background: #fef2f2;
  padding: 1px 4px;
  border-radius: 3px;
}

.cat-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cat-count {
  font-size: 10px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 8px;
}

.inherit-hint {
  font-size: 10px;
  color: #94a3b8;
  font-style: italic;
}

/* 扣分项卡片网格 */
.items-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
  padding: 10px;
  margin-left: 18px;
  padding-left: 14px;
  border-left: 2px solid #e2e8f0;
  background: #fafbfc;
  border-radius: 0 8px 8px 0;
}

.item-card {
  position: relative;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s;
  cursor: default;
}

.item-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.item-card.has-weight {
  background: #fff;
}

.item-card.drag-over {
  border: 2px dashed #6366f1;
  background: #eef2ff;
}

.item-card-header {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 10px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
}

.item-card-name {
  flex: 1;
  min-width: 0;
  order: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
  font-weight: 500;
  color: #334155;
  line-height: 1.4;
}

.item-score-badge {
  position: relative;
  order: 2;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 10px;
  flex-shrink: 0;
  white-space: nowrap;
}

.item-score-badge.fixed {
  background: #fef2f2;
  color: #dc2626;
}

.item-score-badge.dynamic {
  background: #fffbeb;
  color: #d97706;
}

.item-score-badge.range {
  background: #eff6ff;
  color: #2563eb;
}

.item-card-footer {
  padding: 8px 10px;
  background: #fff;
}

.item-weight-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
}

.item-tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border: none;
  background: rgba(255,255,255,0.3);
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  margin-left: 2px;
  transition: all 0.15s;
}

.item-tag-remove:hover {
  background: rgba(255,255,255,0.5);
}

.item-inherit-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 500;
  background: #e5e7eb;
  color: #6b7280;
}

.item-empty-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 500;
  background: #fef3c7;
  color: #b45309;
}

.item-no-weight-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 500;
  background: #f1f5f9;
  color: #64748b;
  border: 1px dashed #cbd5e1;
  cursor: pointer;
  transition: all 0.15s;
}

.item-no-weight-tag:hover {
  background: #e0f2fe;
  border-color: #7dd3fc;
  color: #0284c7;
}

.item-no-weight-tag .restore-hint {
  font-size: 9px;
  opacity: 0.7;
  margin-left: 2px;
}

.item-weight-tag.inherited {
  opacity: 0.85;
}

/* 空状态 */
.empty-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #94a3b8;
  font-size: 13px;
}

/* 右侧面板 */
.btn-add-scheme {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px dashed #c7d2fe;
  background: #eef2ff;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  color: #6366f1;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-add-scheme:hover {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
}

/* 工作区 */
.workspace-schemes {
  padding: 10px;
  min-height: 200px;
}

.empty-workspace {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 20px;
  color: #94a3b8;
  font-size: 12px;
}

.empty-workspace .hint {
  font-size: 11px;
  color: #cbd5e1;
}

/* 方案卡片 */
.scheme-card {
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 10px;
  overflow: hidden;
  cursor: grab;
  transition: all 0.15s;
}

.scheme-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transform: translateY(-1px);
}

.scheme-card:active {
  cursor: grabbing;
}

.scheme-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.scheme-body {
  padding: 10px;
  background: #f8fafc;
}

.scheme-info {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  margin-bottom: 4px;
}

.info-label {
  color: #94a3b8;
}

.info-value {
  color: #475569;
  font-weight: 500;
}

.scheme-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: #f1f5f9;
  border-top: 1px solid #e2e8f0;
}

.drag-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: #94a3b8;
}

.btn-remove-scheme {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  background: #fef2f2;
  border-radius: 4px;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-remove-scheme:hover {
  background: #ef4444;
  color: #fff;
}

/* 统计 */
.stats-section {
  padding: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.stat-value.highlight {
  color: #22c55e;
}

.stat-label {
  font-size: 10px;
  color: #94a3b8;
}

.stats-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #4ade80);
  border-radius: 3px;
  transition: width 0.3s;
}

.stats-percent {
  font-size: 11px;
  color: #64748b;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  width: 420px;
  max-height: 80vh;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.2);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.modal-header h3 svg {
  color: #6366f1;
}

.btn-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: #f1f5f9;
  border-radius: 6px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-close:hover {
  background: #ef4444;
  color: #fff;
}

.modal-body {
  padding: 14px 18px;
  max-height: 60vh;
  overflow-y: auto;
}

.scheme-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 12px;
}

.scheme-search svg {
  color: #94a3b8;
}

.scheme-search input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 13px;
  background: transparent;
  color: #1e293b;
}

.scheme-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.scheme-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.scheme-option:hover:not(.disabled) {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.scheme-option.selected {
  background: #f0fdf4;
  border-color: #86efac;
}

.scheme-option.disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.opt-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
}

.opt-info {
  flex: 1;
}

.opt-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.opt-mode {
  font-size: 11px;
  color: #94a3b8;
}

.opt-check {
  color: #94a3b8;
}

.scheme-option.selected .opt-check {
  color: #22c55e;
}

/* 动画 */
.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 1024px) {
  .main-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .info-bar {
    flex-wrap: wrap;
  }

  .info-field.flex-1,
  .info-field.flex-2 {
    flex: 100%;
  }
}
</style>
