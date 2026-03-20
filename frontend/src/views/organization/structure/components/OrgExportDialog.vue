<template>
  <el-dialog
    :model-value="visible"
    title="导出组织数据"
    width="460px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
    @closed="handleClosed"
  >
    <div class="space-y-5">
      <!-- Current org info -->
      <div class="rounded-lg border border-blue-200 bg-blue-50 px-4 py-2.5">
        <p class="text-sm text-blue-800">
          当前组织：<span class="font-semibold">{{ orgUnit?.unitName }}</span>
          <span v-if="orgUnit?.unitType" class="ml-1 text-blue-500">({{ orgUnit.unitType }})</span>
        </p>
      </div>

      <!-- Export scope -->
      <div>
        <label class="mb-2 block text-sm font-medium text-gray-700">导出范围</label>
        <el-radio-group v-model="exportScope">
          <el-radio value="self">本组织</el-radio>
          <el-radio value="recursive">本组织及以下</el-radio>
        </el-radio-group>
        <p v-if="exportScope === 'recursive'" class="mt-1 text-xs text-gray-400">
          将包含所有子组织数据，共 {{ childOrgIds.length }} 个组织
        </p>
      </div>

      <!-- Export content -->
      <div>
        <label class="mb-2 block text-sm font-medium text-gray-700">导出内容</label>
        <el-checkbox-group v-model="exportContents" class="flex flex-col gap-2">
          <el-checkbox value="members" label="归属成员列表（姓名、用户类型、所属组织）" />
          <el-checkbox value="appointments" label="岗位任命关系（姓名、岗位、任职类型、日期）" />
          <el-checkbox value="staffing" label="岗位编制表（岗位名、编制数、在岗数、空缺数）" />
        </el-checkbox-group>
      </div>
    </div>

    <template #footer>
      <div class="flex items-center justify-end gap-3">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button
          type="primary"
          :loading="exporting"
          :disabled="exportContents.length === 0"
          @click="handleExport"
        >
          导出
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import { userPositionApi, positionApi } from '@/api/position'
import type { OrgMember, PositionStaffing, UserPosition } from '@/types/position'
import { AppointmentTypeLabels } from '@/types/position'

interface OrgNode {
  id: number | string
  unitName: string
  unitType?: string
  children?: OrgNode[]
}

interface Props {
  visible: boolean
  orgUnit: OrgNode | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const exportScope = ref<'self' | 'recursive'>('self')
const exportContents = ref<string[]>(['members'])
const exporting = ref(false)

// Recursively collect all org IDs from the tree node
function collectOrgIds(node: OrgNode): (number | string)[] {
  const ids: (number | string)[] = [node.id]
  if (node.children?.length) {
    for (const child of node.children) {
      ids.push(...collectOrgIds(child))
    }
  }
  return ids
}

// Build a flat map of id -> unitName for labeling
function collectOrgNameMap(node: OrgNode): Map<string, string> {
  const map = new Map<string, string>()
  map.set(String(node.id), node.unitName)
  if (node.children?.length) {
    for (const child of node.children) {
      for (const [k, v] of collectOrgNameMap(child)) {
        map.set(k, v)
      }
    }
  }
  return map
}

const childOrgIds = computed<(number | string)[]>(() => {
  if (!props.orgUnit) return []
  return collectOrgIds(props.orgUnit)
})

const targetOrgIds = computed<(number | string)[]>(() => {
  if (!props.orgUnit) return []
  if (exportScope.value === 'self') return [props.orgUnit.id]
  return childOrgIds.value
})

async function handleExport() {
  if (!props.orgUnit || exportContents.value.length === 0) return

  exporting.value = true
  try {
    const wb = XLSX.utils.book_new()
    const orgIds = targetOrgIds.value
    const orgNameMap = props.orgUnit ? collectOrgNameMap(props.orgUnit) : new Map()

    // Fetch data in parallel based on selected content
    const shouldFetchMembers = exportContents.value.includes('members')
    const shouldFetchAppointments = exportContents.value.includes('appointments')
    const shouldFetchStaffing = exportContents.value.includes('staffing')

    const [allMembers, allAppointments, allStaffing] = await Promise.all([
      shouldFetchMembers ? fetchAllMembers(orgIds, orgNameMap) : Promise.resolve([]),
      shouldFetchAppointments ? fetchAllAppointments(orgIds, orgNameMap) : Promise.resolve([]),
      shouldFetchStaffing ? fetchAllStaffing(orgIds, orgNameMap) : Promise.resolve([]),
    ])

    // Build sheets
    if (shouldFetchMembers && allMembers.length > 0) {
      const ws = XLSX.utils.json_to_sheet(allMembers)
      ws['!cols'] = [{ wch: 14 }, { wch: 12 }, { wch: 20 }]
      XLSX.utils.book_append_sheet(wb, ws, '归属成员列表')
    }

    if (shouldFetchAppointments && allAppointments.length > 0) {
      const ws = XLSX.utils.json_to_sheet(allAppointments)
      ws['!cols'] = [{ wch: 14 }, { wch: 16 }, { wch: 12 }, { wch: 14 }, { wch: 14 }, { wch: 20 }]
      XLSX.utils.book_append_sheet(wb, ws, '岗位任命关系')
    }

    if (shouldFetchStaffing && allStaffing.length > 0) {
      const ws = XLSX.utils.json_to_sheet(allStaffing)
      ws['!cols'] = [{ wch: 16 }, { wch: 10 }, { wch: 10 }, { wch: 10 }, { wch: 20 }]
      XLSX.utils.book_append_sheet(wb, ws, '岗位编制表')
    }

    if (wb.SheetNames.length === 0) {
      ElMessage.warning('没有查询到可导出的数据')
      return
    }

    // Trigger download
    const filename = `${props.orgUnit.unitName}_组织数据.xlsx`
    XLSX.writeFile(wb, filename)
    ElMessage.success('导出成功')
    emit('update:visible', false)
  } catch (error: any) {
    ElMessage.error(error.message || '导出失败，请重试')
  } finally {
    exporting.value = false
  }
}

async function fetchAllMembers(
  orgIds: (number | string)[],
  orgNameMap: Map<string, string>
): Promise<Record<string, string>[]> {
  const results: Record<string, string>[] = []
  const promises = orgIds.map(async (orgId) => {
    const members: OrgMember[] = await userPositionApi.getBelongingMembers(orgId)
    const orgName = orgNameMap.get(String(orgId)) || String(orgId)
    for (const m of members) {
      results.push({
        '姓名': m.userName || '',
        '用户类型': m.userTypeCode || '',
        '所属组织': orgName,
      })
    }
  })
  await Promise.all(promises)
  return results
}

async function fetchAllAppointments(
  orgIds: (number | string)[],
  orgNameMap: Map<string, string>
): Promise<Record<string, string>[]> {
  const results: Record<string, string>[] = []
  const promises = orgIds.map(async (orgId) => {
    const positions = await positionApi.getByOrgUnit(orgId)
    const orgName = orgNameMap.get(String(orgId)) || String(orgId)
    for (const pos of positions) {
      const holders: UserPosition[] = await positionApi.getHolders(pos.id)
      for (const h of holders) {
        results.push({
          '姓名': h.userName || '',
          '岗位': pos.positionName,
          '任职类型': AppointmentTypeLabels[h.appointmentType || ''] || h.appointmentType || '',
          '开始日期': h.startDate || '',
          '结束日期': h.endDate || '',
          '所属组织': orgName,
        })
      }
    }
  })
  await Promise.all(promises)
  return results
}

async function fetchAllStaffing(
  orgIds: (number | string)[],
  orgNameMap: Map<string, string>
): Promise<Record<string, string | number>[]> {
  const results: Record<string, string | number>[] = []
  const promises = orgIds.map(async (orgId) => {
    const staffingList: PositionStaffing[] = await positionApi.getStaffing(orgId)
    const orgName = orgNameMap.get(String(orgId)) || String(orgId)
    for (const s of staffingList) {
      results.push({
        '岗位名称': s.positionName,
        '编制数': s.headcount,
        '在岗数': s.currentCount,
        '空缺数': s.vacancies,
        '所属组织': orgName,
      })
    }
  })
  await Promise.all(promises)
  return results
}

function handleClosed() {
  exportScope.value = 'self'
  exportContents.value = ['members']
}

watch(() => props.visible, (val) => {
  if (!val) handleClosed()
})
</script>
