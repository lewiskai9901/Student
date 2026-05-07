import { describe, it, expect, vi, beforeEach } from 'vitest'
import { BizError } from './request'

const uploadFileMock = vi.fn()
const storageGetMock = vi.fn()

vi.mock('@core/platform/auto', () => ({
  capability: {
    uploadFile: (file: any, opts: any) => uploadFileMock(file, opts),
    storage: { get: (k: string) => storageGetMock(k) }
  }
}))

import { uploadWrapped } from './upload'

beforeEach(() => {
  uploadFileMock.mockReset()
  storageGetMock.mockReset()
})

describe('uploadWrapped', () => {
  it('injects Bearer token and unwraps envelope on success', async () => {
    storageGetMock.mockReturnValue('TOK123')
    uploadFileMock.mockResolvedValueOnce({
      code: 200, message: 'OK',
      data: { fileName: 'a.jpg', fileUrl: 'https://cdn/a.jpg', size: 99 },
      timestamp: 1
    })
    const result = await uploadWrapped<{ fileName: string; fileUrl: string; size: number }>(
      { path: '/p', size: 99 },
      { url: '/files/upload', formData: { directory: 'inspection' } }
    )
    expect(result).toEqual({ fileName: 'a.jpg', fileUrl: 'https://cdn/a.jpg', size: 99 })

    const [, opts] = uploadFileMock.mock.calls[0]
    expect(opts.url).toBe('http://localhost:8080/api/files/upload')
    expect(opts.name).toBe('file')
    expect(opts.formData).toEqual({ directory: 'inspection' })
    expect(opts.header.Authorization).toBe('Bearer TOK123')
  })

  it('omits Authorization when no token', async () => {
    storageGetMock.mockReturnValue(undefined)
    uploadFileMock.mockResolvedValueOnce({
      code: 200, message: 'OK', data: {}, timestamp: 1
    })
    await uploadWrapped({ path: '/p', size: 1 }, { url: '/x' })
    const [, opts] = uploadFileMock.mock.calls[0]
    expect(opts.header).not.toHaveProperty('Authorization')
  })

  it('throws BizError on non-200 envelope', async () => {
    storageGetMock.mockReturnValue('T')
    uploadFileMock.mockResolvedValueOnce({
      code: 4001, message: '文件太大', data: null, timestamp: 1
    })
    await expect(
      uploadWrapped({ path: '/p', size: 1 }, { url: '/x' })
    ).rejects.toThrow(BizError)
  })

  it('throws BizError on malformed envelope', async () => {
    storageGetMock.mockReturnValue('T')
    uploadFileMock.mockResolvedValueOnce('not an envelope')
    await expect(
      uploadWrapped({ path: '/p', size: 1 }, { url: '/x' })
    ).rejects.toThrow(/malformed/)
  })

  it('uses custom fieldName when provided', async () => {
    storageGetMock.mockReturnValue('T')
    uploadFileMock.mockResolvedValueOnce({ code: 200, message: 'OK', data: {}, timestamp: 1 })
    await uploadWrapped({ path: '/p', size: 1 }, { url: '/x', fieldName: 'attachment' })
    const [, opts] = uploadFileMock.mock.calls[0]
    expect(opts.name).toBe('attachment')
  })
})
