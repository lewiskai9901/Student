'use strict'

module.exports = {
  meta: {
    type: 'problem',
    docs: { description: 'core/ 不得 import plugins/* (违反 core 通用纯净)' },
    schema: [],
    messages: {
      coreImportPlugin: 'core/ 不得 import plugins/* — 通用核心必须不感知具体业务插件'
    }
  },
  create(ctx) {
    const file = ctx.getFilename().replace(/\\/g, '/')
    if (!file.includes('/src/core/')) return {}
    return {
      ImportDeclaration(node) {
        const src = node.source.value
        if (typeof src !== 'string') return
        if (src.startsWith('@plugins/') || /(^|\/)plugins\//.test(src)) {
          ctx.report({ node, messageId: 'coreImportPlugin' })
        }
      }
    }
  }
}
