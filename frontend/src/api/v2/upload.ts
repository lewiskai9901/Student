import { http } from '@/utils/request'

/**
 * 上传单个图片
 */
export function uploadImage(file: File): Promise<{ url: string; name: string }> {
  const formData = new FormData()
  formData.append('file', file)

  return http.post<{ url: string; name: string }>('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 批量上传图片
 */
export function uploadImages(files: File[]): Promise<Array<{ url: string; name: string }>> {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })

  return http.post<Array<{ url: string; name: string }>>('/upload/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 获取上传URL (用于el-upload组件)
 */
export function getUploadUrl(): string {
  // 根据环境返回不同的URL
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${baseURL}/upload/image`
}

/**
 * 上传字体文件
 */
export function uploadFont(file: File): Promise<{ url: string; name: string }> {
  const formData = new FormData()
  formData.append('file', file)

  return http.post<{ url: string; name: string }>('/upload/font', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 获取字体上传URL (用于el-upload组件)
 */
export function getFontUploadUrl(): string {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${baseURL}/upload/font`
}
