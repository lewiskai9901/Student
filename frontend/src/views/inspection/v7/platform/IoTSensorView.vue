<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2, Activity } from 'lucide-vue-next'
import {
  getSensors,
  createSensor,
  updateSensor,
  deleteSensor,
  activateSensor,
  deactivateSensor,
  getReadings,
} from '@/api/insp/nfcTag'
import type {
  IoTSensor,
  CreateSensorRequest,
  UpdateSensorRequest,
  SensorReading,
  SensorType,
} from '@/types/insp/iot'

// ==================== State ====================

const loading = ref(false)
const sensors = ref<IoTSensor[]>([])

// Dialog
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const dialogTitle = computed(() => (editingId.value ? '编辑传感器' : '新建传感器'))

const form = ref({
  sensorCode: '',
  sensorName: '',
  sensorType: 'TEMPERATURE' as SensorType,
  locationName: '',
  placeId: null as number | null,
  mqttTopic: '',
  dataUnit: '',
})

// Readings panel
const readingsVisible = ref(false)
const readingsLoading = ref(false)
const selectedSensor = ref<IoTSensor | null>(null)
const readings = ref<SensorReading[]>([])

// ==================== Sensor Type Config ====================

const sensorTypeOptions: { value: SensorType; label: string }[] = [
  { value: 'TEMPERATURE', label: '温度' },
  { value: 'HUMIDITY', label: '湿度' },
  { value: 'AIR_QUALITY', label: '空气质量' },
  { value: 'NOISE', label: '噪音' },
  { value: 'LIGHT', label: '光照' },
  { value: 'SMOKE', label: '烟雾' },
  { value: 'WATER', label: '水质' },
]

function sensorTypeLabel(type: SensorType): string {
  return sensorTypeOptions.find(o => o.value === type)?.label ?? type
}

function sensorTypeTagType(type: SensorType): string {
  switch (type) {
    case 'TEMPERATURE': return 'danger'
    case 'HUMIDITY': return ''
    case 'AIR_QUALITY': return 'success'
    case 'NOISE': return 'warning'
    case 'LIGHT': return ''
    case 'SMOKE': return 'danger'
    case 'WATER': return ''
    default: return 'info'
  }
}

// ==================== Data Loading ====================

async function loadData() {
  loading.value = true
  try {
    sensors.value = await getSensors()
  } catch (e: any) {
    ElMessage.error(e.message || '加载传感器列表失败')
  } finally {
    loading.value = false
  }
}

// ==================== CRUD ====================

function openCreate() {
  editingId.value = null
  form.value = {
    sensorCode: '',
    sensorName: '',
    sensorType: 'TEMPERATURE',
    locationName: '',
    placeId: null,
    mqttTopic: '',
    dataUnit: '',
  }
  dialogVisible.value = true
}

function openEdit(sensor: IoTSensor) {
  editingId.value = sensor.id
  form.value = {
    sensorCode: sensor.sensorCode,
    sensorName: sensor.sensorName,
    sensorType: sensor.sensorType,
    locationName: sensor.locationName ?? '',
    placeId: sensor.placeId,
    mqttTopic: sensor.mqttTopic ?? '',
    dataUnit: sensor.dataUnit ?? '',
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.sensorCode.trim()) {
    ElMessage.warning('请输入传感器编码')
    return
  }
  if (!form.value.sensorName.trim()) {
    ElMessage.warning('请输入传感器名称')
    return
  }
  try {
    if (editingId.value) {
      const req: UpdateSensorRequest = {
        sensorName: form.value.sensorName,
        sensorType: form.value.sensorType,
        locationName: form.value.locationName || undefined,
        placeId: form.value.placeId ?? undefined,
        mqttTopic: form.value.mqttTopic || undefined,
        dataUnit: form.value.dataUnit || undefined,
      }
      await updateSensor(editingId.value, req)
      ElMessage.success('更新成功')
    } else {
      const req: CreateSensorRequest = {
        sensorCode: form.value.sensorCode,
        sensorName: form.value.sensorName,
        sensorType: form.value.sensorType,
        locationName: form.value.locationName || undefined,
        placeId: form.value.placeId ?? undefined,
        mqttTopic: form.value.mqttTopic || undefined,
        dataUnit: form.value.dataUnit || undefined,
      }
      await createSensor(req)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(sensor: IoTSensor) {
  try {
    await ElMessageBox.confirm(`确认删除传感器「${sensor.sensorName}」？`, '确认删除', { type: 'warning' })
    await deleteSensor(sensor.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch { /* cancelled */ }
}

async function handleToggleActive(sensor: IoTSensor) {
  try {
    if (sensor.isActive) {
      await deactivateSensor(sensor.id)
      ElMessage.success('已停用')
    } else {
      await activateSensor(sensor.id)
      ElMessage.success('已启用')
    }
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

// ==================== Readings ====================

async function openReadings(sensor: IoTSensor) {
  selectedSensor.value = sensor
  readingsVisible.value = true
  readingsLoading.value = true
  try {
    readings.value = await getReadings(sensor.id, { limit: 50 })
  } catch (e: any) {
    ElMessage.error(e.message || '加载读数失败')
  } finally {
    readingsLoading.value = false
  }
}

function formatReading(value: number | null, unit: string | null): string {
  if (value == null) return '-'
  return unit ? `${value} ${unit}` : `${value}`
}

// ==================== Lifecycle ====================

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">IoT 传感器管理</h2>
      <el-button type="primary" @click="openCreate">
        <Plus class="w-4 h-4 mr-1" />新建传感器
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="sensors" v-loading="loading" stripe>
        <el-table-column prop="sensorCode" label="传感器编码" width="140">
          <template #default="{ row }">
            <span class="font-mono text-sm">{{ row.sensorCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sensorName" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="sensorTypeTagType(row.sensorType)" size="small">
              {{ sensorTypeLabel(row.sensorType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="locationName" label="安装位置" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.locationName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="最近读数" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.lastReading != null" class="font-medium">
              {{ formatReading(row.lastReading, row.dataUnit) }}
            </span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="最近采集" width="160">
          <template #default="{ row }">
            <span v-if="row.lastReadingAt" class="text-xs text-gray-500">{{ row.lastReadingAt }}</span>
            <span v-else class="text-gray-400 text-xs">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'" size="small">
              {{ row.isActive ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-1">
              <el-button link type="primary" size="small" @click="openReadings(row)">
                <Activity class="w-3.5 h-3.5" />
              </el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="传感器编码" required>
          <el-input
            v-model="form.sensorCode"
            placeholder="输入唯一编码"
            :disabled="!!editingId"
          />
          <div v-if="editingId" class="text-xs text-gray-400 mt-1">编码创建后不可修改</div>
        </el-form-item>
        <el-form-item label="传感器名称" required>
          <el-input v-model="form.sensorName" placeholder="输入传感器名称" />
        </el-form-item>
        <el-form-item label="传感器类型" required>
          <el-select v-model="form.sensorType" class="w-full">
            <el-option
              v-for="opt in sensorTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="安装位置">
          <el-input v-model="form.locationName" placeholder="例如：一号楼 3 楼走廊" />
        </el-form-item>
        <el-form-item label="场所 ID">
          <el-input-number v-model="form.placeId" :min="0" controls-position="right" placeholder="可选" />
        </el-form-item>
        <el-form-item label="MQTT 主题">
          <el-input v-model="form.mqttTopic" placeholder="例如：sensors/temp/001" />
        </el-form-item>
        <el-form-item label="数据单位">
          <el-input v-model="form.dataUnit" placeholder="例如：℃, %RH, dB, lux" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- ==================== Readings Panel ==================== -->
    <el-dialog
      v-model="readingsVisible"
      :title="selectedSensor ? `读数记录 - ${selectedSensor.sensorName}` : '读数记录'"
      width="600px"
    >
      <div class="mb-3 text-sm text-gray-500" v-if="selectedSensor">
        <span class="mr-4">编码: <b class="font-mono">{{ selectedSensor.sensorCode }}</b></span>
        <span class="mr-4">类型: {{ sensorTypeLabel(selectedSensor.sensorType) }}</span>
        <span v-if="selectedSensor.dataUnit">单位: {{ selectedSensor.dataUnit }}</span>
      </div>

      <el-table :data="readings" v-loading="readingsLoading" max-height="400" size="small" stripe>
        <el-table-column label="读数值" width="120" align="right">
          <template #default="{ row }">
            <span class="font-medium">{{ row.readingValue }}</span>
            <span v-if="row.readingUnit" class="text-xs text-gray-400 ml-1">{{ row.readingUnit }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="recordedAt" label="采集时间" min-width="180">
          <template #default="{ row }">
            <span class="text-sm">{{ row.recordedAt }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!readingsLoading && readings.length === 0" class="text-center py-8 text-gray-400">
        暂无读数记录
      </div>

      <template #footer>
        <el-button @click="readingsVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
