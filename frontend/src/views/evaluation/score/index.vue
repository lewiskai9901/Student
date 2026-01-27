<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 搜索栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">学号</label>
          <input v-model="queryParams.studentNo" type="text" placeholder="请输入学号" class="h-9 w-32 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">姓名</label>
          <input v-model="queryParams.studentName" type="text" placeholder="请输入姓名" class="h-9 w-28 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @keyup.enter="handleSearch" />
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">课程</label>
          <select v-model="queryParams.courseId" class="h-9 w-40 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
            <option :value="undefined">全部</option>
            <option v-for="item in courseList" :key="item.id" :value="item.id">{{ item.courseName }}</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label class="text-sm text-gray-700">班级</label>
          <select v-model="queryParams.classId" class="h-9 w-40 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
            <option :value="undefined">全部</option>
            <option v-for="item in classList" :key="item.id" :value="item.id">{{ item.className }}</option>
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
        <h3 class="font-medium text-gray-900">成绩列表</h3>
        <div class="flex gap-2">
          <button class="flex h-9 items-center gap-1 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700" @click="handleAdd"><Plus class="h-4 w-4" />录入成绩</button>
          <button class="flex h-9 items-center gap-1 rounded-lg bg-green-600 px-4 text-sm text-white hover:bg-green-700" @click="handleBatchInput"><FileSpreadsheet class="h-4 w-4" />批量录入</button>
          <button class="flex h-9 items-center gap-1 rounded-lg border border-gray-200 px-4 text-sm text-gray-700 hover:bg-gray-50" @click="handleImport"><Upload class="h-4 w-4" />导入</button>
          <button class="flex h-9 items-center gap-1 rounded-lg border border-gray-200 px-4 text-sm text-gray-700 hover:bg-gray-50" @click="handleExport"><Download class="h-4 w-4" />导出</button>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">学号</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">姓名</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">班级</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">课程</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">学分</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">平时</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">期中</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">期末</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">总评</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">绩点</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">锁定</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-if="loading"><td colspan="12" class="px-4 py-8 text-center text-sm text-gray-500">加载中...</td></tr>
            <tr v-else-if="tableData.length === 0"><td colspan="12" class="px-4 py-8 text-center text-sm text-gray-500">暂无数据</td></tr>
            <tr v-for="row in tableData" :key="row.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.studentNo }}</td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.studentName }}</td>
              <td class="px-4 py-3 text-sm text-gray-500">{{ row.className }}</td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.courseName }}</td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.credit }}</td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.usualScore ?? '-' }}</td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.midtermScore ?? '-' }}</td>
              <td class="px-4 py-3 text-center"><span :class="getScoreClass(row.finalScore)">{{ row.finalScore ?? '-' }}</span></td>
              <td class="px-4 py-3 text-center"><span :class="['font-semibold', getScoreClass(row.totalScore)]">{{ row.totalScore ?? '-' }}</span></td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.gpa?.toFixed(1) ?? '-' }}</td>
              <td class="px-4 py-3 text-center">
                <span :class="['inline-flex rounded-full px-2 py-0.5 text-xs', row.isLocked === 1 ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700']">{{ row.isLocked === 1 ? '已锁定' : '未锁定' }}</span>
              </td>
              <td class="px-4 py-3 text-center">
                <div class="flex items-center justify-center gap-2">
                  <button class="text-sm text-blue-600 hover:text-blue-700 disabled:opacity-50" :disabled="row.isLocked === 1" @click="handleEdit(row)">编辑</button>
                  <button class="text-sm text-red-600 hover:text-red-700 disabled:opacity-50" :disabled="row.isLocked === 1" @click="handleDelete(row)">删除</button>
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
          <button class="h-8 w-8 rounded border border-gray-200 text-sm disabled:opacity-50" :disabled="queryParams.pageNum <= 1" @click="queryParams.pageNum--; fetchData()"><ChevronLeft class="mx-auto h-4 w-4" /></button>
          <span class="text-sm text-gray-700">{{ queryParams.pageNum }} / {{ Math.ceil(total / queryParams.pageSize) || 1 }}</span>
          <button class="h-8 w-8 rounded border border-gray-200 text-sm disabled:opacity-50" :disabled="queryParams.pageNum >= Math.ceil(total / queryParams.pageSize)" @click="queryParams.pageNum++; fetchData()"><ChevronRight class="mx-auto h-4 w-4" /></button>
        </div>
      </div>
    </div>

    <!-- 录入/编辑对话框 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="dialogVisible = false"></div>
        <div class="relative w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
          <div class="mb-6 flex items-center justify-between">
            <h3 class="text-lg font-medium text-gray-900">{{ dialogType === 'add' ? '录入成绩' : '编辑成绩' }}</h3>
            <button class="text-gray-400 hover:text-gray-500" @click="dialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <form @submit.prevent="handleSubmit" class="space-y-4">
            <div v-if="dialogType === 'add'">
              <label class="mb-1 block text-sm text-gray-700">学生<span class="text-red-500">*</span></label>
              <input v-model="studentSearch" type="text" placeholder="搜索学号或姓名" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @input="searchStudents" />
              <div v-if="studentOptions.length > 0" class="mt-1 max-h-32 overflow-y-auto rounded border border-gray-200 bg-white">
                <button type="button" v-for="opt in studentOptions" :key="opt.id" class="w-full px-3 py-2 text-left text-sm hover:bg-gray-50" @click="selectStudent(opt)">{{ opt.studentNo }} - {{ opt.realName }}</button>
              </div>
              <div v-if="formData.studentId" class="mt-1 text-xs text-green-600">已选择: {{ selectedStudentName }}</div>
            </div>
            <div v-if="dialogType === 'add'">
              <label class="mb-1 block text-sm text-gray-700">课程<span class="text-red-500">*</span></label>
              <select v-model="formData.courseId" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                <option :value="undefined" disabled>请选择课程</option>
                <option v-for="item in courseList" :key="item.id" :value="item.id">{{ item.courseName }}</option>
              </select>
            </div>
            <div v-if="dialogType === 'add'">
              <label class="mb-1 block text-sm text-gray-700">学期<span class="text-red-500">*</span></label>
              <select v-model="formData.semesterId" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                <option :value="undefined" disabled>请选择学期</option>
                <option v-for="item in semesterList" :key="item.id" :value="item.id">{{ item.semesterName }}</option>
              </select>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div><label class="mb-1 block text-sm text-gray-700">平时成绩</label><input v-model.number="formData.usualScore" type="number" min="0" max="100" step="0.1" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" /></div>
              <div><label class="mb-1 block text-sm text-gray-700">期中成绩</label><input v-model.number="formData.midtermScore" type="number" min="0" max="100" step="0.1" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" /></div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div><label class="mb-1 block text-sm text-gray-700">期末成绩<span class="text-red-500">*</span></label><input v-model.number="formData.finalScore" type="number" min="0" max="100" step="0.1" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" /></div>
              <div><label class="mb-1 block text-sm text-gray-700">总评成绩</label><input v-model.number="formData.totalScore" type="number" min="0" max="100" step="0.1" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" /></div>
            </div>
            <div><label class="mb-1 block text-sm text-gray-700">备注</label><input v-model="formData.remark" type="text" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" placeholder="备注" /></div>
            <div class="flex justify-end gap-3 pt-4">
              <button type="button" class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-700 hover:bg-gray-50" @click="dialogVisible = false">取消</button>
              <button type="submit" :disabled="submitLoading" class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50">{{ submitLoading ? '保存中...' : '确定' }}</button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- 批量录入对话框 -->
    <Teleport to="body">
      <div v-if="batchDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="batchDialogVisible = false"></div>
        <div class="relative w-full max-w-4xl rounded-lg bg-white p-6 shadow-xl max-h-[90vh] overflow-y-auto">
          <div class="mb-4 flex items-center justify-between">
            <h3 class="text-lg font-medium text-gray-900">批量录入成绩</h3>
            <button class="text-gray-400 hover:text-gray-500" @click="batchDialogVisible = false"><X class="h-5 w-5" /></button>
          </div>
          <div class="mb-4 flex flex-wrap items-center gap-4">
            <div class="flex items-center gap-2"><label class="text-sm text-gray-700">班级</label><select v-model="batchClassId" class="h-9 w-40 rounded-lg border border-gray-200 px-3 text-sm" @change="loadBatchStudents"><option :value="undefined" disabled>请选择班级</option><option v-for="item in classList" :key="item.id" :value="item.id">{{ item.className }}</option></select></div>
            <div class="flex items-center gap-2"><label class="text-sm text-gray-700">课程</label><select v-model="batchCourseId" class="h-9 w-40 rounded-lg border border-gray-200 px-3 text-sm"><option :value="undefined" disabled>请选择课程</option><option v-for="item in courseList" :key="item.id" :value="item.id">{{ item.courseName }}</option></select></div>
            <div class="flex items-center gap-2"><label class="text-sm text-gray-700">学期</label><select v-model="batchSemesterId" class="h-9 w-44 rounded-lg border border-gray-200 px-3 text-sm"><option :value="undefined" disabled>请选择学期</option><option v-for="item in semesterList" :key="item.id" :value="item.id">{{ item.semesterName }}</option></select></div>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full"><thead class="bg-gray-50"><tr><th class="px-3 py-2 text-left text-sm font-medium text-gray-700">学号</th><th class="px-3 py-2 text-left text-sm font-medium text-gray-700">姓名</th><th class="px-3 py-2 text-center text-sm font-medium text-gray-700">平时成绩</th><th class="px-3 py-2 text-center text-sm font-medium text-gray-700">期中成绩</th><th class="px-3 py-2 text-center text-sm font-medium text-gray-700">期末成绩</th><th class="px-3 py-2 text-center text-sm font-medium text-gray-700">总评成绩</th></tr></thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="stu in batchStudents" :key="stu.id" class="hover:bg-gray-50">
                  <td class="px-3 py-2 text-sm text-gray-900">{{ stu.studentNo }}</td>
                  <td class="px-3 py-2 text-sm text-gray-900">{{ stu.realName }}</td>
                  <td class="px-3 py-2"><input v-model.number="stu.usualScore" type="number" min="0" max="100" class="h-8 w-20 rounded border border-gray-200 px-2 text-center text-sm" /></td>
                  <td class="px-3 py-2"><input v-model.number="stu.midtermScore" type="number" min="0" max="100" class="h-8 w-20 rounded border border-gray-200 px-2 text-center text-sm" /></td>
                  <td class="px-3 py-2"><input v-model.number="stu.finalScore" type="number" min="0" max="100" class="h-8 w-20 rounded border border-gray-200 px-2 text-center text-sm" /></td>
                  <td class="px-3 py-2"><input v-model.number="stu.totalScore" type="number" min="0" max="100" class="h-8 w-20 rounded border border-gray-200 px-2 text-center text-sm" /></td>
                </tr>
                <tr v-if="batchStudents.length === 0"><td colspan="6" class="px-3 py-8 text-center text-sm text-gray-500">请先选择班级</td></tr>
              </tbody>
            </table>
          </div>
          <div class="mt-4 flex justify-end gap-3">
            <button type="button" class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-700 hover:bg-gray-50" @click="batchDialogVisible = false">取消</button>
            <button type="button" :disabled="batchSubmitLoading" class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50" @click="handleBatchSubmit">{{ batchSubmitLoading ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, FileSpreadsheet, Upload, Download, X, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { pageScores, inputScore, updateScore, deleteScore, batchInputScores, listAllSemesters, getCoursesBySemester, type StudentScore, type Semester, type Course } from '@/api/evaluation'
import { getAllClasses } from '@/api/organization'
import { getStudents } from '@/api/student'

const queryParams = reactive({ pageNum: 1, pageSize: 10, studentNo: '', studentName: '', courseId: undefined as number | undefined, classId: undefined as number | undefined, semesterId: undefined as number | undefined })
const loading = ref(false), tableData = ref<StudentScore[]>([]), total = ref(0), dialogVisible = ref(false), dialogType = ref<'add' | 'edit'>('add'), submitLoading = ref(false)
const courseList = ref<Course[]>([]), classList = ref<{ id: number; className: string }[]>([]), semesterList = ref<Semester[]>([]), studentOptions = ref<{ id: number; studentNo: string; realName: string }[]>([])
const studentSearch = ref(''), selectedStudentName = ref('')
const formData = reactive({ id: undefined as number | undefined, studentId: undefined as number | undefined, courseId: undefined as number | undefined, semesterId: undefined as number | undefined, usualScore: undefined as number | undefined, midtermScore: undefined as number | undefined, finalScore: undefined as number | undefined, totalScore: undefined as number | undefined, remark: '' })
const batchDialogVisible = ref(false), batchSubmitLoading = ref(false), batchClassId = ref<number>(), batchCourseId = ref<number>(), batchSemesterId = ref<number>(), batchStudents = ref<any[]>([])

const getScoreClass = (score: number | undefined) => {
  if (score === undefined || score === null) return 'text-gray-400'
  if (score >= 90) return 'text-green-600'
  if (score >= 80) return 'text-blue-600'
  if (score >= 70) return 'text-amber-600'
  if (score >= 60) return 'text-gray-600'
  return 'text-red-600'
}

const fetchData = async () => { loading.value = true; try { const res = await pageScores(queryParams); tableData.value = res.data?.records || []; total.value = res.data?.total || 0 } finally { loading.value = false } }
const handleSearch = () => { queryParams.pageNum = 1; fetchData() }
const handleReset = () => { queryParams.studentNo = ''; queryParams.studentName = ''; queryParams.courseId = undefined; queryParams.classId = undefined; queryParams.semesterId = undefined; handleSearch() }

const searchStudents = async () => {
  if (studentSearch.value.length >= 2) { try { const res = await getStudents({ pageNum: 1, pageSize: 10, keyword: studentSearch.value }); studentOptions.value = (res.records || []).map((s: any) => ({ id: s.id, studentNo: s.studentNo, realName: s.realName || s.name })) } catch { studentOptions.value = [] } }
  else { studentOptions.value = [] }
}
const selectStudent = (opt: { id: number; studentNo: string; realName: string }) => { formData.studentId = opt.id; selectedStudentName.value = `${opt.studentNo} - ${opt.realName}`; studentOptions.value = []; studentSearch.value = '' }

const handleAdd = () => { dialogType.value = 'add'; resetForm(); dialogVisible.value = true }
const handleEdit = (row: StudentScore) => { dialogType.value = 'edit'; Object.assign(formData, { id: row.id, studentId: row.studentId, courseId: row.courseId, semesterId: row.semesterId, usualScore: row.usualScore, midtermScore: row.midtermScore, finalScore: row.finalScore, totalScore: row.totalScore, remark: row.remark || '' }); dialogVisible.value = true }
const handleDelete = async (row: StudentScore) => { try { await ElMessageBox.confirm('确定要删除该成绩记录吗？', '提示', { type: 'warning' }); await deleteScore(row.id!); ElMessage.success('删除成功'); fetchData() } catch {} }

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    const data: StudentScore = { studentId: formData.studentId!, courseId: formData.courseId!, semesterId: formData.semesterId!, usualScore: formData.usualScore, midtermScore: formData.midtermScore, finalScore: formData.finalScore, totalScore: formData.totalScore, remark: formData.remark }
    if (dialogType.value === 'add') { await inputScore(data); ElMessage.success('录入成功') } else { await updateScore(formData.id!, data); ElMessage.success('更新成功') }
    dialogVisible.value = false; fetchData()
  } finally { submitLoading.value = false }
}

const resetForm = () => { formData.id = undefined; formData.studentId = undefined; formData.courseId = undefined; formData.semesterId = undefined; formData.usualScore = undefined; formData.midtermScore = undefined; formData.finalScore = undefined; formData.totalScore = undefined; formData.remark = ''; selectedStudentName.value = '' }

const handleBatchInput = () => { batchStudents.value = []; batchDialogVisible.value = true }
const loadBatchStudents = async () => {
  if (!batchClassId.value) { batchStudents.value = []; return }
  try { const res = await getStudents({ pageNum: 1, pageSize: 100, classId: batchClassId.value }); batchStudents.value = (res.records || []).map((s: any) => ({ id: s.id, studentNo: s.studentNo, realName: s.realName || s.name, usualScore: undefined, midtermScore: undefined, finalScore: undefined, totalScore: undefined })) } catch { batchStudents.value = [] }
}
const handleBatchSubmit = async () => {
  if (!batchCourseId.value || !batchSemesterId.value) { ElMessage.warning('请选择课程和学期'); return }
  const scores = batchStudents.value.filter(s => s.finalScore !== undefined).map(s => ({ studentId: s.id, courseId: batchCourseId.value!, semesterId: batchSemesterId.value!, usualScore: s.usualScore, midtermScore: s.midtermScore, finalScore: s.finalScore, totalScore: s.totalScore }))
  if (scores.length === 0) { ElMessage.warning('请至少填写一条成绩'); return }
  batchSubmitLoading.value = true; try { const count = await batchInputScores(scores); ElMessage.success(`成功录入 ${count} 条成绩`); batchDialogVisible.value = false; fetchData() } finally { batchSubmitLoading.value = false }
}
const handleImport = () => { ElMessage.info('导入功能开发中') }
const handleExport = () => { ElMessage.info('导出功能开发中') }

const loadSemesterList = async () => { try { const res = await listAllSemesters(); semesterList.value = res.data || []; const current = semesterList.value.find(s => s.isCurrent === 1); if (current) { queryParams.semesterId = current.id; batchSemesterId.value = current.id; loadCourseList(current.id) } } catch (e) { console.error('加载学期列表失败', e) } }
const loadCourseList = async (semesterId?: number) => { if (!semesterId) { courseList.value = []; return }; try { const res = await getCoursesBySemester(semesterId); courseList.value = res.data || [] } catch (e) { console.error('加载课程列表失败', e) } }
const loadClassList = async () => { try { const res = await getAllClasses(); classList.value = (res || []).map((c: any) => ({ id: c.id, className: c.className })) } catch (e) { console.error('加载班级列表失败', e) } }

onMounted(() => { loadSemesterList(); loadClassList(); fetchData() })
</script>
