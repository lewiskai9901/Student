import type { LongId } from '@/types/common'

/**
 * 检查平台 - IoT/NFC 集成类型
 */

// ==================== NFC 标签 ====================

export interface NfcTag {
  id: LongId
  tenantId?: LongId
  tagUid: string
  locationName: string
  placeId: LongId | null
  orgUnitId: LongId | null
  isActive: boolean
  createdAt: string
}

export interface CreateNfcTagRequest {
  tagUid: string
  locationName: string
  placeId?: LongId
  orgUnitId?: LongId
}

export interface UpdateNfcTagRequest {
  locationName?: string
  placeId?: LongId
  orgUnitId?: LongId
}

// ==================== IoT 传感器 ====================

export type SensorType = 'TEMPERATURE' | 'HUMIDITY' | 'AIR_QUALITY' | 'NOISE' | 'LIGHT' | 'SMOKE' | 'WATER'

export interface IoTSensor {
  id: LongId
  tenantId?: LongId
  sensorCode: string
  sensorName: string
  sensorType: SensorType
  locationName: string | null
  placeId: LongId | null
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
  placeId?: LongId
  mqttTopic?: string
  dataUnit?: string
}

export interface UpdateSensorRequest {
  sensorName?: string
  sensorType?: SensorType
  locationName?: string
  placeId?: LongId
  mqttTopic?: string
  dataUnit?: string
}

export interface SensorReading {
  id: LongId
  sensorId: LongId
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
  id: LongId
  tenantId?: LongId
  templateItemId: LongId
  sensorId: LongId
  autoFill: boolean
  autoScore: boolean
  scoringThresholds: string | null // JSON
  createdAt: string
}

export interface CreateBindingRequest {
  templateItemId: LongId
  sensorId: LongId
  autoFill?: boolean
  autoScore?: boolean
  scoringThresholds?: string
}

// ==================== IM 平台 ====================

export type IMPlatform = 'GENERIC' | 'DINGTALK' | 'WECOM' | 'FEISHU' | 'SLACK'
