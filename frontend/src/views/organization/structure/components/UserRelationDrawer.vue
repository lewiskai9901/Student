<template>
  <el-drawer
    :model-value="visible"
    title=""
    :size="480"
    @update:model-value="emit('update:visible', $event)"
  >
    <template #header>
      <div class="flex items-center gap-2">
        <UserCircle class="h-5 w-5 text-gray-500" />
        <span class="text-base font-semibold text-gray-900">用户关系 — {{ userName }}</span>
      </div>
    </template>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
      <span class="ml-2 text-sm text-gray-500">加载中...</span>
    </div>

    <div v-else class="space-y-5">
      <!-- 基本信息 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-100 px-4 py-2.5 text-xs font-semibold text-gray-700">基本信息</div>
        <div class="grid grid-cols-2 gap-x-6 gap-y-3 px-4 py-3">
          <div>
            <span class="text-xs text-gray-500">归属组织</span>
            <div class="mt-0.5 flex items-center gap-1.5 text-sm text-gray-900">
              {{ userInfo?.orgUnitName || '(未分配)' }}
              <button
                class="rounded p-0.5 text-gray-400 hover:bg-blue-50 hover:text-blue-600"
                title="变更组织"
                @click="showChangeOrg = true"
              >
                <ArrowRightLeft class="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
          <div>
            <span class="text-xs text-gray-500">用户类型</span>
            <div class="mt-0.5 text-sm text-gray-900">{{ userInfo?.userType || '-' }}</div>
          </div>
          <div>
            <span class="text-xs text-gray-500">用户名</span>
            <div class="mt-0.5 text-sm text-gray-900">{{ userInfo?.username || '-' }}</div>
          </div>
          <div>
            <span class="text-xs text-gray-500">姓名</span>
            <div class="mt-0.5 text-sm text-gray-900">{{ userInfo?.realName || '-' }}</div>
          </div>
        </div>
      </div>

      <!-- 岗位任命 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="flex items-center justify-between border-b border-gray-100 px-4 py-2.5">
          <span class="text-xs font-semibold text-gray-700">
            岗位任命 ({{ currentPositions.length }})
          </span>
          <button
            class="inline-flex items-center gap-1 rounded px-2 py-1 text-xs text-blue-600 hover:bg-blue-50"
            @click="emit('update:visible', false)"
          >
            到岗位编制 Tab 任命
          </button>
        </div>

        <div v-if="positions.length === 0" class="px-4 py-6 text-center text-sm text-gray-400">
          暂无岗位任命记录
        </div>
        <div v-else class="divide-y divide-gray-50">
          <div
            v-for="pos in positions"
            :key="pos.id"
            class="px-4 py-3"
            :class="{ 'opacity-50': !pos.isCurrent }"
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="text-sm font-medium text-gray-900">{{ pos.positionName }}</span>
                <span
                  class="rounded-full px-1.5 py-0.5 text-[10px] font-medium"
                  :class="appointmentTagClass(pos.appointmentType || '')"
                >
                  {{ AppointmentTypeLabels[pos.appointmentType || ''] || pos.appointmentType }}
                </span>
                <span
                  v-if="pos.isPrimary"
                  class="rounded bg-blue-50 px-1.5 py-0.5 text-[10px] font-medium text-blue-600"
                >主岗</span>
              </div>
              <button
                v-if="pos.isCurrent"
                class="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-500"
                title="离任"
                @click="handleEndAppointment(pos)"
              >
                <X class="h-3.5 w-3.5" />
              </button>
              <span v-else class="text-[10px] text-gray-400">已离任</span>
            </div>
            <div class="mt-1 flex items-center gap-2 text-xs text-gray-500">
              <span v-if="pos.orgUnitName" class="text-gray-400">{{ pos.orgUnitName }}</span>
              <span class="text-gray-300">|</span>
              <span>{{ pos.startDate || '-' }} ~ {{ pos.endDate || '至今' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 变更组织弹窗 -->
    <div
      v-if="showChangeOrg"
      class="fixed inset-0 z-[2100] flex items-center justify-center bg-black/40"
      @click.self="showChangeOrg = false"
    >
      <div class="w-[400px] rounded-xl bg-white p-6 shadow-xl">
        <h3 class="mb-4 text-base font-semibold text-gray-900">变更归属组织</h3>
        <el-tree-select
          v-model="changeOrgTarget"
          :data="orgTreeOptions"
          :props="{ label: 'unitName', value: 'id', children: 'children' }"
          placeholder="选择目标组织"
          style="width: 100%"
          filterable
          check-strictly
        />
        <div class="mt-5 flex justify-end gap-3">
          <button
            class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="showChangeOrg = false"
          >取消</button>
          <button
            :disabled="!changeOrgTarget || changeOrgSubmitting"
            class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            @click="handleChangeOrg"
          >
            {{ changeOrgSubmitting ? '变更中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { UserCircle, Loader2, X, ArrowRightLeft } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userPositionApi } from '@/api/position'
import { getSimpleUserList } from '@/api/user'
import type { UserPosition } from '@/types/position'
import { AppointmentTypeLabels } from '@/types/position'
import type { SimpleUser } from '@/types/user'
import type { DepartmentResponse } from '@/api/organization'

interface Props {
  visible: boolean
  userId: number | string | null
  userName: string
  treeData: DepartmentResponse[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [v: boolean]
  changed: []
}>()

const loading = ref(false)
const userInfo = ref<SimpleUser | null>(null)
const positions = ref<UserPosition[]>([])

const currentPositions = computed(() => positions.value.filter(p => p.isCurrent))

const loadData = async () => {
  if (!props.userId) return
  loading.value = true
  try {
    const [allUsers, userPositions] = await Promise.all([
      getSimpleUserList(),
      userPositionApi.getByUser(props.userId),
    ])
    userInfo.value = allUsers.find(u => String(u.id) === String(props.userId)) || null
    positions.value = userPositions
  } catch (e: any) {
    console.error('Failed to load user relation data', e)
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.visible, props.userId],
  ([vis]) => {
    if (vis && props.userId) loadData()
  },
  { immediate: true }
)

const appointmentTagClass = (type: string) => {
  switch (type) {
    case 'FORMAL': return 'bg-blue-50 text-blue-700'
    case 'ACTING': return 'bg-amber-50 text-amber-700'
    case 'CONCURRENT': return 'bg-purple-50 text-purple-700'
    case 'PROBATION': return 'bg-gray-100 text-gray-600'
    default: return 'bg-gray-100 text-gray-600'
  }
}

const handleEndAppointment = async (pos: UserPosition) => {
  try {
    await ElMessageBox.confirm(
      `确定要将「${props.userName}」从岗位「${pos.positionName}」离任吗？`,
      '确认离任',
      { type: 'warning' }
    )
    await userPositionApi.endAppointment(pos.id, {
      endDate: new Date().toISOString().slice(0, 10),
      reason: '管理员操作离任',
    })
    ElMessage.success('已离任')
    emit('changed')
    await loadData()
  } catch {
    // cancelled
  }
}

// ==================== Change Org ====================
const showChangeOrg = ref(false)
const changeOrgTarget = ref<number | null>(null)
const changeOrgSubmitting = ref(false)

const orgTreeOptions = computed(() => props.treeData)

const handleChangeOrg = async () => {
  if (!changeOrgTarget.value || !props.userId) return
  changeOrgSubmitting.value = true
  try {
    await userPositionApi.addMember(changeOrgTarget.value, props.userId)
    ElMessage.success('归属组织已变更')
    showChangeOrg.value = false
    changeOrgTarget.value = null
    emit('changed')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '变更失败')
  } finally {
    changeOrgSubmitting.value = false
  }
}
</script>
