/**
 * V7 检查平台 - NFC/IoT 集成 API
 */
import { http } from '@/utils/request'
import type {
  NfcTag,
  CreateNfcTagRequest,
  UpdateNfcTagRequest,
  IoTSensor,
  CreateSensorRequest,
  UpdateSensorRequest,
  SensorReading,
  RecordReadingRequest,
  ItemSensorBinding,
  CreateBindingRequest,
} from '@/types/insp/iot'

const NFC_BASE = '/v7/insp/nfc-tags'
const IOT_BASE = '/v7/insp/iot-sensors'

// ==================== NFC Tags ====================

export function getNfcTags(): Promise<NfcTag[]> {
  return http.get<NfcTag[]>(NFC_BASE)
}

export function getNfcTag(id: number): Promise<NfcTag> {
  return http.get<NfcTag>(`${NFC_BASE}/${id}`)
}

export function getNfcTagByUid(uid: string): Promise<NfcTag> {
  return http.get<NfcTag>(`${NFC_BASE}/by-uid/${uid}`)
}

export function createNfcTag(data: CreateNfcTagRequest): Promise<NfcTag> {
  return http.post<NfcTag>(NFC_BASE, data)
}

export function updateNfcTag(id: number, data: UpdateNfcTagRequest): Promise<NfcTag> {
  return http.put<NfcTag>(`${NFC_BASE}/${id}`, data)
}

export function deleteNfcTag(id: number): Promise<void> {
  return http.delete(`${NFC_BASE}/${id}`)
}

export function activateNfcTag(id: number): Promise<NfcTag> {
  return http.put<NfcTag>(`${NFC_BASE}/${id}/activate`)
}

export function deactivateNfcTag(id: number): Promise<NfcTag> {
  return http.put<NfcTag>(`${NFC_BASE}/${id}/deactivate`)
}

// ==================== IoT Sensors ====================

export function getSensors(): Promise<IoTSensor[]> {
  return http.get<IoTSensor[]>(IOT_BASE)
}

export function getSensor(id: number): Promise<IoTSensor> {
  return http.get<IoTSensor>(`${IOT_BASE}/${id}`)
}

export function createSensor(data: CreateSensorRequest): Promise<IoTSensor> {
  return http.post<IoTSensor>(IOT_BASE, data)
}

export function updateSensor(id: number, data: UpdateSensorRequest): Promise<IoTSensor> {
  return http.put<IoTSensor>(`${IOT_BASE}/${id}`, data)
}

export function deleteSensor(id: number): Promise<void> {
  return http.delete(`${IOT_BASE}/${id}`)
}

export function activateSensor(id: number): Promise<IoTSensor> {
  return http.put<IoTSensor>(`${IOT_BASE}/${id}/activate`)
}

export function deactivateSensor(id: number): Promise<IoTSensor> {
  return http.put<IoTSensor>(`${IOT_BASE}/${id}/deactivate`)
}

// ==================== Sensor Readings ====================

export function getReadings(sensorId: number, params?: { limit?: number; from?: string; to?: string }): Promise<SensorReading[]> {
  return http.get<SensorReading[]>(`${IOT_BASE}/${sensorId}/readings`, { params })
}

export function recordReading(sensorId: number, data: RecordReadingRequest): Promise<SensorReading> {
  return http.post<SensorReading>(`${IOT_BASE}/${sensorId}/readings`, data)
}

// ==================== Sensor Bindings ====================

export function getBindings(params?: { templateItemId?: number; sensorId?: number }): Promise<ItemSensorBinding[]> {
  return http.get<ItemSensorBinding[]>(`${IOT_BASE}/bindings`, { params })
}

export function createBinding(data: CreateBindingRequest): Promise<ItemSensorBinding> {
  return http.post<ItemSensorBinding>(`${IOT_BASE}/bindings`, data)
}

export function deleteBinding(id: number): Promise<void> {
  return http.delete(`${IOT_BASE}/bindings/${id}`)
}

export const inspNfcIoTApi = {
  getNfcTags,
  getNfcTag,
  getNfcTagByUid,
  createNfcTag,
  updateNfcTag,
  deleteNfcTag,
  activateNfcTag,
  deactivateNfcTag,
  getSensors,
  getSensor,
  createSensor,
  updateSensor,
  deleteSensor,
  activateSensor,
  deactivateSensor,
  getReadings,
  recordReading,
  getBindings,
  createBinding,
  deleteBinding,
}
