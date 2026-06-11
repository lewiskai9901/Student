import { describe, it, expect } from 'vitest'
import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

/**
 * 核心总览看板反行业词汇守护 (dashboard 去教育侵入, 2026-06-12)。
 *
 * 背景: 行业看板卡 (教育的"办学规模"/"本学期教学") 由行业插件前端入口
 * (router/plugins/edu.ts) 经 registerDashboardCards 注册, 组件放
 * views/plugins/edu/dashboard/**; 后端对应 DashboardSectionContributor 分区贡献。
 * 核心 DashboardView 与卡片注册表必须零行业词汇 —— 行业概念写进这两个文件
 * (label / 统计键 / 快捷入口) 即回归。
 */

const SRC = join(dirname(fileURLToPath(import.meta.url)), '..', '..')

/** 核心 dashboard 文件 — 必须零行业词汇 */
const CORE_FILES = [
  'views/DashboardView.vue',
  'views/dashboard/dashboardCards.ts',
]

// 注: "成绩"不在禁词表 — 检查平台(通用核心)的"成绩单"(我的成绩单/受检成绩)是核心词汇,
// 与教学成绩无关; 教学成绩入口已注册化到 edu.ts。
const BANNED_TERMS = [
  '班级',
  '学生',
  '教师',
  '专业',
  '课程',
  '学期',
  '排课',
  '考试',
  'majorCount',
  'classCount',
  'studentCount',
  'teacherCount',
  'currentSemester',
  'scheduledRate',
  'teaching',
] as const

describe('核心 dashboard 零行业词汇守护', () => {
  for (const rel of CORE_FILES) {
    it(`${rel} 不含行业词汇`, () => {
      const path = join(SRC, rel)
      if (!existsSync(path)) {
        throw new Error(`核心 dashboard 文件不存在: ${rel} — 若重命名/移动, 同步更新本守护`)
      }
      const content = readFileSync(path, 'utf-8')
      const hits: string[] = []
      for (const term of BANNED_TERMS) {
        if (content.includes(term)) hits.push(term)
      }
      expect(hits, `核心 dashboard 文件 ${rel} 含行业词汇 (应由插件经 registerDashboardCards 贡献): ${hits.join(', ')}`).toEqual([])
    })
  }
})
