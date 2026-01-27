<template>
  <div class="h-screen flex flex-col bg-gray-100">
    <!-- 顶部导航 -->
    <div class="flex items-center justify-between bg-white border-b px-6 py-3">
      <div class="flex items-center gap-4">
        <button
          class="flex items-center gap-1 text-gray-600 hover:text-gray-900"
          @click="goBack"
        >
          <ArrowLeftIcon class="h-5 w-5" />
          <span>返回</span>
        </button>
        <span class="text-gray-300">|</span>
        <div>
          <h1 class="text-lg font-semibold text-gray-900">
            {{ isEdit ? '编辑流程' : '设计流程' }}
          </h1>
          <p class="text-xs text-gray-500">{{ template?.templateName || '新流程' }}</p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <span v-if="saving" class="text-sm text-gray-500">保存中...</span>
        <span v-else-if="lastSaveTime" class="text-sm text-gray-500">
          上次保存: {{ lastSaveTime }}
        </span>
        <button
          class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          :disabled="saving"
          @click="handleSave"
        >
          保存流程
        </button>
        <button
          v-if="template && !template.processDefinitionId"
          class="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700"
          :disabled="saving"
          @click="handleDeploy"
        >
          部署流程
        </button>
      </div>
    </div>

    <!-- 设计器 -->
    <div class="flex-1 p-4">
      <BpmnDesigner
        ref="designerRef"
        :xml="bpmnXml"
        :process-id="processId"
        :process-name="template?.templateName"
        @save="onSave"
        @change="onDesignerChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'
import BpmnDesigner from '@/components/bpmn/BpmnDesigner.vue'
import {
  getWorkflowTemplateDetail,
  updateWorkflowTemplate,
  deployWorkflowTemplate,
  type WorkflowTemplateDTO
} from '@/api/task/workflow'

const route = useRoute()
const router = useRouter()

const designerRef = ref<InstanceType<typeof BpmnDesigner> | null>(null)
const template = ref<WorkflowTemplateDTO | null>(null)
const bpmnXml = ref<string>('')
const saving = ref(false)
const lastSaveTime = ref<string>('')
const hasChanges = ref(false)

// 保持ID为字符串，避免JavaScript大数字精度丢失
const templateId = computed(() => route.params.id as string)
const isEdit = computed(() => !!templateId.value)
const processId = computed(() => template.value?.templateCode || 'Process_' + Date.now())

// 加载模板
const loadTemplate = async () => {
  if (!templateId.value) return

  try {
    template.value = await getWorkflowTemplateDetail(templateId.value)
    if (template.value.bpmnXml) {
      bpmnXml.value = template.value.bpmnXml
    }
  } catch (error) {
    console.error('加载模板失败:', error)
    alert('加载模板失败')
  }
}

// 返回
const goBack = () => {
  if (hasChanges.value) {
    if (!confirm('有未保存的更改，确定要离开吗？')) {
      return
    }
  }
  router.push('/task/workflow')
}

// 设计器变化
const onDesignerChange = () => {
  hasChanges.value = true
}

// 保存流程
const onSave = async (xml: string) => {
  if (!template.value) return

  saving.value = true
  try {
    await updateWorkflowTemplate(template.value.id, {
      ...template.value,
      bpmnXml: xml
    })
    hasChanges.value = false
    lastSaveTime.value = new Date().toLocaleTimeString()
    alert('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
    alert('保存失败')
  } finally {
    saving.value = false
  }
}

// 手动保存
const handleSave = async () => {
  const xml = await designerRef.value?.getXml()
  if (xml) {
    await onSave(xml)
  }
}

// 部署流程
const handleDeploy = async () => {
  if (!template.value) return

  // 先保存
  const xml = await designerRef.value?.getXml()
  if (xml) {
    saving.value = true
    try {
      await updateWorkflowTemplate(template.value.id, {
        ...template.value,
        bpmnXml: xml
      })

      // 再部署
      await deployWorkflowTemplate(template.value.id)
      hasChanges.value = false
      alert('部署成功')
      router.push('/task/workflow')
    } catch (error) {
      console.error('部署失败:', error)
      alert('部署失败')
    } finally {
      saving.value = false
    }
  }
}

onMounted(() => {
  loadTemplate()
})
</script>
