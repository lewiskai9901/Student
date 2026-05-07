import { describe, it, expect, vi, beforeEach } from 'vitest'

const requestWrappedMock = vi.fn()
vi.mock('./request', () => ({
  requestWrapped: (opts: any) => requestWrappedMock(opts)
}))

import { orgApi } from './org'

beforeEach(() => requestWrappedMock.mockReset())

describe('orgApi', () => {
  it('tree() GETs /org-units/tree', async () => {
    requestWrappedMock.mockResolvedValueOnce([])
    await orgApi.tree()
    expect(requestWrappedMock).toHaveBeenCalledWith({ url: '/org-units/tree' })
  })

  it('tree() returns nested structure with string ids', async () => {
    const sample = [{
      id: '100', parentId: null, unitCode: 'HQ', unitName: '总部',
      unitType: 'COMPANY', category: 'ROOT', status: 'ACTIVE',
      children: [{
        id: '101', parentId: '100', unitCode: 'DEPT-A', unitName: '研发部',
        unitType: 'DEPARTMENT', category: 'BRANCH', status: 'ACTIVE'
      }]
    }]
    requestWrappedMock.mockResolvedValueOnce(sample)
    const r = await orgApi.tree()
    expect(r[0].id).toBe('100')
    expect(typeof r[0].id).toBe('string')
    expect(r[0].children?.[0].parentId).toBe('100')
  })
})
