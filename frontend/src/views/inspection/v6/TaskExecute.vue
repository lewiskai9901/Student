<template>
  <div class="v6-task-execute">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <div class="divider"></div>
        <div class="task-info">
          <h2 class="task-title">{{ taskInfo?.projectName || '检查任务' }}</h2>
          <span class="task-date">{{ formatDate(taskInfo?.taskDate) }}</span>
        </div>
      </div>
      <div class="header-center">
        <div class="progress-ring">
          <svg viewBox="0 0 36 36" class="circular-chart">
            <path class="circle-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
            <path class="circle" :stroke-dasharray="`${progressPercent}, 100`" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
          </svg>
          <span class="progress-number">{{ progressPercent }}%</span>
        </div>
        <div class="progress-text">
          <span class="progress-label">检查进度</span>
          <span class="progress-detail">{{ completedCount }} / {{ totalCount }} 已完成</span>
        </div>
      </div>
      <div class="header-right">
        <button class="action-btn secondary" @click="saveProgress">
          <el-icon><Document /></el-icon>
          暂存进度
        </button>
        <button class="action-btn primary" @click="submitTask">
          <el-icon><CircleCheck /></el-icon>
          提交任务
        </button>
      </div>
    </div>

    <!-- 主体双栏布局 -->
    <div class="main-container" v-loading="loading">
      <!-- 左侧 - 组织树 -->
      <div class="left-panel">
        <div class="panel-header">
          <h3>检查目标</h3>
          <span class="target-count">{{ totalCount }}个</span>
        </div>

        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input v-model="searchKeyword" placeholder="搜索目标..." class="search-input" />
          <el-icon v-if="searchKeyword" class="clear-icon" @click="searchKeyword = ''"><Close /></el-icon>
        </div>

        <div class="status-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            :class="['status-tab', { active: currentStatusFilter === tab.value }]"
            @click="currentStatusFilter = tab.value"
          >
            <span class="tab-dot" :style="{ background: tab.color }"></span>
            {{ tab.label }}
            <span class="tab-count">{{ getStatusCount(tab.value) }}</span>
          </button>
        </div>

        <el-scrollbar class="org-tree-scroll">
          <div class="org-tree">
            <template v-for="org in filteredOrgTree" :key="org.id">
              <div class="org-group">
                <div class="org-header" @click="toggleOrg(org)">
                  <el-icon class="expand-icon" :class="{ expanded: org.expanded }">
                    <ArrowRight />
                  </el-icon>
                  <div class="org-info">
                    <span class="org-name">{{ org.name }}</span>
                    <div class="org-progress-bar">
                      <div class="progress-fill" :style="{ width: `${(org.completedCount / org.totalCount) * 100}%` }"></div>
                    </div>
                  </div>
                  <span class="org-count">{{ org.completedCount }}/{{ org.totalCount }}</span>
                </div>

                <div v-if="org.expanded" class="org-children">
                  <template v-for="cls in org.children" :key="cls.id">
                    <div class="class-group">
                      <div class="class-header" @click="toggleClass(cls)">
                        <el-icon class="expand-icon" :class="{ expanded: cls.expanded }">
                          <ArrowRight />
                        </el-icon>
                        <span class="class-name">{{ cls.name }}</span>
                        <span class="class-count">{{ cls.completedCount }}/{{ cls.totalCount }}</span>
                      </div>

                      <div v-if="cls.expanded" class="target-list">
                        <div
                          v-for="target in cls.targets"
                          :key="target.id"
                          class="target-item"
                          :class="{
                            active: currentTarget?.id === target.id,
                            completed: target.status === 'COMPLETED',
                            'in-progress': target.status === 'LOCKED',
                            skipped: target.status === 'SKIPPED'
                          }"
                          @click="selectTarget(target)"
                        >
                          <span class="target-status">
                            <template v-if="target.status === 'COMPLETED'">
                              <el-icon class="status-icon completed"><CircleCheckFilled /></el-icon>
                            </template>
                            <template v-else-if="target.status === 'LOCKED'">
                              <span class="status-dot in-progress"></span>
                            </template>
                            <template v-else-if="target.status === 'SKIPPED'">
                              <el-icon class="status-icon skipped"><RemoveFilled /></el-icon>
                            </template>
                            <template v-else>
                              <span class="status-dot pending"></span>
                            </template>
                          </span>
                          <span class="target-name">{{ target.targetName }}</span>
                          <span v-if="target.status === 'COMPLETED'" class="target-score">
                            {{ target.finalScore }}<small>分</small>
                          </span>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </div>
            </template>
          </div>
        </el-scrollbar>

        <div class="legend-bar">
          <div class="legend-item">
            <span class="legend-dot pending"></span>
            <span>待检查</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot in-progress"></span>
            <span>检查中</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot completed"></span>
            <span>已完成</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot skipped"></span>
            <span>已跳过</span>
          </div>
        </div>
      </div>

      <!-- 右侧 - 打分面板 -->
      <div class="right-panel">
        <template v-if="currentTarget">
          <!-- 面包屑导航 -->
          <div class="breadcrumb">
            <span class="crumb">{{ currentTarget.orgUnitName }}</span>
            <el-icon class="separator"><ArrowRight /></el-icon>
            <span class="crumb">{{ currentTarget.className }}</span>
            <el-icon class="separator"><ArrowRight /></el-icon>
            <span class="crumb current">{{ currentTarget.targetName }}</span>
          </div>

          <!-- 目标信息卡片 -->
          <div class="target-card">
            <div class="target-card-header">
              <div class="target-main">
                <div class="target-avatar">
                  {{ currentTarget.targetName?.charAt(0) || 'T' }}
                </div>
                <div class="target-details">
                  <h3>{{ currentTarget.targetName }}</h3>
                  <div class="target-meta">
                    <span class="meta-item">
                      <el-icon><Location /></el-icon>
                      {{ currentTarget.className }}
                    </span>
                    <span class="meta-item">
                      <el-icon><Tickets /></el-icon>
                      {{ currentTarget.targetType }}
                    </span>
                  </div>
                </div>
              </div>
              <div class="target-status-badge" :class="currentTarget.status?.toLowerCase()">
                {{ getTargetStatusLabel(currentTarget.status) }}
              </div>
            </div>
          </div>

          <!-- 分数展示卡片 -->
          <div class="score-display-card">
            <div class="score-main">
              <div class="score-circle" :class="{ warning: calculateFinalScore < 80, danger: calculateFinalScore < 60 }">
                <span class="score-number">{{ calculateFinalScore }}</span>
                <span class="score-label">当前得分</span>
              </div>
              <div class="score-breakdown">
                <div class="breakdown-item">
                  <span class="breakdown-label">基准分</span>
                  <span class="breakdown-value base">{{ currentTarget.baseScore || 100 }}</span>
                </div>
                <div class="breakdown-item">
                  <span class="breakdown-label">扣分</span>
                  <span class="breakdown-value deduction">-{{ totalDeduction }}</span>
                </div>
                <div class="breakdown-item">
                  <span class="breakdown-label">加分</span>
                  <span class="breakdown-value bonus">+{{ totalBonus }}</span>
                </div>
              </div>
            </div>
            <div class="score-bar">
              <div class="score-bar-fill" :style="{ width: `${calculateFinalScore}%` }" :class="{ warning: calculateFinalScore < 80, danger: calculateFinalScore < 60 }"></div>
            </div>
          </div>

          <!-- 检查项分类 -->
          <div class="categories-section">
            <h4 class="section-title">
              <el-icon><List /></el-icon>
              扣分项目
            </h4>

            <div class="category-cards">
              <div
                v-for="category in categories"
                :key="category.id"
                class="category-card"
                :class="{ 'has-deduction': getCategoryDeduction(category.id) > 0 }"
              >
                <div class="category-header" :style="{ borderLeftColor: category.color || '#409eff' }">
                  <div class="category-title">
                    <span class="category-icon" :style="{ background: category.color || '#409eff' }">
                      {{ category.name.charAt(0) }}
                    </span>
                    <span class="category-name">{{ category.name }}</span>
                    <span v-if="category.weight" class="category-weight">权重 {{ category.weight }}%</span>
                  </div>
                  <div class="category-deduction" v-if="getCategoryDeduction(category.id) > 0">
                    -{{ getCategoryDeduction(category.id) }}分
                  </div>
                </div>

                <div class="inspection-items">
                  <div
                    v-for="item in category.items"
                    :key="item.id"
                    class="inspection-item"
                    :class="{ checked: isItemChecked(category.id, item.id) }"
                  >
                    <div class="item-main" @click="toggleItem(category, item, !isItemChecked(category.id, item.id))">
                      <div class="item-checkbox" :class="{ checked: isItemChecked(category.id, item.id) }">
                        <el-icon v-if="isItemChecked(category.id, item.id)"><Check /></el-icon>
                      </div>
                      <span class="item-name">{{ item.name }}</span>
                      <span class="item-score">-{{ item.score }}分</span>
                    </div>

                    <!-- 已勾选的明细 -->
                    <div v-if="isItemChecked(category.id, item.id)" class="item-details">
                      <div
                        v-for="detail in getItemDetails(category.id, item.id)"
                        :key="detail.id"
                        class="detail-row"
                      >
                        <div class="detail-content">
                          <template v-if="detail.scope === 'INDIVIDUAL'">
                            <span class="individual-tag">{{ detail.individualName }}</span>
                          </template>
                          <span class="detail-remark">{{ detail.remark || '(无备注)' }}</span>
                        </div>
                        <div class="detail-actions">
                          <span class="detail-score">-{{ Math.abs(detail.totalScore) }}分</span>
                          <button class="detail-delete" @click="removeDetail(detail.id)">
                            <el-icon><Delete /></el-icon>
                          </button>
                        </div>
                      </div>

                      <div class="detail-actions-row">
                        <button v-if="item.canLinkIndividual" class="add-individual-btn" @click="showAddIndividualDialog(category, item)">
                          <el-icon><Plus /></el-icon>
                          关联个体
                        </button>
                        <div class="remark-input">
                          <el-input
                            v-model="itemRemarks[`${category.id}-${item.id}`]"
                            size="small"
                            placeholder="添加备注..."
                            @blur="saveItemRemark(category, item)"
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 证据上传 -->
          <div class="evidence-section">
            <h4 class="section-title">
              <el-icon><Picture /></el-icon>
              证据上传
            </h4>
            <div class="evidence-grid">
              <div v-for="evidence in evidences" :key="evidence.id" class="evidence-item">
                <el-image
                  :src="evidence.fileUrl"
                  fit="cover"
                  class="evidence-image"
                  :preview-src-list="[evidence.fileUrl]"
                />
                <button class="evidence-delete" @click="deleteEvidence(evidence.id)">
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
              <el-upload
                action="#"
                :auto-upload="false"
                :show-file-list="false"
                @change="handleUpload"
                class="evidence-upload"
              >
                <div class="upload-placeholder">
                  <el-icon class="upload-icon"><Plus /></el-icon>
                  <span>上传照片</span>
                </div>
              </el-upload>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-section">
            <template v-if="currentTarget.status === 'PENDING'">
              <button class="action-button secondary" @click="skipTarget">
                <el-icon><Close /></el-icon>
                标记跳过
              </button>
              <button class="action-button primary" @click="startTarget">
                <el-icon><VideoPlay /></el-icon>
                开始检查
              </button>
            </template>
            <template v-else-if="currentTarget.status === 'LOCKED'">
              <button class="action-button success large" @click="completeAndNext">
                <el-icon><Check /></el-icon>
                完成并检查下一个
                <el-icon class="arrow"><ArrowRight /></el-icon>
              </button>
            </template>
            <template v-else-if="currentTarget.status === 'COMPLETED'">
              <div class="completed-hint">
                <el-icon><CircleCheckFilled /></el-icon>
                该目标已完成检查
              </div>
            </template>
          </div>
        </template>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <div class="empty-icon">
            <el-icon><Select /></el-icon>
          </div>
          <h3>请选择检查目标</h3>
          <p>从左侧列表中选择一个目标开始检查</p>
        </div>
      </div>
    </div>

    <!-- 添加个体扣分对话框 -->
    <el-dialog v-model="addIndividualDialogVisible" title="添加个体扣分" width="480px" class="modern-dialog">
      <el-form :model="individualForm" label-width="80px">
        <el-form-item label="类型">
          <el-radio-group v-model="individualForm.type">
            <el-radio label="USER">学生</el-radio>
            <el-radio label="PLACE">场所</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="选择">
          <el-select v-model="individualForm.id" filterable placeholder="请选择" style="width: 100%">
            <el-option
              v-for="ind in individualOptions"
              :key="ind.id"
              :label="ind.name"
              :value="ind.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="individualForm.quantity" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="individualForm.remark" type="textarea" rows="2" placeholder="可选填写备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="dialog-btn cancel" @click="addIndividualDialogVisible = false">取消</button>
        <button class="dialog-btn confirm" @click="confirmAddIndividual">确认添加</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, ArrowRight, Search, Close, Document, CircleCheck, CircleCheckFilled,
  RemoveFilled, Location, Tickets, List, Check, Delete, Plus, Picture,
  VideoPlay, Select
} from '@element-plus/icons-vue'
import { v6TaskApi, v6ExecutionApi } from '@/api/v6Inspection'
import { getTemplateItems } from '@/api/v6TemplateItem'
import type { TemplateCategory, TemplateScoreItem } from '@/api/v6TemplateItem'
import type { InspectionTarget } from '@/types/v6Inspection'
import type { InspectionDetail, InspectionEvidence } from '@/api/v6Inspection'

const router = useRouter()
const route = useRoute()
const taskId = route.params.id as string

// 状态
const loading = ref(false)
const taskInfo = ref<any>(null)
const targets = ref<InspectionTarget[]>([])
const currentTarget = ref<InspectionTarget | null>(null)
const details = ref<InspectionDetail[]>([])
const evidences = ref<InspectionEvidence[]>([])
const searchKeyword = ref('')
const currentStatusFilter = ref('all')

// 状态标签页
const statusTabs = [
  { value: 'all', label: '全部', color: '#909399' },
  { value: 'PENDING', label: '待检查', color: '#909399' },
  { value: 'LOCKED', label: '检查中', color: '#e6a23c' },
  { value: 'COMPLETED', label: '已完成', color: '#67c23a' }
]

// 组织树结构
interface OrgNode {
  id: number
  name: string
  expanded: boolean
  completedCount: number
  totalCount: number
  children: ClassNode[]
}

interface ClassNode {
  id: number
  name: string
  expanded: boolean
  completedCount: number
  totalCount: number
  targets: InspectionTarget[]
}

const orgTree = ref<OrgNode[]>([])

// 检查项分类
interface CategoryItem {
  id: number
  name: string
  score: number
  canLinkIndividual: boolean
}

interface Category {
  id: number
  name: string
  weight: number
  color?: string
  items: CategoryItem[]
}

const categories = ref<Category[]>([])

// 从模板加载检查项分类
const loadCategoriesFromTemplate = async (templateId: number) => {
  try {
    const templateCategories = await getTemplateItems(templateId)
    categories.value = templateCategories.map((cat: TemplateCategory) => ({
      id: cat.id,
      name: cat.categoryName,
      weight: cat.weight,
      color: cat.color,
      items: (cat.items || []).map((item: TemplateScoreItem) => ({
        id: item.id,
        name: item.itemName,
        score: item.score,
        canLinkIndividual: item.canLinkIndividual
      }))
    }))
  } catch (error) {
    console.error('加载模板分类失败:', error)
    // 默认数据
    categories.value = [
      {
        id: 1,
        name: '卫生类',
        weight: 40,
        color: '#67c23a',
        items: [
          { id: 101, name: '地面不清洁', score: 3, canLinkIndividual: false },
          { id: 102, name: '床铺不整洁', score: 2, canLinkIndividual: true },
          { id: 103, name: '阳台杂物', score: 2, canLinkIndividual: false },
          { id: 104, name: '卫生间不洁', score: 3, canLinkIndividual: false },
          { id: 105, name: '垃圾未清理', score: 2, canLinkIndividual: false }
        ]
      },
      {
        id: 2,
        name: '安全类',
        weight: 35,
        color: '#f56c6c',
        items: [
          { id: 201, name: '违规电器', score: 5, canLinkIndividual: true },
          { id: 202, name: '私拉电线', score: 3, canLinkIndividual: false },
          { id: 203, name: '消防通道堵塞', score: 5, canLinkIndividual: false },
          { id: 204, name: '门窗损坏', score: 2, canLinkIndividual: false }
        ]
      },
      {
        id: 3,
        name: '秩序类',
        weight: 25,
        color: '#409eff',
        items: [
          { id: 301, name: '物品摆放混乱', score: 2, canLinkIndividual: false },
          { id: 302, name: '私自张贴', score: 1, canLinkIndividual: false },
          { id: 303, name: '饲养宠物', score: 5, canLinkIndividual: true }
        ]
      }
    ]
  }
}

// 检查项备注
const itemRemarks = ref<Record<string, string>>({})

// 添加个体对话框
const addIndividualDialogVisible = ref(false)
const currentAddCategory = ref<any>(null)
const currentAddItem = ref<any>(null)
const individualForm = ref({
  type: 'USER',
  id: null as number | null,
  quantity: 1,
  remark: ''
})
const individualOptions = ref<Array<{ id: number; name: string }>>([])

// 计算属性
const completedCount = computed(() =>
  targets.value.filter(t => t.status === 'COMPLETED').length
)

const totalCount = computed(() => targets.value.length)

const progressPercent = computed(() =>
  totalCount.value > 0 ? Math.round((completedCount.value / totalCount.value) * 100) : 0
)

const totalDeduction = computed(() =>
  details.value
    .filter(d => d.scoringMode === 'DEDUCTION')
    .reduce((sum, d) => sum + Math.abs(d.totalScore), 0)
)

const totalBonus = computed(() =>
  details.value
    .filter(d => d.scoringMode === 'ADDITION')
    .reduce((sum, d) => sum + d.totalScore, 0)
)

const calculateFinalScore = computed(() => {
  const base = currentTarget.value?.baseScore || 100
  return Math.max(0, base - totalDeduction.value + totalBonus.value)
})

const getStatusCount = (status: string) => {
  if (status === 'all') return targets.value.length
  return targets.value.filter(t => t.status === status).length
}

const filteredOrgTree = computed(() => {
  let tree = orgTree.value

  // 搜索过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    tree = tree.map(org => ({
      ...org,
      expanded: true,
      children: org.children.map(cls => ({
        ...cls,
        expanded: true,
        targets: cls.targets.filter(t =>
          t.targetName.toLowerCase().includes(keyword)
        )
      })).filter(cls => cls.targets.length > 0)
    })).filter(org => org.children.length > 0)
  }

  // 状态过滤
  if (currentStatusFilter.value !== 'all') {
    tree = tree.map(org => ({
      ...org,
      expanded: true,
      children: org.children.map(cls => ({
        ...cls,
        expanded: true,
        targets: cls.targets.filter(t => t.status === currentStatusFilter.value)
      })).filter(cls => cls.targets.length > 0)
    })).filter(org => org.children.length > 0)
  }

  return tree
})

// 方法
const formatDate = (date: string | undefined) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const getTargetStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    PENDING: '待检查',
    LOCKED: '检查中',
    COMPLETED: '已完成',
    SKIPPED: '已跳过'
  }
  return labels[status] || status
}

const goBack = () => router.push(`/inspection/v6/tasks/${taskId}`)

const loadData = async () => {
  loading.value = true
  try {
    const [task, targetList] = await Promise.all([
      v6TaskApi.getById(taskId),
      v6TaskApi.getTargets(taskId)
    ])
    taskInfo.value = task
    targets.value = targetList
    buildOrgTree()

    const templateId = task?.templateId || task?.project?.templateId || 1
    await loadCategoriesFromTemplate(templateId)
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const buildOrgTree = () => {
  const orgMap = new Map<number, OrgNode>()
  const classMap = new Map<string, ClassNode>()

  targets.value.forEach(target => {
    const orgId = target.orgUnitId || 0
    const orgName = target.orgUnitName || '未分组'
    const classId = target.classId || 0
    const className = target.className || '未分组'

    if (!orgMap.has(orgId)) {
      orgMap.set(orgId, {
        id: orgId,
        name: orgName,
        expanded: true,
        completedCount: 0,
        totalCount: 0,
        children: []
      })
    }

    const classKey = `${orgId}-${classId}`
    if (!classMap.has(classKey)) {
      const classNode: ClassNode = {
        id: classId,
        name: className,
        expanded: true,
        completedCount: 0,
        totalCount: 0,
        targets: []
      }
      classMap.set(classKey, classNode)
      orgMap.get(orgId)!.children.push(classNode)
    }

    classMap.get(classKey)!.targets.push(target)
    classMap.get(classKey)!.totalCount++
    orgMap.get(orgId)!.totalCount++

    if (target.status === 'COMPLETED') {
      classMap.get(classKey)!.completedCount++
      orgMap.get(orgId)!.completedCount++
    }
  })

  orgTree.value = Array.from(orgMap.values())
}

const toggleOrg = (org: OrgNode) => {
  org.expanded = !org.expanded
}

const toggleClass = (cls: ClassNode) => {
  cls.expanded = !cls.expanded
}

const selectTarget = async (target: InspectionTarget) => {
  currentTarget.value = target
  await loadTargetDetails()
}

const loadTargetDetails = async () => {
  if (!currentTarget.value) return
  try {
    const [detailList, evidenceList] = await Promise.all([
      v6ExecutionApi.getDetailsByTarget(currentTarget.value.id),
      v6ExecutionApi.getEvidencesByTarget(currentTarget.value.id)
    ])
    details.value = detailList
    evidences.value = evidenceList
  } catch (error) {
    console.error('Load details failed:', error)
  }
}

const isItemChecked = (categoryId: number, itemId: number) => {
  return details.value.some(d => d.categoryId === categoryId && d.itemId === itemId)
}

const getItemDetails = (categoryId: number, itemId: number) => {
  return details.value.filter(d => d.categoryId === categoryId && d.itemId === itemId)
}

const getCategoryDeduction = (categoryId: number) => {
  return details.value
    .filter(d => d.categoryId === categoryId && d.scoringMode === 'DEDUCTION')
    .reduce((sum, d) => sum + Math.abs(d.totalScore), 0)
}

const toggleItem = async (category: any, item: any, checked: boolean) => {
  if (!currentTarget.value) return

  if (checked) {
    try {
      await v6ExecutionApi.addDeduction({
        targetId: currentTarget.value.id,
        categoryId: category.id,
        categoryCode: `CAT_${category.id}`,
        categoryName: category.name,
        itemId: item.id,
        itemCode: `ITEM_${item.id}`,
        itemName: item.name,
        score: item.score,
        quantity: 1,
        remark: ''
      })
      await loadTargetDetails()
      await refreshTargetScore()
    } catch (error) {
      ElMessage.error('添加失败')
    }
  } else {
    const itemDetails = getItemDetails(category.id, item.id)
    for (const detail of itemDetails) {
      await v6ExecutionApi.deleteDetail(detail.id)
    }
    await loadTargetDetails()
    await refreshTargetScore()
  }
}

const removeDetail = async (detailId: number) => {
  try {
    await v6ExecutionApi.deleteDetail(detailId)
    await loadTargetDetails()
    await refreshTargetScore()
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const showAddIndividualDialog = (category: any, item: any) => {
  currentAddCategory.value = category
  currentAddItem.value = item
  individualForm.value = { type: 'USER', id: null, quantity: 1, remark: '' }
  individualOptions.value = [
    { id: 1, name: '张三' },
    { id: 2, name: '李四' },
    { id: 3, name: '王五' }
  ]
  addIndividualDialogVisible.value = true
}

const confirmAddIndividual = async () => {
  if (!currentTarget.value || !individualForm.value.id) return

  const selectedIndividual = individualOptions.value.find(i => i.id === individualForm.value.id)
  if (!selectedIndividual) return

  try {
    await v6ExecutionApi.addDeductionWithIndividual({
      targetId: currentTarget.value.id,
      categoryId: currentAddCategory.value.id,
      categoryCode: `CAT_${currentAddCategory.value.id}`,
      categoryName: currentAddCategory.value.name,
      itemId: currentAddItem.value.id,
      itemCode: `ITEM_${currentAddItem.value.id}`,
      itemName: currentAddItem.value.name,
      score: currentAddItem.value.score,
      quantity: individualForm.value.quantity,
      remark: individualForm.value.remark,
      individualType: individualForm.value.type,
      individualId: individualForm.value.id,
      individualName: selectedIndividual.name
    })
    addIndividualDialogVisible.value = false
    await loadTargetDetails()
    await refreshTargetScore()
    ElMessage.success('添加成功')
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

const saveItemRemark = async (category: any, item: any) => {
  // 备注保存逻辑
}

const refreshTargetScore = async () => {
  targets.value = await v6TaskApi.getTargets(taskId)
  buildOrgTree()
  if (currentTarget.value) {
    currentTarget.value = targets.value.find(t => t.id === currentTarget.value?.id) || null
  }
}

const startTarget = async () => {
  if (!currentTarget.value) return
  try {
    await v6TaskApi.lockTarget(currentTarget.value.id)
    await refreshTargetScore()
    ElMessage.success('开始检查')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const completeAndNext = async () => {
  if (!currentTarget.value) return
  try {
    await v6TaskApi.completeTarget(currentTarget.value.id)
    await refreshTargetScore()
    ElMessage.success('检查完成')

    const pendingTargets = targets.value.filter(t => t.status === 'PENDING')
    if (pendingTargets.length > 0) {
      selectTarget(pendingTargets[0])
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const skipTarget = async () => {
  if (!currentTarget.value) return
  try {
    const { value } = await ElMessageBox.prompt('请输入跳过原因', '跳过目标')
    await v6TaskApi.skipTarget(currentTarget.value.id, value || '')
    await refreshTargetScore()
    ElMessage.success('已跳过')
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleUpload = async (file: any) => {
  ElMessage.info('文件上传功能待实现')
}

const deleteEvidence = async (evidenceId: number) => {
  try {
    await v6ExecutionApi.deleteEvidence(evidenceId)
    await loadTargetDetails()
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const saveProgress = () => {
  ElMessage.success('进度已保存')
}

const submitTask = async () => {
  try {
    await ElMessageBox.confirm('确定要提交该任务吗？', '提示')
    await v6TaskApi.submit(taskId)
    ElMessage.success('提交成功')
    router.push(`/inspection/v6/tasks/${taskId}`)
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('提交失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.v6-task-execute {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.25);
  }
}

.divider {
  width: 1px;
  height: 24px;
  background: rgba(255, 255, 255, 0.3);
}

.task-info {
  .task-title {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
  .task-date {
    font-size: 13px;
    opacity: 0.85;
  }
}

.header-center {
  display: flex;
  align-items: center;
  gap: 16px;
}

.progress-ring {
  position: relative;
  width: 48px;
  height: 48px;

  .circular-chart {
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
  }

  .circle-bg {
    fill: none;
    stroke: rgba(255, 255, 255, 0.2);
    stroke-width: 3;
  }

  .circle {
    fill: none;
    stroke: white;
    stroke-width: 3;
    stroke-linecap: round;
    transition: stroke-dasharray 0.3s;
  }

  .progress-number {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    font-size: 12px;
    font-weight: 600;
  }
}

.progress-text {
  display: flex;
  flex-direction: column;

  .progress-label {
    font-size: 12px;
    opacity: 0.85;
  }
  .progress-detail {
    font-size: 14px;
    font-weight: 500;
  }
}

.header-right {
  display: flex;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &.secondary {
    background: rgba(255, 255, 255, 0.15);
    color: white;

    &:hover {
      background: rgba(255, 255, 255, 0.25);
    }
  }

  &.primary {
    background: white;
    color: #667eea;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
  }
}

/* 主体布局 */
.main-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧面板 */
.left-panel {
  width: 320px;
  flex-shrink: 0;
  background: white;
  border-right: 1px solid #e8ecf1;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e8ecf1;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #1a1a2e;
  }

  .target-count {
    padding: 4px 10px;
    background: #f0f2f5;
    border-radius: 12px;
    font-size: 12px;
    color: #606266;
  }
}

.search-box {
  position: relative;
  padding: 12px 16px;
  border-bottom: 1px solid #e8ecf1;

  .search-icon {
    position: absolute;
    left: 28px;
    top: 50%;
    transform: translateY(-50%);
    color: #909399;
  }

  .search-input {
    width: 100%;
    padding: 10px 36px;
    border: 1px solid #e8ecf1;
    border-radius: 8px;
    font-size: 14px;
    transition: all 0.2s;

    &:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }
  }

  .clear-icon {
    position: absolute;
    right: 28px;
    top: 50%;
    transform: translateY(-50%);
    color: #909399;
    cursor: pointer;

    &:hover {
      color: #606266;
    }
  }
}

.status-tabs {
  display: flex;
  padding: 12px 16px;
  gap: 8px;
  border-bottom: 1px solid #e8ecf1;
  overflow-x: auto;

  .status-tab {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    border: none;
    border-radius: 16px;
    background: #f5f7fa;
    font-size: 13px;
    color: #606266;
    cursor: pointer;
    white-space: nowrap;
    transition: all 0.2s;

    &:hover {
      background: #e8ecf1;
    }

    &.active {
      background: #667eea;
      color: white;

      .tab-dot {
        background: white !important;
      }
    }

    .tab-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
    }

    .tab-count {
      padding: 2px 6px;
      background: rgba(0, 0, 0, 0.06);
      border-radius: 8px;
      font-size: 11px;
    }

    &.active .tab-count {
      background: rgba(255, 255, 255, 0.2);
    }
  }
}

.org-tree-scroll {
  flex: 1;
}

.org-tree {
  padding: 8px 0;
}

.org-group {
  .org-header {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: #f5f7fa;
    }

    .expand-icon {
      color: #909399;
      transition: transform 0.2s;

      &.expanded {
        transform: rotate(90deg);
      }
    }

    .org-info {
      flex: 1;
      margin: 0 12px;

      .org-name {
        font-size: 14px;
        font-weight: 500;
        color: #1a1a2e;
      }

      .org-progress-bar {
        height: 3px;
        background: #e8ecf1;
        border-radius: 2px;
        margin-top: 6px;
        overflow: hidden;

        .progress-fill {
          height: 100%;
          background: linear-gradient(90deg, #67c23a, #95d475);
          border-radius: 2px;
          transition: width 0.3s;
        }
      }
    }

    .org-count {
      font-size: 12px;
      color: #909399;
    }
  }
}

.org-children {
  padding-left: 20px;
}

.class-group {
  .class-header {
    display: flex;
    align-items: center;
    padding: 10px 16px;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: #f5f7fa;
    }

    .expand-icon {
      color: #c0c4cc;
      font-size: 12px;
      transition: transform 0.2s;

      &.expanded {
        transform: rotate(90deg);
      }
    }

    .class-name {
      flex: 1;
      margin-left: 8px;
      font-size: 13px;
      color: #606266;
    }

    .class-count {
      font-size: 12px;
      color: #909399;
    }
  }
}

.target-list {
  padding-left: 24px;
}

.target-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  margin: 2px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f5f7fa;
  }

  &.active {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
    border-left: 3px solid #667eea;
  }

  &.completed {
    .target-name {
      color: #67c23a;
    }
  }

  &.in-progress {
    background: rgba(230, 162, 60, 0.08);
  }

  &.skipped {
    opacity: 0.5;
  }

  .target-status {
    width: 20px;
    display: flex;
    justify-content: center;

    .status-icon {
      font-size: 16px;

      &.completed {
        color: #67c23a;
      }

      &.skipped {
        color: #909399;
      }
    }

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.pending {
        background: #dcdfe6;
      }

      &.in-progress {
        background: #e6a23c;
        animation: pulse 1.5s infinite;
      }
    }
  }

  .target-name {
    flex: 1;
    margin-left: 8px;
    font-size: 13px;
    color: #303133;
  }

  .target-score {
    font-size: 14px;
    font-weight: 600;
    color: #67c23a;

    small {
      font-size: 11px;
      font-weight: normal;
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.legend-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 12px;
  border-top: 1px solid #e8ecf1;
  background: #fafbfc;

  .legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #909399;
  }

  .legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.pending {
      background: #dcdfe6;
    }

    &.in-progress {
      background: #e6a23c;
    }

    &.completed {
      background: #67c23a;
    }

    &.skipped {
      background: #909399;
    }
  }
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 14px;

  .crumb {
    color: #909399;
  }

  .separator {
    color: #c0c4cc;
    font-size: 12px;
  }

  .current {
    color: #303133;
    font-weight: 500;
  }
}

.target-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  .target-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .target-main {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .target-avatar {
    width: 48px;
    height: 48px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 20px;
    font-weight: 600;
  }

  .target-details {
    h3 {
      margin: 0 0 6px 0;
      font-size: 18px;
      font-weight: 600;
      color: #1a1a2e;
    }

    .target-meta {
      display: flex;
      gap: 16px;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: #909399;
      }
    }
  }

  .target-status-badge {
    padding: 6px 14px;
    border-radius: 20px;
    font-size: 13px;
    font-weight: 500;

    &.pending {
      background: #f0f2f5;
      color: #909399;
    }

    &.locked {
      background: #fdf6ec;
      color: #e6a23c;
    }

    &.completed {
      background: #f0f9eb;
      color: #67c23a;
    }

    &.skipped {
      background: #f5f5f5;
      color: #909399;
    }
  }
}

.score-display-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  .score-main {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
  }

  .score-circle {
    width: 100px;
    height: 100px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 50%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: white;

    &.warning {
      background: linear-gradient(135deg, #e6a23c, #f5a623);
    }

    &.danger {
      background: linear-gradient(135deg, #f56c6c, #e74c3c);
    }

    .score-number {
      font-size: 32px;
      font-weight: 700;
      line-height: 1;
    }

    .score-label {
      font-size: 12px;
      opacity: 0.9;
      margin-top: 4px;
    }
  }

  .score-breakdown {
    display: flex;
    gap: 32px;

    .breakdown-item {
      text-align: center;

      .breakdown-label {
        display: block;
        font-size: 13px;
        color: #909399;
        margin-bottom: 4px;
      }

      .breakdown-value {
        font-size: 24px;
        font-weight: 600;

        &.base {
          color: #409eff;
        }

        &.deduction {
          color: #f56c6c;
        }

        &.bonus {
          color: #67c23a;
        }
      }
    }
  }

  .score-bar {
    height: 8px;
    background: #e8ecf1;
    border-radius: 4px;
    overflow: hidden;

    .score-bar-fill {
      height: 100%;
      background: linear-gradient(90deg, #667eea, #764ba2);
      border-radius: 4px;
      transition: width 0.3s;

      &.warning {
        background: linear-gradient(90deg, #e6a23c, #f5a623);
      }

      &.danger {
        background: linear-gradient(90deg, #f56c6c, #e74c3c);
      }
    }
  }
}

.categories-section {
  margin-bottom: 24px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 16px 0;
    font-size: 16px;
    font-weight: 600;
    color: #1a1a2e;
  }
}

.category-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.category-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;

  &.has-deduction {
    box-shadow: 0 2px 12px rgba(245, 108, 108, 0.15);
  }

  .category-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: #fafbfc;
    border-left: 4px solid;

    .category-title {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .category-icon {
      width: 28px;
      height: 28px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 14px;
      font-weight: 600;
    }

    .category-name {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
    }

    .category-weight {
      font-size: 12px;
      color: #909399;
      padding: 2px 8px;
      background: #e8ecf1;
      border-radius: 10px;
    }

    .category-deduction {
      font-size: 15px;
      font-weight: 600;
      color: #f56c6c;
    }
  }

  .inspection-items {
    padding: 8px 0;
  }

  .inspection-item {
    padding: 12px 20px;
    transition: background 0.2s;

    &:hover {
      background: #fafbfc;
    }

    &.checked {
      background: #fff8f8;
    }

    .item-main {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
    }

    .item-checkbox {
      width: 20px;
      height: 20px;
      border: 2px solid #dcdfe6;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;

      &.checked {
        background: #f56c6c;
        border-color: #f56c6c;
        color: white;
      }
    }

    .item-name {
      flex: 1;
      font-size: 14px;
      color: #303133;
    }

    .item-score {
      font-size: 14px;
      font-weight: 500;
      color: #f56c6c;
    }

    .item-details {
      margin-top: 12px;
      margin-left: 32px;
      padding: 12px;
      background: #fafbfc;
      border-radius: 8px;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
      border-bottom: 1px solid #e8ecf1;

      &:last-of-type {
        border-bottom: none;
      }
    }

    .detail-content {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .individual-tag {
      padding: 2px 8px;
      background: #409eff;
      color: white;
      border-radius: 4px;
      font-size: 12px;
    }

    .detail-remark {
      font-size: 13px;
      color: #909399;
    }

    .detail-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .detail-score {
      font-size: 13px;
      font-weight: 500;
      color: #f56c6c;
    }

    .detail-delete {
      padding: 4px;
      border: none;
      background: none;
      color: #909399;
      cursor: pointer;

      &:hover {
        color: #f56c6c;
      }
    }

    .detail-actions-row {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-top: 12px;
    }

    .add-individual-btn {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 6px 12px;
      border: 1px dashed #409eff;
      border-radius: 6px;
      background: none;
      color: #409eff;
      font-size: 13px;
      cursor: pointer;

      &:hover {
        background: rgba(64, 158, 255, 0.05);
      }
    }

    .remark-input {
      flex: 1;
    }
  }
}

.evidence-section {
  margin-bottom: 24px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 16px 0;
    font-size: 16px;
    font-weight: 600;
    color: #1a1a2e;
  }

  .evidence-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  .evidence-item {
    position: relative;

    .evidence-image {
      width: 100px;
      height: 100px;
      border-radius: 8px;
      object-fit: cover;
    }

    .evidence-delete {
      position: absolute;
      top: -8px;
      right: -8px;
      width: 24px;
      height: 24px;
      border-radius: 50%;
      border: none;
      background: #f56c6c;
      color: white;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.2s;
    }

    &:hover .evidence-delete {
      opacity: 1;
    }
  }

  .evidence-upload {
    .upload-placeholder {
      width: 100px;
      height: 100px;
      border: 2px dashed #dcdfe6;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: #667eea;
        background: rgba(102, 126, 234, 0.05);
      }

      .upload-icon {
        font-size: 24px;
        color: #909399;
      }

      span {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }
}

.action-section {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;

  .action-button {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 14px 28px;
    border: none;
    border-radius: 10px;
    font-size: 15px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &.secondary {
      background: #f0f2f5;
      color: #606266;

      &:hover {
        background: #e8ecf1;
      }
    }

    &.primary {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: white;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.35);
      }
    }

    &.success {
      background: linear-gradient(135deg, #67c23a, #95d475);
      color: white;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(103, 194, 58, 0.35);
      }

      &.large {
        padding: 16px 40px;
        font-size: 16px;
      }

      .arrow {
        margin-left: 4px;
      }
    }
  }

  .completed-hint {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 14px 28px;
    background: #f0f9eb;
    color: #67c23a;
    border-radius: 10px;
    font-size: 15px;
    font-weight: 500;
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;

  .empty-icon {
    width: 80px;
    height: 80px;
    background: #f5f7fa;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;

    .el-icon {
      font-size: 36px;
      color: #c0c4cc;
    }
  }

  h3 {
    margin: 0 0 8px 0;
    font-size: 18px;
    font-weight: 500;
    color: #606266;
  }

  p {
    margin: 0;
    font-size: 14px;
  }
}

/* 对话框 */
.modern-dialog {
  :deep(.el-dialog__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #e8ecf1;
  }

  :deep(.el-dialog__body) {
    padding: 24px;
  }

  :deep(.el-dialog__footer) {
    padding: 16px 24px;
    border-top: 1px solid #e8ecf1;
  }
}

.dialog-btn {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;

  &.cancel {
    background: #f0f2f5;
    border: none;
    color: #606266;
    margin-right: 12px;

    &:hover {
      background: #e8ecf1;
    }
  }

  &.confirm {
    background: linear-gradient(135deg, #667eea, #764ba2);
    border: none;
    color: white;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
    }
  }
}
</style>
