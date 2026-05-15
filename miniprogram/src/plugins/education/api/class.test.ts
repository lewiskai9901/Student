import { describe, it, expect, vi, beforeEach } from 'vitest'
import { classApi } from './class'
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

describe('classApi', () => {
  beforeEach(() => {
    Object.values(mockUni).forEach(fn => 'mockReset' in fn && fn.mockReset())
  })

  it('list() GETs /user_student/classes with paging query string', async () => {
    mockOk({
      records: [{ id: '1', classCode: 'C01', className: '班1' }],
      total: 1,
      size: 20,
      current: 1
    })
    const result = await classApi.list({ pageNum: 1, pageSize: 20 })
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({
        url: expect.stringMatching(/\/user_student\/classes\?.*pageNum=1.*pageSize=20/)
      })
    )
    expect(result.records).toHaveLength(1)
  })

  it('byId(id) GETs /user_student/classes/{id}', async () => {
    mockOk({ id: '99', classCode: 'C99', className: '班99' })
    const c = await classApi.byId('99')
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/user_student/classes/99') })
    )
    expect(c.id).toBe('99')
  })

  it('propagates BizError from non-200 envelope', async () => {
    mockBizError(5001, '业务异常')
    await expect(classApi.list()).rejects.toBeInstanceOf(BizError)
  })
})
