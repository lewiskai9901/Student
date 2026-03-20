# 评分体系清理与修复 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 删除独立 scoring 领域（V7 是终态），修复 24 个设计问题中的高优先级项

**Architecture:** 删除 `domain/scoring` 全套代码（46 个后端文件 + 前端 API/types/views），修复 V7 `ScoreCalculationDomainService` 的计算逻辑缺陷、前端预览不一致、数据完整性问题

**Tech Stack:** Java 17, Spring Boot, MyBatis Plus, Vue 3, TypeScript

---

## Phase 1: 删除独立 Scoring 领域

### Task 1: 删除后端独立 scoring 代码

**Files to delete (46 files):**
- `backend/src/main/java/com/school/management/domain/scoring/` (entire directory — 11 files)
- `backend/src/main/java/com/school/management/application/scoring/` (entire directory — 10 files)
- `backend/src/main/java/com/school/management/infrastructure/persistence/scoring/` (entire directory — 15 files)
- `backend/src/main/java/com/school/management/interfaces/rest/scoring/` (entire directory — 5 files)

**Verify:** No external imports exist. All `domain.scoring` references are self-contained within these 4 directories.

**Step 1:** Delete the 4 directories

```bash
rm -rf backend/src/main/java/com/school/management/domain/scoring
rm -rf backend/src/main/java/com/school/management/application/scoring
rm -rf backend/src/main/java/com/school/management/infrastructure/persistence/scoring
rm -rf backend/src/main/java/com/school/management/interfaces/rest/scoring
```

**Step 2:** Verify no remaining references

```bash
grep -r "domain\.scoring" backend/src/ --include="*.java"
grep -r "application\.scoring" backend/src/ --include="*.java"
grep -r "interfaces\.rest\.scoring" backend/src/ --include="*.java"
```

Expected: No results (all references were self-contained)

### Task 2: 删除前端独立 scoring 代码

**Files to delete:**
- `frontend/src/api/scoring.ts`
- `frontend/src/types/scoring.ts`
- `frontend/src/views/system/CalculationRuleConfig.vue`
- `frontend/src/views/system/InputTypeConfig.vue`
- `frontend/src/views/inspection/v6/ScoringStrategyManagement.vue`
- `frontend/src/views/inspection/v6/components/scoring/` (entire directory — InputTypeSelector.vue, RuleChainEditor.vue, FormulaEditor.vue, StrategySelector.vue)

**Step 1:** Delete files

```bash
rm frontend/src/api/scoring.ts
rm frontend/src/types/scoring.ts
rm frontend/src/views/system/CalculationRuleConfig.vue
rm frontend/src/views/system/InputTypeConfig.vue
rm frontend/src/views/inspection/v6/ScoringStrategyManagement.vue
rm -rf frontend/src/views/inspection/v6/components/scoring
```

**Step 2:** Remove 3 router entries from `frontend/src/router/index.ts`

Remove these route blocks:
- `/inspection/v6/scoring-strategies` (name: `V6ScoringStrategyManagement`) — around line 331
- `/inspection/v6/input-types` (name: `V6InputTypeConfig`) — around line 367
- `/inspection/v6/calculation-rules` (name: `V6CalculationRuleConfig`) — around line 378

**Step 3:** Check for any remaining imports

```bash
grep -r "from.*api/scoring" frontend/src/ --include="*.ts" --include="*.vue"
grep -r "from.*types/scoring" frontend/src/ --include="*.ts" --include="*.vue"
```

Expected: No results

### Task 3: 验证构建

**Step 1:** Backend build

```bash
cd backend && mvn compile -DskipTests
```

Expected: BUILD SUCCESS

**Step 2:** Frontend build

```bash
cd frontend && npm run build
```

Expected: 0 errors

**Step 3:** Commit

```bash
git add -A
git commit -m "chore: remove standalone scoring domain (V7 is the final architecture)"
```

---

## Phase 2: 修复 ScoreCalculationDomainService (P0 + P1)

### Task 4: 注册为 Spring Bean (P0 #2)

**File:** `backend/src/main/java/com/school/management/domain/inspection/service/ScoreCalculationDomainService.java`

**Step 1:** Add `@Service` annotation and inject `FormulaEvaluator`

Add imports and annotation at class level:

```java
import org.springframework.stereotype.Service;

@Service
public class ScoreCalculationDomainService {
```

Constructor already takes `FormulaEvaluator` — Spring will auto-inject the `GraalVmFormulaEvaluator` `@Component`.

### Task 5: 修复 DEDUCTION 模式 (P1 #4)

**File:** `ScoreCalculationDomainService.java:146-152`

DEDUCTION 应该对 configScore 取反（如果 configScore 是正数，表示扣分额度，应该产生负分）:

**Before:**
```java
case "DEDUCTION":
    rawScore = configScore.multiply(BigDecimal.valueOf(quantity));
    break;
case "ADDITION":
    rawScore = configScore.multiply(BigDecimal.valueOf(quantity));
    break;
```

**After:**
```java
case "DEDUCTION":
    // 扣分: configScore 的绝对值 * quantity，结果为负
    rawScore = configScore.abs().multiply(BigDecimal.valueOf(quantity)).negate();
    break;
case "ADDITION":
    // 加分: configScore 的绝对值 * quantity，结果为正
    rawScore = configScore.abs().multiply(BigDecimal.valueOf(quantity));
    break;
```

### Task 6: 删除冗余归一化模式 + 修复精度 (P1 #5 + #6)

**File 1:** `backend/.../v7/scoring/NormalizationMode.java`

删除 RATE_BASED（与 PER_CAPITA 数学等价）:

```java
public enum NormalizationMode {
    NONE,
    PER_CAPITA,
    SQRT_ADJUSTED,
    CUSTOM
}
```

**File 2:** `backend/.../service/NormalizationCalculator.java`

1. 删除 `case RATE_BASED:` 分支
2. 修复 SQRT_ADJUSTED 使用 BigDecimal MathContext:

```java
case SQRT_ADJUSTED:
    // sqrt(baseline / population) using BigDecimal precision
    BigDecimal ratio = baseline.divide(pop, 20, RoundingMode.HALF_UP);
    factor = ratio.sqrt(new MathContext(15));
    break;
```

**File 3:** 前端 `frontend/src/types/insp/scoring.ts` — 同步删除 RATE_BASED 枚举值

### Task 7: 删除空实现规则类型 (P1 #7)

**File 1:** `backend/.../v7/scoring/RuleType.java`

删除 TIME_DECAY、POPULATION_NORMALIZE、REPEAT_OFFENSE（空实现且误导用户）:

```java
public enum RuleType {
    CEILING,
    FLOOR,
    VETO,
    PROGRESSIVE,
    BONUS,
    CUSTOM
}
```

**File 2:** `ScoreCalculationDomainService.java`

删除 `case TIME_DECAY:`, `case POPULATION_NORMALIZE:`, `case REPEAT_OFFENSE:` 分支（lines 436-449）。
删除 `applyRepeatOffenseRule()` 方法（lines 478-512）。

**File 3:** 前端 `frontend/src/types/insp/scoring.ts` — 同步删除对应枚举值

**File 4:** 前端 `frontend/src/views/inspection/v7/scoring/components/RuleConfigForm.vue` — 删除对应规则类型的表单分支

### Task 8: 修复 PROGRESSIVE 排序 (P1 #8)

**File:** `ScoreCalculationDomainService.java:414-434`

在遍历 thresholds 前先按 count 升序排序:

```java
case PROGRESSIVE: {
    JsonNode thresholds = config.get("thresholds");
    if (thresholds != null && thresholds.isArray()) {
        long deductionCount = itemOutputs.stream()
                .filter(o -> o.getFinalScore().compareTo(BigDecimal.ZERO) < 0)
                .count();

        // Sort thresholds by count ascending
        List<JsonNode> sorted = new ArrayList<>();
        thresholds.forEach(sorted::add);
        sorted.sort(Comparator.comparingInt(n -> n.get("count").asInt()));

        BigDecimal penalty = BigDecimal.ZERO;
        for (JsonNode t : sorted) {
            int count = t.get("count").asInt();
            BigDecimal p = new BigDecimal(t.get("penalty").asText());
            if (deductionCount >= count) {
                penalty = p;
            }
        }
        if (penalty.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal adj = penalty.negate();
            return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), true, adj, "累进扣分: " + deductionCount + " 项违规");
        }
    }
    return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), false, BigDecimal.ZERO, null);
}
```

### Task 9: 修复 clampScore 逻辑 (P1 #9)

**File:** `ScoreCalculationDomainService.java:543-557`

明确逻辑顺序：先 minScore，再 maxScore（minScore 优先级高于 allowNegative）:

```java
private BigDecimal clampScore(BigDecimal score, ScoringProfile profile) {
    BigDecimal min = profile.getMinScore() != null ? profile.getMinScore() : BigDecimal.ZERO;
    BigDecimal max = profile.getMaxScore() != null ? profile.getMaxScore() : new BigDecimal("100");

    // 1. Apply minScore (explicit lower bound)
    if (score.compareTo(min) < 0) {
        score = min;
    }
    // 2. Apply allowNegative constraint (only if minScore didn't already handle it)
    if (!Boolean.TRUE.equals(profile.getAllowNegative()) && score.compareTo(BigDecimal.ZERO) < 0) {
        score = BigDecimal.ZERO;
    }
    // 3. Apply maxScore (upper bound)
    if (score.compareTo(max) > 0) {
        score = max;
    }
    return score;
}
```

### Task 10: Commit Phase 2

```bash
git add -A
git commit -m "fix: repair ScoreCalculationDomainService — register as Bean, fix DEDUCTION/ADDITION, remove dead rule types, fix precision"
```

---

## Phase 3: 修复 ScoringProfile 验证 + 数据完整性 (P2)

### Task 11: ScoringProfile.update() 添加校验 (P2 #13)

**File:** `backend/.../v7/scoring/ScoringProfile.java:61-75`

```java
public void update(BigDecimal baseScore, BigDecimal maxScore, BigDecimal minScore,
                   Boolean allowNegative, Integer precisionDigits,
                   AggregationMethod aggregationMethod, String formulaEngine,
                   String defaultNormalization, Long updatedBy) {
    if (minScore != null && maxScore != null && minScore.compareTo(maxScore) > 0) {
        throw new IllegalArgumentException("minScore 不能大于 maxScore");
    }
    if (precisionDigits != null && (precisionDigits < 0 || precisionDigits > 10)) {
        throw new IllegalArgumentException("precisionDigits 必须在 0-10 之间");
    }
    if (baseScore != null && maxScore != null && baseScore.compareTo(maxScore) > 0) {
        throw new IllegalArgumentException("baseScore 不能大于 maxScore");
    }
    if (Boolean.FALSE.equals(allowNegative) && minScore != null && minScore.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("不允许负分时 minScore 不能小于 0");
    }
    this.baseScore = baseScore;
    this.maxScore = maxScore;
    this.minScore = minScore;
    this.allowNegative = allowNegative;
    this.precisionDigits = precisionDigits;
    this.aggregationMethod = aggregationMethod;
    this.formulaEngine = formulaEngine;
    this.defaultNormalization = defaultNormalization;
    this.updatedBy = updatedBy;
    this.updatedAt = LocalDateTime.now();
}
```

### Task 12: deleteDimension 级联删除 GradeBand (P2 #17)

**File:** `backend/.../v7/ScoringProfileApplicationService.java:107-110`

```java
@Transactional
public void deleteDimension(Long dimensionId) {
    gradeBandRepository.deleteByDimensionId(dimensionId);
    dimensionRepository.deleteById(dimensionId);
}
```

需要确认 `GradeBandRepository` 有 `deleteByDimensionId(Long)` 方法。如果没有，需添加。

### Task 13: GraalVM 超时生效 (P2 #18)

**File:** `backend/.../infrastructure/scoring/GraalVmFormulaEvaluator.java`

在 Context builder 中添加资源限制:

```java
try (Context context = Context.newBuilder("js")
        .engine(engine)
        .allowAllAccess(false)
        .option("engine.MaximumCompilationTime", String.valueOf(TIMEOUT_MS))
        .build()) {
```

注意: GraalVM CE 不支持 `engine.MaximumCompilationTime`，替代方案是用线程 + Future + timeout:

```java
@Override
public BigDecimal evaluate(String formula, Map<String, Object> variables) {
    // ... validation ...

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<BigDecimal> future = executor.submit(() -> evaluateInternal(formula, variables));
    try {
        return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
        future.cancel(true);
        throw new IllegalArgumentException("公式执行超时 (" + TIMEOUT_MS + "ms)");
    } catch (ExecutionException e) {
        throw new IllegalArgumentException("公式执行失败: " + e.getCause().getMessage());
    } finally {
        executor.shutdownNow();
    }
}
```

### Task 14: Commit Phase 3

```bash
git add -A
git commit -m "fix: add ScoringProfile validation, cascade delete GradeBand on dimension delete, enforce formula timeout"
```

---

## Phase 4: 修复前端评分预览 (P1)

### Task 15: 修复 useScoring.ts (P1 #10)

**File:** `frontend/src/composables/insp/useScoring.ts`

**Fix 1:** `aggregateDimensions()` — 使用实际权重而非硬编码 1

改为接收维度列表而非仅 scores map:

```typescript
function aggregateDimensions(
    dimensionResults: DimensionResult[],
    method: AggregationMethod | string,
): number {
    if (dimensionResults.length === 0) return 0

    switch (method) {
        case 'SUM':
            return dimensionResults.reduce((acc, d) => acc + d.rawScore, 0)

        case 'WEIGHTED_AVG': {
            const totalWeight = dimensionResults.reduce((acc, d) => acc + d.weight, 0)
            if (totalWeight === 0) return 0
            const weightedSum = dimensionResults.reduce((acc, d) => acc + d.rawScore * d.weight, 0)
            return weightedSum / totalWeight
        }

        case 'MIN':
            return Math.min(...dimensionResults.map(d => d.rawScore))

        case 'MAX':
            return Math.max(...dimensionResults.map(d => d.rawScore))

        default:
            return dimensionResults.reduce((acc, d) => acc + d.rawScore, 0)
    }
}
```

**Fix 2:** `calculateNormFactor()` — 匹配后端 NormalizationMode 枚举

```typescript
function calculateNormFactor(mode: string, population: number, baseline: number): number {
    switch (mode) {
        case 'PER_CAPITA':
            return population > 0 ? baseline / population : 1
        case 'SQRT_ADJUSTED':
            return population > 0 ? Math.sqrt(baseline / population) : 1
        case 'NONE':
        default:
            return 1
    }
}
```

**Fix 3:** `calculatePreview()` — 修复 baseScore 逻辑，与后端一致

```typescript
// Step 3: Aggregate dimension scores into total
let totalScore = aggregateDimensions(dimensionResults, profile.aggregationMethod)

// Step 4: For SUM aggregation, add baseScore as the starting point
// (matching backend: aggregateScores adds baseScore for SUM mode)
// For non-SUM modes, baseScore is not added (backend behavior)
```

**Fix 4:** 调用处 `calculatePreview` 中更新 `aggregateDimensions` 调用签名

### Task 16: 修复 useSubmissionScoring.ts 硬编码 baseScore (P1 #11)

**File:** `frontend/src/composables/insp/useSubmissionScoring.ts:223`

**Before:**
```typescript
const baseScore = 100
```

**After:** 从 props 或 profile 读取 baseScore（需要检查该 composable 是否有 profile 参数可用）

### Task 17: 同步前端 NormalizationMode 枚举

**File:** `frontend/src/types/insp/scoring.ts`

删除 `RATE_BASED` 枚举值，确保与后端一致：
- NONE, PER_CAPITA, SQRT_ADJUSTED, CUSTOM

同步删除前端 RuleType 中的 TIME_DECAY, POPULATION_NORMALIZE, REPEAT_OFFENSE

### Task 18: 验证前端构建 + Commit

```bash
cd frontend && npm run build
```

Expected: 0 errors

```bash
git add -A
git commit -m "fix: align frontend scoring preview with backend calculation logic"
```

---

## Phase 5: 验证全栈

### Task 19: 全栈构建验证

**Step 1:** Backend compile
```bash
cd backend && mvn compile -DskipTests
```

**Step 2:** Frontend build
```bash
cd frontend && npm run build
```

**Step 3:** Run backend tests (if any scoring-related tests exist)
```bash
cd backend && mvn test -Dtest="*Scoring*" -DfailIfNoTests=false
```

---

## 删除文件清单

### 后端（46 个文件）

**domain/scoring/** (11 files):
- model/aggregate/CalculationRule.java
- model/aggregate/InputType.java
- model/aggregate/ScoringStrategy.java
- model/entity/FormulaFunction.java
- model/entity/FormulaVariable.java
- model/valueobject/ComponentType.java
- model/valueobject/RuleType.java
- model/valueobject/StrategyCategory.java
- model/valueobject/ValueType.java
- repository/CalculationRuleRepository.java
- repository/InputTypeRepository.java
- repository/ScoringStrategyRepository.java
- repository/FormulaFunctionRepository.java
- repository/FormulaVariableRepository.java

**application/scoring/** (10 files):
- CalculationRuleApplicationService.java
- InputTypeApplicationService.java
- ScoringStrategyApplicationService.java
- FormulaApplicationService.java
- dto/CalculationRuleDTO.java
- dto/InputTypeDTO.java
- dto/ScoringStrategyDTO.java
- dto/FormulaFunctionDTO.java
- dto/FormulaVariableDTO.java
- dto/FormulaValidationResult.java
- dto/Create*Command.java (6 files)
- dto/Update*Command.java (3 files)

**infrastructure/persistence/scoring/** (15 files):
- *Mapper.java (5 files)
- *PO.java (5 files)
- *RepositoryImpl.java (5 files)

**interfaces/rest/scoring/** (5 files):
- CalculationRuleController.java
- InputTypeController.java
- ScoringStrategyController.java
- FormulaController.java
- ValidateFormulaRequest.java

### 前端（8+ 文件）

- api/scoring.ts
- types/scoring.ts
- views/system/CalculationRuleConfig.vue
- views/system/InputTypeConfig.vue
- views/inspection/v6/ScoringStrategyManagement.vue
- views/inspection/v6/components/scoring/ (4 files)
- router/index.ts (3 route entries removed)
