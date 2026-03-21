<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { inspProjectApi, updateProject, createPlan } from '@/api/insp/project'
import { inspTemplateApi } from '@/api/insp/template'
import { getOrgUnitTree } from '@/api/organization'
import type { OrgUnitTreeNode } from '@/types'
import type { OrgUnit } from '@/types'
import type { TemplateSection } from '@/types/insp/template'
import { ScopeTypeConfig, type ScopeType } from '@/types/insp/enums'

const router = useRouter()

// ========== State ==========
const currentStep = ref(0)
const submitting = ref(false)
const loadingTemplates = ref(false)
const loadingOrg = ref(false)
const searchKeyword = ref('')

const rootSections = ref<TemplateSection[]>([])
const orgUnits = ref<OrgUnit[]>([])

const form = reactive({
  projectName: '',
  rootSectionId: undefined as number | undefined,
  scopeType: 'ORG' as ScopeType,
  scopeIds: [] as number[],
  startDate: '',
  endDate: '',
})

// ========== Computed ==========
const filteredSections = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return rootSections.value
  return rootSections.value.filter(s =>
    s.sectionName.toLowerCase().includes(kw) ||
    s.sectionCode.toLowerCase().includes(kw) ||
    (s.description || '').toLowerCase().includes(kw)
  )
})

const selectedSection = computed(() =>
  rootSections.value.find(s => Number(s.id) === Number(form.rootSectionId))
)

const selectedScopeLabel = computed(() => {
  const cfg = ScopeTypeConfig[form.scopeType]
  return cfg ? cfg.label : form.scopeType
})

const selectedOrgNames = computed(() => {
  if (form.scopeIds.length === 0) return '未指定'
  const names = form.scopeIds
    .map(id => orgUnits.value.find(u => Number(u.id) === Number(id))?.unitName)
    .filter(Boolean)
  if (names.length <= 2) return names.join(', ')
  return names.slice(0, 2).join(', ') + ` 等${names.length}个`
})

const dateRange = computed(() => {
  if (!form.startDate) return '未设置'
  if (!form.endDate) return form.startDate + ' 起'
  return form.startDate + ' ~ ' + form.endDate
})

// 每步的校验
const canProceedStep0 = computed(() => !!form.rootSectionId)
const canProceedStep1 = computed(() => !!form.projectName.trim() && !!form.startDate)

// 是否已发布
function isPublished(section: TemplateSection): boolean {
  return section.status === 'PUBLISHED'
}

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
  form.rootSectionId = section.id as number
  if (!form.projectName) {
    form.projectName = section.sectionName + ' 检查'
  }
}

// ========== Step 1: 多选 orgUnit toggle ==========
function toggleOrgUnit(id: number) {
  const idx = form.scopeIds.indexOf(id)
  if (idx >= 0) {
    form.scopeIds.splice(idx, 1)
  } else {
    form.scopeIds.push(id)
  }
}

// ========== Step 2: 创建 ==========
async function handleCreate() {
  submitting.value = true
  try {
    // 1. 创建项目（不传 rootSectionId，让模板通过计划关联）
    const project = await inspProjectApi.create({
      projectName: form.projectName,
      startDate: form.startDate,
      // rootSectionId 不传，改为通过第一个计划绑定
    })

    // 2. 创建第一个检查计划（绑定选中的模板）
    if (form.rootSectionId) {
      await createPlan({
        projectId: project.id,
        planName: form.projectName + ' 默认计划',
        rootSectionId: form.rootSectionId,
        sectionIds: '[]',
        scheduleMode: 'ON_DEMAND',
      })
    }

    // 3. 如果有额外配置（scopeType/scopeIds/endDate），更新项目
    const hasExtra = form.scopeIds.length > 0 || form.endDate || form.scopeType !== 'ORG'
    if (hasExtra) {
      const updateData: Record<string, any> = {
        scopeType: form.scopeType,
      }
      if (form.endDate) updateData.endDate = form.endDate
      if (form.scopeIds.length > 0) updateData.scopeConfig = JSON.stringify(form.scopeIds)
      await updateProject(project.id, updateData)
    }

    ElMessage.success('项目已创建')
    router.push(`/inspection/v7/projects/${project.id}`)
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
  } catch {
    // ignore
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
    orgUnits.value = flattenTree(tree) as any[]
  } catch {
    // ignore
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
  <div class="wz-page">
    <!-- Header -->
    <div class="wz-header">
      <button class="wz-back" @click="router.back()" title="返回">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <h1 class="wz-title">创建检查项目</h1>
    </div>

    <!-- Step indicator: minimal dots + lines -->
    <div class="wz-steps">
      <template v-for="idx in [0, 1, 2]" :key="idx">
        <div
          class="wz-step"
          :class="{
            'is-done': idx < currentStep,
            'is-active': idx === currentStep,
            'is-pending': idx > currentStep,
          }"
          @click="idx < currentStep ? (currentStep = idx) : undefined"
        >
          <div class="wz-dot">
            <svg v-if="idx < currentStep" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            <span v-else>{{ idx + 1 }}</span>
          </div>
        </div>
        <div v-if="idx < 2" class="wz-line" :class="idx < currentStep ? 'wz-line--done' : 'wz-line--pending'" />
      </template>
    </div>

    <!-- ==================== Step 0: 选择模板 ==================== -->
    <div v-show="currentStep === 0" class="wz-body">
      <!-- 搜索框 -->
      <div class="wz-search">
        <svg class="wz-search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input
          v-model="searchKeyword"
          type="text"
          class="wz-search-input"
          placeholder="搜索模板..."
        />
        <button v-if="searchKeyword" class="wz-search-clear" @click="searchKeyword = ''">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="loadingTemplates" class="wz-empty">加载模板中...</div>

      <!-- 空状态 -->
      <div v-else-if="rootSections.length === 0" class="wz-empty">
        暂无模板，请先在模板管理中创建模板
      </div>

      <div v-else-if="filteredSections.length === 0" class="wz-empty">
        未找到匹配的模板
      </div>

      <!-- 模板列表 -->
      <div v-else class="tpl-list">
        <div
          v-for="section in filteredSections"
          :key="section.id"
          class="tpl-row"
          :class="{
            'tpl-row--selected': Number(form.rootSectionId) === Number(section.id),
            'tpl-row--disabled': !isPublished(section),
          }"
          @click="selectSection(section)"
        >
          <div class="tpl-main">
            <div class="tpl-name-line">
              <span class="tpl-name">{{ section.sectionName }}</span>
              <span class="tpl-version">v{{ section.latestVersion }}</span>
              <span
                v-if="isPublished(section)"
                class="tpl-status tpl-status--published"
              >已发布</span>
              <span v-else class="tpl-status tpl-status--draft">未发布</span>
            </div>
            <div v-if="section.description" class="tpl-desc">{{ section.description }}</div>
            <div v-if="!isPublished(section)" class="tpl-hint">需要先发布才能选择</div>
          </div>
          <div class="tpl-radio">
            <div
              v-if="isPublished(section)"
              class="tpl-radio-dot"
              :class="{ 'tpl-radio-dot--on': Number(form.rootSectionId) === Number(section.id) }"
            />
            <span v-else class="tpl-radio-disabled">--</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Step 1: 配置范围 ==================== -->
    <div v-show="currentStep === 1" class="wz-body">
      <div class="wz-form">
        <!-- 项目名称 -->
        <div class="wz-field">
          <label class="wz-label">项目名称 <span class="wz-req">*</span></label>
          <input
            v-model="form.projectName"
            type="text"
            class="wz-input"
            placeholder="输入项目名称"
            maxlength="100"
          />
        </div>

        <!-- 检查范围 -->
        <div class="wz-field">
          <label class="wz-label">检查范围</label>
          <div v-if="loadingOrg" class="wz-org-list">加载中...</div>
          <div v-else-if="orgUnits.length === 0" class="wz-org-list">暂无组织单元</div>
          <div v-else class="wz-org-list">
            <div
              v-for="unit in orgUnits"
              :key="unit.id"
              class="wz-org-row"
              :class="{ 'wz-org-row--on': form.scopeIds.includes(Number(unit.id)) }"
              :style="{ paddingLeft: `${8 + (unit as any).depth * 16}px` }"
              @click="toggleOrgUnit(Number(unit.id))"
            >
              <span v-if="(unit as any).depth > 0" class="wz-org-indent" />
              <span class="wz-org-name">{{ unit.unitName }}</span>
              <span v-if="form.scopeIds.includes(Number(unit.id))" class="wz-org-check">✓</span>
            </div>
          </div>
        </div>

        <!-- 起止日期 -->
        <div class="wz-row2">
          <div class="wz-field">
            <label class="wz-label">开始日期 <span class="wz-req">*</span></label>
            <input v-model="form.startDate" type="date" class="wz-input" />
          </div>
          <div class="wz-field">
            <label class="wz-label">结束日期</label>
            <input v-model="form.endDate" type="date" class="wz-input" />
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Step 2: 确认创建 ==================== -->
    <div v-show="currentStep === 2" class="wz-body">
      <table class="wz-summary">
        <tbody>
          <tr>
            <td class="wz-sk">项目名称</td>
            <td class="wz-sv">{{ form.projectName || '--' }}</td>
          </tr>
          <tr>
            <td class="wz-sk">模板</td>
            <td class="wz-sv">
              {{ selectedSection?.sectionName || '--' }}
              <span v-if="selectedSection" class="wz-sv-tag">v{{ selectedSection.latestVersion }}</span>
            </td>
          </tr>
          <tr>
            <td class="wz-sk">检查范围</td>
            <td class="wz-sv">{{ selectedOrgNames }}</td>
          </tr>
          <tr>
            <td class="wz-sk">日期</td>
            <td class="wz-sv">{{ dateRange }}</td>
          </tr>
        </tbody>
      </table>

      <div class="wz-tip">
        创建后可在详情页继续配置检查计划、评级维度和检查员，发布后将自动生成检查任务。
      </div>
    </div>

    <!-- ==================== 底部操作栏 ==================== -->
    <div class="wz-footer">
      <button v-if="currentStep > 0" class="wz-btn wz-btn--outline" @click="prevStep">
        上一步
      </button>
      <span v-else />

      <button
        v-if="currentStep < 2"
        class="wz-btn wz-btn--primary"
        :disabled="currentStep === 0 ? !canProceedStep0 : !canProceedStep1"
        @click="nextStep"
      >
        下一步
      </button>
      <button
        v-if="currentStep === 2"
        class="wz-btn wz-btn--create"
        :disabled="submitting"
        @click="handleCreate"
      >
        {{ submitting ? '创建中...' : '创建项目' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
/* ===== Variables ===== */
:root {
  --wz-primary: #1a6dff;
  --wz-primary-light: #e8f0ff;
  --wz-success: #10b981;
  --wz-success-light: #ecfdf5;
  --wz-bg: #f7f8fa;
  --wz-card: #fff;
  --wz-border: #e5e7eb;
  --wz-text: #1d2129;
  --wz-text2: #4b5563;
  --wz-text3: #9ca3af;
}

/* ===== Page ===== */
.wz-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 20px 32px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #1d2129;
}

/* ===== Header ===== */
.wz-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
}

.wz-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  color: #6b7280;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
}

.wz-back:hover {
  background: #f0f2f5;
  color: #1a6dff;
}

.wz-title {
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  margin: 0;
}

/* ===== Step Indicator ===== */
.wz-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
  padding: 0 60px;
}

.wz-step {
  flex-shrink: 0;
}

.wz-step.is-done {
  cursor: pointer;
}

.wz-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s;
}

.is-done .wz-dot {
  background: #1a6dff;
  color: #fff;
}

.is-active .wz-dot {
  background: #1a6dff;
  color: #fff;
  box-shadow: 0 0 0 4px rgba(26, 109, 255, 0.12);
}

.is-pending .wz-dot {
  background: #e5e7eb;
  color: #9ca3af;
}

.wz-line {
  width: 80px;
  height: 2px;
  margin: 0 6px;
  border-radius: 1px;
}

.wz-line--done {
  background: #1a6dff;
}

.wz-line--pending {
  background: #e5e7eb;
}

/* ===== Body (each step) ===== */
.wz-body {
  min-height: 300px;
  margin-bottom: 16px;
}

/* ===== Search ===== */
.wz-search {
  position: relative;
  margin-bottom: 16px;
}

.wz-search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}

.wz-search-input {
  width: 100%;
  height: 38px;
  padding: 0 36px 0 38px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #1d2129;
  background: #fff;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.wz-search-input::placeholder {
  color: #b0b8c4;
}

.wz-search-input:focus {
  border-color: #1a6dff;
}

.wz-search-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  background: none;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 4px;
}

.wz-search-clear:hover {
  color: #4b5563;
}

/* ===== Empty / Loading ===== */
.wz-empty {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 56px 0;
  border: 1px dashed #e5e7eb;
  border-radius: 8px;
  background: #fafbfc;
}

/* ===== Template List ===== */
.tpl-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #e5e7eb;
}

.tpl-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  cursor: pointer;
  transition: background 0.12s;
  gap: 16px;
}

.tpl-row:hover:not(.tpl-row--disabled) {
  background: #f7f9fc;
}

.tpl-row--selected {
  background: #eff5ff !important;
}

.tpl-row--disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.tpl-main {
  flex: 1;
  min-width: 0;
}

.tpl-name-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tpl-name {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tpl-version {
  font-size: 11px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 1px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}

.tpl-status {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}

.tpl-status--published {
  color: #10b981;
  background: #ecfdf5;
}

.tpl-status--draft {
  color: #9ca3af;
  background: #f3f4f6;
}

.tpl-desc {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tpl-hint {
  font-size: 11px;
  color: #b0b8c4;
  margin-top: 3px;
}

.tpl-radio {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
}

.tpl-radio-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid #d1d5db;
  transition: all 0.15s;
  position: relative;
}

.tpl-radio-dot--on {
  border-color: #1a6dff;
}

.tpl-radio-dot--on::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #1a6dff;
}

.tpl-radio-disabled {
  font-size: 11px;
  color: #d1d5db;
}

/* ===== Form (Step 1) ===== */
.wz-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.wz-row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.wz-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.wz-label {
  font-size: 12px;
  font-weight: 600;
  color: #4b5563;
}

.wz-req {
  color: #ef4444;
}

.wz-opt {
  font-weight: 400;
  color: #9ca3af;
  font-size: 11px;
  margin-left: 4px;
}

.wz-input,
.wz-select {
  height: 36px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  color: #1d2129;
  background: #fff;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.wz-input:focus,
.wz-select:focus {
  border-color: #1a6dff;
}

.wz-input::placeholder {
  color: #b0b8c4;
}

.wz-select {
  appearance: auto;
  cursor: pointer;
}

/* ===== Chip area (org unit selector) ===== */
/* Org tree list */
.wz-org-list {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fafbfc;
  max-height: 240px;
  overflow-y: auto;
  font-size: 12px;
  color: #9ca3af;
}
.wz-org-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  cursor: pointer;
  transition: background 0.1s;
  border-bottom: 1px solid #f0f1f3;
}
.wz-org-row:last-child { border-bottom: none; }
.wz-org-row:hover { background: #f0f4ff; }
.wz-org-row--on { background: #eff6ff; }
.wz-org-row--on:hover { background: #e0edff; }
.wz-org-indent { width: 6px; height: 6px; border-left: 1px solid #d1d5db; border-bottom: 1px solid #d1d5db; flex-shrink: 0; }
.wz-org-name { font-size: 12px; color: #1e2a3a; flex: 1; }
.wz-org-row--on .wz-org-name { color: #1a6dff; font-weight: 500; }
.wz-org-check { color: #1a6dff; font-size: 12px; font-weight: 600; flex-shrink: 0;
}

/* ===== Summary (Step 2) ===== */
.wz-summary {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}

.wz-summary tr {
  border-bottom: 1px solid #f3f4f6;
}

.wz-summary tr:last-child {
  border-bottom: none;
}

.wz-sk {
  width: 90px;
  padding: 11px 16px;
  font-size: 12px;
  color: #6b7280;
  background: #f9fafb;
  text-align: left;
  vertical-align: top;
  font-weight: 500;
}

.wz-sv {
  padding: 11px 16px;
  font-size: 13px;
  color: #1d2129;
  font-weight: 500;
}

.wz-sv-tag {
  display: inline-block;
  font-size: 11px;
  color: #1a6dff;
  background: #eff6ff;
  padding: 1px 6px;
  border-radius: 4px;
  margin-left: 6px;
  font-weight: 500;
}

.wz-tip {
  font-size: 12px;
  color: #4b5563;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  padding: 10px 14px;
  line-height: 1.6;
}

/* ===== Footer ===== */
.wz-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
  margin-top: 8px;
}

.wz-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  outline: none;
  transition: all 0.15s;
}

.wz-btn--outline {
  background: #fff;
  color: #4b5563;
  border: 1px solid #d1d5db;
}

.wz-btn--outline:hover {
  background: #f5f7fa;
  border-color: #b0b8c4;
}

.wz-btn--primary {
  background: #1a6dff;
  color: #fff;
}

.wz-btn--primary:hover:not(:disabled) {
  background: #1558d8;
}

.wz-btn--primary:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.wz-btn--create {
  background: #10b981;
  color: #fff;
  padding: 0 24px;
}

.wz-btn--create:hover:not(:disabled) {
  background: #059669;
}

.wz-btn--create:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ===== Responsive ===== */
@media (max-width: 600px) {
  .wz-row2 {
    grid-template-columns: 1fr;
  }

  .wz-steps {
    padding: 0 20px;
  }

  .wz-line {
    width: 48px;
  }
}
</style>
