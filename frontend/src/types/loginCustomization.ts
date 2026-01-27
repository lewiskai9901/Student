/**
 * 登录页自定义配置类型定义
 */

/**
 * 背景模式
 */
export type BackgroundMode = 'gradient' | 'image' | 'video'

/**
 * 装饰图片
 */
export interface DecorationImage {
  id: string              // 唯一标识
  url: string             // 图片URL
  positionX: number       // X坐标百分比 (0-100)
  positionY: number       // Y坐标百分比 (0-100)
  width: number           // 宽度百分比 (5-50)
  opacity: number         // 透明度 (0-100)
  rotation: number        // 旋转角度 (-180 到 180)
}

/**
 * 轮播项
 */
export interface CarouselItem {
  id: string
  url: string
  type: 'image' | 'gif' | 'video'
}

/**
 * 轮播配置
 */
export interface BackgroundCarouselConfig {
  enabled: boolean
  items: CarouselItem[]   // 最多5个
  interval: number        // 切换间隔 (秒, 3-30)
  transition: 'fade' | 'slide'
}

/**
 * 自定义字体
 */
export interface CustomFont {
  id: string
  name: string            // 显示名称
  url: string             // 字体文件URL
  fontFamily: string      // CSS font-family
  format: 'truetype' | 'woff' | 'woff2'
}

/**
 * 磁吸参考线
 */
export interface SnapGuide {
  type: 'horizontal' | 'vertical'
  position: number        // 百分比位置
  label: string           // 标签
}

/**
 * 磁吸阈值 (百分比)
 */
export const SNAP_THRESHOLD = 5

/**
 * 预定义的磁吸参考线
 */
export const SNAP_GUIDES: SnapGuide[] = [
  { type: 'horizontal', position: 50, label: '垂直居中' },
  { type: 'horizontal', position: 33.33, label: '上1/3' },
  { type: 'horizontal', position: 66.67, label: '下1/3' },
  { type: 'vertical', position: 50, label: '水平居中' },
  { type: 'vertical', position: 25, label: '左1/4' },
  { type: 'vertical', position: 75, label: '右1/4' }
]

/**
 * 显示模式
 * - fullScreen: 全屏模式，背景铺满整个页面，登录框也显示在背景之上
 * - halfScreen: 半屏模式，背景只显示在左侧区域
 */
export type DisplayMode = 'fullScreen' | 'halfScreen'

/**
 * 文字位置
 */
export type TextPosition = 'top' | 'center' | 'bottom'

/**
 * 特性项图标类型
 */
export type FeatureIcon = 'users' | 'chart' | 'building' | 'book' | 'shield' | 'clock' | 'star' | 'check'

/**
 * 特性项
 */
export interface LoginFeature {
  icon: FeatureIcon
  text: string
}

/**
 * 标题字体类型
 */
export type TitleFontFamily = 'default' | 'serif' | 'songti' | 'heiti' | 'kaiti' | 'rounded' | 'artistic'

/**
 * 标题字体粗细
 */
export type TitleFontWeight = 'normal' | 'medium' | 'semibold' | 'bold' | 'extrabold'

/**
 * 表单样式主题
 */
export type FormStyle = 'classic' | 'modern' | 'minimal'

/**
 * 字体选项配置
 */
export interface FontOption {
  value: TitleFontFamily
  label: string
  fontFamily: string
}

/**
 * 可用字体选项
 */
export const fontOptions: FontOption[] = [
  { value: 'default', label: '默认', fontFamily: 'system-ui, -apple-system, sans-serif' },
  { value: 'serif', label: '衬线体', fontFamily: 'Georgia, "Times New Roman", serif' },
  { value: 'songti', label: '宋体', fontFamily: '"SimSun", "宋体", serif' },
  { value: 'heiti', label: '黑体', fontFamily: '"Microsoft YaHei", "微软雅黑", "PingFang SC", sans-serif' },
  { value: 'kaiti', label: '楷体', fontFamily: '"KaiTi", "楷体", "STKaiti", serif' },
  { value: 'rounded', label: '圆体', fontFamily: '"PingFang SC", "苹方", "Noto Sans SC", sans-serif' },
  { value: 'artistic', label: '艺术体', fontFamily: '"STXingkai", "华文行楷", "STCaiyun", cursive' }
]

/**
 * 字体粗细选项
 */
export const fontWeightOptions: { value: TitleFontWeight; label: string; weight: number }[] = [
  { value: 'normal', label: '常规', weight: 400 },
  { value: 'medium', label: '中等', weight: 500 },
  { value: 'semibold', label: '半粗', weight: 600 },
  { value: 'bold', label: '粗体', weight: 700 },
  { value: 'extrabold', label: '特粗', weight: 800 }
]

/**
 * 表单样式选项
 */
export const formStyleOptions: { value: FormStyle; label: string; desc: string }[] = [
  { value: 'classic', label: '经典白底', desc: '简洁传统风格' },
  { value: 'modern', label: '现代玻璃', desc: '毛玻璃+阴影效果' },
  { value: 'minimal', label: '极简透明', desc: '无边框极简风格' }
]

/**
 * 根据字体类型获取字体族
 */
export const getFontFamily = (fontType: TitleFontFamily): string => {
  const option = fontOptions.find(f => f.value === fontType)
  return option?.fontFamily || fontOptions[0].fontFamily
}

/**
 * 根据字体粗细类型获取权重值
 */
export const getFontWeight = (weight: TitleFontWeight): number => {
  const option = fontWeightOptions.find(w => w.value === weight)
  return option?.weight || 700
}

/**
 * 登录页配置
 */
export interface LoginCustomizationConfig {
  /** 显示模式 */
  displayMode: DisplayMode
  /** 背景模式 */
  backgroundMode: BackgroundMode
  /** 背景图片URL */
  backgroundImage: string
  /** 背景视频URL */
  backgroundVideo: string
  /** 主标题 */
  title: string
  /** 副标题 */
  subtitle: string
  /** 特性列表 */
  features: LoginFeature[]
  /** 是否显示Logo */
  showLogo: boolean
  /** 渐变起始颜色 */
  gradientFrom: string
  /** 渐变中间颜色 */
  gradientVia: string
  /** 渐变结束颜色 */
  gradientTo: string
  /** 文字位置预设 */
  textPosition: TextPosition
  /** 文字X坐标百分比 (0-100)，50为居中 */
  textPositionX: number
  /** 文字Y坐标百分比 (0-100)，50为居中 */
  textPositionY: number
  /** 表单背景透明度 (0-100) */
  formBgOpacity: number
  /** 遮罩透明度 (0-100) */
  overlayOpacity: number

  // 新增：版权与显示控制
  /** 登录页版权文字（空则使用系统版权） */
  copyrightText: string
  /** 是否显示标题 */
  showTitle: boolean
  /** 是否显示标语 */
  showSubtitle: boolean
  /** 是否显示版权 */
  showCopyright: boolean

  // 新增：标题字体设置
  /** 标题字体 */
  titleFontFamily: TitleFontFamily
  /** 标题字体大小(px) */
  titleFontSize: number
  /** 标题字体粗细 */
  titleFontWeight: TitleFontWeight
  /** 标题阴影效果 */
  titleTextShadow: boolean
  /** 标题渐变色效果 */
  titleGradient: boolean
  /** 标题渐变起始色 */
  titleGradientFrom: string
  /** 标题渐变结束色 */
  titleGradientTo: string

  // 新增：表单样式
  /** 表单样式主题 */
  formStyle: FormStyle

  // 新增：装饰图片
  /** 装饰图片列表 (最多3个) */
  decorationImages: DecorationImage[]

  // 新增：背景轮播
  /** 背景轮播配置 */
  backgroundCarousel: BackgroundCarouselConfig

  // 新增：自定义字体
  /** 自定义字体列表 */
  customFonts: CustomFont[]
  /** 标题使用的自定义字体ID (空表示使用内置字体) */
  titleCustomFontId: string
}

/**
 * 默认登录页配置
 */
export const defaultLoginConfig: LoginCustomizationConfig = {
  displayMode: 'halfScreen',
  backgroundMode: 'gradient',
  backgroundImage: '',
  backgroundVideo: '',
  title: '',
  subtitle: '专业**智能**教育管理平台',
  features: [
    { icon: 'users', text: '学生信息一站式管理' },
    { icon: 'chart', text: '量化考核智能分析' },
    { icon: 'building', text: '宿舍资源可视化管理' }
  ],
  showLogo: true,
  gradientFrom: '#2563eb',
  gradientVia: '#1d4ed8',
  gradientTo: '#4338ca',
  textPosition: 'center',
  textPositionX: 50,
  textPositionY: 50,
  formBgOpacity: 100,
  overlayOpacity: 40,

  // 新增字段默认值
  copyrightText: '',
  showTitle: true,
  showSubtitle: true,
  showCopyright: true,
  titleFontFamily: 'default',
  titleFontSize: 48,
  titleFontWeight: 'bold',
  titleTextShadow: true,
  titleGradient: false,
  titleGradientFrom: '#ffffff',
  titleGradientTo: '#60a5fa',
  formStyle: 'modern',

  // 新增字段默认值
  decorationImages: [],
  backgroundCarousel: {
    enabled: false,
    items: [],
    interval: 5,
    transition: 'fade'
  },
  customFonts: [],
  titleCustomFontId: ''
}

/**
 * 可用的图标选项
 */
export const featureIconOptions: { value: FeatureIcon; label: string }[] = [
  { value: 'users', label: '用户' },
  { value: 'chart', label: '图表' },
  { value: 'building', label: '建筑' },
  { value: 'book', label: '书本' },
  { value: 'shield', label: '安全' },
  { value: 'clock', label: '时钟' },
  { value: 'star', label: '星星' },
  { value: 'check', label: '勾选' }
]
