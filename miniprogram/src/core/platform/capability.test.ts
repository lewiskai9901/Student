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
  getImageInfo: vi.fn(),
  createCanvasContext: vi.fn(),
  canvasToTempFilePath: vi.fn(),
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

  it('scan() forwards opts to uni.scanCode', async () => {
    uniMock.scanCode.mockImplementation((opts: any) => opts.success({ result: 'X', scanType: 'qr' }))
    const cap = createWeixinCapability()
    await cap.scan({ onlyFromCamera: true })
    expect(uniMock.scanCode).toHaveBeenCalledWith(
      expect.objectContaining({ onlyFromCamera: true })
    )
  })

  it('scan() rejects when uni invokes fail callback', async () => {
    uniMock.scanCode.mockImplementation((opts: any) => opts.fail({ errMsg: 'cancelled' }))
    const cap = createWeixinCapability()
    await expect(cap.scan()).rejects.toMatchObject({ errMsg: 'cancelled' })
  })

  it('uploadFile() rejects on non-2xx HTTP status', async () => {
    uniMock.uploadFile.mockImplementation((opts: any) =>
      opts.success({ statusCode: 500, data: 'server error' })
    )
    const cap = createWeixinCapability()
    await expect(cap.uploadFile({ path: '/p', size: 1 }, { url: '/u', name: 'f' }))
      .rejects.toThrow(/HTTP 500/)
  })

  it('watermarkImage rejects when canvasId is empty', async () => {
    const cap = createWeixinCapability()
    await expect(
      cap.watermarkImage({ path: 'x.jpg', size: 1 }, { canvasId: '', text: 't' })
    ).rejects.toThrow(/canvasId/)
  })

  it('watermarkImage rejects when text is empty', async () => {
    const cap = createWeixinCapability()
    await expect(
      cap.watermarkImage({ path: 'x.jpg', size: 1 }, { canvasId: 'c', text: '   ' })
    ).rejects.toThrow(/text/)
  })

  it('watermarkImage happy path: getImageInfo → draw → canvasToTempFilePath', async () => {
    uniMock.getImageInfo = vi.fn((opts: any) => opts.success({ path: 'resolved/x.jpg', width: 800, height: 600 }))
    uniMock.canvasToTempFilePath = vi.fn((opts: any) => opts.success({ tempFilePath: '/tmp/wm-out.jpg' }))
    const drawCalls: any[] = []
    const ctxStub = {
      drawImage: vi.fn(),
      setFontSize: vi.fn(),
      setFillStyle: vi.fn(),
      setShadow: vi.fn(),
      fillText: vi.fn(),
      draw: vi.fn((reserve: boolean, cb: () => void) => { drawCalls.push({reserve}); cb() })
    }
    uniMock.createCanvasContext = vi.fn(() => ctxStub)

    const cap = createWeixinCapability()
    const result = await cap.watermarkImage(
      { path: 'in.jpg', size: 99 },
      { canvasId: 'wm', text: '2026-05-07 12:00 张三' }
    )

    expect(uniMock.getImageInfo).toHaveBeenCalled()
    expect(uniMock.createCanvasContext).toHaveBeenCalledWith('wm')
    expect(ctxStub.drawImage).toHaveBeenCalledWith('resolved/x.jpg', 0, 0, 800, 600)
    expect(ctxStub.fillText).toHaveBeenCalled()
    expect(drawCalls).toEqual([{reserve: false}])
    expect(uniMock.canvasToTempFilePath).toHaveBeenCalledWith(
      expect.objectContaining({ canvasId: 'wm' })
    )
    expect(result).toEqual({ path: '/tmp/wm-out.jpg', size: 99 })
  })

  it('requestSubscribeMessage() strips errMsg from result', async () => {
    uniMock.requestSubscribeMessage.mockImplementation((opts: any) =>
      opts.success({ errMsg: 'requestSubscribeMessage:ok', T1: 'accept', T2: 'reject' })
    )
    const cap = createWeixinCapability()
    const r = await cap.requestSubscribeMessage(['T1', 'T2'])
    expect(r).toEqual({ T1: 'accept', T2: 'reject' })
    expect(r).not.toHaveProperty('errMsg')
  })
})
