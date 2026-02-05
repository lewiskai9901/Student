<template>
  <div class="space-type-config">
    <div class="config-layout">
      <!-- 左侧：类型树 -->
      <div class="type-tree-panel">
        <div class="panel-header">
          <span class="panel-title">类型层级</span>
          <el-button type="primary" size="small" @click="handleAddRoot">
            <el-icon><Plus /></el-icon> 新增根类型
          </el-button>
        </div>
        <div class="tree-container">
          <el-tree
            ref="treeRef"
            :data="typeTree"
            :props="{ label: 'typeName', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <span class="node-name">{{ data.typeName }}</span>
                <span class="node-code">{{ data.typeCode }}</span>
                <div class="node-actions">
                  <el-button
                    v-if="data.allowedChildTypes?.length !== 0"
                    link
                    size="small"
                    type="primary"
                    @click.stop="handleAddChild(data)"
                  >
                    <el-icon><Plus /></el-icon>
                  </el-button>
                </div>
              </div>
            </template>
          </el-tree>
          <div v-if="typeTree.length === 0 && !loading" class="empty-state">
            <p>暂无类型数据</p>
            <el-button type="primary" size="small" @click="handleAddRoot">创建第一个类型</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：类型详情/编辑 -->
      <div class="type-detail-panel">
        <template v-if="selectedType">
          <div class="panel-header">
            <span class="panel-title">{{ selectedType.typeName }}</span>
            <div class="header-actions">
              <el-button size="small" @click="handleEdit">编辑</el-button>
              <el-button
                size="small"
                type="danger"
                :disabled="selectedType.system"
                @click="handleDelete"
              >
                删除
              </el-button>
            </div>
          </div>
          <div class="detail-content">
            <div class="detail-row">
              <span class="label">类型编码</span>
              <span class="value">{{ selectedType.typeCode }}</span>
            </div>
            <div class="detail-row">
              <span class="label">描述</span>
              <span class="value">{{ selectedType.description || '-' }}</span>
            </div>
            <div class="detail-section">
              <div class="section-title">层级配置</div>
              <div class="detail-row">
                <span class="label">是否根类型</span>
                <el-tag :type="selectedType.rootType ? 'success' : 'info'" size="small">
                  {{ selectedType.rootType ? '是' : '否' }}
                </el-tag>
              </div>
              <div class="detail-row">
                <span class="label">允许子类型</span>
                <span class="value" v-if="selectedType.allowedChildTypes?.length">
                  <el-tag
                    v-for="code in selectedType.allowedChildTypes"
                    :key="code"
                    size="small"
                    class="mr-1"
                  >
                    {{ getTypeName(code) }}
                  </el-tag>
                </span>
                <span class="value text-gray-400" v-else>无（叶子类型）</span>
              </div>
            </div>
            <div class="detail-section">
              <div class="section-title">行为特性</div>
              <div class="features-grid">
                <div class="feature-item" :class="{ active: selectedType.hasCapacity }">
                  <span class="feature-label">容量</span>
                  <span class="feature-value">{{ selectedType.hasCapacity ? selectedType.capacityUnit || '人' : '无' }}</span>
                </div>
                <div class="feature-item" :class="{ active: selectedType.bookable }">
                  <span class="feature-label">可预订</span>
                  <span class="feature-value">{{ selectedType.bookable ? '是' : '否' }}</span>
                </div>
                <div class="feature-item" :class="{ active: selectedType.assignable }">
                  <span class="feature-label">可分配</span>
                  <span class="feature-value">{{ selectedType.assignable ? '是' : '否' }}</span>
                </div>
                <div class="feature-item" :class="{ active: selectedType.occupiable }">
                  <span class="feature-label">可入住</span>
                  <span class="feature-value">{{ selectedType.occupiable ? '是' : '否' }}</span>
                </div>
              </div>
            </div>
            <div class="detail-row">
              <span class="label">状态</span>
              <el-switch
                v-model="selectedType.enabled"
                :disabled="selectedType.system"
                @change="handleToggleStatus"
              />
            </div>
          </div>
        </template>
        <div v-else class="empty-detail">
          <p>请从左侧选择一个类型查看详情</p>
        </div>
      </div>
    </div>

    <!-- V10: 改为 Dialog（弹窗）样式 -->
    <el-dialog
      v-model="drawerVisible"
      :title="isEdit ? '编辑类型' : (parentType ? `新增子类型 - ${parentType.typeName}` : '新增根类型')"
      width="480px"
      destroy-on-close
      align-center
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
      >
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="formData.typeName" placeholder="如: 楼栋、教室" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>

        <el-divider />

        <el-form-item label="是否根类型" v-if="!parentType">
          <el-switch v-model="formData.rootType" />
          <div class="form-tip">开启后可作为顶级空间</div>
        </el-form-item>

        <el-form-item label="允许的子类型">
          <el-select
            v-model="formData.allowedChildTypes"
            multiple
            placeholder="选择允许创建的子类型"
            style="width: 100%"
          >
            <el-option
              v-for="type in availableChildTypes"
              :key="type.typeCode"
              :label="type.typeName"
              :value="type.typeCode"
            />
          </el-select>
          <div class="form-tip">不选则为叶子类型（不能创建子空间）</div>
        </el-form-item>

        <el-divider />

        <el-form-item label="容量">
          <div class="inline-form">
            <el-switch v-model="formData.hasCapacity" />
            <el-select
              v-if="formData.hasCapacity"
              v-model="formData.capacityUnit"
              placeholder="单位"
              style="width: 100px; margin-left: 12px"
            >
              <el-option label="人" value="人" />
              <el-option label="床位" value="床位" />
              <el-option label="工位" value="工位" />
            </el-select>
          </div>
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="formData.bookable">可预订</el-checkbox>
          <el-checkbox v-model="formData.assignable">可分配</el-checkbox>
          <el-checkbox v-model="formData.occupiable">可入住</el-checkbox>
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { universalSpaceTypeApi } from '@/api/universalSpaceType'
import type { UniversalSpaceType, CreateSpaceTypeRequest } from '@/types/universalSpace'

// 数据
const loading = ref(false)
const submitting = ref(false)
const spaceTypes = ref<UniversalSpaceType[]>([])
const selectedType = ref<UniversalSpaceType | null>(null)
const drawerVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const parentType = ref<UniversalSpaceType | null>(null)
const formRef = ref<FormInstance>()
const treeRef = ref()

// 构建类型树
const typeTree = computed(() => {
  const types = spaceTypes.value
  const typeMap = new Map(types.map(t => [t.typeCode, t]))

  // 找出所有根类型
  const rootTypes = types.filter(t => t.rootType)

  // 递归构建子树
  function buildChildren(parentCodes: string[] | undefined): any[] {
    if (!parentCodes?.length) return []
    return parentCodes
      .map(code => typeMap.get(code))
      .filter(Boolean)
      .map(type => ({
        ...type,
        children: buildChildren(type!.allowedChildTypes)
      }))
  }

  return rootTypes.map(root => ({
    ...root,
    children: buildChildren(root.allowedChildTypes)
  }))
})

// 表单数据
const formData = reactive<CreateSpaceTypeRequest & { rootType: boolean }>({
  typeName: '',
  icon: '',
  description: '',
  sortOrder: 0,
  rootType: false,
  allowedChildTypes: [],
  hasCapacity: false,
  bookable: false,
  assignable: false,
  occupiable: false,
  capacityUnit: '人',
  defaultCapacity: undefined
})

const formRules: FormRules = {
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
}

// 可选的子类型
const availableChildTypes = computed(() => {
  if (!isEdit.value) return spaceTypes.value
  return spaceTypes.value.filter(t => t.id !== editingId.value)
})

// 获取类型名称
function getTypeName(code: string): string {
  return spaceTypes.value.find(t => t.typeCode === code)?.typeName || code
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    spaceTypes.value = await universalSpaceTypeApi.getAll()
  } catch (error) {
    console.error('加载空间类型失败:', error)
    ElMessage.error('加载空间类型失败')
  } finally {
    loading.value = false
  }
}

// 点击节点
function handleNodeClick(data: UniversalSpaceType) {
  selectedType.value = data
}

// 新增根类型
function handleAddRoot() {
  isEdit.value = false
  editingId.value = null
  parentType.value = null
  resetForm()
  formData.rootType = true
  drawerVisible.value = true
}

// 新增子类型
function handleAddChild(parent: UniversalSpaceType) {
  isEdit.value = false
  editingId.value = null
  parentType.value = parent
  resetForm()
  formData.rootType = false
  drawerVisible.value = true
}

// 编辑
function handleEdit() {
  if (!selectedType.value) return
  const row = selectedType.value
  isEdit.value = true
  editingId.value = row.id
  parentType.value = null
  Object.assign(formData, {
    typeName: row.typeName,
    icon: row.icon || '',
    description: row.description || '',
    sortOrder: row.sortOrder,
    rootType: row.rootType,
    allowedChildTypes: row.allowedChildTypes || [],
    hasCapacity: row.hasCapacity,
    bookable: row.bookable,
    assignable: row.assignable,
    occupiable: row.occupiable,
    capacityUnit: row.capacityUnit || '人',
    defaultCapacity: row.defaultCapacity
  })
  drawerVisible.value = true
}

// 删除
async function handleDelete() {
  if (!selectedType.value) return
  try {
    await ElMessageBox.confirm(`确定要删除类型"${selectedType.value.typeName}"吗？`, '提示', { type: 'warning' })
    await universalSpaceTypeApi.delete(selectedType.value.id)
    ElMessage.success('删除成功')
    selectedType.value = null
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 切换状态
async function handleToggleStatus() {
  if (!selectedType.value) return
  try {
    if (selectedType.value.enabled) {
      await universalSpaceTypeApi.enable(selectedType.value.id)
    } else {
      await universalSpaceTypeApi.disable(selectedType.value.id)
    }
    ElMessage.success('操作成功')
  } catch (error: any) {
    selectedType.value.enabled = !selectedType.value.enabled
    ElMessage.error(error.message || '操作失败')
  }
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitting.value = true

    // 如果有父类型，需要更新父类型的allowedChildTypes
    if (parentType.value && !isEdit.value) {
      // 创建新类型后需要更新父类型
    }

    if (isEdit.value && editingId.value) {
      await universalSpaceTypeApi.update(editingId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await universalSpaceTypeApi.create(formData)
      ElMessage.success('创建成功')
    }

    drawerVisible.value = false
    loadData()
  } catch (error: any) {
    if (error !== false) {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

// 重置表单
function resetForm() {
  Object.assign(formData, {
    typeName: '',
    icon: '',
    description: '',
    sortOrder: 0,
    rootType: false,
    allowedChildTypes: [],
    hasCapacity: false,
    bookable: false,
    assignable: false,
    occupiable: false,
    capacityUnit: '人',
    defaultCapacity: undefined
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.space-type-config {
  height: 100%;
  padding: 16px;
}

.config-layout {
  display: flex;
  gap: 16px;
  height: 100%;
}

.type-tree-panel {
  width: 320px;
  flex-shrink: 0;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.type-detail-panel {
  flex: 1;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-title {
  font-weight: 600;
  color: #374151;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.tree-container {
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 4px 0;
}

.node-name {
  font-weight: 500;
  color: #374151;
}

.node-code {
  color: #9ca3af;
  font-size: 11px;
}

.node-actions {
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.15s;
}

.tree-node:hover .node-actions {
  opacity: 1;
}

.empty-state,
.empty-detail {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #9ca3af;
}

.detail-content {
  padding: 16px;
  overflow: auto;
}

.detail-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.detail-row .label {
  width: 100px;
  color: #6b7280;
  font-size: 13px;
  flex-shrink: 0;
}

.detail-row .value {
  flex: 1;
  color: #374151;
  font-size: 13px;
}

.detail-section {
  margin: 16px 0;
}

.section-title {
  font-weight: 500;
  color: #374151;
  margin-bottom: 12px;
  font-size: 13px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.feature-item {
  padding: 8px 12px;
  background: #f9fafb;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
}

.feature-item.active {
  background: #ecfdf5;
}

.feature-label {
  color: #6b7280;
  font-size: 12px;
}

.feature-value {
  color: #374151;
  font-size: 12px;
  font-weight: 500;
}

.feature-item.active .feature-value {
  color: #059669;
}

.form-tip {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.inline-form {
  display: flex;
  align-items: center;
}

.mr-1 {
  margin-right: 4px;
}

:deep(.el-tree-node__content) {
  height: auto;
  padding: 4px 8px;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: #eff6ff;
}
</style>
