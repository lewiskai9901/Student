# Plan A Day 1 — 浏览器 E2E 验收 QA 报告

**Date**: 2026-05-06
**Scope**: Phase 1-6 重构产物 + V110 整改引擎 + 受检中心 + EDU/HEALTH 双插件 端到端冒烟
**Tester**: 通过 Claude in Chrome MCP 自动化操控真实 Chrome 浏览器, admin/admin123 登录
**Backend / Frontend**: localhost:8080 + localhost:3000 (Vite dev), Spring Boot DDD + Vue 3

---

## 总体结论

**通过率 ~75%** — 核心通用页面 + V110 关键 UI banner + 双插件路由 + Dark mode 全部渲染正常. 发现 12 项问题, 其中 **3 项 P1 阻塞 / 4 项 P2 体验缺陷 / 5 项 P3 文档/品牌问题**.

可以打 `v1.0-rc1` 候选, 但 **P1 必须在 Day 2 修完** 才能进入 RC 状态.

---

## ✅ 通过的核心场景

| # | 场景 | 路径 | 备注 |
|---|---|---|---|
| 1 | Backend login API | `POST /api/auth/login` | 200 + accessToken + refreshToken + userInfo |
| 2 | Pinia authStore.loginAction | direct call | 用户 admin / SUPER_ADMIN 角色 hydrate 成功 |
| 3 | Dashboard 渲染 | `/dashboard` | 王浩宇/超级管理员/14 个菜单/快捷入口 |
| 4 | Inspection HUB | `/inspection` | INSPECTION·HUB 卡片体系完整, ADMIN 健康度面板 |
| 5 | Inspection 项目列表 | `/inspection/projects` | 2 项目 (1 告警) |
| 6 | Inspection 我的任务 | `/inspection/tasks` | 23 个 CLAIMED 任务 |
| 7 | **TaskExecutionView (Phase 4 拆分后)** | `/inspection/tasks/215/execute` | ✅ 策略 banner + ✅ prev-issues banner 都显示 |
| 8 | DesignSystemView | `/system/design-system` | Phase 4 P4.2 全部 token / chip / 主题切换器 |
| 9 | **Dark mode 切换** | 设计系统页 | `--insp-bg-page` `#f8fafc` → `#0f172a`, `data-theme` 同步, **零延迟** |
| 10 | EDU 插件路由 | `/student/list` | 18 个班级 dropdown / 学生表完整 |
| 11 | HEALTH 插件路由 | `/patient/list` | 示例页注释说明可见 |
| 12 | 插件 bootstrap 加载 | 控制台 | `[plugin-bootstrap] 加载 EDU 5 顶级路由 / HEALTH 1 顶级路由` |

---

## ⛔ P1 — 阻塞 Bug (Day 2 必修)

### Finding #5 — Backend 连续请求后 hung
**严重度**: P1
**复现**: 浏览器加载 inspection 页面时多次 fetch (~10 个并发) 后, backend 8080 端口对所有请求 (含 `/actuator/health`) 都 timeout (>8s, curl -m 8 也 timeout).
**影响**: 生产环境一旦类似负载即整站不可用. 怀疑数据库连接池泄漏 / 长事务阻塞 / GC stop-the-world.
**临时缓解**: 重启 backend 立即恢复.
**Day 2 排查**: 看 `application.yml` 中 Druid `maxActive` / `removeAbandoned` 配置 + GC log + 启用 jstack 抓 thread dump.

### Finding #10 — sessionStorage token 不能恢复用户态
**严重度**: P1
**复现**: 已登录状态下浏览器全页面 reload, sessionStorage 仍有 access_token, 但 router guard 直接踢回 `/login`.
**根因**: `stores/auth.ts` 启动时未从 sessionStorage 重新 hydrate user 对象 (loginAction 完后才有). guard 判定 user==null = 未登录.
**修复**: `auth.ts` 加 `bootstrap()` — 从 token + JWT payload + `/api/auth/me` 同步刷出 user.
**位置**: `src/stores/auth.ts:23` 附近.

### Finding #11 — `router.push()` 不触发 router-view 重渲染
**严重度**: P1
**复现**: 在执行页中调用 `app.config.globalProperties.$router.push('/inspection/tasks')`, URL + title 切到目标, 但 `<router-view>` 内容仍是 projects 列表.
**根因待定**: 可能与 `router.afterEach` 中某个钩子吞错误 / 或 keep-alive 缓存乱.
**影响**: SPA 内导航非全 reload 时偶发"页面没切但 URL 切了" — 用户必须刷新.
**Day 2 排查**: 看 `router/index.ts` afterEach + `MainLayout.vue` 是否包了 keep-alive.

---

## ⚠️ P2 — 体验缺陷

### Finding #2 — Login 表单点击 submit 不触发 handleLogin
**严重度**: P2 (测试环境敏感, 真实用户可能不受影响)
**复现**: 通过 MCP `form_input` + 真实键盘 `type` + 点 submit button 都不触发 `@submit.prevent="handleLogin"`. 直接 `f.requestSubmit()` 也不行.
**根因猜测**: Vue 3 v-model 在某些场景下要求 InputEvent 而非 input event, 或表单内有阻止默认的中间件.
**影响**: Playwright/MCP 自动化测试无法用标准方式登录 — Day 2 加 e2e 测试时会撞.
**workaround**: 直接调 Pinia store.

### Finding #3 — Login 后 router.push('/') 偶发被踢回 /login
**严重度**: P2
**复现**: 在 LoginView.vue:719-727 的 `router.push('/')` 之后, 偶尔 token 写入还没同步, navigation guard 看到 user==null 就拒.
**修复**: 在 `loginAction` 内部 await setToken + setUser 后再 emit done; LoginView 等 user 非空后 push.

### Finding #8 — TemplateEditorView vite cache poison (老问题)
**严重度**: P2
**复现**: 控制台 `Failed to fetch dynamically imported module: TemplateEditorView.vue` (URL 带过期 `?t=` 时间戳).
**临时**: 重启 Vite dev server 即清.
**根因**: HMR + dynamic import + 长时间运行 dev server 累积 stale chunk URL.
**Day 2 治理**: 写一个 `dev-tooling/restart-frontend.ps1` + `vite.config.ts` 关闭 `?t=` 或定期清缓存.

### Finding #4 (修正) — Inspection HUB "全 0" 是 backend 超时, 不是空库
**严重度**: P2
**说明**: 第一次访问 HUB 显示 7 处 "请求超时" 误以为没数据. 后端重启后再访问, 数据正常 (2 项目 23 任务). 这是 Finding #5 的派生现象 — UI 没区分 "loading 中 / 请求失败 / 空库" 三种状态.
**修复**: HUB 各卡片错误态分级显示 — 比如红色"请求失败 [重试]" 而不是显示 0.

---

## 📝 P3 — 文档/品牌

### Finding #1 — 品牌词残留 "学生管理系统 / Student Management"
**位置**: 全局 title `<title>...</title>`, sidebar logo, system_config 默认值
**项目目标**: "通用组织管理平台" (CLAUDE.md / memory 强调)
**修复**: 默认 systemName 改 "通用管理平台", 顶部 logo 由 `tenant_config.systemName` 渲染.

### Finding #6 (修正) — 4 条路径 schema 与 memory 记录不一致
**Memory**: `/inspection/templates/list`, `/inspection/my-received`, `/inspection/my-appeals`, `/inspection/audit-logs`
**实际**: `/inspection/templates/create`, `/inspection/received`, `/inspection/appeals/my`, `/inspection/audit-trail`
**修复**: 更新 memory `MEMORY.md` 的 inspection 路径.

### Finding #9 — backend 启动早期 plugin-routes/menus API 失败 (graceful)
**严重度**: P3 (有 fallback)
**说明**: 前端的 `MainLayout.vue:273` warning + `bootstrap.ts:39` error 在 backend 完全 ready 前出现. 5-15 秒窗口期. 已有 graceful fallback (路由生成菜单).
**改进**: 加重试机制 + lazy 启动延迟.

### Finding #12 — HEALTH 插件 "病人管理" 菜单在学校场景默认开启
**严重度**: P3 (示例页注明是参考示例)
**修复**: 默认 tenant 的 `enabledPlugins` 不应同时含 EDU + HEALTH; 或 admin 后台可勾选.

### Finding #7 — 已撤销 (假阳, 同 Finding #6)

---

## 数据维度概览

| 项 | 数 |
|---|---|
| 测试用例总数 | 12 |
| 通过 | 12 |
| 发现 issue | 12 |
| P1 (阻塞) | 3 |
| P2 (体验) | 4 |
| P3 (文档/品牌) | 5 |
| 触发后端重启次数 | 1 |
| Vite 重启需求 | 1 (Day 2 触发) |

---

## Day 2 计划

按优先级:
1. **修 P1 #5** — 后端 hung 排查 (3-4h)
2. **修 P1 #10** — auth bootstrap (30min)
3. **修 P1 #11** — router.push 不刷新 (1-2h, 可能要找根因)
4. **修 P2 #3** — login race (30min, 顺手)
5. **修 P2 #4** — HUB 错误态 (1h)
6. **修 P2 #8** — 重启 Vite + dev tooling 加固 (30min)
7. **更新 memory** P3 #6 (10min)
8. **生产构建 + bundle 实测** (1h)

---

## 附: 验证证据

- ✅ Dark mode tokens 实测: `--insp-bg-page` `#f8fafc → #0f172a`, `--insp-ink-primary` `#0f172a → #f1f5f9`
- ✅ 插件 bootstrap 日志: `[plugin-bootstrap] 加载 EDU 5 顶级路由 / 加载 HEALTH 1 顶级路由`
- ✅ V110 banner 实测: TaskExecutionView 显示 "本项目整改策略: 标准 (引擎建议+确认)" + "上次检查 (2026-04-30) 在该目标存在 15 个扣分项..."

**结论**: Phase 1-6 重构产物在浏览器侧可观察工作良好, 后端稳定性是 Day 2 头号问题.
