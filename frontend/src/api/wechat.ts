import { http } from '@/utils/request'

/**
 * JS-SDK配置
 */
export interface JsConfig {
  appId?: string
  timestamp?: number
  nonceStr?: string
  signature?: string
  error?: string
}

/**
 * 分享信息
 */
export interface ShareInfo {
  title: string
  desc: string
  link: string
  imgUrl: string
}

/**
 * 获取微信JS-SDK配置
 */
export function getWechatJsConfig(url: string) {
  return http.post<JsConfig>('/wechat/js-config', { url })
}

/**
 * 获取分享信息
 */
export function getShareInfo(notificationId: string | number) {
  return http.get<ShareInfo>(`/wechat/share-info/${notificationId}`)
}

/**
 * 初始化微信分享
 * 需要在微信浏览器中使用
 */
export async function initWechatShare(notificationId: string | number): Promise<boolean> {
  // 检查是否在微信浏览器中
  const ua = navigator.userAgent.toLowerCase()
  const isWechat = ua.includes('micromessenger')

  if (!isWechat) {
    return false
  }

  try {
    // 获取JS-SDK配置
    const config = await getWechatJsConfig(window.location.href)

    if (config.error) {
      return false
    }

    // 获取分享信息
    const shareInfo = await getShareInfo(notificationId)

    // 初始化微信SDK
    // @ts-ignore
    if (typeof wx !== 'undefined') {
      // @ts-ignore
      wx.config({
        debug: false,
        appId: config.appId,
        timestamp: config.timestamp,
        nonceStr: config.nonceStr,
        signature: config.signature,
        jsApiList: [
          'updateAppMessageShareData',
          'updateTimelineShareData'
        ]
      })

      // @ts-ignore
      wx.ready(() => {
        // 分享给朋友
        // @ts-ignore
        wx.updateAppMessageShareData({
          title: shareInfo.title,
          desc: shareInfo.desc,
          link: shareInfo.link,
          imgUrl: shareInfo.imgUrl
        })

        // 分享到朋友圈
        // @ts-ignore
        wx.updateTimelineShareData({
          title: shareInfo.title,
          link: shareInfo.link,
          imgUrl: shareInfo.imgUrl
        })
      })

      // @ts-ignore
      wx.error(() => {
        // 微信SDK错误
      })

      return true
    }

    return false
  } catch {
    return false
  }
}

/**
 * 检查是否在微信浏览器中
 */
export function isWechatBrowser(): boolean {
  const ua = navigator.userAgent.toLowerCase()
  return ua.includes('micromessenger')
}

/**
 * 复制分享链接到剪贴板
 */
export async function copyShareLink(notificationId: string | number): Promise<boolean> {
  try {
    const shareInfo = await getShareInfo(notificationId)
    await navigator.clipboard.writeText(shareInfo.link)
    return true
  } catch {
    return false
  }
}
