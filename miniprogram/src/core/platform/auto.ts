import type { PlatformCapability } from './capability'
import { createWeixinCapability } from './weixin'

export function createCapability(): PlatformCapability {
  // #ifdef MP-WEIXIN
  return createWeixinCapability()
  // #endif
  // #ifndef MP-WEIXIN
  // 其他平台 (alipay/h5/...) Phase B/D 接入,目前 fallback 为 weixin 仅供单测使用,
  // 真实非微信运行时应直接报错。
  if (typeof process !== 'undefined' && process.env?.NODE_ENV === 'test') {
    return createWeixinCapability()
  }
  throw new Error('Platform not supported in Phase A — only mp-weixin is available')
  // #endif
}

let _instance: PlatformCapability | null = null
export const capability: PlatformCapability = new Proxy({} as PlatformCapability, {
  get(_target, prop) {
    if (!_instance) _instance = createCapability()
    return (_instance as any)[prop]
  }
})
