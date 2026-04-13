import { translations } from './zh-CN'

/**
 * 自定义翻译函数
 */
function customTranslate(template: string, replacements?: Record<string, string>): string {
  replacements = replacements || {}

  // 先尝试直接查找翻译
  const translation = translations[template]

  if (translation) {
    // 替换占位符
    return translation.replace(/{([^}]+)}/g, (_: string, key: string) => {
      const value = replacements![key]
      // 尝试翻译替换值
      return translations[value] || value || `{${key}}`
    })
  }

  // 如果没找到直接翻译，尝试匹配带占位符的模板
  // 例如 "Create StartEvent" 可能来自 "Create {type}" + {type: "StartEvent"}
  if (replacements) {
    for (const [tplKey, tplValue] of Object.entries(translations)) {
      if (tplKey.includes('{') && tplKey.includes('}')) {
        // 构建正则表达式来匹配模板
        const regex = new RegExp('^' + tplKey.replace(/{[^}]+}/g, '(.+)') + '$')
        const match = template.match(regex)
        if (match) {
          // 找到匹配的模板，使用翻译后的值
          let result = tplValue
          const i = 1
          for (const key of Object.keys(replacements)) {
            const value = replacements[key]
            const translatedValue = translations[value] || value
            result = result.replace(`{${key}}`, translatedValue)
          }
          return result
        }
      }
    }
  }

  // 如果有占位符格式的模板，尝试翻译
  if (template.includes('{') && replacements) {
    const tpl = translations[template]
    if (tpl) {
      return tpl.replace(/{([^}]+)}/g, (_: string, key: string) => {
        const value = replacements![key]
        return translations[value] || value || `{${key}}`
      })
    }
  }

  // 返回原文
  return template
}

/**
 * 自定义翻译模块
 * bpmn-js 要求的格式
 */
export default {
  translate: ['value', customTranslate]
}
