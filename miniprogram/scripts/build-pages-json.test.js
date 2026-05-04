import { describe, it, expect } from 'vitest'
import { generatePagesJson } from './build-pages-json.js'

describe('generatePagesJson', () => {
  it('merges core pages + plugin subPackages', () => {
    const corePages = [
      { path: 'core/pages/login/index', style: { navigationStyle: 'custom' } },
      { path: 'core/pages/index/index', style: { navigationStyle: 'custom' } }
    ]
    const plugins = [
      {
        key: 'demo',
        subPackage: { root: 'plugins/demo', pages: ['pages/hello'] },
        contributions: [{ type: 'route', path: 'plugins/demo/pages/hello', inSubPackage: true }]
      }
    ]
    const result = generatePagesJson(corePages, plugins)
    expect(result.pages).toHaveLength(2)
    expect(result.subPackages).toHaveLength(1)
    expect(result.subPackages[0].root).toBe('plugins/demo')
    expect(result.subPackages[0].pages).toEqual(['pages/hello'])
  })

  it('preserves tabBar from options', () => {
    const result = generatePagesJson([], [], {
      tabBar: { list: [{ pagePath: 'core/pages/index/index', text: '首页' }] }
    })
    expect(result.tabBar.list).toHaveLength(1)
  })

  it('returns default easycom config', () => {
    const result = generatePagesJson([], [])
    expect(result.easycom.autoscan).toBe(true)
    expect(result.easycom.custom['^wd-(.*)']).toContain('wot-design-uni')
  })

  it('skips plugins without subPackage', () => {
    const plugins = [
      { key: 'a', subPackage: { root: 'plugins/a', pages: ['p1'] }, contributions: [] },
      { key: 'b', contributions: [] } // no subPackage
    ]
    const result = generatePagesJson([], plugins)
    expect(result.subPackages).toHaveLength(1)
    expect(result.subPackages[0].root).toBe('plugins/a')
  })
})
