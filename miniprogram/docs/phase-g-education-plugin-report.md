# Phase G Report — education 行业插件接通

> 日期: 2026-05-07
> 分支: `feature/miniprogram-phase-g`
> 状态: ✅ 完成

## 一句话总结

小程序首个**行业垂直插件**接通,验证 plugin enable/disable 在租户切换时工作 — 装了 EDU 的租户看见学生/班级菜单,没装的看不见。

## 范围

✅ **完成:**
- 完整插件骨架 `src/plugins/education/`(对偶 inspection plugin 结构)
- manifest 启用 gate `(ctx) => ctx.tenantPlugins.includes('EDU')` — 后端 `EducationManifest.getIndustryCode()='EDU'` 对齐
- 4 read API:`studentApi.list/byId/byClass` + `classApi.list/byId`
- 4 read-only stub 页:students / student-detail / classes / class-detail
- 4 permissions(student:list/view + class:list/view)
- 2 home-grid menus(学生 / 班级 — order 40/41,排在 inspection 后)
- 4 routes
- 11 测试(manifest sanity + API 单测)

❌ **不在本期(留后续 / 不做):**
- 教师管理页(后端有 TeacherProfileController,但 stub 太多)
- 出勤 / 学籍预警 / 成绩(后端有 AttendanceController/AcademicWarningController,工作量大)
- 写路径(创建/暂停/复学/毕业/转学 — 都是行政操作,小程序不放)
- 13 种"教育插件"行业模型(StudentPlugin/TeacherPlugin/ClassPlugin 等 EntityType plugins) — 这些是后端运行时类型注册,前端只消费查询数据

## Commit

| SHA | 说明 |
|---|---|
| `a20ae638` | Phase G: EDU 行业插件接通 4 read-only 页 |

## 后端契约对照表

| 端点 | 用途 | 关键字段 |
|---|---|---|
| `GET /user_student?keyword=&orgUnitId=&pageNum=&pageSize=` | 学生分页(参数名沿用) | `records: Student[], total, size, current, pages` |
| `GET /user_student/{id}` | 学生详情 | id, studentNo, name, phone, email, enrollmentDate, className, statusText |
| `GET /user_student/by-class/{classId}` | 班级学生列表 | List<Student> |
| `GET /user_student/classes?keyword=&pageNum=&pageSize=` | 班级分页 | records 元素是 SchoolClassResponse |
| `GET /user_student/classes/{id}` | 班级详情 | id, classCode, className, gradeLevel, currentSize, standardSize, headTeacherName, status |

**Plan 偏差(implementer 当场修正):**
- Plan 假设 `PageResult` 用 `pageSize/pageNum` 字段名,实际后端用 `size/current` — types.ts 用 `size?/current?` optional,前端只读 `records/total`,字段差异不影响
- Plan 假设 `SchoolClassDTO`,实际后端是 `SchoolClassResponse`(同 envelope 形状)
- Plan 假设 `classTeacherName/headcount/statusText`,实际是 `headTeacherName/currentSize/status`(`status: String` 枚举如 ACTIVE,无 statusText)

这次 plan 错了 3 处,implementer 全部对齐到真实后端。又一次"先扫真接口再写 plan"的复盘 — 可以接受,但今后写 plan 应该花 5 分钟核对 DTO 真实字段,而不是凭印象写。

## Manifest 变化

`src/plugins/manifests.json` 现在有 **4** 个 plugin:
- demo(自检桩,1 perm + 1 menu + 1 route)
- healthcare(行业示例桩,1+1+1)
- inspection(通用业务,12+5+9)
- **education(行业,4+2+4)— 新增**

`enabled` 函数不在 manifests.json(JSON 不能存函数),只在 manifest.ts 的 runtime 路径生效。manifests.json 给 build-pages-json 脚本消费。

## 测试基线

- 起点:28 文件 / 131 测试(D-3b 末)
- 终点:**31 文件 / 142 测试,0 失败**
- 增量 +3 文件 / +11 测试:
  - `manifest.test.ts` +4(dispatcher 冲突 + count + menu→perm + EDU gate 大小写)
  - `student.test.ts` +4
  - `class.test.ts` +3

type-check + lint 全绿。verify-manifest OK 4 plugins。

## 文件清单

### 新建 (12 文件)
- `plugins/education/manifest.ts` + `manifest.test.ts`
- `plugins/education/api/types.ts` / `student.ts` + `.test.ts` / `class.ts` + `.test.ts`
- `plugins/education/pages/students.vue` / `student-detail.vue` / `classes.vue` / `class-detail.vue`
- `plugins/education/utils/format.ts`

### 修改 (2 文件)
- `plugins/index.ts` — allPlugins 加 education
- `plugins/manifests.json` — 加 4th plugin entry

## 设计决策

### 为什么 EDU 大写

后端 `EducationManifest.getIndustryCode()` 返回字符串 `'EDU'`(大写)。租户启用 plugin 后 `enabledPlugins` 列表里就是这个大写值。manifest.ts 的 enabled gate 用大小写敏感的 `includes('EDU')` 必须精确匹配。

inspection 是 `'inspection'`(小写),healthcare 是 `'HEALTH'`(大写)— 没有统一规范,每个插件按后端 industryCode 来。这是 plan 偏差风险点,这次正好通过 grep 后端 `getIndustryCode` 实现避开。

### 为什么 4 read API + 4 read-only 页 + 没写路径

行业插件的 demo 价值是"看见数据 + 跳转"。学生 / 班级在小程序的写场景(创建 / 转学 / 毕业)都是行政操作,适合 web 端而非小程序。读取场景(老师查学生 / 班主任看本班)才是小程序真用途。这一期只做读。

### 为什么班级详情 inline 学生列表(非另开 sub-page)

backend `byClass` 返回完整列表无分页,小程序班级一般 30-50 人,inline 显示比"班级 → 学生 sub-page"少一层导航。如果未来出现千人班(比如年级)再拆。

### 为什么不动 healthcare / demo

healthcare 是 Phase B 的扩展验证桩,不是真业务。demo 是平台自检桩。两者保留作架构基准对照,不动。Phase G 只新增 education,不重构。

## 已知限制 / 真机验证清单

代码层全绿,但需真机验:

- [ ] 后端启动 + 真账号登录
- [ ] 在 `tenant_plugin_enablement` 表里给当前租户启用 EDU 插件
- [ ] 重新登录小程序 → enabledPlugins 应包含 'EDU'
- [ ] 首页 home-grid 应显示"学生"+"班级"两个新菜单
- [ ] 点学生 → 列表显示,搜索框输入实时过滤(debounced 300ms),"加载更多"分页
- [ ] 点学生行 → 详情显示
- [ ] 点班级 → 列表显示
- [ ] 点班级行 → 详情显示 + 内嵌该班学生列表
- [ ] 把当前租户的 EDU 禁用 → 重新登录 → 学生/班级菜单消失,inspection 等其他菜单正常
- [ ] 没 student:info view 权限的角色登录 → 列表 toast bizMessage(后端 Casbin 拒绝)

## 累计 master 状态

| 阶段 | commits | 测试 | 阶段类型 |
|---|---|---|---|
| A 平台骨架 | 14 | 51 | 框架 |
| B 健康桩 | 3 | 59 | 框架验证 |
| C inspection MVP | 7 | 91 | 通用插件读 |
| D-1 auth 真集成 | 3 | 96 | 核心 |
| D-2 任务/整改文本 | 6 | 104 | 通用插件写 |
| D-3a 拍照水印上传 | 7 | 118 | 通用插件附件 |
| E 核心浏览(我的/组织/通讯录/场所) | 6 | 126 | 核心 |
| D-3b 检查表+申诉 | 3 | 131 | 通用插件 |
| **G education 行业插件** | 1 | **142** | **行业插件** |
| **总计** | **50 commits** | **142 tests** | — |

## 架构里程碑

Phase G 完成后,小程序端**架构 3 层全部覆盖**:

| 层 | 完成度 |
|---|---|
| 1. 核心基础(auth+用户/组织/场所) | ✅ 浏览级 |
| 2. 通用业务插件(inspection) | ✅ 完整闭环(80%) |
| 3. 行业垂直插件(education) | ✅ 浏览级 |

剩下的不是架构层级问题,是模块深度问题(D-3c 订阅消息 / 教育插件出勤+成绩 / 监控等)。

## Next Steps

候选:
- **真机验证** — 累积 7 期未真跑(D-1+D-2+D-3a+E+D-3b+G)
- **D-3c 微信订阅消息** — 后端缺一整套,工作量大
- **education 插件深化** — 出勤 / 成绩 / 学籍预警 / 教师管理
- **healthcare 插件升级** — 把 stub 改真业务接通(对偶 education)
- **Phase F 监控/ADR/作者文档**

我继续推荐**真机验证**。

---

**P.S.** Phase G 单 commit,subagent 一次过,implementer 现场修正了 3 处 plan 字段假设错误(PageResult/SchoolClassResponse/字段名)— 关键是 plan 没核对后端 DTO 真实字段就开写。今后写 plan 前应 grep 一下相关 DTO 类。
