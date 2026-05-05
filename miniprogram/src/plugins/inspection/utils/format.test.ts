import { describe, it, expect } from 'vitest'
import { taskStatusLabel, taskStatusColor, formatDateTime } from './format'

describe('format helpers', () => {
  it('returns label for known task statuses', () => {
    expect(taskStatusLabel('PENDING')).toBe('待处理')
    expect(taskStatusLabel('IN_PROGRESS')).toBe('进行中')
    expect(taskStatusLabel('SUBMITTED')).toBe('已提交')
    expect(taskStatusLabel('APPROVED')).toBe('已通过')
    expect(taskStatusLabel('REJECTED')).toBe('已驳回')
  })

  it('returns hyphen for unknown / undefined status', () => {
    expect(taskStatusLabel(undefined as any)).toBe('-')
    expect(taskStatusLabel('UNKNOWN' as any)).toBe('-')
  })

  it('returns color hex for known task statuses', () => {
    expect(taskStatusColor('PENDING')).toMatch(/^#[0-9a-f]{6}$/i)
    expect(taskStatusColor('APPROVED')).toMatch(/^#[0-9a-f]{6}$/i)
  })

  it('formats ISO datetime to yyyy-MM-dd HH:mm', () => {
    expect(formatDateTime('2026-05-05T14:30:00')).toBe('2026-05-05 14:30')
    expect(formatDateTime('2026-05-05T14:30:00.123Z')).toBe('2026-05-05 14:30')
  })

  it('returns hyphen for empty/null datetime', () => {
    expect(formatDateTime(undefined)).toBe('-')
    expect(formatDateTime('')).toBe('-')
  })
})
