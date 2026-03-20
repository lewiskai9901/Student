<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import { useInspPlatformStore } from '@/stores/insp/inspPlatformStore'
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

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">问题分类管理</h2>
      <el-button type="primary" @click="openCreate()">
        <Plus class="w-4 h-4 mr-1" />新建分类
      </el-button>
    </div>

    <el-card shadow="never" v-loading="loading">
      <div v-if="treeData.length === 0 && !loading" class="py-12 text-center text-gray-400 text-sm">
        暂无问题分类，点击"新建分类"添加
      </div>
      <el-tree
        v-else
        :data="treeData"
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
