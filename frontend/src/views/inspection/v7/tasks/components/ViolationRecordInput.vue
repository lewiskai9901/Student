<script setup lang="ts">
/**
 * ViolationRecordInput - Inline violation record management
 *
 * Loads existing violation records for a submission detail,
 * allows adding new records with user search, severity, description, score,
 * and deleting existing records.
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Trash2, AlertTriangle } from 'lucide-vue-next'
import { inspViolationApi } from '@/api/insp/project'
import { getSimpleUserList } from '@/api/user'
import type { ViolationRecord } from '@/types/insp/template'
import type { SimpleUser } from '@/types/user'
import { ViolationSeverityConfig, type ViolationSeverity } from '@/types/insp/enums'

const props = withDefaults(defineProps<{
  submissionId: number
  detailId: number
  sectionId?: number
  itemId?: number
  disabled?: boolean
}>(), {
  disabled: false,
})

// ========== State ==========
const loading = ref(false)
const records = ref<ViolationRecord[]>([])
const addingRecord = ref(false)
const saving = ref(false)

// Search
const searchQuery = ref('')
const searchResults = ref<SimpleUser[]>([])
const searchLoading = ref(false)

// New record form
const form = ref({
  userId: undefined as number | undefined,
  userName: '',
  severity: 'MINOR' as ViolationSeverity,
  occurredAt: '',
  description: '',
  score: 0 as number | null,
})

// ========== Load ==========
async function loadRecords() {
  loading.value = true
  try {
    records.value = await inspViolationApi.listBySubmission(props.submissionId)
    // Filter to only records matching this detail
    records.value = records.value.filter(r =>
      r.submissionDetailId === props.detailId ||
      (r.sectionId === props.sectionId && r.itemId === props.itemId)
    )
  } catch (e: any) {
    ElMessage.error(e.message || '加载违纪记录失败')
  } finally {
    loading.value = false
  }
}

// ========== Search ==========
async function handleSearch(query: string) {
  if (!query.trim()) {
    searchResults.value = []
    return
  }
  searchLoading.value = true
  try {
    searchResults.value = await getSimpleUserList(query.trim())
  } catch {
    searchResults.value = []
  }
  searchLoading.value = false
}

function handleUserSelect(userId: number) {
  const user = searchResults.value.find(u => Number(u.id) === userId)
  if (user) {
    form.value.userId = userId
    form.value.userName = user.realName || user.username
  }
}

// ========== CRUD ==========
function showAddForm() {
  form.value = {
    userId: undefined,
    userName: '',
    severity: 'MINOR',
    occurredAt: '',
    description: '',
    score: 0,
  }
  searchQuery.value = ''
  searchResults.value = []
  addingRecord.value = true
}

async function handleSaveRecord() {
  if (!form.value.userId) {
    ElMessage.warning('请选择人员')
    return
  }
  saving.value = true
  try {
    await inspViolationApi.create({
      submissionId: props.submissionId,
      submissionDetailId: props.detailId,
      sectionId: props.sectionId || undefined,
      itemId: props.itemId || undefined,
      userId: form.value.userId,
      userName: form.value.userName,
      severity: form.value.severity,
      occurredAt: form.value.occurredAt || undefined,
      description: form.value.description || undefined,
      score: form.value.score,
    } as any)
    ElMessage.success('已添加')
    addingRecord.value = false
    loadRecords()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDeleteRecord(record: ViolationRecord) {
  try {
    await ElMessageBox.confirm('确定删除该违纪记录?', '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return // cancelled
  }
  try {
    await inspViolationApi.delete(record.id)
    ElMessage.success('已删除')
    loadRecords()
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  }
}

function formatTime(dt: string): string {
  if (!dt) return '-'
  return dt.substring(0, 16).replace('T', ' ')
}

function formatScoreDisplay(score: number | null): string {
  if (score == null) return ''
  return (score > 0 ? '+' : '') + score + '分'
}

onMounted(() => loadRecords())
</script>

<template>
  <div v-loading="loading">
    <!-- Header -->
    <div class="flex items-center justify-between mb-2">
      <span class="text-xs font-medium text-gray-600">
        违纪记录
        <span v-if="records.length > 0" class="text-gray-400">({{ records.length }}条)</span>
      </span>
    </div>

    <!-- Records list -->
    <div v-if="records.length > 0" class="space-y-2 mb-3">
      <div
        v-for="record in records"
        :key="record.id"
        class="flex items-start gap-3 p-3 bg-white border border-gray-200 rounded-lg"
      >
        <div class="w-8 h-8 rounded-full bg-red-50 flex items-center justify-center shrink-0">
          <AlertTriangle class="w-4 h-4 text-red-400" />
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 flex-wrap">
            <span class="font-medium text-gray-800 text-sm">{{ record.userName }}</span>
            <el-tag
              :type="(ViolationSeverityConfig[record.severity]?.type as any)"
              size="small"
              round
              effect="plain"
            >
              {{ ViolationSeverityConfig[record.severity]?.label }}
            </el-tag>
            <span
              v-if="record.score != null"
              class="text-xs text-red-500 font-medium"
            >{{ formatScoreDisplay(record.score) }}</span>
          </div>
          <div v-if="record.description" class="text-xs text-gray-500 mt-0.5 line-clamp-2">
            {{ record.description }}
          </div>
          <div class="text-xs text-gray-300 mt-0.5">
            {{ formatTime(record.occurredAt || record.createdAt) }}
          </div>
        </div>
        <el-button
          v-if="!disabled"
          link
          type="danger"
          size="small"
          @click="handleDeleteRecord(record)"
        >
          <Trash2 class="w-3.5 h-3.5" />
        </el-button>
      </div>
    </div>

    <div v-else-if="!addingRecord" class="text-xs text-gray-400 mb-2">暂无违纪记录</div>

    <!-- Add record form (inline) -->
    <div v-if="addingRecord" class="p-3 bg-orange-50 border border-orange-200 rounded-lg mb-3 space-y-3">
      <div class="text-sm font-medium text-gray-700">添加违纪记录</div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-xs text-gray-500 mb-1">人员 <span class="text-red-500">*</span></label>
          <el-select
            v-model="searchQuery"
            filterable
            remote
            reserve-keyword
            :remote-method="handleSearch"
            :loading="searchLoading"
            placeholder="搜索人员..."
            class="w-full"
            size="small"
            @change="handleUserSelect"
            clearable
          >
            <el-option
              v-for="u in searchResults"
              :key="Number(u.id)"
              :label="(u.realName || u.username) + (u.orgUnitName ? ` (${u.orgUnitName})` : '')"
              :value="Number(u.id)"
            />
          </el-select>
          <div v-if="form.userName" class="text-xs text-gray-500 mt-0.5">已选: {{ form.userName }}</div>
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">严重程度</label>
          <el-select v-model="form.severity" class="w-full" size="small">
            <el-option
              v-for="(cfg, key) in ViolationSeverityConfig"
              :key="key"
              :label="cfg.label"
              :value="key"
            />
          </el-select>
        </div>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-xs text-gray-500 mb-1">发生时间</label>
          <el-date-picker
            v-model="form.occurredAt"
            type="datetime"
            size="small"
            placeholder="选择时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="!w-full"
          />
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">扣分</label>
          <el-input-number
            v-model="form.score"
            :max="0"
            :step="1"
            size="small"
            class="!w-full"
          />
        </div>
      </div>
      <div>
        <label class="block text-xs text-gray-500 mb-1">描述</label>
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          placeholder="违纪描述..."
          size="small"
          resize="none"
        />
      </div>
      <div class="flex items-center gap-2">
        <el-button size="small" type="primary" :loading="saving" @click="handleSaveRecord">保存</el-button>
        <el-button size="small" @click="addingRecord = false">取消</el-button>
      </div>
    </div>

    <!-- Add button -->
    <el-button v-if="!addingRecord && !disabled" size="small" @click="showAddForm">
      <Plus class="w-3.5 h-3.5 mr-1" />添加记录
    </el-button>
  </div>
</template>
