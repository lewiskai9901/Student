<template>
  <div class="etm-page">
    <!-- Header -->
    <header class="etm-header">
      <div class="header-left">
        <h1 class="page-title">事件类型管理</h1>
        <div class="stats-row">
          <span class="stat">分类 <b>{{ categoryCount }}</b></span>
          <i class="stat-sep" />
          <span class="stat">类型 <b>{{ totalCount }}</b></span>
          <i class="stat-sep" />
          <span class="stat"><em class="dot dot-positive" /> 正向 <b class="c-positive">{{ positiveCount }}</b></span>
          <i class="stat-sep" />
          <span class="stat"><em class="dot dot-negative" /> 负向 <b class="c-negative">{{ negativeCount }}</b></span>
          <i class="stat-sep" />
          <span class="stat"><em class="dot dot-neutral" /> 中性 <b class="c-neutral">{{ neutralCount }}</b></span>
        </div>
      </div>
      <button class="btn-create" @click="openCategoryDialog()">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M7 1v12M1 7h12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
        新增分类
      </button>
    </header>

    <!-- Category Cards -->
    <div class="etm-body" v-loading="loading">
      <div v-if="!loading && groupedTypes.length === 0" class="empty-state">
        <p class="empty-title">暂无事件类型</p>
        <p class="empty-sub">点击「新增分类」创建事件分类，然后添加具体类型</p>
      </div>

      <div v-for="group in groupedTypes" :key="group.categoryCode" class="category-card">
        <!-- Card Header -->
        <div class="card-header">
          <div class="card-header-left">
            <span class="category-name">{{ group.categoryName }}</span>
            <span class="polarity-badge" :class="'pol-' + (group.polarity || 'NEUTRAL').toLowerCase()">
              {{ getPolarityLabel(group.polarity) }}
            </span>
            <span class="type-count">{{ group.items.length }} 个类型</span>
          </div>
          <div class="card-header-right">
            <button class="action-btn" @click="openCategoryDialog(group)">编辑</button>
            <button class="action-btn action-primary" @click="openTypeDialog(group.categoryCode, group.categoryName)">添加子类型</button>
          </div>
        </div>

        <!-- Type List -->
        <div class="type-list">
          <div v-if="group.items.length === 0" class="type-empty">暂无类型，点击「添加子类型」</div>
          <div
            v-for="t in group.items" :key="t.id"
            :class="['type-row', isTypePluginDisabled(t) ? 'row-disabled-by-plugin' : '']"
            :title="isTypePluginDisabled(t) ? '所属插件已禁用 — 此事件类型级联软失效 (fire 不触发)' : undefined"
          >
            <em class="dot" :style="{ background: t.color || getPolarityColor(group.polarity) }" />
            <span class="type-name">{{ t.typeName }}</span>
            <span
              v-if="isTypePluginDisabled(t)"
              class="disabled-by-plugin-badge"
              title="所属插件已禁用"
            >插件禁用</span>
            <!-- 来源标签: 行业/插件 chip 替代以前笼统的"预置", 展示真实贡献方 -->
            <span
              v-if="resolveSource(t).code"
              class="source-chip"
              :style="sourceChipStyle(resolveSource(t).code)"
              :title="resolveSource(t).tooltip"
            >{{ resolveSource(t).label }}</span>
            <span v-else class="source-chip source-custom" title="管理员本地创建">自定义</span>
            <code class="type-code">{{ t.typeCode }}</code>
            <div class="type-subjects">
              <span v-for="s in parseSubjects(t.applicableSubjects)" :key="s" class="subject-tag" :class="'subj-' + s.toLowerCase()">{{ s }}</span>
              <span v-if="!parseSubjects(t.applicableSubjects).length" class="subject-none">--</span>
            </div>
            <div class="type-row-actions">
              <template v-if="!t.isSystem">
                <button class="action-btn" @click="openTypeDialog(group.categoryCode, group.categoryName, t)">编辑</button>
                <button class="action-btn action-danger" @click="handleDeleteType(t)">删除</button>
              </template>
              <span v-else class="system-hint">不可修改</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Category Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="catDialogVisible" class="modal-mask" @mousedown="catMaskDown" @click="catMaskClick">
          <div class="modal-box">
            <div class="modal-head">
              <h3>{{ catForm.isEdit ? '编辑分类' : '新建分类' }}</h3>
              <button class="modal-close" @click="catDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 16 16"><path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="fld">
                <label>分类编码 <span class="req">*</span></label>
                <input v-model="catForm.categoryCode" class="fld-input" placeholder="如 REWARD" :disabled="catForm.isEdit" />
              </div>
              <div class="fld">
                <label>分类名称 <span class="req">*</span></label>
                <input v-model="catForm.categoryName" class="fld-input" placeholder="如 奖励" />
              </div>
              <div class="fld">
                <label>极性</label>
                <div class="radio-group">
                  <label class="radio-item" :class="{ active: catForm.polarity === 'POSITIVE' }">
                    <input type="radio" value="POSITIVE" v-model="catForm.polarity" /> 正向
                  </label>
                  <label class="radio-item" :class="{ active: catForm.polarity === 'NEGATIVE' }">
                    <input type="radio" value="NEGATIVE" v-model="catForm.polarity" /> 负向
                  </label>
                  <label class="radio-item" :class="{ active: catForm.polarity === 'NEUTRAL' }">
                    <input type="radio" value="NEUTRAL" v-model="catForm.polarity" /> 中性
                  </label>
                </div>
              </div>
            </div>
            <div class="modal-foot">
              <button class="btn-cancel" @click="catDialogVisible = false">取消</button>
              <button class="btn-save" @click="handleSaveCategory">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Type Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="typeDialogVisible" class="modal-mask" @mousedown="typeMaskDown" @click="typeMaskClick">
          <div class="modal-box">
            <div class="modal-head">
              <h3>{{ typeForm.id ? '编辑事件类型' : '新建事件类型' }}</h3>
              <button class="modal-close" @click="typeDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 16 16"><path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="fld-row">
                <div class="fld">
                  <label>类型名称 <span class="req">*</span></label>
                  <input v-model="typeForm.typeName" class="fld-input" placeholder="如 竞赛获奖" />
                </div>
                <div class="fld">
                  <label>颜色</label>
                  <div class="color-field">
                    <input v-model="typeForm.color" class="fld-input" placeholder="#22c55e" />
                    <input type="color" v-model="typeForm.color" class="color-picker" />
                  </div>
                </div>
              </div>
              <div v-if="typeForm.id" class="fld">
                <label>类型编码</label>
                <input :value="typeForm.typeCode" class="fld-input fld-readonly" readonly />
              </div>
              <div class="fld">
                <label>适用主体</label>
                <div class="checkbox-group">
                  <label v-for="opt in SUBJECT_OPTIONS" :key="opt.value" class="check-item" :class="{ active: typeForm.selectedSubjects.includes(opt.value) }">
                    <input type="checkbox" :value="opt.value" v-model="typeForm.selectedSubjects" />
                    {{ opt.label }}
                  </label>
                </div>
              </div>
              <div class="fld">
                <label>排序</label>
                <input v-model.number="typeForm.sortOrder" type="number" min="0" class="fld-input fld-narrow" placeholder="0" />
              </div>
            </div>
            <div class="modal-foot">
              <button class="btn-cancel" @click="typeDialogVisible = false">取消</button>
              <button class="btn-save" @click="handleSaveType">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listEntityEventTypes,
  createEntityEventType,
  updateEntityEventType,
  deleteEntityEventType,
} from '@/api/entityEvent'
import type { EntityEventType, CreateEntityEventTypeCommand } from '@/types/entityEvent'

// ==================== State ====================
const loading = ref(false)
const allTypes = ref<EntityEventType[]>([])

const SUBJECT_OPTIONS = [
  { value: 'USER', label: '用户' },
  { value: 'ORG_UNIT', label: '组织' },
  { value: 'PLACE', label: '场所' },
]

// ==================== Computed ====================
interface GroupedCategory {
  categoryCode: string
  categoryName: string
  polarity: string
  items: EntityEventType[]
}

const groupedTypes = computed<GroupedCategory[]>(() => {
  const map = new Map<string, GroupedCategory>()
  for (const t of allTypes.value) {
    if (!map.has(t.categoryCode)) {
      map.set(t.categoryCode, {
        categoryCode: t.categoryCode,
        categoryName: t.categoryName,
        polarity: (t as any).categoryPolarity || guessCategoryPolarity(t.categoryCode),
        items: [],
      })
    }
    map.get(t.categoryCode)!.items.push(t)
  }
  return Array.from(map.values())
})

const categoryCount = computed(() => groupedTypes.value.length)
const totalCount = computed(() => allTypes.value.length)
const positiveCount = computed(() => groupedTypes.value.filter(g => g.polarity === 'POSITIVE').reduce((s, g) => s + g.items.length, 0))
const negativeCount = computed(() => groupedTypes.value.filter(g => g.polarity === 'NEGATIVE').reduce((s, g) => s + g.items.length, 0))
const neutralCount = computed(() => totalCount.value - positiveCount.value - negativeCount.value)

// ==================== Helpers ====================
function guessCategoryPolarity(code: string): string {
  const upper = code.toUpperCase()
  if (upper.includes('REWARD') || upper.includes('HONOR') || upper.includes('POSITIVE')) return 'POSITIVE'
  if (upper.includes('VIOLATION') || upper.includes('PENALTY') || upper.includes('NEGATIVE') || upper.includes('DISCIPLINE')) return 'NEGATIVE'
  return 'NEUTRAL'
}

function getPolarityLabel(p: string): string {
  return { POSITIVE: '正向', NEGATIVE: '负向', NEUTRAL: '中性' }[p] || '中性'
}

function getPolarityColor(p: string): string {
  return { POSITIVE: '#22c55e', NEGATIVE: '#ef4444', NEUTRAL: '#9ca3af' }[p] || '#9ca3af'
}

function parseSubjects(json: string | null): string[] {
  if (!json) return []
  try { return JSON.parse(json) } catch { return [] }
}

/** 判断事件类型是否因所属插件被禁而级联软失效 (Phase 2) */
function isTypePluginDisabled(t: any): boolean {
  const v = t?.pluginEnabled
  if (v === false || v === 0) return true
  return false
}

// ==================== 来源解析 ====================
// 优先 origin (PLUGIN:CORE@1.0.0 / TENANT:CUSTOM), 回退 industry, 再退 is_system 推断
function resolveSource(t: any): { code: string; label: string; tooltip: string } {
  const orig = t.origin as string | undefined
  if (orig) {
    const m = orig.match(/^PLUGIN:([A-Z_]+)@/)
    if (m) {
      const code = m[1]
      return { code, label: industryLabel(code), tooltip: `来源: ${t.pluginClass || orig}` }
    }
    if (orig.startsWith('TENANT:CUSTOM')) {
      return { code: 'CUSTOM', label: '自定义', tooltip: '管理员本地创建' }
    }
  }
  if (t.industry) {
    return { code: t.industry, label: industryLabel(t.industry), tooltip: t.pluginClass || t.industry }
  }
  if (t.isSystem) return { code: 'CORE', label: '通用核心', tooltip: '系统内置' }
  return { code: '', label: '', tooltip: '' }
}

function industryLabel(code: string): string {
  return ({ CORE: '通用核心', EDU: '教育行业', HEALTH: '医疗行业', CARE: '养老行业', CUSTOM: '自定义' } as any)[code] || code
}

function sourceChipStyle(code: string): Record<string, string> {
  const color = ({ CORE: '#2563eb', EDU: '#d97706', HEALTH: '#be185d', CARE: '#059669', CUSTOM: '#6b7280' } as any)[code] || '#6b7280'
  return { color, borderColor: color + '60', background: color + '12' }
}

// ==================== Load ====================
async function loadTypes() {
  loading.value = true
  try {
    // 管理员视角: 包含所属插件被禁的事件类型 (pluginEnabled=0), 前端灰显
    allTypes.value = await listEntityEventTypes(true)
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// ==================== Category Dialog ====================
const catDialogVisible = ref(false)
const catMaskTarget = ref<EventTarget | null>(null)
const catForm = reactive({
  isEdit: false,
  categoryCode: '',
  categoryName: '',
  polarity: 'NEUTRAL' as string,
  originalCode: '',
})

function catMaskDown(e: MouseEvent) { catMaskTarget.value = e.target }
function catMaskClick(e: MouseEvent) {
  if (e.target === e.currentTarget && catMaskTarget.value === e.currentTarget) catDialogVisible.value = false
  catMaskTarget.value = null
}

function openCategoryDialog(group?: GroupedCategory) {
  if (group) {
    catForm.isEdit = true
    catForm.categoryCode = group.categoryCode
    catForm.categoryName = group.categoryName
    catForm.polarity = group.polarity
    catForm.originalCode = group.categoryCode
  } else {
    catForm.isEdit = false
    catForm.categoryCode = ''
    catForm.categoryName = ''
    catForm.polarity = 'NEUTRAL'
    catForm.originalCode = ''
  }
  catDialogVisible.value = true
}

async function handleSaveCategory() {
  if (!catForm.categoryCode.trim()) { ElMessage.warning('请填写分类编码'); return }
  if (!catForm.categoryName.trim()) { ElMessage.warning('请填写分类名称'); return }

  if (catForm.isEdit) {
    // Update all types in this category with new categoryName
    const typesInCategory = allTypes.value.filter(t => t.categoryCode === catForm.originalCode)
    try {
      for (const t of typesInCategory) {
        await updateEntityEventType(t.id, { categoryName: catForm.categoryName.trim() })
      }
      ElMessage.success('分类已更新')
      catDialogVisible.value = false
      await loadTypes()
    } catch (e: any) {
      ElMessage.error(e.message || '更新失败')
    }
  } else {
    // Create new category via API
    try {
      await createEntityEventType({
        categoryCode: catForm.categoryCode.trim(),
        categoryName: catForm.categoryName.trim(),
        categoryPolarity: catForm.polarity,
        typeCode: catForm.categoryCode.trim() + '_DEFAULT',
        typeName: catForm.categoryName.trim() + '(默认)',
        isEnabled: 1,
        sortOrder: 0,
      })
      ElMessage.success('分类创建成功，请添加子类型')
      catDialogVisible.value = false
      await loadTypes()
    } catch (e: any) {
      ElMessage.error(e.message || '创建失败')
    }
  }
}

// ==================== Type Dialog ====================
const typeDialogVisible = ref(false)
const typeMaskTarget = ref<EventTarget | null>(null)
const typeForm = reactive({
  id: null as number | null,
  categoryCode: '',
  categoryName: '',
  typeCode: '',
  typeName: '',
  color: '#6b7280',
  selectedSubjects: [] as string[],
  sortOrder: 0,
})

function typeMaskDown(e: MouseEvent) { typeMaskTarget.value = e.target }
function typeMaskClick(e: MouseEvent) {
  if (e.target === e.currentTarget && typeMaskTarget.value === e.currentTarget) typeDialogVisible.value = false
  typeMaskTarget.value = null
}

function autoGenerateTypeCode(categoryCode: string): string {
  // CategoryCode + 6-char base36 timestamp suffix → 唯一且人眼可读
  const suffix = Date.now().toString(36).toUpperCase().slice(-6)
  return `${categoryCode}_${suffix}`
}

function openTypeDialog(categoryCode: string, categoryName: string, existing?: EntityEventType) {
  if (existing) {
    typeForm.id = existing.id
    typeForm.categoryCode = existing.categoryCode
    typeForm.categoryName = existing.categoryName
    typeForm.typeCode = existing.typeCode
    typeForm.typeName = existing.typeName
    typeForm.color = existing.color || '#6b7280'
    typeForm.selectedSubjects = parseSubjects(existing.applicableSubjects)
    typeForm.sortOrder = existing.sortOrder
  } else {
    typeForm.id = null
    typeForm.categoryCode = categoryCode
    typeForm.categoryName = categoryName
    typeForm.typeCode = autoGenerateTypeCode(categoryCode)
    typeForm.typeName = ''
    typeForm.color = '#6b7280'
    typeForm.selectedSubjects = []
    typeForm.sortOrder = 0
  }
  typeDialogVisible.value = true
}

async function handleSaveType() {
  if (!typeForm.typeName.trim()) { ElMessage.warning('请填写类型名称'); return }
  if (!typeForm.typeCode) typeForm.typeCode = autoGenerateTypeCode(typeForm.categoryCode)

  const cmd: CreateEntityEventTypeCommand = {
    categoryCode: typeForm.categoryCode.trim().toUpperCase(),
    categoryName: typeForm.categoryName.trim(),
    typeCode: typeForm.typeCode.trim().toUpperCase(),
    typeName: typeForm.typeName.trim(),
    hasScore: false,
    hasSeverity: false,
    icon: null,
    color: typeForm.color || null,
    applicableSubjects: typeForm.selectedSubjects.length ? JSON.stringify(typeForm.selectedSubjects) : null,
    sortOrder: typeForm.sortOrder,
  }

  try {
    if (typeForm.id) {
      await updateEntityEventType(typeForm.id, cmd)
      ElMessage.success('更新成功')
    } else {
      await createEntityEventType(cmd)
      ElMessage.success('创建成功')
    }
    typeDialogVisible.value = false
    await loadTypes()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDeleteType(t: EntityEventType) {
  try {
    await ElMessageBox.confirm(`确认删除事件类型「${t.typeName}」？此操作不可恢复。`, '确认删除', { type: 'warning' })
    await deleteEntityEventType(t.id)
    ElMessage.success('已删除')
    await loadTypes()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

// ==================== Lifecycle ====================
onMounted(() => { loadTypes() })
</script>

<style scoped>
/* ============================================
   Event Type Management — CourseListView Style
   ============================================ */
.etm-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8f9fb;
  font-family: 'DM Sans', sans-serif;
}

/* Header */
.etm-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 24px 16px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
}
.page-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.025em;
  margin: 0;
}
.stats-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}
.stat { font-size: 12.5px; color: #6b7280; }
.stat b { font-weight: 600; }
.c-positive { color: #16a34a; }
.c-negative { color: #dc2626; }
.c-neutral { color: #6b7280; }
.stat-sep { display: block; width: 1px; height: 10px; background: #d1d5db; }
.dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 2px; vertical-align: middle; }
.dot-positive { background: #22c55e; }
.dot-negative { background: #ef4444; }
.dot-neutral { background: #9ca3af; }

.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.btn-create:hover { background: #1f2937; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }

/* Body */
.etm-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #9ca3af;
}
.empty-title { font-size: 15px; font-weight: 600; color: #6b7280; margin-bottom: 4px; }
.empty-sub { font-size: 13px; }

/* Category Card */
.category-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f9fafb;
  border-bottom: 1px solid #f3f4f6;
}
.card-header-left { display: flex; align-items: center; gap: 8px; }
.card-header-right { display: flex; gap: 6px; }
.category-name { font-size: 14px; font-weight: 700; color: #111827; }
.polarity-badge {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}
.pol-positive { background: #f0fdf4; color: #16a34a; }
.pol-negative { background: #fef2f2; color: #dc2626; }
.pol-neutral { background: #f3f4f6; color: #6b7280; }
.type-count { font-size: 12px; color: #9ca3af; }

/* Type List */
.type-list { padding: 4px 0; }
.type-empty { padding: 16px; text-align: center; font-size: 12px; color: #d1d5db; }
.type-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  border-bottom: 1px solid #f8f9fa;
  transition: background 0.1s;
}
.type-row:last-child { border-bottom: none; }
.type-row:hover { background: #fafbfc; }
.type-name { font-size: 13px; font-weight: 500; color: #111827; min-width: 120px; }
.type-code {
  display: inline-block;
  padding: 2px 7px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  color: #475569;
}
.type-subjects { display: flex; gap: 4px; flex: 1; }
.subject-tag {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 600;
}
.subj-user { background: #eff6ff; color: #2563eb; }
.subj-org_unit { background: #fefce8; color: #ca8a04; }
.subj-place { background: #f0fdf4; color: #16a34a; }
.subject-none { font-size: 11px; color: #d1d5db; }

.type-row-actions { display: flex; gap: 4px; margin-left: auto; align-items: center; }
.system-badge {
  display: inline-block;
  padding: 1px 6px;
  font-size: 10px;
  font-weight: 600;
  color: #6b7280;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 3px;
  letter-spacing: 0.02em;
}
/* 来源 chip — 与插件平台页风格一致, 按 industry 着色 */
.source-chip {
  display: inline-flex; align-items: center;
  padding: 1px 8px;
  font-size: 10.5px;
  font-weight: 500;
  border-radius: 10px;
  border: 1px solid;
  letter-spacing: 0.02em;
  white-space: nowrap;
}
.source-custom { color: #6b7280; background: #f9fafb; border-color: #e5e7eb; }
.system-hint { font-size: 11.5px; color: #9ca3af; padding: 0 8px; }
.action-btn {
  padding: 4px 8px;
  font-size: 12px;
  color: #4b5563;
  background: none;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.1s;
}
.action-btn:hover { background: #f3f4f6; color: #111827; }
.action-primary { color: #2563eb; }
.action-primary:hover { background: #eff6ff; color: #1d4ed8; }
.action-danger:hover { background: #fef2f2; color: #dc2626; }

/* ============================================
   Modal
   ============================================ */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0,0,0,0.3);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-box {
  width: 480px;
  max-width: 90vw;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e8eaed;
}
.modal-head h3 { font-size: 15px; font-weight: 700; color: #111827; margin: 0; }
.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: #f3f4f6;
  color: #6b7280;
  cursor: pointer;
}
.modal-close:hover { background: #e5e7eb; color: #111827; }
.modal-body { padding: 16px 20px; display: flex; flex-direction: column; gap: 12px; }
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #f3f4f6;
}

.fld { display: flex; flex-direction: column; gap: 4px; }
.fld label { font-size: 12px; font-weight: 600; color: #374151; }
.req { color: #ef4444; }
.fld-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-family: inherit;
  color: #111827;
  background: #fafafa;
  outline: none;
  transition: border-color 0.15s;
}
.fld-input:focus { border-color: #2563eb; background: #fff; box-shadow: 0 0 0 3px rgba(37,99,235,0.08); }
.fld-input:disabled { background: #f3f4f6; color: #9ca3af; }
.fld-narrow { max-width: 120px; }
.fld-readonly { background: #f3f4f6; color: #6b7280; cursor: not-allowed; font-family: 'JetBrains Mono', monospace; font-size: 12px; }
.fld-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.color-field { display: flex; gap: 6px; align-items: center; }
.color-picker {
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 2px;
  cursor: pointer;
  flex-shrink: 0;
}

.radio-group { display: flex; gap: 8px; }
.radio-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}
.radio-item input { display: none; }
.radio-item.active { border-color: #2563eb; color: #2563eb; background: #eff6ff; }

.checkbox-group { display: flex; gap: 6px; flex-wrap: wrap; }
.check-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}
.check-item input { display: none; }
.check-item.active { border-color: #2563eb; color: #2563eb; background: #eff6ff; }

.btn-cancel {
  padding: 7px 16px;
  background: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.btn-cancel:hover { background: #e5e7eb; }
.btn-save {
  padding: 7px 16px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.btn-save:hover { background: #1f2937; }

/* Transitions */
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-active .modal-box, .modal-leave-active .modal-box { transition: transform 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal-box { transform: scale(0.95) translateY(10px); }
.modal-leave-to .modal-box { transform: scale(0.95) translateY(10px); }

/* 插件级联禁用的事件类型行: 灰显 (Phase 2) */
.row-disabled-by-plugin { opacity: 0.55; background-color: #fafaf9; }
.row-disabled-by-plugin:hover { background-color: #fef3c7 !important; opacity: 0.8; }

.disabled-by-plugin-badge {
  display: inline-block;
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  color: #a16207;
  background: #fef3c7;
  border: 1px solid #fcd34d;
  line-height: 1.4;
  cursor: help;
  margin-right: 4px;
}
</style>
