<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Plus, Search, Pencil, Trash2, X } from 'lucide-vue-next'
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

const searchKeyword = ref('')
const activeCategoryFilter = ref<string>('ALL')

// ==================== Computed ====================
const categories = computed(() => {
  const map = new Map<string, string>()
  for (const t of allTypes.value) {
    if (!map.has(t.categoryCode)) {
      map.set(t.categoryCode, t.categoryName)
    }
  }
  return Array.from(map.entries()).map(([code, name]) => ({ code, name }))
})

const filteredTypes = computed(() => {
  let list = allTypes.value
  if (activeCategoryFilter.value !== 'ALL') {
    list = list.filter(t => t.categoryCode === activeCategoryFilter.value)
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(t =>
      t.typeName.toLowerCase().includes(kw) ||
      t.typeCode.toLowerCase().includes(kw) ||
      t.categoryName.toLowerCase().includes(kw)
    )
  }
  return list
})

const groupedTypes = computed(() => {
  const groups = new Map<string, { categoryCode: string; categoryName: string; items: EntityEventType[] }>()
  for (const t of filteredTypes.value) {
    if (!groups.has(t.categoryCode)) {
      groups.set(t.categoryCode, { categoryCode: t.categoryCode, categoryName: t.categoryName, items: [] })
    }
    groups.get(t.categoryCode)!.items.push(t)
  }
  return Array.from(groups.values())
})

const totalCount = computed(() => allTypes.value.length)
const enabledCount = computed(() => allTypes.value.filter(t => t.isEnabled).length)

// ==================== Load ====================
async function loadTypes() {
  loading.value = true
  try {
    const data = await listEntityEventTypes()
    allTypes.value = data
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// ==================== Subject helpers ====================
const SUBJECT_OPTIONS = [
  { value: 'USER', label: 'USER' },
  { value: 'ORG', label: 'ORG' },
  { value: 'PLACE', label: 'PLACE' },
  { value: 'ASSET', label: 'ASSET' },
]

function parseSubjects(json: string | null): string[] {
  if (!json) return []
  try { return JSON.parse(json) } catch { return [] }
}

function formatSubjects(json: string | null): string {
  const arr = parseSubjects(json)
  return arr.length ? arr.join(' / ') : '—'
}

// ==================== Dialog ====================
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const maskMouseDownTarget = ref<EventTarget | null>(null)

const form = reactive<{
  categoryCode: string
  categoryName: string
  typeCode: string
  typeName: string
  hasScore: boolean
  hasSeverity: boolean
  selectedSubjects: string[]
  sortOrder: number
}>({
  categoryCode: '',
  categoryName: '',
  typeCode: '',
  typeName: '',
  hasScore: false,
  hasSeverity: false,
  selectedSubjects: [],
  sortOrder: 0,
})

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    categoryCode: '',
    categoryName: '',
    typeCode: '',
    typeName: '',
    hasScore: false,
    hasSeverity: false,
    selectedSubjects: [],
    sortOrder: 0,
  })
  dialogVisible.value = true
}

function openEdit(type: EntityEventType) {
  editingId.value = type.id
  Object.assign(form, {
    categoryCode: type.categoryCode,
    categoryName: type.categoryName,
    typeCode: type.typeCode,
    typeName: type.typeName,
    hasScore: type.hasScore,
    hasSeverity: type.hasSeverity,
    selectedSubjects: parseSubjects(type.applicableSubjects),
    sortOrder: type.sortOrder,
  })
  dialogVisible.value = true
}

function closeDialog() { dialogVisible.value = false }

function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClick(e: MouseEvent) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeDialog()
  maskMouseDownTarget.value = null
}

async function handleSave() {
  if (!form.categoryCode.trim()) { ElMessage.warning('请填写大类编码'); return }
  if (!form.categoryName.trim()) { ElMessage.warning('请填写大类名称'); return }
  if (!form.typeCode.trim()) { ElMessage.warning('请填写类型编码'); return }
  if (!form.typeName.trim()) { ElMessage.warning('请填写类型名称'); return }

  const cmd: CreateEntityEventTypeCommand = {
    categoryCode: form.categoryCode.trim().toUpperCase(),
    categoryName: form.categoryName.trim(),
    typeCode: form.typeCode.trim().toUpperCase(),
    typeName: form.typeName.trim(),
    hasScore: form.hasScore,
    hasSeverity: form.hasSeverity,
    applicableSubjects: form.selectedSubjects.length
      ? JSON.stringify(form.selectedSubjects)
      : null,
    sortOrder: form.sortOrder,
  }

  try {
    if (editingId.value !== null) {
      await updateEntityEventType(editingId.value, cmd)
      ElMessage.success('更新成功')
    } else {
      await createEntityEventType(cmd)
      ElMessage.success('创建成功')
    }
    closeDialog()
    await loadTypes()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

// ==================== Delete ====================
async function handleDelete(type: EntityEventType) {
  try {
    await ElMessageBox.confirm(
      `确认删除事件类型「${type.typeName}」？此操作不可恢复。`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteEntityEventType(type.id)
    ElMessage.success('已删除')
    await loadTypes()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

// ==================== Toggle Enable ====================
async function handleToggleEnable(type: EntityEventType) {
  try {
    await updateEntityEventType(type.id, { isEnabled: !type.isEnabled })
    ElMessage.success(type.isEnabled ? '已禁用' : '已启用')
    await loadTypes()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

// ==================== Lifecycle ====================
onMounted(() => { loadTypes() })
</script>

<template>
  <div class="evt" @click.self="() => {}">
    <!-- Header -->
    <div class="evt-header">
      <div class="evt-header-left">
        <Bell :size="18" class="evt-header-icon" />
        <h1 class="evt-title">事件类型管理</h1>
      </div>
      <button class="btn-primary" @click="openCreate">
        <Plus :size="14" />
        新建类型
      </button>
    </div>

    <!-- Stats bar -->
    <div class="evt-stats">
      <span class="stat-text">共 <strong>{{ totalCount }}</strong> 个类型</span>
      <span class="stat-sep">│</span>
      <span class="stat-text">已启用 <strong class="text-enabled">{{ enabledCount }}</strong></span>
      <span class="stat-sep">│</span>
      <span class="stat-text">已禁用 <strong>{{ totalCount - enabledCount }}</strong></span>
    </div>

    <!-- Toolbar -->
    <div class="evt-toolbar">
      <div class="evt-toolbar-left">
        <!-- Category filter tabs -->
        <button
          class="cat-tab"
          :class="{ active: activeCategoryFilter === 'ALL' }"
          @click="activeCategoryFilter = 'ALL'"
        >全部</button>
        <button
          v-for="cat in categories"
          :key="cat.code"
          class="cat-tab"
          :class="{ active: activeCategoryFilter === cat.code }"
          @click="activeCategoryFilter = cat.code"
        >{{ cat.name }}</button>
      </div>

      <div class="search-wrap">
        <Search :size="13" class="search-icon" />
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索类型编码或名称..."
          class="search-input"
        />
      </div>
    </div>

    <!-- Content -->
    <div class="evt-content">
      <!-- Loading -->
      <div v-if="loading" class="state-loading">
        <div class="spinner" />
        <span>加载中...</span>
      </div>

      <!-- Empty -->
      <div v-else-if="groupedTypes.length === 0" class="state-empty">
        <Bell :size="44" class="empty-icon" />
        <p class="empty-title">暂无事件类型</p>
        <p class="empty-sub">事件类型用于统一记录系统中各主体的行为与事件</p>
        <button class="btn-primary" @click="openCreate">
          <Plus :size="14" /> 创建第一个类型
        </button>
      </div>

      <!-- Groups -->
      <div v-else class="group-list">
        <div v-for="group in groupedTypes" :key="group.categoryCode" class="type-group">
          <div class="group-header">
            <span class="group-code">{{ group.categoryCode }}</span>
            <span class="group-name">{{ group.categoryName }}</span>
            <span class="group-count">{{ group.items.length }} 个</span>
          </div>
          <div class="type-table">
            <div
              v-for="type in group.items"
              :key="type.id"
              class="type-row"
              :class="{ disabled: !type.isEnabled }"
            >
              <div class="type-codes">
                <span class="type-code-badge">{{ type.typeCode }}</span>
              </div>
              <div class="type-name">{{ type.typeName }}</div>
              <div class="type-tags">
                <span v-if="type.hasScore" class="tag tag-score">分值</span>
                <span v-if="type.hasSeverity" class="tag tag-severity">严重程度</span>
              </div>
              <div class="type-subjects">
                <template v-if="parseSubjects(type.applicableSubjects).length">
                  <span
                    v-for="subj in parseSubjects(type.applicableSubjects)"
                    :key="subj"
                    class="subject-tag"
                    :class="'subj-' + subj.toLowerCase()"
                  >{{ subj }}</span>
                </template>
                <span v-else class="subject-none">—</span>
              </div>
              <div class="type-status">
                <span v-if="type.isSystem" class="tag tag-system">系统</span>
                <span v-else class="tag tag-custom">自定义</span>
              </div>
              <div class="type-actions">
                <button
                  class="act-btn"
                  title="编辑"
                  :disabled="type.isSystem"
                  @click="openEdit(type)"
                >
                  <Pencil :size="13" />
                </button>
                <button
                  class="act-btn"
                  :title="type.isEnabled ? '禁用' : '启用'"
                  @click="handleToggleEnable(type)"
                >
                  <span class="toggle-dot" :class="{ on: type.isEnabled }" />
                </button>
                <button
                  class="act-btn act-btn-danger"
                  title="删除"
                  :disabled="type.isSystem"
                  @click="handleDelete(type)"
                >
                  <Trash2 :size="13" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create / Edit Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="dialogVisible"
          class="modal-mask"
          @mousedown="onMaskMouseDown"
          @click="onMaskClick"
        >
          <div class="modal-box">
            <div class="modal-head">
              <h3>{{ editingId !== null ? '编辑事件类型' : '新建事件类型' }}</h3>
              <button class="modal-close" @click="closeDialog"><X :size="16" /></button>
            </div>

            <div class="modal-body">
              <!-- Category row -->
              <div class="fld-row">
                <div class="fld">
                  <label>大类编码 <span class="req">*</span></label>
                  <input
                    v-model="form.categoryCode"
                    placeholder="如：INSP"
                    class="fld-input"
                    :disabled="editingId !== null"
                  />
                </div>
                <div class="fld">
                  <label>大类名称 <span class="req">*</span></label>
                  <input
                    v-model="form.categoryName"
                    placeholder="如：检查"
                    class="fld-input"
                  />
                </div>
              </div>

              <!-- Type row -->
              <div class="fld-row">
                <div class="fld">
                  <label>类型编码 <span class="req">*</span></label>
                  <input
                    v-model="form.typeCode"
                    placeholder="如：INSP_GRADE"
                    class="fld-input"
                    :disabled="editingId !== null"
                  />
                </div>
                <div class="fld">
                  <label>类型名称 <span class="req">*</span></label>
                  <input
                    v-model="form.typeName"
                    placeholder="如：检查评级"
                    class="fld-input"
                  />
                </div>
              </div>

              <!-- Flags row -->
              <div class="fld-row fld-checks">
                <label class="check-label">
                  <input type="checkbox" v-model="form.hasScore" />
                  <span>有分值</span>
                </label>
                <label class="check-label">
                  <input type="checkbox" v-model="form.hasSeverity" />
                  <span>有严重程度</span>
                </label>
              </div>

              <!-- Applicable subjects -->
              <div class="fld">
                <label>适用主体</label>
                <div class="subject-checks">
                  <label
                    v-for="opt in SUBJECT_OPTIONS"
                    :key="opt.value"
                    class="check-label"
                  >
                    <input
                      type="checkbox"
                      :value="opt.value"
                      v-model="form.selectedSubjects"
                    />
                    <span class="subject-tag" :class="'subj-' + opt.value.toLowerCase()">{{ opt.label }}</span>
                  </label>
                </div>
              </div>

              <!-- Sort order -->
              <div class="fld fld-narrow">
                <label>排序</label>
                <input
                  v-model.number="form.sortOrder"
                  type="number"
                  min="0"
                  class="fld-input"
                  placeholder="0"
                />
              </div>
            </div>

            <div class="modal-foot">
              <button class="btn-ghost" @click="closeDialog">取消</button>
              <button class="btn-primary" @click="handleSave">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ==================== Root ==================== */
.evt {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ==================== Header ==================== */
.evt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 14px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.evt-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.evt-header-icon {
  color: #1a6dff;
}
.evt-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}

/* ==================== Stats bar ==================== */
.evt-stats {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 8px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
}
.stat-text {
  font-size: 13px;
  color: #5a6474;
  padding: 0 4px;
}
.stat-text strong {
  color: #1e2a3a;
  font-weight: 700;
}
.text-enabled {
  color: #10b981 !important;
}
.stat-sep {
  color: #dce1e8;
  font-size: 14px;
  padding: 0 14px;
  user-select: none;
}

/* ==================== Toolbar ==================== */
.evt-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
  gap: 12px;
  flex-wrap: wrap;
}
.evt-toolbar-left {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.cat-tab {
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #5a6474;
  background: none;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.cat-tab:hover {
  background: #f4f6f9;
  color: #1e2a3a;
}
.cat-tab.active {
  background: #e8f0ff;
  color: #1a6dff;
  border-color: #c5d8ff;
}

.search-wrap {
  position: relative;
}
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #b8c0cc;
  pointer-events: none;
}
.search-input {
  height: 34px;
  width: 220px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 0 12px 0 32px;
  font-size: 13px;
  outline: none;
  background: #fff;
  color: #3d4757;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.search-input::placeholder { color: #b8c0cc; }
.search-input:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.08);
}

/* ==================== Content ==================== */
.evt-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* ==================== Groups ==================== */
.group-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.type-group {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  overflow: hidden;
}
.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #fafbfc;
  border-bottom: 1px solid #f0f2f5;
}
.group-code {
  font-size: 11px;
  font-weight: 700;
  font-family: 'SFMono-Regular', Consolas, monospace;
  color: #fff;
  background: #1a6dff;
  padding: 2px 8px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}
.group-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e2a3a;
}
.group-count {
  font-size: 12px;
  color: #b8c0cc;
  margin-left: auto;
}

/* ==================== Type rows ==================== */
.type-table {
  display: flex;
  flex-direction: column;
}
.type-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid #f5f6f8;
  transition: background 0.12s;
}
.type-row:last-child {
  border-bottom: none;
}
.type-row:hover {
  background: #fafbfc;
}
.type-row.disabled {
  opacity: 0.5;
}

.type-codes {
  flex-shrink: 0;
  width: 160px;
}
.type-code-badge {
  font-size: 11px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-weight: 600;
  color: #3d4757;
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 4px;
  letter-spacing: 0.3px;
}
.type-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #1e2a3a;
  min-width: 0;
}
.type-tags {
  display: flex;
  gap: 5px;
  flex-shrink: 0;
  min-width: 80px;
}
.type-subjects {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  min-width: 120px;
  flex-wrap: wrap;
}
.subject-none {
  font-size: 12px;
  color: #c8d0db;
}
.type-status {
  flex-shrink: 0;
  width: 60px;
  display: flex;
  justify-content: center;
}
.type-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

/* ==================== Tags ==================== */
.tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 7px;
  border-radius: 4px;
  white-space: nowrap;
}
.tag-score { background: #e8f0ff; color: #1a6dff; }
.tag-severity { background: #fff7ed; color: #d97706; }
.tag-system { background: #f0f2f5; color: #6b7685; }
.tag-custom { background: #ecfdf5; color: #059669; }

/* ==================== Subject tags ==================== */
.subject-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 4px;
  white-space: nowrap;
}
.subj-user { background: #f5f3ff; color: #7c3aed; }
.subj-org { background: #e8f0ff; color: #2563eb; }
.subj-place { background: #ecfdf5; color: #059669; }
.subj-asset { background: #fff7ed; color: #d97706; }

/* ==================== Action buttons ==================== */
.act-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: none;
  border: none;
  border-radius: 6px;
  color: #8c95a3;
  cursor: pointer;
  transition: all 0.12s;
}
.act-btn:hover {
  background: #f0f2f5;
  color: #1a6dff;
}
.act-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  pointer-events: none;
}
.act-btn-danger:hover {
  background: #fef2f2;
  color: #d93025;
}

/* Toggle dot */
.toggle-dot {
  display: block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d1d5db;
  transition: background 0.15s;
}
.toggle-dot.on {
  background: #10b981;
}

/* ==================== State views ==================== */
.state-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 80px 0;
  color: #b8c0cc;
  font-size: 13px;
}
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e8ecf0;
  border-top-color: #1a6dff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.state-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 10px;
}
.empty-icon { color: #dce1e8; }
.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #6b7685;
  margin: 0;
}
.empty-sub {
  font-size: 12px;
  color: #b8c0cc;
  margin: 0;
  text-align: center;
  max-width: 320px;
}

/* ==================== Buttons ==================== */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  background: #1a6dff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.btn-primary:hover { background: #1558d6; }
.btn-ghost {
  padding: 8px 16px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-ghost:hover { background: #f4f6f9; }

/* ==================== Modal ==================== */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(2px);
}
.modal-box {
  width: 520px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.modal-head h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0;
}
.modal-close {
  background: none;
  border: none;
  color: #b8c0cc;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: color 0.12s;
}
.modal-close:hover { color: #5a6474; }
.modal-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 24px;
}
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 20px;
}

/* ==================== Form fields ==================== */
.fld-row {
  display: flex;
  gap: 12px;
}
.fld-row .fld { flex: 1; }
.fld-checks {
  align-items: center;
}
.fld {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.fld-narrow {
  max-width: 120px;
}
.fld label {
  font-size: 12px;
  font-weight: 500;
  color: #5a6474;
}
.fld-input {
  width: 100%;
  box-sizing: border-box;
  height: 34px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 13px;
  outline: none;
  color: #1e2a3a;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
  font-family: inherit;
}
.fld-input::placeholder { color: #b8c0cc; }
.fld-input:focus {
  border-color: #7aadff;
  box-shadow: 0 0 0 3px rgba(26, 109, 255, 0.08);
}
.fld-input:disabled {
  background: #f4f6f9;
  color: #8c95a3;
  cursor: not-allowed;
}

.check-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #3d4757;
}
.check-label input[type="checkbox"] {
  width: 14px;
  height: 14px;
  cursor: pointer;
  accent-color: #1a6dff;
}

.subject-checks {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding: 8px 0 2px;
}

.req { color: #d93025; }

/* Modal transition */
.modal-enter-active { transition: all 0.2s ease-out; }
.modal-leave-active { transition: all 0.15s ease-in; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .modal-box { transform: translateY(12px) scale(0.97); }
.modal-leave-to { opacity: 0; }
.modal-leave-to .modal-box { transform: translateY(-8px) scale(0.98); }
</style>
