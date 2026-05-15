#!/usr/bin/env node
/**
 * 小程序版 SDK post-process: int64 字段 string → LongId.
 *
 * 与 frontend 同思路, 但 LongId import 路径换为 @core/types.
 * 详细注释见 frontend/scripts/post-process-openapi-sdk.mjs.
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '..')
const SPEC_PATH = path.resolve(ROOT, '../backend/openapi.json')
const TYPES_PATH = path.resolve(ROOT, 'src/api-generated/types.gen.ts')

function buildLongIdMap(spec) {
  const map = new Map()
  const schemas = spec.components?.schemas || {}
  for (const [name, schema] of Object.entries(schemas)) {
    const fields = new Set()
    const props = schema.properties || {}
    for (const [fname, fdef] of Object.entries(props)) {
      if (fdef && fdef['x-typescript-type'] === 'LongId') fields.add(fname)
      if (fdef && fdef.type === 'array' && fdef.items?.['x-typescript-type'] === 'LongId') {
        fields.add(fname + '__ARRAY__')
      }
    }
    if (fields.size > 0) map.set(name, fields)
  }
  return map
}

function escapeRegex(s) { return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') }

function patchTypeBlock(content, schemaName, fieldNames) {
  const decl = new RegExp(`(export type ${escapeRegex(schemaName)}\\s*=\\s*\\{)`, 'g')
  let result = content.replace(decl, (_match, prefix) => prefix + '__BLOCK_START__')
  if (!result.includes('__BLOCK_START__')) return content
  const startIdx = result.indexOf('__BLOCK_START__')
  result = result.replace('__BLOCK_START__', '')
  let depth = 1, i = startIdx
  while (i < result.length && depth > 0) {
    if (result[i] === '{') depth++
    else if (result[i] === '}') depth--
    i++
  }
  if (depth !== 0) return content
  const blockEnd = i - 1
  let block = result.slice(startIdx, blockEnd)
  for (const fname of fieldNames) {
    const isArrayMarker = fname.endsWith('__ARRAY__')
    const realFname = isArrayMarker ? fname.replace('__ARRAY__', '') : fname
    if (isArrayMarker) {
      block = block.replace(
        new RegExp(`(${escapeRegex(realFname)}\\??:\\s*)Array<string>`, 'g'),
        (_m, p) => p + 'Array<LongId>'
      )
      block = block.replace(
        new RegExp(`(${escapeRegex(realFname)}\\??:\\s*)string\\[\\]`, 'g'),
        (_m, p) => p + 'LongId[]'
      )
    } else {
      block = block.replace(
        new RegExp(`(${escapeRegex(realFname)}\\??:\\s*)string(?=\\s*[,;\\n}])`, 'g'),
        (_m, p) => p + 'LongId'
      )
    }
  }
  return result.slice(0, startIdx) + block + result.slice(blockEnd)
}

const spec = JSON.parse(fs.readFileSync(SPEC_PATH, 'utf-8'))
const longIdMap = buildLongIdMap(spec)
console.log(`[post-process] ${longIdMap.size} schemas with LongId fields`)

let content = fs.readFileSync(TYPES_PATH, 'utf-8')
let totalPatched = 0
for (const [schemaName, fieldNames] of longIdMap) {
  const before = content
  content = patchTypeBlock(content, schemaName, fieldNames)
  if (content !== before) totalPatched++
}

if (totalPatched > 0 && !content.includes("from '@core/types'")) {
  content = `import type { LongId } from '@core/types'\n\n` + content
}

fs.writeFileSync(TYPES_PATH, content)
console.log(`[post-process] Patched ${totalPatched} schemas; types.gen.ts updated`)
