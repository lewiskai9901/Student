<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Trash2, GripVertical } from 'lucide-vue-next'
import { templateModuleRefApi } from '@/api/insp/templateModuleRef'
import { inspTemplateApi } from '@/api/insp/template'
import { TargetTypeConfig, TemplateStatusConfig } from '@/types/insp/enums'
import type { TemplateModuleRef, InspTemplate } from '@/types/insp/template'

const props = defineProps<{
  templateId: number
  readonly?: boolean
}>()

const refs = ref<TemplateModuleRef[]>([])
const loading = ref(false)
const showPicker = ref(false)
const availableTemplates = ref<InspTemplate[]>([])
const pickerLoading = ref(false)

// Module template name cache
const templateNames = ref<Map<number, InspTemplate>>(new Map())

const enrichedRefs = computed(() =>
  refs.value.map(r => ({
    ...r,
    moduleName: templateNames.value.get(r.moduleTemplateId)?.templateName || `模板 #${r.moduleTemplateId}`,
    moduleTargetType: templateNames.value.get(r.moduleTemplateId)?.targetType || 'ORG',
    moduleStatus: templateNames.value.get(r.moduleTemplateId)?.status || 'DRAFT',
  }))
)

const totalWeight = computed(() => refs.value.reduce((sum, r) => sum + (r.weight || 100), 0))

async function loadRefs() {
  loading.value = true
  try {
    refs.value = await templateModuleRefApi.list(props.templateId)
    // Load template names for display
    for (const r of refs.value) {
      if (!templateNames.value.has(r.moduleTemplateId)) {
        try {
          const tpl = await inspTemplateApi.getById(r.moduleTemplateId)
          templateNames.value.set(r.moduleTemplateId, tpl)
        } catch { /* ignore */ }
      }
    }
  } finally {
    loading.value = false
  }
}

async function openPicker() {
  showPicker.value = true
  pickerLoading.value = true
  try {
    const result = await inspTemplateApi.getList({ page: 1, size: 100 })
    // Filter out self and already-referenced templates
    const existingIds = new Set(refs.value.map(r => Number(r.moduleTemplateId)))
    existingIds.add(Number(props.templateId))
    availableTemplates.value = result.records.filter(t => !existingIds.has(Number(t.id)))
  } finally {
    pickerLoading.value = false
  }
}

async function addRef(moduleTemplate: InspTemplate) {
  try {
    const newRef = await templateModuleRefApi.add(props.templateId, {
      moduleTemplateId: moduleTemplate.id,
      sortOrder: refs.value.length,
      weight: 100,
    })
    refs.value.push(newRef)
    templateNames.value.set(moduleTemplate.id, moduleTemplate)
    showPicker.value = false
    ElMessage.success(`已添加「${moduleTemplate.templateName}」`)
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

async function updateWeight(refItem: TemplateModuleRef, newWeight: number) {
  try {
    await templateModuleRefApi.update(props.templateId, refItem.id, { weight: newWeight })
    const original = refs.value.find(r => r.id === refItem.id)
    if (original) original.weight = newWeight
  } catch (e: any) {
    ElMessage.error(e.message || '更新权重失败')
  }
}

async function removeRef(refItem: TemplateModuleRef) {
  try {
    await ElMessageBox.confirm('确认移除此子模板引用？', '确认', { type: 'warning' })
    await templateModuleRefApi.remove(props.templateId, refItem.id)
    refs.value = refs.value.filter(r => r.id !== refItem.id)
    ElMessage.success('已移除')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '移除失败')
  }
}

onMounted(loadRefs)
</script>

<template>
  <div class="mr-panel">
    <div class="mr-header">
      <h3 class="mr-title">子模板引用</h3>
      <button v-if="!readonly" class="mr-add-btn" @click="openPicker">
        <Plus :size="13" /> 添加子模板
      </button>
    </div>

    <p class="mr-desc">
      引用其他模板作为子模板，创建项目时将自动展开为子项目。每个子模板可设置权重用于分数汇总。
    </p>

    <!-- Weight bar -->
    <div v-if="refs.length > 0" class="mr-weight-bar">
      <div class="mr-weight-track">
        <div class="mr-weight-fill" :style="{ width: Math.min(totalWeight, 100) + '%' }" :class="{ over: totalWeight > 100 }" />
      </div>
      <span class="mr-weight-label" :class="{ warn: totalWeight !== 100 }">
        权重合计: {{ totalWeight }}%
      </span>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="mr-empty">加载中...</div>

    <!-- Empty -->
    <div v-else-if="refs.length === 0" class="mr-empty">暂无子模板引用</div>

    <!-- List -->
    <div v-else class="mr-list">
      <div v-for="item in enrichedRefs" :key="item.id" class="mr-item">
        <div class="mr-item-left">
          <GripVertical :size="12" class="mr-grip" />
          <div class="mr-item-info">
            <span class="mr-item-name">{{ item.moduleName }}</span>
            <span class="mr-item-meta">
              <span class="mr-target-tag">{{ TargetTypeConfig[item.moduleTargetType]?.label }}</span>
              <span class="mr-status-dot" :style="{ background: TemplateStatusConfig[item.moduleStatus]?.color }" />
              {{ TemplateStatusConfig[item.moduleStatus]?.label }}
            </span>
          </div>
        </div>
        <div class="mr-item-right">
          <div class="mr-weight-input">
            <input
              type="number"
              :value="item.weight"
              :disabled="readonly"
              min="0"
              max="100"
              @change="updateWeight(item, Number(($event.target as HTMLInputElement).value))"
            />
            <span class="mr-weight-unit">%</span>
          </div>
          <button v-if="!readonly" class="mr-remove" @click="removeRef(item)">
            <Trash2 :size="13" />
          </button>
        </div>
      </div>
    </div>

    <!-- Template picker dialog -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showPicker" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="showPicker = false">
          <div class="mr-picker">
            <div class="mr-picker-head">
              <h3>选择子模板</h3>
              <button @click="showPicker = false">&times;</button>
            </div>
            <div v-if="pickerLoading" class="mr-picker-loading">加载中...</div>
            <div v-else-if="availableTemplates.length === 0" class="mr-picker-empty">没有可添加的模板</div>
            <div v-else class="mr-picker-list">
              <div
                v-for="tpl in availableTemplates"
                :key="tpl.id"
                class="mr-picker-item"
                @click="addRef(tpl)"
              >
                <div class="mr-picker-name">{{ tpl.templateName }}</div>
                <div class="mr-picker-meta">
                  {{ tpl.templateCode }} ·
                  <span class="mr-target-tag">{{ TargetTypeConfig[tpl.targetType]?.label }}</span> ·
                  {{ TemplateStatusConfig[tpl.status]?.label }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.mr-panel { padding: 16px; }
.mr-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.mr-title { font-size: 13px; font-weight: 600; color: #1e2a3a; margin: 0; }
.mr-add-btn { display: inline-flex; align-items: center; gap: 4px; padding: 5px 12px; background: #1a6dff; color: #fff; border: none; border-radius: 6px; font-size: 12px; cursor: pointer; }
.mr-add-btn:hover { background: #1558d6; }
.mr-desc { font-size: 11px; color: #8c95a3; line-height: 1.5; margin-bottom: 12px; }

.mr-weight-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.mr-weight-track { flex: 1; height: 6px; background: #e8ecf0; border-radius: 3px; overflow: hidden; }
.mr-weight-fill { height: 100%; background: #34d399; border-radius: 3px; transition: width 0.3s; }
.mr-weight-fill.over { background: #ef4444; }
.mr-weight-label { font-size: 11px; color: #5a6474; white-space: nowrap; }
.mr-weight-label.warn { color: #d97706; font-weight: 500; }

.mr-empty { padding: 32px 0; text-align: center; font-size: 12px; color: #8c95a3; }

.mr-list { display: flex; flex-direction: column; gap: 4px; }
.mr-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border: 1px solid #e8ecf0; border-radius: 8px; transition: border-color 0.15s; }
.mr-item:hover { border-color: #93c5fd; }
.mr-item-left { display: flex; align-items: center; gap: 8px; overflow: hidden; }
.mr-grip { color: #d1d5db; flex-shrink: 0; cursor: grab; }
.mr-item-info { min-width: 0; }
.mr-item-name { display: block; font-size: 13px; color: #1e2a3a; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mr-item-meta { display: flex; align-items: center; gap: 6px; font-size: 11px; color: #8c95a3; margin-top: 2px; }
.mr-target-tag { display: inline-block; padding: 0 5px; background: #f0f4ff; color: #1a6dff; border-radius: 3px; font-size: 10px; font-weight: 500; }
.mr-status-dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }
.mr-item-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.mr-weight-input { display: flex; align-items: center; gap: 2px; }
.mr-weight-input input { width: 52px; border: 1px solid #dce1e8; border-radius: 6px; padding: 4px 6px; font-size: 12px; text-align: right; outline: none; }
.mr-weight-input input:focus { border-color: #7aadff; }
.mr-weight-unit { font-size: 11px; color: #8c95a3; }
.mr-remove { background: none; border: none; padding: 4px; color: #8c95a3; cursor: pointer; border-radius: 4px; display: flex; }
.mr-remove:hover { color: #ef4444; background: #fef2f2; }

/* Picker */
.mr-picker { width: 480px; max-height: 70vh; background: #fff; border-radius: 12px; box-shadow: 0 8px 32px rgba(0,0,0,0.15); overflow: hidden; display: flex; flex-direction: column; }
.mr-picker-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; border-bottom: 1px solid #e8ecf0; }
.mr-picker-head h3 { font-size: 14px; font-weight: 600; color: #1e2a3a; margin: 0; }
.mr-picker-head button { background: none; border: none; font-size: 20px; color: #8c95a3; cursor: pointer; padding: 0 4px; }
.mr-picker-loading, .mr-picker-empty { padding: 40px 0; text-align: center; font-size: 13px; color: #8c95a3; }
.mr-picker-list { overflow-y: auto; padding: 8px; }
.mr-picker-item { padding: 10px 14px; border-radius: 8px; cursor: pointer; transition: background 0.12s; }
.mr-picker-item:hover { background: #f0f4ff; }
.mr-picker-name { font-size: 13px; font-weight: 500; color: #1e2a3a; }
.mr-picker-meta { font-size: 11px; color: #8c95a3; margin-top: 2px; display: flex; align-items: center; gap: 4px; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
