<template>
  <!-- Grade Entry Drawer -->
  <Transition name="tm-drawer">
    <div v-if="batch" class="tm-drawer-overlay" @click.self="emit('close')">
      <div class="tm-drawer" style="width: 80%;">
        <div class="tm-drawer-header">
          <div>
            <h3 class="tm-drawer-title">成绩录入</h3>
            <div class="tm-stats" style="margin-top: 4px;">
              <span>{{ batch.courseName }}</span>
              <span class="sep" />
              <span>{{ batch.className }}</span>
              <span class="sep" />
              <span :class="['tm-chip', gradeTypeChipClass(batch.gradeType || 0)]">{{ getGradeTypeName(batch.gradeType || 0) }}</span>
            </div>
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <button class="tm-btn tm-btn-secondary" @click="showImportDialog">批量导入</button>
            <button class="tm-btn tm-btn-primary" :disabled="saving" @click="saveAllGrades">{{ saving ? '保存中...' : '保存全部' }}</button>
            <button class="tm-drawer-close" @click="emit('close')">&times;</button>
          </div>
        </div>
        <div class="tm-drawer-body" style="padding: 16px 24px;">
          <!-- Loading -->
          <div v-if="gradeEntryLoading" style="text-align: center; padding: 60px 0; color: #9ca3af; font-size: 13px;">
            <span class="tm-spin" style="display:inline-block;width:20px;height:20px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...
          </div>

          <!-- Table -->
          <table v-else-if="studentGrades.length > 0" class="tm-table">
            <colgroup>
              <col style="width: 110px" />
              <col style="width: 90px" />
              <col style="width: 100px" />
              <col style="width: 70px" />
              <col style="width: 70px" />
              <col style="width: 80px" />
              <col />
            </colgroup>
            <thead>
              <tr>
                <th>学号</th>
                <th>姓名</th>
                <th>成绩</th>
                <th>等级</th>
                <th>绩点</th>
                <th>状态</th>
                <th class="text-left">备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in studentGrades" :key="row.studentId">
                <td><span class="tm-code">{{ row.studentNo }}</span></td>
                <td style="font-weight: 500;">{{ row.studentName }}</td>
                <td>
                  <input v-model.number="row.totalScore" type="number" min="0" max="100" step="0.1" class="tm-input" style="width: 80px; padding: 4px 6px; text-align: center;" />
                </td>
                <td>
                  <span v-if="row.gradeLevel" :class="['tm-chip', gradeLevelChipClass(row.gradeLevel)]">{{ row.gradeLevel }}</span>
                </td>
                <td class="tm-mono">{{ row.gradePoint }}</td>
                <td>
                  <span :class="['tm-chip', row.status === 1 ? 'tm-chip-green' : 'tm-chip-gray']">
                    {{ row.status === 1 ? '已录入' : '未录入' }}
                  </span>
                </td>
                <td class="text-left">
                  <input v-model="row.remark" class="tm-input" style="padding: 4px 6px;" placeholder="备注..." />
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Empty -->
          <div v-else style="text-align: center; padding: 60px 0; color: #9ca3af; font-size: 13px;">
            暂无学生成绩数据
          </div>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Import Drawer -->
  <Transition name="tm-drawer">
    <div v-if="importDialogVisible" class="tm-drawer-overlay" @click.self="importDialogVisible = false">
      <div class="tm-drawer" style="width: 440px;">
        <div class="tm-drawer-header">
          <h3 class="tm-drawer-title">批量导入成绩</h3>
          <button class="tm-drawer-close" @click="importDialogVisible = false">&times;</button>
        </div>
        <div class="tm-drawer-body">
          <div class="tm-section">
            <div style="padding: 10px 14px; background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 7px; font-size: 12.5px; color: #1d4ed8; margin-bottom: 16px;">
              请先下载导入模板，按模板格式填写成绩后上传。
            </div>
            <div class="tm-field">
              <label class="tm-label">下载模板</label>
              <button class="tm-btn tm-btn-secondary" @click="downloadTemplate">下载模板</button>
            </div>
            <div class="tm-field">
              <label class="tm-label">上传文件（.xlsx/.xls）</label>
              <input type="file" accept=".xlsx,.xls" @change="handleFileChange" style="font-size: 13px;" />
            </div>
          </div>
        </div>
        <div class="tm-drawer-footer">
          <button class="tm-btn tm-btn-secondary" @click="importDialogVisible = false">取消</button>
          <button class="tm-btn tm-btn-primary" :disabled="importing" @click="importGrades">{{ importing ? '导入中...' : '导入' }}</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { gradeApi } from '@/api/teaching'
import type { GradeBatch, StudentGrade } from '@/types/teaching'
import { getGradeTypeName } from './gradeHelpers'

const props = defineProps<{
  batch: GradeBatch | undefined
}>()

const emit = defineEmits<{ close: [] }>()

// Chip helpers
function gradeTypeChipClass(type: number) {
  const map: Record<number, string> = { 1: 'tm-chip-gray', 2: 'tm-chip-amber', 3: 'tm-chip-blue', 4: 'tm-chip-green' }
  return map[type] || 'tm-chip-gray'
}

function gradeLevelChipClass(level: string) {
  if (level.startsWith('A')) return 'tm-chip-green'
  if (level.startsWith('B')) return 'tm-chip-blue'
  if (level.startsWith('C')) return 'tm-chip-amber'
  if (level.startsWith('D')) return 'tm-chip-red'
  return 'tm-chip-gray'
}

// State
const saving = ref(false)
const importing = ref(false)
const gradeEntryLoading = ref(false)
const studentGrades = ref<StudentGrade[]>([])
const selectedFile = ref<File>()
const importDialogVisible = ref(false)

watch(() => props.batch, (val) => {
  if (val) loadGrades()
})

async function loadGrades() {
  if (!props.batch) return
  gradeEntryLoading.value = true
  try {
    const res: any = await gradeApi.getGrades(props.batch.id)
    studentGrades.value = res.data || res
    if (!Array.isArray(studentGrades.value)) studentGrades.value = []
  } catch { ElMessage.error('加载学生成绩数据失败'); studentGrades.value = [] }
  finally { gradeEntryLoading.value = false }
}

const saveAllGrades = async () => {
  if (!props.batch) return
  saving.value = true
  try {
    await gradeApi.batchRecordGrades(props.batch.id, studentGrades.value.map(g => ({ studentId: g.studentId, totalScore: g.totalScore || 0, remark: g.remark })))
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

const showImportDialog = () => { selectedFile.value = undefined; importDialogVisible.value = true }

const downloadTemplate = async () => {
  if (!props.batch) return
  try {
    const res = await gradeApi.getImportTemplate(props.batch.id)
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = '成绩导入模板.xlsx'; link.click(); window.URL.revokeObjectURL(url)
  } catch { ElMessage.error('下载失败') }
}

const handleFileChange = (e: Event) => { selectedFile.value = (e.target as HTMLInputElement).files?.[0] }

const importGrades = async () => {
  if (!props.batch || !selectedFile.value) { ElMessage.warning('请选择文件'); return }
  importing.value = true
  try {
    await gradeApi.importGrades(props.batch.id, selectedFile.value)
    ElMessage.success('导入成功'); importDialogVisible.value = false; loadGrades()
  } catch { ElMessage.error('导入失败') } finally { importing.value = false }
}
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
