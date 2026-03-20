<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
    <!-- 遮罩 -->
    <div class="absolute inset-0 bg-black/50" @click="close"></div>

    <!-- 对话框 -->
    <div class="relative w-full max-w-4xl rounded-lg bg-white shadow-xl">
      <!-- 标题 -->
      <div class="flex items-center justify-between border-b px-6 py-4">
        <h3 class="text-lg font-semibold">创建任务</h3>
        <button class="text-gray-400 hover:text-gray-600" @click="close">
          <XMarkIcon class="h-5 w-5" />
        </button>
      </div>

      <!-- 表单 -->
      <div class="max-h-[70vh] overflow-y-auto px-6 py-4">
        <div class="space-y-4">
          <!-- 任务标题 -->
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">任务标题 *</label>
            <input
              v-model="form.title"
              type="text"
              placeholder="请输入任务标题"
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
            />
          </div>

          <!-- 任务描述 -->
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">任务描述</label>
            <textarea
              v-model="form.description"
              rows="3"
              placeholder="请输入任务描述"
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
            ></textarea>
          </div>

          <!-- 优先级和截止时间 -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="mb-1 block text-sm font-medium text-gray-700">优先级</label>
              <select
                v-model="form.priority"
                class="w-full rounded-md border border-gray-300 px-3 py-2"
              >
                <option :value="1">紧急</option>
                <option :value="2">普通</option>
                <option :value="3">低</option>
              </select>
            </div>
            <div>
              <label class="mb-1 block text-sm font-medium text-gray-700">截止时间</label>
              <input
                v-model="form.dueDate"
                type="datetime-local"
                class="w-full rounded-md border border-gray-300 px-3 py-2"
              />
            </div>
          </div>

          <!-- 分配方式 -->
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">分配方式 *</label>
            <div class="flex gap-4">
              <label class="flex items-center">
                <input
                  v-model="form.assignType"
                  type="radio"
                  :value="1"
                  class="mr-2"
                />
                <span class="text-sm">指定个人</span>
              </label>
              <label class="flex items-center">
                <input
                  v-model="form.assignType"
                  type="radio"
                  :value="2"
                  class="mr-2"
                />
                <span class="text-sm">批量分配</span>
              </label>
            </div>
          </div>

          <!-- 执行人选择 -->
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">执行人 *</label>
            <UserSelector
              v-if="form.assignType === 1"
              v-model="singleAssignee"
              :multiple="false"
              @change="handleSingleAssigneeChange"
            />
            <UserSelector
              v-else
              v-model="multipleAssignees"
              :multiple="true"
              @change="handleMultipleAssigneesChange"
            />
            <!-- 批量分配提示 -->
            <div v-if="form.assignType === 2 && form.targetIds && form.targetIds.length > 0" class="mt-2 flex items-center gap-1 text-sm text-blue-600">
              <InformationCircleIcon class="h-4 w-4" />
              <span>将创建 {{ form.targetIds.length }} 个独立任务，每人一个</span>
            </div>
          </div>

          <!-- 审批流程 -->
          <div>
            <label class="mb-1 block text-sm font-medium text-gray-700">审批流程 *</label>
            <select
              v-model="form.workflowTemplateId"
              class="w-full rounded-md border border-gray-300 px-3 py-2"
            >
              <option :value="undefined">请选择审批流程</option>
              <option v-for="tpl in templateList" :key="tpl.id" :value="tpl.id">
                {{ tpl.templateName }}
                <span v-if="tpl.isDefault === 1">(默认)</span>
              </option>
            </select>
          </div>
        </div>
      </div>

      <!-- 按钮 -->
      <div class="flex justify-end gap-3 border-t px-6 py-4">
        <button
          class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="close"
        >
          取消
        </button>
        <button
          class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          :disabled="submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : '创建任务' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { XMarkIcon, InformationCircleIcon } from '@heroicons/vue/24/outline'
import { createTask, type TaskCreateRequest } from '@/api/task'
import { getEnabledTemplates, type WorkflowTemplateDTO } from '@/api/task/workflow'
import { getSimpleUserList, type SimpleUser } from '@/api/user'
import UserSelector from '@/components/common/UserSelector.vue'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

// 表单数据
const form = reactive<TaskCreateRequest>({
  title: '',
  description: '',
  priority: 2,
  assignType: 1,
  assigneeId: undefined,
  targetIds: [],
  dueDate: undefined,
  workflowTemplateId: undefined,
  attachmentIds: []
})

// 状态
const submitting = ref(false)
const templateList = ref<WorkflowTemplateDTO[]>([])
const userList = ref<SimpleUser[]>([])

// 用户选择器绑定值
const singleAssignee = ref<string | null>(null)
const multipleAssignees = ref<string[]>([])

// 处理单人选择变化
const handleSingleAssigneeChange = (users: { id: string | number; realName: string }[]) => {
  if (users.length > 0) {
    form.assigneeId = users[0].id
  } else {
    form.assigneeId = undefined
  }
}

// 处理多人选择变化
const handleMultipleAssigneesChange = (users: { id: string | number; realName: string }[]) => {
  form.targetIds = users.map(u => u.id)
}

// 加载流程模板
const loadTemplates = async () => {
  try {
    templateList.value = await getEnabledTemplates('TASK')
    // 设置默认模板
    const defaultTpl = templateList.value.find(t => t.isDefault === 1)
    if (defaultTpl) {
      form.workflowTemplateId = defaultTpl.id
    }
  } catch (error) {
    console.error('加载流程模板失败', error)
  }
}

// 加载用户列表
const loadUsers = async () => {
  try {
    userList.value = await getSimpleUserList()
  } catch (error) {
    console.error('加载用户列表失败', error)
  }
}

// 关闭对话框
const close = () => {
  emit('update:visible', false)
}

// 重置表单
const resetForm = () => {
  form.title = ''
  form.description = ''
  form.priority = 2
  form.assignType = 1
  form.assigneeId = undefined
  form.targetIds = []
  form.dueDate = undefined
  form.attachmentIds = []
  // 重置用户选择器
  singleAssignee.value = null
  multipleAssignees.value = []
  // 保留默认模板
  const defaultTpl = templateList.value.find(t => t.isDefault === 1)
  form.workflowTemplateId = defaultTpl?.id
}

// 提交
const handleSubmit = async () => {
  // 验证
  if (!form.title) {
    alert('请输入任务标题')
    return
  }
  if (form.assignType === 1 && !form.assigneeId) {
    alert('请选择执行人')
    return
  }
  if (form.assignType === 2 && (!form.targetIds || form.targetIds.length === 0)) {
    alert('请选择至少一个执行人')
    return
  }
  if (!form.workflowTemplateId) {
    alert('请选择审批流程')
    return
  }

  submitting.value = true
  try {
    await createTask(form)
    emit('success')
    close()
    resetForm()
  } catch (error) {
    console.error('创建任务失败', error)
    alert('创建任务失败')
  } finally {
    submitting.value = false
  }
}

// 监听对话框打开
watch(() => props.visible, (val) => {
  if (val) {
    loadTemplates()
    loadUsers()
  }
})

onMounted(() => {
  loadTemplates()
})
</script>
