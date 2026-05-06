import type { PlatformCapability, KVStorage, WatermarkOpts, LocalFile } from './capability'

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
            if (r.statusCode < 200 || r.statusCode >= 300) {
              return reject(new Error(`upload failed: HTTP ${r.statusCode}`))
            }
            try {
              const body = typeof r.data === 'string' ? JSON.parse(r.data) : r.data
              resolve({ url: body.url, key: body.key })
            } catch (e) { reject(e) }
          },
          fail: reject
        })
      })
    },
    watermarkImage(file: LocalFile, opts: WatermarkOpts): Promise<LocalFile> {
      return new Promise((resolve, reject) => {
        if (!opts.canvasId || !opts.canvasId.trim()) {
          return reject(new Error('canvasId required'))
        }
        if (!opts.text || !opts.text.trim()) {
          return reject(new Error('text required'))
        }
        const fontSize = opts.fontSize ?? 24
        const color = opts.color ?? '#fff'
        const shadowColor = opts.shadowColor ?? 'rgba(0,0,0,0.6)'
        const position = opts.position ?? 'bottom-right'
        const padding = 20
        uni.getImageInfo({
          src: file.path,
          success: (info: any) => {
            try {
              const ctx = uni.createCanvasContext(opts.canvasId)
              ctx.drawImage(info.path, 0, 0, info.width, info.height)
              ctx.setFontSize(fontSize)
              ctx.setFillStyle(color)
              ctx.setShadow(2, 2, 4, shadowColor)
              const textWidth = opts.text.length * fontSize * 0.55
              let x = 0, y = 0
              switch (position) {
                case 'bottom-left':
                  x = padding; y = info.height - padding; break
                case 'top-right':
                  x = info.width - textWidth - padding; y = padding + fontSize; break
                case 'top-left':
                  x = padding; y = padding + fontSize; break
                case 'bottom-right':
                default:
                  x = info.width - textWidth - padding; y = info.height - padding; break
              }
              ctx.fillText(opts.text, x, y)
              ctx.draw(false, () => {
                uni.canvasToTempFilePath({
                  canvasId: opts.canvasId,
                  success: (r: any) => resolve({ path: r.tempFilePath, size: file.size }),
                  fail: reject
                })
              })
            } catch (e) {
              reject(e)
            }
          },
          fail: reject
        })
      })
    },
    requestSubscribeMessage(templateIds) {
      return new Promise((resolve, reject) => {
        uni.requestSubscribeMessage({
          tmplIds: templateIds,
          success: (r: any) => {
            const { errMsg: _errMsg, ...result } = r
            resolve(result)
          },
          fail: reject
        })
      })
    }
  }
}
