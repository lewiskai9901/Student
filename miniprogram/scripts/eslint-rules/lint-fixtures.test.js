import { describe, it, expect } from 'vitest'
import { createRequire } from 'module'

const require = createRequire(import.meta.url)
const { Linter } = require('eslint')
const tsParser = require('@typescript-eslint/parser')
const rules = require('./index.js')

function lintAs(code, filename, ruleId) {
  const linter = new Linter()
  linter.defineParser('@typescript-eslint/parser', tsParser)
  linter.defineRule(`mp-boundary/${ruleId}`, rules.rules[ruleId])
  return linter.verify(code, {
    parser: '@typescript-eslint/parser',
    parserOptions: { ecmaVersion: 2020, sourceType: 'module' },
    rules: { [`mp-boundary/${ruleId}`]: 'error' }
  }, { filename })
}

describe('mp-boundary fixture lint integration', () => {
  it('detects core importing plugin', () => {
    const errs = lintAs(
      "import x from '@plugins/demo'",
      '/src/core/foo.ts',
      'no-core-importing-plugin'
    )
    expect(errs.length).toBeGreaterThan(0)
    expect(errs[0].messageId).toBe('coreImportPlugin')
  })

  it('does NOT flag core importing core', () => {
    const errs = lintAs(
      "import x from '@core/plugin/event-bus'",
      '/src/core/foo.ts',
      'no-core-importing-plugin'
    )
    expect(errs).toHaveLength(0)
  })

  it('detects cross-plugin import', () => {
    const errs = lintAs(
      "import x from '@plugins/healthcare/api'",
      '/src/plugins/inspection/foo.ts',
      'no-cross-plugin-import'
    )
    expect(errs.length).toBeGreaterThan(0)
    expect(errs[0].messageId).toBe('crossPlugin')
  })

  it('does NOT flag intra-plugin import', () => {
    const errs = lintAs(
      "import x from '@plugins/inspection/api'",
      '/src/plugins/inspection/foo.ts',
      'no-cross-plugin-import'
    )
    expect(errs).toHaveLength(0)
  })

  it('detects bare uni api in plugin code', () => {
    const errs = lintAs(
      "declare const uni: any; uni.scanCode({})",
      '/src/plugins/inspection/foo.ts',
      'no-bare-uni-api'
    )
    expect(errs.length).toBeGreaterThan(0)
    expect(errs[0].messageId).toBe('bareUni')
  })

  it('exempts core/platform/ from no-bare-uni-api', () => {
    const errs = lintAs(
      "declare const uni: any; uni.scanCode({})",
      '/src/core/platform/weixin.ts',
      'no-bare-uni-api'
    )
    expect(errs).toHaveLength(0)
  })

  it('detects business literal in core', () => {
    const errs = lintAs(
      "const x = 'STUDENT'",
      '/src/core/foo.ts',
      'no-business-in-core'
    )
    expect(errs.length).toBeGreaterThan(0)
    expect(errs[0].messageId).toBe('businessLiteral')
  })

  it('does NOT flag business literal in plugin', () => {
    const errs = lintAs(
      "const x = 'STUDENT'",
      '/src/plugins/inspection/foo.ts',
      'no-business-in-core'
    )
    expect(errs).toHaveLength(0)
  })
})
