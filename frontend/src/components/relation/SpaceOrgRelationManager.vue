<template>
  <div class="space-org-relation-manager">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">
        添加归属
      </el-button>
      <el-button :icon="Refresh" @click="loadRelations">刷新</el-button>
    </div>

    <!-- 关系列表 -->
    <el-table :data="relations" v-loading="loading" border stripe>
      <el-table-column label="组织" prop="orgUnitId" min-width="150">
        <template #default="{ row }">
          <span>{{ getOrgName(row.orgUnitId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="关系类型" prop="relationTypeLabel" width="100" />
      <el-table-column label="优先级" prop="priorityLevel" width="80" align="center" />
      <el-table-column label="状态" width="200">
        <template #default="{ row }">
          <el-tag v-if="row.isPrimary" type="success" size="small">主归属</el-tag>
          <el-tag v-if="row.hasFullManagementRights" type="warning" size="small" class="ml-1">完整权限</el-tag>
          <el-tag v-if="row.isExpired" type="danger" size="small" class="ml-1">已过期</el-tag>
          <el-tag v-else-if="row.isExpiringSoon" type="warning" size="small" class="ml-1">即将到期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="权限" width="180">
        <template #default="{ row }">
          <el-tag v-if="row.canUse" size="small" class="permission-tag">使用</el-tag>
          <el-tag v-if="row.canManage" size="small" class="permission-tag">管理</el-tag>
          <el-tag v-if="row.canAssign" size="small" class="permission-tag">分配</el-tag>
          <el-tag v-if="row.canInspect" size="small" class="permission-tag">检查</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分配容量" prop="allocatedCapacity" width="100" align="center">
        <template #default="{ row }">
          {{ row.allocatedCapacity ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="200">
        <template #default="{ row }">
          <span v-if="row.startDate || row.endDate">
            {{ row.startDate || '∞' }} ~ {{ row.endDate || '∞' }}
          </span>
          <span v-else>永久</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="!row.isPrimary"
            type="primary"
            link
            size="small"
            @click="handleSetPrimary(row)"
          >
            设为主归属
          </el-button>
          <el-button type="primary" link size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button
            type="danger"
            link
            size="small"
            @click="handleDelete(row)"
            :disabled="row.isPrimary && relations.length > 1"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑组织关系' : '添加组织关系'"
      width="650px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="组织" prop="orgUnitId" v-if="!isEdit">
          <el-tree-select
            v-model="formData.orgUnitId"
            :data="orgTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="选择组织"
            check-strictly
            filterable
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="关系类型" prop="relationType" v-if="!isEdit">
          <el-select v-model="formData.relationType" placeholder="选择关系类型" style="width: 100%">
            <el-option
              v-for="opt in spaceRelationTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            >
              <span>{{ opt.label }}</span>
              <span style="color: #999; font-size: 12px; margin-left: 8px">{{ opt.description }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priorityLevel">
              <el-input-number
                v-model="formData.priorityLevel"
                :min="1"
                :max="99"
                placeholder="优先级（越小越优先）"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分配容量" prop="allocatedCapacity">
              <el-input-number
                v-model="formData.allocatedCapacity"
                :min="0"
                placeholder="座位/床位数"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="权限设置">
          <el-checkbox-group v-model="permissions">
            <el-checkbox label="canUse">使用权</el-checkbox>
            <el-checkbox label="canManage">管理权</el-checkbox>
            <el-checkbox label="canAssign">分配权</el-checkbox>
            <el-checkbox label="canInspect">检查权</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="formData.startDate"
                type="date"
                placeholder="选择开始日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker
                v-model="formData.endDate"
                type="date"
                placeholder="选择结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="权重比例" prop="weightRatio">
              <el-input-number
                v-model="formData.weightRatio"
                :min="0"
                :max="100"
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号" prop="sortOrder">
              <el-input-number v-model="formData.sortOrder" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="2"
            placeholder="备注信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { spaceOrgRelationApi } from '@/api/spaceOrgRelation'
import { orgUnitApi } from '@/api/organization'
import type {
  SpaceOrgRelation,
  SpaceOrgRelationFormData,
  SpaceRelationType
} from '@/types/spaceOrgRelation'
import { createDefaultSpaceOrgRelationFormData, spaceRelationTypeOptions } from '@/types/spaceOrgRelation'

interface Props {
  spaceId: number
}

const props = defineProps<Props>()

// 状态
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const relations = ref<SpaceOrgRelation[]>([])
const orgTree = ref<any[]>([])
const orgMap = ref<Map<number, string>>(new Map())

const formRef = ref<FormInstance>()
const formData = ref<SpaceOrgRelationFormData>(createDefaultSpaceOrgRelationFormData())

// 权限复选框
const permissions = computed({
  get: () => {
    const perms: string[] = []
    if (formData.value.canUse) perms.push('canUse')
    if (formData.value.canManage) perms.push('canManage')
    if (formData.value.canAssign) perms.push('canAssign')
    if (formData.value.canInspect) perms.push('canInspect')
    return perms
  },
  set: (val: string[]) => {
    formData.value.canUse = val.includes('canUse')
    formData.value.canManage = val.includes('canManage')
    formData.value.canAssign = val.includes('canAssign')
    formData.value.canInspect = val.includes('canInspect')
  }
})

const formRules: FormRules = {
  orgUnitId: [{ required: true, message: '请选择组织', trigger: 'change' }],
  relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }]
}

// 加载组织树
const loadOrgTree = async () => {
  try {
    const data = await orgUnitApi.getTree()
    orgTree.value = data
    // 构建ID到名称的映射
    const buildMap = (nodes: any[]) => {
      nodes.forEach(node => {
        orgMap.value.set(node.id, node.name)
        if (node.children) {
          buildMap(node.children)
        }
      })
    }
    buildMap(data)
  } catch (error) {
    console.error('加载组织树失败', error)
  }
}

// 获取组织名称
const getOrgName = (orgUnitId: number): string => {
  return orgMap.value.get(orgUnitId) || `组织#${orgUnitId}`
}

// 加载场所关系
const loadRelations = async () => {
  if (!props.spaceId) return

  loading.value = true
  try {
    relations.value = await spaceOrgRelationApi.getSpaceRelations(props.spaceId)
  } catch (error) {
    console.error('加载场所组织关系失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 添加关系
const handleAdd = () => {
  isEdit.value = false
  editingId.value = null
  formData.value = createDefaultSpaceOrgRelationFormData()
  formData.value.spaceId = props.spaceId
  dialogVisible.value = true
}

// 编辑关系
const handleEdit = (row: SpaceOrgRelation) => {
  isEdit.value = true
  editingId.value = row.id
  formData.value = {
    spaceId: row.spaceId,
    orgUnitId: row.orgUnitId,
    relationType: row.relationType,
    isPrimary: row.isPrimary,
    priorityLevel: row.priorityLevel || 1,
    canUse: row.canUse,
    canManage: row.canManage,
    canAssign: row.canAssign,
    canInspect: row.canInspect,
    useSchedule: row.useSchedule || '',
    startDate: row.startDate || '',
    endDate: row.endDate || '',
    allocatedCapacity: row.allocatedCapacity,
    weightRatio: row.weightRatio,
    sortOrder: row.sortOrder,
    remark: row.remark || ''
  }
  dialogVisible.value = true
}

// 设为主归属
const handleSetPrimary = async (row: SpaceOrgRelation) => {
  try {
    await ElMessageBox.confirm(
      `确定将「${getOrgName(row.orgUnitId)}」设为主归属吗？`,
      '确认'
    )
    await spaceOrgRelationApi.setPrimary(props.spaceId, row.id)
    ElMessage.success('设置成功')
    loadRelations()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('设置失败')
    }
  }
}

// 删除关系
const handleDelete = async (row: SpaceOrgRelation) => {
  try {
    await ElMessageBox.confirm(
      `确定删除与「${getOrgName(row.orgUnitId)}」的关系吗？`,
      '确认删除',
      { type: 'warning' }
    )
    await spaceOrgRelationApi.deleteRelation(row.id)
    ElMessage.success('删除成功')
    loadRelations()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      await spaceOrgRelationApi.updateRelation(editingId.value, {
        priorityLevel: formData.value.priorityLevel,
        canUse: formData.value.canUse,
        canManage: formData.value.canManage,
        canAssign: formData.value.canAssign,
        canInspect: formData.value.canInspect,
        useSchedule: formData.value.useSchedule || undefined,
        startDate: formData.value.startDate || undefined,
        endDate: formData.value.endDate || undefined,
        allocatedCapacity: formData.value.allocatedCapacity || undefined,
        weightRatio: formData.value.weightRatio,
        sortOrder: formData.value.sortOrder,
        remark: formData.value.remark || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await spaceOrgRelationApi.addRelation({
        spaceId: props.spaceId,
        orgUnitId: formData.value.orgUnitId!,
        relationType: formData.value.relationType as SpaceRelationType,
        isPrimary: formData.value.isPrimary,
        priorityLevel: formData.value.priorityLevel,
        canUse: formData.value.canUse,
        canManage: formData.value.canManage,
        canAssign: formData.value.canAssign,
        canInspect: formData.value.canInspect,
        useSchedule: formData.value.useSchedule || undefined,
        startDate: formData.value.startDate || undefined,
        endDate: formData.value.endDate || undefined,
        allocatedCapacity: formData.value.allocatedCapacity || undefined,
        weightRatio: formData.value.weightRatio,
        sortOrder: formData.value.sortOrder,
        remark: formData.value.remark || undefined
      })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadRelations()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
  } finally {
    submitting.value = false
  }
}

// 监听spaceId变化
watch(() => props.spaceId, (newVal) => {
  if (newVal) {
    loadRelations()
  }
}, { immediate: true })

onMounted(() => {
  loadOrgTree()
})
</script>

<style scoped>
.space-org-relation-manager {
  padding: 16px;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.ml-1 {
  margin-left: 4px;
}

.permission-tag {
  margin-right: 4px;
}
</style>
