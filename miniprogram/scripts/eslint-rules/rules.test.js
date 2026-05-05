import { describe, it } from 'vitest'
import { RuleTester } from 'eslint'
import { createRequire } from 'module'

const require = createRequire(import.meta.url)
const noCoreImportingPlugin = require('./no-core-importing-plugin')
const noCrossPluginImport = require('./no-cross-plugin-import')
const noBareUniApi = require('./no-bare-uni-api')
const noBusinessInCore = require('./no-business-in-core')

const tester = new RuleTester({
  parser: require.resolve('@typescript-eslint/parser'),
  parserOptions: { ecmaVersion: 2020, sourceType: 'module' }
})

describe('mp-boundary rules', () => {
  it('runs', () => {
    tester.run('no-core-importing-plugin', noCoreImportingPlugin, {
      valid: [
        { code: "import x from '../platform/capability'", filename: 'src/core/plugin/dispatcher.ts' },
        { code: "import x from '@plugins/demo'", filename: 'src/plugins/healthcare/manifest.ts' },
        { code: "import x from '@core/plugin/event-bus'", filename: 'src/plugins/inspection/foo.ts' }
      ],
      invalid: [
        {
          code: "import x from '@plugins/demo'",
          filename: 'src/core/plugin/dispatcher.ts',
          errors: [{ messageId: 'coreImportPlugin' }]
        },
        {
          code: "import x from '../../plugins/demo/api'",
          filename: 'src/core/utils/foo.ts',
          errors: [{ messageId: 'coreImportPlugin' }]
        }
      ]
    })

    tester.run('no-cross-plugin-import', noCrossPluginImport, {
      valid: [
        { code: "import x from './pages/foo'", filename: 'src/plugins/inspection/manifest.ts' },
        { code: "import x from '@core/plugin/event-bus'", filename: 'src/plugins/inspection/manifest.ts' },
        { code: "import x from '@plugins/inspection/api'", filename: 'src/plugins/inspection/foo.ts' }
      ],
      invalid: [
        {
          code: "import x from '@plugins/healthcare/api'",
          filename: 'src/plugins/inspection/manifest.ts',
          errors: [{ messageId: 'crossPlugin' }]
        },
        {
          code: "import x from '../../plugins/healthcare/api'",
          filename: 'src/plugins/inspection/foo.ts',
          errors: [{ messageId: 'crossPlugin' }]
        }
      ]
    })

    tester.run('no-bare-uni-api', noBareUniApi, {
      valid: [
        { code: "capability.scan()", filename: 'src/plugins/inspection/pages/scan.vue' },
        { code: "uni.scanCode({})", filename: 'src/core/platform/weixin.ts' },
        { code: "uni.scanCode({})", filename: 'src/core/platform/auto.ts' }
      ],
      invalid: [
        {
          code: "uni.scanCode({})",
          filename: 'src/plugins/inspection/pages/scan.vue',
          errors: [{ messageId: 'bareUni' }]
        },
        {
          code: "uni.getLocation({})",
          filename: 'src/core/api/foo.ts',
          errors: [{ messageId: 'bareUni' }]
        }
      ]
    })

    tester.run('no-business-in-core', noBusinessInCore, {
      valid: [
        { code: "const x = 'tenant'", filename: 'src/core/api/auth.ts' },
        { code: "const x = 'STUDENT'", filename: 'src/plugins/inspection/api.ts' },
        { code: "const x = 'STUDENT'", filename: 'src/plugins/healthcare/foo.ts' }
      ],
      invalid: [
        {
          code: "const x = 'STUDENT'",
          filename: 'src/core/utils/foo.ts',
          errors: [{ messageId: 'businessLiteral' }]
        },
        {
          code: "if (type === 'PATIENT') {}",
          filename: 'src/core/api/foo.ts',
          errors: [{ messageId: 'businessLiteral' }]
        }
      ]
    })
  })
})
