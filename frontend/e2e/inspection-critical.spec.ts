import { test, expect } from './fixtures/auth.fixture'

/**
 * Inspection 检查平台 关键路径 e2e (Track 1 — Road to A)
 *
 * 覆盖 10 条 smoke + 流程:
 *   1. 登录后能进入 /inspection/projects
 *   2. 列表视图加载 (KPI + 工具条 + 表头)
 *   3. 切换看板视图: URL 含 view=kanban + 3 列分组渲染
 *   4. 切换时间轴视图: URL 含 view=timeline&scale=auto + 缩放控件
 *   5. KPI "进行中" 按钮筛选: 激活 + URL status=PUBLISHED
 *   6. ARCHIVED toggle: URL archived=1 切换
 *   7. 列头点击排序 (项目): URL sort=name&dir=desc
 *   8. 详情页 5 tab 顺序正确 (总览>检查配置>人员与任务>成绩统计>设置)
 *   9. 详情页切到 "检查配置" tab 内容渲染
 *   10. 时间轴 "本季" 缩放: URL scale=quarter
 *
 * 登录由 fixture (auth.fixture.ts) 自动注入 sessionStorage token, 不需要 beforeEach.
 */

test.describe('Inspection 检查项目页', () => {
  test.describe.configure({ retries: 1, timeout: 60000 })

  test('1. 进入 /inspection/projects 加载完成', async ({ page }) => {
    await page.goto('/inspection/projects')
    await expect(page.locator('h1:has-text("检查项目")')).toBeVisible({ timeout: 10000 })
  })

  test('2. 列表视图: KPI + 工具条 + 表头都渲染', async ({ page }) => {
    await page.goto('/inspection/projects')
    await expect(page.locator('h1:has-text("检查项目")')).toBeVisible({ timeout: 10000 })
    // KPI 4 个 (总数 / 进行中 / 草稿 / 告警)
    await expect(page.locator('.kpi__label:has-text("总数")')).toBeVisible()
    await expect(page.locator('.kpi__label:has-text("进行中")')).toBeVisible()
    await expect(page.locator('.kpi__label:has-text("草稿")')).toBeVisible()
    await expect(page.locator('.kpi__label:has-text("告警")')).toBeVisible()
    // 工具条: 3 个视图按钮
    await expect(page.locator('.view-tab:has-text("列表")')).toBeVisible()
    await expect(page.locator('.view-tab:has-text("看板")')).toBeVisible()
    await expect(page.locator('.view-tab:has-text("时间轴")')).toBeVisible()
    // 表头
    await expect(page.locator('.row--head .col-name:has-text("项目")')).toBeVisible()
  })

  test('3. 看板视图: URL view=kanban + 3 列分组', async ({ page }) => {
    await page.goto('/inspection/projects')
    await page.locator('.view-tab:has-text("看板")').click()
    await expect(page).toHaveURL(/view=kanban/, { timeout: 5000 })
    // 3 个 bucket 标题
    await expect(page.locator('.bucket-name:has-text("需要我处理")')).toBeVisible()
    await expect(page.locator('.bucket-name:has-text("监控中")')).toBeVisible()
    await expect(page.locator('.bucket-name:has-text("已闭环")')).toBeVisible()
  })

  test('4. 时间轴视图: URL view=timeline&scale=auto + 缩放控件', async ({ page }) => {
    await page.goto('/inspection/projects')
    await page.locator('.view-tab:has-text("时间轴")').click()
    await expect(page).toHaveURL(/view=timeline/, { timeout: 5000 })
    await expect(page).toHaveURL(/scale=auto/)
    // 缩放控件
    await expect(page.locator('.chip:has-text("自适应")')).toBeVisible()
    await expect(page.locator('.chip:has-text("本月")').nth(1)).toBeVisible()
    await expect(page.locator('.chip:has-text("本季")')).toBeVisible()
    await expect(page.locator('.chip:has-text("本年")')).toBeVisible()
  })

  test('5. KPI 进行中 按钮筛选 + URL status=PUBLISHED', async ({ page }) => {
    await page.goto('/inspection/projects')
    await page.locator('.kpi:has(.kpi__label:has-text("进行中"))').click()
    await expect(page).toHaveURL(/status=PUBLISHED/, { timeout: 5000 })
    await expect(page.locator('.kpi.is-active .kpi__label:has-text("进行中")')).toBeVisible()
  })

  test('6. ARCHIVED toggle: URL archived=1 切换', async ({ page }) => {
    await page.goto('/inspection/projects')
    await page.locator('.archive-toggle').click()
    await expect(page).toHaveURL(/archived=1/, { timeout: 5000 })
    await expect(page.locator('.archive-toggle:has-text("显示已归档")')).toBeVisible()
  })

  test('7. 列头排序 (项目): URL sort=name', async ({ page }) => {
    await page.goto('/inspection/projects')
    await page.locator('.row--head .col-name.sortable').click()
    await expect(page).toHaveURL(/sort=name/, { timeout: 5000 })
    await expect(page.locator('.sort-arrow').first()).toBeVisible()
  })

  test('8. 详情页 5 tab 顺序: 总览 > 检查配置 > 人员与任务 > 成绩统计 > 设置', async ({ page }) => {
    // 列表至少有 1 项目可点
    await page.goto('/inspection/projects')
    await page.waitForSelector('.row--data', { timeout: 10000 })
    await page.locator('.row--data').first().click()
    await page.waitForURL(/\/inspection\/projects\/\d+/, { timeout: 10000 })

    // 等 tab 容器渲染
    await page.waitForSelector('.pdv-tab', { timeout: 10000 })
    const tabs = await page.locator('.pdv-tab').allTextContents()
    expect(tabs.length).toBeGreaterThanOrEqual(5)
    expect(tabs[0]).toContain('总览')
    expect(tabs[1]).toContain('检查配置')
    expect(tabs[2]).toContain('人员与任务')
    expect(tabs[3]).toContain('成绩统计')
    expect(tabs[4]).toContain('设置')
  })

  test('9. 详情页 切换到检查配置 tab 渲染内容', async ({ page }) => {
    await page.goto('/inspection/projects')
    await page.waitForSelector('.row--data', { timeout: 10000 })
    await page.locator('.row--data').first().click()
    await page.waitForURL(/\/inspection\/projects\/\d+/, { timeout: 10000 })
    await page.locator('.pdv-tab:has-text("检查配置")').click()
    await expect(page.locator('.pdv-tab.active:has-text("检查配置")')).toBeVisible({ timeout: 3000 })
  })

  test('10. 时间轴 本季 缩放: URL scale=quarter', async ({ page }) => {
    await page.goto('/inspection/projects?view=timeline')
    await page.waitForSelector('.chip:has-text("本季")', { timeout: 5000 })
    await page.locator('.chip:has-text("本季")').click()
    await expect(page).toHaveURL(/scale=quarter/, { timeout: 5000 })
  })
})
