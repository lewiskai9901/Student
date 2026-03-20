<template>
  <div v-if="alerts.length > 0" class="capacity-alert-banner">
    <el-alert
      :title="alertTitle"
      :type="alertType"
      :closable="false"
      show-icon
    >
      <template #default>
        <div class="alert-content">
          <div class="summary">
            <span class="summary-item">
              <el-tag type="warning" size="small">警告</el-tag>
              {{ summary.warningCount }} 个
            </span>
            <span class="summary-item">
              <el-tag type="danger" size="small">严重</el-tag>
              {{ summary.criticalCount }} 个
            </span>
            <span class="summary-item">
              <el-tag type="danger" size="small">已满</el-tag>
              {{ summary.fullCount }} 个
            </span>
          </div>

          <el-collapse v-if="collapsible" v-model="activeNames" class="alert-list">
            <el-collapse-item name="details" title="查看详情">
              <el-table :data="alerts" size="small" max-height="300">
                <el-table-column prop="placeName" label="场所" min-width="150" />
                <el-table-column label="占用率" width="120">
                  <template #default="{ row }">
                    <el-progress
                      :percentage="row.occupancyRate"
                      :status="getProgressStatus(row.alertLevel)"
                      :show-text="true"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="容量" width="100">
                  <template #default="{ row }">
                    {{ row.currentOccupancy }} / {{ row.capacity }}
                  </template>
                </el-table-column>
                <el-table-column label="级别" width="80">
                  <template #default="{ row }">
                    <el-tag :type="getAlertTagType(row.alertLevel)" size="small">
                      {{ getAlertLevelLabel(row.alertLevel) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="responsibleUserName" label="负责人" width="100" />
              </el-table>
            </el-collapse-item>
          </el-collapse>

          <div v-else class="simple-list">
            <div v-for="alert in alerts.slice(0, 5)" :key="alert.placeId" class="alert-item">
              <el-tag :type="getAlertTagType(alert.alertLevel)" size="small">
                {{ alert.placeName }}
              </el-tag>
              <span class="occupancy">{{ alert.currentOccupancy }}/{{ alert.capacity }}</span>
              <span class="rate">{{ alert.occupancyRate.toFixed(1) }}%</span>
            </div>
            <div v-if="alerts.length > 5" class="more">
              还有 {{ alerts.length - 5 }} 个场所...
            </div>
          </div>
        </div>
      </template>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getHighOccupancyAlerts } from '@/api/capacityAlert'
import type { CapacityAlertDTO, AlertLevel } from '@/types/capacityAlert'

const props = withDefaults(
  defineProps<{
    typeCode?: string
    refreshInterval?: number // 秒
    collapsible?: boolean
    defaultExpanded?: boolean
  }>(),
  {
    refreshInterval: 60,
    collapsible: true,
    defaultExpanded: false
  }
)

const alerts = ref<CapacityAlertDTO[]>([])
const activeNames = ref<string[]>(props.defaultExpanded ? ['details'] : [])
let refreshTimer: NodeJS.Timeout | null = null

const summary = computed(() => {
  const warningCount = alerts.value.filter(a => a.alertLevel === 'WARNING').length
  const criticalCount = alerts.value.filter(a => a.alertLevel === 'CRITICAL').length
  const fullCount = alerts.value.filter(a => a.alertLevel === 'FULL').length
  return { warningCount, criticalCount, fullCount }
})

const alertTitle = computed(() => {
  const total = alerts.value.length
  return `容量告警提醒：发现 ${total} 个场所占用率≥80%`
})

const alertType = computed((): 'success' | 'warning' | 'error' | 'info' => {
  if (summary.value.fullCount > 0 || summary.value.criticalCount > 0) return 'error'
  if (summary.value.warningCount > 0) return 'warning'
  return 'info'
})

const loadAlerts = async () => {
  try {
    alerts.value = await getHighOccupancyAlerts(props.typeCode)
  } catch (error) {
    console.error('加载容量告警失败:', error)
  }
}

const getAlertLevelLabel = (level: AlertLevel): string => {
  const labels: Record<AlertLevel, string> = {
    WARNING: '警告',
    CRITICAL: '严重',
    FULL: '已满'
  }
  return labels[level]
}

const getAlertTagType = (level: AlertLevel): 'success' | 'warning' | 'danger' | 'info' => {
  if (level === 'FULL' || level === 'CRITICAL') return 'danger'
  if (level === 'WARNING') return 'warning'
  return 'info'
}

const getProgressStatus = (level: AlertLevel): 'success' | 'warning' | 'exception' | undefined => {
  if (level === 'FULL' || level === 'CRITICAL') return 'exception'
  if (level === 'WARNING') return 'warning'
  return undefined
}

const startAutoRefresh = () => {
  if (props.refreshInterval) {
    refreshTimer = setInterval(() => {
      loadAlerts()
    }, props.refreshInterval * 1000)
  }
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadAlerts()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})

defineExpose({
  loadAlerts
})
</script>

<style scoped lang="scss">
.capacity-alert-banner {
  margin-bottom: 16px;

  .alert-content {
    .summary {
      display: flex;
      gap: 16px;
      margin-bottom: 12px;

      .summary-item {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
      }
    }

    .alert-list {
      margin-top: 12px;
    }

    .simple-list {
      .alert-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 6px 0;

        .occupancy {
          font-size: 13px;
          color: var(--el-text-color-secondary);
        }

        .rate {
          font-weight: 500;
          color: var(--el-color-danger);
        }
      }

      .more {
        padding: 6px 0;
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}
</style>
