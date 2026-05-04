'use strict'

const FORBIDDEN = ['STUDENT', 'TEACHER', 'PATIENT', 'CLASS', 'COURSE', 'GRADE']

module.exports = {
  meta: {
    type: 'problem',
    docs: { description: 'core/ 不得出现行业字面量,请放到对应插件' },
    schema: [],
    messages: {
      businessLiteral: 'core/ 不得出现行业字面量 "{{lit}}",请将该业务移到对应插件'
    }
  },
  create(ctx) {
    const file = ctx.getFilename().replace(/\\/g, '/')
    if (!file.includes('/src/core/')) return {}
    return {
      Literal(node) {
        if (typeof node.value === 'string' && FORBIDDEN.includes(node.value)) {
          ctx.report({ node, messageId: 'businessLiteral', data: { lit: node.value } })
        }
      }
    }
  }
}
