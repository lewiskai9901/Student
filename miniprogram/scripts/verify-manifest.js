const fs = require('fs')
const path = require('path')

function verify(plugins) {
  const errors = []
  const seenPluginKeys = new Set()
  const seenMenuKeys = new Map()      // key → pluginKey
  const seenRoutePaths = new Map()    // path → pluginKey
  const seenScanPrefixes = new Map()  // prefix → pluginKey
  const declaredPerms = new Set()
  const referencedPerms = []

  for (const p of plugins) {
    if (!p.key || typeof p.key !== 'string') {
      errors.push(`Plugin missing key: ${JSON.stringify(p)}`)
      continue
    }
    if (seenPluginKeys.has(p.key)) {
      errors.push(`Duplicate plugin key: "${p.key}"`)
    }
    seenPluginKeys.add(p.key)
    if (p.schemaVersion !== 1) {
      errors.push(`Plugin "${p.key}" has invalid schemaVersion: ${p.schemaVersion} (expected 1)`)
    }

    for (const c of p.contributions || []) {
      if (c.type === 'menu') {
        if (seenMenuKeys.has(c.key)) {
          errors.push(`Duplicate menu key "${c.key}" (plugin "${p.key}" conflicts with "${seenMenuKeys.get(c.key)}")`)
        } else {
          seenMenuKeys.set(c.key, p.key)
        }
        if (c.perm) referencedPerms.push({ code: c.perm, source: `${p.key}/menu/${c.key}` })
      } else if (c.type === 'route') {
        if (seenRoutePaths.has(c.path)) {
          errors.push(`Duplicate route path "${c.path}" (plugin "${p.key}" conflicts with "${seenRoutePaths.get(c.path)}")`)
        } else {
          seenRoutePaths.set(c.path, p.key)
        }
      } else if (c.type === 'scan-resolver') {
        if (seenScanPrefixes.has(c.prefix)) {
          errors.push(`Duplicate scan-resolver prefix "${c.prefix}" (plugin "${p.key}" conflicts with "${seenScanPrefixes.get(c.prefix)}")`)
        } else {
          seenScanPrefixes.set(c.prefix, p.key)
        }
      } else if (c.type === 'permission') {
        declaredPerms.add(c.code)
      }
    }
  }

  for (const ref of referencedPerms) {
    if (!declaredPerms.has(ref.code)) {
      errors.push(`Permission "${ref.code}" referenced by ${ref.source} not declared by any PermissionContribution`)
    }
  }
  return errors
}

async function main() {
  const file = path.resolve(__dirname, '../src/plugins/manifests.json')
  if (!fs.existsSync(file)) {
    console.log('[verify-manifest] no plugins/manifests.json yet — skipping (Task 10 will create it)')
    return
  }
  const plugins = JSON.parse(fs.readFileSync(file, 'utf-8'))
  const errors = verify(plugins)
  if (errors.length > 0) {
    console.error('[verify-manifest] FAILED:')
    errors.forEach(e => console.error('  - ' + e))
    process.exit(1)
  }
  console.log(`[verify-manifest] OK: ${plugins.length} plugins`)
}

if (require.main === module) {
  main().catch(e => { console.error(e); process.exit(1) })
}

module.exports = { verify, main }
