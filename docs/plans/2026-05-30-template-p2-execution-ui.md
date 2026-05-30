# 模板全实现 P2 — 执行端录入 UI 实现计划

> **For Claude:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development / executing-plans。

**Goal:** 让全部 13 种评分模式 + 全部采集类型在检查执行页(`TaskExecutionView`)都能真渲染控件、采集值、保存,且保存格式与 P1 后端 `ItemScoreEvaluator` 的算分契约对齐。

**Architecture:** 方案 A——把已写好的孤儿组件 `ScoringItemRow.vue`(13 模式全控件,含复杂模式 multiInputs 多维值)接进 `TaskExecutionView` 替换内联 switch;采集类型借用 `FormItemRenderer.vue` 的控件(签名板/GPS/日期/多选/上传)。保留执行页外层 section/target 导航、键盘、EVENT_STREAM、VIOLATION_RECORD、PERSON_SCORE 框架。复杂多维值存 `SubmissionDetail.dimensions`(JSON,保存契约已支持),媒体存 url 进 responseValue + `addEvidence` 双写。

**Tech Stack:** Vue3 + TS + Element Plus · 复用 `api/upload.ts`/`file.ts`/`addEvidence`/`signature_pad`/`useGeolocation`。

**前置:** P1 完成(ItemScoreEvaluator 后端权威算分)。**关键契约**:P2 各模式 handler 存进 `responseValue`/`dimensions` 的格式,必须是 P1 evaluator `scoreItem` 能解析的(见各 task)。

---

## 现状(实测,来自架构测绘)
- `TaskExecutionView.vue` 渲染按 scoringMode(:1549-1685):7+1 模式有控件;**WEIGHTED_MULTI/RISK_MATRIX/THRESHOLD/FORMULA 完全无控件(空白卡片)**;18 采集类型全落一个 `el-input` 文本框(:1651)。
- 保存通道统一:`persistDetailResponse(detail,{responseValue,scoringMode,score,dimensions?})` → `store.updateDetailResponse` → PUT。干净,易接。
- 孤儿 `ScoringItemRow.vue`:13 模式控件全有(复杂模式用 `multiInputs: Record<id,Record<string,number>>`)。
- 孤儿 `FormItemRenderer.vue`:22 类型全有,含 signature_pad 签名 / useGeolocation GPS / el-upload 媒体。
- `SubmissionDetail.dimensions`(JSON)字段就绪且在 `UpdateDetailResponseRequest` 保存契约里——复杂多维值的去处。
- **已知 bug**:`<PersonScoreGrid>`(:1674)没接 `@update:scores` → 逐人评分静默不保存。

---

## ✅ 已决策(2026-05-30 用户按推荐确认 "按照你的建议")
1. **复杂模式 severity→分 方向**:
   - RISK_MATRIX = **扣分**(风险越高扣越多,P1 默认即此,无需改)
   - THRESHOLD = **扣分**(越界扣分)
   - WEIGHTED_MULTI = **得分**(多维加权合成一个正分,**与 P1 默认扣分语义相反 → P2 接通时需把 WEIGHTED_MULTI 的换算改为 `severity×maxScore` 或 `(1-severity)×maxScore` 得正分,并相应调 P1 evaluator 的 `complex()` 分支**)
   - FORMULA = 按公式结果(可正可负)
2. **FORMULA 契约**:**模板侧(ItemEditor)补一个"公式表达式"字段** `cfg.formula`,与 P1 evaluator 读 `cfg.formula` 对齐(而非改 evaluator 读 formulaType)。
3. **THRESHOLD cfg**:用 `thresholds:[{upTo,score}]`,P2 补 ItemEditor 编辑器对齐。
4. **CUMULATIVE 字段名**:统一成 **`scorePerUnit`**(改 ItemEditor 的 `scorePerCount` 写出端 → `scorePerUnit`;evaluator 已两者容错)。

> 注:WEIGHTED_MULTI 得分语义意味着 P2 不是纯前端活——需回 P1 的 `ItemScoreEvaluator.complex()` 把 WEIGHTED_MULTI 单独走得分换算(其余复杂模式维持扣分)。这点在 P2 Task 1 一并处理。

---

## Task 1: 复杂模式控件 + 多维值保存(最大黑洞)
**Files:** Modify `TaskExecutionView.vue`(引入复杂模式分支或接 `ScoringItemRow`);确认 `responseValue`/`dimensions` 写入格式匹配 P1 `ItemScoreEvaluator`(读 `RiskMatrixNormalizer`/`WeightedMultiNormalizer` 确认输入格式:RISK_MATRIX responseValue="概率idx,影响idx";WEIGHTED_MULTI 多维)。

- 为 WEIGHTED_MULTI/RISK_MATRIX/THRESHOLD/FORMULA 加录入控件(从 ScoringItemRow 取)。
- 新增 `multiInputs` 状态 + `handleWeightedMulti/RiskMatrix/Threshold/Formula`,把多维值序列化进 `responseValue`(P1 evaluator 能解析的格式)+ 可选 `dimensions`,`score` 留空(P1 后端权威算,前端不必算)。
- **验证**:配一个 RISK_MATRIX 项 → 执行页能选概率×影响 → 保存 → 后端 evaluator 算出分(端到端,需 P1 已接通)。
- type-check 0。Commit。

## Task 2: 采集类型专用控件(替换文本框兜底)
**Files:** Modify `TaskExecutionView.vue`,借 `FormItemRenderer` 控件。
- DATE/TIME/DATETIME → el-date-picker/time-picker;SELECT/RADIO → el-select/radio(选项来自 responseSet 或 cfg);MULTI_SELECT/CHECKBOX → 多选,`responseValue=JSON.stringify(array)`;NUMBER → el-input-number;TEXT/TEXTAREA → 对应。
- 按 itemType 在采集兜底分支前加细分(保留文本框作未知类型 fallback)。
- type-check 0。Commit。

## Task 3: 媒体上传 PHOTO/VIDEO/FILE_UPLOAD
**Files:** Modify `TaskExecutionView.vue`;复用 `api/upload.ts`/`file.ts` + `addEvidence`。
- el-upload 控件,上传得 url → 存 responseValue + `addEvidence(submissionId,{...})` 双写(参考 ScoringItemRow:576)。
- type-check 0。Commit。

## Task 4: SIGNATURE 签名 + GPS 定位
**Files:** Modify `TaskExecutionView.vue`;复用 `FormItemRenderer` 的 signature_pad(:99-161)+ useGeolocation(:170)。
- SIGNATURE:弹签名板→导出 png→上传→存 url。GPS:取坐标→`responseValue="lat,lng"`。
- BARCODE:保留文本兜底,标注"扫码移动端支持"(P2 不做,唯一无桌面能力)。
- type-check 0。Commit。

## Task 5: 修 PERSON_SCORE 持久化 bug
**Files:** `TaskExecutionView.vue:1674` `<PersonScoreGrid>` 补 `@update:scores` 监听 → 序列化逐人分进 `dimensions`(或专用接口)→ persistDetailResponse。
- 写测试/手验:逐人评分能存能回显。Commit。

## Task 6: 端到端验证
- `npm run type-check` 0 · `npm run build` 绿(bundle 闸)。
- orchestrator 真启动 + 浏览器手验:每种模式/类型能填能存能回显(抽样)。
- 顺带验证 P1 端到端:复杂模式填完后端真算分。

---

## 验证基线(P2 完成)
- [ ] 13 模式执行页都有真控件(复杂模式不再空白)
- [ ] 采集类型有专属控件(日期/选择/多选/数字/媒体/签名/GPS),非裸文本框
- [ ] 复杂模式保存格式与 P1 evaluator 对齐,端到端真算分
- [ ] PERSON_SCORE 持久化 bug 已修
- [ ] 孤儿组件 ScoringItemRow/FormItemRenderer 已接入(消灭死代码)或明确复用
- [ ] type-check 0 + build 绿 + 真启动手验

## 明确不做
- BARCODE 桌面扫码(无能力,文本兜底 + 标注)
- 移动端执行页(TaskExecutionMobileView 仍只读)——P2 只做桌面执行页
- 条件逻辑/事件流/校验(P3)
