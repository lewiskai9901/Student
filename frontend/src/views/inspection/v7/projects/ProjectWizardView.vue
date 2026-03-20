<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { inspProjectApi, updateProject } from '@/api/insp/project'
import { inspTemplateApi } from '@/api/insp/template'
import { getOrgUnits } from '@/api/organization'
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
  rootSections.value.find(s => s.id === form.rootSectionId)
)

const selectedScopeLabel = computed(() => {
  const cfg = ScopeTypeConfig[form.scopeType]
  return cfg ? cfg.label : form.scopeType
})

const selectedOrgNames = computed(() => {
  if (form.scopeIds.length === 0) return '未指定'
  const names = form.scopeIds
    .map(id => orgUnits.value.find(u => u.id === id)?.unitName)
    .filter(Boolean)
  if (names.length <= 2) return names.join('、')
  return names.slice(0, 2).join('、') + ` 等${names.length}个`
})

const dateRange = computed(() => {
  if (!form.startDate) return '未设置'
  if (!form.endDate) return form.startDate + ' 起'
  return form.startDate + ' ~ ' + form.endDate
})

// 每步的校验
const canProceedStep0 = computed(() => !!form.rootSectionId)
const canProceedStep1 = computed(() => !!form.projectName.trim() && !!form.startDate)

// ========== Navigation ==========
function nextStep() {
  if (currentStep.value === 0 && !canProceedStep0.value) {
    ElMessage.warning('请先选择一个模板')
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
  form.rootSectionId = section.id as number
  if (!form.projectName) {
    form.projectName = section.sectionName + ' 检查'
  }
}

// ========== Step 2: 创建 ==========
async function handleCreate() {
  submitting.value = true
  try {
    // 1. 创建项目（只传 projectName, rootSectionId, startDate）
    const project = await inspProjectApi.create({
      projectName: form.projectName,
      rootSectionId: form.rootSectionId!,
      startDate: form.startDate,
    })

    // 2. 如果有额外配置（scopeType/scopeIds/endDate），更新项目
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
    const result = await inspTemplateApi.getList({ page: 1, size: 100, status: 'PUBLISHED' })
    rootSections.value = result.records
  } catch {
    // ignore
  } finally {
    loadingTemplates.value = false
  }
}

async function loadOrgUnits() {
  loadingOrg.value = true
  try {
    orgUnits.value = await getOrgUnits()
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
  <div class="wizard-page">
    <!-- Header -->
    <div class="wizard-header">
      <button class="back-btn" @click="router.back()">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <span class="wizard-title">创建检查项目</span>
    </div>

    <!-- Step indicator -->
    <div class="step-bar">
      <template v-for="(label, idx) in ['选择模板', '配置范围', '确认创建']" :key="idx">
        <!-- Step node -->
        <div
          class="step-node"
          :class="{
            'step-done': idx < currentStep,
            'step-active': idx === currentStep,
            'step-pending': idx > currentStep,
          }"
          @click="idx < currentStep ? (currentStep = idx) : undefined"
          :style="idx < currentStep ? 'cursor:pointer' : ''"
        >
          <div class="step-dot">
            <svg v-if="idx < currentStep" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            <span v-else>{{ idx + 1 }}</span>
          </div>
          <span class="step-label">{{ label }}</span>
        </div>
        <!-- Connector line -->
        <div v-if="idx < 2" class="step-line" :class="idx < currentStep ? 'step-line-done' : 'step-line-pending'" />
      </template>
    </div>

    <!-- ==================== Step 0: 选择模板 ==================== -->
    <div v-show="currentStep === 0" class="step-content">
      <div class="section-title">第一步：选择模板</div>

      <!-- 搜索框 -->
      <div class="search-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索模板名称或编码..."
          clearable
          class="search-input"
        >
          <template #prefix>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </template>
        </el-input>
      </div>

      <!-- 加载中 -->
      <div v-if="loadingTemplates" class="loading-hint">加载模板中...</div>

      <!-- 空状态 -->
      <div v-else-if="rootSections.length === 0" class="empty-hint">
        暂无已发布的模板，请先在「模板管理」中创建并发布模板
      </div>

      <!-- 模板卡片网格 -->
      <div v-else-if="filteredSections.length === 0" class="empty-hint">
        未找到匹配的模板
      </div>

      <div v-else class="template-grid">
        <div
          v-for="section in filteredSections"
          :key="section.id"
          class="template-card"
          :class="{ 'template-card-selected': form.rootSectionId === section.id }"
          @click="selectSection(section)"
        >
          <!-- 选中角标 -->
          <div v-if="form.rootSectionId === section.id" class="selected-badge">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>

          <!-- 模板图标 -->
          <div class="card-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
          </div>

          <div class="card-name">{{ section.sectionName }}</div>
          <div class="card-code">{{ section.sectionCode }}</div>

          <div v-if="section.description" class="card-desc">{{ section.description }}</div>

          <div class="card-meta">
            <span class="meta-version">v{{ section.latestVersion }}</span>
            <span class="meta-status">已发布</span>
          </div>

          <div class="card-select-btn" :class="{ 'card-select-btn-active': form.rootSectionId === section.id }">
            {{ form.rootSectionId === section.id ? '已选择 ✓' : '选择' }}
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Step 1: 配置范围 ==================== -->
    <div v-show="currentStep === 1" class="step-content">
      <div class="section-title">第二步：配置范围</div>

      <!-- 项目名称 -->
      <div class="field-group">
        <label class="field-label">项目名称 <span class="required">*</span></label>
        <el-input
          v-model="form.projectName"
          placeholder="输入项目名称"
          maxlength="100"
          show-word-limit
        />
      </div>

      <!-- 检查范围 -->
      <div class="card-block">
        <div class="block-title">检查范围</div>
        <div class="two-col">
          <div class="field-group">
            <label class="field-label">范围类型</label>
            <el-select v-model="form.scopeType" class="full-width">
              <el-option
                v-for="(cfg, key) in ScopeTypeConfig"
                :key="key"
                :label="cfg.label"
                :value="key"
              />
            </el-select>
          </div>
          <div class="field-group">
            <label class="field-label">范围配置</label>
            <el-select
              v-model="form.scopeIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              filterable
              clearable
              placeholder="选择组织单元（可选）"
              class="full-width"
              :loading="loadingOrg"
            >
              <el-option
                v-for="unit in orgUnits"
                :key="unit.id"
                :label="unit.unitName"
                :value="unit.id"
              />
            </el-select>
          </div>
        </div>
      </div>

      <!-- 起止日期 -->
      <div class="card-block">
        <div class="block-title">起止日期</div>
        <div class="two-col">
          <div class="field-group">
            <label class="field-label">开始日期 <span class="required">*</span></label>
            <el-date-picker
              v-model="form.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择开始日期"
              class="full-width"
            />
          </div>
          <div class="field-group">
            <label class="field-label">结束日期</label>
            <el-date-picker
              v-model="form.endDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择结束日期（可选）"
              class="full-width"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Step 2: 确认创建 ==================== -->
    <div v-show="currentStep === 2" class="step-content">
      <div class="section-title">第三步：确认创建</div>

      <div class="summary-card">
        <div class="summary-header">项目摘要</div>
        <div class="summary-body">
          <div class="summary-row">
            <span class="summary-key">项目名称</span>
            <span class="summary-val">{{ form.projectName || '—' }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-key">模板</span>
            <span class="summary-val">
              {{ selectedSection?.sectionName || '—' }}
              <span v-if="selectedSection" class="summary-tag">v{{ selectedSection.latestVersion }}</span>
            </span>
          </div>
          <div class="summary-row">
            <span class="summary-key">检查范围</span>
            <span class="summary-val">
              {{ selectedScopeLabel }}
              <span class="summary-dot">·</span>
              {{ selectedOrgNames }}
            </span>
          </div>
          <div class="summary-row">
            <span class="summary-key">日期</span>
            <span class="summary-val">{{ dateRange }}</span>
          </div>
        </div>
      </div>

      <div class="tip-block">
        创建后可在详情页继续配置检查计划、评级维度和检查员，发布后将自动生成检查任务。
      </div>
    </div>

    <!-- ==================== 底部操作栏 ==================== -->
    <div class="bottom-bar">
      <button v-if="currentStep > 0" class="btn-secondary" @click="prevStep">
        ← 上一步
      </button>
      <span v-else />

      <div class="btn-group-right">
        <button
          v-if="currentStep < 2"
          class="btn-primary"
          :disabled="currentStep === 0 ? !canProceedStep0 : !canProceedStep1"
          @click="nextStep"
        >
          下一步 →
        </button>
        <button
          v-if="currentStep === 2"
          class="btn-primary btn-create"
          :disabled="submitting"
          @click="handleCreate"
        >
          <span v-if="submitting">创建中...</span>
          <span v-else>创建项目</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 页面容器 ===== */
.wizard-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px;
  font-family: inherit;
}

/* ===== 顶部标题 ===== */
.wizard-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  color: #606266;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s;
}

.back-btn:hover {
  background: #f5f7fa;
  color: #1a6dff;
}

.wizard-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

/* ===== 步骤条 ===== */
.step-bar {
  display: flex;
  align-items: center;
  margin-bottom: 28px;
}

.step-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.step-dot {
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

.step-done .step-dot {
  background: #1a6dff;
  color: #fff;
}

.step-active .step-dot {
  background: #1a6dff;
  color: #fff;
  width: 32px;
  height: 32px;
  box-shadow: 0 0 0 4px rgba(26, 109, 255, 0.15);
}

.step-pending .step-dot {
  background: #f0f2f5;
  color: #9ca3af;
}

.step-label {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.step-done .step-label,
.step-active .step-label {
  color: #1a6dff;
}

.step-pending .step-label {
  color: #9ca3af;
}

.step-line {
  flex: 1;
  height: 2px;
  margin: 0 8px;
  border-radius: 1px;
  transition: background 0.2s;
}

.step-line-done {
  background: #1a6dff;
}

.step-line-pending {
  background: #e5e7eb;
}

/* ===== 步骤内容区 ===== */
.step-content {
  min-height: 320px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  padding-bottom: 12px;
  border-bottom: 2px solid #1a6dff;
  margin-bottom: 20px;
  display: inline-block;
}

/* ===== 搜索行 ===== */
.search-row {
  margin-bottom: 16px;
}

.search-input {
  width: 280px;
}

/* ===== 加载/空状态 ===== */
.loading-hint,
.empty-hint {
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
  padding: 48px 0;
  border: 1px dashed #e5e7eb;
  border-radius: 10px;
}

/* ===== 模板网格 ===== */
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.template-card {
  position: relative;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px 14px 12px;
  cursor: pointer;
  transition: all 0.18s;
  background: #fff;
  overflow: hidden;
}

.template-card:hover {
  border-color: #93b8ff;
  box-shadow: 0 2px 8px rgba(26, 109, 255, 0.08);
}

.template-card-selected {
  border-color: #1a6dff;
  background: #f0f5ff;
  box-shadow: 0 2px 10px rgba(26, 109, 255, 0.12);
}

/* 选中角标 */
.selected-badge {
  position: absolute;
  top: 0;
  right: 0;
  width: 24px;
  height: 24px;
  background: #1a6dff;
  border-radius: 0 10px 0 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

/* 卡片图标 */
.card-icon {
  color: #1a6dff;
  margin-bottom: 8px;
  opacity: 0.8;
}

.template-card-selected .card-icon {
  opacity: 1;
}

.card-name {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-code {
  font-size: 11px;
  color: #9ca3af;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 11px;
  color: #6b7280;
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.meta-version {
  font-size: 11px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 1px 6px;
  border-radius: 4px;
}

.meta-status {
  font-size: 11px;
  color: #10b981;
  background: #ecfdf5;
  padding: 1px 6px;
  border-radius: 4px;
}

.card-select-btn {
  font-size: 12px;
  text-align: center;
  padding: 5px 0;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  color: #6b7280;
  background: #f9fafb;
  transition: all 0.15s;
}

.template-card:hover .card-select-btn {
  border-color: #1a6dff;
  color: #1a6dff;
  background: #f0f5ff;
}

.card-select-btn-active {
  border-color: #1a6dff !important;
  color: #1a6dff !important;
  background: #e8f0ff !important;
  font-weight: 600;
}

/* ===== 表单字段 ===== */
.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #4b5563;
}

.required {
  color: #ef4444;
}

.full-width {
  width: 100%;
}

/* ===== 卡片块 ===== */
.card-block {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
  background: #fff;
}

.block-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 14px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

/* ===== 摘要卡片 ===== */
.summary-card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 16px;
}

.summary-header {
  background: #f9fafb;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
  border-bottom: 1px solid #e5e7eb;
}

.summary-body {
  background: #fff;
}

.summary-row {
  display: flex;
  align-items: baseline;
  padding: 10px 16px;
  border-bottom: 1px solid #f3f4f6;
  gap: 12px;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-key {
  font-size: 12px;
  color: #6b7280;
  width: 72px;
  flex-shrink: 0;
}

.summary-val {
  font-size: 12px;
  color: #1d2129;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.summary-tag {
  font-size: 11px;
  color: #1a6dff;
  background: #eff6ff;
  padding: 1px 6px;
  border-radius: 4px;
}

.summary-dot {
  color: #9ca3af;
}

/* ===== 提示块 ===== */
.tip-block {
  font-size: 12px;
  color: #4b5563;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 10px 14px;
  line-height: 1.6;
}

/* ===== 底部操作栏 ===== */
.bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
  margin-top: 4px;
}

.btn-group-right {
  display: flex;
  gap: 8px;
}

.btn-secondary,
.btn-primary {
  height: 34px;
  padding: 0 18px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  outline: none;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-secondary {
  background: #f5f7fa;
  color: #374151;
  border: 1px solid #e5e7eb;
}

.btn-secondary:hover {
  background: #e9ecf2;
}

.btn-primary {
  background: #1a6dff;
  color: #fff;
  border: 1px solid #1a6dff;
}

.btn-primary:hover:not(:disabled) {
  background: #1558d8;
}

.btn-primary:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.btn-create {
  background: #10b981;
  border-color: #10b981;
  padding: 0 22px;
}

.btn-create:hover:not(:disabled) {
  background: #059669;
  border-color: #059669;
}

/* ===== 响应式 ===== */
@media (max-width: 600px) {
  .template-grid {
    grid-template-columns: 1fr 1fr;
  }

  .two-col {
    grid-template-columns: 1fr;
  }

  .step-label {
    display: none;
  }

  .search-input {
    width: 100%;
  }
}
</style>
