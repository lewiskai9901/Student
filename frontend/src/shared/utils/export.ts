/**
 * 文件导出工具函数
 */

/**
 * 下载blob文件
 * @param blob 文件blob
 * @param filename 文件名
 */
export function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * 根据Content-Disposition header获取文件名
 * @param contentDisposition Content-Disposition header值
 * @returns 文件名
 */
export function getFilenameFromContentDisposition(contentDisposition: string): string {
  if (!contentDisposition) {
    return `导出文件_${Date.now()}.xlsx`
  }

  // 尝试获取filename*=UTF-8''...格式(RFC 5987)
  const filenameRegex = /filename\*=UTF-8''(.+)/
  const matches = filenameRegex.exec(contentDisposition)
  if (matches && matches[1]) {
    return decodeURIComponent(matches[1])
  }

  // 尝试获取filename="..."格式
  const filenameRegex2 = /filename="?(.+)"?/
  const matches2 = filenameRegex2.exec(contentDisposition)
  if (matches2 && matches2[1]) {
    return matches2[1]
  }

  return `导出文件_${Date.now()}.xlsx`
}

/**
 * 导出Excel文件(通用方法)
 * @param apiCall API调用函数,应返回Blob
 * @param defaultFilename 默认文件名
 */
export async function exportExcel(
  apiCall: () => Promise<any>,
  defaultFilename: string = `导出数据_${Date.now()}.xlsx`
): Promise<void> {
  try {
    const response = await apiCall()

    // 如果response是axios的响应对象
    let blob: Blob
    let filename = defaultFilename

    if (response.data instanceof Blob) {
      blob = response.data
      // 尝试从header中获取文件名
      const contentDisposition = response.headers['content-disposition']
      if (contentDisposition) {
        filename = getFilenameFromContentDisposition(contentDisposition)
      }
    } else if (response instanceof Blob) {
      blob = response
    } else {
      throw new Error('响应数据格式错误')
    }

    downloadBlob(blob, filename)
  } catch (error) {
    console.error('导出失败:', error)
    throw error
  }
}
