<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
    @closed="handleClosed"
  >
    <div class="space-y-5">
      <!-- Basic Info -->
      <div>
        <h4 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">基本信息</h4>

        <!-- Name -->
        <div>
          <label class="mb-1 block text-sm font-medium text-gray-700">
            组织名称 <span class="text-red-500">*</span>
          </label>
          <el-input
            v-model="formData.unitName"
            placeholder="请输入组织名称"
            maxlength="50"
            show-word-limit
            @input="handleNameInput"
          />
        </div>

        <!-- Type Selection -->
        <div class="mt-3">
          <label class="mb-2 block text-sm font-medium text-gray-700">
            组织类型 <span class="text-red-500">*</span>
          </label>
          <div v-if="typesLoading" class="flex items-center gap-2 text-sm text-gray-400">
            <div class="h-4 w-4 animate-spin rounded-full border-2 border-blue-500 border-t-transparent"></div>
            加载类型中...
          </div>
          <div v-else-if="availableTypes.length > 0" class="flex flex-wrap gap-2">
            <button
              v-for="t in availableTypes"
              :key="t.typeCode"
              type="button"
              class="inline-flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-sm transition-colors"
              :class="formData.unitType === t.typeCode
                ? 'border-blue-300 bg-blue-50 text-blue-700 ring-1 ring-blue-200'
                : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300 hover:bg-gray-50'"
              :disabled="isEdit"
              @click="selectType(t)"
            >
              <span
                class="flex h-5 w-5 items-center justify-center rounded bg-gray-100 text-gray-500"
              >
                <Building2 class="h-3 w-3" />
              </span>
              {{ t.typeName }}
            </button>
          </div>
          <p v-else class="text-sm text-gray-400">暂无可用类型</p>
          <p v-if="typeError" class="mt-1 text-xs text-red-500">{{ typeError }}</p>
        </div>

        <!-- Auto-generated Code -->
        <div class="mt-3">
          <label class="mb-1 block text-sm font-medium text-gray-700">组织编码</label>
          <div v-if="isEdit" class="flex items-center gap-2">
            <code class="rounded bg-gray-100 px-2.5 py-1.5 font-mono text-sm text-gray-600">{{ formData.unitCode }}</code>
            <span class="text-xs text-gray-400">编码不可修改</span>
          </div>
          <div v-else class="flex items-center gap-2">
            <code class="rounded bg-gray-100 px-2.5 py-1.5 font-mono text-sm text-gray-600">
              {{ formData.unitCode || '选择类型后自动生成' }}
            </code>
            <button
              v-if="formData.unitType"
              type="button"
              class="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
              title="重新生成编码"
              @click="regenerateCode"
            >
              <RefreshCw class="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      </div>

      <!-- Hierarchy -->
      <div>
        <h4 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">层级关系</h4>
        <div>
          <label class="mb-1 block text-sm font-medium text-gray-700">上级组织</label>
          <el-tree-select
            v-model="formData.parentId"
            :data="parentOptions"
            :props="treeSelectProps"
            placeholder="留空为顶级组织"
            clearable
            check-strictly
            :render-after-expand="false"
            style="width: 100%"
          >
            <template #default="{ data }">
              <div class="flex items-center gap-2">
                <span
                  class="flex h-5 w-5 items-center justify-center rounded"
                  :style="{
                    backgroundColor: `${data.typeColor || '#6b7280'}18`,
                    color: data.typeColor || '#6b7280'
                  }"
                >
                  <Building2 class="h-3 w-3" />
                </span>
                <span>{{ data.unitName }}</span>
                <span class="text-xs text-gray-400">({{ data.unitCode }})</span>
              </div>
            </template>
          </el-tree-select>
        </div>
      </div>

      <!-- Position Selection (only for create mode, when type has templates) -->
      <div v-if="!isEdit && positionTemplates.length > 0">
        <h4 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">初始岗位</h4>
        <p class="mb-2 text-xs text-gray-400">从模板中选择要创建的岗位，可设置编制人数</p>
        <div class="space-y-2">
          <div
            v-for="(pos, idx) in positionSelections"
            :key="idx"
            class="flex items-center gap-3 rounded-lg border px-3 py-2 transition-colors"
            :class="pos.selected
              ? 'border-blue-200 bg-blue-50/50'
              : 'border-gray-200 bg-white'"
          >
            <input
              type="checkbox"
              v-model="pos.selected"
              class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            <span class="flex-1 text-sm" :class="pos.selected ? 'text-gray-800' : 'text-gray-500'">
              {{ pos.positionName }}
            </span>
            <div v-if="pos.selected" class="flex items-center gap-1.5">
              <span class="text-xs text-gray-400">编制</span>
              <el-input-number
                v-model="pos.headcount"
                :min="1"
                :max="99"
                size="small"
                controls-position="right"
                style="width: 80px"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

      <!-- Dynamic Extension Fields (from plugin metadata_schema) -->
      <div v-if="typeSchema && typeSchema.fields?.length > 0">
        <h4 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">扩展属性</h4>
        <DynamicForm
          :schema="typeSchema"
          v-model="formData.attributes"
        />
      </div>

    <template #footer>
      <div class="flex items-center justify-end gap-3">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '创建组织' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Building2, RefreshCw } from 'lucide-vue-next'
import DynamicForm from '@/components/extension/DynamicForm.vue'
import { entityTypeApi } from '@/api/entityType'
import {
  orgUnitApi,
  getEnabledOrgUnitTypes,
  getAllowedChildTypes,
  type DepartmentResponse,
  type OrgUnitTypeConfig
} from '@/api/organization'
// ClassForm removed - org management is now generic, class binding done in student module

interface Props {
  visible: boolean
  department?: DepartmentResponse | null
  parentDepartment?: DepartmentResponse | null
  allDepartments: DepartmentResponse[]
}

const props = withDefaults(defineProps<Props>(), {
  department: null,
  parentDepartment: null
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: [createdId?: number]
}>()

const submitting = ref(false)
const typesLoading = ref(false)
const availableTypes = ref<OrgUnitTypeConfig[]>([])
const typeError = ref('')

const formData = reactive({
  unitName: '',
  unitCode: '',
  unitType: '',
  parentId: null as number | null,
  attributes: {} as Record<string, any>,
})

// Dynamic schema from entity_type_configs (loaded when type is selected)
const typeSchema = ref<{ fields: any[] } | null>(null)

// Position selection state
interface PositionSelection {
  positionName: string
  selected: boolean
  headcount: number
}
const positionSelections = ref<PositionSelection[]>([])

const isEdit = computed(() => !!props.department)

// Get position templates from the currently selected type
const positionTemplates = computed(() => {
  if (!formData.unitType) return []
  const selectedTypeConfig = availableTypes.value.find(t => t.typeCode === formData.unitType)
  return selectedTypeConfig?.defaultPositions || []
})

const dialogTitle = computed(() => {
  if (isEdit.value) return '编辑组织'
  // 显示选中的类型名，如 "新建年级 — 经济与信息技术系"
  const selectedType = availableTypes.value.find(t => t.typeCode === formData.unitType)
  const typeName = selectedType?.typeName || '子组织'
  if (props.parentDepartment) return `新建${typeName} — ${props.parentDepartment.unitName}`
  return '新建组织'
})

const treeSelectProps = {
  value: 'id',
  label: 'unitName',
  children: 'children'
}

// --- Code generation ---
const CODE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

function generateCode(typeCode: string): string {
  let suffix = ''
  for (let i = 0; i < 6; i++) {
    suffix += CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)]
  }
  return `${typeCode}_${suffix}`
}

function regenerateCode() {
  if (formData.unitType) {
    formData.unitCode = generateCode(formData.unitType)
  }
}

function selectType(t: OrgUnitTypeConfig) {
  if (isEdit.value) return
  formData.unitType = t.typeCode
  typeError.value = ''
  formData.unitCode = generateCode(t.typeCode)
  buildPositionSelections(t)
  // Load dynamic schema from entity_type_configs
  loadTypeSchema(t.typeCode)
}

async function loadTypeSchema(typeCode: string) {
  typeSchema.value = null
  formData.attributes = {}
  try {
    const res = await entityTypeApi.get('ORG_UNIT', typeCode)
    const data = (res as any).data || res
    if (data?.metadataSchema) {
      const schema = typeof data.metadataSchema === 'string' ? JSON.parse(data.metadataSchema) : data.metadataSchema
      if (schema?.fields?.length > 0) {
        typeSchema.value = schema
        // Apply default values
        for (const f of schema.fields) {
          if (f.defaultValue !== undefined) formData.attributes[f.key] = f.defaultValue
          if (f.config?.default !== undefined) formData.attributes[f.key] = f.config.default
        }
      }
    }
  } catch { /* Plugin not registered for this type, no dynamic fields */ }
}

function buildPositionSelections(typeConfig: OrgUnitTypeConfig) {
  const templates = typeConfig.defaultPositions || []
  positionSelections.value = templates.map(tpl => ({
    positionName: tpl.positionName,
    selected: true,  // All selected by default
    headcount: 1
  }))
}

function handleNameInput() {
  // Clear type error when user starts filling the form
  if (typeError.value && formData.unitType) {
    typeError.value = ''
  }
}

// --- Parent options ---
const parentOptions = computed(() => {
  if (!props.allDepartments.length) return []

  const excludeIds = new Set<number>()
  if (isEdit.value && props.department) {
    const collectIds = (node: DepartmentResponse) => {
      excludeIds.add(node.id)
      node.children?.forEach(collectIds)
    }
    collectIds(props.department)
  }

  const filterDepts = (items: DepartmentResponse[]): DepartmentResponse[] => {
    return items
      .filter(item => !excludeIds.has(item.id))
      .map(item => ({
        ...item,
        children: item.children ? filterDepts(item.children) : undefined
      }))
  }

  return filterDepts(props.allDepartments)
})

// --- Load types ---
const loadTypes = async () => {
  typesLoading.value = true
  try {
    if (props.parentDepartment) {
      availableTypes.value = await getAllowedChildTypes(props.parentDepartment.unitType)
    } else {
      // Root creation: only show top-level types (no parentTypeCode)
      const allTypes = await getEnabledOrgUnitTypes()
      availableTypes.value = allTypes.filter(t => !t.parentTypeCode)
    }
    // Auto-select if only one type available
    if (!isEdit.value && availableTypes.value.length === 1) {
      selectType(availableTypes.value[0])
    }
  } catch {
    ElMessage.error('加载组织类型失败')
    availableTypes.value = []
  } finally {
    typesLoading.value = false
  }
}

// --- Watch dialog ---
watch(() => props.visible, async (val) => {
  if (val) {
    await loadTypes()

    if (props.department) {
      // Edit mode
      formData.unitName = props.department.unitName
      formData.unitCode = props.department.unitCode
      formData.unitType = props.department.unitType
      formData.parentId = props.department.parentId || null
    } else if (props.parentDepartment) {
      // Add child mode
      formData.parentId = props.parentDepartment.id
    }
  }
})

// --- Reset ---
const handleClosed = () => {
  Object.assign(formData, {
    unitName: '',
    unitCode: '',
    unitType: '',
    parentId: null,
  })
  availableTypes.value = []
  typeError.value = ''
  positionSelections.value = []
}

// --- Submit ---
const handleSubmit = async () => {
  // Validate
  if (!formData.unitName.trim()) {
    ElMessage.error('请输入组织名称')
    return
  }
  if (formData.unitName.trim().length < 2) {
    ElMessage.error('组织名称至少2个字符')
    return
  }
  if (!isEdit.value) {
    if (!formData.unitType) {
      typeError.value = '请选择组织类型'
      return
    }
    if (!formData.unitCode) {
      ElMessage.error('编码生成异常，请重试')
      return
    }
  }

  submitting.value = true
  try {
    if (isEdit.value && props.department) {
      await orgUnitApi.update(props.department.id, {
        unitName: formData.unitName
      })
      ElMessage.success('组织更新成功')
    } else {
      // Collect selected positions
      const selectedPositions = positionSelections.value
        .filter(p => p.selected && p.positionName.trim())
        .map(p => ({ positionName: p.positionName, headcount: p.headcount }))

      const created = await orgUnitApi.create({
        unitName: formData.unitName,
        unitCode: formData.unitCode,
        unitType: formData.unitType,
        parentId: formData.parentId || undefined,
        selectedPositions: selectedPositions.length > 0 ? selectedPositions : undefined,
        attributes: Object.keys(formData.attributes).length > 0 ? formData.attributes : undefined,
      })
      ElMessage.success('组织创建成功')
      emit('update:visible', false)
      emit('success', created?.id ?? undefined)
      return
    }

    emit('update:visible', false)
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    submitting.value = false
  }
}
</script>
