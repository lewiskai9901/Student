<template>
  <div class="user-org-relation-manager">
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
      <el-table-column label="职务" prop="positionTitle" width="120">
        <template #default="{ row }">
          {{ row.positionTitle || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="180">
        <template #default="{ row }">
          <el-tag v-if="row.isPrimary" type="success" size="small">主归属</el-tag>
          <el-tag v-if="row.isLeader" type="warning" size="small" class="ml-1">领导</el-tag>
          <el-tag v-if="row.isExpired" type="danger" size="small" class="ml-1">已过期</el-tag>
          <el-tag v-else-if="row.isExpiringSoon" type="warning" size="small" class="ml-1">即将到期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="权重" prop="weightRatio" width="80" align="center">
        <template #default="{ row }">
          {{ row.weightRatio }}%
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
      width="600px"
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
              v-for="opt in relationTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            >
              <span>{{ opt.label }}</span>
              <span style="color: #999; font-size: 12px; margin-left: 8px">{{ opt.description }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="职务名称" prop="positionTitle">
          <el-input v-model="formData.positionTitle" placeholder="如：班主任、年级主任" />
        </el-form-item>

        <el-form-item label="职务级别" prop="positionLevel">
          <el-input-number
            v-model="formData.positionLevel"
            :min="1"
            :max="10"
            placeholder="1-10，数字越小级别越高"
            style="width: 100%"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="是否领导" prop="isLeader">
              <el-switch v-model="formData.isLeader" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="管理权限" prop="canManage">
              <el-switch v-model="formData.canManage" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="审批权限" prop="canApprove">
              <el-switch v-model="formData.canApprove" />
            </el-form-item>
          </el-col>
        </el-row>

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
import { ref, onMounted, watch } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { userOrgRelationApi } from '@/api/userOrgRelation'
import { orgUnitApi } from '@/api/organization'
import type {
  UserOrgRelation,
  UserOrgRelationFormData,
  RelationType
} from '@/types/userOrgRelation'
import { createDefaultUserOrgRelationFormData, relationTypeOptions } from '@/types/userOrgRelation'

interface Props {
  userId: number
}

const props = defineProps<Props>()

// 状态
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const relations = ref<UserOrgRelation[]>([])
const orgTree = ref<any[]>([])
const orgMap = ref<Map<number, string>>(new Map())

const formRef = ref<FormInstance>()
const formData = ref<UserOrgRelationFormData>(createDefaultUserOrgRelationFormData())

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

// 加载用户关系
const loadRelations = async () => {
  if (!props.userId) return

  loading.value = true
  try {
    relations.value = await userOrgRelationApi.getUserRelations(props.userId)
  } catch (error) {
    console.error('加载用户组织关系失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 添加关系
const handleAdd = () => {
  isEdit.value = false
  editingId.value = null
  formData.value = createDefaultUserOrgRelationFormData()
  formData.value.userId = props.userId
  dialogVisible.value = true
}

// 编辑关系
const handleEdit = (row: UserOrgRelation) => {
  isEdit.value = true
  editingId.value = row.id
  formData.value = {
    userId: row.userId,
    orgUnitId: row.orgUnitId,
    relationType: row.relationType,
    positionTitle: row.positionTitle || '',
    positionLevel: row.positionLevel,
    isPrimary: row.isPrimary,
    isLeader: row.isLeader,
    canManage: row.canManage,
    canApprove: row.canApprove,
    startDate: row.startDate || '',
    endDate: row.endDate || '',
    weightRatio: row.weightRatio,
    sortOrder: row.sortOrder,
    remark: row.remark || ''
  }
  dialogVisible.value = true
}

// 设为主归属
const handleSetPrimary = async (row: UserOrgRelation) => {
  try {
    await ElMessageBox.confirm(
      `确定将「${getOrgName(row.orgUnitId)}」设为主归属吗？`,
      '确认'
    )
    await userOrgRelationApi.setPrimary(props.userId, row.id)
    ElMessage.success('设置成功')
    loadRelations()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('设置失败')
    }
  }
}

// 删除关系
const handleDelete = async (row: UserOrgRelation) => {
  try {
    await ElMessageBox.confirm(
      `确定删除与「${getOrgName(row.orgUnitId)}」的关系吗？`,
      '确认删除',
      { type: 'warning' }
    )
    await userOrgRelationApi.deleteRelation(row.id)
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
      await userOrgRelationApi.updateRelation(editingId.value, {
        positionTitle: formData.value.positionTitle || undefined,
        positionLevel: formData.value.positionLevel || undefined,
        isLeader: formData.value.isLeader,
        canManage: formData.value.canManage,
        canApprove: formData.value.canApprove,
        startDate: formData.value.startDate || undefined,
        endDate: formData.value.endDate || undefined,
        weightRatio: formData.value.weightRatio,
        sortOrder: formData.value.sortOrder,
        remark: formData.value.remark || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await userOrgRelationApi.addRelation({
        userId: props.userId,
        orgUnitId: formData.value.orgUnitId!,
        relationType: formData.value.relationType as RelationType,
        positionTitle: formData.value.positionTitle || undefined,
        positionLevel: formData.value.positionLevel || undefined,
        isPrimary: formData.value.isPrimary,
        isLeader: formData.value.isLeader,
        canManage: formData.value.canManage,
        canApprove: formData.value.canApprove,
        startDate: formData.value.startDate || undefined,
        endDate: formData.value.endDate || undefined,
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

// 监听userId变化
watch(() => props.userId, (newVal) => {
  if (newVal) {
    loadRelations()
  }
}, { immediate: true })

onMounted(() => {
  loadOrgTree()
})
</script>

<style scoped>
.user-org-relation-manager {
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
</style>
