<template>
  <div class="grade-view">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="学期">
          <el-select v-model="queryParams.semesterId" placeholder="选择学期" @change="loadBatches">
            <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="成绩类型">
          <el-select v-model="queryParams.gradeType" placeholder="全部" clearable @change="loadBatches">
            <el-option :value="1" label="平时成绩" />
            <el-option :value="2" label="期中成绩" />
            <el-option :value="3" label="期末成绩" />
            <el-option :value="4" label="总评成绩" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable @change="loadBatches">
            <el-option :value="0" label="草稿" />
            <el-option :value="1" label="已提交" />
            <el-option :value="2" label="已审核" />
            <el-option :value="3" label="已发布" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>成绩录入批次</span>
          <el-button type="primary" @click="showBatchDialog()">新建批次</el-button>
        </div>
      </template>

      <el-table :data="batches" v-loading="loading" border stripe>
        <el-table-column prop="batchName" label="批次名称" min-width="180" />
        <el-table-column prop="courseName" label="课程" width="150" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="gradeType" label="成绩类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getGradeTypeTag(row.gradeType)">
              {{ getGradeTypeName(row.gradeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inputDeadline" label="录入截止" width="120" />
        <el-table-column prop="createdByName" label="创建人" width="100" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click="enterGrades(row)">录入</el-button>
            <el-button size="small" text @click="viewStatistics(row)">统计</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              text
              type="primary"
              @click="submitBatch(row)"
            >提交</el-button>
            <el-button
              v-if="row.status === 1"
              size="small"
              text
              type="success"
              @click="approveBatch(row)"
            >审核</el-button>
            <el-button
              v-if="row.status === 2"
              size="small"
              text
              type="warning"
              @click="publishBatch(row)"
            >发布</el-button>
            <el-button size="small" text @click="exportGrades(row)">导出</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadBatches"
        @current-change="loadBatches"
      />
    </el-card>

    <!-- 批次对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      :title="batchForm.id ? '编辑批次' : '新建成绩批次'"
      width="600px"
    >
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="批次名称" prop="batchName">
          <el-input v-model="batchForm.batchName" placeholder="如：高等数学-期末成绩" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学期" prop="semesterId">
              <el-select v-model="batchForm.semesterId" style="width: 100%">
                <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成绩类型" prop="gradeType">
              <el-select v-model="batchForm.gradeType" style="width: 100%">
                <el-option :value="1" label="平时成绩" />
                <el-option :value="2" label="期中成绩" />
                <el-option :value="3" label="期末成绩" />
                <el-option :value="4" label="总评成绩" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程" prop="courseId">
              <el-select
                v-model="batchForm.courseId"
                filterable
                remote
                :remote-method="searchCourses"
                placeholder="搜索课程"
                style="width: 100%"
              >
                <el-option
                  v-for="c in courseOptions"
                  :key="c.id"
                  :value="c.id"
                  :label="`${c.code} - ${c.name}`"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级" prop="classId">
              <el-select v-model="batchForm.classId" placeholder="选择班级" style="width: 100%">
                <el-option v-for="cls in classOptions" :key="cls.id" :value="cls.id" :label="cls.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="录入截止">
          <el-date-picker v-model="batchForm.inputDeadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBatch">保存</el-button>
      </template>
    </el-dialog>

    <!-- 成绩录入抽屉 -->
    <el-drawer v-model="gradeEntryDrawerVisible" title="成绩录入" size="80%">
      <div class="entry-header">
        <div class="batch-info">
          <h3>{{ currentBatch?.batchName }}</h3>
          <div class="batch-meta">
            <span>{{ currentBatch?.courseName }}</span>
            <span>{{ currentBatch?.className }}</span>
            <el-tag :type="getGradeTypeTag(currentBatch?.gradeType || 0)">
              {{ getGradeTypeName(currentBatch?.gradeType || 0) }}
            </el-tag>
          </div>
        </div>
        <div class="entry-actions">
          <el-button @click="showImportDialog">批量导入</el-button>
          <el-button type="primary" :loading="saving" @click="saveAllGrades">保存全部</el-button>
        </div>
      </div>

      <el-table :data="studentGrades" border style="margin-top: 16px">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column label="成绩" width="120">
          <template #default="{ row }">
            <el-input-number
              v-model="row.totalScore"
              :min="0"
              :max="100"
              :precision="1"
              size="small"
              style="width: 100px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="gradeLevel" label="等级" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.gradeLevel" :type="getGradeLevelTag(row.gradeLevel)" size="small">
              {{ row.gradeLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="gradePoint" label="绩点" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已录入' : '未录入' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" placeholder="备注..." />
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 成绩统计抽屉 -->
    <el-drawer v-model="statisticsDrawerVisible" title="成绩统计" size="60%">
      <template v-if="statistics">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="总人数" :value="statistics.totalCount" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="及格人数" :value="statistics.passCount">
              <template #suffix>
                <span class="stat-rate">({{ (statistics.passRate * 100).toFixed(1) }}%)</span>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="优秀人数" :value="statistics.excellentCount">
              <template #suffix>
                <span class="stat-rate">({{ (statistics.excellentRate * 100).toFixed(1) }}%)</span>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="平均分" :value="statistics.averageScore" :precision="1" />
          </el-col>
        </el-row>

        <el-divider />

        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>成绩分布</template>
              <div class="distribution-chart">
                <div
                  v-for="item in statistics.distribution"
                  :key="item.range"
                  class="distribution-bar"
                >
                  <span class="range-label">{{ item.range }}</span>
                  <div class="bar-container">
                    <div class="bar" :style="{ width: `${item.percentage}%` }"></div>
                  </div>
                  <span class="count-label">{{ item.count }}人 ({{ item.percentage.toFixed(1) }}%)</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>成绩概览</template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="最高分">{{ statistics.maxScore }}</el-descriptions-item>
                <el-descriptions-item label="最低分">{{ statistics.minScore }}</el-descriptions-item>
                <el-descriptions-item label="及格率">{{ (statistics.passRate * 100).toFixed(1) }}%</el-descriptions-item>
                <el-descriptions-item label="优秀率">{{ (statistics.excellentRate * 100).toFixed(1) }}%</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </el-drawer>

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入成绩" width="500px">
      <el-alert type="info" :closable="false" style="margin-bottom: 20px">
        请先下载导入模板，按模板格式填写成绩后上传。
      </el-alert>
      <el-form label-width="100px">
        <el-form-item label="下载模板">
          <el-button @click="downloadTemplate">下载模板</el-button>
        </el-form-item>
        <el-form-item label="上传文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
          >
            <template #trigger>
              <el-button type="primary">选择文件</el-button>
            </template>
            <template #tip>
              <div class="el-upload__tip">只能上传 xlsx/xls 文件</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="importGrades">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadInstance } from 'element-plus'
import { gradeApi, semesterApi, courseApi } from '@/api/teaching'
import type { GradeBatch, StudentGrade, GradeStatistics, Semester, Course, GradeQueryParams } from '@/types/teaching'

// 状态
const loading = ref(false)
const saving = ref(false)
const importing = ref(false)
const batches = ref<GradeBatch[]>([])
const studentGrades = ref<StudentGrade[]>([])
const statistics = ref<GradeStatistics>()
const semesters = ref<Semester[]>([])
const courseOptions = ref<Course[]>([])
const total = ref(0)
const currentBatch = ref<GradeBatch>()
const selectedFile = ref<File>()

// 对话框状态
const batchDialogVisible = ref(false)
const gradeEntryDrawerVisible = ref(false)
const statisticsDrawerVisible = ref(false)
const importDialogVisible = ref(false)

// 表单
const batchFormRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()
const batchForm = ref<Partial<GradeBatch>>({})

const queryParams = reactive<GradeQueryParams>({
  semesterId: undefined,
  gradeType: undefined,
  status: undefined,
  page: 1,
  size: 10,
})

// 选项数据
const classOptions = ref([
  { id: 1, name: '计算机2024-1班' },
  { id: 2, name: '计算机2024-2班' },
  { id: 3, name: '软件2024-1班' },
])

// 验证规则
const batchRules: FormRules = {
  batchName: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  gradeType: [{ required: true, message: '请选择成绩类型', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
}

// 方法
const loadSemesters = async () => {
  try {
    const res = await semesterApi.list()
    semesters.value = res.data || res
    if (semesters.value.length > 0) {
      const current = semesters.value.find(s => s.isCurrent)
      if (current) {
        queryParams.semesterId = current.id
      }
    }
  } catch (error) {
    console.error('Failed to load semesters:', error)
  }
}

const loadBatches = async () => {
  loading.value = true
  try {
    const res = await gradeApi.listBatches(queryParams)
    const data = res.data || res
    batches.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('Failed to load batches:', error)
  } finally {
    loading.value = false
  }
}

const searchCourses = async (query: string) => {
  if (query.length < 2) return
  try {
    const res = await courseApi.list({ keyword: query, page: 1, size: 20 })
    const data = res.data || res
    courseOptions.value = data.records || []
  } catch (error) {
    console.error('Failed to search courses:', error)
  }
}

const showBatchDialog = (batch?: GradeBatch) => {
  batchForm.value = batch
    ? { ...batch }
    : { semesterId: queryParams.semesterId, gradeType: 3 }
  batchDialogVisible.value = true
}

const saveBatch = async () => {
  await batchFormRef.value?.validate()
  saving.value = true
  try {
    if (batchForm.value.id) {
      await gradeApi.updateBatch(batchForm.value.id, batchForm.value)
    } else {
      await gradeApi.createBatch(batchForm.value)
    }
    ElMessage.success('保存成功')
    batchDialogVisible.value = false
    loadBatches()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const enterGrades = async (batch: GradeBatch) => {
  currentBatch.value = batch
  try {
    const res = await gradeApi.getGrades(batch.id)
    studentGrades.value = res.data || res
  } catch (error) {
    console.error('Failed to load grades:', error)
    // Mock data for demo
    studentGrades.value = [
      { id: 1, batchId: batch.id, studentId: 1, studentName: '张三', studentNo: '2024001', semesterId: 1, courseId: 1, classId: 1, gradeType: 3, totalScore: 85, gradeLevel: 'B+', gradePoint: 3.3, status: 1, remark: '' },
      { id: 2, batchId: batch.id, studentId: 2, studentName: '李四', studentNo: '2024002', semesterId: 1, courseId: 1, classId: 1, gradeType: 3, totalScore: 92, gradeLevel: 'A', gradePoint: 4.0, status: 1, remark: '' },
      { id: 3, batchId: batch.id, studentId: 3, studentName: '王五', studentNo: '2024003', semesterId: 1, courseId: 1, classId: 1, gradeType: 3, totalScore: undefined, gradeLevel: '', gradePoint: 0, status: 0, remark: '' },
    ]
  }
  gradeEntryDrawerVisible.value = true
}

const saveAllGrades = async () => {
  if (!currentBatch.value) return
  saving.value = true
  try {
    await gradeApi.batchRecordGrades(
      currentBatch.value.id,
      studentGrades.value.map(g => ({
        studentId: g.studentId,
        totalScore: g.totalScore || 0,
        remark: g.remark,
      }))
    )
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const viewStatistics = async (batch: GradeBatch) => {
  currentBatch.value = batch
  try {
    const res = await gradeApi.getStatistics({ batchId: batch.id })
    statistics.value = res.data || res
  } catch (error) {
    console.error('Failed to load statistics:', error)
    // Mock data for demo
    statistics.value = {
      totalCount: 50,
      passCount: 45,
      passRate: 0.9,
      excellentCount: 15,
      excellentRate: 0.3,
      averageScore: 78.5,
      maxScore: 98,
      minScore: 42,
      distribution: [
        { range: '90-100', count: 8, percentage: 16 },
        { range: '80-89', count: 15, percentage: 30 },
        { range: '70-79', count: 12, percentage: 24 },
        { range: '60-69', count: 10, percentage: 20 },
        { range: '0-59', count: 5, percentage: 10 },
      ],
    }
  }
  statisticsDrawerVisible.value = true
}

const submitBatch = async (batch: GradeBatch) => {
  await ElMessageBox.confirm('提交后成绩将进入审核流程，确定提交吗？', '提示', { type: 'info' })
  try {
    await gradeApi.submitBatch(batch.id)
    ElMessage.success('提交成功')
    loadBatches()
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const approveBatch = async (batch: GradeBatch) => {
  await ElMessageBox.confirm('确定审核通过该批次成绩吗？', '提示', { type: 'info' })
  try {
    await gradeApi.approveBatch(batch.id)
    ElMessage.success('审核通过')
    loadBatches()
  } catch (error) {
    ElMessage.error('审核失败')
  }
}

const publishBatch = async (batch: GradeBatch) => {
  await ElMessageBox.confirm('发布后学生将可查看成绩，确定发布吗？', '提示', { type: 'warning' })
  try {
    await gradeApi.publishBatch(batch.id)
    ElMessage.success('发布成功')
    loadBatches()
  } catch (error) {
    ElMessage.error('发布失败')
  }
}

const exportGrades = async (batch: GradeBatch) => {
  try {
    const res = await gradeApi.exportGrades(batch.id)
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${batch.batchName}_成绩.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const showImportDialog = () => {
  selectedFile.value = undefined
  importDialogVisible.value = true
}

const downloadTemplate = async () => {
  if (!currentBatch.value) return
  try {
    const res = await gradeApi.getImportTemplate(currentBatch.value.id)
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '成绩导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

const handleFileChange = (file: any) => {
  selectedFile.value = file.raw
}

const importGrades = async () => {
  if (!currentBatch.value || !selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  importing.value = true
  try {
    await gradeApi.importGrades(currentBatch.value.id, selectedFile.value)
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    const res = await gradeApi.getGrades(currentBatch.value.id)
    studentGrades.value = res.data || res
  } catch (error) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

const getGradeTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '平时成绩', 2: '期中成绩', 3: '期末成绩', 4: '总评成绩' }
  return names[type] || '未知'
}

const getGradeTypeTag = (type: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    1: 'info',
    2: 'warning',
    3: '',
    4: 'success',
  }
  return types[type] || 'info'
}

const getStatusName = (status: number) => {
  const names: Record<number, string> = { 0: '草稿', 1: '已提交', 2: '已审核', 3: '已发布' }
  return names[status] || '未知'
}

const getStatusTag = (status: number) => {
  const types: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'info',
    1: 'warning',
    2: '',
    3: 'success',
  }
  return types[status] || 'info'
}

const getGradeLevelTag = (level: string) => {
  if (level.startsWith('A')) return 'success'
  if (level.startsWith('B')) return ''
  if (level.startsWith('C')) return 'warning'
  if (level.startsWith('D')) return 'danger'
  return 'info'
}

onMounted(async () => {
  await loadSemesters()
  loadBatches()
})
</script>

<style scoped lang="scss">
.grade-view {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.entry-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  .batch-info {
    h3 {
      margin: 0 0 8px 0;
    }

    .batch-meta {
      display: flex;
      gap: 16px;
      color: #606266;
      font-size: 14px;
      align-items: center;
    }
  }

  .entry-actions {
    display: flex;
    gap: 8px;
  }
}

.stat-rate {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.distribution-chart {
  .distribution-bar {
    display: flex;
    align-items: center;
    margin-bottom: 12px;

    .range-label {
      width: 60px;
      font-size: 13px;
    }

    .bar-container {
      flex: 1;
      height: 20px;
      background: #f0f2f5;
      border-radius: 4px;
      margin: 0 12px;
      overflow: hidden;

      .bar {
        height: 100%;
        background: linear-gradient(90deg, #409eff, #79bbff);
        border-radius: 4px;
        transition: width 0.3s;
      }
    }

    .count-label {
      width: 100px;
      font-size: 12px;
      color: #606266;
    }
  }
}
</style>
