import { describe, it, expect, vi, beforeEach } from 'vitest'

const requestWrappedMock = vi.fn()
vi.mock('./request', () => ({
  requestWrapped: (opts: any) => requestWrappedMock(opts)
}))

import { placeApi } from './place'

beforeEach(() => requestWrappedMock.mockReset())

describe('placeApi', () => {
  it('tree() defaults to maxDepth=0 (unlimited)', async () => {
    requestWrappedMock.mockResolvedValueOnce([])
    await placeApi.tree()
    expect(requestWrappedMock).toHaveBeenCalledWith({ url: '/v9/places/tree?maxDepth=0' })
  })

  it('tree() forwards maxDepth param', async () => {
    requestWrappedMock.mockResolvedValueOnce([])
    await placeApi.tree(3)
    expect(requestWrappedMock).toHaveBeenCalledWith({ url: '/v9/places/tree?maxDepth=3' })
  })

  it('tree() returns nested place nodes with capacity', async () => {
    const sample = [{
      id: 1, placeCode: 'BLD-A', placeName: 'A 楼', typeCode: 'BUILDING',
      hasCapacity: false, leaf: false,
      children: [{
        id: 2, parentId: 1, placeCode: 'A-101', placeName: '101 室',
        typeCode: 'ROOM', hasCapacity: true, capacity: 30, currentOccupancy: 18, leaf: true
      }]
    }]
    requestWrappedMock.mockResolvedValueOnce(sample)
    const r = await placeApi.tree()
    expect(r[0].children?.[0].capacity).toBe(30)
    expect(r[0].children?.[0].currentOccupancy).toBe(18)
  })
})
