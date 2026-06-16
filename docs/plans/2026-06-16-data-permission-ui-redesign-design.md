# 数据权限配置 UI 重设计（预览驱动 · 默认+例外 · 统一三轴组件）

> 状态：设计稿（2026-06-16，方向已确认）。M1 把数据范围模型重建为可组合三轴后,配置 UI 仍是旧的"场景模板/高级双模式 + 32 行模块墙 + 把三轴控件塞进表格行"。本设计把配置体验整体重做,纯前端/IA 重构,**不动后端**(复用 M1 的三轴 API + 能力声明)。

## 0. 诊断:模型干净了,UI 还是缝补

- **双模式割裂**:场景模板藏住全部新能力;高级是 32 个模块的 flat wall,逐行配心智重。
- **可组合性没被表达**:"我管理的组织 ∩ 排除管理者 ∩ 学生"埋在某一行的二级展开里,看不见、不可比较。
- **预览脱节**:有预览面板但和配置区分离。
- **三轴控件硬塞进表格行**,无统一视觉/交互语言。

## 1. 新心智模型:一条默认范围 + 若干资源例外 + 常驻预览

砍掉"场景模板/高级"切换,collapse 成**一个界面**:

```
┌─ 数据范围 · 租户管理员 ──────────────────────[模板▾] [保存]┐
│  默认范围  (轴①组织锚点, 适用所有资源, 90% 配到这够)        │
│   组织: ○全部 ○仅本人 ●本部门[☑含以下] ○我[关系▾]的组织   │
│        ○指定组织[树选]                                      │
│                                                            │
│  资源例外  (只列与默认不同的)              [+ 添加例外]    │
│   ┌ 用户    我管理的组织 · 排除[管理者] · 仅[学生]   [×] ┐ │
│   └ 检查记录  本部门                                 [×] ┘ │
│   其余 30 资源 → 跟随默认                     [展开全部]   │
├─ 预览 · 此角色实际能看到 ─────────────────────────────────┤
│   用户 → 我管理的组织(B/C)的成员, 排除管理者, 仅学生        │
│   检查记录 → 本部门                                        │
│   其余 → 本部门及以下                                      │
│   [ 模拟用户 ID ___ ▸ 看真实数据 ]                         │
└────────────────────────────────────────────────────────────┘
```

**关键决定(及理由):**
1. **删双模式**。模式切换制造"简单藏能力/高级吓退人"两难。统一成 默认(快)+ 例外(细),progressive disclosure 内建。模板降级为右上角"[模板▾] 一键载入"动作,不再是 tab。
2. **32 行墙 → 默认 + 例外**。多数角色对多数资源同一意图("本部门及以下")。设一次默认套全部,只把"不一样的"作例外卡片。其余折叠,需要时"展开全部"。
3. **默认 = 仅轴①(组织锚点)**;**②③ 只在例外里**。理由:关系过滤/类型过滤是资源特定的(只 user 等成员型/有 type_field 的资源支持),做全局默认无意义。默认管"组织能到哪",例外管"某资源的精细收窄"。
4. **三轴生成器做成一块可复用组件** `ScopeBuilder.vue`,默认范围(只露轴①)和每个例外卡片(露能力允许的轴)共用,视觉语言统一。
5. **预览常驻、配置即所见**,自然语言逐资源 + 模拟用户看真实数据。

## 2. 复用组件 `ScopeBuilder.vue`(统一三轴编辑器)

把 T10 塞在 AdvancedModuleEditor 行内的三轴控件抽成独立组件:

```
props: { spec: ScopeSpecVM, capabilities: {relationFilterable, typeEntity, allowedAnchors}, axisOnlyOrg?: boolean }
emits: update:spec
```
- **轴① 组织锚点**:segmented/radio —— 全部 / 仅本人 / 本部门[含以下] / 我[关系▾]的组织 / 指定组织[树]。关系下拉来自 `relationTypeApi`(toType=ORG_UNIT)。指定组织复用 `CustomScopeTreePicker`。
- **轴② 关系过滤**(仅 `relationFilterable`):chips —— ○不限 ○仅[关系▾] ○排除[关系▾]。关系来自关系字典。
- **轴③ 类型过滤**(仅 `typeEntity`):chips 多选,来自 `entityTypeApi.list(typeEntity)`。
- `axisOnlyOrg=true` 时只渲染轴①(给"默认范围"用)。
- 紧凑风格,复用项目 chip/segmented 语言。

## 3. 加载/保存映射(不动后端)

后端是 per-resource `role_data_scopes` 行。UI 的"默认+例外"在前端合成:

- **加载**:拉该角色所有资源的 spec → 计算"众数 spec"(出现最多的轴① anchor 组合)作为**默认**;与默认不同 或 带②③ 的资源 → **例外卡片**。无配置资源 → 视作默认。
- **保存**:默认范围 expand 成"所有非例外资源"的 SavePermissionCommand(轴①);例外 → 各自完整三轴 command。一次 PUT(复用 `/roles/{id}/data-permissions`,M1 已支持三轴字段)。
- **能力**:每资源 `relationFilterable`/`typeEntity`/`allowedScopes` 来自模块列表(M1 已暴露)。例外的 ScopeBuilder 按该资源能力显隐 ②③。
- **预设**:仍发 scopeCode(向后兼容)+ 三轴(M1 saveRolePermission 优先三轴)。

## 4. 预览 / 模拟

- 自然语言逐资源渲染(复用 T10 PreviewPanel 的 `composeAxisLine` 逻辑):默认行 + 每个例外行 + "其余→默认"。
- 模拟用户(已有入口):输入用户 ID,后端按该角色范围返回其可见数据计数/样本(预览驱动验证)。

## 5. 视觉语言

- 紧凑,复用 PageHeader/StatBar/紧凑卡片(memory: feedback_compact_ui_components);chips 表达过滤;segmented 表达锚点;能力门控清晰;无彩色图标块。
- 关系/类型 label 全来自 API(无行业字面量,守 no-industry-vocab)。

## 6. 分阶段落地

- **P1** 抽 `ScopeBuilder.vue`(从 T10 AdvancedModuleEditor 行内三轴提取,能力驱动,axisOnlyOrg 选项)。组件级测试 + type-check。
- **P2** 新统一视图 `DataScopeStudio.vue`:默认范围(ScopeBuilder axisOnlyOrg)+ 资源例外(列表+添加,每个例外用 ScopeBuilder)+ 预览。
- **P3** 加载推断(众数→默认 + 例外)/ 保存展开(默认→全资源 + 例外覆盖)逻辑 + 单测(纯函数,好测)。
- **P4** 模板降级为"载入预设"动作;预览/模拟接好。
- **P5** 切换 PermissionConfigurator/AccessConsoleView 数据权限 tab 到新视图;退役 SceneTemplatePanel + 老 AdvancedModuleEditor(或保留为"展开全部"的底层)。type-check + no-industry-vocab + 浏览器 E2E。

## 7. 风险/边界

- **默认推断的歧义**:众数 spec 不唯一时,取 level 最高(最宽)或 SELF 兜底;"展开全部"给 power user 逐资源真相。保存→加载需 round-trip 稳定(同一配置存读不漂移)——P3 重点测。
- **纯 UI 重构,零后端/数据风险**;M1 已合 master 做基线。
- M2(读/写分离 UI)未来在"默认/例外"各挂读写两份即可扩展,本 IA 不返工。
