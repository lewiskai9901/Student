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
    .map(p => ({
      root: p.subPackage.root,
      // uni-cli expects each entry as { path } object; manifests.json stores plain strings.
      pages: p.subPackage.pages.map(pg => (typeof pg === 'string' ? { path: pg } : pg))
    }))

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
  // login must be first (launch page); index/message/mine follow as tabBar pages; rest alphabetical.
  const priority = ['login', 'index', 'message', 'mine']
  const ordered = priority.filter(d => dirs.includes(d))
    .concat(dirs.filter(d => !priority.includes(d)))
  // Page-specific style: login uses custom (no nav bar — full-screen splash);
  // tab pages and browse pages use default nav bar so users get a back arrow.
  const customNav = new Set(['login'])
  const pageTitles = {
    index: '首页',
    message: '消息',
    mine: '我的',
    org: '组织架构',
    directory: '通讯录',
    place: '场所'
  }
  for (const d of ordered) {
    const indexFile = path.join(corePagesDir, d, 'index.vue')
    if (fs.existsSync(indexFile)) {
      const style = customNav.has(d)
        ? { navigationStyle: 'custom' }
        : { navigationBarTitleText: pageTitles[d] || d }
      result.push({ path: `core/pages/${d}/index`, style })
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
  const path = require('path')
  const fs = require('fs')
  const staticDir = path.resolve(__dirname, '../src/static/tabbar')
  const hasIcon = (file) => fs.existsSync(path.join(staticDir, file))
  const withIcon = (entry, icon, active) => {
    if (hasIcon(icon) && hasIcon(active)) {
      entry.iconPath = `static/tabbar/${icon}`
      entry.selectedIconPath = `static/tabbar/${active}`
    }
    return entry
  }
  const list = []
  if (known.has('core/pages/index/index')) {
    list.push(withIcon({ pagePath: 'core/pages/index/index', text: '首页' }, 'home.png', 'home-active.png'))
  }
  if (known.has('core/pages/message/index')) {
    list.push(withIcon({ pagePath: 'core/pages/message/index', text: '消息' }, 'message.png', 'message-active.png'))
  }
  if (known.has('core/pages/mine/index')) {
    list.push(withIcon({ pagePath: 'core/pages/mine/index', text: '我的' }, 'mine.png', 'mine-active.png'))
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
