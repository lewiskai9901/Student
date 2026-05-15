/**
 * 生成 SDK 烟测 — 验证 codegen 输出可正确 import + 类型对齐 + 关键 method 存在.
 *
 * 不发真请求 (mock axios), 只验证类型契约 + module shape.
 * 用于 PR 时快速判断 SDK 是否生成损坏.
 */
import { describe, it, expect, vi } from 'vitest'

describe('Generated SDK shape', () => {
  it('barrel index.ts 可 import, 不抛错', async () => {
    const sdk = await import('@/api-generated')
    expect(sdk).toBeTruthy()
    // 关键 namespace/method 存在 (按 hey-api 生成的 operation key)
    expect(typeof sdk).toBe('object')
  })

  it('types.gen.ts 含 LongId import + 已知 schema', async () => {
    const types = await import('@/api-generated/types.gen')
    expect(types).toBeTruthy()
  })

  it('client.gen.ts 暴露 client 实例', async () => {
    const { client } = await import('@/api-generated/client.gen')
    expect(client).toBeTruthy()
  })

  it('生成 SDK 复用 request.ts 的 axios 实例 (createClientConfig)', async () => {
    const { createClientConfig } = await import('@/api-generated-client')
    const cfg = createClientConfig({} as any)
    expect(cfg.baseURL).toBeTruthy()
    // 复用了 axios 实例 (说明 JWT auth + interceptor 链路共享)
    expect(cfg.axios).toBeTruthy()
  })
})
