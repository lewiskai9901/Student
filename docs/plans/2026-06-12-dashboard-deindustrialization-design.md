# Dashboard 去教育侵入 — 设计

日期: 2026-06-12
状态: 已确认 (两个核心决策经用户选定推荐项)
前置: 2026-06-02 核心去教育侵入审计已完成分区级贡献点改造
(`DashboardSectionContributor` + EDU `TeachingDashboardContributor`,
教师工作台迁入插件, `DashboardReadModel`/`OccupantType` 已删)。

## 剩余侵入面 (2026-06-12 核实)

### 后端核心 `application/dashboard/DashboardOverviewQueryService`
1. `getOrgStats` 直查教育表 `majors` + `classes`。守护 `NoIndustryTableInCoreTest`
   没拦的原因: 表名作为参数传入 `countByOrgColumn("classes", ...)` 后字符串拼接,
   文本扫描 `FROM classes` 抓不到; 且 `majors` 不在 FORBIDDEN_TABLES。
2. 教育语义 feature 键 `isLearner`/`canTeach` 写死核心常量。
3. organization 分区输出教育契约键 `majorCount/classCount/studentCount/teacherCount`。

### 前端
1. `views/DashboardView.vue` 硬编码: 组织卡 4 个教育数字、整张 teaching 卡
   (学期/课程/排课率进度条)、快捷按钮 4 个教育入口。
2. `types/dashboard.ts` 契约含教育键。

## 决策

### 决策 1 (后端): EDU 新分区贡献 — 零新 SPI
复用现有 `DashboardSectionContributor`:
- EDU 新增 `EducationDashboardContributor` (`plugins/education/application/dashboard/`),
  `sectionKey="education"`, 产出 majorCount/classCount/studentCount/teacherCount,
  scope 收敛与 TeachingDashboardContributor 同构 (unrestricted/subtree/single/deny)。
- 学生/教师数走核心 `MembershipResolver` 通用 feature 计数 (插件 import 核心合法),
  `isLearner`/`canTeach` 键值移入插件。
- 核心 `getOrgStats` 收缩为仅 `orgUnitCount`; `countByOrgColumn`(唯二调用方是
  majors/classes) 与 `FEATURE_*` 常量删除。核心对教育表/教育 feature 键零引用。
- 否决"统计项级贡献点 (DashboardStatContributor)": 多一个 SPI 维度只换来
  "数字显示在哪张卡"的区别, YAGNI。

### 决策 2 (前端): 组件注册模式 — 与 relationScenes 同构
- 新注册表 (如 `src/views/dashboard/dashboardCards.ts`):
  `DashboardCardDef { pluginCode, sectionKey, component, order }`
  + `registerDashboardCards(code, defs)` / `enabledDashboardCards(codes)`。
- `DashboardView` 单次拉 `/dashboard/overview`; 通用卡 (组织/检查/系统) 保留;
  注册卡按 `overview[sectionKey]` 传 prop, 按 order 渲染。
- teaching 卡整体搬为 EDU 组件; 新增"办学规模"卡 (education 分区 4 数字);
  4 个教育快捷按钮经同一注册表贡献 (shortcuts 字段)。
- 在 `router/plugins/edu.ts` 登记, bootstrap 按 `usePluginsStore().codes` 动态
  import — EDU 禁用时卡与按钮整体消失。
- `types/dashboard.ts` 剥离教育键 (OrgStats 只剩 orgUnitCount; teaching 类型
  随组件移入 edu 归属)。
- 否决"纯数据驱动渲染": teaching 卡的进度条等定制 UI 表达力不足, 会做出
  难看的通用渲染器。

### 守护加固
- `NoIndustryTableInCoreTest`: FORBIDDEN_TABLES += majors; 新增规则禁核心
  (plugins/ 外) 出现教育表名**带引号字符串字面量** — 堵参数化拼接绕过
  (本次漏网的根因)。
- 前端新增 vitest: 核心 `DashboardView.vue` 零教育词汇
  (同 data-permissions/no-industry-vocab.spec 先例)。

## 验证口径
1. 后端单测/架构守护全绿 (守护先 RED 证明能抓现状, 修完 GREEN)。
2. 真启动: `/dashboard/overview` 含 education 分区且数字与改造前一致
   (admin 视角对照); dpt_ct (CLASS_TEACHER, scope 收敛) 视角正常。
3. 前端 type-check + vitest 绿; 浏览器或 API 验卡片渲染。
4. EDU 禁用语义: education/teaching 分区与对应前端卡同时消失 (代码评审确认
   bean 条件与注册表过滤, 不实际禁用插件)。
