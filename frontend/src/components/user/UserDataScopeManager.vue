<!--
  @deprecated 此组件已废弃
  用户级数据范围配置已废弃，请使用角色级数据权限配置
  数据权限现在通过 RolesView.vue 中的角色数据权限配置功能管理
  此组件将在未来版本中删除
-->
<template>
  <div class="user-data-scope-manager">
    <!-- 标题和操作栏 -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <Database class="h-5 w-5 text-blue-600" />
        <h3 class="text-base font-semibold text-gray-900">数据范围</h3>
        <span class="text-sm text-gray-500">(决定用户能访问哪些数据)</span>
      </div>
      <button
        @click="showAddDialog = true"
        class="flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 transition-colors"
      >
        <Plus class="h-4 w-4" />
        添加范围
      </button>
    </div>

    <!-- 数据范围列表 -->
    <div v-if="loading" class="flex items-center justify-center py-8">
      <Loader2 class="h-6 w-6 animate-spin text-blue-600" />
      <span class="ml-2 text-gray-500">加载中...</span>
    </div>

    <div v-else-if="scopes.length === 0" class="text-center py-8 text-gray-500">
      <Database class="h-12 w-12 mx-auto text-gray-300 mb-2" />
      <p>暂未配置数据范围</p>
      <p class="text-sm mt-1">点击"添加范围"按钮配置用户可访问的数据</p>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="scope in scopes"
        :key="scope.id"
        class="flex items-center justify-between p-4 bg-gray-50 rounded-lg border border-gray-200 hover:border-blue-300 transition-colors"
      >
        <div class="flex items-center gap-3">
          <!-- 范围类型图标 -->
          <div
            class="flex h-10 w-10 items-center justify-center rounded-lg"
            :class="getScopeTypeStyle(scope.scopeType).bgClass"
          >
            <component :is="getScopeTypeStyle(scope.scopeType).icon" class="h-5 w-5" :class="getScopeTypeStyle(scope.scopeType).iconClass" />
          </div>

          <div>
            <div class="flex items-center gap-2">
              <span class="font-medium text-gray-900">{{ scope.displayName || scope.scopeExpression }}</span>
              <span
                class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
                :class="getScopeTypeStyle(scope.scopeType).badgeClass"
              >
                {{ getScopeTypeName(scope.scopeType) }}
              </span>
            </div>
            <div class="text-sm text-gray-500 mt-0.5">
              <code class="text-xs bg-gray-100 px-1.5 py-0.5 rounded">{{ scope.scopeExpression }}</code>
              <span v-if="scope.assignedByName" class="ml-2">由 {{ scope.assignedByName }} </span>
              <span v-if="scope.assignedAt">于 {{ formatDate(scope.assignedAt) }} 添加</span>
            </div>
          </div>
        </div>

        <button
          @click="handleDelete(scope)"
          class="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          title="删除"
        >
          <Trash2 class="h-4 w-4" />
        </button>
      </div>
    </div>

    <!-- 添加范围弹窗 -->
    <Teleport to="body">
      <div
        v-if="showAddDialog"
        class="fixed inset-0 z-50 flex items-center justify-center"
      >
        <div class="fixed inset-0 bg-black/50" @click="showAddDialog = false"></div>
        <div class="relative w-full max-w-lg rounded-xl bg-white shadow-xl">
          <div class="flex items-center justify-between border-b border-gray-200 px-5 py-4">
            <h3 class="text-lg font-semibold text-gray-900">添加数据范围</h3>
            <button
              @click="showAddDialog = false"
              class="text-gray-400 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="p-5 space-y-4">
            <!-- 范围类型选择 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">范围类型</label>
              <div class="grid grid-cols-3 gap-2">
                <button
                  v-for="type in scopeTypeOptions"
                  :key="type.code"
                  @click="selectScopeType(type.code)"
                  class="flex flex-col items-center gap-1.5 p-3 rounded-lg border-2 transition-all"
                  :class="addForm.scopeType === type.code
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'"
                >
                  <component
                    :is="getScopeTypeStyle(type.code).icon"
                    class="h-5 w-5"
                    :class="addForm.scopeType === type.code ? 'text-blue-600' : 'text-gray-400'"
                  />
                  <span
                    class="text-xs font-medium"
                    :class="addForm.scopeType === type.code ? 'text-blue-600' : 'text-gray-600'"
                  >
                    {{ type.name }}
                  </span>
                </button>
              </div>
            </div>

            <!-- 部门选择 (DEPT 和 DEPT_GRADE) -->
            <div v-if="addForm.scopeType === 'DEPT' || addForm.scopeType === 'DEPT_GRADE'">
              <label class="block text-sm font-medium text-gray-700 mb-2">选择部门</label>
              <select
                v-model="addForm.deptId"
                class="w-full h-10 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              >
                <option :value="null" disabled>请选择部门</option>
                <option
                  v-for="item in flatDepartments"
                  :key="item.id"
                  :value="item.id"
                >
                  {{ item.name }}
                </option>
              </select>
            </div>

            <!-- 年级选择 (GRADE 和 DEPT_GRADE) -->
            <div v-if="addForm.scopeType === 'GRADE' || addForm.scopeType === 'DEPT_GRADE'">
              <label class="block text-sm font-medium text-gray-700 mb-2">选择年级</label>
              <select
                v-model="addForm.gradeId"
                class="w-full h-10 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              >
                <option :value="null" disabled>请选择年级</option>
                <option
                  v-for="item in grades"
                  :key="item.id"
                  :value="item.id"
                >
                  {{ item.gradeName }}
                </option>
              </select>
            </div>

            <!-- 班级选择 (CLASS) -->
            <div v-if="addForm.scopeType === 'CLASS'">
              <label class="block text-sm font-medium text-gray-700 mb-2">选择班级</label>
              <select
                v-model="addForm.classId"
                class="w-full h-10 rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              >
                <option :value="null" disabled>请选择班级</option>
                <option
                  v-for="item in classes"
                  :key="item.id"
                  :value="item.id"
                >
                  {{ item.className }}
                </option>
              </select>
            </div>

            <!-- 预览生成的范围表达式 -->
            <div v-if="previewExpression" class="p-3 bg-gray-50 rounded-lg">
              <div class="text-xs text-gray-500 mb-1">范围表达式预览:</div>
              <code class="text-sm font-mono text-blue-600">{{ previewExpression }}</code>
            </div>
          </div>

          <div class="flex justify-end gap-3 border-t border-gray-200 px-5 py-4">
            <button
              @click="showAddDialog = false"
              class="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
            >
              取消
            </button>
            <button
              @click="handleAdd"
              :disabled="!canSubmit || submitting"
              class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Loader2 v-if="submitting" class="h-4 w-4 animate-spin inline mr-1" />
              确定
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Database,
  Plus,
  Trash2,
  X,
  Loader2,
  Building2,
  GraduationCap,
  Users,
  Globe,
  User,
  Layers
} from 'lucide-vue-next'
import {
  getUserScopes,
  assignScope,
  revokeScope,
  buildScopeExpression,
  getScopeTypeName as getTypeName,
  type ScopeAssignmentDTO,
  type ScopeTypeCode
} from '@/api/v2/quantification-extra'
import { getDepartmentTree } from '@/api/v2/organization'
import { getAllGrades } from '@/api/v2/organization'
import { getAllClasses } from '@/api/v2/organization'

const props = defineProps<{
  userId: number
}>()

const loading = ref(false)
const submitting = ref(false)
const showAddDialog = ref(false)
const scopes = ref<ScopeAssignmentDTO[]>([])

// 添加表单
const addForm = reactive({
  scopeType: '' as ScopeTypeCode | '',
  deptId: null as number | null,
  gradeId: null as number | null,
  classId: null as number | null
})

// 范围类型选项
const scopeTypeOptions: { code: ScopeTypeCode; name: string }[] = [
  { code: 'ALL', name: '全部数据' },
  { code: 'DEPT', name: '部门' },
  { code: 'GRADE', name: '年级' },
  { code: 'DEPT_GRADE', name: '部门+年级' },
  { code: 'CLASS', name: '班级' },
  { code: 'SELF', name: '仅本人' }
]

// 范围选项数据
const departments = ref<any[]>([])
const grades = ref<any[]>([])
const classes = ref<any[]>([])

// 扁平化部门列表
const flatDepartments = computed(() => {
  return flattenDepartments(departments.value)
})

// 预览表达式
const previewExpression = computed(() => {
  if (!addForm.scopeType) return ''

  switch (addForm.scopeType) {
    case 'ALL':
      return 'scope:*'
    case 'SELF':
      return 'scope:self'
    case 'DEPT':
      return addForm.deptId ? buildScopeExpression('DEPT', addForm.deptId) : ''
    case 'GRADE':
      return addForm.gradeId ? buildScopeExpression('GRADE', addForm.gradeId) : ''
    case 'DEPT_GRADE':
      return (addForm.deptId && addForm.gradeId)
        ? buildScopeExpression('DEPT_GRADE', addForm.deptId, addForm.gradeId)
        : ''
    case 'CLASS':
      return addForm.classId ? buildScopeExpression('CLASS', addForm.classId) : ''
    default:
      return ''
  }
})

// 是否可以提交
const canSubmit = computed(() => {
  if (!addForm.scopeType) return false

  switch (addForm.scopeType) {
    case 'ALL':
    case 'SELF':
      return true
    case 'DEPT':
      return !!addForm.deptId
    case 'GRADE':
      return !!addForm.gradeId
    case 'DEPT_GRADE':
      return !!addForm.deptId && !!addForm.gradeId
    case 'CLASS':
      return !!addForm.classId
    default:
      return false
  }
})

// 选择范围类型时重置相关字段
function selectScopeType(type: ScopeTypeCode) {
  addForm.scopeType = type
  addForm.deptId = null
  addForm.gradeId = null
  addForm.classId = null
}

// 扁平化部门树
function flattenDepartments(depts: any[], level = 0): any[] {
  const result: any[] = []
  for (const dept of depts) {
    result.push({
      id: dept.id,
      name: '\u3000'.repeat(level) + dept.deptName
    })
    if (dept.children && dept.children.length > 0) {
      result.push(...flattenDepartments(dept.children, level + 1))
    }
  }
  return result
}

// 获取范围类型样式
function getScopeTypeStyle(type: ScopeTypeCode | string) {
  switch (type) {
    case 'ALL':
      return {
        icon: Globe,
        bgClass: 'bg-amber-100',
        iconClass: 'text-amber-600',
        badgeClass: 'bg-amber-100 text-amber-700'
      }
    case 'DEPT':
    case 'DEPARTMENT':
      return {
        icon: Building2,
        bgClass: 'bg-purple-100',
        iconClass: 'text-purple-600',
        badgeClass: 'bg-purple-100 text-purple-700'
      }
    case 'GRADE':
      return {
        icon: GraduationCap,
        bgClass: 'bg-blue-100',
        iconClass: 'text-blue-600',
        badgeClass: 'bg-blue-100 text-blue-700'
      }
    case 'DEPT_GRADE':
      return {
        icon: Layers,
        bgClass: 'bg-indigo-100',
        iconClass: 'text-indigo-600',
        badgeClass: 'bg-indigo-100 text-indigo-700'
      }
    case 'CLASS':
      return {
        icon: Users,
        bgClass: 'bg-green-100',
        iconClass: 'text-green-600',
        badgeClass: 'bg-green-100 text-green-700'
      }
    case 'SELF':
      return {
        icon: User,
        bgClass: 'bg-gray-100',
        iconClass: 'text-gray-600',
        badgeClass: 'bg-gray-100 text-gray-700'
      }
    default:
      return {
        icon: Database,
        bgClass: 'bg-gray-100',
        iconClass: 'text-gray-600',
        badgeClass: 'bg-gray-100 text-gray-700'
      }
  }
}

// 获取范围类型名称
function getScopeTypeName(type: ScopeTypeCode | string) {
  return getTypeName(type as ScopeTypeCode)
}

// 获取显示名称
function getDisplayName(): string {
  switch (addForm.scopeType) {
    case 'ALL':
      return '全部数据'
    case 'SELF':
      return '仅本人数据'
    case 'DEPT':
      const dept = flatDepartments.value.find(d => d.id === addForm.deptId)
      return dept?.name?.trim() || ''
    case 'GRADE':
      const grade = grades.value.find(g => g.id === addForm.gradeId)
      return grade?.gradeName || ''
    case 'DEPT_GRADE':
      const d = flatDepartments.value.find(d => d.id === addForm.deptId)
      const g = grades.value.find(g => g.id === addForm.gradeId)
      return `${d?.name?.trim() || ''} - ${g?.gradeName || ''}`
    case 'CLASS':
      const cls = classes.value.find(c => c.id === addForm.classId)
      return cls?.className || ''
    default:
      return ''
  }
}

// 格式化日期
function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

// 加载用户数据范围
async function loadScopes() {
  loading.value = true
  try {
    const data = await getUserScopes(props.userId)
    scopes.value = data || []
  } catch (error) {
    console.error('加载数据范围失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载选项数据
async function loadOptions() {
  try {
    const [deptData, gradeData, classData] = await Promise.all([
      getDepartmentTree(),
      getAllGrades(),
      getAllClasses()
    ])
    departments.value = deptData || []
    grades.value = gradeData || []
    classes.value = classData || []
  } catch (error) {
    console.error('加载选项数据失败:', error)
  }
}

// 添加数据范围
async function handleAdd() {
  if (!addForm.scopeType || !previewExpression.value) {
    ElMessage.warning('请选择范围类型和具体范围')
    return
  }

  submitting.value = true
  try {
    await assignScope({
      userId: props.userId,
      scopeType: addForm.scopeType,
      scopeExpression: previewExpression.value,
      displayName: getDisplayName()
    })
    ElMessage.success('添加成功')
    showAddDialog.value = false
    resetForm()
    loadScopes()
  } catch (error: any) {
    ElMessage.error(error.message || '添加失败')
  } finally {
    submitting.value = false
  }
}

// 重置表单
function resetForm() {
  addForm.scopeType = ''
  addForm.deptId = null
  addForm.gradeId = null
  addForm.classId = null
}

// 删除数据范围
async function handleDelete(scope: ScopeAssignmentDTO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除数据范围【${scope.displayName || scope.scopeExpression}】吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await revokeScope({
      userId: props.userId,
      scopeExpression: scope.scopeExpression
    })
    ElMessage.success('删除成功')
    loadScopes()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 监听用户ID变化
watch(() => props.userId, (newUserId) => {
  if (newUserId) {
    loadScopes()
  }
}, { immediate: true })

onMounted(() => {
  loadOptions()
})
</script>

<style scoped>
.user-data-scope-manager {
  min-height: 200px;
}
</style>
