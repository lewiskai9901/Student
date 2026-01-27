<template>
  <el-dialog
    v-model="visible"
    title="快捷录入"
    width="900px"
    :close-on-click-modal="false"
    @close="handleClose"
    class="quick-entry-dialog"
  >
    <div class="quick-entry-layout">
      <!-- 左侧：扣分项列表 -->
      <div class="left-panel">
        <div class="panel-header">
          <span class="panel-title">扣分项</span>
          <!-- 轮次切换 -->
          <div v-if="(props.totalRounds || 1) > 1" class="round-tabs">
            <button
              v-for="round in props.totalRounds"
              :key="round"
              :class="['round-tab', { active: selectedRound === round }]"
              @click="handleRoundChange(round)"
            >
              {{ getRoundDisplayName(round) }}
            </button>
          </div>
        </div>
        <div class="item-list">
          <div
            v-for="item in deductionItems"
            :key="item.id"
            :class="['item-card', { active: selectedDeductionItem?.id === item.id }]"
            @click="selectDeductionItem(item)"
          >
            <div class="item-info">
              <span class="item-name">{{ item.itemName }}</span>
              <span class="item-category">{{ item.categoryName }}</span>
            </div>
            <span class="item-score">-{{ item.perPersonScore || item.baseScore }}</span>
          </div>
          <div v-if="deductionItems.length === 0" class="empty-tip">
            暂无可用扣分项
          </div>
        </div>
      </div>

      <!-- 右侧：录入区域 -->
      <div class="right-panel">
        <template v-if="selectedDeductionItem">
          <!-- 当前扣分项 -->
          <div class="current-item">
            <Tag class="h-4 w-4" />
            <span class="name">{{ selectedDeductionItem.itemName }}</span>
            <span class="score">-{{ selectedDeductionItem.perPersonScore || selectedDeductionItem.baseScore }}分/人</span>
          </div>

          <!-- 搜索学生 -->
          <div class="search-section">
            <div class="search-box">
              <Search class="search-icon" />
              <input
                ref="searchInputRef"
                v-model="searchKeyword"
                type="text"
                placeholder="输入姓名或学号搜索学生..."
                @input="handleSearch"
              />
              <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">
                <X class="h-4 w-4" />
              </button>
            </div>
            <!-- 搜索结果 -->
            <div v-if="searchKeyword" class="search-results">
              <div v-if="searching" class="loading-tip">
                <Loader2 class="h-4 w-4 animate-spin" />
                <span>搜索中...</span>
              </div>
              <template v-else>
                <div
                  v-for="student in searchResults"
                  :key="student.id"
                  class="student-result"
                  @click="addStudent(student)"
                >
                  <div class="student-info">
                    <span class="name">{{ student.realName }}</span>
                    <span class="meta">{{ student.studentNo }} · {{ student.className }}</span>
                  </div>
                  <Plus class="h-4 w-4 text-blue-500" />
                </div>
                <div v-if="searchResults.length === 0" class="empty-tip small">
                  未找到匹配的学生
                </div>
              </template>
            </div>
          </div>

          <!-- 待提交列表 -->
          <div class="pending-section">
            <div class="section-header">
              <span class="title">待提交</span>
              <span class="count">{{ pendingList.length }}人</span>
              <div class="batch-btns" v-if="pendingList.length > 0">
                <button class="batch-btn" @click="openBatchPhoto">
                  <Camera class="h-3.5 w-3.5" />
                  批量照片
                </button>
                <button class="batch-btn" @click="openBatchRemark">
                  <MessageSquare class="h-3.5 w-3.5" />
                  批量备注
                </button>
              </div>
            </div>

            <div class="pending-list" v-if="pendingList.length > 0">
              <div
                v-for="(item, index) in pendingList"
                :key="item.student.id"
                :class="['pending-item', { expanded: expandedIndex === index }]"
              >
                <div class="item-row" @click="toggleExpand(index)">
                  <div class="student-info">
                    <span class="name">{{ item.student.realName }}</span>
                    <span class="class">{{ item.student.className }}</span>
                  </div>
                  <div class="item-status">
                    <span v-if="item.photos.length > 0" class="status-badge photo">
                      <Camera class="h-3 w-3" />
                      {{ item.photos.length }}
                    </span>
                    <span v-if="item.remark" class="status-badge remark">
                      <MessageSquare class="h-3 w-3" />
                    </span>
                  </div>
                  <button class="remove-btn" @click.stop="removeStudent(index)">
                    <X class="h-4 w-4" />
                  </button>
                </div>

                <!-- 展开的编辑区域 -->
                <div v-if="expandedIndex === index" class="expand-area">
                  <div class="edit-row">
                    <label>照片</label>
                    <el-upload
                      v-model:file-list="item.photoFileList"
                      action="/api/upload/image"
                      :headers="uploadHeaders"
                      list-type="picture-card"
                      :limit="5"
                      :on-success="(res: any) => handleUploadSuccess(res, index)"
                      :on-remove="() => handleUploadRemove(index)"
                      class="mini-upload"
                    >
                      <Plus class="h-4 w-4 text-gray-400" />
                    </el-upload>
                  </div>
                  <div class="edit-row">
                    <label>备注</label>
                    <el-input
                      v-model="item.remark"
                      type="textarea"
                      :rows="2"
                      placeholder="输入备注..."
                      size="small"
                    />
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="empty-tip">
              搜索并添加学生到列表
            </div>
          </div>
        </template>

        <div v-else class="placeholder-tip">
          <ArrowLeft class="h-6 w-6" />
          <span>请从左侧选择扣分项</span>
        </div>
      </div>
    </div>

    <!-- 底部操作 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :disabled="pendingList.length === 0"
          :loading="submitting"
          @click="handleSubmit"
        >
          提交 {{ pendingList.length }} 条记录
        </el-button>
      </div>
    </template>

    <!-- 批量照片弹窗 -->
    <el-dialog
      v-model="showBatchPhoto"
      title="批量添加照片"
      width="400px"
      append-to-body
    >
      <div class="batch-panel">
        <p class="batch-hint">照片将添加到所有 {{ pendingList.length }} 名学生</p>
        <el-upload
          v-model:file-list="batchPhotoFileList"
          action="/api/upload/image"
          :headers="uploadHeaders"
          list-type="picture-card"
          :limit="5"
          :on-success="handleBatchPhotoSuccess"
        >
          <Plus class="h-6 w-6 text-gray-400" />
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="showBatchPhoto = false">取消</el-button>
        <el-button type="primary" @click="applyBatchPhoto">应用到全部</el-button>
      </template>
    </el-dialog>

    <!-- 批量备注弹窗 -->
    <el-dialog
      v-model="showBatchRemark"
      title="批量添加备注"
      width="400px"
      append-to-body
    >
      <div class="batch-panel">
        <p class="batch-hint">备注将添加到所有 {{ pendingList.length }} 名学生</p>
        <el-input
          v-model="batchRemark"
          type="textarea"
          :rows="3"
          placeholder="输入备注信息..."
        />
      </div>
      <template #footer>
        <el-button @click="showBatchRemark = false">取消</el-button>
        <el-button type="primary" @click="applyBatchRemark">应用到全部</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Plus, X, Tag, Camera, MessageSquare,
  Loader2, ArrowLeft
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import {
  getQuickEntryDeductionItems,
  searchStudentsForQuickEntry,
  submitQuickEntry,
  checkQuickEntryDuplicate,
  type QuickEntryDeductionItemDTO,
  type QuickEntryStudentDTO
} from '@/api/quickEntry'

// 待提交项
interface PendingItem {
  student: QuickEntryStudentDTO
  photos: string[]
  photoFileList: any[]
  remark: string
}

const props = defineProps<{
  modelValue: boolean
  checkId: number | string
  totalRounds?: number
  roundNames?: string[]
  currentRound?: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'refresh'): void
}>()

const authStore = useAuthStore()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.accessToken}`
}))

// 状态
const allDeductionItems = ref<QuickEntryDeductionItemDTO[]>([])
const selectedRound = ref(1)
const selectedDeductionItem = ref<QuickEntryDeductionItemDTO | null>(null)
const searchKeyword = ref('')
const searchResults = ref<QuickEntryStudentDTO[]>([])
const searching = ref(false)
const submitting = ref(false)
const pendingList = ref<PendingItem[]>([])
const expandedIndex = ref(-1)
const searchInputRef = ref<HTMLInputElement | null>(null)

// 批量操作
const showBatchPhoto = ref(false)
const showBatchRemark = ref(false)
const batchPhotoFileList = ref<any[]>([])
const batchPhotoUrls = ref<string[]>([])
const batchRemark = ref('')

// 根据轮次过滤扣分项
const deductionItems = computed(() => {
  if ((props.totalRounds || 1) <= 1) {
    return allDeductionItems.value
  }
  return allDeductionItems.value.filter(item => {
    if (item.participatedRoundsList && item.participatedRoundsList.length > 0) {
      return item.participatedRoundsList.includes(selectedRound.value)
    }
    if (item.participatedRounds) {
      const rounds = item.participatedRounds.split(',').map((s: string) => parseInt(s.trim()))
      return rounds.includes(selectedRound.value)
    }
    return true
  })
})

// 轮次名称
const getRoundDisplayName = (round: number) => {
  if (props.roundNames && props.roundNames[round - 1]) {
    return props.roundNames[round - 1]
  }
  return `第${round}轮`
}

// 切换轮次
function handleRoundChange(round: number) {
  selectedRound.value = round
  if (selectedDeductionItem.value) {
    const stillAvailable = deductionItems.value.some(item => item.id === selectedDeductionItem.value?.id)
    if (!stillAvailable) {
      selectedDeductionItem.value = null
      pendingList.value = []
    }
  }
}

// 加载数据
async function loadData() {
  if (!props.checkId) return
  try {
    selectedRound.value = props.currentRound || 1
    const items = await getQuickEntryDeductionItems(props.checkId)
    allDeductionItems.value = items || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  }
}

// 选择扣分项
function selectDeductionItem(item: QuickEntryDeductionItemDTO) {
  if (selectedDeductionItem.value?.id === item.id) return
  selectedDeductionItem.value = item
  pendingList.value = []
  expandedIndex.value = -1
  nextTick(() => {
    searchInputRef.value?.focus()
  })
}

// 搜索
let searchTimeout: ReturnType<typeof setTimeout> | null = null

function handleSearch() {
  if (searchTimeout) clearTimeout(searchTimeout)
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  searchTimeout = setTimeout(async () => {
    searching.value = true
    try {
      const res = await searchStudentsForQuickEntry(props.checkId, searchKeyword.value.trim(), 15)
      searchResults.value = res || []
    } catch (error) {
      console.error('搜索失败:', error)
    } finally {
      searching.value = false
    }
  }, 300)
}

function clearSearch() {
  searchKeyword.value = ''
  searchResults.value = []
  searchInputRef.value?.focus()
}

// 添加学生（带重复检查）
const addingStudent = ref(false)
async function addStudent(student: QuickEntryStudentDTO) {
  // 检查本地列表
  if (pendingList.value.some(item => item.student.id === student.id)) {
    ElMessage.warning('该学生已在列表中')
    return
  }

  if (!selectedDeductionItem.value) {
    ElMessage.warning('请先选择扣分项')
    return
  }

  // 检查数据库中是否已存在
  addingStudent.value = true
  try {
    const result = await checkQuickEntryDuplicate(props.checkId, {
      deductionItemId: selectedDeductionItem.value.id,
      studentId: student.id,
      checkRound: selectedRound.value
    })

    if (result.isDuplicate) {
      ElMessage.warning(`${student.realName} 已被 ${result.existingRecordOperator || '他人'} 录入过该扣分项`)
      return
    }

    // 没有重复，添加到列表
    pendingList.value.push({
      student,
      photos: [],
      photoFileList: [],
      remark: ''
    })
    searchKeyword.value = ''
    searchResults.value = []
    ElMessage.success(`已添加 ${student.realName}`)
  } catch (error: any) {
    console.error('检查重复失败:', error)
    // 如果检查失败，仍然允许添加（降级处理）
    pendingList.value.push({
      student,
      photos: [],
      photoFileList: [],
      remark: ''
    })
    searchKeyword.value = ''
    searchResults.value = []
    ElMessage.success(`已添加 ${student.realName}`)
  } finally {
    addingStudent.value = false
  }
}

// 移除学生
function removeStudent(index: number) {
  pendingList.value.splice(index, 1)
  if (expandedIndex.value === index) {
    expandedIndex.value = -1
  } else if (expandedIndex.value > index) {
    expandedIndex.value--
  }
}

// 展开/收起
function toggleExpand(index: number) {
  expandedIndex.value = expandedIndex.value === index ? -1 : index
}

// 上传成功
function handleUploadSuccess(response: any, index: number) {
  if (response.code === 200 && response.data) {
    pendingList.value[index].photos.push(response.data)
  }
}

// 移除照片
function handleUploadRemove(index: number) {
  // 从 photoFileList 同步到 photos
  const item = pendingList.value[index]
  item.photos = item.photoFileList
    .filter((f: any) => f.url || f.response?.data)
    .map((f: any) => f.url || f.response?.data)
}

// 批量照片
function openBatchPhoto() {
  batchPhotoFileList.value = []
  batchPhotoUrls.value = []
  showBatchPhoto.value = true
}

function handleBatchPhotoSuccess(response: any) {
  if (response.code === 200 && response.data) {
    batchPhotoUrls.value.push(response.data)
  }
}

function applyBatchPhoto() {
  if (batchPhotoUrls.value.length === 0) {
    ElMessage.warning('请先上传照片')
    return
  }
  pendingList.value.forEach(item => {
    item.photos = [...item.photos, ...batchPhotoUrls.value]
    // 同步到 fileList 用于显示
    batchPhotoUrls.value.forEach(url => {
      item.photoFileList.push({ url, name: 'batch' })
    })
  })
  showBatchPhoto.value = false
  ElMessage.success(`已为 ${pendingList.value.length} 人添加 ${batchPhotoUrls.value.length} 张照片`)
}

// 批量备注
function openBatchRemark() {
  batchRemark.value = ''
  showBatchRemark.value = true
}

function applyBatchRemark() {
  if (!batchRemark.value.trim()) {
    ElMessage.warning('请输入备注内容')
    return
  }
  pendingList.value.forEach(item => {
    item.remark = item.remark ? item.remark + '；' + batchRemark.value : batchRemark.value
  })
  showBatchRemark.value = false
  ElMessage.success(`已为 ${pendingList.value.length} 人添加备注`)
}

// 提交
async function handleSubmit() {
  if (pendingList.value.length === 0 || !selectedDeductionItem.value) return

  submitting.value = true
  let successCount = 0
  let failCount = 0

  try {
    for (const item of pendingList.value) {
      try {
        await submitQuickEntry(props.checkId, {
          deductionItemId: selectedDeductionItem.value.id,
          studentId: item.student.id,
          checkRound: selectedRound.value,
          remark: item.remark || undefined,
          photoUrls: item.photos.length > 0 ? item.photos : undefined
        })
        successCount++
      } catch (error) {
        failCount++
        console.error(`录入 ${item.student.realName} 失败:`, error)
      }
    }

    if (successCount > 0) {
      ElMessage.success(`成功录入 ${successCount} 条${failCount > 0 ? `，${failCount} 条失败` : ''}`)
      emit('refresh')
      pendingList.value = []
      expandedIndex.value = -1
    } else {
      ElMessage.error('全部录入失败')
    }
  } finally {
    submitting.value = false
  }
}

// 关闭
function handleClose() {
  visible.value = false
  selectedDeductionItem.value = null
  searchKeyword.value = ''
  searchResults.value = []
  pendingList.value = []
  expandedIndex.value = -1
}

// 监听打开
watch(() => props.modelValue, (val) => {
  if (val) loadData()
})
</script>

<style scoped>
.quick-entry-layout {
  display: flex;
  gap: 20px;
  height: 520px;
}

/* 左侧面板 */
.left-panel {
  width: 280px;
  display: flex;
  flex-direction: column;
  background: #f9fafb;
  border-radius: 12px;
  overflow: hidden;
}

.panel-header {
  padding: 14px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.round-tabs {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}

.round-tab {
  flex: 1;
  padding: 6px 8px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.round-tab.active {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #3b82f6;
}

.item-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.item-card:hover {
  border-color: #93c5fd;
}

.item-card.active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.item-card .item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-card .item-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
}

.item-card .item-category {
  font-size: 11px;
  color: #9ca3af;
}

.item-card .item-score {
  font-size: 15px;
  font-weight: 600;
  color: #ef4444;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.current-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #eff6ff;
  border-radius: 8px;
  color: #3b82f6;
}

.current-item .name {
  font-size: 14px;
  font-weight: 500;
}

.current-item .score {
  margin-left: auto;
  font-size: 14px;
  font-weight: 600;
  color: #ef4444;
}

/* 搜索区域 */
.search-section {
  position: relative;
}

.search-box {
  display: flex;
  align-items: center;
  padding: 0 14px;
  height: 42px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: border-color 0.2s;
}

.search-box:focus-within {
  border-color: #3b82f6;
}

.search-icon {
  width: 18px;
  height: 18px;
  color: #9ca3af;
  margin-right: 10px;
}

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  background: transparent;
}

.clear-btn {
  padding: 4px;
  border: none;
  background: none;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 4px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
}

.student-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.student-result:hover {
  background: #f3f4f6;
}

.student-result .student-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.student-result .name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.student-result .meta {
  font-size: 12px;
  color: #9ca3af;
}

.loading-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  font-size: 13px;
  color: #9ca3af;
}

/* 待提交区域 */
.pending-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.section-header .title {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.section-header .count {
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 10px;
}

.batch-btns {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.batch-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.batch-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.pending-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pending-item {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.pending-item.expanded {
  border-color: #3b82f6;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
}

.item-row .student-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-row .name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.item-row .class {
  font-size: 12px;
  color: #9ca3af;
}

.item-status {
  display: flex;
  gap: 6px;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.status-badge.photo {
  background: #dbeafe;
  color: #3b82f6;
}

.status-badge.remark {
  background: #fef3c7;
  color: #d97706;
}

.remove-btn {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: none;
  background: #fef2f2;
  color: #ef4444;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.remove-btn:hover {
  background: #fee2e2;
}

/* 展开编辑区 */
.expand-area {
  padding: 12px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edit-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.edit-row label {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
}

.mini-upload :deep(.el-upload--picture-card) {
  width: 60px;
  height: 60px;
}

.mini-upload :deep(.el-upload-list__item) {
  width: 60px;
  height: 60px;
}

/* 占位提示 */
.placeholder-tip {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #9ca3af;
  font-size: 14px;
}

.empty-tip {
  text-align: center;
  padding: 24px;
  font-size: 13px;
  color: #9ca3af;
}

.empty-tip.small {
  padding: 16px;
}

/* 批量面板 */
.batch-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.batch-hint {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

/* 底部 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
