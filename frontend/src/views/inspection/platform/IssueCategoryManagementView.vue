<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2, Search } from 'lucide-vue-next'
import { useInspPlatformStore } from '@/stores/inspection/inspPlatformStore'
import type { IssueCategory } from '@/types/insp/platform'

const store = useInspPlatformStore()

const loading = ref(false)
const treeData = ref<IssueCategory[]>([])
const showDialog = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  categoryCode: '',
  categoryName: '',
  parentId: undefined as number | undefined,
  icon: '',
  sortOrder: 0,
})

const treeProps = {
  label: 'categoryName',
  children: 'children',
}

async function loadData() {
  loading.value = true
  try {
    await store.fetchIssueCategories()
    treeData.value = store.issueCategories
  } catch (e: any) {
    ElMessage.error(e.message || '加载问题分类失败')
  } finally {
    loading.value = false
  }
}

function openCreate(parentId?: number) {
  editingId.value = null
  form.value = {
    categoryCode: '',
    categoryName: '',
    parentId: parentId,
    icon: '',
    sortOrder: 0,
  }
  showDialog.value = true
}

function openEdit(node: IssueCategory) {
  editingId.value = node.id
  form.value = {
    categoryCode: node.categoryCode,
    categoryName: node.categoryName,
    parentId: node.parentId,
    icon: node.icon ?? '',
    sortOrder: node.sortOrder,
  }
  showDialog.value = true
}

async function handleSave() {
  if (!form.value.categoryCode || !form.value.categoryName) {
    ElMessage.warning('请填写分类编码和名称')
    return
  }
  try {
    if (editingId.value) {
      await store.updateIssueCategory(editingId.value, {
        categoryCode: form.value.categoryCode,
        categoryName: form.value.categoryName,
        parentId: form.value.parentId,
        icon: form.value.icon || undefined,
        sortOrder: form.value.sortOrder,
      })
      ElMessage.success('更新成功')
    } else {
      await store.createIssueCategory({
        categoryCode: form.value.categoryCode,
        categoryName: form.value.categoryName,
        parentId: form.value.parentId,
        icon: form.value.icon || undefined,
        sortOrder: form.value.sortOrder,
        isEnabled: true,
      })
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(node: IssueCategory) {
  try {
    await ElMessageBox.confirm(
      `确认删除分类「${node.categoryName}」？${node.children?.length ? '其子分类也将被删除。' : ''}`,
      '确认删除',
      { type: 'warning' }
    )
    await store.deleteIssueCategory(node.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

// Flatten tree for parent selection in dialog
function flattenCategories(nodes: IssueCategory[], level = 0): { id: number; label: string }[] {
  const result: { id: number; label: string }[] = []
  for (const node of nodes) {
    const prefix = '\u00A0\u00A0'.repeat(level)
    result.push({ id: node.id, label: `${prefix}${node.categoryName}` })
    if (node.children?.length) {
      result.push(...flattenCategories(node.children, level + 1))
    }
  }
  return result
}

// ============== S+ 设计样板 ==============
const searchKw = ref('')
function filterTree(nodes: IssueCategory[]): IssueCategory[] {
  const q = searchKw.value.trim().toLowerCase()
  if (!q) return nodes
  return nodes.reduce<IssueCategory[]>((acc, n) => {
    const selfMatch = n.categoryName.toLowerCase().includes(q) || n.categoryCode.toLowerCase().includes(q)
    const childrenFiltered = n.children ? filterTree(n.children) : []
    if (selfMatch || childrenFiltered.length > 0) {
      acc.push({ ...n, children: childrenFiltered.length > 0 ? childrenFiltered : n.children })
    }
    return acc
  }, [])
}
const filteredTreeData = computed(() => filterTree(treeData.value))

const stats = computed(() => {
  function flatten(nodes: IssueCategory[]): IssueCategory[] {
    const out: IssueCategory[] = []
    for (const n of nodes) { out.push(n); if (n.children) out.push(...flatten(n.children)) }
    return out
  }
  const all = flatten(treeData.value)
  return {
    total: all.length,
    enabled: all.filter(n => n.isEnabled).length,
    disabled: all.filter(n => !n.isEnabled).length,
    topLevel: treeData.value.length,
  }
})

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">问题分类管理</h2>
      <div class="flex items-center gap-3 ic-stats">
        <span class="ic-stat"><span class="ic-stat__num">{{ stats.total }}</span><span class="ic-stat__label">分类总数</span></span>
        <span class="ic-stat-rule" />
        <span class="ic-stat"><span class="ic-stat__num">{{ stats.topLevel }}</span><span class="ic-stat__label">顶级</span></span>
        <span class="ic-stat-rule" />
        <span class="ic-stat"><span class="ic-stat__num" :style="{ color: stats.enabled > 0 ? '#10b981' : '' }">{{ stats.enabled }}</span><span class="ic-stat__label">启用</span></span>
        <el-button type="primary" @click="openCreate()" class="ml-3">
          <Plus class="w-4 h-4 mr-1" />新建分类
        </el-button>
      </div>
    </div>

    <div class="ic-search-bar">
      <Search class="w-4 h-4 ic-search-icon" />
      <input v-model="searchKw" placeholder="搜索分类编码 / 名称..." class="ic-search-input" />
    </div>

    <el-card shadow="never" v-loading="loading">
      <div v-if="treeData.length === 0 && !loading" class="py-12 text-center text-gray-400 text-sm">
        暂无问题分类，点击"新建分类"添加
      </div>
      <div v-else-if="filteredTreeData.length === 0" class="py-12 text-center text-gray-400 text-sm">
        没有匹配 "{{ searchKw }}" 的分类
      </div>
      <el-tree
        v-else
        :data="filteredTreeData"
        :props="treeProps"
        node-key="id"
        default-expand-all
        :expand-on-click-node="false"
      >
        <template #default="{ node, data }">
          <div class="flex items-center justify-between w-full py-1 group">
            <div class="flex items-center gap-2">
              <span v-if="data.icon" class="text-base">{{ data.icon }}</span>
              <span class="text-sm">{{ data.categoryName }}</span>
              <span class="text-xs text-gray-400">({{ data.categoryCode }})</span>
              <el-tag v-if="!data.isEnabled" type="info" size="small">已停用</el-tag>
            </div>
            <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition">
              <el-button link size="small" type="primary" @click.stop="openCreate(data.id)">
                <Plus class="w-3.5 h-3.5" />
              </el-button>
              <el-button link size="small" type="primary" @click.stop="openEdit(data)">
                <Pencil class="w-3.5 h-3.5" />
              </el-button>
              <el-button link size="small" type="danger" @click.stop="handleDelete(data)">
                <Trash2 class="w-3.5 h-3.5" />
              </el-button>
            </div>
          </div>
        </template>
      </el-tree>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="showDialog" :title="editingId ? '编辑分类' : '新建分类'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="分类编码" required>
          <el-input v-model="form.categoryCode" placeholder="如 HYGIENE, SAFETY" />
        </el-form-item>
        <el-form-item label="分类名称" required>
          <el-input v-model="form.categoryName" placeholder="输入分类名称" />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" placeholder="无上级（顶级）" clearable class="w-full">
            <el-option
              v-for="cat in flattenCategories(treeData)"
              :key="cat.id"
              :label="cat.label"
              :value="cat.id"
              :disabled="cat.id === editingId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Emoji 或图标名称（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ic-stats { display: flex; align-items: center; gap: 12px; }
.ic-stat { display: inline-flex; flex-direction: column; align-items: center; min-width: 56px; padding: 2px 8px; }
.ic-stat__num { font-family: var(--insp-font-mono, monospace); font-size: 16px; font-weight: 700; color: var(--insp-ink-primary, #1f2937); line-height: 1; }
.ic-stat__label { font-size: 10px; color: var(--insp-ink-tertiary, #999); margin-top: 2px; letter-spacing: 0.04em; }
.ic-stat-rule { width: 1px; height: 18px; background: var(--insp-border-subtle, #eee); }

.ic-search-bar {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-default, #e5e7eb);
  border-radius: 6px;
  max-width: 320px;
}
.ic-search-icon { color: var(--insp-ink-tertiary, #999); }
.ic-search-input {
  flex: 1;
  height: 30px;
  border: 0;
  background: transparent;
  font-size: 12px;
  font-family: inherit;
  outline: none;
  color: var(--insp-ink-primary, #1f2937);
}
</style>
