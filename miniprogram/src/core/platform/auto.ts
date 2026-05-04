import type { PlatformCapability } from './capability'
import { createWeixinCapability } from './weixin'

export function createCapability(): PlatformCapability {
  // #ifdef MP-WEIXIN
  return createWeixinCapability()
  // #endif
  // #ifndef MP-WEIXIN
  // 暂只支持微信,其他平台后续添加
  return createWeixinCapability()
  // #endif
}

export const capability: PlatformCapability = createCapability()
