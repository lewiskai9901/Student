/**
 * V7 检查平台 - 相机拍照 Composable
 * 使用 MediaDevices API，降级到 file input
 */
import { ref } from 'vue'

export function useCamera() {
  const isCapturing = ref(false)
  const error = ref<string | null>(null)

  /**
   * 拍照或选择图片
   * 优先使用 capture="environment" 的 file input（移动端直接调起相机）
   */
  function capturePhoto(): Promise<File | null> {
    return new Promise((resolve) => {
      error.value = null
      isCapturing.value = true

      const input = document.createElement('input')
      input.type = 'file'
      input.accept = 'image/*'
      input.capture = 'environment' // 后置摄像头

      input.onchange = () => {
        isCapturing.value = false
        const file = input.files?.[0] ?? null
        resolve(file)
      }

      input.oncancel = () => {
        isCapturing.value = false
        resolve(null)
      }

      // Fallback: if cancel event not supported
      const handleBlur = () => {
        setTimeout(() => {
          if (isCapturing.value) {
            isCapturing.value = false
            resolve(null)
          }
        }, 500)
      }
      window.addEventListener('focus', handleBlur, { once: true })

      input.click()
    })
  }

  /**
   * 选择视频文件
   */
  function captureVideo(): Promise<File | null> {
    return new Promise((resolve) => {
      error.value = null
      isCapturing.value = true

      const input = document.createElement('input')
      input.type = 'file'
      input.accept = 'video/*'
      input.capture = 'environment'

      input.onchange = () => {
        isCapturing.value = false
        resolve(input.files?.[0] ?? null)
      }

      input.oncancel = () => {
        isCapturing.value = false
        resolve(null)
      }

      input.click()
    })
  }

  return {
    isCapturing,
    error,
    capturePhoto,
    captureVideo,
  }
}
