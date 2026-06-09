import { describe, it, expect } from 'vitest'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join, relative, sep } from 'node:path'

/**
 * 核心数据权限 UI 反行业词汇守护。
 *
 * 背景: CUSTOM data-scope 已统一到 org-units —— 自定义范围选择器是一棵通用组织树
 * (班级/年级本身就是 org_unit), 通用核心的数据权限向导零行业词汇。行业特化维度
 * (如教育"我的学生" 按班级/年级切片) 由行业插件前端入口 (router/plugins/edu.ts,
 * 在本目录之外) 通过 registerScopeSpecializations / registerRoleTemplates 注册。
 *
 * 本测试锁死: frontend/src/views/access/data-permissions/** 下不得新增任何行业专属
 * 字面量或标识符。允许的例外仅限于下方 ALLOWED 白名单里逐条登记的「解释性注释」——
 * 它们用教育作举例说明"什么东西由插件注册到别处", 不是核心硬编码。
 *
 * 若有人把行业概念写进核心功能代码 (label / 分支判断 / 字段名), 本测试会失败。
 */

const DIR = join(dirname(fileURLToPath(import.meta.url)), '..')

// 禁止出现的行业专属中文词汇 + 标识符
const BANNED_TERMS = [
  '班级',
  '年级',
  '专业',
  '同学',
  '班主任',
  '学生',
  'getAllClasses',
  'getAllCohorts',
  'customGradeIds',
  'customClassIds',
  'gradeIds',
  'classIds',
] as const

/**
 * 白名单: 已审计的合法例外 (解释插件贡献架构的注释, 用教育作举例)。
 * key = 相对 data-permissions 目录的 POSIX 路径; value = 该文件里允许的 (term -> 出现次数)。
 * 这些都是 JSDoc / 行内注释, 非功能代码。新增任何超出此基线的命中即视为回归。
 */
const ALLOWED: Record<string, Partial<Record<(typeof BANNED_TERMS)[number], number>>> = {
  // 插件特化注册表接口的 JSDoc, 用 "我的学生/班级/年级/专业" 举例说明插件注册什么
  'dataScopeSpecializations.ts': { 学生: 2, 班级: 1, 年级: 1, 专业: 1 },
  // composable 说明: 它本身不认识行业概念, 教育仅作举例
  'composables/useSceneTemplate.ts': { 学生: 1 },
  // 注释解释 EDU 关掉则插件模板 (班主任/年级主任) 不出现; 实际模板来自 pluginsStore.codes 数据驱动
  'components/TemplateLibraryDialog.vue': { 班主任: 1, 年级: 1 },
}

function listFiles(dir: string): string[] {
  const out: string[] = []
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) {
      if (entry === '__tests__' || entry === 'node_modules') continue
      out.push(...listFiles(full))
    } else if (/\.(ts|vue|tsx|js)$/.test(entry)) {
      out.push(full)
    }
  }
  return out
}

function toPosix(rel: string): string {
  return rel.split(sep).join('/')
}

function countOccurrences(haystack: string, needle: string): number {
  let count = 0
  let idx = haystack.indexOf(needle)
  while (idx !== -1) {
    count++
    idx = haystack.indexOf(needle, idx + needle.length)
  }
  return count
}

describe('data-permissions core: no industry vocabulary', () => {
  const files = listFiles(DIR)

  it('finds files to scan (sanity)', () => {
    expect(files.length).toBeGreaterThan(0)
  })

  it('contains no industry-specific literal beyond the audited whitelist', () => {
    const violations: string[] = []

    for (const file of files) {
      const relPath = toPosix(relative(DIR, file))
      const content = readFileSync(file, 'utf-8')
      const allowedForFile = ALLOWED[relPath] ?? {}

      for (const term of BANNED_TERMS) {
        const found = countOccurrences(content, term)
        const allowed = allowedForFile[term] ?? 0
        if (found > allowed) {
          violations.push(
            `${relPath}: "${term}" found ${found}x, whitelist allows ${allowed}x ` +
              `(industry vocabulary must not appear in core data-permission UI)`,
          )
        }
      }
    }

    expect(violations, `Industry vocabulary leak(s):\n${violations.join('\n')}`).toEqual([])
  })

  it('whitelist has no stale entries (every allowed file still exists & still matches)', () => {
    const stale: string[] = []
    for (const [relPath, terms] of Object.entries(ALLOWED)) {
      const full = join(DIR, ...relPath.split('/'))
      let content = ''
      try {
        content = readFileSync(full, 'utf-8')
      } catch {
        stale.push(`${relPath}: whitelisted file no longer exists`)
        continue
      }
      for (const [term, expected] of Object.entries(terms)) {
        const actual = countOccurrences(content, term)
        if (actual !== expected) {
          stale.push(
            `${relPath}: "${term}" whitelist expects ${expected}x but found ${actual}x ` +
              `(update ALLOWED to match the cleaned-up file)`,
          )
        }
      }
    }
    expect(stale, `Stale whitelist entries:\n${stale.join('\n')}`).toEqual([])
  })
})
