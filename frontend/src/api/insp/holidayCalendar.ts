/**
 * V7 检查平台 - 假日日历 API
 */
import { http } from '@/utils/request'
import type { HolidayCalendar } from '@/types/insp/platform'

const BASE = '/v7/insp/holiday-calendars'

// ==================== CRUD ====================

export function listHolidayCalendars(): Promise<HolidayCalendar[]> {
  return http.get<HolidayCalendar[]>(BASE)
}

export function getHolidayCalendar(id: number): Promise<HolidayCalendar> {
  return http.get<HolidayCalendar>(`${BASE}/${id}`)
}

export function getByYear(year: number): Promise<HolidayCalendar[]> {
  return http.get<HolidayCalendar[]>(`${BASE}/by-year`, { params: { year } })
}

export function getDefaultCalendar(): Promise<HolidayCalendar> {
  return http.get<HolidayCalendar>(`${BASE}/default`)
}

export function createHolidayCalendar(data: Partial<HolidayCalendar>): Promise<HolidayCalendar> {
  return http.post<HolidayCalendar>(BASE, data)
}

export function updateHolidayCalendar(id: number, data: Partial<HolidayCalendar>): Promise<HolidayCalendar> {
  return http.put<HolidayCalendar>(`${BASE}/${id}`, data)
}

export function deleteHolidayCalendar(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== API 对象 ====================

export const holidayCalendarApi = {
  list: listHolidayCalendars,
  getById: getHolidayCalendar,
  getByYear,
  getDefault: getDefaultCalendar,
  create: createHolidayCalendar,
  update: updateHolidayCalendar,
  delete: deleteHolidayCalendar,
}
