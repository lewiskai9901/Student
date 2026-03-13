<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Pencil, Trash2, RefreshCw, Library,
  Tag, Package, ChevronDown, X,
} from 'lucide-vue-next'
import type { LibraryItem, CreateLibraryItemRequest, UpdateLibraryItemRequest } from '@/types/insp/template'
import type { ItemType } from '@/types/insp/enums'
import {
  getLibraryItems, getLibraryCategories,
  createLibraryItem, updateLibraryItem, deleteLibraryItem, syncLibraryItem,
} from '@/api/insp/library'

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
  <div class="lib-page">
    <!-- Header -->
    <div class="lib-header">
      <div class="flex items-center gap-3">
        <Library :size="20" class="text-blue-500" />
        <h2 class="lib-title">检查项库</h2>
        <span class="lib-count">{{ items.length }} 项</span>
      </div>
      <button class="lib-btn-primary" @click="openCreate">
        <Plus :size="14" /> 新建项目
      </button>
    </div>

    <!-- Toolbar -->
    <div class="lib-toolbar">
      <div class="lib-search">
        <Search :size="14" class="lib-search-icon" />
        <input v-model="query.keyword" placeholder="搜索编码、名称、标签..."
               @keyup.enter="loadItems" />
      </div>
      <select v-model="query.category" class="lib-select" @change="loadItems">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
      </select>
    </div>

    <!-- List -->
    <div v-if="loading" class="lib-loading">加载中...</div>
    <div v-else-if="items.length === 0" class="lib-empty">暂无库项目，点击上方"新建项目"开始</div>
    <div v-else class="lib-list">
      <div v-for="item in filteredItems" :key="item.id" class="lib-card">
        <div class="lib-card-header">
          <div class="flex items-center gap-2">
            <span class="lib-code">{{ item.itemCode }}</span>
            <span class="lib-type-badge">{{ getTypeLabel(item.itemType) }}</span>
            <span v-if="item.isStandard" class="lib-std-badge">标准</span>
          </div>
          <div class="flex items-center gap-1">
            <button class="lib-ic" title="同步到模板" @click="handleSync(item)">
              <RefreshCw :size="14" />
            </button>
            <button class="lib-ic" title="编辑" @click="openEdit(item)">
              <Pencil :size="14" />
            </button>
            <button class="lib-ic danger" title="删除" @click="handleDelete(item)">
              <Trash2 :size="14" />
            </button>
          </div>
        </div>
        <div class="lib-card-name">{{ item.itemName }}</div>
        <div v-if="item.description" class="lib-card-desc">{{ item.description }}</div>
        <div class="lib-card-footer">
          <div class="flex items-center gap-2 flex-wrap">
            <span v-if="item.category" class="lib-cat">
              <Package :size="11" /> {{ item.category }}
            </span>
            <span v-for="tag in getTagList(item.tags)" :key="tag" class="lib-tag">
              {{ tag }}
            </span>
          </div>
          <span class="lib-usage">引用 {{ item.usageCount }} 次</span>
        </div>
      </div>
    </div>

    <!-- Dialog -->
    <Transition name="lib-modal">
      <div v-if="dialogVisible" class="lib-mask" @mousedown="onMaskMouseDown" @click="onMaskClose($event, () => dialogVisible = false)">
        <div class="lib-modal">
          <div class="lib-modal-head">
            <h3>{{ dialogTitle }}</h3>
            <button class="lib-modal-close" @click="dialogVisible = false">&times;</button>
          </div>
          <div class="lib-modal-body">
            <div class="grid grid-cols-2 gap-4">
              <div class="lib-fld">
                <label>项目编码</label>
                <input v-model="form.itemCode" :disabled="!!editingId" placeholder="唯一编码" />
              </div>
              <div class="lib-fld">
                <label>项目名称</label>
                <input v-model="form.itemName" placeholder="检查项名称" />
              </div>
              <div class="lib-fld">
                <label>字段类型</label>
                <select v-model="form.itemType">
                  <option v-for="opt in itemTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div class="lib-fld">
                <label>分类</label>
                <input v-model="form.category" placeholder="如：卫生、安全、纪律" />
              </div>
              <div class="lib-fld col-span-2">
                <label>标签（逗号分隔）</label>
                <input v-model="form.tags" placeholder="日常,重点,高频" />
              </div>
              <div class="lib-fld col-span-2">
                <label>描述</label>
                <textarea v-model="form.description" rows="2" placeholder="可选描述"></textarea>
              </div>
              <div class="lib-fld col-span-2">
                <label>默认帮助内容</label>
                <textarea v-model="form.defaultHelpContent" rows="2" placeholder="检查员看到的帮助说明"></textarea>
              </div>
              <div class="flex items-center gap-2 col-span-2">
                <input v-model="form.isStandard" type="checkbox" id="lib-std" />
                <label for="lib-std" class="lib-checkbox-label">标准项（不可删除）</label>
              </div>
            </div>
          </div>
          <div class="lib-modal-foot">
            <button class="lib-btn-ghost" @click="dialogVisible = false">取消</button>
            <button class="lib-btn-primary" @click="handleSave">保存</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.lib-page { padding:24px; max-width:1200px; margin:0 auto; }
.lib-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:20px; }
.lib-title { font-size:18px; font-weight:600; color:#1e2a3a; margin:0; }
.lib-count { font-size:12px; color:#8c95a3; background:#f4f6f9; padding:2px 8px; border-radius:10px; }

.lib-toolbar { display:flex; gap:12px; margin-bottom:20px; }
.lib-search { position:relative; flex:1; max-width:400px; }
.lib-search-icon { position:absolute; left:12px; top:50%; transform:translateY(-50%); color:#b8c0cc; }
.lib-search input { width:100%; padding:8px 12px 8px 34px; border:1px solid #dce1e8; border-radius:8px; font-size:13px; outline:none; transition:border-color 0.2s, box-shadow 0.2s; }
.lib-search input:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }
.lib-select { padding:8px 12px; border:1px solid #dce1e8; border-radius:8px; font-size:13px; outline:none; background:#fff; color:#1e2a3a; min-width:140px; }
.lib-select:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }

.lib-loading, .lib-empty { text-align:center; padding:48px; color:#8c95a3; font-size:14px; }

.lib-list { display:grid; grid-template-columns:repeat(auto-fill, minmax(340px, 1fr)); gap:14px; }
.lib-card { border:1px solid #e8ecf0; border-radius:12px; padding:16px; transition:border-color 0.15s, box-shadow 0.15s; background:#fff; }
.lib-card:hover { border-color:#c5d0e0; box-shadow:0 2px 12px rgba(0,0,0,0.05); }
.lib-card-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:8px; }
.lib-code { font-size:11px; font-family:monospace; color:#8c95a3; background:#f4f6f9; padding:2px 6px; border-radius:4px; }
.lib-type-badge { font-size:11px; color:#1a6dff; background:#eef4ff; padding:2px 8px; border-radius:10px; font-weight:500; }
.lib-std-badge { font-size:10px; color:#fff; background:#67C23A; padding:1px 6px; border-radius:8px; font-weight:500; }
.lib-card-name { font-size:14px; font-weight:600; color:#1e2a3a; margin-bottom:4px; }
.lib-card-desc { font-size:12px; color:#8c95a3; margin-bottom:8px; line-height:1.5; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; }
.lib-card-footer { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.lib-cat { display:inline-flex; align-items:center; gap:3px; font-size:11px; color:#5a6474; background:#f4f6f9; padding:2px 8px; border-radius:8px; }
.lib-tag { font-size:11px; color:#1a6dff; background:#eef4ff; padding:1px 6px; border-radius:8px; }
.lib-usage { font-size:11px; color:#b8c0cc; white-space:nowrap; }

.lib-ic { background:none; border:none; padding:5px; color:#b8c0cc; cursor:pointer; border-radius:6px; display:flex; align-items:center; transition:all 0.12s; }
.lib-ic:hover { color:#1a6dff; background:#f0f4ff; }
.lib-ic.danger:hover { color:#d93025; background:#fef0ef; }

/* Buttons */
.lib-btn-primary { display:inline-flex; align-items:center; gap:5px; padding:8px 16px; background:#1a6dff; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:500; cursor:pointer; transition:background 0.15s; }
.lib-btn-primary:hover { background:#1558d6; }
.lib-btn-ghost { padding:8px 16px; background:none; border:1px solid #dce1e8; border-radius:8px; font-size:13px; color:#5a6474; cursor:pointer; transition:background 0.15s; }
.lib-btn-ghost:hover { background:#f4f6f9; }

/* Modal */
.lib-mask { position:fixed; inset:0; z-index:1000; display:flex; align-items:center; justify-content:center; background:rgba(15,23,42,0.4); backdrop-filter:blur(2px); }
.lib-modal { width:600px; max-height:85vh; background:#fff; border-radius:14px; box-shadow:0 24px 64px rgba(0,0,0,0.18); overflow-y:auto; }
.lib-modal-head { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0; }
.lib-modal-head h3 { font-size:16px; font-weight:600; color:#1e2a3a; margin:0; }
.lib-modal-close { background:none; border:none; font-size:22px; color:#b8c0cc; cursor:pointer; padding:0 4px; line-height:1; }
.lib-modal-close:hover { color:#5a6474; }
.lib-modal-body { padding:20px 24px; }
.lib-modal-foot { display:flex; justify-content:flex-end; gap:10px; padding:0 24px 20px; }

.lib-modal-enter-active { transition:all 0.2s ease-out; }
.lib-modal-leave-active { transition:all 0.15s ease-in; }
.lib-modal-enter-from { opacity:0; }
.lib-modal-enter-from .lib-modal { transform:translateY(12px) scale(0.97); }
.lib-modal-leave-to { opacity:0; }
.lib-modal-leave-to .lib-modal { transform:translateY(-8px) scale(0.98); }

/* Form fields */
.lib-fld label { display:block; font-size:12px; font-weight:500; color:#5a6474; margin-bottom:5px; }
.lib-fld input, .lib-fld select, .lib-fld textarea { width:100%; border:1px solid #dce1e8; border-radius:8px; padding:8px 12px; font-size:13px; outline:none; transition:border-color 0.2s, box-shadow 0.2s; color:#1e2a3a; background:#fff; }
.lib-fld input:focus, .lib-fld select:focus, .lib-fld textarea:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }
.lib-fld input:disabled { background:#f4f6f9; color:#8c95a3; }
.lib-fld textarea { resize:vertical; font-family:inherit; }
.lib-checkbox-label { font-size:13px; color:#5a6474; }
</style>
