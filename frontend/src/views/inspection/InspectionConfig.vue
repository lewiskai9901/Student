<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Settings, FileText, Scale, Award, CalendarClock, ShieldAlert,
  Plus, Edit3, Trash2, Copy, ChevronDown, ChevronRight,
  GripVertical, Camera, Users, MessageSquare, ToggleLeft, ToggleRight,
  Check, X, Archive, Eye, Layers
} from 'lucide-vue-next'
import { ElSelect, ElOption, ElInput, ElSwitch, ElInputNumber } from 'element-plus'

// ─── Types ───
type ConfigTab = 'templates' | 'weights' | 'ratings' | 'schedule' | 'autoRules'
type ScoringMode = 'DEDUCTION_ONLY' | 'BASE_SCORE' | 'DUAL_TRACK'
type DeductMode = 'FIXED' | 'PER_PERSON' | 'RANGE'
type CheckMode = 'CHECKLIST' | 'FREE_DEDUCTION' | 'BOTH'
type SpaceType = 'DORMITORY' | 'CLASSROOM' | 'NONE'

interface DeductionItem {
  id: number
  name: string
  deductMode: DeductMode
  score: number
  maxScore?: number
  requirePhoto: boolean
  requireRemark: boolean
  requireStudent: boolean
  sortOrder: number
}

interface BonusItem {
  id: number
  name: string
  bonusMode: 'FIXED' | 'PROGRESSIVE' | 'IMPROVEMENT'
  fixedBonus: number
  description: string
}

interface Category {
  id: number
  name: string
  checkMode: CheckMode
  spaceType: SpaceType
  rounds: number[]
  expanded: boolean
  deductionItems: DeductionItem[]
  bonusItems: BonusItem[]
}

interface Template {
  id: number
  name: string
  code: string
  scoringMode: ScoringMode
  baseScore: number
  totalRounds: number
  roundNames: string[]
  categories: Category[]
  enabled: boolean
  useCount: number
  createdAt: string
}

interface WeightScheme {
  id: number
  name: string
  algorithm: string
  description: string
  enabled: boolean
}

interface RatingRule {
  id: number
  name: string
  method: string
  levels: { name: string; min: number; max: number; color: string }[]
  enabled: boolean
}

// ─── State ───
const activeTab = ref<ConfigTab>('templates')
const editingTemplate = ref(false)
const editTemplate = ref<Template | null>(null)

// ─── Mock Data ───
const templates: Template[] = [
  {
    id: 1, name: '日常宿舍卫生检查模板', code: 'TPL-001', scoringMode: 'BASE_SCORE', baseScore: 100,
    totalRounds: 3, roundNames: ['早检', '午检', '晚检'], enabled: true, useCount: 45, createdAt: '2026-01-15',
    categories: [
      {
        id: 1, name: '卫生检查', checkMode: 'CHECKLIST', spaceType: 'DORMITORY', rounds: [1, 2], expanded: true,
        deductionItems: [
          { id: 1, name: '地面不洁', deductMode: 'FIXED', score: 2.0, requirePhoto: true, requireRemark: true, requireStudent: false, sortOrder: 1 },
          { id: 2, name: '被褥未叠', deductMode: 'PER_PERSON', score: 0.5, requirePhoto: true, requireRemark: true, requireStudent: true, sortOrder: 2 },
          { id: 3, name: '违规电器', deductMode: 'FIXED', score: 5.0, requirePhoto: true, requireRemark: true, requireStudent: true, sortOrder: 3 },
          { id: 4, name: '物品摆放', deductMode: 'RANGE', score: 0, maxScore: 3, requirePhoto: true, requireRemark: true, requireStudent: false, sortOrder: 4 },
        ],
        bonusItems: [
          { id: 1, name: '主动整改', bonusMode: 'FIXED', fixedBonus: 2.0, description: '发现问题后主动整改' },
          { id: 2, name: '连续优秀', bonusMode: 'PROGRESSIVE', fixedBonus: 0, description: '连续3周+1, 5周+2, 8周+3' },
        ],
      },
      {
        id: 2, name: '纪律检查', checkMode: 'FREE_DEDUCTION', spaceType: 'NONE', rounds: [1, 2, 3], expanded: false,
        deductionItems: [
          { id: 5, name: '迟到', deductMode: 'FIXED', score: 2.0, requirePhoto: false, requireRemark: true, requireStudent: true, sortOrder: 1 },
          { id: 6, name: '课间打闹', deductMode: 'FIXED', score: 2.0, requirePhoto: false, requireRemark: true, requireStudent: true, sortOrder: 2 },
          { id: 7, name: '吸烟', deductMode: 'FIXED', score: 5.0, requirePhoto: true, requireRemark: true, requireStudent: true, sortOrder: 3 },
          { id: 8, name: '玩手机', deductMode: 'FIXED', score: 3.0, requirePhoto: false, requireRemark: true, requireStudent: true, sortOrder: 4 },
        ],
        bonusItems: [],
      },
    ],
  },
  {
    id: 2, name: '课堂纪律检查模板', code: 'TPL-002', scoringMode: 'DEDUCTION_ONLY', baseScore: 0,
    totalRounds: 1, roundNames: ['检查'], enabled: true, useCount: 23, createdAt: '2026-01-10',
    categories: [
      {
        id: 3, name: '课堂纪律', checkMode: 'FREE_DEDUCTION', spaceType: 'CLASSROOM', rounds: [1], expanded: false,
        deductionItems: [
          { id: 9, name: '课堂纪律差', deductMode: 'FIXED', score: 3.0, requirePhoto: false, requireRemark: true, requireStudent: false, sortOrder: 1 },
          { id: 10, name: '课间喧哗', deductMode: 'FIXED', score: 1.0, requirePhoto: false, requireRemark: true, requireStudent: false, sortOrder: 2 },
        ],
        bonusItems: [],
      },
      {
        id: 4, name: '考勤', checkMode: 'FREE_DEDUCTION', spaceType: 'NONE', rounds: [1], expanded: false,
        deductionItems: [
          { id: 11, name: '旷课', deductMode: 'PER_PERSON', score: 3.0, requirePhoto: false, requireRemark: true, requireStudent: true, sortOrder: 1 },
          { id: 12, name: '早退', deductMode: 'PER_PERSON', score: 2.0, requirePhoto: false, requireRemark: true, requireStudent: true, sortOrder: 2 },
        ],
        bonusItems: [],
      },
    ],
  },
]

const weightSchemes: WeightScheme[] = [
  { id: 1, name: '标准均衡权重', algorithm: 'CATEGORY_WEIGHTED', description: '各检查类别按配置权重加权计算', enabled: true },
  { id: 2, name: '重卫生权重', algorithm: 'CATEGORY_WEIGHTED', description: '卫生60% + 纪律25% + 考勤15%', enabled: false },
  { id: 3, name: '纯平均权重', algorithm: 'EQUAL_WEIGHTED', description: '所有类别等权重平均', enabled: false },
]

const ratingRules: RatingRule[] = [
  {
    id: 1, name: '标准五级评级', method: 'SCORE_RANGE',
    levels: [
      { name: '优秀', min: 95, max: 100, color: '#10b981' },
      { name: '良好', min: 85, max: 94.99, color: '#3b82f6' },
      { name: '中等', min: 70, max: 84.99, color: '#f59e0b' },
      { name: '及格', min: 60, max: 69.99, color: '#f97316' },
      { name: '不及格', min: 0, max: 59.99, color: '#ef4444' },
    ],
    enabled: true,
  },
  {
    id: 2, name: '百分位排名评级', method: 'PERCENTILE',
    levels: [
      { name: '优秀', min: 90, max: 100, color: '#10b981' },
      { name: '良好', min: 60, max: 89.99, color: '#3b82f6' },
      { name: '待改进', min: 0, max: 59.99, color: '#f59e0b' },
    ],
    enabled: false,
  },
]

const tabs: { key: ConfigTab; label: string; icon: any }[] = [
  { key: 'templates', label: '模板管理', icon: FileText },
  { key: 'weights', label: '权重方案', icon: Scale },
  { key: 'ratings', label: '评级规则', icon: Award },
  { key: 'schedule', label: '调度策略', icon: CalendarClock },
  { key: 'autoRules', label: '整改规则', icon: ShieldAlert },
]

const scoringModeLabels: Record<ScoringMode, string> = {
  DEDUCTION_ONLY: '纯扣分制',
  BASE_SCORE: '基准分制',
  DUAL_TRACK: '双轨制',
}

const deductModeLabels: Record<DeductMode, string> = {
  FIXED: '固定扣分',
  PER_PERSON: '按人扣分',
  RANGE: '区间扣分',
}

const checkModeLabels: Record<CheckMode, string> = {
  CHECKLIST: '逐项核验',
  FREE_DEDUCTION: '自由扣分',
  BOTH: '两者都支持',
}

const spaceTypeLabels: Record<SpaceType, string> = {
  DORMITORY: '宿舍',
  CLASSROOM: '教室',
  NONE: '无',
}

function toggleCategory(category: Category) {
  category.expanded = !category.expanded
}

function openEditor(template: Template) {
  editTemplate.value = JSON.parse(JSON.stringify(template))
  editingTemplate.value = true
}

function closeEditor() {
  editTemplate.value = null
  editingTemplate.value = false
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900 mb-1">检查配置中心</h1>
      <p class="text-sm text-gray-500">管理检查模板、权重方案、评级规则与调度策略</p>
    </div>

    <!-- Tabs -->
    <div class="flex gap-1 mb-6 bg-white rounded-xl p-1.5 shadow-sm border border-gray-100 w-fit">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200"
        :class="[
          activeTab === tab.key
            ? 'bg-gray-900 text-white shadow-sm'
            : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
        ]"
        @click="activeTab = tab.key"
      >
        <component :is="tab.icon" class="w-4 h-4" />
        {{ tab.label }}
      </button>
    </div>

    <!-- Templates Tab -->
    <div v-if="activeTab === 'templates' && !editingTemplate">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold text-gray-800">检查模板列表</h2>
        <button class="flex items-center gap-1.5 px-4 py-2 bg-gray-900 text-white text-sm rounded-lg hover:bg-gray-800 transition-colors">
          <Plus class="w-4 h-4" />
          新建模板
        </button>
      </div>

      <div class="space-y-4">
        <div
          v-for="tpl in templates"
          :key="tpl.id"
          class="bg-white rounded-xl p-5 shadow-sm border border-gray-100 hover:shadow-md transition-shadow"
        >
          <div class="flex items-start justify-between mb-3">
            <div>
              <div class="flex items-center gap-3 mb-1">
                <h3 class="text-base font-semibold text-gray-900">{{ tpl.name }}</h3>
                <span
                  class="text-xs px-2 py-0.5 rounded-full"
                  :class="tpl.enabled ? 'bg-emerald-50 text-emerald-600' : 'bg-gray-100 text-gray-400'"
                >{{ tpl.enabled ? '启用' : '停用' }}</span>
              </div>
              <div class="flex items-center gap-4 text-xs text-gray-400">
                <span>编码: {{ tpl.code }}</span>
                <span>评分模式: {{ scoringModeLabels[tpl.scoringMode] }}{{ tpl.scoringMode !== 'DEDUCTION_ONLY' ? `(${tpl.baseScore}分)` : '' }}</span>
                <span>轮次: {{ tpl.roundNames.join(', ') }}</span>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <button class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-50 rounded-lg transition-colors" @click="openEditor(tpl)">
                <Edit3 class="w-4 h-4" />
              </button>
              <button class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-50 rounded-lg transition-colors">
                <Copy class="w-4 h-4" />
              </button>
              <button class="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors">
                <Trash2 class="w-4 h-4" />
              </button>
            </div>
          </div>

          <!-- Category summary -->
          <div class="flex flex-wrap gap-3 mt-3 pt-3 border-t border-gray-100">
            <div
              v-for="cat in tpl.categories"
              :key="cat.id"
              class="flex items-center gap-2 px-3 py-1.5 bg-gray-50 rounded-lg text-xs text-gray-600"
            >
              <Layers class="w-3.5 h-3.5 text-gray-400" />
              <span class="font-medium">{{ cat.name }}</span>
              <span class="text-gray-400">({{ checkModeLabels[cat.checkMode] }})</span>
              <span class="text-gray-400">{{ cat.deductionItems.length }}项</span>
            </div>
            <div class="flex-1" />
            <span class="text-xs text-gray-400 self-center">已使用 {{ tpl.useCount }} 次</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Template Editor -->
    <div v-if="activeTab === 'templates' && editingTemplate && editTemplate">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100">
        <!-- Editor Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 class="text-lg font-semibold text-gray-900">编辑模板: {{ editTemplate.name }}</h2>
          <div class="flex items-center gap-2">
            <button class="px-4 py-2 text-sm text-gray-500 hover:text-gray-700 transition-colors" @click="closeEditor">取消</button>
            <button class="px-4 py-2 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors">保存草稿</button>
            <button class="px-4 py-2 text-sm bg-gray-900 text-white rounded-lg hover:bg-gray-800 transition-colors">发布模板</button>
          </div>
        </div>

        <!-- Basic info -->
        <div class="px-6 py-5 border-b border-gray-100">
          <div class="grid grid-cols-2 gap-6">
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1.5 block">模板名称</label>
              <ElInput v-model="editTemplate.name" placeholder="输入模板名称" />
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1.5 block">评分模式</label>
              <div class="flex gap-3">
                <label
                  v-for="mode in (['DEDUCTION_ONLY', 'BASE_SCORE', 'DUAL_TRACK'] as ScoringMode[])"
                  :key="mode"
                  class="flex items-center gap-2 px-3 py-2 rounded-lg border cursor-pointer transition-colors"
                  :class="[
                    editTemplate.scoringMode === mode
                      ? 'border-gray-900 bg-gray-50'
                      : 'border-gray-200 hover:border-gray-300'
                  ]"
                >
                  <input type="radio" :value="mode" v-model="editTemplate.scoringMode" class="sr-only" />
                  <span
                    class="w-4 h-4 rounded-full border-2 flex items-center justify-center"
                    :class="editTemplate.scoringMode === mode ? 'border-gray-900' : 'border-gray-300'"
                  >
                    <span v-if="editTemplate.scoringMode === mode" class="w-2 h-2 rounded-full bg-gray-900" />
                  </span>
                  <span class="text-sm" :class="editTemplate.scoringMode === mode ? 'text-gray-900 font-medium' : 'text-gray-500'">
                    {{ scoringModeLabels[mode] }}
                  </span>
                </label>
              </div>
            </div>
          </div>
          <div v-if="editTemplate.scoringMode !== 'DEDUCTION_ONLY'" class="mt-4 flex items-center gap-4">
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1.5 block">基准分</label>
              <ElInputNumber v-model="editTemplate.baseScore" :min="0" :max="200" size="default" />
            </div>
          </div>
          <div class="mt-4 grid grid-cols-2 gap-6">
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1.5 block">检查轮次</label>
              <div class="flex items-center gap-2">
                <ElInputNumber v-model="editTemplate.totalRounds" :min="1" :max="5" size="default" />
                <span class="text-sm text-gray-400">轮</span>
              </div>
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700 mb-1.5 block">轮次名称</label>
              <div class="flex gap-2">
                <ElInput
                  v-for="(name, i) in editTemplate.roundNames"
                  :key="i"
                  v-model="editTemplate.roundNames[i]"
                  size="default"
                  style="width: 80px"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Categories -->
        <div class="px-6 py-5">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-semibold text-gray-800">检查类别</h3>
            <button class="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 transition-colors">
              <Plus class="w-4 h-4" />
              添加类别
            </button>
          </div>

          <div class="space-y-3">
            <div
              v-for="cat in editTemplate.categories"
              :key="cat.id"
              class="border border-gray-200 rounded-xl overflow-hidden"
            >
              <!-- Category header -->
              <div
                class="flex items-center gap-3 px-4 py-3 bg-gray-50 cursor-pointer hover:bg-gray-100 transition-colors"
                @click="toggleCategory(cat)"
              >
                <component :is="cat.expanded ? ChevronDown : ChevronRight" class="w-4 h-4 text-gray-400" />
                <span class="text-sm font-semibold text-gray-800">{{ cat.name }}</span>
                <div class="flex items-center gap-3 text-xs text-gray-400 ml-4">
                  <span>模式: {{ checkModeLabels[cat.checkMode] }}</span>
                  <span>空间: {{ spaceTypeLabels[cat.spaceType] }}</span>
                  <span>轮次: {{ cat.rounds.map(r => editTemplate.roundNames[r - 1] || `轮${r}`).join(', ') }}</span>
                </div>
                <div class="flex-1" />
                <span class="text-xs text-gray-400">{{ cat.deductionItems.length }}项扣分 · {{ cat.bonusItems.length }}项加分</span>
              </div>

              <!-- Category content -->
              <Transition name="expand">
                <div v-if="cat.expanded" class="px-4 py-4 border-t border-gray-100">
                  <!-- Settings row -->
                  <div class="grid grid-cols-3 gap-4 mb-5 pb-4 border-b border-gray-100">
                    <div>
                      <label class="text-xs font-medium text-gray-500 mb-1 block">检查模式</label>
                      <ElSelect v-model="cat.checkMode" size="small">
                        <ElOption value="CHECKLIST" label="逐项核验" />
                        <ElOption value="FREE_DEDUCTION" label="自由扣分" />
                        <ElOption value="BOTH" label="两者都支持" />
                      </ElSelect>
                    </div>
                    <div>
                      <label class="text-xs font-medium text-gray-500 mb-1 block">关联空间</label>
                      <ElSelect v-model="cat.spaceType" size="small">
                        <ElOption value="DORMITORY" label="宿舍" />
                        <ElOption value="CLASSROOM" label="教室" />
                        <ElOption value="NONE" label="无" />
                      </ElSelect>
                    </div>
                    <div>
                      <label class="text-xs font-medium text-gray-500 mb-1 block">参与轮次</label>
                      <div class="flex items-center gap-2">
                        <label
                          v-for="(name, i) in editTemplate.roundNames"
                          :key="i"
                          class="flex items-center gap-1"
                        >
                          <input type="checkbox" :value="i + 1" v-model="cat.rounds" class="rounded border-gray-300" />
                          <span class="text-xs text-gray-600">{{ name }}</span>
                        </label>
                      </div>
                    </div>
                  </div>

                  <!-- Deduction items table -->
                  <div class="mb-4">
                    <div class="flex items-center justify-between mb-2">
                      <span class="text-xs font-semibold text-gray-600">扣分项</span>
                      <button class="flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600">
                        <Plus class="w-3 h-3" />
                        添加
                      </button>
                    </div>
                    <table class="w-full text-sm">
                      <thead>
                        <tr class="text-xs text-gray-400 border-b border-gray-100">
                          <th class="text-left pb-2 w-8">#</th>
                          <th class="text-left pb-2">名称</th>
                          <th class="text-left pb-2 w-24">扣分模式</th>
                          <th class="text-left pb-2 w-20">分值</th>
                          <th class="text-center pb-2 w-28">要求选项</th>
                          <th class="text-right pb-2 w-16">操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="item in cat.deductionItems" :key="item.id" class="border-b border-gray-50 hover:bg-gray-50/50">
                          <td class="py-2.5 text-gray-400">
                            <GripVertical class="w-4 h-4 cursor-grab" />
                          </td>
                          <td class="py-2.5 font-medium text-gray-800">{{ item.name }}</td>
                          <td class="py-2.5">
                            <span class="text-xs px-2 py-0.5 rounded bg-gray-100 text-gray-600">
                              {{ deductModeLabels[item.deductMode] }}
                            </span>
                          </td>
                          <td class="py-2.5 text-red-600 font-medium">
                            <template v-if="item.deductMode === 'RANGE'">0~{{ item.maxScore }}</template>
                            <template v-else-if="item.deductMode === 'PER_PERSON'">-{{ item.score }}/人</template>
                            <template v-else>-{{ item.score }}</template>
                          </td>
                          <td class="py-2.5">
                            <div class="flex items-center justify-center gap-2">
                              <Camera class="w-3.5 h-3.5" :class="item.requirePhoto ? 'text-blue-500' : 'text-gray-300'" />
                              <MessageSquare class="w-3.5 h-3.5" :class="item.requireRemark ? 'text-blue-500' : 'text-gray-300'" />
                              <Users class="w-3.5 h-3.5" :class="item.requireStudent ? 'text-blue-500' : 'text-gray-300'" />
                            </div>
                          </td>
                          <td class="py-2.5 text-right">
                            <div class="flex items-center justify-end gap-1">
                              <button class="p-1 text-gray-400 hover:text-gray-600"><Edit3 class="w-3.5 h-3.5" /></button>
                              <button class="p-1 text-gray-400 hover:text-red-500"><Trash2 class="w-3.5 h-3.5" /></button>
                            </div>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  <!-- Bonus items (only in DUAL_TRACK mode) -->
                  <div v-if="editTemplate.scoringMode === 'DUAL_TRACK' || cat.bonusItems.length > 0">
                    <div class="flex items-center justify-between mb-2">
                      <span class="text-xs font-semibold text-emerald-600">加分项</span>
                      <button class="flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600">
                        <Plus class="w-3 h-3" />
                        添加
                      </button>
                    </div>
                    <div v-if="cat.bonusItems.length === 0" class="text-xs text-gray-400 py-2">
                      暂无加分项，仅双轨制模式下生效
                    </div>
                    <div v-else class="space-y-2">
                      <div v-for="bonus in cat.bonusItems" :key="bonus.id"
                        class="flex items-center gap-3 px-3 py-2 bg-emerald-50/50 rounded-lg border border-emerald-100"
                      >
                        <span class="text-sm font-medium text-emerald-700">{{ bonus.name }}</span>
                        <span class="text-xs text-emerald-500">{{ bonus.bonusMode === 'FIXED' ? `+${bonus.fixedBonus}分` : bonus.description }}</span>
                        <div class="flex-1" />
                        <button class="p-1 text-emerald-400 hover:text-emerald-600"><Edit3 class="w-3.5 h-3.5" /></button>
                        <button class="p-1 text-emerald-400 hover:text-red-500"><Trash2 class="w-3.5 h-3.5" /></button>
                      </div>
                    </div>
                  </div>
                </div>
              </Transition>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Weights Tab -->
    <div v-if="activeTab === 'weights'">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold text-gray-800">权重方案</h2>
        <button class="flex items-center gap-1.5 px-4 py-2 bg-gray-900 text-white text-sm rounded-lg hover:bg-gray-800 transition-colors">
          <Plus class="w-4 h-4" />
          新建方案
        </button>
      </div>
      <div class="space-y-3">
        <div v-for="scheme in weightSchemes" :key="scheme.id"
          class="bg-white rounded-xl p-5 shadow-sm border border-gray-100 flex items-center gap-4"
        >
          <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
            <Scale class="w-5 h-5 text-blue-500" />
          </div>
          <div class="flex-1">
            <div class="flex items-center gap-2">
              <h3 class="text-sm font-semibold text-gray-900">{{ scheme.name }}</h3>
              <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">{{ scheme.algorithm }}</span>
            </div>
            <p class="text-xs text-gray-400 mt-0.5">{{ scheme.description }}</p>
          </div>
          <div class="flex items-center gap-3">
            <span
              class="text-xs font-medium"
              :class="scheme.enabled ? 'text-emerald-600' : 'text-gray-400'"
            >{{ scheme.enabled ? '启用中' : '已停用' }}</span>
            <button class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-50 rounded-lg"><Edit3 class="w-4 h-4" /></button>
          </div>
        </div>
      </div>
    </div>

    <!-- Ratings Tab -->
    <div v-if="activeTab === 'ratings'">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold text-gray-800">评级规则</h2>
        <button class="flex items-center gap-1.5 px-4 py-2 bg-gray-900 text-white text-sm rounded-lg hover:bg-gray-800 transition-colors">
          <Plus class="w-4 h-4" />
          新建规则
        </button>
      </div>
      <div class="space-y-4">
        <div v-for="rule in ratingRules" :key="rule.id"
          class="bg-white rounded-xl p-5 shadow-sm border border-gray-100"
        >
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-3">
              <h3 class="text-sm font-semibold text-gray-900">{{ rule.name }}</h3>
              <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">{{ rule.method === 'SCORE_RANGE' ? '分数段划分' : '百分位排名' }}</span>
              <span
                class="text-xs px-2 py-0.5 rounded-full"
                :class="rule.enabled ? 'bg-emerald-50 text-emerald-600' : 'bg-gray-100 text-gray-400'"
              >{{ rule.enabled ? '启用' : '停用' }}</span>
            </div>
            <button class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-50 rounded-lg"><Edit3 class="w-4 h-4" /></button>
          </div>
          <div class="flex gap-2">
            <div
              v-for="level in rule.levels"
              :key="level.name"
              class="flex-1 rounded-lg p-3 border"
              :style="{ borderColor: level.color + '40', background: level.color + '10' }"
            >
              <div class="text-sm font-semibold mb-1" :style="{ color: level.color }">{{ level.name }}</div>
              <div class="text-xs text-gray-500">{{ rule.method === 'SCORE_RANGE' ? `${level.min} - ${level.max}分` : `前${100 - level.min}%` }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Schedule Tab -->
    <div v-if="activeTab === 'schedule'">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold text-gray-800">调度策略</h2>
        <button class="flex items-center gap-1.5 px-4 py-2 bg-gray-900 text-white text-sm rounded-lg hover:bg-gray-800 transition-colors">
          <Plus class="w-4 h-4" />
          新建策略
        </button>
      </div>
      <div class="space-y-3">
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <div class="flex items-center gap-3 mb-3">
            <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
              <CalendarClock class="w-5 h-5 text-blue-500" />
            </div>
            <div>
              <h3 class="text-sm font-semibold text-gray-900">工作日每日检查</h3>
              <span class="text-xs text-gray-400">类型: 周期性 | Cron: 0 8 * * 1-5</span>
            </div>
            <div class="flex-1" />
            <span class="text-xs px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600">运行中</span>
          </div>
          <div class="grid grid-cols-4 gap-4 text-xs text-gray-500 bg-gray-50 rounded-lg p-3">
            <div><span class="text-gray-400">时间窗口:</span> 08:00 - 12:00</div>
            <div><span class="text-gray-400">跳过节假日:</span> 是</div>
            <div><span class="text-gray-400">轮转模式:</span> 轮询分配</div>
            <div><span class="text-gray-400">每人每日上限:</span> 5次</div>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <div class="flex items-center gap-3 mb-3">
            <div class="w-10 h-10 rounded-lg bg-amber-50 flex items-center justify-center">
              <CalendarClock class="w-5 h-5 text-amber-500" />
            </div>
            <div>
              <h3 class="text-sm font-semibold text-gray-900">随机抽检</h3>
              <span class="text-xs text-gray-400">类型: 随机 | 每周3次, 抽取50%班级</span>
            </div>
            <div class="flex-1" />
            <span class="text-xs px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600">运行中</span>
          </div>
          <div class="grid grid-cols-4 gap-4 text-xs text-gray-500 bg-gray-50 rounded-lg p-3">
            <div><span class="text-gray-400">每周频次:</span> 3次</div>
            <div><span class="text-gray-400">最小间隔:</span> 1天</div>
            <div><span class="text-gray-400">抽样比例:</span> 50%</div>
            <div><span class="text-gray-400">排除自检:</span> 是</div>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-lg bg-red-50 flex items-center justify-center">
              <ShieldAlert class="w-5 h-5 text-red-500" />
            </div>
            <div>
              <h3 class="text-sm font-semibold text-gray-900">连续低分触发专项检查</h3>
              <span class="text-xs text-gray-400">类型: 触发式 | 条件: 连续3次扣分超过15分</span>
            </div>
            <div class="flex-1" />
            <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-400">已停用</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Auto Rules Tab -->
    <div v-if="activeTab === 'autoRules'">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold text-gray-800">自动整改规则</h2>
        <button class="flex items-center gap-1.5 px-4 py-2 bg-gray-900 text-white text-sm rounded-lg hover:bg-gray-800 transition-colors">
          <Plus class="w-4 h-4" />
          新建规则
        </button>
      </div>
      <div class="space-y-3">
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <div class="flex items-center gap-3 mb-3">
            <h3 class="text-sm font-semibold text-gray-900">严重及以上自动创建整改工单</h3>
            <span class="text-xs px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600">启用</span>
          </div>
          <div class="grid grid-cols-3 gap-4 text-xs text-gray-500 bg-gray-50 rounded-lg p-3">
            <div><span class="text-gray-400">触发条件:</span> 严重程度 &ge; SEVERE</div>
            <div><span class="text-gray-400">自动分配:</span> 班主任</div>
            <div><span class="text-gray-400">默认期限:</span> 3天</div>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <div class="flex items-center gap-3 mb-3">
            <h3 class="text-sm font-semibold text-gray-900">单项扣分超过5分自动创建整改</h3>
            <span class="text-xs px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600">启用</span>
          </div>
          <div class="grid grid-cols-3 gap-4 text-xs text-gray-500 bg-gray-50 rounded-lg p-3">
            <div><span class="text-gray-400">触发条件:</span> 单项扣分 &ge; 5.0</div>
            <div><span class="text-gray-400">自动分配:</span> 班主任</div>
            <div><span class="text-gray-400">默认期限:</span> 2天</div>
          </div>
        </div>
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <div class="flex items-center gap-3 mb-3">
            <h3 class="text-sm font-semibold text-gray-900">安全类问题自动升级</h3>
            <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-400">已停用</span>
          </div>
          <div class="grid grid-cols-3 gap-4 text-xs text-gray-500 bg-gray-50 rounded-lg p-3">
            <div><span class="text-gray-400">触发条件:</span> 类别 = 安全</div>
            <div><span class="text-gray-400">自动分配:</span> 宿管主任</div>
            <div><span class="text-gray-400">默认期限:</span> 1天</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}
.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
}
.expand-enter-to,
.expand-leave-from {
  max-height: 800px;
  opacity: 1;
}
</style>
