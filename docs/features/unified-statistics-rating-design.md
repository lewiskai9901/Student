# 统计分析与评级管理统一架构设计

> 版本：V1.0
> 日期：2024年12月
> 目标：解决现有统计、评级、排名模块的架构混乱问题

---

## 一、现状问题总结

### 1.1 评级体系混乱（三套并存）

| 体系 | 表 | Controller | Service | 问题 |
|------|-----|------------|---------|------|
| V3.0通用 | rating_templates/rules/levels | RatingController | RatingService | 与检查计划脱节 |
| 旧检查评级 | check_rating_* | CheckRatingConfigController | CheckRatingConfigService | 功能不完整 |
| 新检查计划评级 | check_plan_rating_* | CheckPlanRatingController | CheckPlanRatingService | 当前主力，但与V3.0重复 |

### 1.2 统计系统重叠

| 系统 | Controller | 功能 | 问题 |
|------|-----------|------|------|
| 传统统计 | CheckPlanStatisticsController | 排名、类别、趋势 | 功能较完整但缺少高级分析 |
| 智能统计 | SmartStatisticsController | 排名、覆盖率、轮次分析 | 与传统统计大量重复 |

### 1.3 前端组件重复

- `components/statistics/` 和 `components/analysis/` 存在同名同功能组件
- API 层 `checkPlanStatistics.ts` 和 `smartStatistics.ts` 功能重叠

---

## 二、目标架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           量化分析中心                                   │
│                     (Quantification Analytics Center)                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                        前端统一入口                               │  │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐     │  │
│  │  │ 统计概览 │ │ 班级排名 │ │ 评级管理 │ │ 频次统计 │ │ 数据导出 │     │  │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘     │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                    │                                     │
│                                    ▼                                     │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                      统一 API 层                                  │  │
│  │                                                                   │  │
│  │  /api/quantification/analytics/*     (统计分析)                   │  │
│  │  /api/quantification/ratings/*       (评级管理)                   │  │
│  │  /api/quantification/export/*        (数据导出)                   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                    │                                     │
│                                    ▼                                     │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                      后端服务层                                   │  │
│  │                                                                   │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │              AnalyticsService (统计分析服务)                 │  │  │
│  │  │  ├─ getOverview()          概览指标                          │  │  │
│  │  │  ├─ getClassRanking()      班级排名 (合并两套)               │  │  │
│  │  │  ├─ getCategoryStats()     类别统计                          │  │  │
│  │  │  ├─ getItemStats()         扣分项统计                        │  │  │
│  │  │  ├─ getTrendData()         趋势分析                          │  │  │
│  │  │  ├─ getRoundAnalysis()     轮次分析                          │  │  │
│  │  │  ├─ getCoverage()          覆盖率                            │  │  │
│  │  │  ├─ getClassTracking()     班级追踪                          │  │  │
│  │  │  └─ generateInsights()     智能洞察                          │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  │                                                                   │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │              RatingService (评级管理服务)                    │  │  │
│  │  │  ├─ 规则管理                                                 │  │  │
│  │  │  │  ├─ createRule()        创建评级规则                      │  │  │
│  │  │  │  ├─ updateRule()        更新规则                          │  │  │
│  │  │  │  ├─ deleteRule()        删除规则                          │  │  │
│  │  │  │  └─ getRules()          查询规则                          │  │  │
│  │  │  ├─ 等级管理                                                 │  │  │
│  │  │  │  ├─ createLevel()       创建等级                          │  │  │
│  │  │  │  ├─ updateLevels()      批量更新等级                      │  │  │
│  │  │  │  └─ getLevels()         查询等级                          │  │  │
│  │  │  ├─ 评级计算                                                 │  │  │
│  │  │  │  ├─ calculateDaily()    单次检查评级                      │  │  │
│  │  │  │  ├─ calculateSummary()  周期汇总评级                      │  │  │
│  │  │  │  └─ recalculate()       重新计算                          │  │  │
│  │  │  ├─ 结果管理                                                 │  │  │
│  │  │  │  ├─ getResults()        查询结果                          │  │  │
│  │  │  │  ├─ getClassHistory()   班级评级历史                      │  │  │
│  │  │  │  └─ getStatistics()     评级统计                          │  │  │
│  │  │  ├─ 审核发布                                                 │  │  │
│  │  │  │  ├─ approve()           审核通过                          │  │  │
│  │  │  │  ├─ reject()            审核驳回                          │  │  │
│  │  │  │  └─ publish()           发布                              │  │  │
│  │  │  └─ 频次统计                                                 │  │  │
│  │  │     ├─ getFrequency()      查询频次                          │  │  │
│  │  │     └─ getTopClasses()     TOP班级                           │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  │                                                                   │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │              ExportCenterService (导出中心服务)              │  │  │
│  │  │  ├─ 实时扣分导出                                             │  │  │
│  │  │  ├─ 评级通报导出                                             │  │  │
│  │  │  ├─ 统计分析导出                                             │  │  │
│  │  │  └─ 异步任务管理                                             │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                    │                                     │
│                                    ▼                                     │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                      数据层 (精简后)                              │  │
│  │                                                                   │  │
│  │  核心表（保留）:                                                  │  │
│  │  ├─ check_plan_rating_rules    → 重命名为 rating_rules           │  │
│  │  ├─ check_plan_rating_levels   → 重命名为 rating_levels          │  │
│  │  ├─ check_plan_rating_results  → 重命名为 rating_results         │  │
│  │  └─ rating_audit_logs          (新增，统一审计)                   │  │
│  │                                                                   │  │
│  │  废弃表（标记删除）:                                               │  │
│  │  ├─ ❌ rating_templates        (功能合并到rules)                 │  │
│  │  ├─ ❌ rating_rules (旧)       (被新rules替代)                   │  │
│  │  ├─ ❌ rating_levels (旧)      (被新levels替代)                  │  │
│  │  ├─ ❌ check_rating_config     (废弃)                            │  │
│  │  ├─ ❌ check_rating_level      (废弃)                            │  │
│  │  └─ ❌ check_rating_result     (废弃)                            │  │
│  │                                                                   │  │
│  │  缓存表（可选）:                                                   │  │
│  │  └─ rating_frequency_cache     (频次缓存，定时刷新)               │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块职责划分

| 模块 | 职责 | 合并来源 |
|------|------|---------|
| **AnalyticsService** | 所有统计分析功能 | CheckPlanStatisticsService + SmartStatisticsService |
| **RatingService** | 评级规则、计算、结果、审核、发布 | RatingService + CheckPlanRatingService + RatingFrequencyService |
| **ExportCenterService** | 三种场景导出 | 新建 |

---

## 三、数据库重构

### 3.1 表重命名与精简

```sql
-- ============================================================
-- 第一步：重命名核心表（保留数据）
-- ============================================================

-- 评级规则表
ALTER TABLE check_plan_rating_rules RENAME TO rating_rules;

-- 评级等级表
ALTER TABLE check_plan_rating_levels RENAME TO rating_levels;

-- 评级结果表
ALTER TABLE check_plan_rating_results RENAME TO rating_results;

-- 评级频次表（可选保留作为缓存）
ALTER TABLE check_plan_rating_frequency RENAME TO rating_frequency_cache;

-- ============================================================
-- 第二步：标记废弃表（不立即删除，先标记）
-- ============================================================

-- 添加废弃标记注释
ALTER TABLE rating_templates COMMENT = '[DEPRECATED] 功能已合并到rating_rules';
ALTER TABLE check_rating_config COMMENT = '[DEPRECATED] 已废弃';
ALTER TABLE check_rating_level COMMENT = '[DEPRECATED] 已废弃';
ALTER TABLE check_rating_result COMMENT = '[DEPRECATED] 已废弃';

-- ============================================================
-- 第三步：新增审计日志表
-- ============================================================

CREATE TABLE rating_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 关联信息
    rule_id BIGINT NOT NULL COMMENT '评级规则ID',
    result_id BIGINT COMMENT '评级结果ID',
    class_id BIGINT COMMENT '班级ID',

    -- 操作信息
    action VARCHAR(30) NOT NULL COMMENT '操作类型: CALCULATE/APPROVE/REJECT/PUBLISH/MODIFY',
    action_detail TEXT COMMENT '操作详情JSON',

    -- 变更追踪
    old_level_id BIGINT COMMENT '变更前等级',
    new_level_id BIGINT COMMENT '变更后等级',
    old_score DECIMAL(10,2) COMMENT '变更前分数',
    new_score DECIMAL(10,2) COMMENT '变更后分数',

    -- 审计字段
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(50),
    operated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500),

    INDEX idx_rule (rule_id),
    INDEX idx_result (result_id),
    INDEX idx_class (class_id),
    INDEX idx_action (action),
    INDEX idx_time (operated_at)
) COMMENT '评级操作审计日志';
```

### 3.2 rating_rules 表结构（合并后）

```sql
-- 评级规则表（合并 rating_templates 的功能）
CREATE TABLE rating_rules (
    id BIGINT PRIMARY KEY,

    -- 基础信息
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称（原模板名称）',
    rule_code VARCHAR(50) COMMENT '规则编码',
    description TEXT COMMENT '规则描述',

    -- 绑定关系
    check_plan_id BIGINT NOT NULL COMMENT '关联检查计划',

    -- 评级配置
    rule_type VARCHAR(20) NOT NULL COMMENT 'DAILY-单次检查 / SUMMARY-周期汇总',
    score_source VARCHAR(20) NOT NULL COMMENT 'TOTAL-总分 / CATEGORY-按类别',
    category_id BIGINT COMMENT '类别ID（scoreSource=CATEGORY时）',
    category_name VARCHAR(50) COMMENT '类别名称（冗余）',
    use_weighted_score TINYINT DEFAULT 0 COMMENT '是否使用加权分数',

    -- 划分方式
    division_method VARCHAR(20) NOT NULL COMMENT 'SCORE_RANGE/RANK_COUNT/PERCENTAGE',

    -- 汇总配置（SUMMARY类型）
    summary_method VARCHAR(20) COMMENT 'AVERAGE-平均 / SUM-累加',
    rating_cycle VARCHAR(20) COMMENT 'DAILY/WEEKLY/MONTHLY/CUSTOM',
    cycle_config JSON COMMENT '周期配置',

    -- 审核配置
    require_approval TINYINT DEFAULT 0 COMMENT '是否需要审核',
    auto_calculate TINYINT DEFAULT 1 COMMENT '是否自动计算',
    auto_publish TINYINT DEFAULT 0 COMMENT '是否自动发布',

    -- 状态
    enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,

    -- 审计
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_plan (check_plan_id),
    INDEX idx_type (rule_type),
    INDEX idx_category (category_id)
) COMMENT '评级规则表（统一）';
```

---

## 四、后端重构

### 4.1 Controller 层精简

#### 废弃的 Controller

```java
// ❌ 废弃：CheckPlanStatisticsController
// 功能合并到 AnalyticsController

// ❌ 废弃：SmartStatisticsController
// 功能合并到 AnalyticsController

// ❌ 废弃：RatingController (旧V3.0)
// 功能合并到 RatingManagementController

// ❌ 废弃：CheckRatingConfigController
// 配置功能合并到 RatingManagementController
```

#### 新的统一 Controller

```java
/**
 * 统计分析控制器（统一入口）
 */
@RestController
@RequestMapping("/api/quantification/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ========== 概览 ==========

    @GetMapping("/{planId}/overview")
    public Result<AnalyticsOverviewVO> getOverview(
            @PathVariable Long planId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(analyticsService.getOverview(planId, startDate, endDate));
    }

    // ========== 排名 ==========

    @GetMapping("/{planId}/class-ranking")
    public Result<PageResult<ClassRankingVO>> getClassRanking(
            @PathVariable Long planId,
            @ModelAttribute RankingQueryDTO query) {
        return Result.success(analyticsService.getClassRanking(planId, query));
    }

    @GetMapping("/{planId}/class-tracking/{classId}")
    public Result<ClassTrackingVO> getClassTracking(
            @PathVariable Long planId,
            @PathVariable Long classId) {
        return Result.success(analyticsService.getClassTracking(planId, classId));
    }

    // ========== 类别与扣分项 ==========

    @GetMapping("/{planId}/category-stats")
    public Result<List<CategoryStatsVO>> getCategoryStats(
            @PathVariable Long planId,
            @ModelAttribute StatsQueryDTO query) {
        return Result.success(analyticsService.getCategoryStats(planId, query));
    }

    @GetMapping("/{planId}/item-stats")
    public Result<List<ItemStatsVO>> getItemStats(
            @PathVariable Long planId,
            @ModelAttribute StatsQueryDTO query) {
        return Result.success(analyticsService.getItemStats(planId, query));
    }

    // ========== 趋势与分析 ==========

    @GetMapping("/{planId}/trend")
    public Result<TrendDataVO> getTrendData(
            @PathVariable Long planId,
            @ModelAttribute TrendQueryDTO query) {
        return Result.success(analyticsService.getTrendData(planId, query));
    }

    @GetMapping("/{planId}/round-analysis")
    public Result<RoundAnalysisVO> getRoundAnalysis(
            @PathVariable Long planId,
            @RequestParam(required = false) Long checkRecordId) {
        return Result.success(analyticsService.getRoundAnalysis(planId, checkRecordId));
    }

    // ========== 覆盖率与洞察 ==========

    @GetMapping("/{planId}/coverage")
    public Result<CoverageVO> getCoverage(
            @PathVariable Long planId,
            @ModelAttribute StatsQueryDTO query) {
        return Result.success(analyticsService.getCoverage(planId, query));
    }

    @GetMapping("/{planId}/insights")
    public Result<List<InsightVO>> getInsights(
            @PathVariable Long planId,
            @ModelAttribute StatsQueryDTO query) {
        return Result.success(analyticsService.generateInsights(planId, query));
    }
}
```

```java
/**
 * 评级管理控制器（统一入口）
 */
@RestController
@RequestMapping("/api/quantification/ratings")
@RequiredArgsConstructor
public class RatingManagementController {

    private final RatingService ratingService;

    // ========== 规则管理 ==========

    @PostMapping("/rules")
    public Result<RatingRuleVO> createRule(@RequestBody @Valid RatingRuleCreateDTO dto) {
        return Result.success(ratingService.createRule(dto));
    }

    @PutMapping("/rules/{ruleId}")
    public Result<RatingRuleVO> updateRule(
            @PathVariable Long ruleId,
            @RequestBody @Valid RatingRuleUpdateDTO dto) {
        return Result.success(ratingService.updateRule(ruleId, dto));
    }

    @DeleteMapping("/rules/{ruleId}")
    public Result<Void> deleteRule(@PathVariable Long ruleId) {
        ratingService.deleteRule(ruleId);
        return Result.success();
    }

    @GetMapping("/rules")
    public Result<List<RatingRuleVO>> getRules(
            @RequestParam Long checkPlanId,
            @RequestParam(required = false) String ruleType) {
        return Result.success(ratingService.getRules(checkPlanId, ruleType));
    }

    @GetMapping("/rules/{ruleId}")
    public Result<RatingRuleDetailVO> getRuleDetail(@PathVariable Long ruleId) {
        return Result.success(ratingService.getRuleDetail(ruleId));
    }

    // ========== 等级管理 ==========

    @GetMapping("/rules/{ruleId}/levels")
    public Result<List<RatingLevelVO>> getLevels(@PathVariable Long ruleId) {
        return Result.success(ratingService.getLevels(ruleId));
    }

    @PutMapping("/rules/{ruleId}/levels")
    public Result<List<RatingLevelVO>> updateLevels(
            @PathVariable Long ruleId,
            @RequestBody @Valid List<RatingLevelDTO> levels) {
        return Result.success(ratingService.updateLevels(ruleId, levels));
    }

    // ========== 评级计算 ==========

    @PostMapping("/calculate/daily")
    public Result<CalculationResultVO> calculateDaily(
            @RequestBody @Valid DailyCalculationDTO dto) {
        return Result.success(ratingService.calculateDaily(dto));
    }

    @PostMapping("/calculate/summary")
    public Result<CalculationResultVO> calculateSummary(
            @RequestBody @Valid SummaryCalculationDTO dto) {
        return Result.success(ratingService.calculateSummary(dto));
    }

    @PostMapping("/calculate/recalculate/{resultId}")
    public Result<CalculationResultVO> recalculate(@PathVariable Long resultId) {
        return Result.success(ratingService.recalculate(resultId));
    }

    // ========== 结果查询 ==========

    @GetMapping("/results")
    public Result<PageResult<RatingResultVO>> getResults(
            @ModelAttribute RatingResultQueryDTO query) {
        return Result.success(ratingService.getResults(query));
    }

    @GetMapping("/results/{resultId}")
    public Result<RatingResultDetailVO> getResultDetail(@PathVariable Long resultId) {
        return Result.success(ratingService.getResultDetail(resultId));
    }

    @GetMapping("/results/class/{classId}/history")
    public Result<List<RatingResultVO>> getClassHistory(
            @PathVariable Long classId,
            @RequestParam Long checkPlanId,
            @RequestParam(required = false) Long ruleId) {
        return Result.success(ratingService.getClassHistory(classId, checkPlanId, ruleId));
    }

    @GetMapping("/statistics")
    public Result<RatingStatisticsVO> getStatistics(
            @RequestParam Long ruleId,
            @RequestParam(required = false) Long checkRecordId,
            @RequestParam(required = false) LocalDate periodStart,
            @RequestParam(required = false) LocalDate periodEnd) {
        return Result.success(ratingService.getStatistics(ruleId, checkRecordId, periodStart, periodEnd));
    }

    // ========== 审核发布 ==========

    @GetMapping("/pending")
    public Result<List<RatingResultVO>> getPendingResults(
            @RequestParam Long checkPlanId) {
        return Result.success(ratingService.getPendingResults(checkPlanId));
    }

    @PostMapping("/results/{resultId}/approve")
    public Result<Void> approve(
            @PathVariable Long resultId,
            @RequestBody(required = false) ApprovalDTO dto) {
        ratingService.approve(resultId, dto);
        return Result.success();
    }

    @PostMapping("/results/{resultId}/reject")
    public Result<Void> reject(
            @PathVariable Long resultId,
            @RequestBody @Valid RejectionDTO dto) {
        ratingService.reject(resultId, dto);
        return Result.success();
    }

    @PostMapping("/results/batch-approve")
    public Result<BatchResultVO> batchApprove(@RequestBody @Valid BatchApprovalDTO dto) {
        return Result.success(ratingService.batchApprove(dto));
    }

    @PostMapping("/results/{resultId}/publish")
    public Result<Void> publish(@PathVariable Long resultId) {
        ratingService.publish(resultId);
        return Result.success();
    }

    @PostMapping("/results/batch-publish")
    public Result<BatchResultVO> batchPublish(@RequestBody @Valid BatchPublishDTO dto) {
        return Result.success(ratingService.batchPublish(dto));
    }

    // ========== 频次统计 ==========

    @GetMapping("/frequency")
    public Result<FrequencyDataVO> getFrequency(
            @RequestParam Long checkPlanId,
            @RequestParam Long ruleId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(ratingService.getFrequency(checkPlanId, ruleId, startDate, endDate));
    }

    @GetMapping("/frequency/top-classes")
    public Result<List<TopClassVO>> getTopClasses(
            @RequestParam Long checkPlanId,
            @RequestParam Long ruleId,
            @RequestParam Long levelId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(ratingService.getTopClasses(checkPlanId, ruleId, levelId, limit));
    }
}
```

### 4.2 Service 层精简

```java
/**
 * 统计分析服务（统一）
 * 合并 CheckPlanStatisticsService + SmartStatisticsService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final CheckRecordClassStatsNewMapper statsMapper;
    private final CheckRecordNewMapper recordMapper;
    private final CheckRecordDeductionNewMapper deductionMapper;
    private final DailyCheckMapper dailyCheckMapper;
    private final ClassMapper classMapper;

    /**
     * 获取统计概览
     */
    public AnalyticsOverviewVO getOverview(Long planId, LocalDate startDate, LocalDate endDate) {
        // 合并原 CheckPlanStatisticsService.getOverview
        // 和 SmartStatisticsService.getSmartOverview 的逻辑

        AnalyticsOverviewVO vo = new AnalyticsOverviewVO();

        // 基础统计
        vo.setTotalChecks(countChecks(planId, startDate, endDate));
        vo.setTotalClasses(countClasses(planId, startDate, endDate));
        vo.setTotalDeductionScore(sumDeductionScore(planId, startDate, endDate));
        vo.setAvgScorePerCheck(calculateAvgScore(planId, startDate, endDate));

        // 智能统计（来自SmartStatistics）
        vo.setCoverage(calculateCoverage(planId, startDate, endDate));
        vo.setTrend(calculateTrend(planId, startDate, endDate));
        vo.setWarnings(generateWarnings(planId, startDate, endDate));
        vo.setInsights(generateQuickInsights(planId, startDate, endDate));

        return vo;
    }

    /**
     * 获取班级排名（统一接口）
     */
    public PageResult<ClassRankingVO> getClassRanking(Long planId, RankingQueryDTO query) {
        // 统一排名逻辑，支持多种排序和比较方式
        // 合并原来两套系统的排名功能

        // 支持的比较模式：
        // - TOTAL: 总扣分排名
        // - AVERAGE: 平均扣分排名
        // - WEIGHTED: 加权分数排名
        // - VS_GRADE_AVG: 与年级平均比较

        return doGetClassRanking(planId, query);
    }

    // ... 其他方法
}
```

```java
/**
 * 评级管理服务（统一）
 * 合并 RatingService + CheckPlanRatingService + RatingFrequencyService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RatingService {

    private final RatingRuleMapper ruleMapper;
    private final RatingLevelMapper levelMapper;
    private final RatingResultMapper resultMapper;
    private final RatingAuditLogMapper auditLogMapper;
    private final CheckRecordClassStatsNewMapper statsMapper;

    // ========== 规则管理 ==========

    @Transactional
    public RatingRuleVO createRule(RatingRuleCreateDTO dto) {
        // 验证检查计划存在
        // 创建规则
        // 创建默认等级（如果需要）
        // 记录审计日志
    }

    // ========== 评级计算 ==========

    @Transactional
    public CalculationResultVO calculateDaily(DailyCalculationDTO dto) {
        // 1. 获取规则和等级配置
        // 2. 查询检查记录的班级统计数据
        // 3. 根据划分方式计算每个班级的等级
        // 4. 保存评级结果
        // 5. 如果配置了自动审核，则直接通过
        // 6. 如果配置了自动发布，则直接发布
        // 7. 记录审计日志
    }

    @Transactional
    public CalculationResultVO calculateSummary(SummaryCalculationDTO dto) {
        // 1. 获取规则和等级配置
        // 2. 聚合时间范围内的班级统计数据
        // 3. 根据汇总方式（平均/累加）计算
        // 4. 根据划分方式计算每个班级的等级
        // 5. 保存评级结果
        // 6. 记录审计日志
    }

    // ========== 频次统计（内置功能）==========

    public FrequencyDataVO getFrequency(Long checkPlanId, Long ruleId,
            LocalDate startDate, LocalDate endDate) {
        // 直接从 rating_results 表聚合计算
        // 不再依赖独立的 frequency 表

        String sql = """
            SELECT
                r.level_id,
                l.level_name,
                l.level_color,
                COUNT(*) as count,
                COUNT(DISTINCT r.class_id) as class_count
            FROM rating_results r
            JOIN rating_levels l ON r.level_id = l.id
            WHERE r.rule_id = :ruleId
              AND r.check_date BETWEEN :startDate AND :endDate
              AND r.publish_status = 1
            GROUP BY r.level_id, l.level_name, l.level_color
            ORDER BY l.level_order
            """;

        // 执行查询并返回
    }

    // ... 其他方法
}
```

---

## 五、前端重构

### 5.1 API 层精简

```typescript
// frontend/src/api/quantificationAnalytics.ts
// 合并 checkPlanStatistics.ts + smartStatistics.ts

import request from '@/utils/request'

const BASE_URL = '/api/quantification/analytics'

// ========== 概览 ==========

export function getOverview(planId: string | number, params?: {
  startDate?: string
  endDate?: string
}) {
  return request.get(`${BASE_URL}/${planId}/overview`, { params })
}

// ========== 排名 ==========

export function getClassRanking(planId: string | number, params: {
  pageNum?: number
  pageSize?: number
  compareMode?: 'TOTAL' | 'AVERAGE' | 'WEIGHTED' | 'VS_GRADE_AVG'
  startDate?: string
  endDate?: string
  gradeIds?: number[]
  departmentIds?: number[]
}) {
  return request.get(`${BASE_URL}/${planId}/class-ranking`, { params })
}

export function getClassTracking(planId: string | number, classId: number) {
  return request.get(`${BASE_URL}/${planId}/class-tracking/${classId}`)
}

// ========== 类别与扣分项 ==========

export function getCategoryStats(planId: string | number, params?: StatsQueryParams) {
  return request.get(`${BASE_URL}/${planId}/category-stats`, { params })
}

export function getItemStats(planId: string | number, params?: StatsQueryParams) {
  return request.get(`${BASE_URL}/${planId}/item-stats`, { params })
}

// ========== 趋势与分析 ==========

export function getTrendData(planId: string | number, params: TrendQueryParams) {
  return request.get(`${BASE_URL}/${planId}/trend`, { params })
}

export function getRoundAnalysis(planId: string | number, checkRecordId?: number) {
  return request.get(`${BASE_URL}/${planId}/round-analysis`, {
    params: { checkRecordId }
  })
}

// ========== 覆盖率与洞察 ==========

export function getCoverage(planId: string | number, params?: StatsQueryParams) {
  return request.get(`${BASE_URL}/${planId}/coverage`, { params })
}

export function getInsights(planId: string | number, params?: StatsQueryParams) {
  return request.get(`${BASE_URL}/${planId}/insights`, { params })
}
```

```typescript
// frontend/src/api/quantificationRatings.ts
// 合并 checkPlanRating.ts + ratingFrequency.ts

import request from '@/utils/request'

const BASE_URL = '/api/quantification/ratings'

// ========== 规则管理 ==========

export function createRule(data: RatingRuleCreateDTO) {
  return request.post(`${BASE_URL}/rules`, data)
}

export function updateRule(ruleId: number, data: RatingRuleUpdateDTO) {
  return request.put(`${BASE_URL}/rules/${ruleId}`, data)
}

export function deleteRule(ruleId: number) {
  return request.delete(`${BASE_URL}/rules/${ruleId}`)
}

export function getRules(checkPlanId: number, ruleType?: string) {
  return request.get(`${BASE_URL}/rules`, {
    params: { checkPlanId, ruleType }
  })
}

// ========== 评级计算 ==========

export function calculateDaily(data: DailyCalculationDTO) {
  return request.post(`${BASE_URL}/calculate/daily`, data)
}

export function calculateSummary(data: SummaryCalculationDTO) {
  return request.post(`${BASE_URL}/calculate/summary`, data)
}

// ========== 结果查询 ==========

export function getResults(params: RatingResultQueryParams) {
  return request.get(`${BASE_URL}/results`, { params })
}

export function getClassHistory(classId: number, checkPlanId: number, ruleId?: number) {
  return request.get(`${BASE_URL}/results/class/${classId}/history`, {
    params: { checkPlanId, ruleId }
  })
}

// ========== 审核发布 ==========

export function approve(resultId: number, data?: ApprovalDTO) {
  return request.post(`${BASE_URL}/results/${resultId}/approve`, data)
}

export function reject(resultId: number, data: RejectionDTO) {
  return request.post(`${BASE_URL}/results/${resultId}/reject`, data)
}

export function batchApprove(data: BatchApprovalDTO) {
  return request.post(`${BASE_URL}/results/batch-approve`, data)
}

export function publish(resultId: number) {
  return request.post(`${BASE_URL}/results/${resultId}/publish`)
}

// ========== 频次统计 ==========

export function getFrequency(params: FrequencyQueryParams) {
  return request.get(`${BASE_URL}/frequency`, { params })
}

export function getTopClasses(params: TopClassesQueryParams) {
  return request.get(`${BASE_URL}/frequency/top-classes`, { params })
}
```

### 5.2 组件精简

```
// 删除的组件
❌ frontend/src/views/quantification/components/analysis/  (整个目录)

// 保留并整合的组件
✓ frontend/src/views/quantification/components/statistics/
  ├─ OverviewSection.vue        (概览卡片组)
  ├─ RankingSection.vue         (排名 = 表格 + 图表)
  ├─ CategorySection.vue        (类别统计)
  ├─ ItemSection.vue            (扣分项统计)
  ├─ TrendSection.vue           (趋势分析)
  ├─ RoundAnalysisSection.vue   (轮次分析)
  ├─ CoverageSection.vue        (覆盖率)
  ├─ InsightsSection.vue        (智能洞察)
  └─ ClassTrackingDialog.vue    (班级追踪弹窗)

// 保留并整合的组件
✓ frontend/src/views/quantification/components/rating/
  ├─ RuleManager.vue            (规则管理)
  ├─ LevelEditor.vue            (等级编辑)
  ├─ CalculationPanel.vue       (计算面板)
  ├─ ResultsTable.vue           (结果列表)
  ├─ AuditPanel.vue             (审核面板)
  ├─ FrequencyChart.vue         (频次图表)
  └─ TopClassesCard.vue         (TOP班级卡片)
```

### 5.3 页面整合

```
// 删除的页面
❌ SmartStatisticsView.vue      (功能合并)
❌ AnalysisResultView.vue       (功能合并)
❌ AnalysisConfigListView.vue   (功能合并)

// 新的统一页面
✓ QuantificationAnalyticsView.vue   (统计分析 - 统一入口)
  └─ 包含: 概览、排名、类别、趋势、轮次分析、覆盖率、洞察

✓ QuantificationRatingView.vue      (评级管理 - 统一入口)
  └─ 包含: 规则管理、计算、结果、审核、发布、频次
```

---

## 六、迁移策略

### 6.1 分阶段迁移

```
阶段1: 数据层迁移（1周）
├─ 重命名核心表
├─ 标记废弃表
├─ 创建审计日志表
└─ 数据迁移脚本

阶段2: 后端服务迁移（2周）
├─ 创建新的统一Service
├─ 创建新的统一Controller
├─ 旧Controller添加@Deprecated注解
└─ 旧Controller转发到新Controller

阶段3: 前端迁移（1周）
├─ 创建新的统一API模块
├─ 创建新的统一页面
├─ 组件整合
└─ 路由更新

阶段4: 清理（1周）
├─ 删除废弃的Controller
├─ 删除废弃的Service
├─ 删除废弃的组件
└─ 删除废弃的API模块
```

### 6.2 兼容性保证

```java
// 旧Controller转发示例
@RestController
@RequestMapping("/check-plans/{planId}/statistics")
@Deprecated
public class CheckPlanStatisticsController {

    private final AnalyticsController analyticsController;

    @GetMapping("/class-ranking")
    public Result<?> getClassRanking(@PathVariable Long planId, ...) {
        log.warn("使用已废弃的API: /check-plans/{}/statistics/class-ranking, " +
                 "请迁移到 /api/quantification/analytics/{}/class-ranking", planId, planId);
        return analyticsController.getClassRanking(planId, ...);
    }
}
```

---

## 七、收益分析

| 维度 | 重构前 | 重构后 | 收益 |
|------|-------|-------|------|
| Controller数量 | 7个 | 3个 | 减少57% |
| Service数量 | 6个 | 3个 | 减少50% |
| API路由 | 3套重叠 | 1套统一 | 消除混乱 |
| 数据库表 | 12个 | 5个 | 减少58% |
| 前端组件 | 2套重复 | 1套统一 | 减少50% |
| 代码维护 | 困难 | 简单 | 显著提升 |

---

## 八、总结

本方案通过以下方式解决现有架构混乱问题：

1. **统一数据层**：精简为5张核心表，消除3套并存的评级体系
2. **统一服务层**：合并为3个核心服务（Analytics、Rating、Export）
3. **统一接口层**：建立规范的 `/api/quantification/*` 路由族
4. **统一前端**：消除重复组件和页面，建立清晰的模块结构

重构后的架构更加清晰、易于维护，同时保持了所有现有功能。
