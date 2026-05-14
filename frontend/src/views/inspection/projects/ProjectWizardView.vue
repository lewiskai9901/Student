<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { inspProjectApi, updateProject, createPlan } from '@/api/inspection/project'
import { inspTemplateApi } from '@/api/inspection/template'
import { getOrgUnitTree } from '@/api/organization'
import type { OrgUnitTreeNode } from '@/types'
import type { OrgUnit } from '@/types'
import type { TemplateSection } from '@/types/insp/template'
import { ScopeTypeConfig, TargetTypeConfig, type ScopeType, type TargetType } from '@/types/insp/enums'
import { ArrowLeft, Check } from 'lucide-vue-next'

const router = useRouter()

// ========== State ==========
const currentStep = ref(0)
const submitting = ref(false)
const loadingTemplates = ref(false)
const loadingOrg = ref(false)
const searchKeyword = ref('')
const scopeSearchKeyword = ref('')

const rootSections = ref<TemplateSection[]>([])
const orgTree = ref<OrgUnitTreeNode[]>([])
const flatOrgUnits = ref<(OrgUnit & { depth: number })[]>([])

const form = reactive({
  projectName: '',
  rootSectionId: undefined as LongId | undefined,
  scopeType: 'ORG' as ScopeType,
  scopeIds: [] as string[],
  startDate: '',
  endDate: '',
})

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
const canProceedStep1 = computed(() => !!form.projectName.trim() && !!form.startDate)

function isPublished(section: TemplateSection): boolean {
  return section.status === 'PUBLISHED'
}

// ========== Scope Tree ==========

// Collect unique unitType values for quick-filter buttons
const availableUnitTypes = computed(() => {
  const typeMap = new Map<string, { code: string; name: string; count: number }>()
  for (const unit of flatOrgUnits.value) {
    const code = unit.unitType || ''
    if (!code) continue
    if (typeMap.has(code)) {
      typeMap.get(code)!.count++
    } else {
      typeMap.set(code, { code, name: unit.typeName || code, count: 1 })
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

// el-tree data format
const treeData = computed(() => {
  function mapNode(node: OrgUnitTreeNode): any {
    return {
      id: String(node.id),
      label: node.unitName,
      typeName: node.typeName || node.unitType || '',
      unitType: node.unitType || '',
      children: (node.children || []).map(mapNode),
    }
  }
  return filteredOrgTree.value.map(mapNode)
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
  // Get all leaf + branch node keys from current filtered tree
  const allKeys = flatOrgUnits.value
    .filter(u => !activeTypeFilter.value || u.unitType === activeTypeFilter.value)
    .map(u => String(u.id))
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
  const keys = flatOrgUnits.value
    .filter(u => u.unitType === typeCode)
    .map(u => String(u.id))
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
    ElMessage.warning('请填写项目名称和开始日期')
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
  try {
    // Bug 2: create 时传 rootSectionId, 否则项目和模板脱钩
    const project = await inspProjectApi.create({
      projectName: form.projectName,
      rootSectionId: form.rootSectionId,
      startDate: form.startDate,
    })

    if (form.rootSectionId) {
      await createPlan({
        projectId: project.id,
        planName: form.projectName + ' 默认计划',
        rootSectionId: form.rootSectionId,
        sectionIds: '[]',
        scheduleMode: 'ON_DEMAND',
      })
    }

    // Bug 2: scope+date 总是 update, 不依赖 hasExtra 兜底, 否则向导 step2 选的范围会丢
    const updateData: Record<string, any> = {
      scopeType: form.scopeType,
    }
    if (form.endDate) updateData.endDate = form.endDate
    if (form.scopeIds.length > 0) {
      updateData.scopeConfig = JSON.stringify(form.scopeIds.map(String))
    }
    await updateProject(project.id, updateData)

    ElMessage.success('项目已创建')
    router.push(`/inspection/projects/${project.id}`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

// ========== Init ==========
async function loadTemplates() {
  loadingTemplates.value = true
  try {
    const result = await inspTemplateApi.getList({ page: 1, size: 200 })
    rootSections.value = result.records
  } catch (e) {
    console.warn('Failed to load templates', e)
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
  try {
    const tree = await getOrgUnitTree()
    orgTree.value = tree
    flatOrgUnits.value = flattenTree(tree) as any[]
  } catch (e) {
    console.warn('Failed to load org units', e)
  } finally {
    loadingOrg.value = false
  }
}

onMounted(() => {
  loadTemplates()
  loadOrgUnits()
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
        共 3 步 · 当前 <strong class="insp-num">{{ currentStep + 1 }}</strong> / 3
      </div>
    </header>

    <!-- Step indicator -->
    <nav class="wz-rail">
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

    <!-- ==================== Step 0: 选择模板 ==================== -->
    <section v-show="currentStep === 0" class="wz-card">
      <header class="wz-card__head">
        <span class="wz-card__title">选择检查模板</span>
        <div class="wz-card__search">
          <input v-model="searchKeyword" type="text" placeholder="搜索模板名称..." />
          <button v-if="searchKeyword" class="wz-card__clear" @click="searchKeyword = ''" title="清除">×</button>
        </div>
      </header>

      <div v-if="loadingTemplates" class="wz-state">加载模板中…</div>
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

    <!-- ==================== Step 1: 配置范围 ==================== -->
    <section v-show="currentStep === 1" class="wz-card">
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
            <input v-model="scopeSearchKeyword" class="scope-search" placeholder="搜索组织..." />
          </div>

          <!-- Tree -->
          <div class="scope-tree-wrap">
            <div v-if="loadingOrg" class="wz-state wz-state--small">加载中...</div>
            <div v-else-if="treeData.length === 0" class="wz-state wz-state--small">暂无组织单元</div>
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
    <section v-show="currentStep === 2" class="wz-card">
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

    <!-- Footer -->
    <footer class="wz-foot">
      <button v-if="currentStep > 0" class="insp-btn" @click="prevStep">
        < 上一步
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

@media (max-width: 720px) {
  .wz-rail__label { display: none; }
  .wz-row { grid-template-columns: 1fr; }
}
</style>
