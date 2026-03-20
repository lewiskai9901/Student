/**
 * V7 检查平台 - NFC 读取 composable
 * 使用 Web NFC API 读取 NFC 标签 (仅 Android Chrome 支持)
 */
import { ref, onUnmounted } from 'vue'

export function useNfcReader() {
  const isSupported = ref(false)
  const isScanning = ref(false)
  const lastTagUid = ref<string | null>(null)
  const lastReadAt = ref<string | null>(null)
  const error = ref<string | null>(null)

  let abortController: AbortController | null = null

  // Check support
  isSupported.value = 'NDEFReader' in window

  async function startScan() {
    if (!isSupported.value) {
      error.value = '当前浏览器/设备不支持 NFC'
      return
    }

    error.value = null
    abortController = new AbortController()

    try {
      const ndef = new (window as any).NDEFReader()
      await ndef.scan({ signal: abortController.signal })
      isScanning.value = true

      ndef.addEventListener('reading', (event: any) => {
        lastTagUid.value = event.serialNumber
        lastReadAt.value = new Date().toISOString()
      })

      ndef.addEventListener('readingerror', () => {
        error.value = 'NFC 标签读取失败'
      })
    } catch (err: any) {
      error.value = err.message || 'NFC 扫描启动失败'
      isScanning.value = false
    }
  }

  function stopScan() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    isScanning.value = false
  }

  function reset() {
    stopScan()
    lastTagUid.value = null
    lastReadAt.value = null
    error.value = null
  }

  onUnmounted(() => {
    stopScan()
  })

  return {
    isSupported,
    isScanning,
    lastTagUid,
    lastReadAt,
    error,
    startScan,
    stopScan,
    reset,
  }
}
