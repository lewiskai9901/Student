<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import {
  getNfcTags,
  createNfcTag,
  updateNfcTag,
  deleteNfcTag,
  activateNfcTag,
  deactivateNfcTag,
} from '@/api/insp/nfcTag'
import type { NfcTag, CreateNfcTagRequest, UpdateNfcTagRequest } from '@/types/insp/iot'

// ==================== State ====================

const loading = ref(false)
const tags = ref<NfcTag[]>([])

// Dialog
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const dialogTitle = computed(() => (editingId.value ? '编辑 NFC 标签' : '新建 NFC 标签'))

const form = ref({
  tagUid: '',
  locationName: '',
  placeId: null as number | null,
  orgUnitId: null as number | null,
})

// ==================== Data Loading ====================

async function loadData() {
  loading.value = true
  try {
    tags.value = await getNfcTags()
  } catch (e: any) {
    ElMessage.error(e.message || '加载 NFC 标签列表失败')
  } finally {
    loading.value = false
  }
}

// ==================== CRUD ====================

function openCreate() {
  editingId.value = null
  form.value = { tagUid: '', locationName: '', placeId: null, orgUnitId: null }
  dialogVisible.value = true
}

function openEdit(tag: NfcTag) {
  editingId.value = tag.id
  form.value = {
    tagUid: tag.tagUid,
    locationName: tag.locationName,
    placeId: tag.placeId,
    orgUnitId: tag.orgUnitId,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.tagUid.trim()) {
    ElMessage.warning('请输入标签 UID')
    return
  }
  if (!form.value.locationName.trim()) {
    ElMessage.warning('请输入位置名称')
    return
  }
  try {
    if (editingId.value) {
      const req: UpdateNfcTagRequest = {
        locationName: form.value.locationName,
        placeId: form.value.placeId ?? undefined,
        orgUnitId: form.value.orgUnitId ?? undefined,
      }
      await updateNfcTag(editingId.value, req)
      ElMessage.success('更新成功')
    } else {
      const req: CreateNfcTagRequest = {
        tagUid: form.value.tagUid,
        locationName: form.value.locationName,
        placeId: form.value.placeId ?? undefined,
        orgUnitId: form.value.orgUnitId ?? undefined,
      }
      await createNfcTag(req)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(tag: NfcTag) {
  try {
    await ElMessageBox.confirm(`确认删除 NFC 标签「${tag.tagUid}」？`, '确认删除', { type: 'warning' })
    await deleteNfcTag(tag.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch { /* cancelled */ }
}

async function handleToggleActive(tag: NfcTag) {
  try {
    if (tag.isActive) {
      await deactivateNfcTag(tag.id)
      ElMessage.success('已停用')
    } else {
      await activateNfcTag(tag.id)
      ElMessage.success('已启用')
    }
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

// ==================== Lifecycle ====================

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">NFC 标签管理</h2>
      <el-button type="primary" @click="openCreate">
        <Plus class="w-4 h-4 mr-1" />新建标签
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="tags" v-loading="loading" stripe>
        <el-table-column prop="tagUid" label="标签 UID" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-mono text-sm">{{ row.tagUid }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="locationName" label="位置名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="场所 ID" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.placeId">{{ row.placeId }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="组织 ID" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.orgUnitId">{{ row.orgUnitId }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'" size="small">
              {{ row.isActive ? '已启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            <span class="text-xs text-gray-500">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-1">
              <el-button link type="primary" size="small" @click="handleToggleActive(row)">
                {{ row.isActive ? '停用' : '启用' }}
              </el-button>
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
    </el-card>

    <!-- ==================== Create/Edit Dialog ==================== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标签 UID" required>
          <el-input
            v-model="form.tagUid"
            placeholder="输入 NFC 标签 UID"
            :disabled="!!editingId"
          />
          <div v-if="editingId" class="text-xs text-gray-400 mt-1">标签 UID 创建后不可修改</div>
        </el-form-item>
        <el-form-item label="位置名称" required>
          <el-input v-model="form.locationName" placeholder="例如：一号楼 301 室" />
        </el-form-item>
        <el-form-item label="场所 ID">
          <el-input-number v-model="form.placeId" :min="0" controls-position="right" placeholder="可选" />
        </el-form-item>
        <el-form-item label="组织 ID">
          <el-input-number v-model="form.orgUnitId" :min="0" controls-position="right" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
