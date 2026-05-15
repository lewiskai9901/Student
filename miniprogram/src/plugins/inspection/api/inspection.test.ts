import { describe, it, expect, vi, beforeEach } from 'vitest'
import { inspectionApi } from './inspection'
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

describe('inspectionApi', () => {
  beforeEach(() => {
    Object.values(mockUni).forEach(fn => 'mockReset' in fn && fn.mockReset())
  })

  it('myTasks() GETs /inspection/tasks/my-tasks', async () => {
    mockOk([{ id: '1', projectId: '9', templateId: '1', status: 'PENDING' }])
    const tasks = await inspectionApi.myTasks()
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/inspection/tasks/my-tasks') })
    )
    expect(tasks).toHaveLength(1)
    expect(tasks[0].id).toBe('1')
  })

  it('taskById(id) GETs /inspection/tasks/{id}', async () => {
    mockOk({ id: '42', projectId: '9', templateId: '1', status: 'IN_PROGRESS' })
    const t = await inspectionApi.taskById('42')
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/inspection/tasks/42') })
    )
    expect(t.id).toBe('42')
  })

  it('availableTasks() GETs /inspection/tasks/available', async () => {
    mockOk([])
    await inspectionApi.availableTasks()
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/inspection/tasks/available') })
    )
  })

  it('myCases() GETs /inspection/corrective-cases/my-cases', async () => {
    mockOk([])
    await inspectionApi.myCases()
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/inspection/corrective-cases/my-cases') })
    )
  })

  it('caseById(id) GETs /inspection/corrective-cases/{id}', async () => {
    mockOk({ id: '7', projectId: '9', status: 'IN_PROGRESS' })
    const c = await inspectionApi.caseById('7')
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/inspection/corrective-cases/7') })
    )
    expect(c.id).toBe('7')
  })

  it('myAppeals() GETs /inspection/appeals/my', async () => {
    mockOk([])
    await inspectionApi.myAppeals()
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/inspection/appeals/my') })
    )
  })

  it('appealById(id) GETs /inspection/appeals/{id}', async () => {
    mockOk({ id: '5', submissionDetailId: '100', submitterId: 1, reason: 'r', status: 'PENDING' })
    const a = await inspectionApi.appealById('5')
    expect(mockUni.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining('/inspection/appeals/5') })
    )
    expect(a.id).toBe('5')
  })

  it('propagates BizError from non-200 envelope', async () => {
    mockBizError(5001, '业务异常')
    await expect(inspectionApi.myTasks()).rejects.toBeInstanceOf(BizError)
  })
})
