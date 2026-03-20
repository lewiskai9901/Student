<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2, Star } from 'lucide-vue-next'
import { useInspPlatformStore } from '@/stores/insp/inspPlatformStore'
import type { HolidayCalendar } from '@/types/insp/platform'

const store = useInspPlatformStore()

const loading = ref(false)
const calendars = ref<HolidayCalendar[]>([])
const showDialog = ref(false)
const editingId = ref<number | null>(null)
const filterYear = ref<number | undefined>(undefined)

const currentYear = new Date().getFullYear()
const yearOptions = computed(() => {
  const years: number[] = []
  for (let y = currentYear - 2; y <= currentYear + 2; y++) {
    years.push(y)
  }
  return years
})

const form = ref({
  calendarName: '',
  year: currentYear,
  holidays: '',
  workdays: '',
  isDefault: false,
})

function parseJsonArray(json: string): string[] {
  if (!json) return []
  try { return JSON.parse(json) } catch { return [] }
}

function countDates(json: string): number {
  return parseJsonArray(json).length
}

function formatDatesPreview(json: string): string {
  const arr = parseJsonArray(json)
  if (arr.length === 0) return '-'
  if (arr.length <= 3) return arr.join(', ')
  return `${arr.slice(0, 3).join(', ')} ... (${arr.length})`
}

async function loadData() {
  loading.value = true
  try {
    await store.fetchHolidayCalendars(filterYear.value)
    calendars.value = store.holidayCalendars
  } catch (e: any) {
    ElMessage.error(e.message || '加载假日日历失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = {
    calendarName: '',
    year: currentYear,
    holidays: '',
    workdays: '',
    isDefault: false,
  }
  showDialog.value = true
}

function openEdit(cal: HolidayCalendar) {
  editingId.value = cal.id
  const holidays = parseJsonArray(cal.holidays)
  const workdays = parseJsonArray(cal.workdays)
  form.value = {
    calendarName: cal.calendarName,
    year: cal.year,
    holidays: holidays.join('\n'),
    workdays: workdays.join('\n'),
    isDefault: cal.isDefault,
  }
  showDialog.value = true
}

function linesToJsonArray(text: string): string {
  const lines = text
    .split('\n')
    .map(l => l.trim())
    .filter(l => l.length > 0)
  return JSON.stringify(lines)
}

async function handleSave() {
  if (!form.value.calendarName.trim()) {
    ElMessage.warning('请输入日历名称')
    return
  }
  try {
    const payload = {
      calendarName: form.value.calendarName,
      year: form.value.year,
      holidays: linesToJsonArray(form.value.holidays),
      workdays: linesToJsonArray(form.value.workdays),
      isDefault: form.value.isDefault,
    }
    if (editingId.value) {
      await store.updateHolidayCalendar(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await store.createHolidayCalendar(payload)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(cal: HolidayCalendar) {
  try {
    await ElMessageBox.confirm(`确认删除假日日历「${cal.calendarName}」？`, '确认删除', { type: 'warning' })
    await store.deleteHolidayCalendar(cal.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

function handleYearChange() {
  loadData()
}

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">假日日历</h2>
      <div class="flex items-center gap-3">
        <el-select
          v-model="filterYear"
          placeholder="全部年份"
          clearable
          class="w-32"
          @change="handleYearChange"
        >
          <el-option
            v-for="y in yearOptions"
            :key="y"
            :label="`${y}年`"
            :value="y"
          />
        </el-select>
        <el-button type="primary" @click="openCreate">
          <Plus class="w-4 h-4 mr-1" />新建日历
        </el-button>
      </div>
    </div>

    <el-table :data="calendars" v-loading="loading" stripe>
      <el-table-column prop="calendarName" label="日历名称" min-width="160" />
      <el-table-column prop="year" label="年份" width="90" align="center" />
      <el-table-column label="假日数" width="90" align="center">
        <template #default="{ row }">
          {{ countDates(row.holidays) }}
        </template>
      </el-table-column>
      <el-table-column label="调休工作日数" width="120" align="center">
        <template #default="{ row }">
          {{ countDates(row.workdays) }}
        </template>
      </el-table-column>
      <el-table-column label="假日预览" min-width="200">
        <template #default="{ row }">
          <span class="text-gray-500 text-xs">{{ formatDatesPreview(row.holidays) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="默认" width="70" align="center">
        <template #default="{ row }">
          <Star v-if="row.isDefault" class="w-4 h-4 text-yellow-500 inline" />
          <span v-else class="text-gray-300">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-1">
            <el-button link type="primary" size="small" @click="openEdit(row)">
              <Pencil class="w-3.5 h-3.5" />
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="showDialog" :title="editingId ? '编辑假日日历' : '新建假日日历'" width="560px">
      <el-form label-width="110px">
        <el-form-item label="日历名称" required>
          <el-input v-model="form.calendarName" placeholder="例: 2026年法定节假日" />
        </el-form-item>
        <el-form-item label="年份" required>
          <el-select v-model="form.year" class="w-full">
            <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="假日列表">
          <el-input
            v-model="form.holidays"
            type="textarea"
            :rows="6"
            placeholder="每行一个日期，格式: YYYY-MM-DD&#10;例:&#10;2026-01-01&#10;2026-01-26&#10;2026-01-27"
          />
          <div class="text-xs text-gray-400 mt-1">每行填写一个日期，格式 YYYY-MM-DD</div>
        </el-form-item>
        <el-form-item label="调休工作日">
          <el-input
            v-model="form.workdays"
            type="textarea"
            :rows="4"
            placeholder="每行一个日期，格式: YYYY-MM-DD&#10;例:&#10;2026-01-31&#10;2026-02-14"
          />
          <div class="text-xs text-gray-400 mt-1">周末调为工作日的日期，每行一个</div>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
