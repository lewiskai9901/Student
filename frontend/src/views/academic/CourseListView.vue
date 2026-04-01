<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">课程管理</h1>
        <p class="mt-0.5 text-sm text-gray-500">管理全校课程信息</p>
      </div>
      <button
        class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
        @click="showDialog()"
      >
        新建课程
      </button>
    </div>

    <!-- Stats bar -->
    <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
      <span class="text-sm text-gray-500">课程总数 <span class="font-semibold text-gray-900">{{ total }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">启用 <span class="font-semibold text-gray-900">{{ activeCount }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">停用 <span class="font-semibold text-gray-900">{{ inactiveCount }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">必修 <span class="font-semibold text-gray-900">{{ requiredCount }}</span></span>
      <div class="h-3 w-px bg-gray-200" />
      <span class="text-sm text-gray-500">选修 <span class="font-semibold text-gray-900">{{ electiveCount }}</span></span>
    </div>

    <!-- Filter row -->
    <div class="flex items-center gap-3 border-b border-gray-200 bg-white px-6 py-3">
      <el-input
        v-model="queryParams.keyword"
        placeholder="课程名称/编码"
        clearable
        class="!w-56"
        @keyup.enter="search"
      />
      <el-select v-model="queryParams.courseType" placeholder="课程类型" clearable class="!w-32">
        <el-option :value="1" label="必修课" />
        <el-option :value="2" label="选修课" />
        <el-option :value="3" label="通识课" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-28">
        <el-option :value="1" label="启用" />
        <el-option :value="0" label="停用" />
      </el-select>
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
      <div class="overflow-hidden rounded-xl border border-gray-200 bg-white">
        <el-table :data="courses" v-loading="loading" stripe>
          <el-table-column prop="code" label="课程编码" width="120" />
          <el-table-column prop="name" label="课程名称" min-width="150" />
          <el-table-column prop="courseType" label="课程类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="getCourseTypeTag(row.courseType)">
                {{ getCourseTypeName(row.courseType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="credits" label="学分" width="80" align="center" />
          <el-table-column prop="totalHours" label="总学时" width="80" align="center" />
          <el-table-column label="理论/实践" width="100" align="center">
            <template #default="{ row }">
              {{ row.theoryHours }} / {{ row.practiceHours }}
            </template>
          </el-table-column>
          <el-table-column prop="departmentName" label="开课部门" width="120" />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <span
                class="inline-block rounded px-1.5 py-0.5 text-xs font-medium"
                :class="row.status === 1 ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-100 text-gray-500'"
              >
                {{ row.status === 1 ? '启用' : '停用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text @click="showDialog(row)">编辑</el-button>
              <el-button
                size="small"
                text
                :type="row.status === 1 ? 'warning' : 'success'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
              <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadCourses"
          @current-change="loadCourses"
        />
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑课程' : '新建课程'"
      width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程编码" prop="code">
              <el-input v-model="form.code" placeholder="如：CS101" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程名称" prop="name">
              <el-input v-model="form.name" placeholder="课程名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="英文名称">
              <el-input v-model="form.englishName" placeholder="English Name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程类型" prop="courseType">
              <el-select v-model="form.courseType" style="width: 100%">
                <el-option :value="1" label="必修课" />
                <el-option :value="2" label="选修课" />
                <el-option :value="3" label="通识课" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学分" prop="credits">
              <el-input-number v-model="form.credits" :min="0.5" :max="20" :step="0.5" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总学时" prop="totalHours">
              <el-input-number v-model="form.totalHours" :min="1" :max="500" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="理论学时">
              <el-input-number v-model="form.theoryHours" :min="0" :max="form.totalHours" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实践学时">
              <el-input-number v-model="form.practiceHours" :min="0" :max="form.totalHours" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="课程描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="课程简介..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { courseApi } from '@/api/academic'
import type { Course, CourseQueryParams } from '@/types/academic'

// 状态
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const courses = ref<Course[]>([])
const total = ref(0)

const queryParams = reactive<CourseQueryParams>({
  keyword: '',
  courseType: undefined,
  status: undefined,
  page: 1,
  size: 10,
})

const formRef = ref<FormInstance>()
const form = ref<Partial<Course>>({})

const rules: FormRules = {
  code: [{ required: true, message: '请输入课程编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseType: [{ required: true, message: '请选择课程类型', trigger: 'change' }],
  credits: [{ required: true, message: '请输入学分', trigger: 'blur' }],
  totalHours: [{ required: true, message: '请输入总学时', trigger: 'blur' }],
}

// 统计数据 (computed from current page data)
const activeCount = computed(() => courses.value.filter(c => c.status === 1).length)
const inactiveCount = computed(() => courses.value.filter(c => c.status === 0).length)
const requiredCount = computed(() => courses.value.filter(c => c.courseType === 1).length)
const electiveCount = computed(() => courses.value.filter(c => c.courseType === 2).length)

// 方法
const loadCourses = async () => {
  loading.value = true
  try {
    const res: any = await courseApi.list(queryParams)
    const data = res.data || res
    courses.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('Failed to load courses:', error)
  } finally {
    loading.value = false
  }
}

const search = () => {
  queryParams.page = 1
  loadCourses()
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.courseType = undefined
  queryParams.status = undefined
  queryParams.page = 1
  loadCourses()
}

const showDialog = (course?: Course) => {
  form.value = course
    ? { ...course }
    : { credits: 2, totalHours: 32, theoryHours: 24, practiceHours: 8, courseType: 1, status: 1 }
  dialogVisible.value = true
}

const saveCourse = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.value.id) {
      await courseApi.update(form.value.id, form.value)
    } else {
      await courseApi.create(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadCourses()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (course: Course) => {
  const newStatus = course.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确定${action}课程"${course.name}"吗？`, '提示', { type: 'warning' })
  try {
    await courseApi.updateStatus(course.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadCourses()
  } catch (error) {
    ElMessage.error(`${action}失败`)
  }
}

const handleDelete = async (course: Course) => {
  await ElMessageBox.confirm(`确定删除课程"${course.name}"吗？此操作不可恢复。`, '警告', {
    type: 'warning',
    confirmButtonText: '确定删除',
    confirmButtonClass: 'el-button--danger',
  })
  try {
    await courseApi.delete(course.id)
    ElMessage.success('删除成功')
    loadCourses()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const getCourseTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '必修课', 2: '选修课', 3: '通识课' }
  return names[type] || '未知'
}

const getCourseTypeTag = (type: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    1: 'danger',
    2: 'success',
    3: 'info',
  }
  return types[type] || 'info'
}

onMounted(() => {
  loadCourses()
})
</script>
