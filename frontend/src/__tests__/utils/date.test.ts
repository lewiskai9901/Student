import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  formatDate, formatDateTime, formatTime,
  formatRelativeTime, getDateRange, isToday, daysBetween,
} from '@/utils/date'

describe('date utils', () => {
  describe('formatDate', () => {
    it('null/undefined/空 返回 -', () => {
      expect(formatDate(null)).toBe('-')
      expect(formatDate(undefined)).toBe('-')
      expect(formatDate('')).toBe('-')
    })

    it('合法日期格式化', () => {
      expect(formatDate('2026-05-04')).toBe('2026-05-04')
      expect(formatDate(new Date('2026-05-04T00:00:00'))).toBe('2026-05-04')
    })

    it('自定义 format', () => {
      expect(formatDate('2026-05-04', 'MM/DD')).toBe('05/04')
      expect(formatDate('2026-05-04', 'YYYY')).toBe('2026')
    })

    it('非法日期返回 -', () => {
      expect(formatDate('not-a-date')).toBe('-')
      expect(formatDate('xxxxx')).toBe('-')
    })

    it('月日补零', () => {
      expect(formatDate('2026-01-09')).toBe('2026-01-09')
    })
  })

  describe('formatDateTime', () => {
    it('完整格式化', () => {
      expect(formatDateTime('2026-05-04T10:30:45')).toBe('2026-05-04 10:30:45')
    })

    it('自定义 format', () => {
      expect(formatDateTime('2026-05-04T10:30:45', 'MM-DD HH:mm')).toBe('05-04 10:30')
    })

    it('null 返回 -', () => {
      expect(formatDateTime(null)).toBe('-')
    })
  })

  describe('formatTime', () => {
    it('只输出时间', () => {
      expect(formatTime('2026-05-04T10:30:45')).toBe('10:30:45')
    })

    it('null 返回 -', () => {
      expect(formatTime(null)).toBe('-')
    })
  })

  describe('formatRelativeTime', () => {
    beforeEach(() => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-05-04T12:00:00'))
    })
    afterEach(() => { vi.useRealTimers() })

    it('1 年前', () => {
      expect(formatRelativeTime('2025-05-04T12:00:00')).toBe('1年前')
    })

    it('1 个月前', () => {
      expect(formatRelativeTime('2026-04-01T12:00:00')).toBe('1个月前')
    })

    it('5 天前', () => {
      expect(formatRelativeTime('2026-04-29T12:00:00')).toBe('5天前')
    })

    it('null 返回 -', () => {
      expect(formatRelativeTime(null)).toBe('-')
    })
  })

  describe('getDateRange', () => {
    beforeEach(() => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-05-15T10:00:00'))
    })
    afterEach(() => { vi.useRealTimers() })

    it('today 返回单日范围 (含时间)', () => {
      const [start, end] = getDateRange('today')
      expect(start.startsWith('2026-05-15')).toBe(true)
      expect(end.startsWith('2026-05-15')).toBe(true)
    })

    it('month 返回当月起止', () => {
      const [start, end] = getDateRange('month')
      expect(start.startsWith('2026-05-01')).toBe(true)
      expect(end.startsWith('2026-05-31')).toBe(true)
    })

    it('year 返回全年', () => {
      const [start, end] = getDateRange('year')
      expect(start.startsWith('2026-01-01')).toBe(true)
      expect(end.startsWith('2026-12-31')).toBe(true)
    })
  })

  describe('isToday', () => {
    beforeEach(() => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-05-04T15:00:00'))
    })
    afterEach(() => { vi.useRealTimers() })

    it('今天返回 true', () => {
      expect(isToday('2026-05-04T08:00:00')).toBe(true)
    })

    it('昨天返回 false', () => {
      expect(isToday('2026-05-03T23:59:59')).toBe(false)
    })
  })

  describe('daysBetween', () => {
    it('5 天差', () => {
      expect(daysBetween('2026-05-01', '2026-05-06')).toBe(5)
    })

    it('反向也是 5 天 (绝对值)', () => {
      expect(daysBetween('2026-05-06', '2026-05-01')).toBe(5)
    })

    it('同一天返回 0', () => {
      expect(daysBetween('2026-05-01', '2026-05-01')).toBe(0)
    })
  })
})
