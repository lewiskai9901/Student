/**
 * V7 检查平台 - 表单字段渲染 Composable
 *
 * 将 ItemType 枚举映射到 Element Plus / 自定义组件和默认 props
 */

export interface ItemRenderConfig {
  /** 组件名称（Element Plus 或自定义组件） */
  component: string
  /** 组件默认 props */
  props: Record<string, any>
}

/**
 * 字段类型 -> 渲染配置映射表
 */
const TYPE_CONFIG: Record<string, ItemRenderConfig> = {
  // ---- 文本类 ----
  TEXT:         { component: 'el-input', props: { type: 'text' } },
  TEXTAREA:    { component: 'el-input', props: { type: 'textarea', rows: 3 } },
  RICH_TEXT:   { component: 'el-input', props: { type: 'textarea', rows: 5 } },
  // ---- 数值类 ----
  NUMBER:      { component: 'el-input-number', props: {} },
  SLIDER:      { component: 'el-slider', props: {} },
  // ---- 选择类 ----
  SELECT:       { component: 'el-select', props: {} },
  MULTI_SELECT: { component: 'el-select', props: { multiple: true } },
  CHECKBOX:     { component: 'el-checkbox', props: {} },
  RADIO:        { component: 'el-radio-group', props: {} },
  // ---- 日期 / 时间类 ----
  DATE:        { component: 'el-date-picker', props: { type: 'date' } },
  TIME:        { component: 'el-time-picker', props: {} },
  DATETIME:    { component: 'el-date-picker', props: { type: 'datetime' } },
  // ---- 媒体 / 上传类 ----
  PHOTO:       { component: 'upload-photo', props: {} },
  VIDEO:       { component: 'upload-video', props: {} },
  FILE_UPLOAD: { component: 'upload-file', props: {} },
  SIGNATURE:   { component: 'signature-pad', props: {} },
  // ---- 特殊输入 ----
  GPS:         { component: 'gps-capture', props: {} },
  BARCODE:     { component: 'barcode-scanner', props: {} },
}

/** 上传型字段 */
const UPLOAD_TYPES = new Set(['PHOTO', 'VIDEO', 'FILE'])

export function useFormItemRenderer() {
  /**
   * 获取指定字段类型的渲染配置
   * 未知类型回退到普通文本输入
   */
  function getConfig(itemType: string): ItemRenderConfig {
    return TYPE_CONFIG[itemType] || { component: 'el-input', props: {} }
  }

  /**
   * 是否为上传类字段
   */
  function isUploadType(itemType: string): boolean {
    return UPLOAD_TYPES.has(itemType)
  }

  /**
   * 获取完整类型配置表（供配置界面或调试使用）
   */
  function getAllTypes(): Record<string, ItemRenderConfig> {
    return { ...TYPE_CONFIG }
  }

  return {
    getConfig,
    isUploadType,
    getAllTypes,
    typeConfig: TYPE_CONFIG,
  }
}
