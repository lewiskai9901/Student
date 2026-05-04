'use strict'

const FORBIDDEN = [
  'scanCode', 'getLocation', 'chooseImage', 'uploadFile',
  'getStorageSync', 'setStorageSync', 'removeStorageSync',
  'requestSubscribeMessage'
]

module.exports = {
  meta: {
    type: 'problem',
    docs: { description: '插件代码禁止裸调 uni.* 受限 API,必须通过 capability' },
    schema: [],
    messages: {
      bareUni: '插件代码禁止裸调 uni.{{api}},请通过 capability.* 调用'
    }
  },
  create(ctx) {
    const file = ctx.getFilename().replace(/\\/g, '/')
    // Allow inside core/platform/ — that's where the wrappers live
    if (file.includes('/src/core/platform/')) return {}
    return {
      MemberExpression(node) {
        if (
          node.object && node.object.type === 'Identifier' && node.object.name === 'uni' &&
          node.property && node.property.type === 'Identifier' && FORBIDDEN.includes(node.property.name)
        ) {
          ctx.report({ node, messageId: 'bareUni', data: { api: node.property.name } })
        }
      }
    }
  }
}
