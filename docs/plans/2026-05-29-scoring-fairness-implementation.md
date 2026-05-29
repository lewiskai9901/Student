# 评分规模公平性 — 实现计划 (Scoring Fairness Implementation Plan)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让检查平台的评分对"规模"公平 —— 人多/场所多/子组织多的单位不再天然吃亏 —— 同时清掉评分模块里的死代码与概念错位功能。

**Architecture:** 6 层评分流水线。本计划落地其中 3 个未实现/坏掉的层 + 2 项清理：
1. **Stage 1 子项归一化** — 扣分 ÷「该维度构成单位数」，分母由 `NormalizationBasisResolver` SPI 按 `TargetType + section.normalizeBy` 从 组织/用户/场所 三个基础模块实时取。
2. **Stage 2 规则链补全** — `applyRule` switch 漏了 `PENALTY` / `PROGRESSIVE_BONUS`，补上。
3. **Stage 5 组织树汇总** — 新增 `org_unit_scores`，沿 `OrgUnit.tree_path` 自底向上用 **MEAN**（非 SUM）滚动，产出当前完全缺失的"组织级得分"。
4. **清理 A** — 彻底删 `trend / decay / multiRater / calibration` 死字段（含 DB 列）。
5. **清理 B** — 去掉 `60` 及格线硬编码，统一走 `gradeBand`。

**核心原理:** 求和(SUM)是规模不公平的唯一根源；每一层把"绝对求和"换成"按构成单位数取均值/率"，规模自动抵消。Stage 1 治单体内部(人/场所多)，Stage 5 治汇总(子组织多)。

**Tech Stack:** Spring Boot 3.2 + MyBatis Plus + MySQL；DDD 六边形（domain/application/infrastructure/interfaces）；Vue 3 + TS（前端配置）。测试：JUnit5 `@SpringBootTest` + 领域单测；归一化/规则链走纯领域单测（无需 Spring）。

**关键约束（来自项目记忆，必须遵守）:**
- **彻底删除**：删字段必须同步写 DROP COLUMN 迁移 + 改建表 SQL，不留旧列。
- **通用核心**：引擎/resolver 里**禁止** `if (type=="STUDENT")` 之类行业字面量；分母只认 `TargetType` + 三个基础模块的通用字段。已有守护 `NoIndustryTypeLiteralInCoreTest`。
- **migration 条件化**：新迁移写成 `information_schema` 条件式（参考 V97/V104），可重复执行。
- **自动重启**：改后端后自动 kill 旧进程再起，不问用户。
- 验证不能只靠单测 —— MyBatis 拦截器/装配相关改动必须真启动 + 真 INSERT 验证（见 `feedback_unit_test_is_not_enough`）。

---

## 阶段与依赖

```
Phase 0 (规则链补全)   ── 独立，最快，先做
Phase 1 (归一化接通)   ── 依赖 Phase 0 不强；可并行
   ├─ 1A resolver SPI
   ├─ 1B profile 加字段 + 迁移
   └─ 1C 装配接通
Phase 2 (前端 normalizeBy 配置)  ── 依赖 1B
Phase 3 (组织树 roll-up)         ── 依赖 1C（要有正确的 submission 分才有意义）
Phase 4 (删死代码)               ── 独立，可最后做（避免与 1B 改同文件冲突，放 1B 之后）
Phase 5 (去 60 硬编码)           ── 独立
```

每个 Phase 结束都能独立编译 + 启动 + 提交。

---

# Phase 0 — 规则链补全 (PENALTY / PROGRESSIVE_BONUS)

**背景:** `ScoreCalculationDomainService.applyRule()` 的 switch（`ScoreCalculationDomainService.java:256`）只有 `VETO/BONUS/PROGRESSIVE/CUSTOM` 四个 case。`PENALTY` 和 `PROGRESSIVE_BONUS` 落到 `default` 静默 no-op —— 前端能配，后端不算。

### Task 0.1: PENALTY 规则

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/inspection/service/ScoreCalculationDomainService.java:256` (switch 内新增 case)
- Test: `backend/src/test/java/com/school/management/domain/inspection/service/ScoreCalculationDomainServiceTest.java`

**Step 1: 写失败测试**

PENALTY 语义：当指定 `penaltyItems` 中任一项不合格（finalScore != 0）时，按 `penaltyScore` 固定扣分（每项扣或一次性扣 —— 取每命中一项扣一次，与 BONUS 对称）。

```java
@Test
void applyRule_penalty_deductsPerMatchedItem() {
    // profile: max=100 min=0; 一个维度 baseScore=100 权重 100
    // 两个 item 命中 penaltyItems，penaltyScore=5 → 扣 10
    ScoringProfile profile = profileWithBase100();
    List<ScoreDimension> dims = List.of(dim("D1", 100, 100));
    CalculationRule penalty = rule("R_PEN", RuleType.PENALTY, 10,
        "{\"penaltyItems\":[\"I1\",\"I2\"],\"penaltyScore\":5}");
    // I1/I2 不合格(扣分项, finalScore<0)，I3 合格
    List<ItemScoreInput> inputs = List.of(
        deductionItem("I1", "D1", 1), // -1 raw → finalScore<0
        deductionItem("I2", "D1", 1),
        fixedItem("I3", "D1", 0));
    ScoreResult r = service.calculate(profile, dims, List.of(penalty), List.of(), inputs, 0);
    // base 100 - 2(item扣) ... 关注 deduction 由规则贡献的 -10
    assertThat(r.getRuleApplications())
        .anySatisfy(a -> {
            assertThat(a.getRuleType()).isEqualTo(RuleType.PENALTY);
            assertThat(a.isApplied()).isTrue();
            assertThat(a.getAdjustment()).isEqualByComparingTo("-10");
        });
}
```

> 测试辅助方法 `profileWithBase100/dim/rule/deductionItem/fixedItem` 若不存在则在测试类顶部补私有工厂方法。参考该测试类已有用例的构造方式。

**Step 2: 运行确认失败**

Run: `cd backend && mvn test -Dtest=ScoreCalculationDomainServiceTest#applyRule_penalty_deductsPerMatchedItem`
Expected: FAIL —— PENALTY 落到 default，`isApplied()=false`，adjustment=0。

**Step 3: 实现 PENALTY case**

在 `applyRule` switch 中 `case PROGRESSIVE` 之后加：

```java
case PENALTY: {
    JsonNode penaltyItems = config.get("penaltyItems");
    BigDecimal penaltyScore = getDecimal(config, "penaltyScore", BigDecimal.ZERO);
    if (penaltyItems != null && penaltyItems.isArray()) {
        Set<String> set = new HashSet<>();
        penaltyItems.forEach(n -> set.add(n.asText()));
        long count = itemOutputs.stream()
                .filter(o -> set.contains(o.getItemCode())
                        && o.getFinalScore().compareTo(BigDecimal.ZERO) != 0)
                .count();
        if (count > 0) {
            BigDecimal adj = penaltyScore.abs().multiply(BigDecimal.valueOf(count)).negate();
            return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), true, adj,
                    "扣分 x" + count);
        }
    }
    return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), false, BigDecimal.ZERO, null);
}
```

**Step 4: 运行确认通过**

Run: `cd backend && mvn test -Dtest=ScoreCalculationDomainServiceTest#applyRule_penalty_deductsPerMatchedItem`
Expected: PASS

**Step 5: 提交**

```bash
git add backend/src/main/java/.../ScoreCalculationDomainService.java backend/src/test/java/.../ScoreCalculationDomainServiceTest.java
git commit -m "fix(inspection): 补全 PENALTY 规则 — applyRule switch 漏实现导致静默 no-op"
```

### Task 0.2: PROGRESSIVE_BONUS 规则

**Files:** 同上。

**Step 1: 写失败测试** —— 与 PROGRESSIVE 对称，但统计合格项数(finalScore>0)，命中阈值累进**加分**：

```java
@Test
void applyRule_progressiveBonus_addsByThreshold() {
    ScoringProfile profile = profileWithBase100();
    List<ScoreDimension> dims = List.of(dim("D1", 100, 100));
    CalculationRule rule = rule("R_PB", RuleType.PROGRESSIVE_BONUS, 10,
        "{\"thresholds\":[{\"count\":2,\"bonus\":3},{\"count\":4,\"bonus\":6}]}");
    // 4 个合格(加分)项 → 命中 count>=4 → +6
    List<ItemScoreInput> inputs = List.of(
        additionItem("I1","D1",1), additionItem("I2","D1",1),
        additionItem("I3","D1",1), additionItem("I4","D1",1));
    ScoreResult r = service.calculate(profile, dims, List.of(rule), List.of(), inputs, 0);
    assertThat(r.getRuleApplications()).anySatisfy(a -> {
        assertThat(a.getRuleType()).isEqualTo(RuleType.PROGRESSIVE_BONUS);
        assertThat(a.getAdjustment()).isEqualByComparingTo("6");
    });
}
```

**Step 2: 运行确认失败** (default no-op)

**Step 3: 实现** —— 复制 `PROGRESSIVE` 结构，改为统计 `finalScore > 0`，字段 `bonus`，adjustment 为正：

```java
case PROGRESSIVE_BONUS: {
    JsonNode thresholds = config.get("thresholds");
    if (thresholds != null && thresholds.isArray()) {
        long bonusCount = itemOutputs.stream()
                .filter(o -> o.getFinalScore().compareTo(BigDecimal.ZERO) > 0)
                .count();
        List<JsonNode> sorted = new ArrayList<>();
        thresholds.forEach(sorted::add);
        sorted.sort(Comparator.comparingInt(n -> n.path("count").asInt(0)));
        BigDecimal bonus = BigDecimal.ZERO;
        for (JsonNode t : sorted) {
            if (bonusCount >= t.path("count").asInt(0)) {
                bonus = getDecimal(t, "bonus", BigDecimal.ZERO);
            }
        }
        if (bonus.compareTo(BigDecimal.ZERO) != 0) {
            return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), true, bonus,
                    "累进加分: " + bonusCount + " 项达标");
        }
    }
    return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), false, BigDecimal.ZERO, null);
}
```

**Step 4: 运行确认通过**

**Step 5: 提交** `fix(inspection): 补全 PROGRESSIVE_BONUS 规则`

---

# Phase 1 — 接通归一化 (Stage 1)

**背景（两个独立 blocker，缺一不可）:**
1. `ScoreAggregationService.java:173` 调 `calculate(..., 0)` —— `population` 永远 0。
2. `ScoreAggregationService.java:392` `buildItemScoreInputs` 把 `NormalizationConfig` 传 `null` —— 即便 population 非 0，`calculateItemScore:155` 的 `normConfig != null` 也进不去。
3. `ScoringProfile` 根本**没有归一化配置字段** —— 要新增。

**设计决定:** 归一化是**章节级**（一个 section 的所有 item 共用一个分母/模式），不是 per-item。分母是**单一标量** `population`，由 resolver 按 `(TargetType, section.normalizeBy)` 算出，复用引擎现成的 `PER_CAPITA = baseline/population` 公式。

### Task 1A.1: NormalizationBasisResolver SPI 接口

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/inspection/service/NormalizationBasisResolver.java`
- Create enum: `backend/src/main/java/com/school/management/domain/inspection/model/scoring/NormalizeBy.java`

**Step 1: 写 enum**

```java
package com.school.management.domain.inspection.model.scoring;

/** 归一化分母维度 — 决定 population 从哪个基础模块取 */
public enum NormalizeBy {
    NONE,        // 不归一化
    PER_MEMBER,  // ÷ 成员数 (User 模块)
    PER_PLACE,   // ÷ 关联场所数 (Place 模块)
    PER_SUB_ORG  // ÷ 子组织数 (OrgUnit tree)
}
```

**Step 2: 写 SPI 接口**

```java
package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.model.scoring.NormalizeBy;

/**
 * 归一化分母解析器 — 按 (检查目标类型, 归一化维度) 算出 population 分母.
 *
 * <p>通用核心契约: 引擎不知道"班级/学生/教室"是什么, 只问本 resolver 要分母.
 * 分母来源限定在三个基础模块的通用字段 (成员数 / 场所容量 / 子组织数).
 * 行业插件可实现本接口覆盖默认逻辑 (如学校用"在册学生数").
 */
public interface NormalizationBasisResolver {

    /**
     * @param targetType  检查目标类型 (ORG/PLACE/USER/...)
     * @param targetId    目标 ID
     * @param normalizeBy 章节声明的归一化维度
     * @return 归一化分母 population; 无法解析或 USER 单体时返回 1 (引擎 population<=0 时不归一)
     */
    int resolveDenominator(TargetType targetType, Long targetId, NormalizeBy normalizeBy);
}
```

**Step 3: 提交** `feat(inspection): 归一化分母 SPI 接口 + NormalizeBy 枚举`

### Task 1A.2: 默认实现 DefaultNormalizationBasisResolver

**Files:**
- Create: `backend/src/main/java/com/school/management/infrastructure/inspection/DefaultNormalizationBasisResolver.java`
- Test: `backend/src/test/java/com/school/management/infrastructure/inspection/DefaultNormalizationBasisResolverTest.java`

**先确认取数来源（执行时 grep 落地，不要臆造方法名）:**
- 成员数：`access_relations` 中 subject 挂在该 orgUnit 下的 USER 计数 —— 找 `AccessRelationRepository` 是否有 `countMembersOfOrgUnit` 类方法；无则新增。
- 子组织数：`OrgUnitRepository`（已确认含 `tree_path`/`findByParentId` 相关）—— 直接子级 `countByParentId(orgUnitId)`，或全后代用 `tree_path LIKE '/.../id/%'`。**用直接子级**（roll-up 是逐层的，分母用直接子级更一致）。
- 场所数：place-org 关系（记忆：`/v6/place-org-relations`，表 `place_org_relations`）—— 找对应 repository 的 count 方法。
- 场所容量（当 target 本身是 PLACE）：`Place.capacity`。

**Step 1: 写失败测试**（mock 各 repository）

```java
@Test
void orgPerMember_returnsMemberCount() {
    when(accessRelationRepo.countMembersOfOrgUnit(5L)).thenReturn(40L);
    assertThat(resolver.resolveDenominator(TargetType.ORG, 5L, NormalizeBy.PER_MEMBER)).isEqualTo(40);
}
@Test
void orgPerSubOrg_returnsChildCount() {
    when(orgUnitRepo.countByParentId(5L)).thenReturn(20L);
    assertThat(resolver.resolveDenominator(TargetType.ORG, 5L, NormalizeBy.PER_SUB_ORG)).isEqualTo(20);
}
@Test
void userTarget_alwaysOne() {
    assertThat(resolver.resolveDenominator(TargetType.USER, 9L, NormalizeBy.PER_MEMBER)).isEqualTo(1);
}
@Test
void none_returnsOne() {
    assertThat(resolver.resolveDenominator(TargetType.ORG, 5L, NormalizeBy.NONE)).isEqualTo(1);
}
@Test
void placeTarget_returnsCapacity() {
    when(placeRepo.findCapacity(7L)).thenReturn(Optional.of(50));
    assertThat(resolver.resolveDenominator(TargetType.PLACE, 7L, NormalizeBy.PER_PLACE)).isEqualTo(50);
}
```

**Step 2: 运行确认失败**（类不存在）

**Step 3: 实现**

```java
@Component
@RequiredArgsConstructor
public class DefaultNormalizationBasisResolver implements NormalizationBasisResolver {

    private final AccessRelationRepository accessRelationRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PlaceOrgRelationRepository placeOrgRelationRepository;
    private final PlaceRepository placeRepository;

    @Override
    public int resolveDenominator(TargetType targetType, Long targetId, NormalizeBy normalizeBy) {
        if (normalizeBy == null || normalizeBy == NormalizeBy.NONE) return 1;
        if (targetType == TargetType.USER) return 1;          // 单体无规模问题
        if (targetId == null) return 1;
        try {
            if (targetType == TargetType.PLACE) {
                // 场所目标: 容量/房间数; 解析不到回退 1
                return placeRepository.findCapacity(targetId).filter(c -> c > 0).orElse(1);
            }
            if (targetType == TargetType.ORG) {
                long n = switch (normalizeBy) {
                    case PER_MEMBER  -> accessRelationRepository.countMembersOfOrgUnit(targetId);
                    case PER_PLACE   -> placeOrgRelationRepository.countPlacesOfOrgUnit(targetId);
                    case PER_SUB_ORG -> orgUnitRepository.countByParentId(targetId);
                    default -> 1L;
                };
                return n > 0 ? (int) n : 1;
            }
        } catch (Exception e) {
            // 取数失败不能让评分崩 — 回退不归一
            LoggerFactory.getLogger(getClass()).warn(
                "归一化分母解析失败 type={} id={} by={}: {}", targetType, targetId, normalizeBy, e.getMessage());
        }
        return 1;
    }
}
```

> 缺失的 repository count 方法在本 task 内顺手补（`countMembersOfOrgUnit` / `countPlacesOfOrgUnit` / `OrgUnit countByParentId` / `Place findCapacity`）。每补一个写一行 mapper SQL 或 MyBatis-Plus `selectCount(wrapper)`。

**Step 4: 运行确认通过** `mvn test -Dtest=DefaultNormalizationBasisResolverTest`

**Step 5: 提交** `feat(inspection): 归一化分母默认实现 — 按目标类型从 组织/用户/场所 取分母`

### Task 1B.1: ScoringProfile 加归一化字段 + DB 迁移

**Files:**
- Modify model: `backend/src/main/java/com/school/management/domain/inspection/model/scoring/ScoringProfile.java`（字段 + Builder + getter）
- Modify PO: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/.../ScoringProfilePO.java`（执行时 grep 确认路径）
- Modify mapper 转换：找 `ScoringProfile` 的 RepositoryImpl / converter
- Create migration: `database/migrations/V20260529_1__scoring_profile_normalization.sql`
- 改建表 SQL：`database/schema/` 中 scoring_profiles 的建表（grep `scoring_profiles`）

**新增字段（4 个）:**
```
normalizeBy          VARCHAR  -- NONE|PER_MEMBER|PER_PLACE|PER_SUB_ORG, 默认 NONE
normalizationMode    VARCHAR  -- NONE|PER_CAPITA|SQRT_ADJUSTED, 默认 NONE
baselinePopulation   INT      -- 基准规模 (如"标准班=40"), 默认 1
normFloor / normCap  DECIMAL  -- 系数下限/上限, 可空
```

**Step 1: 迁移（条件化，可重复执行）**

```sql
-- V20260529_1__scoring_profile_normalization.sql
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scoring_profiles' AND COLUMN_NAME = 'normalize_by');
SET @sql := IF(@col = 0,
  'ALTER TABLE scoring_profiles
     ADD COLUMN normalize_by VARCHAR(20) NOT NULL DEFAULT ''NONE'',
     ADD COLUMN normalization_mode VARCHAR(20) NOT NULL DEFAULT ''NONE'',
     ADD COLUMN baseline_population INT NOT NULL DEFAULT 1,
     ADD COLUMN norm_floor DECIMAL(10,4) NULL,
     ADD COLUMN norm_cap DECIMAL(10,4) NULL',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
```

**Step 2: 应用迁移并验证列存在**

Run: `mysql -u root -p student_management < database/migrations/V20260529_1__scoring_profile_normalization.sql`
然后 `DESC scoring_profiles;` 确认 5 列在。

**Step 3: 改 model + PO + converter**（加字段，机械改动；同步改 `database/schema` 建表 SQL）

**Step 4: 编译 + 启动验证**

Run: `cd backend && mvn -q -DskipTests package` 然后按记忆里的命令启动，确认无映射错误。

**Step 5: 提交** `feat(inspection): scoring_profiles 新增章节级归一化配置字段`

### Task 1C.1: 装配接通 — resolve denominator + 建 NormalizationConfig + 传 population

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/inspection/ScoreAggregationService.java`
  - 注入 `NormalizationBasisResolver`
  - `computeScoreFields`（:160）：在调 `calculate` 前 resolve denominator
  - `buildItemScoreInputs`（:380）：用 profile 的归一化配置构建 `NormalizationConfig`（不再传 null）

**Step 1: 写集成测试** `ScoreAggregationServiceTest`（或在现有测试类加）

场景：ORG 目标 + PER_MEMBER + PER_CAPITA + baseline=40。40 人单位 factor=1（扣分不变）；80 人单位 factor=0.5（同样违规扣分减半）。断言两单位归一后 finalScore 对齐。

```java
@Test
void orgScore_perCapitaNormalizesByMemberCount() {
    // profile.normalizeBy=PER_MEMBER, mode=PER_CAPITA, baseline=40
    // resolver mock: target 100 → 40 人; target 200 → 80 人
    // 两边相同的 -10 总扣分 → 归一后: 40人扣10, 80人扣5
    ...
    assertThat(small.deductionTotal).isEqualByComparingTo("-10");
    assertThat(big.deductionTotal).isEqualByComparingTo("-5");
}
```

**Step 2: 运行确认失败**（当前 population=0 + normConfig=null，两单位扣分相同 -10）

**Step 3: 实现**

`computeScoreFields` 改造（profileId != null 分支）：

```java
ScoringProfile profile = scoringProfileRepository.findById(profileId).orElseThrow(...);
...
// Stage 1 归一化: 解析分母
int population = 1;
if (profile.getNormalizeBy() != null && profile.getNormalizeBy() != NormalizeBy.NONE
        && subjectType != null && subjectId != null) {
    TargetType tt = TargetType.valueOf(/* subjectType 反映回 TargetType, ORG_UNIT→ORG */);
    population = normalizationBasisResolver.resolveDenominator(tt, subjectId, profile.getNormalizeBy());
}
List<ItemScoreInput> inputs = buildItemScoreInputs(details, profile); // 传 profile 以建 normConfig
applyEscalationPolicies(profileId, subjectType, subjectId, inputs);
ScoreResult result = scoreCalculationService.calculate(profile, dimensions, rules, gradeBands, inputs, population);
```

`buildItemScoreInputs` 增参 `ScoringProfile profile`，对每个 input 用 profile 配置建 `NormalizationConfig`：

```java
NormalizationConfig nc = (profile.getNormalizationMode() == null
        || profile.getNormalizationMode() == NormalizationMode.NONE)
    ? null
    : new NormalizationConfig(true, profile.getNormalizationMode(),
        profile.getBaselinePopulation() != null ? profile.getBaselinePopulation() : 1,
        profile.getNormCap(), profile.getNormFloor(), null);
inputs.add(new ItemScoreInput(itemCode, dimensionId, scoringMode,
        configScore, responseNumericValue, quantity, nc));
```

> 注意 `mapTargetTypeToSubject`（:401）把 ORG→"ORG_UNIT"。反向映射时 "ORG_UNIT"→`TargetType.ORG`。写个小 helper，别散落字面量。

**Step 4: 运行确认通过** + 真启动 + 真跑一次提交评分，看 `score_breakdown` JSON 里 `normFactor != 1`。

**Step 5: 提交** `feat(inspection): 接通章节级归一化 — 解析分母并传入评分引擎 (Stage 1)`

---

# Phase 2 — 前端 normalizeBy 章节配置

**背景:** Stage 1 接通后，管理员需要在评分编辑器里为每个章节选"按什么归一化"。放在 `ScoringProfileEditor.vue` 的"基础设置"折叠块（InspAccordion）内。

### Task 2.1: 类型 + API

**Files:**
- Modify: `frontend/src/types/insp/scoring.ts` —— `ScoringProfile` 加 `normalizeBy / normalizationMode / baselinePopulation / normFloor? / normCap?`
- 确认后端 DTO/SDK 已透出这些字段（grep openapi schema 或 controller 返回）

### Task 2.2: 编辑器 UI

**Files:**
- Modify: `frontend/src/views/inspection/scoring/ScoringProfileEditor.vue`

在基础设置 InspAccordion 内加一组（紧凑风格，遵循 UI 偏好——纯文字标签 + select，无彩色图标）：
- `归一化维度` select：不归一 / 按成员数 / 按场所数 / 按子组织数
- `归一化方式` select：不归一 / 人均 / 开方折中（仅当维度≠不归一时显示）
- `基准规模` number（如 40）
- 行内帮助文字：解释"人多/场所多的单位扣分会按此摊平"

**验证:** `cd frontend && npm run type-check`（baseline 0 错），手动在浏览器配一个 PER_MEMBER 章节保存→刷新→值仍在。

**提交:** `feat(inspection): 评分编辑器章节级归一化配置 UI`

---

# Phase 3 — 组织树汇总 roll-up (Stage 5)

**背景:** 当前最高汇总是 `ProjectScore`（项目×日期一条，`recomputeProjectScore:332` 简单平均）。**系统没有"组织级得分"**，部门/年级/学院排名无合法聚合层。这是"5 班 vs 20 班"的根因。

**设计:** 新增 `org_unit_scores`，对一个 (project, cycleDate) 的所有 submission，按其 target 所属 orgUnit 分组，沿 `OrgUnit.tree_path` **自底向上**逐层用 **MEAN** 滚动。叶子组织分 = 该组织 target 们的 submission 均分；父组织分 = 直接子组织分的均值（数量自动抵消）。

### Task 3.1: 表 + 迁移

**Files:**
- Create: `database/migrations/V20260529_2__org_unit_scores.sql`
- 改建表 SQL 入 `database/schema/`

```sql
CREATE TABLE IF NOT EXISTS org_unit_scores (
  id            BIGINT PRIMARY KEY,
  tenant_id     BIGINT NOT NULL DEFAULT 1,
  project_id    BIGINT NOT NULL,
  org_unit_id   BIGINT NOT NULL,
  cycle_date    DATE   NOT NULL,
  score         DECIMAL(8,2) NOT NULL,
  grade         VARCHAR(32) NULL,
  child_count   INT NOT NULL DEFAULT 0,   -- 参与汇总的直接子单位/目标数
  source_count  INT NOT NULL DEFAULT 0,   -- 该子树下叶子 submission 总数
  created_at    DATETIME NOT NULL,
  updated_at    DATETIME NOT NULL,
  UNIQUE KEY uk_org_score (tenant_id, project_id, org_unit_id, cycle_date)
);
```
> `child_count` 留作 Phase 5+ 方差/加权增强用；本期只填值不参与计算。

### Task 3.2: 领域模型 + 仓储 + PO + mapper

**Files:** 按 inspection 既有 ProjectScore 的目录结构平移一套 `OrgUnitScore`（model / repository 接口 / PO / RepositoryImpl / Mapper.xml）。
> org_unit_id 写入受 `CompositeMetaObjectHandler` 横切填充影响 —— 确认 PO 有 `orgUnitId` 字段并在 `InspectionUpstreamRouter` 注册（见记忆 `project_inspection_orgunit_filler`），避免触发 `InspectionWriteMustSetOrgUnitIdTest` 守护失败。

### Task 3.3: roll-up 服务

**Files:**
- Create: `backend/src/main/java/com/school/management/application/inspection/OrgUnitScoreRollupService.java`
- Test: `.../OrgUnitScoreRollupServiceTest.java`

**Step 1: 写失败测试**（核心断言：MEAN 抵消数量）

```java
@Test
void rollup_usesMeanNotSum_soSubOrgCountIsFair() {
    // 部门A 5 子组织 [90,90,90,90,60]; 部门B 20 子组织 [90×19,60]
    // MEAN: A=84, B=88.5  ← 不是把扣分加总
    ...
    assertThat(deptA.getScore()).isEqualByComparingTo("84.00");
    assertThat(deptB.getScore()).isEqualByComparingTo("88.50");
}
```

**Step 2: 运行确认失败**

**Step 3: 实现** —— 算法：
1. 取该 (project, cycleDate) 所有 COMPLETED submission，按 target 的 orgUnitId 分组，叶子分 = 组内 finalScore 均值。
2. 收集涉及的 orgUnit，按 `tree_path` 深度**降序**（最深先算）。
3. 逐个：若有直接子组织已算分 → 取子组织分均值；否则用叶子分。`upsert` 进 `org_unit_scores`（唯一键幂等，参考 `recomputeProjectScore` 的 DuplicateKey 处理）。

> `tree_path` 解析直接父子关系：`OrgUnit.parentId` 已有，直接用 `countByParentId`/`findByParentId`；`tree_path` 仅用于确定计算顺序（深度）。

**Step 4: 运行确认通过** + 真启动验证一次真实 project 的 roll-up 落库。

**Step 5: 提交** `feat(inspection): 组织树得分 roll-up — 沿 tree_path 用均值汇总 (Stage 5)`

### Task 3.4: 触发接线

**Files:**
- Modify: `ScoreAggregationService.recomputeProjectScore`（:309）末尾调用 `orgUnitScoreRollupService.rollup(projectId, cycleDate)`。
> 注意事务：roll-up 读的是已 save 的 submission/projectScore，放在同一 @Transactional 末尾即可；跨 service 若走事件则用 `@TransactionalEventListener(AFTER_COMMIT)`（见记忆事件监听器约定）。

**提交:** `feat(inspection): 项目分重算后级联触发组织树 roll-up`

### Task 3.5: 查询 API + 前端展示（可后置）

- `GET /inspection/org-scores?projectId=&cycleDate=` 返回某周期组织树各级分。
- 前端在项目"评分/评级"Tab 加组织排名视图（紧凑列表，无装饰图标）。
> 此 task 非阻塞公平性目标，可作为独立后续 PR。

---

# Phase 4 — 删死代码（trend / decay / multiRater / calibration）

**背景:** `ScoringProfile.java:24-44` 的 4 组高级算法字段，引擎 0 引用，是纯死配置 + 概念错位（趋势属分析、衰减属整改时效）。按"彻底删除"原则删干净，含 DB 列。

### Task 4.1: 后端删字段

**Files:**
- Modify: `ScoringProfile.java`（删 24-44 字段 + Builder 对应 + getter/setter）
- Modify: ScoringProfilePO + converter + 任何引用处（grep `trendFactorEnabled|decayEnabled|multiRaterMode|calibrationEnabled` 全仓）
- Create migration: `database/migrations/V20260529_3__drop_scoring_dead_columns.sql`（条件化 DROP COLUMN）
- 改建表 SQL 删列

**Step 1: 全仓 grep 确认无业务消费**

Run: `grep -rn "trendFactorEnabled\|decayEnabled\|multiRaterMode\|calibrationEnabled" backend frontend`
Expected: 仅 model/PO/converter/前端 editor + types 引用，无计算逻辑消费。**若发现意外消费方 → 停，先确认再删**（见记忆 `feedback_deprecated_is_not_dead_code` 教训）。

**Step 2: 写条件化 DROP 迁移**

```sql
-- 对每列: 存在才 DROP
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME='scoring_profiles' AND COLUMN_NAME='trend_factor_enabled');
SET @sql := IF(@c>0, 'ALTER TABLE scoring_profiles DROP COLUMN trend_factor_enabled, DROP COLUMN ...', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
```
（列出全部 trend_*/decay_*/multi_rater_*/rater_*/consensus_*/calibration_* 列）

**Step 3: 删后端字段 + 编译**

**Step 4: 删前端** —— `ScoringProfileEditor.vue` 的高级算法 InspAccordion 块 + `scoring.ts` 对应字段 + `ConceptDiagram.vue` 里"高级调整 时效/趋势"步骤改为不再宣称衰减/趋势（改为仅"规则链"输出，或删该步）。

**Step 5: 编译 + 启动 + type-check 全绿**

Run: `cd backend && mvn -q -DskipTests package` + 启动；`cd frontend && npm run type-check`

**Step 6: 提交** `refactor(inspection): 彻底删除死的高级算法字段 (trend/decay/multiRater/calibration) 含 DB 列`

---

# Phase 5 — 去 60 及格线硬编码

**背景:** `ScoreAggregationService` 无评分配置回退路径 `:213` `:245` 写死 `passed = pct >= 60`。应走 gradeBand / profile 配置，不写死。

### Task 5.1

**Files:** Modify `ScoreAggregationService.java:210-246`

**做法:** 及格判定改为：若该 section 的 gradeBand 标了"及格档"（或 profile 有 `passScore`），按配置判；无配置则 `passed=null`（未知，不臆断 60）。
> 若 profile/gradeBand 无"及格线"概念，新增一个可空 `passScore` 字段（同 Phase 1B 迁移模式），默认 null = 不判及格。**此项需先确认产品是否需要"及格"语义** —— 若评级体系只有等级无及格，直接把 passed 置 null 即可，不引入新字段。

**提交:** `refactor(inspection): 去除及格线 60 硬编码, 走配置`

---

## 收尾验证 checklist（全部 Phase 后）

- [ ] `cd backend && mvn test` 全绿（含新增归一化/规则/roll-up 测试）
- [ ] `cd backend && mvn -q -DskipTests package` + 真启动无报错
- [ ] 真跑一次：ORG 目标 PER_MEMBER 章节，大小两单位 → 归一后扣分按人数摊平（看 score_breakdown.normFactor）
- [ ] 真跑一次：5 子组织 vs 20 子组织部门 → org_unit_scores 用均值，数量不碾压
- [ ] `cd frontend && npm run type-check`（baseline 0）
- [ ] `cd frontend && npm run id-number-check`（baseline 0）
- [ ] ArchUnit 全绿（尤其 `NoIndustryTypeLiteralInCoreTest` — resolver 无行业字面量）
- [ ] `grep` 确认 trend/decay/multiRater/calibration 全仓 0 残留
- [ ] DB 三个迁移可重复执行（再跑一次不报错）

## 明确不做（YAGNI）
- 方差/中位数/规模加权（Stage 5 二阶公平）—— 等真实反馈再加，`org_unit_scores.child_count` 已预留。
- multiRater / calibration 的"实现"—— 直接删，不补。
- CUSTOM 公式扩展到 12 变量 —— 维持单 `score` 变量现状，不扩。
