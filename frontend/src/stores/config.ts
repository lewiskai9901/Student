import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { http } from '@/utils/request'
import type { LoginCustomizationConfig, LoginFeature, BackgroundMode, TextPosition, DisplayMode, TitleFontFamily, TitleFontWeight, FormStyle, DecorationImage, BackgroundCarouselConfig, CustomFont } from '@/types/loginCustomization'
import { defaultLoginConfig } from '@/types/loginCustomization'

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

  // 登录页自定义配置
  const loginDisplayMode = ref<DisplayMode>('halfScreen')
  const loginBackgroundMode = ref<BackgroundMode>('gradient')
  const loginBackgroundImage = ref('')
  const loginBackgroundVideo = ref('')
  const loginTitle = ref('')
  const loginSubtitle = ref('专业**智能**教育管理平台')
  const loginFeatures = ref<LoginFeature[]>([
    { icon: 'users', text: '学生信息一站式管理' },
    { icon: 'chart', text: '量化考核智能分析' },
    { icon: 'building', text: '宿舍资源可视化管理' }
  ])
  const loginShowLogo = ref(true)
  const loginGradientFrom = ref('#2563eb')
  const loginGradientVia = ref('#1d4ed8')
  const loginGradientTo = ref('#4338ca')
  const loginTextPosition = ref<TextPosition>('center')
  const loginTextPositionX = ref(50)
  const loginTextPositionY = ref(50)
  const loginFormBgOpacity = ref(100)
  const loginOverlayOpacity = ref(40)

  // 新增：版权与显示控制
  const loginCopyrightText = ref('')
  const loginShowTitle = ref(true)
  const loginShowSubtitle = ref(true)
  const loginShowCopyright = ref(true)

  // 新增：标题字体设置
  const loginTitleFontFamily = ref<TitleFontFamily>('default')
  const loginTitleFontSize = ref(48)
  const loginTitleFontWeight = ref<TitleFontWeight>('bold')
  const loginTitleTextShadow = ref(true)
  const loginTitleGradient = ref(false)
  const loginTitleGradientFrom = ref('#ffffff')
  const loginTitleGradientTo = ref('#60a5fa')

  // 新增：表单样式
  const loginFormStyle = ref<FormStyle>('modern')

  // 新增：装饰图片
  const loginDecorationImages = ref<DecorationImage[]>([])

  // 新增：背景轮播
  const loginCarouselEnabled = ref(false)
  const loginCarouselItems = ref<BackgroundCarouselConfig['items']>([])
  const loginCarouselInterval = ref(5)
  const loginCarouselTransition = ref<'fade' | 'slide'>('fade')

  // 新增：自定义字体
  const loginCustomFonts = ref<CustomFont[]>([])
  const loginTitleCustomFontId = ref('')

  // 登录页配置计算属性
  const loginConfig = computed<LoginCustomizationConfig>(() => ({
    displayMode: loginDisplayMode.value,
    backgroundMode: loginBackgroundMode.value,
    backgroundImage: loginBackgroundImage.value,
    backgroundVideo: loginBackgroundVideo.value,
    title: loginTitle.value || systemName.value,
    subtitle: loginSubtitle.value,
    features: loginFeatures.value,
    showLogo: loginShowLogo.value,
    gradientFrom: loginGradientFrom.value,
    gradientVia: loginGradientVia.value,
    gradientTo: loginGradientTo.value,
    textPosition: loginTextPosition.value,
    textPositionX: loginTextPositionX.value,
    textPositionY: loginTextPositionY.value,
    formBgOpacity: loginFormBgOpacity.value,
    overlayOpacity: loginOverlayOpacity.value,
    // 新增字段
    copyrightText: loginCopyrightText.value,
    showTitle: loginShowTitle.value,
    showSubtitle: loginShowSubtitle.value,
    showCopyright: loginShowCopyright.value,
    titleFontFamily: loginTitleFontFamily.value,
    titleFontSize: loginTitleFontSize.value,
    titleFontWeight: loginTitleFontWeight.value,
    titleTextShadow: loginTitleTextShadow.value,
    titleGradient: loginTitleGradient.value,
    titleGradientFrom: loginTitleGradientFrom.value,
    titleGradientTo: loginTitleGradientTo.value,
    formStyle: loginFormStyle.value,
    // 新增字段
    decorationImages: loginDecorationImages.value,
    backgroundCarousel: {
      enabled: loginCarouselEnabled.value,
      items: loginCarouselItems.value,
      interval: loginCarouselInterval.value,
      transition: loginCarouselTransition.value
    },
    customFonts: loginCustomFonts.value,
    titleCustomFontId: loginTitleCustomFontId.value
  }))

  // 加载状态
  const loading = ref(false)
  const loaded = ref(false)
  let loadingPromise: Promise<void> | null = null

  /**
   * 从后端加载系统配置
   */
  const loadSystemConfig = async () => {
    // 如果已经加载过，直接返回
    if (loaded.value) {
      return
    }

    // 如果正在加载，等待加载完成
    if (loading.value && loadingPromise) {
      return loadingPromise
    }

    loading.value = true
    loadingPromise = (async () => {
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
              // 登录页自定义配置
              case 'ui.login.display_mode':
                loginDisplayMode.value = (config.configValue as DisplayMode) || 'halfScreen'
                break
              case 'ui.login.background_mode':
                loginBackgroundMode.value = (config.configValue as BackgroundMode) || 'gradient'
                break
              case 'ui.login.background_image':
                loginBackgroundImage.value = config.configValue || ''
                break
              case 'ui.login.background_video':
                loginBackgroundVideo.value = config.configValue || ''
                break
              case 'ui.login.title':
                loginTitle.value = config.configValue || ''
                break
              case 'ui.login.subtitle':
                loginSubtitle.value = config.configValue || '专业**智能**教育管理平台'
                break
              case 'ui.login.features':
                if (config.configValue) {
                  try {
                    loginFeatures.value = JSON.parse(config.configValue)
                  } catch {
                    // 使用默认值
                  }
                }
                break
              case 'ui.login.show_logo':
                loginShowLogo.value = config.configValue === 'true'
                break
              case 'ui.login.gradient_from':
                loginGradientFrom.value = config.configValue || '#2563eb'
                break
              case 'ui.login.gradient_via':
                loginGradientVia.value = config.configValue || '#1d4ed8'
                break
              case 'ui.login.gradient_to':
                loginGradientTo.value = config.configValue || '#4338ca'
                break
              case 'ui.login.text_position':
                loginTextPosition.value = (config.configValue as TextPosition) || 'center'
                break
              case 'ui.login.text_position_x':
                loginTextPositionX.value = parseInt(config.configValue) || 50
                break
              case 'ui.login.text_position_y':
                loginTextPositionY.value = parseInt(config.configValue) || 50
                break
              case 'ui.login.form_bg_opacity':
                loginFormBgOpacity.value = parseInt(config.configValue) || 100
                break
              case 'ui.login.overlay_opacity':
                loginOverlayOpacity.value = parseInt(config.configValue) || 40
                break
              // 新增：版权与显示控制
              case 'ui.login.copyright_text':
                loginCopyrightText.value = config.configValue || ''
                break
              case 'ui.login.show_title':
                loginShowTitle.value = config.configValue !== 'false'
                break
              case 'ui.login.show_subtitle':
                loginShowSubtitle.value = config.configValue !== 'false'
                break
              case 'ui.login.show_copyright':
                loginShowCopyright.value = config.configValue !== 'false'
                break
              // 新增：标题字体设置
              case 'ui.login.title_font_family':
                loginTitleFontFamily.value = (config.configValue as TitleFontFamily) || 'default'
                break
              case 'ui.login.title_font_size':
                loginTitleFontSize.value = parseInt(config.configValue) || 48
                break
              case 'ui.login.title_font_weight':
                loginTitleFontWeight.value = (config.configValue as TitleFontWeight) || 'bold'
                break
              case 'ui.login.title_text_shadow':
                loginTitleTextShadow.value = config.configValue !== 'false'
                break
              case 'ui.login.title_gradient':
                loginTitleGradient.value = config.configValue === 'true'
                break
              case 'ui.login.title_gradient_from':
                loginTitleGradientFrom.value = config.configValue || '#ffffff'
                break
              case 'ui.login.title_gradient_to':
                loginTitleGradientTo.value = config.configValue || '#60a5fa'
                break
              // 新增：表单样式
              case 'ui.login.form_style':
                loginFormStyle.value = (config.configValue as FormStyle) || 'modern'
                break
              // 新增：装饰图片
              case 'ui.login.decoration_images':
                if (config.configValue) {
                  try {
                    loginDecorationImages.value = JSON.parse(config.configValue)
                  } catch {
                    // 使用默认值
                  }
                }
                break
              // 新增：背景轮播
              case 'ui.login.carousel_enabled':
                loginCarouselEnabled.value = config.configValue === 'true'
                break
              case 'ui.login.carousel_items':
                if (config.configValue) {
                  try {
                    loginCarouselItems.value = JSON.parse(config.configValue)
                  } catch {
                    // 使用默认值
                  }
                }
                break
              case 'ui.login.carousel_interval':
                loginCarouselInterval.value = parseInt(config.configValue) || 5
                break
              case 'ui.login.carousel_transition':
                loginCarouselTransition.value = (config.configValue as 'fade' | 'slide') || 'fade'
                break
              // 新增：自定义字体
              case 'ui.login.custom_fonts':
                if (config.configValue) {
                  try {
                    loginCustomFonts.value = JSON.parse(config.configValue)
                  } catch {
                    // 使用默认值
                  }
                }
                break
              case 'ui.login.title_custom_font_id':
                loginTitleCustomFontId.value = config.configValue || ''
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
      loadingPromise = null
    }
    })()

    return loadingPromise
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

    // 登录页自定义配置
    loginDisplayMode,
    loginBackgroundMode,
    loginBackgroundImage,
    loginBackgroundVideo,
    loginTitle,
    loginSubtitle,
    loginFeatures,
    loginShowLogo,
    loginGradientFrom,
    loginGradientVia,
    loginGradientTo,
    loginTextPosition,
    loginTextPositionX,
    loginTextPositionY,
    loginFormBgOpacity,
    loginOverlayOpacity,
    // 新增：版权与显示控制
    loginCopyrightText,
    loginShowTitle,
    loginShowSubtitle,
    loginShowCopyright,
    // 新增：标题字体设置
    loginTitleFontFamily,
    loginTitleFontSize,
    loginTitleFontWeight,
    loginTitleTextShadow,
    loginTitleGradient,
    loginTitleGradientFrom,
    loginTitleGradientTo,
    // 新增：表单样式
    loginFormStyle,
    // 新增：装饰图片
    loginDecorationImages,
    // 新增：背景轮播
    loginCarouselEnabled,
    loginCarouselItems,
    loginCarouselInterval,
    loginCarouselTransition,
    // 新增：自定义字体
    loginCustomFonts,
    loginTitleCustomFontId,
    loginConfig,

    // 方法
    loadSystemConfig,
    refreshConfig,
    loadFromCache,
    saveToCache,
    watchAndCache
  }
})
