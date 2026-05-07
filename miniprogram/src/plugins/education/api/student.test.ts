import { describe, it, expect, vi, beforeEach } from 'vitest'
import { studentApi } from './student'
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

describe('studentApi', () => {
  beforeEach(() => {
    Object.values(mockUni).forEach(fn => 'mockReset' in fn && fn.mockReset())
  })

  it('list() GETs /user_student with paging query string', async () => {
    mockOk({ records: [{ id: 1, studentNo: 'S001', name: 'Alice' }], total: 1, size: 20, current: 1 })
    const result = await studentApi.list({ keyword: 'al', pageNum: 1, pageSize: 20 })
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({
        url: expect.stringMatching(/\/user_student\?.*keyword=al.*pageNum=1.*pageSize=20/)
      })
    )
    expect(result.records).toHaveLength(1)
    expect(result.total).toBe(1)
  })

  it('byId(id) GETs /user_student/{id}', async () => {
    mockOk({ id: 42, studentNo: 'S042', name: 'Bob' })
    const s = await studentApi.byId(42)
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/user_student/42') })
    )
    expect(s.id).toBe(42)
  })

  it('byClass(classId) GETs /user_student/by-class/{classId}', async () => {
    mockOk([])
    await studentApi.byClass(7)
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/user_student/by-class/7') })
    )
  })

  it('propagates BizError from non-200 envelope', async () => {
    mockBizError(5001, '业务异常')
    await expect(studentApi.list()).rejects.toBeInstanceOf(BizError)
  })
})
