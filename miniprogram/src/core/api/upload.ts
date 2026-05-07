import { capability } from '@core/platform/auto'
import { BizError, type ResultEnvelope } from './request'
import type { LocalFile } from '@core/platform/capability'

export interface UploadWrappedOpts {
  url: string                            // path relative to BASE_URL
  formData?: Record<string, string>
  fieldName?: string                     // default 'file'
}

const BASE_URL = 'http://localhost:8080/api'

/**
 * 上传文件并解包 Result<T> 信封. 自动注入 Authorization.
 */
export async function uploadWrapped<T>(
  file: LocalFile,
  opts: UploadWrappedOpts
): Promise<T> {
  const token = capability.storage.get<string>('accessToken')
  const header: Record<string, string> = {}
  if (token) header.Authorization = `Bearer ${token}`

  const body = await capability.uploadFile(file, {
    url: BASE_URL + opts.url,
    name: opts.fieldName ?? 'file',
    formData: opts.formData,
    header
  })

  const envelope = body as ResultEnvelope<T>
  if (!envelope || typeof envelope.code !== 'number') {
    throw new BizError(-1, 'malformed upload response envelope')
  }
  if (envelope.code !== 200) {
    throw new BizError(envelope.code, envelope.message ?? 'upload failed')
  }
  return envelope.data
}
