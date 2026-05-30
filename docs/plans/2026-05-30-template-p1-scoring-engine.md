# 模板全实现 P1 — 评分引擎实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans / superpowers:subagent-driven-development to implement task-by-task.

**Goal:** 让全部 13 种评分模式在后端真实算分,根治"配了 ScoringProfile 反而算坏/恒 0"的两条路径矛盾,使每项得分由**唯一的服务端权威计算器**决定。

**Architecture:** 新建领域服务 `ItemScoreEvaluator`,输入 `(ScoringMode, responseValue, scoringConfig)` 输出该项原始分,在一处实现全部 13 模式的算法(加减/通过失败/等级查表/量表比例/累计/多维加权/风险矩阵/阈值/公式)。复杂模式(WEIGHTED_MULTI/RISK_MATRIX/THRESHOLD)复用既有 `SeverityNormalizer` 家族的归一值 × maxScore。提交/重算时由后端调用它写 `detail.score`(不信任前端传的 score)。评分引擎 `ScoreCalculationDomainService` 改为**以 `detail.score` 为该项原始分**做维度聚合/加权/归一化/规则链/等级映射——退役 `mapScoringMode`+`RESPONSE_MAPPED` 的错误重算路径,两条路径(有/无 ScoringProfile)从此都消费同一个权威 detail.score。

**Tech Stack:** Spring Boot 3.2 · 领域服务 · Jackson JsonNode 解析 scoringConfig · 既有 `FormulaEvaluator`/`SeverityNormalizer` · JUnit5。

---

## 现状根因(实测)

- `ScoreCalculationDomainService.calculateItemScore`(:121-151)switch 只认 DEDUCTION/ADDITION/FIXED/RESPONSE_MAPPED。
- `ScoreAggregationService.mapScoringMode`(:574-588)把 8 种非加减模式一锅塞 `RESPONSE_MAPPED`;`RESPONSE_MAPPED` 只 `new BigDecimal(responseValue)` —— 等级/通过标签数值化失败 → rawScore 恒 0;量表把星数当原始分。
- 两条路径:有 ScoringProfile 走引擎(忽略 `detail.score` 重算→错);无 ScoringProfile 走 `sum += detail.score`(:209-211)(对)。**矛盾**。
- **前端契约(已实测)**:`TaskExecutionView` 每种模式都持久化 `responseValue`(原值:'PASS'/等级 label/星数/数字)+ `score`(已算好)。scoringConfig 字段:`passScore/failScore`、`levels[].{label,score}`、`maxStars`/`maxRating`+`maxScore`、`scorePerUnit`、`minScore/maxScore`。
- 复杂模式数学已存在于 `domain/inspection/correction/` 的 `SeverityNormalizer`/`RiskMatrixNormalizer`/`ComplexNormalizer`(产 [0,1] severity),但只服务整改,从未接回得分。

---

## 设计决定:服务端权威 detail.score

`ItemScoreEvaluator.scoreItem(mode, responseValue, scoringConfigJson)` 是**唯一**计算每项分的地方:
- 提交/改答案时(`InspSubmissionApplicationService`),后端用它算 `detail.score`,**忽略前端传来的 score**(前端 score 降级为预览)。
- 评分引擎不再自己按 mode 重算每项分,而是把 `detail.score` 当该项 rawScore,只做聚合/加权/归一化/规则链/等级。
- 无 ScoringProfile 路径已是 `sum += detail.score`,天然一致。

> 注:复杂模式(RISK_MATRIX/WEIGHTED_MULTI/THRESHOLD)的执行端录入控件在 P2 实现;P1 先把**算法**就位 + 单测覆盖,使"只要 responseValue 按约定格式给到就能算对"。P1 不依赖 P2。

---

## Task 1: ItemScoreEvaluator 骨架 + 加减/固定/累计模式

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/inspection/service/ItemScoreEvaluator.java`
- Test: `backend/src/test/java/com/school/management/domain/inspection/service/ItemScoreEvaluatorTest.java`

**接口:**
```java
@Service
public class ItemScoreEvaluator {
    private final ObjectMapper om = new ObjectMapper();
    /** @param mode ScoringMode; @param responseValue 检查员原始响应; @param scoringConfigJson item 的 scoringConfig JSON(可空)
        @return 该项原始分(扣分为负). 无法判定时返回 0. */
    public BigDecimal scoreItem(ScoringMode mode, String responseValue, String scoringConfigJson) { ... }
}
```

**Step 1: 写失败测试**(DEDUCTION/ADDITION/CUMULATIVE/DIRECT)
```java
// DEDUCTION: cfg.score=2, responseValue="3"(数量) → -6
assertThat(ev.scoreItem(ScoringMode.DEDUCTION, "3", "{\"score\":\"2\"}")).isEqualByComparingTo("-6");
// ADDITION: cfg.score=2, qty=2 → +4
assertThat(ev.scoreItem(ScoringMode.ADDITION, "2", "{\"score\":\"2\"}")).isEqualByComparingTo("4");
// CUMULATIVE: scorePerUnit=1, count=5 → 5 (累计加分)
assertThat(ev.scoreItem(ScoringMode.CUMULATIVE, "5", "{\"scorePerUnit\":\"1\"}")).isEqualByComparingTo("5");
// DIRECT: responseValue 即分
assertThat(ev.scoreItem(ScoringMode.DIRECT, "88", "{}")).isEqualByComparingTo("88");
```
> 数量解析:DEDUCTION/ADDITION/CUMULATIVE 的 responseValue 是数量/次数;configScore 从 cfg.score/configScore/baseScore 取(复用 `ScoreAggregationService.parseConfigScore` 的字段约定)。DEDUCTION 取负。

**Step 2-4:** 跑失败 → 实现这 4 个 case(switch on ScoringMode)→ 跑通过。
命令:`cd backend; $env:JAVA_HOME="C:\Program Files\Java\jdk-17"; $env:PATH="C:\Program Files\Java\jdk-17\bin;C:\Program Files\apache-maven-3.9.11\bin;$env:PATH"; mvn test "-Dtest=ItemScoreEvaluatorTest"`

**Step 5: Commit** `feat(inspection): ItemScoreEvaluator 骨架 + 加减/固定/累计模式`

---

## Task 2: PASS_FAIL / LEVEL / SCORE_TABLE / TIERED_DEDUCTION(查表类)

**Files:** 同 Task 1。

**契约(前端实测):**
- PASS_FAIL: responseValue ∈ {"PASS","FAIL"};分 = PASS→`cfg.passScore`(默认 0),FAIL→`cfg.failScore`(默认 -5)。空响应→0。
- LEVEL / SCORE_TABLE: responseValue = 等级 label;在 `cfg.levels`(数组,元素 `{label, score}`;SCORE_TABLE 可能叫 `cfg.table`)里按 label 查 score。查不到→0。
- TIERED_DEDUCTION: 同查表,responseValue=档位 label → cfg.levels/tiers 里的 score(扣分,通常已是负数;若配置为正则取负——按 cfg 实际符号,先读 ItemEditor 确认 tiers 存正还是负)。

**Step 1: 写失败测试**
```java
assertThat(ev.scoreItem(PASS_FAIL, "FAIL", "{\"passScore\":\"0\",\"failScore\":\"-5\"}")).isEqualByComparingTo("-5");
assertThat(ev.scoreItem(PASS_FAIL, "PASS", "{\"passScore\":\"2\"}")).isEqualByComparingTo("2");
assertThat(ev.scoreItem(LEVEL, "良", "{\"levels\":[{\"label\":\"优\",\"score\":\"10\"},{\"label\":\"良\",\"score\":\"8\"}]}")).isEqualByComparingTo("8");
assertThat(ev.scoreItem(LEVEL, "不存在", "{\"levels\":[]}")).isEqualByComparingTo("0");
```
> 先 Read `frontend/src/views/inspection/templates/components/ItemEditor.vue` 确认 LEVEL/SCORE_TABLE/TIERED 配置里等级数组的真实字段名(`levels`? `table`? `tiers`? 元素 `label`/`value`/`score`),按真实字段实现,别假设。

**Step 2-5:** 失败 → 实现 → 通过 → commit `feat(inspection): ItemScoreEvaluator 查表类模式(PASS_FAIL/LEVEL/SCORE_TABLE/TIERED)`

---

## Task 3: RATING_SCALE(量表比例换算)

**契约:** responseValue=星数;分 = `round(stars / maxStars * maxScore)`;maxStars=`cfg.maxStars ?? cfg.maxRating ?? 5`,maxScore=`cfg.maxScore ?? 100`。

**Step 1: 失败测试**
```java
// 4 星 / 5 星 * 100 = 80
assertThat(ev.scoreItem(RATING_SCALE, "4", "{\"maxStars\":\"5\",\"maxScore\":\"100\"}")).isEqualByComparingTo("80");
```
**Step 2-5:** 实现(BigDecimal,HALF_UP)→ 通过 → commit。

---

## Task 4: 复杂模式复用 SeverityNormalizer(WEIGHTED_MULTI / RISK_MATRIX / THRESHOLD)

**Files:** 同上 + 读 `domain/inspection/correction/SeverityNormalizer.java` 及 `RiskMatrixNormalizer`/`ComplexNormalizer`。

**设计:** 这些 normalizer 已能把 responseValue + cfg 算成 [0,1] severity。得分 = `severity × maxScore`(或按业务:风险越高扣越多 → 视 cfg 语义可能是 `-(severity × maxDeduct)`)。**先读 normalizer 的 normalize 签名 + 一个 ComplexNormalizerTest 看输入格式**(RISK_MATRIX responseValue 形如 "概率idx,影响idx";矩阵 + levelToSeverity 在 cfg)。

**Step 1: 失败测试**(参考 ComplexNormalizerTest 的 CFG)
```java
// RISK_MATRIX "3,3" + 矩阵 → VH → severity 1.0; 若 cfg.maxScore=10 且语义为扣分 → -10 (按你确认的语义)
```
> ⚠️ 决策点:这三种是"得分"还是"扣分"语义?读 cfg 是否有 maxScore/maxDeduct 字段 + ItemEditor 怎么描述。**若语义不明,在返回里标出,让 orchestrator 定**,别硬编死方向。

**Step 2-5:** 复用 normalizer 算 severity → × 分值 → 通过 → commit。

---

## Task 5: FORMULA(公式引擎)

**Files:** 同上 + 复用 `domain/inspection/service/FormulaEvaluator.java`。

**契约:** cfg.formula = JS/表达式;变量至少 `responseValue`(数值)。先读 FormulaEvaluator.evaluate 签名 + 它支持的变量。

**Step 1: 失败测试**
```java
// formula = "value * 2", responseValue=5 → 10 (变量名以 FormulaEvaluator 实际支持的为准)
```
**Step 2-5:** 实现(解析 cfg.formula,装变量,evaluate)→ 通过 → commit。

---

## Task 6: 提交路径改为服务端权威 detail.score

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/inspection/InspSubmissionApplicationService.java`(completeSubmission / updateDetailResponse 等写 detail.score 处)
- 注入 `ItemScoreEvaluator`

**Step 1:** grep 找 detail.score 被设置/接收的所有写入点(updateDetailResponse、completeSubmission)。
**Step 2:** 改为:保存 detail 时 `detail.score = itemScoreEvaluator.scoreItem(mode, responseValue, scoringConfig)`,**忽略前端传入的 score**(前端 score 仅预览)。
**Step 3:** 写集成/单测:提交一个 LEVEL=良 的 detail,断言落库 detail.score = 等级表里的分(不等于前端可能传的任意值)。
**Step 4-5:** 编译 + 测试 + commit `feat(inspection): 提交时服务端权威计算 detail.score`

---

## Task 7: 评分引擎改为消费 detail.score(退役错误重算)

**Files:**
- Modify: `ScoreCalculationDomainService.calculateItemScore`(:121-176)
- Modify: `ScoreAggregationService.buildItemScoreInputs`(:404+)+ 退役/简化 `mapScoringMode`
- Test: `ScoreCalculationDomainServiceTest` / `ScoreAggregationServiceNormalizationTest`

**设计:** `ItemScoreInput` 增加 `itemScore`(来自 detail.score);`calculateItemScore` 直接用 `itemScore` 当 rawScore(仍保留归一化乘子逻辑),不再 switch on scoringMode 重算。`mapScoringMode`/`RESPONSE_MAPPED` 重算逻辑删除或废弃。归一化/维度聚合/规则链/等级**不变**。

**Step 1:** 写失败测试:有 ScoringProfile 时,LEVEL=良(detail.score=8)的项,引擎 finalScore 反映 8(而非旧逻辑的 0)。
**Step 2-3:** 改 ItemScoreInput + calculateItemScore + buildItemScoreInputs。
**Step 4:** 跑 `ScoreCalculationDomainServiceTest,ScoreAggregationServiceNormalizationTest,ScoreAggregationServiceResolveTest` 全绿(修因签名变化而破的既有测试)。
**Step 5:** commit `feat(inspection): 评分引擎以 detail.score 为项原始分, 退役 mapScoringMode 错误重算`

---

## Task 8: 端到端验证 + 真启动

**Step 1:** `mvn -q -DskipTests package` BUILD SUCCESS。
**Step 2:** 全 inspection 测试套件绿:`mvn test "-Dtest=com.school.management.application.inspection.*,com.school.management.domain.inspection.**"`
**Step 3:** orchestrator 真启动后端(spring-boot:run 后台 + 看 Started + kill)——验证无 bean 环、ItemScoreEvaluator 注入成功。
**Step 4:** commit(若有收尾)。

---

## 验证基线(P1 完成)
- [ ] 13 种模式各有 ItemScoreEvaluator 单测且绿
- [ ] 提交 LEVEL/PASS_FAIL/RATING 落库 detail.score 正确(服务端权威)
- [ ] 有 ScoringProfile 时这些模式 finalScore 不再恒 0(两路径一致)
- [ ] 全 inspection 测试套件绿 + 真启动绿
- [ ] mapScoringMode 错误重算路径已退役

## 明确不做(P1 范围外,留后续 Phase)
- 执行端录入控件(P2):复杂模式的 UI 录入
- 条件逻辑/事件流/校验(P3)、库目录页(P4)、编辑器收尾(P5)
- ⚠️ Task 4 复杂模式"得分 vs 扣分"语义未定者,实现时标出待 orchestrator 拍
