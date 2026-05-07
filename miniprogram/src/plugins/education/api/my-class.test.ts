import { describe, it, expect, vi, beforeEach } from 'vitest'
import { myClassApi } from './my-class'
import { BizError } from '@core/api/request'

const mockUni = {
  request: vi.fn(),
  reLaunch: vi.fn(),
  getStorageSync: vi.fn(() => undefined),
  setStorageSync: vi.fn(),
  removeStorageSync: vi.fn(),
  getSystemInfoSync: vi.fn(() => ({ platform: 'devtools', SDKVersion: '3.0.0' }))
}
;(globalThis as any).uni = mockUni

function mockOk(data: unknown) {
  mockUni.request.mockImplementation((o: any) =>
    o.success({ statusCode: 200, data: { code: 200, message: 'ok', data, timestamp: 1 } })
  )
}

function mockBizError(code: number, message: string) {
  mockUni.request.mockImplementation((o: any) =>
    o.success({ statusCode: 200, data: { code, message, data: null, timestamp: 1 } })
  )
}

describe('myClassApi', () => {
  beforeEach(() => {
    Object.values(mockUni).forEach(fn => 'mockReset' in fn && fn.mockReset())
  })

  it('list() GETs /my-class/classes', async () => {
    mockOk([{ id: 1, classCode: 'C001', className: '高一(1)班' }])
    const result = await myClassApi.list()
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/my-class/classes') })
    )
    expect(result).toHaveLength(1)
    expect(result[0].id).toBe(1)
  })

  it('overview(7) GETs /my-class/classes/7/overview', async () => {
    mockOk({ orgUnitId: 7, className: '高一(1)班', studentCount: 42 })
    const ov = await myClassApi.overview(7)
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/my-class/classes/7/overview') })
    )
    expect(ov.studentCount).toBe(42)
  })

  it('students(7, { keyword: "Z" }) GETs /my-class/classes/7/user_student?keyword=Z', async () => {
    mockOk([])
    await myClassApi.students(7, { keyword: 'Z' })
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({
        url: expect.stringMatching(/\/my-class\/classes\/7\/user_student\?keyword=Z/)
      })
    )
  })

  it('propagates BizError from non-200 envelope', async () => {
    mockBizError(5001, '业务异常')
    await expect(myClassApi.list()).rejects.toBeInstanceOf(BizError)
  })
})
