import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/utils/request'

/**
 * 系统配置项接口
 */
interface SystemConfigItem {
  id: number
  configKey: string
  configValue: string
  configGroup?: string
  status?: number
}

/**
 * 系统配置 Store
 * 用于管理从后端加载的系统配置
 */
export const useConfigStore = defineStore('config', () => {
  // 系统配置
  const systemName = ref('学生管理系统')
  const systemVersion = ref('1.0.0')
  const systemLogo = ref('/logo.png')
  const systemCopyright = ref('Copyright © 2025')

  // 业务配置
  const passwordMinLength = ref(6)
  const maxFileSize = ref(5)
  const allowedFileTypes = ref('jpg,jpeg,png,gif,webp')
  const sessionTimeout = ref(30)

  // UI配置
  const defaultPageSize = ref(10)
  const pageSizeOptions = ref([10, 20, 50, 100])
  const themeColor = ref('#409EFF')

  // 加载状态
  const loading = ref(false)
  const loaded = ref(false)

  /**
   * 从后端加载系统配置
   */
  const loadSystemConfig = async () => {
    // 如果已经加载过，直接返回
    if (loaded.value) {
      return
    }

    // 如果正在加载，等待加载完成
    if (loading.value) {
      return
    }

    loading.value = true
    try {
      // 从后端获取 system 分组的配置（使用公开API，无需认证）
      const systemConfigs = await http.get('/system/configs/public/system')

      if (systemConfigs && Array.isArray(systemConfigs)) {
        // 遍历配置，更新对应的值
        systemConfigs.forEach((config: SystemConfigItem) => {
          switch (config.configKey) {
            case 'system.name':
              systemName.value = config.configValue || '学生管理系统'
              break
            case 'system.version':
              systemVersion.value = config.configValue || '1.0.0'
              break
            case 'system.logo':
              systemLogo.value = config.configValue || '/logo.png'
              break
            case 'system.copyright':
              systemCopyright.value = config.configValue || 'Copyright © 2025'
              break
          }
        })
      }

      // 加载 business 分组的配置（也使用公开API）
      try {
        const businessConfigs = await http.get('/system/configs/public/business')
        if (businessConfigs && Array.isArray(businessConfigs)) {
          businessConfigs.forEach((config: SystemConfigItem) => {
            switch (config.configKey) {
              case 'business.password_min_length':
                passwordMinLength.value = parseInt(config.configValue) || 6
                break
              case 'business.max_file_size':
                maxFileSize.value = parseInt(config.configValue) || 5
                break
              case 'business.allowed_file_types':
                allowedFileTypes.value = config.configValue || 'jpg,jpeg,png,gif,webp'
                break
              case 'business.session_timeout':
                sessionTimeout.value = parseInt(config.configValue) || 30
                break
            }
          })
        }
      } catch {
        // 使用默认值
      }

      // 加载 ui 分组的配置（也使用公开API）
      try {
        const uiConfigs = await http.get('/system/configs/public/ui')
        if (uiConfigs && Array.isArray(uiConfigs)) {
          uiConfigs.forEach((config: SystemConfigItem) => {
            switch (config.configKey) {
              case 'ui.default_page_size':
                defaultPageSize.value = parseInt(config.configValue) || 10
                break
              case 'ui.page_size_options':
                // 配置值格式: "10,20,50,100"
                if (config.configValue) {
                  pageSizeOptions.value = config.configValue
                    .split(',')
                    .map((v: string) => parseInt(v.trim()))
                    .filter((v: number) => !isNaN(v))
                }
                break
              case 'ui.theme_color':
                themeColor.value = config.configValue || '#409EFF'
                break
            }
          })
        }
      } catch {
        // 使用默认值
      }

      loaded.value = true
    } catch {
      // 使用默认值，不影响系统运行
    } finally {
      loading.value = false
    }
  }

  /**
   * 刷新系统配置（强制重新加载）
   */
  const refreshConfig = async () => {
    loaded.value = false
    await loadSystemConfig()
    // 加载完成后保存到缓存
    saveToCache()
  }

  /**
   * 从 localStorage 加载缓存的配置
   */
  const loadFromCache = () => {
    try {
      const cached = localStorage.getItem('system_config')
      if (cached) {
        const config = JSON.parse(cached)
        systemName.value = config.systemName || systemName.value
        systemVersion.value = config.systemVersion || systemVersion.value
        systemLogo.value = config.systemLogo || systemLogo.value
        systemCopyright.value = config.systemCopyright || systemCopyright.value
        loaded.value = true
      }
    } catch {
      // 缓存读取失败，使用默认值
    }
  }

  /**
   * 保存配置到 localStorage
   */
  const saveToCache = () => {
    try {
      const config = {
        systemName: systemName.value,
        systemVersion: systemVersion.value,
        systemLogo: systemLogo.value,
        systemCopyright: systemCopyright.value,
        timestamp: Date.now()
      }
      localStorage.setItem('system_config', JSON.stringify(config))
    } catch {
      // 缓存保存失败，忽略
    }
  }

  // 监听配置变化，自动保存到缓存
  const watchAndCache = () => {
    // 当配置加载完成后，保存到缓存
    if (loaded.value) {
      saveToCache()
    }
  }

  return {
    // 状态
    systemName,
    systemVersion,
    systemLogo,
    systemCopyright,
    passwordMinLength,
    maxFileSize,
    allowedFileTypes,
    sessionTimeout,
    defaultPageSize,
    pageSizeOptions,
    themeColor,
    loading,
    loaded,

    // 方法
    loadSystemConfig,
    refreshConfig,
    loadFromCache,
    saveToCache,
    watchAndCache
  }
})
