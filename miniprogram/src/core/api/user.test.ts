import { describe, it, expect, vi, beforeEach } from 'vitest'

const requestWrappedMock = vi.fn()
vi.mock('./request', () => ({
  requestWrapped: (opts: any) => requestWrappedMock(opts)
}))

import { userApi } from './user'

beforeEach(() => requestWrappedMock.mockReset())

describe('userApi', () => {
  it('byOrgUnit GETs /users/by-org-unit/{id}', async () => {
    requestWrappedMock.mockResolvedValueOnce([])
    await userApi.byOrgUnit(100)
    expect(requestWrappedMock).toHaveBeenCalledWith({ url: '/users/by-org-unit/100' })
  })

  it('byOrgUnit returns user list with numeric ids', async () => {
    const sample = [
      { id: 7, username: 'alice', realName: '艾丽斯', userType: 'STAFF', orgUnitId: 100, status: '启用', roleNames: ['MANAGER'] }
    ]
    requestWrappedMock.mockResolvedValueOnce(sample)
    const r = await userApi.byOrgUnit(100)
    expect(r[0].id).toBe(7)
    expect(r[0].roleNames).toEqual(['MANAGER'])
  })
})
