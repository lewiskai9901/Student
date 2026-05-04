const fs = require('fs')
const path = require('path')

const DEFAULT_EASYCOM = {
  autoscan: true,
  custom: {
    '^wd-(.*)': 'wot-design-uni/components/wd-$1/wd-$1.vue',
    '^(?!z-paging-refresh|z-paging-load-more)z-paging(.*)': 'z-paging/components/z-paging$1/z-paging$1.vue'
  }
}

const DEFAULT_GLOBAL_STYLE = {
  navigationBarTextStyle: 'black',
  navigationBarBackgroundColor: '#f3f5f8',
  backgroundColor: '#f3f5f8'
}

function generatePagesJson(corePages, plugins, options = {}) {
  const subPackages = plugins
    .filter(p => p.subPackage)
    .map(p => ({ root: p.subPackage.root, pages: [...p.subPackage.pages] }))

  return {
    easycom: options.easycom || DEFAULT_EASYCOM,
    pages: corePages,
    subPackages,
    tabBar: options.tabBar,
    globalStyle: options.globalStyle || DEFAULT_GLOBAL_STYLE
  }
}

function readCorePages() {
  const corePagesDir = path.resolve(__dirname, '../src/core/pages')
  const result = []
  if (!fs.existsSync(corePagesDir)) return result
  const dirs = fs.readdirSync(corePagesDir).sort()
  for (const d of dirs) {
    const indexFile = path.join(corePagesDir, d, 'index.vue')
    if (fs.existsSync(indexFile)) {
      result.push({ path: `core/pages/${d}/index`, style: { navigationStyle: 'custom' } })
    }
  }
  return result
}

function readPluginManifests() {
  const file = path.resolve(__dirname, '../src/plugins/manifests.json')
  if (!fs.existsSync(file)) return []
  return JSON.parse(fs.readFileSync(file, 'utf-8'))
}

function buildTabBar(corePages) {
  const known = new Set(corePages.map(p => p.path))
  const list = []
  if (known.has('core/pages/index/index')) {
    list.push({ pagePath: 'core/pages/index/index', text: '首页', iconPath: 'static/tabbar/home.png', selectedIconPath: 'static/tabbar/home-active.png' })
  }
  if (known.has('core/pages/message/index')) {
    list.push({ pagePath: 'core/pages/message/index', text: '消息', iconPath: 'static/tabbar/message.png', selectedIconPath: 'static/tabbar/message-active.png' })
  }
  if (known.has('core/pages/mine/index')) {
    list.push({ pagePath: 'core/pages/mine/index', text: '我的', iconPath: 'static/tabbar/mine.png', selectedIconPath: 'static/tabbar/mine-active.png' })
  }
  if (list.length === 0) return undefined
  return {
    color: '#c0c8d0',
    selectedColor: '#3a7bd5',
    backgroundColor: '#ffffff',
    borderStyle: 'white',
    list
  }
}

async function main() {
  const corePages = readCorePages()
  const plugins = readPluginManifests()
  const tabBar = buildTabBar(corePages)
  const result = generatePagesJson(corePages, plugins, { tabBar })

  // Drop undefined keys so the JSON is clean
  if (result.tabBar === undefined) delete result.tabBar

  const out = path.resolve(__dirname, '../src/pages.json')
  fs.writeFileSync(out, JSON.stringify(result, null, 2) + '\n', 'utf-8')
  console.log(`[build-pages-json] generated ${out} with ${corePages.length} core pages and ${plugins.length} plugins (${result.subPackages.length} subPackages)`)
}

if (require.main === module) {
  main().catch(e => { console.error(e); process.exit(1) })
}

module.exports = { generatePagesJson, readCorePages, readPluginManifests, buildTabBar, main }
