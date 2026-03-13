<template>
  <!-- V10: 改为 Dialog（弹窗）样式 -->
  <el-dialog
    :model-value="visible"
    @update:model-value="handleClose"
    :title="dialogTitle"
    width="560px"
    :close-on-click-modal="false"
    class="space-form-dialog"
    destroy-on-close
    align-center
  >
    <div class="drawer-content">
      <!-- 创建模式：类型选择 -->
      <div v-if="mode === 'create' && !selectedTypeCode" class="type-selector">
        <div class="text-sm text-gray-500 mb-3">
          {{ parentSpace ? `在"${parentSpace.spaceName}"下创建` : '创建根级空间' }}
        </div>
        <div class="type-list">
          <div
            v-for="type in allowedTypes"
            :key="type.typeCode"
            class="type-item"
            @click="selectType(type)"
          >
            <div
              class="type-icon"
              :class="getTypeColorClass(type.typeCode)"
            >
              <el-icon class="text-white">
                <component :is="getTypeIcon(type.icon)" />
              </el-icon>
            </div>
            <div class="type-info">
              <div class="type-name">{{ type.typeName }}</div>
              <div class="type-tags">
                <span v-if="type.hasCapacity" class="mini-tag green">容量</span>
                <span v-if="type.bookable" class="mini-tag blue">预订</span>
                <span v-if="type.assignable" class="mini-tag amber">分配</span>
                <span v-if="type.occupiable" class="mini-tag purple">入住</span>
              </div>
            </div>
            <el-icon class="text-gray-300"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 表单区域 -->
      <div v-else class="form-area">
        <!-- 已选类型卡片 -->
        <div v-if="mode === 'create' && selectedType" class="selected-type">
          <div
            class="type-icon-sm"
            :class="getTypeColorClass(selectedType.typeCode)"
          >
            <el-icon class="text-white text-sm">
              <component :is="getTypeIcon(selectedType.icon)" />
            </el-icon>
          </div>
          <div class="flex-1">
            <span class="font-medium text-gray-700">{{ selectedType.typeName }}</span>
            <span class="text-gray-400 text-xs ml-2" v-if="parentSpace">
              父级: {{ parentSpace.spaceName }}
            </span>
          </div>
          <el-button
            v-if="allowedTypes && allowedTypes.length > 1"
            size="small"
            text
            @click="selectedTypeCode = ''"
          >
            更换
          </el-button>
        </div>

        <!-- 紧凑表单 -->
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-position="top"
          size="default"
          class="compact-form"
        >
          <!-- 名称 -->
          <el-form-item label="空间名称" prop="spaceName">
            <el-input
              v-model="formData.spaceName"
              placeholder="请输入空间名称"
              maxlength="50"
            />
          </el-form-item>

          <!-- 容量（如果支持） -->
          <el-form-item v-if="showCapacitySection" label="容量" prop="capacity">
            <el-input-number
              v-model="formData.capacity"
              :min="0"
              :max="99999"
              placeholder="最大容量"
              controls-position="right"
              class="w-full"
            />
            <template #label>
              容量
              <span v-if="capacityUnit" class="text-gray-400 font-normal">（{{ capacityUnit }}）</span>
            </template>
          </el-form-item>

          <!-- 描述（折叠） -->
          <el-collapse-transition>
            <el-form-item v-if="showDescription" label="描述" prop="description">
              <el-input
                v-model="formData.description"
                type="textarea"
                :rows="2"
                placeholder="选填"
                maxlength="500"
              />
            </el-form-item>
          </el-collapse-transition>

          <div v-if="!showDescription" class="mb-3">
            <el-button text type="primary" size="small" @click="showDescription = true">
              + 添加描述
            </el-button>
          </div>

          <!-- 关联信息（折叠） -->
          <el-collapse-transition>
            <div v-if="showRelation" class="relation-section">
              <div class="grid grid-cols-2 gap-3">
                <el-form-item label="所属组织" prop="orgUnitId">
                  <el-tree-select
                    v-model="formData.orgUnitId"
                    :data="orgOptions"
                    placeholder="选择"
                    clearable
                    check-strictly
                    :render-after-expand="false"
                    class="w-full"
                  />
                </el-form-item>

                <el-form-item label="负责人" prop="responsibleUserId">
                  <el-select
                    v-model="formData.responsibleUserId"
                    placeholder="选择"
                    clearable
                    filterable
                    class="w-full"
                  >
                    <el-option
                      v-for="user in userOptions"
                      :key="user.id"
                      :label="user.realName"
                      :value="user.id"
                    />
                  </el-select>
                </el-form-item>
              </div>
            </div>
          </el-collapse-transition>

          <div v-if="!showRelation" class="mb-3">
            <el-button text type="primary" size="small" @click="showRelation = true">
              + 设置关联
            </el-button>
          </div>

          <!-- 特性提示 -->
          <div v-if="showFeaturesSection" class="features-hint">
            <span v-if="selectedType?.bookable" class="feature-badge blue">可预订</span>
            <span v-if="selectedType?.assignable" class="feature-badge amber">可分配</span>
            <span v-if="selectedType?.occupiable" class="feature-badge purple">可入住</span>
          </div>
        </el-form>
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="mode === 'create' && !selectedTypeCode"
          @click="handleSubmit"
        >
          {{ mode === 'create' ? '创建' : '保存' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, markRaw } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  ArrowRight, School, OfficeBuilding, House, Menu, Location, Grid, Box
} from '@element-plus/icons-vue'
import { universalSpaceApi } from '@/api/universalSpace'
import type { SpaceTreeNode, UniversalSpaceType, CreateSpaceRequest, UpdateSpaceRequest } from '@/types/universalSpace'
import { orgUnitApi } from '@/api/organization'
import { userApi } from '@/api/user'

// Props
const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  parentSpace?: SpaceTreeNode | null
  editData?: SpaceTreeNode | null
  allowedTypes?: UniversalSpaceType[]
}>()

// Emits
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'success': []
}>()

// ========== 表单数据 ==========
const formRef = ref<FormInstance>()
const selectedTypeCode = ref('')
const submitting = ref(false)
const showDescription = ref(false)
const showRelation = ref(false)

const formData = ref({
  spaceName: '',
  description: '',
  capacity: undefined as number | undefined,
  orgUnitId: undefined as number | undefined,
  responsibleUserId: undefined as number | undefined
})

const formRules: FormRules = {
  spaceName: [
    { required: true, message: '请输入空间名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

// 选项数据
const orgOptions = ref<any[]>([])
const userOptions = ref<any[]>([])

// ========== 计算属性 ==========
const dialogTitle = computed(() => {
  if (props.mode === 'edit') {
    return `编辑 - ${props.editData?.spaceName || ''}`
  }
  if (selectedTypeCode.value && selectedType.value) {
    return `新增${selectedType.value.typeName}`
  }
  return '新增空间'
})

const selectedType = computed(() => {
  if (props.mode === 'edit' && props.editData) {
    return {
      typeCode: props.editData.typeCode,
      typeName: props.editData.typeName || '',
      icon: props.editData.typeIcon,
      hasCapacity: props.editData.hasCapacity,
      bookable: props.editData.bookable,
      assignable: props.editData.assignable,
      occupiable: props.editData.occupiable,
      capacityUnit: props.editData.capacityUnit
    } as UniversalSpaceType
  }
  return props.allowedTypes?.find(t => t.typeCode === selectedTypeCode.value)
})

const showCapacitySection = computed(() => selectedType.value?.hasCapacity ?? false)

const showFeaturesSection = computed(() => {
  const type = selectedType.value
  return type && (type.bookable || type.assignable || type.occupiable)
})

const capacityUnit = computed(() => selectedType.value?.capacityUnit || '')

// ========== 方法 ==========
function selectType(type: UniversalSpaceType) {
  selectedTypeCode.value = type.typeCode
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (props.mode === 'create') {
      const request: CreateSpaceRequest = {
        spaceName: formData.value.spaceName,
        typeCode: selectedTypeCode.value,
        description: formData.value.description || undefined,
        parentId: props.parentSpace?.id,
        capacity: formData.value.capacity,
        orgUnitId: formData.value.orgUnitId,
        responsibleUserId: formData.value.responsibleUserId
      }
      await universalSpaceApi.create(request)
      ElMessage.success('空间创建成功')
    } else {
      const request: UpdateSpaceRequest = {
        spaceName: formData.value.spaceName,
        description: formData.value.description || undefined,
        capacity: formData.value.capacity,
        orgUnitId: formData.value.orgUnitId,
        responsibleUserId: formData.value.responsibleUserId
      }
      await universalSpaceApi.update(props.editData!.id, request)
      ElMessage.success('空间更新成功')
    }
    emit('success')
    handleClose()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function handleClose() {
  emit('update:visible', false)
}

function resetForm() {
  selectedTypeCode.value = ''
  showDescription.value = false
  showRelation.value = false
  formData.value = {
    spaceName: '',
    description: '',
    capacity: undefined,
    orgUnitId: undefined,
    responsibleUserId: undefined
  }
  formRef.value?.resetFields()
}

async function loadOptions() {
  try {
    const orgData = await orgUnitApi.getTree()
    orgOptions.value = transformOrgTree(orgData)
  } catch (error) {
    console.error('加载组织数据失败:', error)
  }

  try {
    const userData = await userApi.list({ pageNum: 1, pageSize: 500, status: 1 })
    userOptions.value = userData.records || []
  } catch (error) {
    console.error('加载用户数据失败:', error)
  }
}

function transformOrgTree(nodes: any[]): any[] {
  return nodes.map(node => ({
    value: node.id,
    label: node.unitName || node.name,
    children: node.children ? transformOrgTree(node.children) : undefined
  }))
}

// ========== 工具函数 ==========
function getTypeIcon(iconName?: string) {
  const iconMap: Record<string, any> = {
    'School': markRaw(School),
    'OfficeBuilding': markRaw(OfficeBuilding),
    'House': markRaw(House),
    'Menu': markRaw(Menu),
    'Location': markRaw(Location),
    'Grid': markRaw(Grid),
    'Box': markRaw(Box)
  }
  return iconMap[iconName || ''] || OfficeBuilding
}

function getTypeColorClass(typeCode: string) {
  const colorMap: Record<string, string> = {
    'SITE': 'bg-blue-500',
    'BUILDING': 'bg-purple-500',
    'FLOOR': 'bg-cyan-500',
    'ROOM': 'bg-green-500',
    'AREA': 'bg-amber-500',
    'STATION': 'bg-rose-500'
  }
  return colorMap[typeCode] || 'bg-gray-500'
}

// ========== 监听 ==========
watch(() => props.visible, (val) => {
  if (val) {
    resetForm()
    loadOptions()

    if (props.mode === 'edit' && props.editData) {
      formData.value = {
        spaceName: props.editData.spaceName,
        description: props.editData.description || '',
        capacity: props.editData.capacity,
        orgUnitId: props.editData.orgUnitId,
        responsibleUserId: props.editData.responsibleUserId
      }
      // 编辑模式下，如果有描述或关联则展开
      showDescription.value = !!props.editData.description
      showRelation.value = !!props.editData.orgUnitId || !!props.editData.responsibleUserId
    } else if (props.mode === 'create' && props.allowedTypes?.length === 1) {
      selectedTypeCode.value = props.allowedTypes[0].typeCode
    }
  }
})
</script>

<style scoped>
/* V10: Dialog 样式 */
.space-form-dialog :deep(.el-dialog__header) {
  margin-bottom: 0;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.space-form-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.space-form-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
}

.drawer-content {
  padding: 16px 20px;
  max-height: 60vh;
  overflow-y: auto;
}

/* 类型选择列表 */
.type-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.type-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.type-item:hover {
  border-color: #93c5fd;
  background-color: #f8fafc;
}

.type-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.type-info {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
}

.type-name {
  font-weight: 500;
  color: #374151;
}

.type-tags {
  display: flex;
  gap: 4px;
  margin-top: 4px;
}

.mini-tag {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 3px;
}

.mini-tag.green { background: #dcfce7; color: #166534; }
.mini-tag.blue { background: #dbeafe; color: #1e40af; }
.mini-tag.amber { background: #fef3c7; color: #92400e; }
.mini-tag.purple { background: #f3e8ff; color: #7c3aed; }

/* 已选类型 */
.selected-type {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 16px;
}

.type-icon-sm {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
}

/* 紧凑表单 */
.compact-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.compact-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: #4b5563;
  padding-bottom: 4px;
}

.compact-form :deep(.el-input__inner),
.compact-form :deep(.el-textarea__inner) {
  border-radius: 6px;
}

.relation-section {
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 16px;
}

/* 特性提示 */
.features-hint {
  display: flex;
  gap: 6px;
  padding: 10px 0;
  border-top: 1px dashed #e5e7eb;
}

.feature-badge {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
}

.feature-badge.blue { background: #dbeafe; color: #1e40af; }
.feature-badge.amber { background: #fef3c7; color: #92400e; }
.feature-badge.purple { background: #f3e8ff; color: #7c3aed; }

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.w-full {
  width: 100%;
}
</style>
