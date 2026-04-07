<template>
  <div class="p-6">
    <div class="mb-4 flex items-center justify-between">
      <h3 class="text-base font-semibold text-gray-900">班级学生列表</h3>
      <button class="inline-flex h-8 items-center gap-1.5 rounded-lg bg-blue-600 px-3 text-sm text-white hover:bg-blue-700" @click="handleAddStudent">
        <Plus class="h-4 w-4" />
        添加学生
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
    </div>

    <!-- 表格 -->
    <div v-else class="overflow-hidden rounded-lg border border-gray-200">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">学号</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">姓名</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">性别</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">联系电话</th>
            <th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">宿舍</th>
            <th class="px-4 py-3 text-center text-xs font-medium uppercase tracking-wider text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200 bg-white">
          <tr v-for="row in studentList" :key="row.id" class="hover:bg-gray-50">
            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-900">{{ row.studentNo }}</td>
            <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">{{ row.name }}</td>
            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">{{ row.gender === 1 ? '男' : '女' }}</td>
            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">{{ row.phone || '-' }}</td>
            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">{{ row.dormitoryInfo || '-' }}</td>
            <td class="whitespace-nowrap px-4 py-3 text-center">
              <button class="text-sm text-blue-600 hover:text-blue-800" @click="handleView(row)">查看</button>
              <button class="ml-3 text-sm text-red-600 hover:text-red-800" @click="handleRemove(row)">移除</button>
            </td>
          </tr>
          <tr v-if="studentList.length === 0">
            <td colspan="6" class="px-4 py-12 text-center text-sm text-gray-500">暂无学生数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 操作按钮 -->
    <div class="mt-6 flex justify-end border-t border-gray-200 pt-4">
      <button class="h-9 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50" @click="$emit('close')">关闭</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from 'lucide-vue-next'

interface Props { orgUnitId: number | null }
const props = defineProps<Props>()
const emit = defineEmits<{ close: [] }>()

const loading = ref(false)
const studentList = ref<any[]>([])

const loadStudentList = async () => {
  if (!props.orgUnitId) return
  loading.value = true
  try {
    // 模拟数据
    studentList.value = [
      { id: 1, studentNo: '2021001', name: '张三', gender: 1, phone: '13800138001', dormitoryInfo: '1号楼201' },
      { id: 2, studentNo: '2021002', name: '李四', gender: 2, phone: '13800138002', dormitoryInfo: '1号楼202' }
    ]
  } catch (error) {
    ElMessage.error('加载学生列表失败')
  } finally {
    loading.value = false
  }
}

const handleAddStudent = () => { ElMessage.info('添加学生功能开发中...') }
const handleView = (row: any) => { ElMessage.info('查看学生功能开发中...') }
const handleRemove = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要将学生 "${row.name}" 从班级中移除吗？`, '移除确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    ElMessage.success('学生移除成功')
    loadStudentList()
  } catch (error) { /* 用户取消 */ }
}

onMounted(() => { loadStudentList() })
</script>
