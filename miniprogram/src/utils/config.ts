/**
 * 应用配置
 */

// 判断平台
// @ts-ignore
const isH5 = typeof window !== 'undefined' && typeof document !== 'undefined'

// 环境配置
const ENV_CONFIG = {
  development: {
    // H5模式使用代理路径，小程序模式使用完整地址
    BASE_URL: isH5 ? '/api' : 'http://127.0.0.1:8080/api',
    WX_APPID: 'wx_dev_appid'
  },
  production: {
    BASE_URL: 'https://api.yourschool.com/api',
    WX_APPID: 'wx_prod_appid'
  }
}

// 当前环境
const ENV = process.env.NODE_ENV === 'production' ? 'production' : 'development'

export const config = {
  ...ENV_CONFIG[ENV],
  // 请求超时时间
  REQUEST_TIMEOUT: 30000,
  // Token存储key
  TOKEN_KEY: 'access_token',
  REFRESH_TOKEN_KEY: 'refresh_token',
  USER_INFO_KEY: 'user_info',
  // 版本号
  VERSION: '1.0.0'
}

export default config
