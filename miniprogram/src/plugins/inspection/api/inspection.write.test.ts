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

  it('addEvidence POSTs body to submissions evidences endpoint', async () => {
    requestWrappedMock.mockResolvedValueOnce({
      id: 99, submissionId: 7, evidenceType: 'PHOTO',
      fileName: 'a.jpg', fileUrl: 'https://cdn/a.jpg'
    })
    const r = await inspectionApi.addEvidence(7, {
      detailId: 21, evidenceType: 'PHOTO', fileName: 'a.jpg', fileUrl: 'https://cdn/a.jpg'
    })
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/submissions/7/evidences',
      method: 'POST',
      data: { detailId: 21, evidenceType: 'PHOTO', fileName: 'a.jpg', fileUrl: 'https://cdn/a.jpg' }
    })
    expect(r.id).toBe(99)
  })

  it('submitCorrection passes evidenceIds when provided', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 9, status: 'SUBMITTED' })
    await inspectionApi.submitCorrection(9, '已完成', [99, 100])
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/corrective-cases/9/submit-correction',
      method: 'POST',
      data: { correctionNote: '已完成', evidenceIds: [99, 100] }
    })
  })

  it('submissionsByTask GETs with taskId query', async () => {
    requestWrappedMock.mockResolvedValueOnce([{ id: 5, taskId: 1, status: 'IN_PROGRESS' }])
    const r = await inspectionApi.submissionsByTask(1)
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/submissions?taskId=1'
    })
    expect(r[0].id).toBe(5)
  })

  it('submissionDetails GETs by submissionId', async () => {
    requestWrappedMock.mockResolvedValueOnce([])
    await inspectionApi.submissionDetails(5)
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/submissions/5/details'
    })
  })

  it('updateDetailResponse PUTs body to detail response endpoint', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 21, submissionId: 5, templateItemId: 7, itemCode: 'I1', itemName: '项 1' })
    await inspectionApi.updateDetailResponse(21, {
      responseValue: 'PASS', scoringMode: 'PASS_FAIL', score: 100
    })
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/submissions/details/21/response',
      method: 'PUT',
      data: { responseValue: 'PASS', scoringMode: 'PASS_FAIL', score: 100 }
    })
  })

  it('completeSubmission POSTs without body', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 5, taskId: 1, status: 'COMPLETED' })
    const r = await inspectionApi.completeSubmission(5)
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/submissions/5/complete',
      method: 'POST'
    })
    expect(r.status).toBe('COMPLETED')
  })
})
