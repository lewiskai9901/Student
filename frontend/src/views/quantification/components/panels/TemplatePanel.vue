<template>
  <div class="h-full flex flex-col">
    <!-- 头部 -->
    <div class="px-6 py-4 border-b border-gray-200 bg-white flex items-center justify-between">
      <div>
        <h2 class="text-lg font-semibold text-gray-900">
          {{ isNew ? '新增检查模板' : '编辑检查模板' }}
        </h2>
        <p class="text-sm text-gray-500 mt-0.5">配置模板基本信息和关联的检查类别</p>
      </div>
      <button @click="emit('cancel')" class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg">
        <X class="h-5 w-5" />
      </button>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 overflow-auto p-6">
      <!-- 基本信息 -->
      <div class="bg-white rounded-xl border border-gray-200 p-6 mb-6">
        <h3 class="text-sm font-medium text-gray-900 mb-4 flex items-center gap-2">
          <Info class="h-4 w-4 text-blue-500" />
          基本信息
        </h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              模板名称 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.templateName"
              type="text"
              placeholder="例如：宿舍日常检查"
              class="w-full h-10 px-3 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
            <textarea
              v-model="form.description"
              rows="2"
              placeholder="简要描述模板用途..."
              class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
            ></textarea>
          </div>
        </div>

        <!-- 快捷设置 -->
        <div class="mt-4 flex gap-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="isDefault" class="h-4 w-4 rounded text-amber-500" />
            <Star class="h-4 w-4 text-amber-500" />
            <span class="text-sm text-gray-700">设为默认模板</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="isEnabled" class="h-4 w-4 rounded text-green-500" />
            <CheckCircle class="h-4 w-4 text-green-500" />
            <span class="text-sm text-gray-700">启用模板</span>
          </label>
        </div>
      </div>

      <!-- 检查类别 -->
      <div class="bg-white rounded-xl border border-gray-200 p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-medium text-gray-900 flex items-center gap-2">
            <FolderOpen class="h-4 w-4 text-emerald-500" />
            检查类别
            <span class="bg-emerald-100 text-emerald-700 text-xs px-2 py-0.5 rounded-full">
              {{ form.categories.length }}
            </span>
          </h3>
        </div>

        <!-- 类别选择区域 -->
        <div class="grid grid-cols-2 gap-4">
          <!-- 可选类别 -->
          <div>
            <div class="text-xs text-gray-500 mb-2">可选类别</div>
            <div class="border border-dashed border-gray-300 rounded-lg p-3 max-h-60 overflow-auto space-y-2">
              <div
                v-for="cat in availableCategories"
                :key="cat.id"
                @click="addCategory(cat)"
                class="flex items-center gap-2 p-2 rounded-lg bg-gray-50 hover:bg-emerald-50 hover:border-emerald-200 border border-transparent cursor-pointer text-sm"
              >
                <Folder class="h-4 w-4 text-gray-400" />
                <span class="flex-1 truncate">{{ cat.categoryName }}</span>
                <Plus class="h-4 w-4 text-gray-400" />
              </div>
              <div v-if="availableCategories.length === 0" class="text-center py-4 text-xs text-gray-400">
                所有类别已添加
              </div>
            </div>
          </div>

          <!-- 已选类别 -->
          <div>
            <div class="text-xs text-gray-500 mb-2">已选类别（拖拽排序）</div>
            <div class="border border-emerald-200 bg-emerald-50/30 rounded-lg p-3 max-h-60 overflow-auto space-y-2">
              <div
                v-for="(item, index) in form.categories"
                :key="item.categoryId"
                class="flex items-center gap-2 p-2 rounded-lg bg-white border border-gray-200 text-sm"
              >
                <GripVertical class="h-4 w-4 text-gray-300 cursor-move" />
                <span class="flex-1 truncate">{{ getCategoryName(item.categoryId) }}</span>
                <select
                  v-model="item.linkType"
                  class="text-xs border border-gray-200 rounded px-1 py-0.5"
                >
                  <option :value="0">不关联</option>
                  <option :value="1">宿舍</option>
                  <option :value="2">教室</option>
                </select>
                <button
                  @click="removeCategory(index)"
                  class="p-1 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded"
                >
                  <X class="h-3.5 w-3.5" />
                </button>
              </div>
              <div v-if="form.categories.length === 0" class="text-center py-4 text-xs text-gray-400">
                请从左侧添加类别
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作 -->
    <div class="px-6 py-4 border-t border-gray-200 bg-white flex items-center justify-between">
      <div>
        <button
          v-if="!isNew"
          @click="handleDelete"
          class="px-4 py-2 text-sm text-red-600 hover:bg-red-50 rounded-lg"
        >
          删除模板
        </button>
      </div>
      <div class="flex gap-3">
        <button
          @click="emit('cancel')"
          class="px-4 py-2 text-sm text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
        >
          取消
        </button>
        <button
          @click="handleSubmit"
          :disabled="saving"
          class="px-6 py-2 text-sm text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
        >
          <Loader2 v-if="saving" class="h-4 w-4 animate-spin" />
          保存
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  X, Info, Star, CheckCircle, FolderOpen, Folder, Plus, GripVertical, Loader2
} from 'lucide-vue-next'
import {
  createCheckTemplate, updateCheckTemplate, deleteCheckTemplate
} from '@/api/checkTemplates'

const props = defineProps<{
  template: any
  isNew?: boolean
  categories: any[]
}>()

const emit = defineEmits(['saved', 'deleted', 'cancel'])

const saving = ref(false)

const form = reactive({
  templateName: '',
  description: '',
  isDefault: 0,
  status: 1,
  categories: [] as { categoryId: number; linkType: number; sortOrder: number; isRequired: number }[]
})

const isDefault = computed({
  get: () => form.isDefault === 1,
  set: (val) => { form.isDefault = val ? 1 : 0 }
})

const isEnabled = computed({
  get: () => form.status === 1,
  set: (val) => { form.status = val ? 1 : 0 }
})

// 可选类别（未添加的）
const availableCategories = computed(() => {
  const selectedIds = form.categories.map(c => c.categoryId)
  return props.categories.filter(cat => !selectedIds.includes(cat.id))
})

// 获取类别名称
const getCategoryName = (categoryId: number) => {
  const cat = props.categories.find(c => c.id === categoryId)
  return cat?.categoryName || '未知类别'
}

// 添加类别
const addCategory = (cat: any) => {
  form.categories.push({
    categoryId: cat.id,
    linkType: 0,
    sortOrder: form.categories.length + 1,
    isRequired: 1
  })
}

// 移除类别
const removeCategory = (index: number) => {
  form.categories.splice(index, 1)
  form.categories.forEach((item, i) => { item.sortOrder = i + 1 })
}

// 提交
const handleSubmit = async () => {
  if (!form.templateName.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (form.categories.length === 0) {
    ElMessage.warning('请至少添加一个检查类别')
    return
  }

  saving.value = true
  try {
    const submitData = {
      templateName: form.templateName,
      description: form.description,
      isDefault: form.isDefault,
      status: form.status,
      categories: form.categories
    }

    if (props.isNew) {
      await createCheckTemplate(submitData)
      ElMessage.success('创建成功')
    } else {
      await updateCheckTemplate(props.template.id, { ...submitData, id: props.template.id })
      ElMessage.success('更新成功')
    }
    emit('saved')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 删除
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板"${form.templateName}"吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteCheckTemplate(props.template.id)
    ElMessage.success('删除成功')
    emit('deleted')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 监听props变化，更新表单
watch(() => props.template, (val) => {
  if (val) {
    form.templateName = val.templateName || ''
    form.description = val.description || ''
    form.isDefault = val.isDefault || 0
    form.status = val.status ?? 1
    form.categories = (val.categories || []).map((c: any) => ({
      categoryId: c.categoryId,
      linkType: c.linkType || 0,
      sortOrder: c.sortOrder || 1,
      isRequired: c.isRequired ?? 1
    }))
  }
}, { immediate: true })
</script>
