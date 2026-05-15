#!/usr/bin/env node
/**
 * @hey-api/openapi-ts post-process: 把 int64 字段的类型从 `string` 还原成 `LongId`.
 *
 * 背景: 后端 LongAsStringModelConverter 给所有 Long 字段标 `x-typescript-type: LongId`,
 * 但 hey-api 不读 vendor extension, 默认生成 `string` (因为 schema 是 string/int64).
 *
 * 本脚本读取 openapi.json, 收集所有标了 x-typescript-type: LongId 的字段
 * (按 schema name + field name), 然后修改 types.gen.ts 让这些字段用 LongId.
 *
 * 算法: 对每个 schema, 找到 export type SchemaName = { ... } 块, 在块内
 * 把 `fieldName: string` / `fieldName?: string` 替换为 `LongId` / `LongId | undefined`.
 *
 * 调用: npm run openapi:generate (postscript)
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const FRONTEND_ROOT = path.resolve(__dirname, '..')
const SPEC_PATH = path.resolve(FRONTEND_ROOT, '../backend/openapi.json')
const TYPES_PATH = path.resolve(FRONTEND_ROOT, 'src/api-generated/types.gen.ts')
const SDK_PATH = path.resolve(FRONTEND_ROOT, 'src/api-generated/sdk.gen.ts')

function buildLongIdMap(spec) {
  /**
   * Returns Map<schemaName, Set<fieldName>>.
   * Each (schemaName, fieldName) pair has x-typescript-type: LongId in the spec.
   */
  const map = new Map()
  const schemas = spec.components?.schemas || {}
  for (const [name, schema] of Object.entries(schemas)) {
    const fields = new Set()
    const props = schema.properties || {}
    for (const [fname, fdef] of Object.entries(props)) {
      if (fdef && fdef['x-typescript-type'] === 'LongId') {
        fields.add(fname)
      }
      // arrays of LongId
      if (fdef && fdef.type === 'array' && fdef.items?.['x-typescript-type'] === 'LongId') {
        // separate marker for array fields
        fields.add(fname + '__ARRAY__')
      }
    }
    if (fields.size > 0) {
      map.set(name, fields)
    }
  }
  return map
}

function patchTypeBlock(content, schemaName, fieldNames) {
  /**
   * Replace fields' `string` types within `export type SchemaName = { ... }` block.
   * Handles both required (`fieldName: string`) and optional (`fieldName?: string`).
   */
  // Find the type block: `export type X = {\n ... \n}` (greedy until matching brace at same indent)
  // We use a simpler approach: locate the type declaration and walk braces
  const decl = new RegExp(`(export type ${escapeRegex(schemaName)}\\s*=\\s*\\{)`, 'g')
  let modified = false
  let result = content.replace(decl, (_match, prefix) => {
    // Find this block's end (matching closing brace) and patch within
    return prefix + '__BLOCK_START__'
  })
  if (!result.includes('__BLOCK_START__')) return content

  // Walk to find matching `}` for the block
  const startIdx = result.indexOf('__BLOCK_START__')
  result = result.replace('__BLOCK_START__', '')
  let depth = 1
  let i = startIdx
  while (i < result.length && depth > 0) {
    const ch = result[i]
    if (ch === '{') depth++
    else if (ch === '}') depth--
    i++
  }
  if (depth !== 0) return content // unbalanced, skip
  const blockEnd = i - 1

  let block = result.slice(startIdx, blockEnd)
  for (const fname of fieldNames) {
    const isArrayMarker = fname.endsWith('__ARRAY__')
    const realFname = isArrayMarker ? fname.replace('__ARRAY__', '') : fname
    if (isArrayMarker) {
      // `realFname?: Array<string>` → `Array<LongId>`
      block = block.replace(
        new RegExp(`(${escapeRegex(realFname)}\\??:\\s*)Array<string>`, 'g'),
        (_m, prefix) => { modified = true; return prefix + 'Array<LongId>' }
      )
      block = block.replace(
        new RegExp(`(${escapeRegex(realFname)}\\??:\\s*)string\\[\\]`, 'g'),
        (_m, prefix) => { modified = true; return prefix + 'LongId[]' }
      )
    } else {
      // `realFname?: string` → `LongId` (don't touch union types like `string | null`)
      block = block.replace(
        new RegExp(`(${escapeRegex(realFname)}\\??:\\s*)string(?=\\s*[,;\\n}])`, 'g'),
        (_m, prefix) => { modified = true; return prefix + 'LongId' }
      )
    }
  }
  return result.slice(0, startIdx) + block + result.slice(blockEnd)
}

function escapeRegex(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

// ─── Main ────────────────────────────────────────────────────────────────

const spec = JSON.parse(fs.readFileSync(SPEC_PATH, 'utf-8'))
const longIdMap = buildLongIdMap(spec)
console.log(`[post-process] ${longIdMap.size} schemas with LongId fields`)

let typesContent = fs.readFileSync(TYPES_PATH, 'utf-8')
let totalPatched = 0
for (const [schemaName, fieldNames] of longIdMap) {
  const before = typesContent
  typesContent = patchTypeBlock(typesContent, schemaName, fieldNames)
  if (typesContent !== before) totalPatched++
}

// Add LongId import if any patch happened
if (totalPatched > 0 && !typesContent.includes("from '@/types/common'")) {
  typesContent = `import type { LongId } from '@/types/common'\n\n` + typesContent
}

fs.writeFileSync(TYPES_PATH, typesContent)
console.log(`[post-process] Patched ${totalPatched} schemas; types.gen.ts updated`)

// SDK file — params/responses inherit types from types.gen.ts via imports, so usually no patch needed
console.log(`[post-process] Done.`)
