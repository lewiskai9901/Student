<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 搜索栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">课程编码</label>
          <input v-model="queryParams.courseCode" type="text" placeholder="请输入课程编码" class="h-9 w-36 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">课程名称</label>
          <input v-model="queryParams.courseName" type="text" placeholder="请输入课程名称" class="h-9 w-36 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">课程类型</label>
          <select v-model="queryParams.courseType" class="h-9 w-28 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
            <option value="">全部</option>
            <option value="required">必修课</option>
            <option value="elective">选修课</option>
            <option value="practice">实践课</option>
            <option value="general">通识课</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">学期</label>
          <select v-model="queryParams.semesterId" class="h-9 w-44 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
            <option :value="undefined">全部</option>
            <option v-for="item in semesterList" :key="item.id" :value="item.id">{{ item.semesterName }}</option>
          </select>
        </div>
        <div class="flex gap-2">
          <button class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleSearch">查询</button>
          <button class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-700 hover:bg-gray-50" @click="handleReset">重置</button>
        </div>
      </div>
    </div>

    <!-- 表格区域 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <h3 class="font-medium text-gray-900">课程列表</h3>
        <div class="flex gap-2">
          <button class="flex h-9 items-center gap-1 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleAdd">
            <Plus class="h-4 w-4" />新增课程
          </button>
          <button class="flex h-9 items-center gap-1 rounded-lg border border-gray-200 px-4 text-sm text-gray-700 hover:bg-gray-50" @click="handleImport">
            <Upload class="h-4 w-4" />导入
          </button>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">课程编码</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">课程名称</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">课程类型</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">学分</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">学时</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">开课院系</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">授课教师</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">学期</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">状态</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-if="loading"><td colspan="10" class="px-4 py-8 text-center text-sm text-gray-500">加载中...</td></tr>
            <tr v-else-if="tableData.length === 0"><td colspan="10" class="px-4 py-8 text-center text-sm text-gray-500">暂无数据</td></tr>
            <tr v-for="row in tableData" :key="row.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.courseCode }}</td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.courseName }}</td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ getCourseTypeText(row.courseType) }}</td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.credit }}</td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.hours }}</td>
              <td class="px-4 py-3 text-sm text-gray-500">{{ row.departmentName || '-' }}</td>
              <td class="px-4 py-3 text-sm text-gray-500">{{ row.teacherName || '-' }}</td>
              <td class="px-4 py-3 text-sm text-gray-500">{{ row.semesterName }}</td>
              <td class="px-4 py-3 text-center">
                <span :class="['inline-flex rounded-full px-2 py-0.5 text-xs', row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700']">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="px-4 py-3 text-center">
                <div class="flex items-center justify-center gap-2">
                  <button class="text-sm text-blue-600 hover:text-blue-700" @click="handleEdit(row)">编辑</button>
                  <button class="text-sm text-red-600 hover:text-red-700" @click="handleDelete(row)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <span class="text-sm text-gray-500">共 {{ total }} 条</span>
        <div class="flex items-center gap-2">
          <button class="h-8 w-8 rounded border border-gray-200 text-sm disabled:opacity-50" :disabled="queryParams.pageNum <= 1" @click="queryParams.pageNum--; fetchData()">
            <ChevronLeft class="mx-auto h-4 w-4" />
          </button>
          <span class="text-sm text-gray-700">{{ queryParams.pageNum }} / {{ Math.ceil(total / queryParams.pageSize) || 1 }}</span>
          <button class="h-8 w-8 rounded border border-gray-200 text-sm disabled:opacity-50" :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)" @click="queryParams.pageNum++; fetchData()">
            <ChevronRight class="mx-auto h-4 w-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="dialogVisible = false"></div>
        <div class="relative w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
          <div class="mb-6 flex items-center justify-between">
            <h3 class="text-lg font-medium text-gray-900">{{ dialogType === 'add' ? '新增课程' : '编辑课程' }}</h3>
            <button class="text-gray-400 hover:text-gray-500" @click="dialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <form @submit.prevent="handleSubmit" class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1 block text-sm text-gray-700">课程编码<span class="text-red-500">*</span></label>
                <input v-model="formData.courseCode" type="text" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" placeholder="请输入课程编码" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">课程名称<span class="text-red-500">*</span></label>
                <input v-model="formData.courseName" type="text" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" placeholder="请输入课程名称" />
              </div>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">课程类型<span class="text-red-500">*</span></label>
              <select v-model="formData.courseType" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                <option value="">请选择类型</option>
                <option value="required">必修课</option>
                <option value="elective">选修课</option>
                <option value="practice">实践课</option>
                <option value="general">通识课</option>
              </select>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1 block text-sm text-gray-700">学分<span class="text-red-500">*</span></label>
                <input v-model.number="formData.credit" type="number" min="0" max="20" step="0.5" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">学时</label>
                <input v-model.number="formData.hours" type="number" min="0" max="200" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">学期<span class="text-red-500">*</span></label>
              <select v-model="formData.semesterId" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                <option :value="undefined" disabled>请选择学期</option>
                <option v-for="item in semesterList" :key="item.id" :value="item.id">{{ item.semesterName }}</option>
              </select>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">开课院系</label>
              <select v-model="formData.departmentId" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                <option :value="undefined">请选择院系</option>
                <option v-for="item in departmentList" :key="item.id" :value="item.id">{{ item.deptName }}</option>
              </select>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">授课教师</label>
              <input v-model="formData.teacherName" type="text" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" placeholder="请输入授课教师" />
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">描述</label>
              <textarea v-model="formData.description" rows="3" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" placeholder="请输入课程描述"></textarea>
            </div>
            <div class="flex justify-end gap-3 pt-4">
              <button type="button" class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-700 hover:bg-gray-50" @click="dialogVisible = false">取消</button>
              <button type="submit" :disabled="submitLoading" class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50">
                {{ submitLoading ? '保存中...' : '确定' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, X, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { pageCourses, createCourse, updateCourse, deleteCourse, listAllSemesters, type Course, type Semester } from '@/api/v2/evaluation'
import { getDepartmentList } from '@/api/v2/organization'

const queryParams = reactive({ pageNum: 1, pageSize: 10, courseCode: '', courseName: '', courseType: '', semesterId: undefined as number | undefined })
const loading = ref(false), tableData = ref<Course[]>([]), total = ref(0), dialogVisible = ref(false), dialogType = ref<'add' | 'edit'>('add'), submitLoading = ref(false)
const semesterList = ref<Semester[]>([]), departmentList = ref<{ id: number; deptName: string }[]>([])
const formData = reactive({ id: undefined as number | undefined, courseCode: '', courseName: '', courseType: '', credit: 2, hours: 32, semesterId: undefined as number | undefined, departmentId: undefined as number | undefined, teacherName: '', description: '' })

const getCourseTypeText = (type: string) => {
  const map: Record<string, string> = { required: '必修课', elective: '选修课', practice: '实践课', general: '通识课' }
  return map[type] || type
}

const fetchData = async () => {
  loading.value = true
  try { const res = await pageCourses(queryParams); tableData.value = res.data?.records || []; total.value = res.data?.total || 0 } finally { loading.value = false }
}
const handleSearch = () => { queryParams.pageNum = 1; fetchData() }
const handleReset = () => { queryParams.courseCode = ''; queryParams.courseName = ''; queryParams.courseType = ''; queryParams.semesterId = undefined; handleSearch() }

const handleAdd = () => { dialogType.value = 'add'; resetForm(); dialogVisible.value = true }
const handleEdit = (row: Course) => {
  dialogType.value = 'edit'
  Object.assign(formData, { id: row.id, courseCode: row.courseCode, courseName: row.courseName, courseType: row.courseType, credit: row.credit, hours: row.hours, semesterId: row.semesterId, departmentId: row.departmentId, teacherName: row.teacherName, description: row.description })
  dialogVisible.value = true
}
const handleDelete = async (row: Course) => {
  try { await ElMessageBox.confirm('确定要删除该课程吗？', '提示', { type: 'warning' }); await deleteCourse(row.id!); ElMessage.success('删除成功'); fetchData() } catch {}
}
const handleImport = () => { ElMessage.info('导入功能开发中') }

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    const data: Course = { courseCode: formData.courseCode, courseName: formData.courseName, courseType: formData.courseType, credit: formData.credit, hours: formData.hours, semesterId: formData.semesterId, departmentId: formData.departmentId, teacherName: formData.teacherName, description: formData.description }
    if (dialogType.value === 'add') { await createCourse(data); ElMessage.success('新增成功') } else { await updateCourse(formData.id!, data); ElMessage.success('更新成功') }
    dialogVisible.value = false; fetchData()
  } finally { submitLoading.value = false }
}

const resetForm = () => { formData.id = undefined; formData.courseCode = ''; formData.courseName = ''; formData.courseType = ''; formData.credit = 2; formData.hours = 32; formData.semesterId = undefined; formData.departmentId = undefined; formData.teacherName = ''; formData.description = '' }

const loadSemesterList = async () => { try { const res = await listAllSemesters(); semesterList.value = res.data || [] } catch (e) { console.error('加载学期列表失败', e) } }
const loadDepartmentList = async () => { try { const res = await getDepartmentList(); departmentList.value = (res || []).map((d: any) => ({ id: d.id, deptName: d.deptName })) } catch (e) { console.error('加载院系列表失败', e) } }

onMounted(() => { fetchData(); loadSemesterList(); loadDepartmentList() })
</script>
