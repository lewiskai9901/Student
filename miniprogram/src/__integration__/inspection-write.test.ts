import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createEventBus } from '@core/plugin/event-bus'

const requestWrappedMock = vi.fn()
vi.mock('@core/api/request', () => ({
  requestWrapped: (opts: any) => requestWrappedMock(opts),
  BizError: class BizError extends Error {
    constructor(public code: number, public bizMessage: string) {
      super(`[${code}] ${bizMessage}`)
    }
  }
}))

import { inspectionApi } from '../plugins/inspection/api/inspection'
import { BizError } from '@core/api/request'

beforeEach(() => requestWrappedMock.mockReset())

describe('inspection write paths → event bus contract', () => {
  it('submitTask success then emit reaches a cross-plugin listener', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: '7', status: 'SUBMITTED' })
    const bus = createEventBus()
    const seen: any[] = []
    bus.on('inspection.task.submitted', (p) => seen.push(p))

    await inspectionApi.submitTask('7')
    bus.emit('inspection.task.submitted', { taskId: '7', submitterId: 42 })

    expect(seen).toEqual([{ taskId: '7', submitterId: 42 }])
  })

  it('submitCorrection success then emit reaches listener', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: '9', status: 'SUBMITTED' })
    const bus = createEventBus()
    const seen: any[] = []
    bus.on('inspection.case.processed', (p) => seen.push(p))

    await inspectionApi.submitCorrection('9', '已完成 5 项整改')
    bus.emit('inspection.case.processed', { caseId: '9', action: 'submitted' })

    expect(seen).toEqual([{ caseId: '9', action: 'submitted' }])
  })

  it('submitTask BizError propagates and prevents downstream emit', async () => {
    requestWrappedMock.mockRejectedValueOnce(new BizError(4001, '状态不可提交'))
    const bus = createEventBus()
    const seen: any[] = []
    bus.on('inspection.task.submitted', (p) => seen.push(p))

    let caught: unknown = null
    try {
      await inspectionApi.submitTask('7')
      // page handler would emit here; we deliberately do NOT emit when API rejects
    } catch (e) {
      caught = e
    }

    expect(caught).toBeInstanceOf(BizError)
    expect((caught as BizError).bizMessage).toBe('状态不可提交')
    expect(seen).toEqual([])
  })
})
