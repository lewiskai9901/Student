<template>
  <div class="user-space-relation-manager">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">
        {{ mode === 'user' ? '分配场所' : '分配用户' }}
      </el-button>
      <el-button :icon="Refresh" @click="loadRelations">刷新</el-button>
    </div>

    <!-- 关系列表 -->
    <el-table :data="relations" v-loading="loading" border stripe>
      <el-table-column v-if="mode === 'user'" label="场所" prop="spaceId" min-width="150">
        <template #default="{ row }">
          <span>{{ getSpaceName(row.spaceId) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="mode === 'space'" label="用户" prop="userId" min-width="120">
        <template #default="{ row }">
          <span>{{ getUserName(row.userId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="关系类型" prop="relationTypeLabel" width="100" />
      <el-table-column label="位置" width="120">
        <template #default="{ row }">
          <span v-if="row.positionCode">
            {{ row.positionName || row.positionCode }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="180">
        <template #default="{ row }">
          <el-tag v-if="row.isPrimary" type="success" size="small">主要</el-tag>
          <el-tag v-if="row.needsPayment" type="danger" size="small" class="ml-1">待缴费</el-tag>
          <el-tag v-if="row.isExpired" type="info" size="small" class="ml-1">已过期</el-tag>
          <el-tag v-else-if="row.isExpiringSoon" type="warning" size="small" class="ml-1">即将到期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="费用" width="120">
        <template #default="{ row }">
          <span v-if="row.feeAmount">
            ¥{{ row.feeAmount }}
            <el-tag v-if="row.feePaid" type="success" size="small">已缴</el-tag>
          </span>
          <span v-else>-</span>
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
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="!row.isPrimary && mode === 'user'"
            type="primary"
            link
            size="small"
            @click="handleSetPrimary(row)"
          >
            设为主要
          </el-button>
          <el-button
            v-if="row.needsPayment"
            type="success"
            link
            size="small"
            @click="handleMarkPaid(row)"
          >
            标记已缴
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
      :title="isEdit ? '编辑场所分配' : '添加场所分配'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item v-if="mode === 'user' && !isEdit" label="场所" prop="spaceId">
          <el-tree-select
            v-model="formData.spaceId"
            :data="spaceTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="选择场所"
            check-strictly
            filterable
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item v-if="mode === 'space' && !isEdit" label="用户" prop="userId">
          <el-select
            v-model="formData.userId"
            filterable
            remote
            placeholder="搜索用户"
            :remote-method="searchUsers"
            :loading="userSearchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.name"
              :value="user.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="关系类型" prop="relationType" v-if="!isEdit">
          <el-select v-model="formData.relationType" placeholder="选择关系类型" style="width: 100%">
            <el-option
              v-for="opt in userSpaceRelationTypeOptions"
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
            <el-form-item label="位置编码" prop="positionCode">
              <el-input v-model="formData.positionCode" placeholder="如：A-101" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="位置名称" prop="positionName">
              <el-input v-model="formData.positionName" placeholder="如：1号床位" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="使用权" prop="canUse">
              <el-switch v-model="formData.canUse" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理权" prop="canManage">
              <el-switch v-model="formData.canManage" />
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
            <el-form-item label="费用金额" prop="feeAmount">
              <el-input-number
                v-model="formData.feeAmount"
                :min="0"
                :precision="2"
                placeholder="费用"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否已缴" prop="feePaid">
              <el-switch v-model="formData.feePaid" />
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
import { userSpaceRelationApi } from '@/api/userSpaceRelation'
import type {
  UserSpaceRelation,
  UserSpaceRelationFormData,
  UserSpaceRelationType
} from '@/types/userSpaceRelation'
import { createDefaultUserSpaceRelationFormData, userSpaceRelationTypeOptions } from '@/types/userSpaceRelation'

interface Props {
  mode: 'user' | 'space'  // user: 查看用户的场所; space: 查看场所的用户
  userId?: number
  spaceId?: number
}

const props = defineProps<Props>()

// 状态
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const relations = ref<UserSpaceRelation[]>([])
const spaceTree = ref<any[]>([])
const spaceMap = ref<Map<number, string>>(new Map())
const userMap = ref<Map<number, string>>(new Map())
const userOptions = ref<{ id: number; name: string }[]>([])
const userSearchLoading = ref(false)

const formRef = ref<FormInstance>()
const formData = ref<UserSpaceRelationFormData>(createDefaultUserSpaceRelationFormData())

const formRules: FormRules = {
  spaceId: [{ required: true, message: '请选择场所', trigger: 'change' }],
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }]
}

// 获取场所名称
const getSpaceName = (spaceId: number): string => {
  return spaceMap.value.get(spaceId) || `场所#${spaceId}`
}

// 获取用户名称
const getUserName = (userId: number): string => {
  return userMap.value.get(userId) || `用户#${userId}`
}

// 搜索用户
const searchUsers = async (query: string) => {
  if (!query) {
    userOptions.value = []
    return
  }
  userSearchLoading.value = true
  try {
    // TODO: 实现用户搜索API
    // const users = await userApi.search(query)
    // userOptions.value = users
    userOptions.value = []
  } catch (error) {
    console.error('搜索用户失败', error)
  } finally {
    userSearchLoading.value = false
  }
}

// 加载关系
const loadRelations = async () => {
  loading.value = true
  try {
    if (props.mode === 'user' && props.userId) {
      relations.value = await userSpaceRelationApi.getUserRelations(props.userId)
    } else if (props.mode === 'space' && props.spaceId) {
      relations.value = await userSpaceRelationApi.getSpaceUsers(props.spaceId)
    }
  } catch (error) {
    console.error('加载关系失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 添加关系
const handleAdd = () => {
  isEdit.value = false
  editingId.value = null
  formData.value = createDefaultUserSpaceRelationFormData()
  if (props.mode === 'user') {
    formData.value.userId = props.userId || null
  } else {
    formData.value.spaceId = props.spaceId || null
  }
  dialogVisible.value = true
}

// 编辑关系
const handleEdit = (row: UserSpaceRelation) => {
  isEdit.value = true
  editingId.value = row.id
  formData.value = {
    userId: row.userId,
    spaceId: row.spaceId,
    relationType: row.relationType,
    positionCode: row.positionCode || '',
    positionName: row.positionName || '',
    isPrimary: row.isPrimary,
    canUse: row.canUse,
    canManage: row.canManage,
    startDate: row.startDate || '',
    endDate: row.endDate || '',
    feeAmount: row.feeAmount,
    feePaid: row.feePaid,
    sortOrder: row.sortOrder,
    remark: row.remark || ''
  }
  dialogVisible.value = true
}

// 设为主要场所
const handleSetPrimary = async (row: UserSpaceRelation) => {
  try {
    await ElMessageBox.confirm(
      `确定将此场所设为主要场所吗？`,
      '确认'
    )
    await userSpaceRelationApi.setPrimary(row.userId, row.id)
    ElMessage.success('设置成功')
    loadRelations()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('设置失败')
    }
  }
}

// 标记已缴费
const handleMarkPaid = async (row: UserSpaceRelation) => {
  try {
    await ElMessageBox.confirm(
      `确定标记此记录为已缴费吗？`,
      '确认'
    )
    await userSpaceRelationApi.markAsPaid(row.id)
    ElMessage.success('标记成功')
    loadRelations()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 删除关系
const handleDelete = async (row: UserSpaceRelation) => {
  try {
    await ElMessageBox.confirm(
      `确定删除此分配关系吗？`,
      '确认删除',
      { type: 'warning' }
    )
    await userSpaceRelationApi.deleteRelation(row.id)
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
      await userSpaceRelationApi.updateRelation(editingId.value, {
        positionCode: formData.value.positionCode || undefined,
        positionName: formData.value.positionName || undefined,
        canUse: formData.value.canUse,
        canManage: formData.value.canManage,
        startDate: formData.value.startDate || undefined,
        endDate: formData.value.endDate || undefined,
        feeAmount: formData.value.feeAmount || undefined,
        feePaid: formData.value.feePaid,
        sortOrder: formData.value.sortOrder,
        remark: formData.value.remark || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await userSpaceRelationApi.addRelation({
        userId: formData.value.userId!,
        spaceId: formData.value.spaceId!,
        relationType: formData.value.relationType as UserSpaceRelationType,
        positionCode: formData.value.positionCode || undefined,
        positionName: formData.value.positionName || undefined,
        isPrimary: formData.value.isPrimary,
        canUse: formData.value.canUse,
        canManage: formData.value.canManage,
        startDate: formData.value.startDate || undefined,
        endDate: formData.value.endDate || undefined,
        feeAmount: formData.value.feeAmount || undefined,
        feePaid: formData.value.feePaid,
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

// 监听props变化
watch([() => props.userId, () => props.spaceId], () => {
  loadRelations()
}, { immediate: true })

onMounted(() => {
  // TODO: 加载场所树等初始数据
})
</script>

<style scoped>
.user-space-relation-manager {
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
