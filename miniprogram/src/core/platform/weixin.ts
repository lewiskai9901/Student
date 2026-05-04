import type { PlatformCapability, KVStorage } from './capability'

declare const uni: any

const storage: KVStorage = {
  get: (k) => { try { const v = uni.getStorageSync(k); return v === '' ? undefined : v } catch { return undefined } },
  set: (k, v) => uni.setStorageSync(k, v),
  remove: (k) => uni.removeStorageSync(k)
}

export function createWeixinCapability(): PlatformCapability {
  return {
    platform: 'mp-weixin',
    storage,
    systemInfo() {
      const info = uni.getSystemInfoSync()
      return { platform: info.platform, sdkVersion: info.SDKVersion }
    },
    scan(opts) {
      return new Promise((resolve, reject) => {
        uni.scanCode({
          ...opts,
          success: (r: any) => resolve({ code: r.result, rawType: r.scanType }),
          fail: reject
        })
      })
    },
    getLocation(opts) {
      return new Promise((resolve, reject) => {
        uni.getLocation({ type: opts?.type ?? 'gcj02', success: resolve, fail: reject })
      })
    },
    takePhoto(opts) {
      return new Promise((resolve, reject) => {
        uni.chooseImage({
          count: opts.count ?? 1,
          sourceType: opts.sourceType ?? ['camera'],
          sizeType: opts.sizeType ?? ['compressed'],
          success: (r: any) => resolve(r.tempFiles.map((f: any) => ({ path: f.path, size: f.size }))),
          fail: reject
        })
      })
    },
    uploadFile(file, opts) {
      return new Promise((resolve, reject) => {
        uni.uploadFile({
          url: opts.url,
          filePath: file.path,
          name: opts.name,
          formData: opts.formData,
          header: opts.header,
          success: (r: any) => {
            try {
              const body = typeof r.data === 'string' ? JSON.parse(r.data) : r.data
              resolve({ url: body.url, key: body.key })
            } catch (e) { reject(e) }
          },
          fail: reject
        })
      })
    },
    requestSubscribeMessage(templateIds) {
      return new Promise((resolve, reject) => {
        uni.requestSubscribeMessage({ tmplIds: templateIds, success: resolve, fail: reject })
      })
    }
  }
}
