import { test, expect, request as pwRequest } from '@playwright/test'

/**
 * 后端契约级 E2E — 回归用.
 *
 * 覆盖:
 * 1. 未知路由应返回 404 + 标准 Result 包裹 (不是 500 "服务器内部错误")
 *    — 背景: Spring Boot 3.2 未知路由抛 NoResourceFoundException, 而旧 GlobalExceptionHandler
 *      只注册了 NoHandlerFoundException, 导致未知路径被兜底 Exception handler 吞成 500.
 *      修复: GlobalExceptionHandler 新增 NoResourceFoundException handler.
 */

const API_BASE = 'http://localhost:8080/api'

async function loginGetToken(ctx: Awaited<ReturnType<typeof pwRequest.newContext>>) {
  const resp = await ctx.post(`${API_BASE}/auth/login`, {
    data: { username: 'admin', password: 'admin123' },
  })
  expect(resp.status()).toBe(200)
  const body = await resp.json()
  return body.data.accessToken as string
}

test.describe('API contract regression', () => {
  test('unknown route returns 404 (not 500) with standard Result envelope', async () => {
    const ctx = await pwRequest.newContext()
    const token = await loginGetToken(ctx)

    // 注意: 路径必须不匹配任何 @RequestMapping 模板, 否则会被路由到真实 handler
    // 并产生 400 (参数类型错误)/403 (权限) 等业务错误, 而非 NoResourceFoundException.
    const paths = [
      '/my/dashboard/teacher',       // 曾返 500 的真实触发路径
      '/nonexistent/route',
      '/totally-unknown-segment-12345',
    ]
    for (const p of paths) {
      const resp = await ctx.get(`${API_BASE}${p}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      expect(resp.status(), `${p} should be 404`).toBe(404)
      const body = await resp.json()
      expect(body.code, `${p} body.code should be 404`).toBe(404)
      expect(body.message).toContain('不存在')
    }
    await ctx.dispose()
  })

  test('real endpoint still returns 200 (sanity)', async () => {
    const ctx = await pwRequest.newContext()
    const token = await loginGetToken(ctx)
    const resp = await ctx.get(`${API_BASE}/my/dashboard/summary`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(resp.status()).toBe(200)
    await ctx.dispose()
  })
})
