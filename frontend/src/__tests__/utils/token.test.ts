import { describe, it, expect, vi, beforeEach } from 'vitest'
import { parseJwtPayload, isTokenExpired, getTokenRemainingTime } from '@/utils/token'

/** 构造一个 JWT-like token (header + base64 payload + signature). */
function makeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

describe('token utils', () => {
  describe('parseJwtPayload', () => {
    it('解析合法 JWT', () => {
      const t = makeJwt({ sub: 'admin', exp: 1234567890 })
      expect(parseJwtPayload(t)).toEqual({ sub: 'admin', exp: 1234567890 })
    })

    it('段数不对返回 null', () => {
      expect(parseJwtPayload('invalid')).toBeNull()
      expect(parseJwtPayload('a.b')).toBeNull()
      expect(parseJwtPayload('a.b.c.d')).toBeNull()
    })

    it('payload 不是合法 JSON 返回 null', () => {
      expect(parseJwtPayload('header.notjson.sig')).toBeNull()
    })

    it('支持 base64url 字符 (- _)', () => {
      // base64url 中 - 和 _ 替代 + 和 /
      const t = makeJwt({ x: 'a+b/c' })
      // parseJwtPayload 内部转回标准 base64 也应能解码
      expect(parseJwtPayload(t)?.x).toBe('a+b/c')
    })
  })

  describe('isTokenExpired', () => {
    beforeEach(() => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
    })

    it('null 视为过期', () => {
      expect(isTokenExpired(null)).toBe(true)
    })

    it('exp 缺失视为过期', () => {
      const t = makeJwt({ sub: 'x' })
      expect(isTokenExpired(t)).toBe(true)
    })

    it('exp 大于当前时间 + buffer 未过期', () => {
      const future = Math.floor(Date.now() / 1000) + 3600  // 1 小时后
      const t = makeJwt({ exp: future })
      expect(isTokenExpired(t)).toBe(false)
    })

    it('exp 小于当前时间已过期', () => {
      const past = Math.floor(Date.now() / 1000) - 100
      const t = makeJwt({ exp: past })
      expect(isTokenExpired(t)).toBe(true)
    })

    it('exp 在 buffer 区内视为过期', () => {
      const nearFuture = Math.floor(Date.now() / 1000) + 30  // 30 秒后, buffer=60
      const t = makeJwt({ exp: nearFuture })
      expect(isTokenExpired(t, 60)).toBe(true)
    })

    it('自定义 buffer=0', () => {
      const nearFuture = Math.floor(Date.now() / 1000) + 30
      const t = makeJwt({ exp: nearFuture })
      expect(isTokenExpired(t, 0)).toBe(false)
    })
  })

  describe('getTokenRemainingTime', () => {
    beforeEach(() => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
    })

    it('null 返回 0', () => {
      expect(getTokenRemainingTime(null)).toBe(0)
    })

    it('已过期返回 0', () => {
      const past = Math.floor(Date.now() / 1000) - 100
      expect(getTokenRemainingTime(makeJwt({ exp: past }))).toBe(0)
    })

    it('未过期返回剩余毫秒', () => {
      const future = Math.floor(Date.now() / 1000) + 3600  // 1h
      const remain = getTokenRemainingTime(makeJwt({ exp: future }))
      expect(remain).toBeGreaterThan(3500_000)
      expect(remain).toBeLessThanOrEqual(3600_000)
    })

    it('exp 缺失返回 0', () => {
      expect(getTokenRemainingTime(makeJwt({ sub: 'x' }))).toBe(0)
    })
  })
})
