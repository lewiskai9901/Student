<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Pencil, Trash2, RefreshCw, Library, Package,
} from 'lucide-vue-next'
import type { LibraryItem, CreateLibraryItemRequest, UpdateLibraryItemRequest } from '@/types/insp/template'
import type { ItemType } from '@/types/insp/enums'
import {
  getLibraryItems, getLibraryCategories,
  createLibraryItem, updateLibraryItem, deleteLibraryItem, syncLibraryItem,
} from '@/api/inspection/library'

// ==================== State ====================
const loading = ref(false)
const items = ref<LibraryItem[]>([])
const categories = ref<string[]>([])

const query = reactive({
  keyword: '',
  category: '',
})

// Mousedown guard for dialog mask
const maskMouseDownTarget = ref<EventTarget | null>(null)
function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClose(e: MouseEvent, closeFn: () => void) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeFn()
  maskMouseDownTarget.value = null
}

// Dialog state
const dialogVisible = ref(false)
const dialogTitle = ref('创建库项目')
const editingId = ref<number | null>(null)
const form = reactive({
  itemCode: '',
  itemName: '',
  itemType: 'TEXT' as ItemType,
  description: '',
  category: '',
  tags: '',
  defaultConfig: '',
  defaultValidationRules: '',
  defaultScoringConfig: '',
  defaultHelpContent: '',
  isStandard: false,
})

const itemTypeOptions: { value: ItemType; label: string }[] = [
  { value: 'TEXT', label: '文本' },
  { value: 'TEXTAREA', label: '多行文本' },
  { value: 'NUMBER', label: '数值' },
  { value: 'SELECT', label: '单选' },
  { value: 'MULTI_SELECT', label: '多选' },
  { value: 'CHECKBOX', label: '复选框' },
  { value: 'RADIO', label: '单选按钮' },
  { value: 'DATE', label: '日期' },
  { value: 'TIME', label: '时间' },
  { value: 'DATETIME', label: '日期时间' },
  { value: 'PHOTO', label: '照片' },
  { value: 'VIDEO', label: '视频' },
  { value: 'SIGNATURE', label: '签名' },
  { value: 'FILE_UPLOAD', label: '文件上传' },
  { value: 'GPS', label: 'GPS' },
  { value: 'BARCODE', label: '条码' },
  { value: 'RATING', label: '评分' },
  { value: 'PASS_FAIL', label: '通过/不通过' },
  { value: 'SLIDER', label: '滑块' },
  { value: 'RICH_TEXT', label: '富文本' },
  { value: 'CALCULATED', label: '计算字段' },
  { value: 'CHECKLIST', label: '检查清单' },
]

const filteredItems = computed(() => items.value)

// ==================== Actions ====================
async function loadItems() {
  loading.value = true
  try {
    const params: Record<string, string> = {}
    if (query.keyword) params.keyword = query.keyword
    if (query.category) params.category = query.category
    items.value = await getLibraryItems(params)
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await getLibraryCategories()
  } catch { /* ignore */ }
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '创建库项目'
  Object.assign(form, {
    itemCode: '', itemName: '', itemType: 'TEXT',
    description: '', category: '', tags: '',
    defaultConfig: '', defaultValidationRules: '',
    defaultScoringConfig: '', defaultHelpContent: '',
    isStandard: false,
  })
  dialogVisible.value = true
}

function openEdit(item: LibraryItem) {
  editingId.value = item.id
  dialogTitle.value = '编辑库项目'
  Object.assign(form, {
    itemCode: item.itemCode,
    itemName: item.itemName,
    itemType: item.itemType,
    description: item.description || '',
    category: item.category || '',
    tags: item.tags || '',
    defaultConfig: item.defaultConfig || '',
    defaultValidationRules: item.defaultValidationRules || '',
    defaultScoringConfig: item.defaultScoringConfig || '',
    defaultHelpContent: item.defaultHelpContent || '',
    isStandard: item.isStandard,
  })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.itemCode || !form.itemName) {
    ElMessage.warning('编码和名称不能为空')
    return
  }
  try {
    if (editingId.value) {
      const data: UpdateLibraryItemRequest = {
        itemName: form.itemName,
        itemType: form.itemType,
        description: form.description || undefined,
        category: form.category || undefined,
        tags: form.tags || undefined,
        defaultConfig: form.defaultConfig || undefined,
        defaultValidationRules: form.defaultValidationRules || undefined,
        defaultScoringConfig: form.defaultScoringConfig || undefined,
        defaultHelpContent: form.defaultHelpContent || undefined,
        isStandard: form.isStandard,
      }
      await updateLibraryItem(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      const data: CreateLibraryItemRequest = {
        itemCode: form.itemCode,
        itemName: form.itemName,
        itemType: form.itemType,
        description: form.description || undefined,
        category: form.category || undefined,
        tags: form.tags || undefined,
        defaultConfig: form.defaultConfig || undefined,
        defaultValidationRules: form.defaultValidationRules || undefined,
        defaultScoringConfig: form.defaultScoringConfig || undefined,
        defaultHelpContent: form.defaultHelpContent || undefined,
        isStandard: form.isStandard,
      }
      await createLibraryItem(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadItems()
    loadCategories()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(item: LibraryItem) {
  if (item.isStandard) {
    ElMessage.warning('标准项不可删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除「${item.itemName}」？`, '确认删除', { type: 'warning' })
    await deleteLibraryItem(item.id)
    ElMessage.success('删除成功')
    loadItems()
  } catch { /* cancelled */ }
}

async function handleSync(item: LibraryItem) {
  try {
    const count = await syncLibraryItem(item.id)
    ElMessage.success(`已同步到 ${count} 个模板项`)
  } catch (e: any) {
    ElMessage.error(e.message || '同步失败')
  }
}

function getTypeLabel(type: string) {
  return itemTypeOptions.find(o => o.value === type)?.label || type
}

function getTagList(tags: string | null): string[] {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

onMounted(() => {
  loadItems()
  loadCategories()
})
</script>

<template>
  <div class="insp-shell lib-archive">
    <!-- ── Editorial header ─────────── -->
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">检查项库 / Item Library</div>
        <h1 class="insp-display page-title">检查项库</h1>
      </div>
      <div class="head-stats">
        <div class="insp-stat">
          <span class="insp-stat__value">{{ items.length }}</span>
          <span class="insp-stat__label">项目总数</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value" :style="{ color: 'var(--insp-pass)' }">{{ items.filter(i => i.isStandard).length }}</span>
          <span class="insp-stat__label">标准项</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value">{{ categories.length }}</span>
          <span class="insp-stat__label">分类</span>
        </div>
        <button class="head-cta insp-btn insp-btn--accent insp-btn--lg" @click="openCreate">
          新建项目
        </button>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong head-divider" />

    <!-- ── Filter rail ─────────── -->
    <nav class="filter-rail">
      <button class="filter-tab" :class="{ 'is-active': !query.category }" @click="query.category = ''; loadItems()">
        <span class="filter-tab__label">全部分类</span>
        <span class="filter-tab__count">{{ items.length }}</span>
      </button>
      <button v-for="c in categories" :key="c" class="filter-tab"
              :class="{ 'is-active': query.category === c }"
              @click="query.category = c; loadItems()">
        <span class="filter-tab__label">{{ c }}</span>
      </button>
      <div class="filter-spacer" />
      <div class="filter-search">
        <input v-model="query.keyword" placeholder="搜索编码 · 名称 · 标签..."
               @keyup.enter="loadItems" />
      </div>
    </nav>

    <!-- ── Library register ─────────── -->
    <section v-loading="loading" class="library">
      <article v-for="(item, i) in filteredItems" :key="item.id" class="lib-row">
        <div class="row-num insp-num">{{ String(i + 1).padStart(3, '0') }}</div>

        <div class="row-meta">
          <div class="row-name-line">
            <span class="row-name">{{ item.itemName }}</span>
            <span class="insp-chip insp-chip--info">{{ getTypeLabel(item.itemType) }}</span>
            <span v-if="item.isStandard" class="insp-stamp">标准</span>
          </div>
          <div class="row-code-line">
            <span class="row-code">{{ item.itemCode }}</span>
            <span v-if="item.category" class="row-sep">·</span>
            <span v-if="item.category" class="row-cat">{{ item.category }}</span>
          </div>
        </div>

        <div class="row-desc">
          <p v-if="item.description" class="row-desc__text">{{ item.description }}</p>
          <div v-if="getTagList(item.tags).length" class="row-tags">
            <span v-for="tag in getTagList(item.tags)" :key="tag" class="row-tag">#{{ tag }}</span>
          </div>
        </div>

        <div class="row-usage">
          <span class="usage-num insp-num">{{ item.usageCount || 0 }}</span>
          <span class="usage-label">次引用</span>
        </div>

        <div class="row-actions" @click.stop>
          <button class="insp-btn insp-btn--sm" @click="handleSync(item)" title="同步到使用此项的模板">同步</button>
          <button class="insp-btn insp-btn--sm" @click="openEdit(item)">编辑</button>
          <button v-if="!item.isStandard" class="insp-btn insp-btn--sm insp-btn--ghost" @click="handleDelete(item)" title="删除">×</button>
        </div>
      </article>

      <div v-if="!loading && filteredItems.length === 0" class="empty">
        <div class="insp-stamp">无记录</div>
        <p class="empty-hint">{{ query.keyword || query.category ? '未找到匹配项' : '尚未创建任何检查项' }}</p>
        <button v-if="!query.keyword && !query.category" class="insp-btn insp-btn--accent insp-btn--lg" @click="openCreate">
          创建第一个检查项
        </button>
      </div>
    </section>

    <!-- ── Modal ─────────── -->
    <Transition name="lib-modal">
      <div v-if="dialogVisible" class="lib-mask" @mousedown="onMaskMouseDown" @click="onMaskClose($event, () => dialogVisible = false)">
        <div class="lib-modal">
          <div class="lib-modal-head">
            <span class="insp-eyebrow">{{ editingId ? 'edit' : 'new' }}</span>
            <h3 class="lib-modal-title">{{ dialogTitle }}</h3>
            <button class="lib-modal-close" @click="dialogVisible = false">&times;</button>
          </div>
          <div class="lib-modal-body">
            <div class="form-grid">
              <div class="lib-fld">
                <label class="insp-caps">项目编码</label>
                <input v-model="form.itemCode" :disabled="!!editingId" placeholder="唯一编码" />
              </div>
              <div class="lib-fld">
                <label class="insp-caps">项目名称</label>
                <input v-model="form.itemName" placeholder="检查项名称" />
              </div>
              <div class="lib-fld">
                <label class="insp-caps">字段类型</label>
                <select v-model="form.itemType">
                  <option v-for="opt in itemTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div class="lib-fld">
                <label class="insp-caps">分类</label>
                <input v-model="form.category" placeholder="如: 卫生 / 安全 / 纪律" />
              </div>
              <div class="lib-fld lib-fld--full">
                <label class="insp-caps">标签 · 逗号分隔</label>
                <input v-model="form.tags" placeholder="日常,重点,高频" />
              </div>
              <div class="lib-fld lib-fld--full">
                <label class="insp-caps">描述</label>
                <textarea v-model="form.description" rows="2" placeholder="可选描述"></textarea>
              </div>
              <div class="lib-fld lib-fld--full">
                <label class="insp-caps">默认帮助内容</label>
                <textarea v-model="form.defaultHelpContent" rows="2" placeholder="检查员看到的帮助说明"></textarea>
              </div>
              <label class="lib-checkbox lib-fld--full">
                <input v-model="form.isStandard" type="checkbox" />
                <span>标准项 · 不可删除</span>
              </label>
            </div>
          </div>
          <div class="lib-modal-foot">
            <button class="insp-btn" @click="dialogVisible = false">取消</button>
            <button class="insp-btn insp-btn--accent" @click="handleSave">保存</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.lib-archive {
  padding: 32px 48px 64px;
  max-width: 1500px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--insp-bg-page);
}

/* ─ Header ─────── */
.page-head {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: var(--insp-sp-7);
  margin-bottom: var(--insp-sp-4);
}
.page-title { font-size: 44px; margin: 0; font-weight: 500; }
.head-stats {
  display: flex; align-items: center; gap: var(--insp-sp-6);
  padding: 4px 0;
}
.head-rule { width: 1px; height: 32px; background: var(--insp-border-default); }
.head-cta { margin-left: var(--insp-sp-5); }
.head-divider { margin: 0 0 var(--insp-sp-7); }

/* ─ Filter rail ─────── */
.filter-rail {
  display: flex; align-items: center; gap: 0;
  margin-bottom: var(--insp-sp-5);
  border-bottom: 1px solid var(--insp-border-subtle);
  flex-wrap: wrap;
}
.filter-tab {
  position: relative;
  display: inline-flex; align-items: baseline; gap: var(--insp-sp-2);
  padding: 10px 16px 12px;
  border: 0; background: transparent; cursor: pointer;
  font-family: inherit; font-size: var(--insp-text-md); font-weight: 500;
  color: var(--insp-ink-tertiary);
  transition: color var(--insp-t-fast);
}
.filter-tab:hover { color: var(--insp-ink-primary); }
.filter-tab.is-active { color: var(--insp-ink-primary); }
.filter-tab.is-active::after {
  content: ''; position: absolute; left: 16px; right: 16px; bottom: -1px;
  height: 2px; background: var(--insp-accent);
}
.filter-tab__count {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs); font-weight: 500;
  color: var(--insp-ink-quaternary);
}
.filter-tab.is-active .filter-tab__count { color: var(--insp-accent); }
.filter-spacer { flex: 1; }
.filter-search input {
  padding: 6px 12px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  font-family: inherit;
  font-size: var(--insp-text-sm);
  width: 240px;
  background: var(--insp-bg-surface);
}
.filter-search input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}

/* ─ Library rows ─────── */
.library { display: flex; flex-direction: column; }

.lib-row {
  display: grid;
  grid-template-columns: 56px 280px 1fr 110px 200px;
  gap: var(--insp-sp-5);
  align-items: start;
  padding: var(--insp-sp-5) 0;
  border-bottom: 1px solid var(--insp-border-subtle);
  transition: background var(--insp-t-fast);
}
.lib-row:hover {
  background: linear-gradient(to right, var(--insp-bg-subtle), transparent 80%);
}

.row-num {
  font-size: 26px; font-weight: 500;
  color: var(--insp-ink-quaternary);
  letter-spacing: -0.02em; line-height: 1;
  padding-top: 2px;
}

.row-meta { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.row-name-line {
  display: flex; align-items: center; gap: var(--insp-sp-2);
  flex-wrap: wrap;
}
.row-name {
  font-family: var(--insp-font-display);
  font-size: 18px; font-weight: 500;
  color: var(--insp-ink-primary);
  letter-spacing: var(--insp-tracking-tight);
}
.row-code-line {
  display: flex; align-items: center; gap: 6px;
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}
.row-code {
  font-family: var(--insp-font-mono);
  font-weight: 500;
  color: var(--insp-ink-secondary);
}
.row-sep { color: var(--insp-ink-quaternary); }
.row-cat { color: var(--insp-ink-secondary); }

.row-desc { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.row-desc__text {
  font-size: var(--insp-text-md);
  line-height: var(--insp-leading-snug);
  color: var(--insp-ink-secondary);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.row-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.row-tag {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  color: var(--insp-accent);
  font-weight: 500;
}

.row-usage {
  display: flex; flex-direction: column; align-items: flex-end;
  gap: 2px;
  text-align: right;
}
.usage-num {
  font-family: var(--insp-font-mono);
  font-size: 22px; font-weight: 600;
  color: var(--insp-ink-primary);
  line-height: 1;
  letter-spacing: -0.02em;
}
.usage-label {
  font-size: var(--insp-text-2xs);
  font-weight: 500;
  letter-spacing: var(--insp-tracking-caps);
  text-transform: uppercase;
  color: var(--insp-ink-tertiary);
}

.row-actions {
  display: flex; gap: var(--insp-sp-2);
  justify-content: flex-end;
  align-items: flex-start;
  flex-wrap: wrap;
  padding-top: 2px;
}

/* ─ Empty ─────── */
.empty { padding: 80px 0; text-align: center; }
.empty-hint {
  margin: var(--insp-sp-4) 0 var(--insp-sp-5);
  color: var(--insp-ink-tertiary);
  font-size: var(--insp-text-sm);
}

/* ─ Modal ─────── */
.lib-mask {
  position: fixed; inset: 0; z-index: 1000;
  display: flex; align-items: center; justify-content: center;
  background: var(--insp-bg-overlay);
  backdrop-filter: blur(2px);
}
.lib-modal {
  width: 640px; max-height: 85vh;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-strong);
  border-radius: var(--insp-radius-md);
  box-shadow: var(--insp-shadow-lg);
  overflow-y: auto;
}
.lib-modal-head {
  display: flex; align-items: baseline; gap: var(--insp-sp-3);
  padding: var(--insp-sp-5) var(--insp-sp-6) var(--insp-sp-3);
  border-bottom: 1px solid var(--insp-border-subtle);
}
.lib-modal-title {
  font-family: var(--insp-font-display);
  font-size: 24px; font-weight: 500;
  letter-spacing: var(--insp-tracking-display);
  margin: 0; color: var(--insp-ink-primary);
}
.lib-modal-close {
  margin-left: auto;
  background: transparent; border: 0;
  font-size: 22px; color: var(--insp-ink-tertiary);
  cursor: pointer; line-height: 1;
}
.lib-modal-close:hover { color: var(--insp-ink-primary); }
.lib-modal-body { padding: var(--insp-sp-5) var(--insp-sp-6); }
.lib-modal-foot {
  display: flex; justify-content: flex-end; gap: var(--insp-sp-3);
  padding: var(--insp-sp-4) var(--insp-sp-6);
  border-top: 1px solid var(--insp-border-subtle);
}

.lib-modal-enter-active, .lib-modal-leave-active { transition: opacity var(--insp-t-medium); }
.lib-modal-enter-from, .lib-modal-leave-to { opacity: 0; }

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--insp-sp-4);
}
.lib-fld { display: flex; flex-direction: column; gap: 6px; }
.lib-fld--full { grid-column: span 2; }
.lib-fld label { display: block; }
.lib-fld input, .lib-fld select, .lib-fld textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  font-family: inherit;
  font-size: var(--insp-text-md);
  background: var(--insp-bg-surface);
  color: var(--insp-ink-primary);
  transition: border-color var(--insp-t-fast);
}
.lib-fld input:focus, .lib-fld select:focus, .lib-fld textarea:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}
.lib-fld input:disabled {
  background: var(--insp-bg-subtle);
  color: var(--insp-ink-tertiary);
}
.lib-fld textarea { resize: vertical; }

.lib-checkbox {
  display: flex; align-items: center; gap: var(--insp-sp-2);
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-secondary);
  cursor: pointer;
}
.lib-checkbox input { accent-color: var(--insp-accent); }

/* ─ Responsive ─────── */
@media (max-width: 1100px) {
  .lib-row { grid-template-columns: 44px 1fr 90px 200px; }
  .row-desc { display: none; }
}
@media (max-width: 800px) {
  .lib-archive { padding: 20px 16px 64px; }
  .page-title { font-size: 32px; }
  .lib-row { grid-template-columns: 44px 1fr; gap: var(--insp-sp-3); }
  .row-usage, .row-actions { grid-column: 2; align-items: flex-start; }
  .form-grid { grid-template-columns: 1fr; }
  .lib-fld--full { grid-column: span 1; }
}
</style>
