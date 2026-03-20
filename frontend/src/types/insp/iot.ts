/**
 * V7 检查平台 - IoT/NFC 集成类型
 */

// ==================== NFC 标签 ====================

export interface NfcTag {
  id: number
  tenantId?: number
  tagUid: string
  locationName: string
  placeId: number | null
  orgUnitId: number | null
  isActive: boolean
  createdAt: string
}

export interface CreateNfcTagRequest {
  tagUid: string
  locationName: string
  placeId?: number
  orgUnitId?: number
}

export interface UpdateNfcTagRequest {
  locationName?: string
  placeId?: number
  orgUnitId?: number
}

// ==================== IoT 传感器 ====================

export type SensorType = 'TEMPERATURE' | 'HUMIDITY' | 'AIR_QUALITY' | 'NOISE' | 'LIGHT' | 'SMOKE' | 'WATER'

export interface IoTSensor {
  id: number
  tenantId?: number
  sensorCode: string
  sensorName: string
  sensorType: SensorType
  locationName: string | null
  placeId: number | null
  mqttTopic: string | null
  dataUnit: string | null
  isActive: boolean
  lastReading: number | null
  lastReadingAt: string | null
  createdAt: string
}

export interface CreateSensorRequest {
  sensorCode: string
  sensorName: string
  sensorType: SensorType
  locationName?: string
  placeId?: number
  mqttTopic?: string
  dataUnit?: string
}

export interface UpdateSensorRequest {
  sensorName?: string
  sensorType?: SensorType
  locationName?: string
  placeId?: number
  mqttTopic?: string
  dataUnit?: string
}

export interface SensorReading {
  id: number
  sensorId: number
  readingValue: number
  readingUnit: string | null
  recordedAt: string
}

export interface RecordReadingRequest {
  readingValue: number
  readingUnit?: string
  recordedAt?: string
}

// ==================== 检查项-传感器绑定 ====================

export interface ItemSensorBinding {
  id: number
  tenantId?: number
  templateItemId: number
  sensorId: number
  autoFill: boolean
  autoScore: boolean
  scoringThresholds: string | null // JSON
  createdAt: string
}

export interface CreateBindingRequest {
  templateItemId: number
  sensorId: number
  autoFill?: boolean
  autoScore?: boolean
  scoringThresholds?: string
}

// ==================== IM 平台 ====================

export type IMPlatform = 'GENERIC' | 'DINGTALK' | 'WECOM' | 'FEISHU' | 'SLACK'
