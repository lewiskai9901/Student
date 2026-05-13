import { describe, it, expect, beforeAll } from 'vitest'

/**
 * Tier 6 Block B-6: 小程序与真 backend 的 API 契约测.
 *
 * 现有 inspection.test.ts / auth.test.ts 测的是 mock uni.request 的拼接逻辑.
 * 本文件用 node fetch 真打 backend, 验证 backend 实际返回的 JSON 字段集是否
 * 与小程序 types.ts (InspTask / LoginResp) 对齐 — 防止后端改字段名时小程序静默失败.
 *
 * !! 关键契约 !! JacksonConfig.java:37 把所有 Long 序列化为 string (防 JS 53-bit
 * 大数丢精度). 所以 backend.id/userId/projectId/expiresIn 实际是 string, 不是 number.
 * 小程序 types.ts 标 number 是误导, 实际 runtime 接收 string. JS == 兼容掩盖了它.
 * 本测对 ID/数值 Long 字段用 stringOrNumber 容错断言.
 *
 * 设计哲学:
 *   - backend 不可达 → skip 不 fail (本地开发与 CI 都友好)
 *   - 不依赖具体 seed 数据条数, 只校验 envelope + 字段集
 *   - 用 admin/admin123, 与 frontend e2e 一致
 */

/** 守护 Long-as-string 全局策略下的契约: 数字 ID 应是 string 或 number */
function expectNumericId(v: unknown, label: string) {
  if (typeof v !== 'string' && typeof v !== 'number') {
    throw new Error(`${label}: 期望 string|number, 实际 ${typeof v}`)
  }
  // 必须能 parseInt 成功 (Number(string) finite)
  const n = Number(v)
  if (!Number.isFinite(n)) {
    throw new Error(`${label}: 值 ${String(v)} 不能 parse 为有效数字`)
  }
}

const BACKEND = process.env.BACKEND_URL || 'http://localhost:8080/api'

interface ResultEnv<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

interface LoginRespShape {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: {
    userId: number
    username: string
    realName: string
    roles: string[]
    permissions: string[]
  }
  enabledPlugins: string[]
}

let backendUp = false
let token: string | null = null

beforeAll(async () => {
  try {
    const r = await fetch(`${BACKEND}/actuator/health`, {
      // 5s 超时, 避免 CI 卡死
      signal: AbortSignal.timeout(5000),
    })
    backendUp = r.ok
  } catch {
    backendUp = false
  }
})

describe('小程序 ↔ Backend 契约', () => {
  it('POST /auth/login admin/admin123 → 返回 LoginResp shape', async () => {
    if (!backendUp) {
      console.warn(`Backend ${BACKEND} 不可达, skip 契约测试`)
      return
    }
    const r = await fetch(`${BACKEND}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123' }),
    })
    expect(r.status).toBe(200)
    const env = await r.json() as ResultEnv<LoginRespShape>
    expect(env.code).toBe(200)
    expect(env.data).toBeTruthy()
    // 字段集断言 (与 auth.ts LoginResp interface 一一对应)
    expect(typeof env.data.accessToken).toBe('string')
    expect(typeof env.data.refreshToken).toBe('string')
    expect(typeof env.data.tokenType).toBe('string')
    // expiresIn 是 Long → 全局序列化为 string. number|string 都接受.
    expectNumericId(env.data.expiresIn, 'expiresIn')
    expect(env.data.userInfo).toBeTruthy()
    expectNumericId(env.data.userInfo.userId, 'userInfo.userId')
    expect(typeof env.data.userInfo.username).toBe('string')
    expect(Array.isArray(env.data.userInfo.roles)).toBe(true)
    expect(Array.isArray(env.data.userInfo.permissions)).toBe(true)
    expect(Array.isArray(env.data.enabledPlugins)).toBe(true)
    token = env.data.accessToken
  })

  it('GET /inspection/tasks/my-tasks → 返回 InspTask[] (字段集对齐 types.ts)', async () => {
    if (!backendUp || !token) {
      console.warn('skip — backend down 或 token 未获取')
      return
    }
    const r = await fetch(`${BACKEND}/inspection/tasks/my-tasks`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(r.status).toBe(200)
    const env = await r.json() as ResultEnv<any[]>
    expect(env.code).toBe(200)
    expect(Array.isArray(env.data)).toBe(true)

    // 若有任务, 必须字段 (与 types.ts InspTask 对齐): id / projectId / status
    if (env.data.length > 0) {
      const t = env.data[0]
      expectNumericId(t.id, 'task.id')
      expectNumericId(t.projectId, 'task.projectId')
      expect(typeof t.status).toBe('string')
      // status 必须在 TaskStatus 枚举内
      const validStatuses = ['PENDING', 'CLAIMED', 'IN_PROGRESS', 'SUBMITTED',
        'APPROVED', 'REJECTED', 'CANCELLED', 'UNDER_REVIEW']
      expect(validStatuses).toContain(t.status)
    }
  })

  it('GET /inspection/appeals/my → 返回 InspAppeal[] (字段集对齐)', async () => {
    if (!backendUp || !token) {
      console.warn('skip')
      return
    }
    const r = await fetch(`${BACKEND}/inspection/appeals/my`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(r.status).toBe(200)
    const env = await r.json() as ResultEnv<any[]>
    expect(env.code).toBe(200)
    expect(Array.isArray(env.data)).toBe(true)

    if (env.data.length > 0) {
      const a = env.data[0]
      expectNumericId(a.id, 'appeal.id')
      // submissionDetailId 是 types.ts 必填字段 — Long 全局序列化为 string
      expectNumericId(a.submissionDetailId, 'appeal.submissionDetailId')
      expect(typeof a.reason).toBe('string')
      const validStatuses = ['PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'UNDER_REVIEW']
      expect(validStatuses).toContain(a.status)
    }
  })

  it('GET /auth/me 用 token 返回 LoginUserInfo', async () => {
    if (!backendUp || !token) {
      console.warn('skip')
      return
    }
    const r = await fetch(`${BACKEND}/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(r.status).toBe(200)
    const env = await r.json() as ResultEnv<any>
    expect(env.code).toBe(200)
    expectNumericId(env.data.userId, 'me.userId')
    expect(typeof env.data.username).toBe('string')
    expect(Array.isArray(env.data.roles)).toBe(true)
    expect(Array.isArray(env.data.permissions)).toBe(true)
  })
})
