<template>
  <div class="p-6 bg-gray-50 min-h-full">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">组织类型配置</h1>
      <p class="mt-1 text-sm text-gray-500">配置组织层级类型（学校、院系、系部等）</p>
    </div>

    <!-- 操作栏 -->
    <div class="mb-4 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <button
          @click="handleAdd"
          class="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          <Plus class="h-4 w-4" />
          新增类型
        </button>
        <button
          @click="loadData"
          class="inline-flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          刷新
        </button>
      </div>
      <div class="text-sm text-gray-500">
        共 {{ orgTypes.length }} 个类型
      </div>
    </div>

    <!-- 类型列表 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">类型编码</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">类型名称</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">父类型</th>
            <th class="px-4 py-3 text-center text-xs font-medium uppercase text-gray-500">层级</th>
            <th class="px-4 py-3 text-center text-xs font-medium uppercase text-gray-500">类别</th>
            <th class="px-4 py-3 text-center text-xs font-medium uppercase text-gray-500">特性</th>
            <th class="px-4 py-3 text-center text-xs font-medium uppercase text-gray-500">状态</th>
            <th class="px-4 py-3 text-center text-xs font-medium uppercase text-gray-500">系统</th>
            <th class="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody v-if="loading">
          <tr>
            <td colspan="9" class="py-12 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-600" />
              <div class="mt-2 text-sm text-gray-500">加载中...</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="orgTypes.length === 0">
          <tr>
            <td colspan="9" class="py-12 text-center">
              <Layers class="mx-auto h-12 w-12 text-gray-300" />
              <div class="mt-2 text-sm text-gray-500">暂无类型数据</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else class="divide-y divide-gray-100">
          <tr v-for="item in orgTypes" :key="item.id" class="hover:bg-gray-50">
            <td class="px-4 py-3">
              <span class="rounded bg-gray-100 px-2 py-0.5 font-mono text-xs text-gray-600">
                {{ item.typeCode }}
              </span>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-2">
                <div
                  class="flex h-8 w-8 items-center justify-center rounded-lg"
                  :style="{ backgroundColor: item.color + '20' }"
                >
                  <component
                    :is="getIconComponent(item.icon)"
                    class="h-4 w-4"
                    :style="{ color: item.color }"
                  />
                </div>
                <span class="font-medium text-gray-900">{{ item.typeName }}</span>
              </div>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">
              {{ item.parentTypeCode || '-' }}
            </td>
            <td class="px-4 py-3 text-center">
              <span class="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
                L{{ item.levelOrder }}
              </span>
            </td>
            <td class="px-4 py-3 text-center">
              <span
                :class="item.isAcademic ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700'"
                class="rounded px-2 py-0.5 text-xs font-medium"
              >
                {{ item.isAcademic ? '教学单位' : '职能部门' }}
              </span>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-1">
                <span
                  v-if="item.canHaveChildren"
                  class="rounded bg-green-100 px-1.5 py-0.5 text-xs text-green-700"
                  title="可有子级"
                >子级</span>
                <span
                  v-if="item.canBeInspected"
                  class="rounded bg-purple-100 px-1.5 py-0.5 text-xs text-purple-700"
                  title="可被检查"
                >检查</span>
              </div>
            </td>
            <td class="px-4 py-3 text-center">
              <button
                @click="handleToggleStatus(item)"
                :disabled="item.isSystem"
                :class="[
                  'relative inline-flex h-5 w-9 items-center rounded-full transition-colors',
                  item.isEnabled ? 'bg-blue-600' : 'bg-gray-300',
                  item.isSystem ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'
                ]"
              >
                <span
                  :class="[
                    'inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform',
                    item.isEnabled ? 'translate-x-4' : 'translate-x-0.5'
                  ]"
                />
              </button>
            </td>
            <td class="px-4 py-3 text-center">
              <span
                v-if="item.isSystem"
                class="rounded bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700"
              >预置</span>
              <span v-else class="text-gray-400">-</span>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-end gap-1">
                <button
                  @click="handleEdit(item)"
                  class="rounded p-1.5 text-gray-500 hover:bg-blue-50 hover:text-blue-600"
                  title="编辑"
                >
                  <Pencil class="h-4 w-4" />
                </button>
                <button
                  v-if="!item.isSystem"
                  @click="handleDelete(item)"
                  class="rounded p-1.5 text-gray-500 hover:bg-red-50 hover:text-red-600"
                  title="删除"
                >
                  <Trash2 class="h-4 w-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 编辑对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="dialogVisible = false"></div>
          <div class="relative w-full max-w-xl rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">{{ isEdit ? '编辑组织类型' : '新增组织类型' }}</h3>
              <button @click="dialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="max-h-[60vh] overflow-y-auto p-6">
              <div class="space-y-4">
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">
                      类型编码 <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="formData.typeCode"
                      :disabled="isEdit"
                      type="text"
                      placeholder="如：DEPARTMENT"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none disabled:bg-gray-100"
                    />
                  </div>
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">
                      类型名称 <span class="text-red-500">*</span>
                    </label>
                    <input
                      v-model="formData.typeName"
                      type="text"
                      placeholder="如：系部"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    />
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">父类型</label>
                    <select
                      v-model="formData.parentTypeCode"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    >
                      <option value="">顶级类型</option>
                      <option v-for="type in parentTypeOptions" :key="type.typeCode" :value="type.typeCode">
                        {{ type.typeName }} ({{ type.typeCode }})
                      </option>
                    </select>
                  </div>
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">层级顺序</label>
                    <input
                      v-model.number="formData.levelOrder"
                      type="number"
                      min="1"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    />
                    <p class="mt-1 text-xs text-gray-400">1=一级(学校)，2=二级(系部)...</p>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">图标</label>
                    <input
                      v-model="formData.icon"
                      type="text"
                      placeholder="如：Building"
                      class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                    />
                  </div>
                  <div>
                    <label class="mb-1 block text-sm text-gray-700">颜色</label>
                    <input
                      v-model="formData.color"
                      type="color"
                      class="h-9 w-full rounded-lg border border-gray-300 px-1"
                    />
                  </div>
                </div>

                <div>
                  <label class="mb-1 block text-sm text-gray-700">描述</label>
                  <textarea
                    v-model="formData.description"
                    rows="2"
                    placeholder="类型描述..."
                    class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                  ></textarea>
                </div>

                <div>
                  <label class="mb-2 block text-sm font-medium text-gray-700">类别</label>
                  <div class="flex gap-4">
                    <label class="flex items-center gap-2">
                      <input v-model="formData.isAcademic" :value="true" type="radio" name="category" class="text-blue-600" />
                      <span class="text-sm text-gray-700">教学单位</span>
                    </label>
                    <label class="flex items-center gap-2">
                      <input v-model="formData.isAcademic" :value="false" type="radio" name="category" class="text-blue-600" />
                      <span class="text-sm text-gray-700">职能部门</span>
                    </label>
                  </div>
                </div>

                <div>
                  <label class="mb-2 block text-sm font-medium text-gray-700">特性配置</label>
                  <div class="grid grid-cols-2 gap-3">
                    <label class="flex items-center gap-2">
                      <input v-model="formData.canHaveChildren" type="checkbox" class="rounded border-gray-300" />
                      <span class="text-sm text-gray-700">可有子级</span>
                    </label>
                    <label class="flex items-center gap-2">
                      <input v-model="formData.canBeInspected" type="checkbox" class="rounded border-gray-300" />
                      <span class="text-sm text-gray-700">可被检查</span>
                    </label>
                  </div>
                </div>
              </div>
            </div>
            <div class="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
              <button
                @click="dialogVisible = false"
                class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                @click="handleSubmit"
                :disabled="submitLoading"
                class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, RefreshCw, Pencil, Trash2, X, Loader2, Layers,
  Building, Briefcase, BookOpen, Users, Settings, FileText, School, UserCheck, Truck, DollarSign
} from 'lucide-vue-next'
import { orgTypeApi } from '@/api/orgType'
import type { OrgType, CreateOrgTypeRequest, UpdateOrgTypeRequest } from '@/types/orgType'

const loading = ref(false)
const submitLoading = ref(false)
const orgTypes = ref<OrgType[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref<number>()

const formData = reactive<CreateOrgTypeRequest>({
  typeCode: '',
  typeName: '',
  parentTypeCode: '',
  levelOrder: 1,
  icon: '',
  color: '#1890ff',
  description: '',
  isAcademic: true,
  canBeInspected: true,
  canHaveChildren: true,
  sortOrder: 0
})

// 父类型选项（排除当前编辑项，且必须允许子级）
const parentTypeOptions = computed(() => {
  return orgTypes.value.filter(t =>
    (!isEdit.value || t.id !== currentId.value) && t.canHaveChildren
  )
})

// 图标组件映射
const iconComponents: Record<string, any> = {
  School, Building, Briefcase, BookOpen, Users, Settings, FileText, UserCheck, Truck, DollarSign
}

const getIconComponent = (iconName: string | null) => {
  return iconComponents[iconName || 'Building'] || Building
}

const loadData = async () => {
  loading.value = true
  try {
    orgTypes.value = await orgTypeApi.getAll()
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  currentId.value = undefined
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (item: OrgType) => {
  isEdit.value = true
  currentId.value = item.id
  Object.assign(formData, {
    typeCode: item.typeCode,
    typeName: item.typeName,
    parentTypeCode: item.parentTypeCode || '',
    levelOrder: item.levelOrder,
    icon: item.icon || '',
    color: item.color || '#1890ff',
    description: item.description || '',
    isAcademic: item.isAcademic,
    canBeInspected: item.canBeInspected,
    canHaveChildren: item.canHaveChildren,
    sortOrder: item.sortOrder
  })
  dialogVisible.value = true
}

const handleDelete = async (item: OrgType) => {
  try {
    await ElMessageBox.confirm(
      `确定删除类型"${item.typeName}"吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await orgTypeApi.delete(item.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleToggleStatus = async (item: OrgType) => {
  if (item.isSystem) return
  try {
    if (item.isEnabled) {
      await orgTypeApi.disable(item.id)
    } else {
      await orgTypeApi.enable(item.id)
    }
    item.isEnabled = !item.isEnabled
    ElMessage.success('状态已更新')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleSubmit = async () => {
  if (!formData.typeCode || !formData.typeName) {
    ElMessage.error('请填写必填项')
    return
  }

  submitLoading.value = true
  try {
    if (isEdit.value && currentId.value) {
      const updateData: UpdateOrgTypeRequest = {
        typeName: formData.typeName,
        icon: formData.icon,
        color: formData.color,
        description: formData.description,
        isAcademic: formData.isAcademic,
        canBeInspected: formData.canBeInspected,
        canHaveChildren: formData.canHaveChildren,
        sortOrder: formData.sortOrder
      }
      await orgTypeApi.update(currentId.value, updateData)
      ElMessage.success('更新成功')
    } else {
      await orgTypeApi.create(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const resetForm = () => {
  Object.assign(formData, {
    typeCode: '',
    typeName: '',
    parentTypeCode: '',
    levelOrder: 1,
    icon: '',
    color: '#1890ff',
    description: '',
    isAcademic: true,
    canBeInspected: true,
    canHaveChildren: true,
    sortOrder: 0
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
