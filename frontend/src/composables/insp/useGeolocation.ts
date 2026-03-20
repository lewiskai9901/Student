/**
 * V7 检查平台 - GPS 定位 Composable
 * 使用 Geolocation API
 */
import { ref, readonly } from 'vue'

export interface GeoPosition {
  latitude: number
  longitude: number
  accuracy: number
  timestamp: number
}

export function useGeolocation() {
  const position = ref<GeoPosition | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  function getCurrentPosition(): Promise<GeoPosition | null> {
    if (!navigator.geolocation) {
      error.value = '浏览器不支持地理定位'
      return Promise.resolve(null)
    }

    loading.value = true
    error.value = null

    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const geo: GeoPosition = {
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
            accuracy: pos.coords.accuracy,
            timestamp: pos.timestamp,
          }
          position.value = geo
          loading.value = false
          resolve(geo)
        },
        (err) => {
          loading.value = false
          switch (err.code) {
            case err.PERMISSION_DENIED:
              error.value = '定位权限被拒绝'
              break
            case err.POSITION_UNAVAILABLE:
              error.value = '位置信息不可用'
              break
            case err.TIMEOUT:
              error.value = '定位超时'
              break
            default:
              error.value = '定位失败'
          }
          resolve(null)
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 60000,
        }
      )
    })
  }

  return {
    position: readonly(position),
    loading: readonly(loading),
    error: readonly(error),
    getCurrentPosition,
  }
}
