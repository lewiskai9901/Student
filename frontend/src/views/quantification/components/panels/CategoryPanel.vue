<template>
  <div class="h-full flex flex-col">
    <!-- 头部 -->
    <div class="px-6 py-4 border-b border-gray-200 bg-white flex items-center justify-between">
      <div>
        <h2 class="text-lg font-semibold text-gray-900">
          {{ isNew ? '新增检查类别' : '编辑检查类别' }}
        </h2>
        <p class="text-sm text-gray-500 mt-0.5">配置检查类别的基本信息</p>
      </div>
      <button @click="emit('cancel')" class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg">
        <X class="h-5 w-5" />
      </button>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 overflow-auto p-6">
      <div class="bg-white rounded-xl border border-gray-200 p-6 max-w-2xl">
        <h3 class="text-sm font-medium text-gray-900 mb-4 flex items-center gap-2">
          <FolderOpen class="h-4 w-4 text-blue-500" />
          类别信息
        </h3>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              类别名称 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.categoryName"
              type="text"
              placeholder="例如：卫生检查"
              class="w-full h-10 px-3 border border-gray-300 rounded-lg text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              类别编码 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.categoryCode"
              type="text"
              placeholder="例如：hygiene"
              class="w-full h-10 px-3 border border-gray-300 rounded-lg text-sm font-mono focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
            />
            <p class="mt-1 text-xs text-gray-400">用于系统识别，建议使用英文字母</p>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">状态</label>
            <div class="flex gap-4">
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="form.status" :value="1" class="h-4 w-4 text-blue-600" />
                <span class="text-sm text-gray-700">启用</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="form.status" :value="0" class="h-4 w-4 text-blue-600" />
                <span class="text-sm text-gray-700">禁用</span>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- 提示信息 -->
      <div class="mt-6 bg-blue-50 border border-blue-200 rounded-xl p-4 max-w-2xl">
        <div class="flex gap-3">
          <Info class="h-5 w-5 text-blue-500 flex-shrink-0 mt-0.5" />
          <div class="text-sm text-blue-700">
            <p class="font-medium mb-1">使用说明</p>
            <ul class="list-disc list-inside text-blue-600 space-y-1">
              <li>检查类别是扣分项的分组容器</li>
              <li>创建类别后，可在该类别下添加扣分项</li>
              <li>模板可以引用多个检查类别</li>
            </ul>
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
          删除类别
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
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { X, FolderOpen, Info, Loader2 } from 'lucide-vue-next'
import { createCategory, updateCategory, deleteCategory } from '@/api/quantification'

const props = defineProps<{
  category: any
  isNew?: boolean
}>()

const emit = defineEmits(['saved', 'deleted', 'cancel'])

const saving = ref(false)

const form = reactive({
  categoryName: '',
  categoryCode: '',
  status: 1
})

// 提交
const handleSubmit = async () => {
  if (!form.categoryName.trim()) {
    ElMessage.warning('请输入类别名称')
    return
  }
  if (!form.categoryCode.trim()) {
    ElMessage.warning('请输入类别编码')
    return
  }

  saving.value = true
  try {
    const submitData = {
      categoryName: form.categoryName,
      categoryCode: form.categoryCode,
      status: form.status
    }

    if (props.isNew) {
      await createCategory(submitData)
      ElMessage.success('创建成功')
    } else {
      await updateCategory(props.category.id, submitData)
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
      `确定要删除类别"${form.categoryName}"吗？删除后该类别下的扣分项也将被删除。`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteCategory(props.category.id)
    ElMessage.success('删除成功')
    emit('deleted')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 监听props变化，更新表单
watch(() => props.category, (val) => {
  if (val) {
    form.categoryName = val.categoryName || val.typeName || ''
    form.categoryCode = val.categoryCode || val.typeCode || ''
    form.status = val.status ?? val.isActive ?? 1
  }
}, { immediate: true })
</script>
