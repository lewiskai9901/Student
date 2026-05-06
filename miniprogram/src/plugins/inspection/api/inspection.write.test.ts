import { describe, it, expect, vi, beforeEach } from 'vitest'

const requestWrappedMock = vi.fn()
vi.mock('@core/api/request', () => ({
  requestWrapped: (opts: any) => requestWrappedMock(opts)
}))

import { inspectionApi } from './inspection'

beforeEach(() => requestWrappedMock.mockReset())

describe('inspectionApi write paths', () => {
  it('claimTask POSTs body with inspectorName', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 1, status: 'CLAIMED' })
    const r = await inspectionApi.claimTask(1, '张三')
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/tasks/1/claim',
      method: 'POST',
      data: { inspectorName: '张三' }
    })
    expect(r.status).toBe('CLAIMED')
  })

  it('startTask POSTs without body', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 1, status: 'IN_PROGRESS' })
    await inspectionApi.startTask(1)
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/tasks/1/start',
      method: 'POST'
    })
  })

  it('submitTask POSTs without body', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 1, status: 'SUBMITTED' })
    await inspectionApi.submitTask(1)
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/tasks/1/submit',
      method: 'POST'
    })
  })

  it('startCaseWork POSTs without body', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 9, status: 'IN_PROGRESS' })
    await inspectionApi.startCaseWork(9)
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/corrective-cases/9/start-work',
      method: 'POST'
    })
  })

  it('submitCorrection POSTs note + empty evidenceIds', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 9, status: 'SUBMITTED' })
    const r = await inspectionApi.submitCorrection(9, '已修复')
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/corrective-cases/9/submit-correction',
      method: 'POST',
      data: { correctionNote: '已修复', evidenceIds: [] }
    })
    expect(r.status).toBe('SUBMITTED')
  })
})
