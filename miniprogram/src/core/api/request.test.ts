import { describe, it, expect, vi, beforeEach } from 'vitest'
import { requestWrapped, BizError } from './request'

const mockUni = {
  request: vi.fn(),
  reLaunch: vi.fn(),
  getStorageSync: vi.fn(() => undefined),
  setStorageSync: vi.fn(),
  removeStorageSync: vi.fn(),
  getSystemInfoSync: vi.fn(() => ({ platform: 'devtools', SDKVersion: '3.0.0' }))
}
;(globalThis as any).uni = mockUni

describe('requestWrapped (Result envelope)', () => {
  beforeEach(() => Object.values(mockUni).forEach(fn => 'mockReset' in fn && fn.mockReset()))

  it('unwraps Result.data on code=200', async () => {
    mockUni.request.mockImplementation((o: any) =>
      o.success({ statusCode: 200, data: { code: 200, message: 'ok', data: { id: '1', name: 'Demo' }, timestamp: 1 } })
    )
    const r = await requestWrapped<{ id: number; name: string }>({ url: '/x' })
    expect(r).toEqual({ id: '1', name: 'Demo' })
  })

  it('throws BizError on non-200 result code', async () => {
    mockUni.request.mockImplementation((o: any) =>
      o.success({ statusCode: 200, data: { code: 5001, message: '业务错', data: null, timestamp: 1 } })
    )
    await expect(requestWrapped({ url: '/x' })).rejects.toBeInstanceOf(BizError)
  })

  it('BizError carries code and bizMessage', async () => {
    mockUni.request.mockImplementation((o: any) =>
      o.success({ statusCode: 200, data: { code: 5001, message: 'biz fail', data: null, timestamp: 1 } })
    )
    try {
      await requestWrapped({ url: '/x' })
      throw new Error('should have thrown')
    } catch (e) {
      expect(e).toBeInstanceOf(BizError)
      expect((e as BizError).code).toBe(5001)
      expect((e as BizError).bizMessage).toBe('biz fail')
    }
  })

  it('throws BizError with -1/unknown when envelope is malformed', async () => {
    mockUni.request.mockImplementation((o: any) =>
      o.success({ statusCode: 200, data: null })
    )
    await expect(requestWrapped({ url: '/x' })).rejects.toBeInstanceOf(BizError)
  })
})
