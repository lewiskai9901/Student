<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { inspProjectApi, updateProject, createPlan, cloneProject } from '@/api/inspection/project'
import { inspTemplateApi } from '@/api/inspection/template'
import { getOrgUnitTree } from '@/api/organization'
import { universalPlaceApi } from '@/api/universalPlace'
import type { PlaceTreeNode } from '@/types/universalPlace'
import { getSimpleUserList } from '@/api/user'
import type { SimpleUser } from '@/types/user'
import type { OrgUnitTreeNode } from '@/types'
import type { OrgUnit } from '@/types'
import type { TemplateSection } from '@/types/insp/template'
import type { InspProject } from '@/types/insp/project'
import { ScopeTypeConfig, TargetTypeConfig, type ScopeType, type TargetType } from '@/types/insp/enums'
import { ArrowLeft, Check, Copy, FileText } from 'lucide-vue-next'

const router = useRouter()

// ========== State ==========
const currentStep = ref(0)
const submitting = ref(false)
const loadingTemplates = ref(false)
const loadingOrg = ref(false)
// P0: 区分"加载失败"与"暂无数据" — 失败显式错误态 + 重试, 不伪装成空数据
const templatesError = ref(false)
const orgError = ref(false)
const searchKeyword = ref('')
const scopeSearchKeyword = ref('')

const rootSections = ref<TemplateSection[]>([])
const orgTree = ref<OrgUnitTreeNode[]>([])
const flatOrgUnits = ref<(OrgUnit & { depth: number })[]>([])
// V20260524 Bug#11: 模板 targetType=PLACE 时加载场所树
const placeTree = ref<PlaceTreeNode[]>([])
const flatPlaces = ref<PlaceTreeNode[]>([])
// V20260524 彻底完善: USER 维度
const userList = ref<SimpleUser[]>([])

const form = reactive({
  projectName: '',
  rootSectionId: undefined as LongId | undefined,
  scopeType: 'ORG' as ScopeType,
  scopeIds: [] as string[],
  startDate: '',
  endDate: '',
})

// ========== Mode (Phase 4 新增) ==========
// 'template' = 现有模板新建流程 (3 步向导); 'clone' = 克隆既有项目 (单步表单)
type WizardMode = 'template' | 'clone'
const mode = ref<WizardMode>('template')

// ========== Clone Mode State ==========
const loadingProjects = ref(false)
const projectsError = ref(false)
const allProjects = ref<InspProject[]>([])
const projectSearchKeyword = ref('')
const cloning = ref(false)

const cloneForm = reactive({
  sourceProjectId: undefined as LongId | undefined,
  projectName: '',
  orgUnitId: undefined as LongId | undefined,
  startDate: '',
  endDate: '',
  cloneInspectors: false,
})

// 可克隆项目: 排除已归档
const cloneableProjects = computed(() => {
  const list = allProjects.value.filter(p => p.status !== 'ARCHIVED')
  const kw = projectSearchKeyword.value.trim().toLowerCase()
  if (!kw) return list
  return list.filter(p =>
    p.projectName.toLowerCase().includes(kw) ||
    (p.projectCode || '').toLowerCase().includes(kw),
  )
})

const selectedSourceProject = computed(() =>
  allProjects.value.find(p => String(p.id) === String(cloneForm.sourceProjectId)),
)

const canClone = computed(() =>
  !!cloneForm.sourceProjectId &&
  !!cloneForm.projectName.trim() &&
  !!cloneForm.orgUnitId &&
  !!cloneForm.startDate,
)

async function loadProjectsForClone() {
  loadingProjects.value = true
  projectsError.value = false
  try {
    allProjects.value = await inspProjectApi.getList()
  } catch (e: any) {
    projectsError.value = true
    ElMessage.error('加载项目列表失败: ' + (e?.message || '未知错误'))
  } finally {
    loadingProjects.value = false
  }
}

function selectSourceProject(p: InspProject) {
  cloneForm.sourceProjectId = p.id
  if (!cloneForm.projectName) {
    cloneForm.projectName = p.projectName + ' (副本)'
  }
}

async function handleClone() {
  if (!canClone.value || !cloneForm.sourceProjectId) {
    ElMessage.warning('请先选择源项目并填写完整信息')
    return
  }
  cloning.value = true
  try {
    const created = await cloneProject(cloneForm.sourceProjectId, {
      projectName: cloneForm.projectName,
      orgUnitId: cloneForm.orgUnitId!,
      startDate: cloneForm.startDate,
      endDate: cloneForm.endDate || undefined,
      cloneInspectors: cloneForm.cloneInspectors,
    })
    ElMessage.success(`已克隆为新项目 ${created.projectName}`)
    router.push(`/inspection/projects/${created.id}`)
  } catch (e: any) {
    ElMessage.error('克隆失败: ' + (e?.message || '未知错误'))
  } finally {
    cloning.value = false
  }
}

// 切换模式时清理另一边的状态, 避免提交时混淆
function switchMode(m: WizardMode) {
  if (m === mode.value) return
  mode.value = m
  // 切到 clone 时按需加载项目列表
  if (m === 'clone' && allProjects.value.length === 0 && !loadingProjects.value) {
    loadProjectsForClone()
  }
}

// ========== Template Info ==========
const selectedSection = computed(() =>
  rootSections.value.find(s => s.id === form.rootSectionId)
)

const templateTargetType = computed<TargetType | null>(() => {
  return selectedSection.value?.targetType as TargetType | null
})

// ========== Computed ==========
const filteredSections = computed(() => {
  // P0: 项目向导只允许选 PUBLISHED 模板, 草稿/废弃/归档模板不可用
  let list = rootSections.value.filter(s => s.status === 'PUBLISHED')
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return list
  return list.filter(s =>
    s.sectionName.toLowerCase().includes(kw) ||
    s.sectionCode.toLowerCase().includes(kw) ||
    (s.description || '').toLowerCase().includes(kw)
  )
})

const selectedScopeLabel = computed(() => {
  const cfg = ScopeTypeConfig[form.scopeType]
  return cfg ? cfg.label : form.scopeType
})

const selectedOrgNames = computed(() => {
  if (form.scopeIds.length === 0) return '未指定'
  const names = form.scopeIds
    .map(id => flatOrgUnits.value.find(u => String(u.id) === String(id))?.unitName)
    .filter(Boolean)
  if (names.length <= 3) return names.join(', ')
  return names.slice(0, 2).join(', ') + ` 等${names.length}个`
})

const dateRange = computed(() => {
  if (!form.startDate) return '未设置'
  if (!form.endDate) return form.startDate + ' 起'
  return form.startDate + ' ~ ' + form.endDate
})

const canProceedStep0 = computed(() => !!form.rootSectionId)
// P0: Step1 校验必须含 scopeIds, 否则可创建空范围项目
const canProceedStep1 = computed(() =>
  !!form.projectName.trim() && !!form.startDate && form.scopeIds.length > 0
)

function isPublished(section: TemplateSection): boolean {
  return section.status === 'PUBLISHED'
}

// ========== Scope Tree ==========

// V20260524 Bug#11: 按 scopeType 收集 quick-filter 类型
const availableUnitTypes = computed(() => {
  const typeMap = new Map<string, { code: string; name: string; count: number }>()
  if (form.scopeType === 'PLACE') {
    for (const p of flatPlaces.value) {
      const code = p.typeCode || ''
      if (!code) continue
      const existing = typeMap.get(code)
      if (existing) existing.count++
      else typeMap.set(code, { code, name: p.typeName || code, count: 1 })
    }
  } else {
    for (const unit of flatOrgUnits.value) {
      const code = unit.unitType || ''
      if (!code) continue
      const existing = typeMap.get(code)
      if (existing) existing.count++
      else typeMap.set(code, { code, name: unit.typeName || code, count: 1 })
    }
  }
  return Array.from(typeMap.values())
})

// Active type filter for quick-filter buttons
const activeTypeFilter = ref<string | null>(null)

// Filter tree nodes based on search keyword and type filter
function filterOrgNode(node: OrgUnitTreeNode): OrgUnitTreeNode | null {
  const kw = scopeSearchKeyword.value.trim().toLowerCase()
  const typeFilter = activeTypeFilter.value

  const filteredChildren = (node.children || [])
    .map(c => filterOrgNode(c))
    .filter(Boolean) as OrgUnitTreeNode[]

  const selfMatch =
    (!kw || node.unitName.toLowerCase().includes(kw) || (node.typeName || '').toLowerCase().includes(kw)) &&
    (!typeFilter || node.unitType === typeFilter)

  if (selfMatch || filteredChildren.length > 0) {
    return { ...node, children: filteredChildren.length > 0 ? filteredChildren : (selfMatch ? node.children : filteredChildren) }
  }
  return null
}

const filteredOrgTree = computed(() => {
  if (!scopeSearchKeyword.value && !activeTypeFilter.value) return orgTree.value
  return orgTree.value.map(n => filterOrgNode(n)).filter(Boolean) as OrgUnitTreeNode[]
})

// V20260524 Bug#11: 按 scopeType 切换加载源 (ORG / PLACE / USER)
function mapOrgNode(node: OrgUnitTreeNode): any {
  return {
    id: String(node.id),
    label: node.unitName,
    typeName: node.typeName || node.unitType || '',
    unitType: node.unitType || '',
    children: (node.children || []).map(mapOrgNode),
  }
}
function mapPlaceNode(node: PlaceTreeNode): any {
  return {
    id: String(node.id),
    label: node.placeName,
    typeName: node.typeName || node.typeCode || '',
    unitType: node.typeCode || '',
    children: (node.children || []).map(mapPlaceNode),
  }
}

/** 过滤场所树 (按 keyword + activeTypeFilter) */
function filterPlaceNode(node: PlaceTreeNode): PlaceTreeNode | null {
  const matchName = !scopeSearchKeyword.value
    || node.placeName.toLowerCase().includes(scopeSearchKeyword.value.toLowerCase())
  const matchType = !activeTypeFilter.value || node.typeCode === activeTypeFilter.value
  const filteredChildren = (node.children || [])
    .map(c => filterPlaceNode(c))
    .filter(Boolean) as PlaceTreeNode[]
  if (matchName && matchType) return { ...node, children: filteredChildren }
  if (filteredChildren.length > 0) return { ...node, children: filteredChildren }
  return null
}

const filteredPlaceTree = computed(() => {
  if (!scopeSearchKeyword.value && !activeTypeFilter.value) return placeTree.value
  return placeTree.value.map(n => filterPlaceNode(n)).filter(Boolean) as PlaceTreeNode[]
})

// el-tree data format — 按 scopeType 选数据源
const treeData = computed(() => {
  if (form.scopeType === 'USER') {
    // USER 模式: 平铺成"扁平树" (无 children), 每人一行
    const kw = scopeSearchKeyword.value.trim().toLowerCase()
    return userList.value
      .filter(u => !kw || (u.realName || u.username || '').toLowerCase().includes(kw)
                || (u.orgUnitName || '').toLowerCase().includes(kw))
      .map(u => ({
        id: String(u.id),
        label: (u.realName || u.username || ('#' + u.id)) as string,
        typeName: u.orgUnitName || '',
        unitType: '',
        children: [],
      }))
  }
  if (form.scopeType === 'PLACE') return filteredPlaceTree.value.map(mapPlaceNode)
  return filteredOrgTree.value.map(mapOrgNode)
})

const treeRef = ref<any>(null)

// Handle check change from el-tree
function handleTreeCheck() {
  if (!treeRef.value) return
  const checked = treeRef.value.getCheckedKeys(false) as string[]
  form.scopeIds = checked
}

// Select all visible nodes
function selectAll() {
  if (!treeRef.value) return
  let allKeys: string[]
  if (form.scopeType === 'USER') {
    allKeys = userList.value.map(u => String(u.id))
  } else if (form.scopeType === 'PLACE') {
    allKeys = flatPlaces.value
      .filter(p => !activeTypeFilter.value || p.typeCode === activeTypeFilter.value)
      .map(p => String(p.id))
  } else {
    allKeys = flatOrgUnits.value
      .filter(u => !activeTypeFilter.value || u.unitType === activeTypeFilter.value)
      .map(u => String(u.id))
  }
  for (const key of allKeys) {
    treeRef.value.setChecked(key, true, false)
  }
  handleTreeCheck()
}

// Deselect all
function deselectAll() {
  if (!treeRef.value) return
  treeRef.value.setCheckedKeys([])
  form.scopeIds = []
}

// Select all nodes of a specific type
function selectByType(typeCode: string) {
  if (!treeRef.value) return
  let keys: string[]
  if (form.scopeType === 'PLACE') {
    keys = flatPlaces.value.filter(p => p.typeCode === typeCode).map(p => String(p.id))
  } else {
    keys = flatOrgUnits.value.filter(u => u.unitType === typeCode).map(u => String(u.id))
  }
  for (const key of keys) {
    treeRef.value.setChecked(key, true, false)
  }
  handleTreeCheck()
}

// Toggle type filter button
function toggleTypeFilter(code: string) {
  activeTypeFilter.value = activeTypeFilter.value === code ? null : code
}

// Sync checked keys when tree data changes
watch(treeData, () => {
  nextTick(() => {
    if (treeRef.value && form.scopeIds.length > 0) {
      treeRef.value.setCheckedKeys(form.scopeIds)
    }
  })
})

// ========== Navigation ==========
function nextStep() {
  if (currentStep.value === 0 && !canProceedStep0.value) {
    ElMessage.warning('请先选择一个已发布的模板')
    return
  }
  if (currentStep.value === 1 && !canProceedStep1.value) {
    if (!form.projectName.trim() || !form.startDate) {
      ElMessage.warning('请填写项目名称和开始日期')
    } else {
      ElMessage.warning('请至少选择一个检查范围目标')
    }
    return
  }
  if (currentStep.value < 2) currentStep.value++
}

function prevStep() {
  if (currentStep.value > 0) currentStep.value--
}

// ========== Step 0: 选择模板 ==========
function selectSection(section: TemplateSection) {
  if (!isPublished(section)) return
  form.rootSectionId = section.id
  if (!form.projectName) {
    form.projectName = section.sectionName + ' 检查'
  }
  // Auto-set scopeType based on template targetType
  if (section.targetType === 'ORG') form.scopeType = 'ORG'
  else if (section.targetType === 'PLACE') form.scopeType = 'PLACE'
  else if (section.targetType === 'USER') form.scopeType = 'USER'
}

// ========== Step 2: 创建 ==========
async function handleCreate() {
  submitting.value = true
  // P0: 向导 3 步串行写无原子性. 一旦项目实体已建出来, 后续 scope/plan 失败
  // 不能笼统报错丢弃 — 跳转到已建项目详情页, 让用户在那里补全, 避免重复空项目.
  let createdProjectId: LongId | undefined
  try {
    // Step A: create 时传 rootSectionId, 否则项目和模板脱钩
    const project = await inspProjectApi.create({
      projectName: form.projectName,
      rootSectionId: form.rootSectionId!,
      startDate: form.startDate,
      // 项目数据权限边界 — 取检查范围首个组织 (Step1 已校验 scopeIds 非空)
      orgUnitId: form.scopeIds[0],
    })
    createdProjectId = project.id

    // Step B: scope+date 总是 update, 否则向导 step2 选的范围会丢
    const updateData: Record<string, any> = {
      scopeType: form.scopeType,
    }
    if (form.endDate) updateData.endDate = form.endDate
    if (form.scopeIds.length > 0) {
      updateData.scopeConfig = JSON.stringify(form.scopeIds.map(String))
    }
    await updateProject(project.id, updateData)

    // Step C: 默认计划
    if (form.rootSectionId) {
      await createPlan({
        projectId: project.id,
        planName: form.projectName + ' 默认计划',
        rootSectionId: form.rootSectionId,
        sectionIds: '[]',
        scheduleMode: 'ON_DEMAND',
        ratersPerTarget: 1,
      })
    }

    ElMessage.success('项目已创建')
    router.push(`/inspection/projects/${project.id}`)
  } catch (e: any) {
    const msg = e?.message || '未知错误'
    if (createdProjectId) {
      // 项目实体已落库, 仅范围/计划未保存 — 跳详情页补全, 不要让用户重复建项目
      ElMessage.error(`项目已创建, 但范围/计划未保存 (${msg})，请在详情页补全`)
      router.push(`/inspection/projects/${createdProjectId}`)
    } else {
      ElMessage.error('创建失败: ' + msg)
    }
  } finally {
    submitting.value = false
  }
}

// ========== Init ==========
async function loadTemplates() {
  loadingTemplates.value = true
  templatesError.value = false
  try {
    const result = await inspTemplateApi.getList({ page: 1, size: 200 })
    rootSections.value = result.records
  } catch (e: any) {
    templatesError.value = true
    ElMessage.error('加载模板列表失败: ' + (e?.message || '未知错误'))
  } finally {
    loadingTemplates.value = false
  }
}

function flattenTree(nodes: OrgUnitTreeNode[], depth = 0): Array<OrgUnit & { depth: number }> {
  const result: Array<OrgUnit & { depth: number }> = []
  for (const node of nodes) {
    result.push({ ...node, depth } as any)
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children, depth + 1))
    }
  }
  return result
}

async function loadOrgUnits() {
  loadingOrg.value = true
  orgError.value = false
  try {
    const tree = await getOrgUnitTree()
    orgTree.value = tree
    flatOrgUnits.value = flattenTree(tree) as any[]
  } catch (e: any) {
    orgError.value = true
    ElMessage.error('加载组织单元失败: ' + (e?.message || '未知错误'))
  } finally {
    loadingOrg.value = false
  }
}

/** V20260524 Bug#11: 加载场所树 (模板 targetType=PLACE 时调) */
async function loadPlaces() {
  loadingOrg.value = true
  orgError.value = false
  try {
    const tree = await universalPlaceApi.getTree()
    placeTree.value = tree
    // flat: 递归展开供 selectAll / selectByType 用
    const flat: PlaceTreeNode[] = []
    function walk(list: PlaceTreeNode[]) {
      for (const n of list) {
        flat.push(n)
        if (n.children?.length) walk(n.children)
      }
    }
    walk(tree)
    flatPlaces.value = flat
  } catch (e: any) {
    orgError.value = true
    ElMessage.error('加载场所失败: ' + (e?.message || '未知错误'))
  } finally {
    loadingOrg.value = false
  }
}

/** V20260524 USER: 加载所有用户 (无 keyword 拿全量, 默认 200 上限由后端控) */
async function loadUsers() {
  loadingOrg.value = true
  orgError.value = false
  try {
    userList.value = await getSimpleUserList()
  } catch (e: any) {
    orgError.value = true
    ElMessage.error('加载人员失败: ' + (e?.message || '未知错误'))
  } finally {
    loadingOrg.value = false
  }
}

/** 根据 scopeType 切换数据源, 选完模板自动触发 */
watch(() => form.scopeType, (st) => {
  form.scopeIds = [] // 切换 scope 类型, 清空已选 (不同 ID 空间)
  if (st === 'PLACE' && placeTree.value.length === 0) loadPlaces()
  else if (st === 'USER' && userList.value.length === 0) loadUsers()
  else if (st === 'ORG' && orgTree.value.length === 0) loadOrgUnits()
})

onMounted(() => {
  loadTemplates()
  loadOrgUnits()  // 默认先加载 ORG (大多数场景), PLACE 切换时按需懒加载
})
</script>

<template>
  <div class="insp-shell wz-page">
    <!-- Header -->
    <header class="wz-head">
      <button class="wz-back" @click="router.back()" title="返回">
        <ArrowLeft :size="14" />
      </button>
      <div class="wz-head__lead">
        <span class="insp-eyebrow">新建项目 · New Campaign</span>
        <h1 class="wz-title">创建检查项目</h1>
      </div>
      <div class="wz-head__hint">
        <template v-if="mode === 'template'">共 3 步 · 当前 <strong class="insp-num">{{ currentStep + 1 }}</strong> / 3</template>
        <template v-else>克隆模式 · 单步表单</template>
      </div>
    </header>

    <!-- Step indicator (仅模板新建模式; 克隆模式单步无需向导) -->
    <nav v-if="mode === 'template'" class="wz-rail">
      <button
        v-for="(label, idx) in ['选择模板', '配置范围', '确认创建']" :key="idx"
        class="wz-rail__step"
        :class="{
          'is-done': idx < currentStep,
          'is-active': idx === currentStep,
          'is-pending': idx > currentStep,
          'is-clickable': idx < currentStep,
        }"
        :disabled="idx > currentStep"
        @click="idx < currentStep && (currentStep = idx)"
      >
        <span class="wz-rail__num">
          <Check v-if="idx < currentStep" :size="11" />
          <span v-else class="insp-num">{{ idx + 1 }}</span>
        </span>
        <span class="wz-rail__label">{{ label }}</span>
        <span v-if="idx === currentStep" class="wz-rail__cursor">v</span>
      </button>
    </nav>

    <!-- ==================== Step 0: 次级模式切换 ==================== -->
    <!-- IA 修正: 默认走"用模板新建"主路径, 克隆作为次级入口 (右上小链接),
         避免让所有新用户进门先选择,降低认知负担. -->
    <div v-if="currentStep === 0" class="wz-mode-link">
      <a v-if="mode === 'template'" class="wz-mode-link-a" @click.prevent="switchMode('clone')">
        <Copy :size="12" /> 或从已有项目克隆 →
      </a>
      <a v-else class="wz-mode-link-a" @click.prevent="switchMode('template')">
        <FileText :size="12" /> ← 返回用模板新建
      </a>
    </div>

    <div class="wz-steps">
    <Transition name="wz-step" mode="out-in">
    <!-- ==================== Step 0 · Mode A: 选择模板 ==================== -->
    <section v-if="currentStep === 0 && mode === 'template'" key="step0-template" class="wz-card">
      <header class="wz-card__head">
        <span class="wz-card__title">选择检查模板</span>
        <div class="wz-card__search">
          <input v-model="searchKeyword" type="text" placeholder="搜索模板名称..." />
          <button v-if="searchKeyword" class="wz-card__clear" @click="searchKeyword = ''" title="清除">×</button>
        </div>
      </header>

      <div v-if="loadingTemplates" class="wz-state">加载模板中…</div>
      <div v-else-if="templatesError" class="wz-state wz-state--error">
        <span>模板列表加载失败</span>
        <button class="insp-btn insp-btn--sm" @click="loadTemplates">重试</button>
      </div>
      <div v-else-if="rootSections.length === 0" class="wz-state">暂无模板, 请先在模板管理中创建</div>
      <div v-else-if="filteredSections.length === 0" class="wz-state">未找到匹配的模板</div>

      <ul v-else class="tpl-list">
        <li
          v-for="section in filteredSections" :key="section.id"
          class="tpl-row"
          :class="{
            'is-selected': form.rootSectionId === section.id,
            'is-disabled': !isPublished(section),
          }"
          @click="selectSection(section)"
        >
          <div class="tpl-radio">
            <span class="tpl-radio__dot" :class="{ 'is-on': form.rootSectionId === section.id }" />
          </div>
          <div class="tpl-main">
            <div class="tpl-line1">
              <span class="tpl-name">{{ section.sectionName }}</span>
              <span class="tpl-version insp-num">v{{ section.latestVersion }}</span>
              <span class="insp-chip"
                    :class="isPublished(section) ? 'insp-chip--pass' : 'insp-chip--pending'">
                {{ isPublished(section) ? '已发布' : '未发布' }}
              </span>
              <span v-if="section.targetType" class="insp-chip insp-chip--info">
                {{ TargetTypeConfig[section.targetType as TargetType]?.label || section.targetType }}
              </span>
            </div>
            <div v-if="section.description" class="tpl-desc">{{ section.description }}</div>
            <div v-if="!isPublished(section)" class="tpl-warn">> 需要先在模板编辑页发布才能选择</div>
          </div>
        </li>
      </ul>
    </section>

    <!-- ==================== Step 0 · Mode B: 克隆既有项目 ==================== -->
    <section v-else-if="currentStep === 0 && mode === 'clone'" key="step0-clone" class="wz-card">
      <header class="wz-card__head">
        <span class="wz-card__title">克隆既有项目</span>
        <div class="wz-card__hint">深拷贝项目设置 / 评分方案 / 检查计划 / 指标 · 不拷贝执行数据 (任务/提交/分数)</div>
      </header>

      <div class="wz-form">
        <!-- Source project picker -->
        <div class="wz-fld">
          <span class="wz-fld__label">源项目 <span class="wz-req">*</span></span>
          <div class="wz-card__search" style="margin-bottom: 8px">
            <input v-model="projectSearchKeyword" type="text" placeholder="搜索源项目名称 / Code..." />
            <button v-if="projectSearchKeyword" class="wz-card__clear" @click="projectSearchKeyword = ''" title="清除">×</button>
          </div>
          <div v-if="loadingProjects" class="wz-state wz-state--small">加载项目列表…</div>
          <div v-else-if="projectsError" class="wz-state wz-state--small wz-state--error">
            <span>项目列表加载失败</span>
            <button class="insp-btn insp-btn--sm" @click="loadProjectsForClone">重试</button>
          </div>
          <div v-else-if="cloneableProjects.length === 0" class="wz-state wz-state--small">
            {{ projectSearchKeyword ? '未找到匹配项目' : '暂无可克隆项目 (已排除归档)' }}
          </div>
          <ul v-else class="tpl-list" style="max-height: 320px; overflow-y: auto">
            <li
              v-for="p in cloneableProjects" :key="p.id"
              class="tpl-row"
              :class="{ 'is-selected': String(cloneForm.sourceProjectId) === String(p.id) }"
              @click="selectSourceProject(p)"
            >
              <div class="tpl-radio">
                <span class="tpl-radio__dot" :class="{ 'is-on': String(cloneForm.sourceProjectId) === String(p.id) }" />
              </div>
              <div class="tpl-main">
                <div class="tpl-line1">
                  <span class="tpl-name">{{ p.projectName }}</span>
                  <span v-if="p.projectCode" class="tpl-version insp-num">{{ p.projectCode }}</span>
                  <span class="insp-chip insp-chip--info">{{ p.status }}</span>
                </div>
                <div v-if="p.startDate" class="tpl-desc">
                  {{ p.startDate }}<template v-if="p.endDate"> ~ {{ p.endDate }}</template>
                </div>
              </div>
            </li>
          </ul>
        </div>

        <!-- New project name -->
        <label class="wz-fld">
          <span class="wz-fld__label">新项目名称 <span class="wz-req">*</span></span>
          <input v-model="cloneForm.projectName" type="text" class="wz-input" placeholder="输入新项目名称" maxlength="100" />
        </label>

        <!-- Org unit + dates -->
        <div class="wz-row">
          <label class="wz-fld">
            <span class="wz-fld__label">所属组织 <span class="wz-req">*</span></span>
            <el-tree-select
              v-model="cloneForm.orgUnitId"
              :data="orgTree"
              :props="{ value: 'id', label: 'unitName', children: 'children' }"
              :render-after-expand="false"
              check-strictly
              filterable
              placeholder="选择新项目所属组织"
              class="wz-input"
              style="width: 100%"
            />
          </label>
        </div>
        <div class="wz-row">
          <label class="wz-fld">
            <span class="wz-fld__label">开始日期 <span class="wz-req">*</span></span>
            <input v-model="cloneForm.startDate" type="date" class="wz-input" />
          </label>
          <label class="wz-fld">
            <span class="wz-fld__label">结束日期 · 可选</span>
            <input v-model="cloneForm.endDate" type="date" class="wz-input" />
          </label>
        </div>

        <!-- Inspectors toggle -->
        <label class="wz-fld" style="flex-direction: row; align-items: center; gap: 8px">
          <input v-model="cloneForm.cloneInspectors" type="checkbox" style="margin: 0" />
          <span class="wz-fld__label" style="margin: 0">同时克隆检查员名单</span>
          <span class="wz-fld__hint" style="margin-left: auto">默认不克隆 — 每项目通常需重新指定检查员</span>
        </label>

        <!-- Preview -->
        <div v-if="selectedSourceProject" class="wz-tip">
          <span class="wz-tip__icon">i</span>
          将基于 <strong>{{ selectedSourceProject.projectName }}</strong> 创建新项目, 包含其全部评分方案 / 检查计划 / 指标配置.
          新项目初始为 DRAFT 状态, 可在详情页继续微调.
        </div>
      </div>
    </section>

    <!-- ==================== Step 1: 配置范围 ==================== -->
    <section v-else-if="currentStep === 1" key="step1" class="wz-card">
      <header class="wz-card__head">
        <span class="wz-card__title">配置项目信息与检查范围</span>
      </header>

      <div class="wz-form">
        <!-- Name -->
        <label class="wz-fld">
          <span class="wz-fld__label">项目名称 <span class="wz-req">*</span></span>
          <input v-model="form.projectName" type="text" class="wz-input" placeholder="输入项目名称" maxlength="100" />
        </label>

        <!-- Date row -->
        <div class="wz-row">
          <label class="wz-fld">
            <span class="wz-fld__label">开始日期 <span class="wz-req">*</span></span>
            <input v-model="form.startDate" type="date" class="wz-input" />
          </label>
          <label class="wz-fld">
            <span class="wz-fld__label">结束日期 · 可选</span>
            <input v-model="form.endDate" type="date" class="wz-input" />
          </label>
        </div>

        <!-- Scope -->
        <div class="wz-fld">
          <span class="wz-fld__label">
            检查范围 <span class="wz-req">*</span>
            <span v-if="templateTargetType" class="wz-fld__hint">
              模板目标: {{ TargetTypeConfig[templateTargetType]?.label || templateTargetType }}
            </span>
          </span>

          <!-- Type filter row -->
          <div v-if="availableUnitTypes.length > 0" class="scope-types">
            <button
              v-for="ut in availableUnitTypes" :key="ut.code"
              class="scope-type"
              :class="{ 'is-active': activeTypeFilter === ut.code }"
              @click="toggleTypeFilter(ut.code)"
            >
              {{ ut.name }}
              <span class="insp-num scope-type__count">{{ ut.count }}</span>
            </button>
          </div>

          <!-- Action buttons + search -->
          <div class="scope-actions">
            <button class="insp-btn insp-btn--sm" @click="selectAll">全选</button>
            <button class="insp-btn insp-btn--sm" @click="deselectAll">清空</button>
            <template v-if="availableUnitTypes.length > 0">
              <span class="scope-actions__sep" />
              <button
                v-for="ut in availableUnitTypes" :key="'sel-' + ut.code"
                class="insp-btn insp-btn--sm"
                @click="selectByType(ut.code)"
              >选全部 {{ ut.name }}</button>
            </template>
            <div class="scope-actions__spacer" />
            <input v-model="scopeSearchKeyword" class="scope-search"
              :placeholder="form.scopeType === 'PLACE' ? '搜索场所...' : (form.scopeType === 'USER' ? '搜索人员...' : '搜索组织...')" />
          </div>

          <!-- Tree -->
          <div class="scope-tree-wrap">
            <div v-if="loadingOrg" class="wz-state wz-state--small">加载中...</div>
            <div v-else-if="orgError" class="wz-state wz-state--small wz-state--error">
              <span>{{ form.scopeType === 'PLACE' ? '场所' : (form.scopeType === 'USER' ? '人员' : '组织单元') }}加载失败</span>
              <button class="insp-btn insp-btn--sm"
                @click="form.scopeType === 'PLACE' ? loadPlaces() : (form.scopeType === 'USER' ? loadUsers() : loadOrgUnits())">重试</button>
            </div>
            <div v-else-if="treeData.length === 0" class="wz-state wz-state--small">暂无{{ form.scopeType === 'PLACE' ? '场所' : (form.scopeType === 'USER' ? '人员' : '组织单元') }}</div>
            <el-tree
              v-else
              ref="treeRef"
              :data="treeData" show-checkbox node-key="id"
              :default-expand-all="true" :check-strictly="true"
              :default-checked-keys="form.scopeIds"
              :props="{ label: 'label', children: 'children' }"
              @check="handleTreeCheck"
              class="scope-tree"
            >
              <template #default="{ data }">
                <span class="scope-tree-node">
                  <span class="scope-tree-name">{{ data.label }}</span>
                  <span v-if="data.typeName" class="scope-tree-type">{{ data.typeName }}</span>
                </span>
              </template>
            </el-tree>
          </div>

          <div v-if="form.scopeIds.length > 0" class="scope-summary">
            <Check :size="12" />
            已选 <strong class="insp-num">{{ form.scopeIds.length }}</strong> 个目标
          </div>
        </div>
      </div>
    </section>

    <!-- ==================== Step 2: 确认创建 ==================== -->
    <section v-else key="step2" class="wz-card">
      <header class="wz-card__head">
        <span class="wz-card__title">确认创建</span>
        <span class="wz-card__hint">检查信息无误后点击右下"创建项目"</span>
      </header>

      <dl class="wz-summary">
        <div class="wz-srow">
          <dt>项目名称</dt>
          <dd class="wz-srow__value">{{ form.projectName || '—' }}</dd>
        </div>
        <div class="wz-srow">
          <dt>模板</dt>
          <dd class="wz-srow__value">
            {{ selectedSection?.sectionName || '—' }}
            <span v-if="selectedSection" class="insp-chip insp-chip--info">v{{ selectedSection.latestVersion }}</span>
            <span v-if="templateTargetType" class="insp-chip insp-chip--pending">
              {{ TargetTypeConfig[templateTargetType]?.label }}
            </span>
          </dd>
        </div>
        <div class="wz-srow">
          <dt>检查范围</dt>
          <dd class="wz-srow__value">
            {{ selectedOrgNames }}
            <span v-if="form.scopeIds.length > 0" class="wz-srow__count insp-num">{{ form.scopeIds.length }} 个</span>
          </dd>
        </div>
        <div class="wz-srow">
          <dt>日期</dt>
          <dd class="wz-srow__value insp-num">{{ dateRange }}</dd>
        </div>
      </dl>

      <div class="wz-tip">
        <span class="wz-tip__icon">i</span>
        创建后将进入项目详情页, 可继续配置检查计划/评级维度/检查员; 发布后系统自动生成检查任务.
      </div>
    </section>
    </Transition>
    </div>

    <!-- Footer -->
    <footer class="wz-foot">
      <template v-if="mode === 'template'">
        <button v-if="currentStep > 0" class="insp-btn" @click="prevStep">
          &lt; 上一步
        </button>
        <span v-else />
        <div class="wz-foot__spacer" />
        <button
          v-if="currentStep < 2"
          class="insp-btn insp-btn--accent"
          :disabled="currentStep === 0 ? !canProceedStep0 : !canProceedStep1"
          @click="nextStep"
        >
          下一步 >
        </button>
        <button
          v-if="currentStep === 2"
          class="insp-btn insp-btn--accent"
          :disabled="submitting"
          @click="handleCreate"
        >
          {{ submitting ? '创建中…' : '创建项目' }}
        </button>
      </template>
      <template v-else>
        <span />
        <div class="wz-foot__spacer" />
        <button
          class="insp-btn insp-btn--accent"
          :disabled="!canClone || cloning"
          @click="handleClone"
        >
          <Copy :size="13" />
          {{ cloning ? '克隆中…' : '克隆创建' }}
        </button>
      </template>
    </footer>
  </div>
</template>

<style scoped>
.wz-page {
  padding: 12px 16px;
  max-width: 1100px;
  margin: 0 auto;
}

/* ─ Head ─────── */
.wz-head {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 10px 14px;
  margin-bottom: 10px;
}
.wz-back {
  display: inline-flex;
  align-items: center; justify-content: center;
  width: 28px; height: 28px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-strong);
  border-radius: var(--insp-radius-sm);
  color: var(--insp-ink-tertiary);
  cursor: pointer;
  transition: all var(--insp-t-fast);
}
.wz-back:hover { color: var(--insp-accent); border-color: var(--insp-accent); }
.wz-head__lead { display: flex; flex-direction: column; gap: 2px; }
.wz-title {
  font-size: 16px; font-weight: 700;
  margin: 0;
  color: var(--insp-ink-primary);
}
.wz-head__hint {
  margin-left: auto;
  font-size: 12px;
  color: var(--insp-ink-tertiary);
}
.wz-head__hint strong {
  color: var(--insp-accent);
  font-weight: 700;
}

/* ─ Step rail ─────── */
.wz-rail {
  display: flex;
  align-items: stretch;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 0;
  margin-bottom: 10px;
  overflow: hidden;
}
.wz-rail__step {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: transparent;
  border: 0;
  border-right: 1px solid var(--insp-border-subtle);
  cursor: not-allowed;
  font-family: inherit;
  font-size: 12px;
  color: var(--insp-ink-tertiary);
  position: relative;
  transition: all var(--insp-t-fast);
}
.wz-rail__step:last-child { border-right: 0; }
.wz-rail__step.is-clickable { cursor: pointer; }
.wz-rail__step.is-clickable:hover { background: var(--insp-bg-subtle); }
.wz-rail__step.is-active {
  background: var(--insp-accent-paler);
  color: var(--insp-accent);
  font-weight: 600;
}
.wz-rail__step.is-done { color: var(--insp-pass); }

.wz-rail__num {
  display: inline-flex;
  align-items: center; justify-content: center;
  width: 22px; height: 22px;
  border-radius: 50%;
  background: var(--insp-bg-subtle);
  border: 1px solid var(--insp-border-default);
  font-family: var(--insp-font-mono);
  font-size: 11px; font-weight: 600;
  color: var(--insp-ink-tertiary);
}
.wz-rail__step.is-active .wz-rail__num {
  background: var(--insp-accent);
  border-color: var(--insp-accent);
  color: white;
}
.wz-rail__step.is-done .wz-rail__num {
  background: var(--insp-pass);
  border-color: var(--insp-pass);
  color: white;
}

.wz-rail__label {
  font-size: 13px;
  font-weight: 500;
}
.wz-rail__cursor {
  margin-left: auto;
  color: var(--insp-accent);
  font-size: 9px;
}

/* ─ Steps container (transition anchor) ─────── */
.wz-steps { position: relative; }

/* ─ Card ─────── */
.wz-card {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  margin-bottom: 10px;
  overflow: hidden;
}
.wz-card__head {
  display: flex; align-items: center; justify-content: space-between;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--insp-border-subtle);
}
.wz-card__title {
  font-size: 13px; font-weight: 600;
  color: var(--insp-ink-primary);
}
.wz-card__hint {
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}

.wz-card__search {
  position: relative;
  display: flex; align-items: center;
}
.wz-card__search input {
  height: 26px;
  padding: 0 10px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  font-size: 12px;
  font-family: inherit;
  width: 200px;
  background: var(--insp-bg-surface);
}
.wz-card__search input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}
.wz-card__clear {
  position: absolute;
  right: 4px;
  width: 18px; height: 18px;
  border: 0;
  background: transparent;
  font-size: 14px;
  color: var(--insp-ink-tertiary);
  cursor: pointer;
}

/* ─ State ─────── */
.wz-state {
  padding: 40px 20px;
  text-align: center;
  font-size: 12px;
  color: var(--insp-ink-tertiary);
}
.wz-state--small { padding: 20px; }
.wz-state--error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--insp-fail);
}

/* ─ Step transition (P1 #9) ─────── */
.wz-step-enter-active,
.wz-step-leave-active {
  transition: opacity var(--insp-t-medium) var(--insp-ease-out),
              transform var(--insp-t-medium) var(--insp-ease-out);
}
.wz-step-enter-from { opacity: 0; transform: translateX(12px); }
.wz-step-leave-to { opacity: 0; transform: translateX(-12px); }
.wz-step-leave-active { position: absolute; width: 100%; }

/* ─ Template list ─────── */
.tpl-list {
  list-style: none; margin: 0; padding: 0;
}
.tpl-row {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 10px;
  align-items: start;
  padding: 12px 14px;
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
}
.tpl-row:last-child { border-bottom: 0; }
.tpl-row:hover { background: var(--insp-bg-subtle); }
.tpl-row.is-selected {
  background: var(--insp-accent-paler);
}
.tpl-row.is-disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.tpl-radio {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
}
.tpl-radio__dot {
  display: block;
  width: 14px; height: 14px;
  border: 2px solid var(--insp-border-strong);
  border-radius: 50%;
  background: var(--insp-bg-surface);
  position: relative;
}
.tpl-radio__dot.is-on {
  border-color: var(--insp-accent);
  background: var(--insp-accent);
}
.tpl-radio__dot.is-on::after {
  content: '';
  position: absolute;
  inset: 3px;
  background: white;
  border-radius: 50%;
}

.tpl-main { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.tpl-line1 {
  display: flex; align-items: center; gap: 6px;
  flex-wrap: wrap;
}
.tpl-name {
  font-size: 13px; font-weight: 600;
  color: var(--insp-ink-primary);
}
.tpl-version {
  font-family: var(--insp-font-mono);
  font-size: 11px; font-weight: 600;
  color: var(--insp-accent);
  padding: 1px 6px;
  border: 1px solid var(--insp-accent-pale);
  border-radius: 3px;
}
.tpl-desc {
  font-size: 12px;
  color: var(--insp-ink-tertiary);
  line-height: 1.5;
}
.tpl-warn {
  font-size: 11px;
  color: var(--insp-fail);
  font-weight: 500;
}

/* ─ Form ─────── */
.wz-form {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.wz-fld {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.wz-fld__label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--insp-ink-secondary);
}
.wz-fld__hint {
  font-size: 11px;
  color: var(--insp-ink-tertiary);
  font-weight: 400;
}
.wz-req {
  color: var(--insp-fail);
  font-weight: 700;
}
.wz-input {
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  font-size: 13px;
  font-family: inherit;
  background: var(--insp-bg-surface);
  color: var(--insp-ink-primary);
}
.wz-input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}

.wz-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

/* ─ Scope ─────── */
.scope-types {
  display: flex; gap: 4px;
  margin-top: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.scope-type {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 24px;
  padding: 0 10px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-pill);
  background: var(--insp-bg-surface);
  font-family: inherit;
  font-size: 11px;
  font-weight: 500;
  color: var(--insp-ink-secondary);
  cursor: pointer;
  transition: all var(--insp-t-fast);
}
.scope-type:hover { border-color: var(--insp-accent); color: var(--insp-accent); }
.scope-type.is-active {
  background: var(--insp-accent);
  color: white;
  border-color: var(--insp-accent);
}
.scope-type__count {
  font-family: var(--insp-font-mono);
  font-size: 10px;
  opacity: 0.85;
}

.scope-actions {
  display: flex; gap: 4px;
  align-items: center;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.scope-actions__sep {
  width: 1px; height: 16px;
  background: var(--insp-border-default);
  margin: 0 4px;
}
.scope-actions__spacer { flex: 1; }
.scope-search {
  height: 24px;
  padding: 0 10px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  font-size: 11px;
  font-family: inherit;
  width: 180px;
  background: var(--insp-bg-surface);
}
.scope-search:focus {
  outline: none;
  border-color: var(--insp-accent);
}

.scope-tree-wrap {
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  background: var(--insp-bg-surface);
  max-height: 320px;
  overflow-y: auto;
  padding: 4px 0;
}
.scope-tree :deep(.el-tree-node__content) {
  height: 28px;
  font-size: 12px;
}
.scope-tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.scope-tree-name {
  font-size: 12px;
  color: var(--insp-ink-primary);
}
.scope-tree-type {
  font-family: var(--insp-font-mono);
  font-size: 10px;
  color: var(--insp-ink-tertiary);
  padding: 0 4px;
  background: var(--insp-bg-subtle);
  border-radius: 3px;
}

.scope-summary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 4px 10px;
  background: var(--insp-pass-pale);
  border: 1px solid var(--insp-pass-border);
  border-radius: var(--insp-radius-sm);
  font-size: 12px;
  color: var(--insp-pass);
}
.scope-summary strong {
  font-family: var(--insp-font-mono);
  font-weight: 700;
}

/* ─ Summary ─────── */
.wz-summary {
  margin: 0;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.wz-srow {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 14px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--insp-border-subtle);
}
.wz-srow:last-child { border-bottom: 0; }
.wz-srow dt {
  font-size: 11px;
  font-weight: 500;
  color: var(--insp-ink-tertiary);
  letter-spacing: 0.04em;
  text-transform: uppercase;
  margin: 0;
}
.wz-srow__value {
  font-size: 13px;
  color: var(--insp-ink-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.wz-srow__count {
  font-family: var(--insp-font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--insp-accent);
  padding: 1px 6px;
  background: var(--insp-accent-paler);
  border-radius: 3px;
}

.wz-tip {
  margin: 0 14px 14px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  background: var(--insp-info-pale);
  border: 1px solid var(--insp-info-border);
  border-radius: var(--insp-radius-sm);
  font-size: 12px;
  color: var(--insp-info);
  line-height: 1.5;
}
.wz-tip__icon {
  display: inline-flex;
  align-items: center; justify-content: center;
  width: 16px; height: 16px;
  background: var(--insp-info);
  color: white;
  border-radius: 50%;
  font-style: italic;
  font-weight: 700;
  font-size: 10px;
  flex-shrink: 0;
  margin-top: 1px;
}

/* ─ Footer ─────── */
.wz-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  position: sticky;
  bottom: 8px;
}
.wz-foot__spacer { flex: 1; }

/* ─ Mode switch (Phase 4: 模板新建 vs 克隆既有) ─────── */
/* IA 修正后的次级模式入口 — 不对等 segmented, 默认走模板新建主路径 */
.wz-mode-link {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}
.wz-mode-link-a {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--insp-ink-tertiary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all var(--insp-t-fast);
}
.wz-mode-link-a:hover {
  color: var(--insp-accent);
  background: var(--insp-bg-subtle);
}

@media (max-width: 720px) {
  .wz-rail__label { display: none; }
  .wz-row { grid-template-columns: 1fr; }
}
</style>
