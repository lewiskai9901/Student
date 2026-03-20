import { http } from '@/utils/request'

export interface FileUploadResponse {
  fileId: number
  originalName: string
  storedName: string
  fileSize: number
  fileType: string
  fileUrl: string
  uploadTime: string
}

/**
 * 上传单个文件
 */
export function uploadFile(
  file: File,
  businessType?: string,
  businessId?: number | string
): Promise<FileUploadResponse> {
  const formData = new FormData()
  formData.append('file', file)
  if (businessType) {
    formData.append('businessType', businessType)
  }
  if (businessId) {
    formData.append('businessId', businessId.toString())
  }

  return http.post<FileUploadResponse>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 批量上传文件
 */
export function uploadFiles(
  files: File[],
  businessType?: string,
  businessId?: number | string
): Promise<FileUploadResponse[]> {
  const formData = new FormData()
  files.forEach((file) => {
    formData.append('files', file)
  })
  if (businessType) {
    formData.append('businessType', businessType)
  }
  if (businessId) {
    formData.append('businessId', businessId.toString())
  }

  return http.post<FileUploadResponse[]>('/files/upload/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 删除文件
 */
export function deleteFile(fileId: number | string): Promise<void> {
  return http.delete(`/files/${fileId}`)
}

/**
 * 获取文件信息
 */
export function getFileInfo(fileId: number | string): Promise<FileUploadResponse> {
  return http.get<FileUploadResponse>(`/files/${fileId}`)
}

/**
 * 根据业务查询文件列表
 */
export function getFilesByBusiness(
  businessType: string,
  businessId: number | string
): Promise<FileUploadResponse[]> {
  return http.get<FileUploadResponse[]>('/files/business', {
    params: { businessType, businessId }
  })
}

/**
 * 下载文件
 */
export function downloadFile(fileId: number | string): string {
  return `/api/files/download/${fileId}`
}
