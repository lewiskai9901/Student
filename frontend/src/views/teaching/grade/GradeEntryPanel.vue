<template>
  <el-drawer v-model="drawerVisible" title="成绩录入" size="80%" @close="onDrawerClose">
    <div class="flex items-start justify-between">
      <div>
        <h3 class="text-base font-semibold text-gray-900">{{ batch?.batchName }}</h3>
        <div class="mt-1 flex items-center gap-3 text-sm text-gray-500">
          <span>{{ batch?.courseName }}</span>
          <div class="h-3 w-px bg-gray-200" />
          <span>{{ batch?.className }}</span>
          <div class="h-3 w-px bg-gray-200" />
          <el-tag :type="(getGradeTypeTag(batch?.gradeType || 0) || undefined) as any" size="small">
            {{ getGradeTypeName(batch?.gradeType || 0) }}
          </el-tag>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <el-button @click="showImportDialog">批量导入</el-button>
        <el-button type="primary" :loading="saving" @click="saveAllGrades">保存全部</el-button>
      </div>
    </div>

    <div v-if="gradeEntryLoading" class="flex items-center justify-center py-20">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
    </div>

    <el-table v-else :data="studentGrades" border class="mt-4">
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
          <el-tag v-if="row.gradeLevel" :type="getGradeLevelTag(row.gradeLevel) as any" size="small">
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

    <div
      v-if="!gradeEntryLoading && studentGrades.length === 0"
      class="flex flex-col items-center justify-center py-12 text-gray-400"
    >
      <p class="text-sm">暂无学生成绩数据</p>
    </div>
  </el-drawer>

  <!-- Import Dialog -->
  <el-dialog v-model="importDialogVisible" title="批量导入成绩" width="500px" :close-on-click-modal="false">
    <el-alert type="info" :closable="false" class="mb-5">
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
            <div class="mt-1 text-xs text-gray-400">只能上传 xlsx/xls 文件</div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="importDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="importing" @click="importGrades">导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { gradeApi } from '@/api/teaching'
import type { GradeBatch, StudentGrade } from '@/types/teaching'
import { getGradeTypeName, getGradeTypeTag, getGradeLevelTag } from './gradeHelpers'

const props = defineProps<{
  batch: GradeBatch | undefined
}>()

const emit = defineEmits<{
  close: []
}>()

// Drawer visibility driven by batch prop
const drawerVisible = ref(false)

watch(() => props.batch, (val) => {
  if (val) {
    drawerVisible.value = true
    loadGrades()
  } else {
    drawerVisible.value = false
  }
})

function onDrawerClose() {
  emit('close')
}

// State
const saving = ref(false)
const importing = ref(false)
const gradeEntryLoading = ref(false)
const studentGrades = ref<StudentGrade[]>([])
const selectedFile = ref<File>()
const importDialogVisible = ref(false)

// Load grades for the batch
async function loadGrades() {
  if (!props.batch) return
  gradeEntryLoading.value = true
  try {
    const res: any = await gradeApi.getGrades(props.batch.id)
    studentGrades.value = res.data || res
    if (!Array.isArray(studentGrades.value)) {
      studentGrades.value = []
    }
  } catch (error) {
    console.error('Failed to load grades:', error)
    ElMessage.error('加载学生成绩数据失败')
    studentGrades.value = []
  } finally {
    gradeEntryLoading.value = false
  }
}

// Save all grades
const saveAllGrades = async () => {
  if (!props.batch) return
  saving.value = true
  try {
    await gradeApi.batchRecordGrades(
      props.batch.id,
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

// Import
const showImportDialog = () => {
  selectedFile.value = undefined
  importDialogVisible.value = true
}

const downloadTemplate = async () => {
  if (!props.batch) return
  try {
    const res = await gradeApi.getImportTemplate(props.batch.id)
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
  if (!props.batch || !selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  importing.value = true
  try {
    await gradeApi.importGrades(props.batch.id, selectedFile.value)
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    loadGrades()
  } catch (error) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}
</script>
