<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2, Search, ListChecks, GripVertical } from 'lucide-vue-next'
import { responseSetApi } from '@/api/insp/responseSet'
import type { ResponseSet, ResponseSetOption, CreateResponseSetRequest, CreateOptionRequest, UpdateOptionRequest } from '@/types/insp/template'
import InspEmptyState from '../shared/InspEmptyState.vue'

// State
const loading = ref(false)
const sets = ref<ResponseSet[]>([])
const total = ref(0)
const queryParams = reactive({ page: 1, size: 20, keyword: '' })

// Mousedown guard for dialog mask
const maskMouseDownTarget = ref<EventTarget | null>(null)
function onMaskMouseDown(e: MouseEvent) { maskMouseDownTarget.value = e.target }
function onMaskClose(e: MouseEvent, closeFn: () => void) {
  if (e.target === e.currentTarget && maskMouseDownTarget.value === e.currentTarget) closeFn()
  maskMouseDownTarget.value = null
}

// Set dialog
const setDialogVisible = ref(false)
const setDialogTitle = ref('创建选项集')
const editingSetId = ref<number | null>(null)
const setForm = ref({ setCode: '', setName: '', isGlobal: true })

// Options panel
const selectedSet = ref<ResponseSet | null>(null)
const options = ref<ResponseSetOption[]>([])
const optionsLoading = ref(false)
const optionDialogVisible = ref(false)
const editingOptionId = ref<number | null>(null)
const optionForm = ref({ optionValue: '', optionLabel: '', optionColor: '#409EFF', score: 0, isFlagged: false, sortOrder: 0 })

// Load sets
async function loadSets() {
  loading.value = true
  try {
    const result = await responseSetApi.getList(queryParams)
    sets.value = result.records
    total.value = result.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载选项集失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.page = 1
  loadSets()
}

// Set CRUD
function openCreateSet() {
  editingSetId.value = null
  setDialogTitle.value = '创建选项集'
  setForm.value = { setCode: '', setName: '', isGlobal: true }
  setDialogVisible.value = true
}

function openEditSet(s: ResponseSet) {
  editingSetId.value = s.id
  setDialogTitle.value = '编辑选项集'
  setForm.value = { setCode: s.setCode, setName: s.setName, isGlobal: s.isGlobal }
  setDialogVisible.value = true
}

async function handleSaveSet() {
  if (!setForm.value.setName.trim()) { ElMessage.warning('请输入名称'); return }
  try {
    if (editingSetId.value) {
      await responseSetApi.update(editingSetId.value, { setName: setForm.value.setName, isGlobal: setForm.value.isGlobal })
    } else {
      if (!setForm.value.setCode.trim()) { ElMessage.warning('请输入编码'); return }
      await responseSetApi.create({ setCode: setForm.value.setCode, setName: setForm.value.setName, isGlobal: setForm.value.isGlobal })
    }
    ElMessage.success('保存成功')
    setDialogVisible.value = false
    loadSets()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

async function handleDeleteSet(s: ResponseSet) {
  try {
    await ElMessageBox.confirm(`确认删除选项集「${s.setName}」？`, '确认删除', { type: 'warning' })
    await responseSetApi.delete(s.id)
    ElMessage.success('已删除')
    if (selectedSet.value?.id === s.id) { selectedSet.value = null; options.value = [] }
    loadSets()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '删除失败') }
}

// Select set → load options
async function selectSet(s: ResponseSet) {
  selectedSet.value = s
  optionsLoading.value = true
  try {
    options.value = await responseSetApi.getOptions(s.id)
  } catch (e: any) { ElMessage.error(e.message || '加载选项失败') }
  finally { optionsLoading.value = false }
}

// Option CRUD
function openCreateOption() {
  if (!selectedSet.value) return
  editingOptionId.value = null
  optionForm.value = { optionValue: '', optionLabel: '', optionColor: '#409EFF', score: 0, isFlagged: false, sortOrder: options.value.length + 1 }
  optionDialogVisible.value = true
}

function openEditOption(opt: ResponseSetOption) {
  editingOptionId.value = opt.id
  optionForm.value = {
    optionValue: opt.optionValue,
    optionLabel: opt.optionLabel,
    optionColor: opt.optionColor || '#409EFF',
    score: opt.score || 0,
    isFlagged: opt.isFlagged,
    sortOrder: opt.sortOrder,
  }
  optionDialogVisible.value = true
}

async function handleSaveOption() {
  if (!selectedSet.value) return
  if (!optionForm.value.optionValue.trim() || !optionForm.value.optionLabel.trim()) {
    ElMessage.warning('请填写选项值和标签')
    return
  }
  try {
    if (editingOptionId.value) {
      const data: UpdateOptionRequest = {
        optionLabel: optionForm.value.optionLabel,
        optionColor: optionForm.value.optionColor,
        score: optionForm.value.score,
        isFlagged: optionForm.value.isFlagged,
        sortOrder: optionForm.value.sortOrder,
      }
      await responseSetApi.updateOption(selectedSet.value.id, editingOptionId.value, data)
    } else {
      const data: CreateOptionRequest = {
        optionValue: optionForm.value.optionValue,
        optionLabel: optionForm.value.optionLabel,
        optionColor: optionForm.value.optionColor,
        score: optionForm.value.score,
        isFlagged: optionForm.value.isFlagged,
        sortOrder: optionForm.value.sortOrder,
      }
      await responseSetApi.addOption(selectedSet.value.id, data)
    }
    ElMessage.success('保存成功')
    optionDialogVisible.value = false
    selectSet(selectedSet.value)
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

async function handleDeleteOption(opt: ResponseSetOption) {
  if (!selectedSet.value) return
  try {
    await responseSetApi.deleteOption(selectedSet.value.id, opt.id)
    ElMessage.success('已删除')
    selectSet(selectedSet.value)
  } catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

onMounted(loadSets)
</script>

<template>
  <div class="flex h-full p-6">
    <!-- Left: Set list -->
    <div class="w-80 shrink-0 border-r border-gray-200 pr-4">
      <div class="mb-4 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <ListChecks :size="18" class="text-blue-500" />
          <h2 class="text-base font-semibold text-gray-800">选项集</h2>
        </div>
        <button class="rounded bg-blue-500 p-1.5 text-white hover:bg-blue-600" @click="openCreateSet">
          <Plus :size="16" />
        </button>
      </div>
      <div class="relative mb-3">
        <Search :size="14" class="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
        <input
          v-model="queryParams.keyword"
          class="w-full rounded-md border border-gray-300 py-1.5 pl-8 pr-3 text-sm outline-none focus:border-blue-400"
          placeholder="搜索..."
          @keyup.enter="handleSearch"
        />
      </div>
      <div v-if="loading" class="py-8 text-center text-sm text-gray-400">加载中...</div>
      <InspEmptyState v-else-if="sets.length === 0" title="暂无选项集" description="创建可复用选项集供模板使用" action-label="创建选项集" @action="openCreateSet" />
      <div v-else class="space-y-1">
        <div
          v-for="s in sets"
          :key="s.id"
          class="group flex cursor-pointer items-center justify-between rounded-md px-3 py-2 transition"
          :class="selectedSet?.id === s.id ? 'bg-blue-50 text-blue-600' : 'hover:bg-gray-50'"
          @click="selectSet(s)"
        >
          <div>
            <div class="text-sm font-medium">{{ s.setName }}</div>
            <div class="text-xs text-gray-400">{{ s.setCode }}{{ s.isGlobal ? ' · 全局' : '' }}</div>
          </div>
          <div class="flex gap-1 opacity-0 group-hover:opacity-100">
            <button class="text-gray-400 hover:text-blue-500" @click.stop="openEditSet(s)"><Pencil :size="14" /></button>
            <button class="text-gray-400 hover:text-red-500" @click.stop="handleDeleteSet(s)"><Trash2 :size="14" /></button>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Options panel -->
    <div class="flex-1 pl-6">
      <template v-if="selectedSet">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-base font-semibold text-gray-700">{{ selectedSet.setName }} - 选项列表</h3>
          <button class="flex items-center gap-1 rounded-md bg-blue-500 px-3 py-1.5 text-sm text-white hover:bg-blue-600" @click="openCreateOption">
            <Plus :size="14" /> 添加选项
          </button>
        </div>
        <div v-if="optionsLoading" class="py-8 text-center text-sm text-gray-400">加载中...</div>
        <div v-else-if="options.length === 0" class="py-12 text-center text-sm text-gray-400">暂无选项，点击上方按钮添加</div>
        <div v-else class="space-y-1">
          <div
            v-for="opt in options"
            :key="opt.id"
            class="group flex items-center justify-between rounded-lg border border-gray-100 px-4 py-2.5 transition hover:border-blue-200"
          >
            <div class="flex items-center gap-3">
              <GripVertical :size="14" class="text-gray-300" />
              <span
                class="inline-block h-4 w-4 rounded-full border"
                :style="{ backgroundColor: opt.optionColor || '#ddd' }"
              />
              <span class="font-medium text-gray-700">{{ opt.optionLabel }}</span>
              <span class="text-xs text-gray-400">{{ opt.optionValue }}</span>
              <span v-if="opt.score" class="rounded bg-blue-50 px-1.5 py-0.5 text-xs text-blue-600">{{ opt.score }}分</span>
              <span v-if="opt.isFlagged" class="rounded bg-red-50 px-1.5 py-0.5 text-xs text-red-500">标记</span>
            </div>
            <div class="flex gap-1.5 opacity-0 transition group-hover:opacity-100">
              <button class="text-gray-400 hover:text-blue-500" @click="openEditOption(opt)"><Pencil :size="14" /></button>
              <button class="text-gray-400 hover:text-red-500" @click="handleDeleteOption(opt)"><Trash2 :size="14" /></button>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="flex h-full items-center justify-center text-sm text-gray-400">
        ← 选择一个选项集查看选项
      </div>
    </div>

    <!-- Set Dialog -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="setDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @mousedown="onMaskMouseDown" @click="onMaskClose($event, () => setDialogVisible = false)">
          <div class="w-[420px] rounded-lg bg-white p-6 shadow-xl">
            <h3 class="mb-4 text-base font-semibold">{{ setDialogTitle }}</h3>
            <div class="space-y-3">
              <div v-if="!editingSetId">
                <label class="mb-1 block text-sm text-gray-600">编码 <span class="text-red-400">*</span></label>
                <input v-model="setForm.setCode" class="w-full rounded-md border px-3 py-2 text-sm outline-none focus:border-blue-400" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-600">名称 <span class="text-red-400">*</span></label>
                <input v-model="setForm.setName" class="w-full rounded-md border px-3 py-2 text-sm outline-none focus:border-blue-400" />
              </div>
              <label class="flex items-center gap-2 text-sm text-gray-600">
                <input v-model="setForm.isGlobal" type="checkbox" class="rounded" /> 全局选项集
              </label>
            </div>
            <div class="mt-5 flex justify-end gap-3">
              <button class="rounded-md border px-4 py-2 text-sm text-gray-600 hover:bg-gray-50" @click="setDialogVisible = false">取消</button>
              <button class="rounded-md bg-blue-500 px-4 py-2 text-sm text-white hover:bg-blue-600" @click="handleSaveSet">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Option Dialog -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="optionDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @mousedown="onMaskMouseDown" @click="onMaskClose($event, () => optionDialogVisible = false)">
          <div class="w-[420px] rounded-lg bg-white p-6 shadow-xl">
            <h3 class="mb-4 text-base font-semibold">{{ editingOptionId ? '编辑选项' : '添加选项' }}</h3>
            <div class="space-y-3">
              <div>
                <label class="mb-1 block text-sm text-gray-600">选项值 <span class="text-red-400">*</span></label>
                <input v-model="optionForm.optionValue" :disabled="!!editingOptionId" class="w-full rounded-md border px-3 py-2 text-sm outline-none focus:border-blue-400" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-600">显示标签 <span class="text-red-400">*</span></label>
                <input v-model="optionForm.optionLabel" class="w-full rounded-md border px-3 py-2 text-sm outline-none focus:border-blue-400" />
              </div>
              <div class="flex gap-3">
                <div class="flex-1">
                  <label class="mb-1 block text-sm text-gray-600">颜色</label>
                  <input v-model="optionForm.optionColor" type="color" class="h-9 w-full rounded-md border" />
                </div>
                <div class="flex-1">
                  <label class="mb-1 block text-sm text-gray-600">分值</label>
                  <input v-model.number="optionForm.score" type="number" class="w-full rounded-md border px-3 py-2 text-sm outline-none focus:border-blue-400" />
                </div>
                <div class="flex-1">
                  <label class="mb-1 block text-sm text-gray-600">排序</label>
                  <input v-model.number="optionForm.sortOrder" type="number" class="w-full rounded-md border px-3 py-2 text-sm outline-none focus:border-blue-400" />
                </div>
              </div>
              <label class="flex items-center gap-2 text-sm text-gray-600">
                <input v-model="optionForm.isFlagged" type="checkbox" class="rounded" /> 标记为问题项
              </label>
            </div>
            <div class="mt-5 flex justify-end gap-3">
              <button class="rounded-md border px-4 py-2 text-sm text-gray-600 hover:bg-gray-50" @click="optionDialogVisible = false">取消</button>
              <button class="rounded-md bg-blue-500 px-4 py-2 text-sm text-white hover:bg-blue-600" @click="handleSaveOption">保存</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
