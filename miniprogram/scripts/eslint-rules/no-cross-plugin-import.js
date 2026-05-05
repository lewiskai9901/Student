'use strict'

module.exports = {
  meta: {
    type: 'problem',
    docs: { description: '插件 A 不得直接 import 插件 B,跨插件通信请走 EventBus 或 Contribution' },
    schema: [],
    messages: {
      crossPlugin: '插件 "{{my}}" 不得 import 插件 "{{other}}",跨插件通信请走 EventBus 或 Contribution'
    }
  },
  create(ctx) {
    const file = ctx.getFilename().replace(/\\/g, '/')
    const m = file.match(/\/src\/plugins\/([^/]+)\//)
    if (!m) return {}
    const myPlugin = m[1]
    return {
      ImportDeclaration(node) {
        const src = node.source.value
        if (typeof src !== 'string') return
        const target = src.match(/^@plugins\/([^/]+)/) || src.match(/(?:^|\/)plugins\/([^/]+)/)
        if (target && target[1] !== myPlugin) {
          ctx.report({ node, messageId: 'crossPlugin', data: { my: myPlugin, other: target[1] } })
        }
      }
    }
  }
}
