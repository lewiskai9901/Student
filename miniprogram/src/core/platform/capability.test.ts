import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createWeixinCapability } from './weixin'

const uniMock = {
  scanCode: vi.fn(),
  getLocation: vi.fn(),
  chooseImage: vi.fn(),
  uploadFile: vi.fn(),
  getStorageSync: vi.fn(),
  setStorageSync: vi.fn(),
  removeStorageSync: vi.fn(),
  requestSubscribeMessage: vi.fn(),
  getSystemInfoSync: vi.fn(() => ({ platform: 'devtools', SDKVersion: '3.0.0' }))
}
;(globalThis as any).uni = uniMock

describe('Weixin PlatformCapability', () => {
  beforeEach(() => Object.values(uniMock).forEach(fn => 'mockReset' in fn && fn.mockReset()))

  it('scan() resolves with raw code', async () => {
    uniMock.scanCode.mockImplementation((opts: any) => opts.success({ result: 'ABC', scanType: 'qr' }))
    const cap = createWeixinCapability()
    const r = await cap.scan()
    expect(r.code).toBe('ABC')
    expect(r.rawType).toBe('qr')
  })

  it('storage adapter wraps uni.getStorageSync', () => {
    uniMock.getStorageSync.mockReturnValue('val')
    const cap = createWeixinCapability()
    expect(cap.storage.get('k')).toBe('val')
    expect(uniMock.getStorageSync).toHaveBeenCalledWith('k')
  })

  it('platform identifier is mp-weixin', () => {
    const cap = createWeixinCapability()
    expect(cap.platform).toBe('mp-weixin')
  })
})
