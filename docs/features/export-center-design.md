# 量化检查导出中心 - 详细设计文档

> 版本：V1.0
> 日期：2024年12月
> 作者：系统设计

---

## 一、现状分析与问题诊断

### 1.1 现有导出能力

| 功能 | 实现状态 | 数据来源 | 问题 |
|------|---------|---------|------|
| 扣分明细导出 | ✅ 已实现 | DailyCheckDetail | 绑定到检查计划，无法实时导出进行中的检查 |
| 评级结果导出 | ⚠️ 部分实现 | CheckPlanRatingResult | RatingExportRequest已定义，但完整实现缺失 |
| 统计排名导出 | ⚠️ 部分实现 | 聚合查询 | 仅支持简单的班级排名导出 |

### 1.2 核心问题

1. **数据源分散**：扣分明细、评级结果、统计数据分布在不同的表和服务中
2. **场景覆盖不全**：
   - 无法在检查进行中导出部分数据（如第一轮迟到学生）
   - 评级导出缺乏多维度支持（卫生优秀、纪律优秀等）
   - 统计导出缺乏灵活的时间范围和聚合方式
3. **前端交互复杂**：过多的下拉选择框，用户需要理解复杂的配置项

### 1.3 用户场景深度分析

#### 场景一：实时扣分通报（检查进行中）

```
时间线：
─────────────────────────────────────────────────────
│ 第一轮检查 │ 【此时需要导出】 │ 第二轮检查 │ 检查结束 │
─────────────────────────────────────────────────────
             ↑
      检查还未结束，但需要导出迟到学生名单进行通报
```

**需求特点**：
- 数据来源：`DailyCheckDetail`（实时数据）
- 筛选维度：轮次、扣分项、班级
- 输出形式：学生明细表（姓名、学号、班级、扣分项、备注）

#### 场景二：评级通报（检查结束后）

```
多维度评级示例：
┌─────────────────────────────────────────────────┐
│           今日量化检查评级通报                   │
├─────────────────────────────────────────────────┤
│ 🏆 综合优秀班级：21计算机1班、21网络2班...      │
│ 🧹 卫生优秀班级：21电商1班、22计算机3班...      │
│ 📚 纪律优秀班级：22网络1班、21软件2班...        │
│ ⚠️ 需要整改班级：21机电2班、22电子1班...        │
└─────────────────────────────────────────────────┘
```

**需求特点**：
- 数据来源：`CheckPlanRatingResult` + `CheckPlanRatingLevel`
- 筛选维度：评级规则（综合/卫生/纪律）、等级（优秀/良好/需整改）
- 输出形式：分类通报文档

#### 场景三：统计分析报告

```
两种统计模式：
┌────────────────────┐  ┌────────────────────┐
│ 单次检查排名       │  │ 周期汇总排名        │
├────────────────────┤  ├────────────────────┤
│ 2024-12-20检查     │  │ 2024年12月第3周     │
│ 1. 21计算机1班 0分 │  │ 1. 21计算机1班 2分  │
│ 2. 21网络2班   1分 │  │ 2. 21网络2班   5分  │
│ 3. 22电商1班   2分 │  │ 3. 22电商1班   8分  │
│ ...               │  │ ...                │
└────────────────────┘  └────────────────────┘
```

**需求特点**：
- 数据来源：`CheckRecordClassStatsNew`（单次）或聚合查询（周期）
- 筛选维度：时间范围、年级、系部
- 输出形式：排名表格 + 统计图表

---

## 二、架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                       导出中心 (Export Center)                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    前端 - ExportCenterView.vue               ││
│  │  ┌───────────┐  ┌───────────┐  ┌───────────┐                ││
│  │  │ 实时扣分  │  │ 评级通报  │  │ 统计分析  │ ← 场景Tab      ││
│  │  │   导出    │  │   导出    │  │   导出    │                ││
│  │  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘                ││
│  │        └──────────────┼──────────────┘                      ││
│  │                       ▼                                      ││
│  │        ┌──────────────────────────────┐                     ││
│  │        │     配置面板 + 实时预览       │                     ││
│  │        └──────────────────────────────┘                     ││
│  └─────────────────────────────────────────────────────────────┘│
│                              │                                   │
│                              ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    API 层 - exportCenter.ts                  ││
│  │  POST /api/export-center/preview                            ││
│  │  POST /api/export-center/export                             ││
│  │  GET  /api/export-center/templates                          ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                       后端 - Export Center                       │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                   ExportCenterController                     ││
│  │         /api/export-center/*                                 ││
│  └───────────────────────────┬─────────────────────────────────┘│
│                              ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    ExportCenterService                       ││
│  │  - 统一入口，路由到具体策略                                   ││
│  │  - 预览生成、文件导出                                         ││
│  └───────────────────────────┬─────────────────────────────────┘│
│                              ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                  ExportStrategyFactory                       ││
│  │  根据 ExportScenario 返回对应策略                             ││
│  └───────────────────────────┬─────────────────────────────────┘│
│              ┌───────────────┼───────────────┐                   │
│              ▼               ▼               ▼                   │
│  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐          │
│  │ RealtimeDeduct│ │ RatingReport  │ │ Statistics    │          │
│  │ ExportStrategy│ │ ExportStrategy│ │ ExportStrategy│          │
│  └───────┬───────┘ └───────┬───────┘ └───────┬───────┘          │
│          │                 │                 │                   │
│          ▼                 ▼                 ▼                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    数据访问层                                 ││
│  │  DailyCheckDetail  CheckPlanRatingResult  CheckRecordStats  ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 核心设计决策

#### 决策1：统一入口 vs 分散入口

**选择：统一入口（导出中心）**

理由：
- 用户只需学习一个界面
- 代码复用率高（预览、文件生成等公共逻辑）
- 便于后续扩展新的导出场景

#### 决策2：前端交互设计

**选择：场景驱动 + 卡片式配置 + 实时预览**

```
传统方式（不推荐）：
┌─────────────────────────────────────┐
│ 导出类型：[下拉选择 ▼]              │
│ 检查计划：[下拉选择 ▼]              │
│ 日常检查：[下拉选择 ▼]              │
│ 检查轮次：[多选下拉 ▼]              │
│ 扣分项目：[多选下拉 ▼]              │
│ ...太多选择框...                    │
└─────────────────────────────────────┘

新设计（推荐）：
┌─────────────────────────────────────────────────────────┐
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                  │
│  │📋 扣分  │  │🏆 评级  │  │📊 统计  │  ← 场景Tab      │
│  │  通报   │  │  通报   │  │  分析   │                  │
│  └────┬────┘  └─────────┘  └─────────┘                  │
│       │                                                  │
│  ┌────▼────────────────────────────────────────────────┐│
│  │ 选择检查：                                          ││
│  │ ┌────────────────────────────────────────────────┐  ││
│  │ │ 📅 12月20日 日常检查 (进行中)          [选中]  │  ││
│  │ │ 📅 12月19日 日常检查 (已完成)                  │  ││
│  │ └────────────────────────────────────────────────┘  ││
│  │                                                      ││
│  │ 快速筛选：  [第一轮] [第二轮] [全部轮次]            ││
│  │                                                      ││
│  │ 扣分项目：  点击选择需要导出的项目                  ││
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐              ││
│  │ │ ✓ 迟到   │ │ □ 旷课   │ │ ✓ 早退   │              ││
│  │ └──────────┘ └──────────┘ └──────────┘              ││
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐              ││
│  │ │ □ 卫生差 │ │ □ 玩手机 │ │ □ 其他   │              ││
│  │ └──────────┘ └──────────┘ └──────────┘              ││
│  └──────────────────────────────────────────────────────┘│
│                                                          │
│  ┌──────────────────────────────────────────────────────┐│
│  │ 预览 (共12条记录)                    [Excel] [Word] ││
│  │ ┌────────────────────────────────────────────────┐  ││
│  │ │ 序号 │ 学号 │ 姓名 │ 班级 │ 扣分项 │ 备注      │  ││
│  │ ├────────────────────────────────────────────────┤  ││
│  │ │  1  │ 2101 │ 张三 │ 21计1│ 迟到   │ 迟到5分钟 │  ││
│  │ │  2  │ 2102 │ 李四 │ 21计1│ 迟到   │           │  ││
│  │ │ ... │      │      │      │        │           │  ││
│  │ └────────────────────────────────────────────────┘  ││
│  └──────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

#### 决策3：数据模型扩展

**需要扩展的表结构**：

无需新增表，但需要确保以下查询能力：
- `DailyCheckDetail` 支持按轮次、扣分项的高效查询
- `CheckPlanRatingResult` 支持按维度（综合/卫生/纪律）查询
- 聚合查询支持时间范围的班级排名

---

## 三、详细设计

### 3.1 数据模型

#### 3.1.1 导出场景枚举

```java
public enum ExportScenario {
    /**
     * 实时扣分导出 - 检查进行中也可导出
     */
    REALTIME_DEDUCTION("realtime_deduction", "扣分通报"),

    /**
     * 评级通报导出 - 导出各类评级结果
     */
    RATING_REPORT("rating_report", "评级通报"),

    /**
     * 统计分析导出 - 排名和汇总统计
     */
    STATISTICS_ANALYSIS("statistics", "统计分析");

    private final String code;
    private final String name;
}
```

#### 3.1.2 统一导出请求

```java
@Data
public class ExportCenterRequest {
    /**
     * 导出场景
     */
    @NotNull
    private ExportScenario scenario;

    /**
     * 导出格式
     */
    private ExportFormat format = ExportFormat.EXCEL;

    /**
     * 场景配置（多态）
     */
    @NotNull
    private ExportScenarioConfig config;
}

/**
 * 场景配置基类
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RealtimeDeductionConfig.class, name = "realtime"),
    @JsonSubTypes.Type(value = RatingReportConfig.class, name = "rating"),
    @JsonSubTypes.Type(value = StatisticsConfig.class, name = "statistics")
})
public abstract class ExportScenarioConfig {
    public abstract ExportScenario getScenario();
}
```

#### 3.1.3 实时扣分导出配置

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class RealtimeDeductionConfig extends ExportScenarioConfig {

    /**
     * 日常检查ID（必填）
     */
    @NotNull
    private Long dailyCheckId;

    /**
     * 检查轮次（可选，null表示全部）
     */
    private List<Integer> checkRounds;

    /**
     * 扣分项ID列表（可选，null表示全部）
     */
    private List<Long> deductionItemIds;

    /**
     * 类别ID列表（可选）
     */
    private List<Long> categoryIds;

    /**
     * 班级ID列表（可选，null表示全部）
     */
    private List<Long> classIds;

    /**
     * 分组方式
     */
    private GroupBy groupBy = GroupBy.CLASS;

    /**
     * 是否包含学生详情
     */
    private Boolean includeStudentDetail = true;

    /**
     * 文档模板（可选，用于生成正式通报）
     */
    private String documentTemplate;

    public enum GroupBy {
        NONE,           // 不分组，按学生列表
        CLASS,          // 按班级分组
        DEPARTMENT,     // 按系部分组
        DEDUCTION_ITEM  // 按扣分项分组
    }

    @Override
    public ExportScenario getScenario() {
        return ExportScenario.REALTIME_DEDUCTION;
    }
}
```

#### 3.1.4 评级通报导出配置

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class RatingReportConfig extends ExportScenarioConfig {

    /**
     * 检查记录ID（单次检查评级时使用）
     */
    private Long checkRecordId;

    /**
     * 检查计划ID（周期汇总评级时使用）
     */
    private Long checkPlanId;

    /**
     * 评级规则ID列表（可选，null表示全部规则）
     * 支持同时导出多个维度：综合评级、卫生评级、纪律评级
     */
    private List<Long> ratingRuleIds;

    /**
     * 等级筛选（可选，如只导出"优秀"等级）
     */
    private List<Long> levelIds;

    /**
     * 等级名称筛选（备选方案）
     */
    private List<String> levelNames;

    /**
     * 时间范围（周期汇总时使用）
     */
    private LocalDate periodStart;
    private LocalDate periodEnd;

    /**
     * 输出模式
     */
    private OutputMode outputMode = OutputMode.GROUPED_BY_LEVEL;

    public enum OutputMode {
        GROUPED_BY_LEVEL,    // 按等级分组（优秀班级、良好班级...）
        GROUPED_BY_DIMENSION,// 按维度分组（卫生优秀、纪律优秀...）
        FLAT_LIST,           // 平铺列表（班级、等级、分数...）
        SUMMARY_ONLY         // 仅汇总（各等级数量统计）
    }

    @Override
    public ExportScenario getScenario() {
        return ExportScenario.RATING_REPORT;
    }
}
```

#### 3.1.5 统计分析导出配置

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class StatisticsConfig extends ExportScenarioConfig {

    /**
     * 统计类型
     */
    @NotNull
    private StatisticsType statsType;

    /**
     * 检查记录ID（单次检查排名时使用）
     */
    private Long checkRecordId;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 时间范围（周期统计时使用）
     */
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * 统计目标
     */
    private StatisticsTarget target = StatisticsTarget.CLASS;

    /**
     * 排名范围（如只导出前10名）
     */
    private Integer topN;

    /**
     * 筛选条件
     */
    private List<Long> departmentIds;
    private List<Long> gradeIds;

    /**
     * 是否包含趋势数据（周期统计时）
     */
    private Boolean includeTrend = false;

    /**
     * 分数类型
     */
    private ScoreType scoreType = ScoreType.TOTAL;

    public enum StatisticsType {
        SINGLE_CHECK_RANKING,   // 单次检查排名
        PERIOD_TOTAL_RANKING,   // 周期总分排名
        PERIOD_AVERAGE_RANKING, // 周期平均分排名
        TREND_ANALYSIS          // 趋势分析
    }

    public enum StatisticsTarget {
        CLASS,       // 按班级统计
        DEPARTMENT,  // 按系部统计
        GRADE        // 按年级统计
    }

    public enum ScoreType {
        TOTAL,       // 原始扣分
        WEIGHTED     // 加权扣分
    }

    @Override
    public ExportScenario getScenario() {
        return ExportScenario.STATISTICS_ANALYSIS;
    }
}
```

### 3.2 服务层设计

#### 3.2.1 策略接口

```java
public interface ExportStrategy<C extends ExportScenarioConfig> {

    /**
     * 获取策略对应的场景
     */
    ExportScenario getScenario();

    /**
     * 获取预览数据
     */
    ExportPreviewResult preview(C config);

    /**
     * 生成导出文件
     */
    byte[] export(C config, ExportFormat format);

    /**
     * 获取默认列配置
     */
    List<ExportColumn> getDefaultColumns();

    /**
     * 获取文件名
     */
    String getFileName(C config, ExportFormat format);
}
```

#### 3.2.2 实时扣分导出策略

```java
@Service
@Slf4j
public class RealtimeDeductionExportStrategy
    implements ExportStrategy<RealtimeDeductionConfig> {

    private final DailyCheckMapper dailyCheckMapper;
    private final DailyCheckDetailMapper detailMapper;
    private final ClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final DeductionItemMapper itemMapper;

    @Override
    public ExportScenario getScenario() {
        return ExportScenario.REALTIME_DEDUCTION;
    }

    @Override
    public ExportPreviewResult preview(RealtimeDeductionConfig config) {
        // 1. 获取日常检查信息
        DailyCheck dailyCheck = dailyCheckMapper.selectById(config.getDailyCheckId());
        if (dailyCheck == null) {
            throw new BusinessException("日常检查不存在");
        }

        // 2. 构建查询条件
        LambdaQueryWrapper<DailyCheckDetail> wrapper = buildQueryWrapper(config);

        // 3. 查询扣分明细
        List<DailyCheckDetail> details = detailMapper.selectList(wrapper);

        // 4. 展开学生记录
        List<DeductionStudentRecord> records = expandStudentRecords(details);

        // 5. 根据分组方式组织数据
        Object groupedData = groupData(records, config.getGroupBy());

        // 6. 构建预览结果
        return ExportPreviewResult.builder()
            .scenario(ExportScenario.REALTIME_DEDUCTION)
            .title(buildTitle(dailyCheck, config))
            .totalCount(records.size())
            .summary(buildSummary(dailyCheck, records))
            .columns(getDefaultColumns())
            .previewData(groupedData)
            .build();
    }

    private LambdaQueryWrapper<DailyCheckDetail> buildQueryWrapper(
            RealtimeDeductionConfig config) {
        LambdaQueryWrapper<DailyCheckDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyCheckDetail::getCheckId, config.getDailyCheckId())
               .eq(DailyCheckDetail::getDeleted, 0);

        // 按轮次筛选
        if (config.getCheckRounds() != null && !config.getCheckRounds().isEmpty()) {
            wrapper.in(DailyCheckDetail::getCheckRound, config.getCheckRounds());
        }

        // 按扣分项筛选
        if (config.getDeductionItemIds() != null && !config.getDeductionItemIds().isEmpty()) {
            wrapper.in(DailyCheckDetail::getDeductionItemId, config.getDeductionItemIds());
        }

        // 按类别筛选
        if (config.getCategoryIds() != null && !config.getCategoryIds().isEmpty()) {
            wrapper.in(DailyCheckDetail::getCategoryId, config.getCategoryIds());
        }

        // 按班级筛选
        if (config.getClassIds() != null && !config.getClassIds().isEmpty()) {
            wrapper.in(DailyCheckDetail::getClassId, config.getClassIds());
        }

        return wrapper;
    }

    private List<DeductionStudentRecord> expandStudentRecords(
            List<DailyCheckDetail> details) {
        List<DeductionStudentRecord> records = new ArrayList<>();

        for (DailyCheckDetail detail : details) {
            if (!StringUtils.hasText(detail.getStudentIds())) {
                continue;
            }

            String[] studentIds = detail.getStudentIds().split(",");
            String[] studentNames = detail.getStudentNames() != null
                ? detail.getStudentNames().split(",")
                : new String[0];

            for (int i = 0; i < studentIds.length; i++) {
                DeductionStudentRecord record = new DeductionStudentRecord();
                record.setStudentId(Long.parseLong(studentIds[i].trim()));
                record.setStudentName(i < studentNames.length ? studentNames[i].trim() : "");
                record.setClassId(detail.getClassId());
                record.setClassName(detail.getClassName());
                record.setDeductionItemId(detail.getDeductionItemId());
                record.setDeductionItemName(detail.getDeductionItemName());
                record.setDeductScore(detail.getDeductScore());
                record.setRemark(detail.getRemark());
                record.setCheckRound(detail.getCheckRound());
                record.setCheckTime(detail.getCreatedAt());

                // 查询学生详细信息
                enrichStudentInfo(record);

                records.add(record);
            }
        }

        return records;
    }

    @Override
    public List<ExportColumn> getDefaultColumns() {
        return Arrays.asList(
            ExportColumn.of("index", "序号", 50),
            ExportColumn.of("studentNo", "学号", 100),
            ExportColumn.of("studentName", "姓名", 80),
            ExportColumn.of("className", "班级", 120),
            ExportColumn.of("deductionItemName", "扣分项目", 120),
            ExportColumn.of("deductScore", "扣分", 60),
            ExportColumn.of("remark", "备注", 150)
        );
    }

    @Override
    public String getFileName(RealtimeDeductionConfig config, ExportFormat format) {
        DailyCheck dailyCheck = dailyCheckMapper.selectById(config.getDailyCheckId());
        String datePart = dailyCheck.getCheckDate()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String roundPart = "";
        if (config.getCheckRounds() != null && config.getCheckRounds().size() == 1) {
            roundPart = "_第" + config.getCheckRounds().get(0) + "轮";
        }
        return "扣分通报_" + datePart + roundPart + format.getExtension();
    }
}
```

#### 3.2.3 评级通报导出策略

```java
@Service
@Slf4j
public class RatingReportExportStrategy
    implements ExportStrategy<RatingReportConfig> {

    private final CheckPlanRatingResultMapper resultMapper;
    private final CheckPlanRatingRuleMapper ruleMapper;
    private final CheckPlanRatingLevelMapper levelMapper;
    private final CheckRecordNewMapper recordMapper;

    @Override
    public ExportScenario getScenario() {
        return ExportScenario.RATING_REPORT;
    }

    @Override
    public ExportPreviewResult preview(RatingReportConfig config) {
        // 1. 获取评级结果
        List<CheckPlanRatingResult> results = queryRatingResults(config);

        // 2. 获取评级规则信息
        Map<Long, RatingRuleInfo> ruleInfoMap = loadRuleInfo(results);

        // 3. 根据输出模式组织数据
        Object groupedData = organizeByOutputMode(results, ruleInfoMap, config.getOutputMode());

        // 4. 构建预览结果
        return ExportPreviewResult.builder()
            .scenario(ExportScenario.RATING_REPORT)
            .title(buildTitle(config))
            .totalCount(results.size())
            .summary(buildSummary(results, ruleInfoMap))
            .columns(getColumnsForMode(config.getOutputMode()))
            .previewData(groupedData)
            .build();
    }

    private Object organizeByOutputMode(
            List<CheckPlanRatingResult> results,
            Map<Long, RatingRuleInfo> ruleInfoMap,
            RatingReportConfig.OutputMode mode) {

        switch (mode) {
            case GROUPED_BY_LEVEL:
                // 按等级分组：优秀班级、良好班级...
                return results.stream()
                    .collect(Collectors.groupingBy(
                        r -> r.getLevelName(),
                        LinkedHashMap::new,
                        Collectors.toList()
                    ));

            case GROUPED_BY_DIMENSION:
                // 按维度分组：卫生优秀、纪律优秀...
                return results.stream()
                    .collect(Collectors.groupingBy(
                        r -> ruleInfoMap.get(r.getRuleId()).getDimensionName()
                             + r.getLevelName(),
                        LinkedHashMap::new,
                        Collectors.toList()
                    ));

            case FLAT_LIST:
                // 平铺列表
                return results.stream()
                    .map(r -> toFlatRecord(r, ruleInfoMap.get(r.getRuleId())))
                    .collect(Collectors.toList());

            case SUMMARY_ONLY:
                // 仅汇总
                return buildSummaryData(results, ruleInfoMap);

            default:
                return results;
        }
    }

    @Override
    public List<ExportColumn> getDefaultColumns() {
        return Arrays.asList(
            ExportColumn.of("ranking", "排名", 60),
            ExportColumn.of("className", "班级", 120),
            ExportColumn.of("gradeName", "年级", 80),
            ExportColumn.of("levelName", "评级", 80),
            ExportColumn.of("score", "扣分", 60)
        );
    }
}
```

#### 3.2.4 统计分析导出策略

```java
@Service
@Slf4j
public class StatisticsExportStrategy
    implements ExportStrategy<StatisticsConfig> {

    private final CheckRecordClassStatsNewMapper statsMapper;
    private final CheckRecordNewMapper recordMapper;
    private final ClassMapper classMapper;

    @Override
    public ExportScenario getScenario() {
        return ExportScenario.STATISTICS_ANALYSIS;
    }

    @Override
    public ExportPreviewResult preview(StatisticsConfig config) {
        List<ClassRankingRecord> rankings = switch (config.getStatsType()) {
            case SINGLE_CHECK_RANKING -> querySingleCheckRanking(config);
            case PERIOD_TOTAL_RANKING -> queryPeriodTotalRanking(config);
            case PERIOD_AVERAGE_RANKING -> queryPeriodAverageRanking(config);
            case TREND_ANALYSIS -> queryTrendData(config);
        };

        // 应用topN筛选
        if (config.getTopN() != null && config.getTopN() > 0) {
            rankings = rankings.stream()
                .limit(config.getTopN())
                .collect(Collectors.toList());
        }

        return ExportPreviewResult.builder()
            .scenario(ExportScenario.STATISTICS_ANALYSIS)
            .title(buildTitle(config))
            .totalCount(rankings.size())
            .summary(buildSummary(config, rankings))
            .columns(getDefaultColumns())
            .previewData(rankings)
            .build();
    }

    private List<ClassRankingRecord> queryPeriodTotalRanking(StatisticsConfig config) {
        // 聚合查询：按时间范围统计班级总扣分
        String sql = """
            SELECT
                s.class_id,
                s.class_name,
                s.grade_id,
                s.grade_name,
                s.department_id,
                s.department_name,
                SUM(CASE WHEN :scoreType = 'WEIGHTED' AND s.weighted_enabled = 1
                    THEN s.weighted_deduction_score
                    ELSE s.total_deduction_score END) as total_score,
                COUNT(DISTINCT r.id) as check_count,
                AVG(CASE WHEN :scoreType = 'WEIGHTED' AND s.weighted_enabled = 1
                    THEN s.weighted_deduction_score
                    ELSE s.total_deduction_score END) as avg_score
            FROM check_record_class_stats_new s
            JOIN check_records_new r ON s.record_id = r.id
            WHERE r.check_date BETWEEN :startDate AND :endDate
              AND r.plan_id = :planId
              AND r.status = 1
            GROUP BY s.class_id
            ORDER BY total_score ASC
            """;

        // 执行查询并添加排名
        List<ClassRankingRecord> results = executeQuery(sql, config);
        addRanking(results);
        return results;
    }

    @Override
    public List<ExportColumn> getDefaultColumns() {
        return Arrays.asList(
            ExportColumn.of("ranking", "排名", 60),
            ExportColumn.of("className", "班级", 120),
            ExportColumn.of("gradeName", "年级", 80),
            ExportColumn.of("departmentName", "系部", 100),
            ExportColumn.of("totalScore", "总扣分", 80),
            ExportColumn.of("checkCount", "检查次数", 80),
            ExportColumn.of("avgScore", "平均扣分", 80)
        );
    }
}
```

### 3.3 控制器层

```java
@RestController
@RequestMapping("/api/export-center")
@RequiredArgsConstructor
@Slf4j
public class ExportCenterController {

    private final ExportCenterService exportCenterService;

    /**
     * 获取预览数据
     */
    @PostMapping("/preview")
    public Result<ExportPreviewResult> preview(@RequestBody @Valid ExportCenterRequest request) {
        return Result.success(exportCenterService.preview(request));
    }

    /**
     * 导出文件
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody @Valid ExportCenterRequest request) {
        byte[] data = exportCenterService.export(request);
        String fileName = exportCenterService.getFileName(request);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"")
            .header(HttpHeaders.CONTENT_TYPE, request.getFormat().getContentType())
            .body(data);
    }

    /**
     * 获取可用的日常检查列表（用于实时导出）
     */
    @GetMapping("/daily-checks")
    public Result<List<DailyCheckOption>> getDailyChecks(
            @RequestParam Long planId,
            @RequestParam(required = false) Boolean includeCompleted) {
        return Result.success(exportCenterService.getDailyCheckOptions(planId, includeCompleted));
    }

    /**
     * 获取扣分项列表（用于筛选）
     */
    @GetMapping("/deduction-items")
    public Result<List<DeductionItemOption>> getDeductionItems(
            @RequestParam Long dailyCheckId) {
        return Result.success(exportCenterService.getDeductionItemOptions(dailyCheckId));
    }

    /**
     * 获取评级规则列表
     */
    @GetMapping("/rating-rules")
    public Result<List<RatingRuleOption>> getRatingRules(
            @RequestParam Long planId) {
        return Result.success(exportCenterService.getRatingRuleOptions(planId));
    }
}
```

---

## 四、前端设计

### 4.1 设计原则

1. **场景驱动**：用户先选择要做什么，再进行配置
2. **减少选择框**：使用Tab、卡片、Chip等更直观的交互方式
3. **实时预览**：配置变化时立即显示预览效果
4. **渐进式披露**：基础配置展示，高级选项可展开

### 4.2 页面结构

```
ExportCenterView.vue
├── components/
│   ├── ScenarioTabs.vue          # 场景选择Tab
│   ├── RealtimeDeductionPanel.vue # 实时扣分配置面板
│   ├── RatingReportPanel.vue      # 评级通报配置面板
│   ├── StatisticsPanel.vue        # 统计分析配置面板
│   ├── ExportPreview.vue          # 预览区域
│   └── common/
│       ├── CheckSelector.vue      # 检查选择器（卡片式）
│       ├── RoundChips.vue         # 轮次选择（Chip组）
│       ├── ItemSelector.vue       # 扣分项选择（标签式）
│       ├── LevelSelector.vue      # 等级选择（彩色标签）
│       └── DateRangePicker.vue    # 日期范围选择
```

### 4.3 核心组件设计

#### 4.3.1 场景选择 Tab

```vue
<template>
  <div class="scenario-tabs">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      :class="[
        'scenario-tab',
        { 'scenario-tab--active': modelValue === tab.value }
      ]"
      @click="$emit('update:modelValue', tab.value)"
    >
      <component :is="tab.icon" class="scenario-tab__icon" />
      <div class="scenario-tab__content">
        <span class="scenario-tab__title">{{ tab.title }}</span>
        <span class="scenario-tab__desc">{{ tab.description }}</span>
      </div>
    </button>
  </div>
</template>

<script setup lang="ts">
import { FileText, Trophy, BarChart3 } from 'lucide-vue-next'

const tabs = [
  {
    value: 'realtime',
    title: '扣分通报',
    description: '导出扣分学生名单，支持检查进行中导出',
    icon: FileText
  },
  {
    value: 'rating',
    title: '评级通报',
    description: '导出优秀班级、需整改班级等评级结果',
    icon: Trophy
  },
  {
    value: 'statistics',
    title: '统计分析',
    description: '导出班级排名、周期汇总等统计数据',
    icon: BarChart3
  }
]
</script>

<style scoped>
.scenario-tabs {
  display: flex;
  gap: 16px;
  padding: 8px;
  background: #f8fafc;
  border-radius: 16px;
}

.scenario-tab {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: transparent;
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.scenario-tab:hover {
  background: white;
}

.scenario-tab--active {
  background: white;
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.scenario-tab__icon {
  width: 24px;
  height: 24px;
  color: #64748b;
}

.scenario-tab--active .scenario-tab__icon {
  color: #3b82f6;
}

.scenario-tab__content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.scenario-tab__title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.scenario-tab__desc {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}
</style>
```

#### 4.3.2 轮次选择 Chips

```vue
<template>
  <div class="round-chips">
    <span class="round-chips__label">检查轮次</span>
    <div class="round-chips__list">
      <button
        :class="['round-chip', { 'round-chip--active': isAllSelected }]"
        @click="selectAll"
      >
        全部
      </button>
      <button
        v-for="round in rounds"
        :key="round"
        :class="['round-chip', { 'round-chip--active': isSelected(round) }]"
        @click="toggle(round)"
      >
        第{{ round }}轮
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: number[]
  maxRounds: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number[]]
}>()

const rounds = computed(() =>
  Array.from({ length: props.maxRounds }, (_, i) => i + 1)
)

const isAllSelected = computed(() => props.modelValue.length === 0)

const isSelected = (round: number) => props.modelValue.includes(round)

const toggle = (round: number) => {
  if (isSelected(round)) {
    emit('update:modelValue', props.modelValue.filter(r => r !== round))
  } else {
    emit('update:modelValue', [...props.modelValue, round])
  }
}

const selectAll = () => {
  emit('update:modelValue', [])
}
</script>

<style scoped>
.round-chips {
  display: flex;
  align-items: center;
  gap: 12px;
}

.round-chips__label {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  white-space: nowrap;
}

.round-chips__list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.round-chip {
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.round-chip:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.round-chip--active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}
</style>
```

#### 4.3.3 扣分项选择器（标签式）

```vue
<template>
  <div class="item-selector">
    <div class="item-selector__header">
      <span class="item-selector__label">扣分项目</span>
      <button class="item-selector__toggle" @click="toggleAll">
        {{ isAllSelected ? '取消全选' : '全选' }}
      </button>
    </div>
    <div class="item-selector__grid">
      <button
        v-for="item in items"
        :key="item.id"
        :class="['item-tag', { 'item-tag--selected': isSelected(item.id) }]"
        @click="toggle(item.id)"
      >
        <span class="item-tag__name">{{ item.name }}</span>
        <span v-if="item.count" class="item-tag__count">{{ item.count }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface DeductionItem {
  id: number
  name: string
  count?: number
}

const props = defineProps<{
  modelValue: number[]
  items: DeductionItem[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number[]]
}>()

const isAllSelected = computed(() =>
  props.modelValue.length === props.items.length
)

const isSelected = (id: number) => props.modelValue.includes(id)

const toggle = (id: number) => {
  if (isSelected(id)) {
    emit('update:modelValue', props.modelValue.filter(i => i !== id))
  } else {
    emit('update:modelValue', [...props.modelValue, id])
  }
}

const toggleAll = () => {
  if (isAllSelected.value) {
    emit('update:modelValue', [])
  } else {
    emit('update:modelValue', props.items.map(i => i.id))
  }
}
</script>

<style scoped>
.item-selector__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.item-selector__label {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.item-selector__toggle {
  font-size: 13px;
  color: #3b82f6;
  background: none;
  border: none;
  cursor: pointer;
}

.item-selector__grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.item-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 13px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.item-tag:hover {
  background: #f1f5f9;
}

.item-tag--selected {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #1d4ed8;
}

.item-tag__name {
  font-weight: 500;
}

.item-tag__count {
  padding: 2px 6px;
  font-size: 11px;
  font-weight: 600;
  background: #e2e8f0;
  border-radius: 10px;
}

.item-tag--selected .item-tag__count {
  background: #bfdbfe;
  color: #1d4ed8;
}
</style>
```

#### 4.3.4 评级等级选择器

```vue
<template>
  <div class="level-selector">
    <span class="level-selector__label">导出等级</span>
    <div class="level-selector__list">
      <button
        v-for="level in levels"
        :key="level.id"
        :class="['level-tag', { 'level-tag--selected': isSelected(level.id) }]"
        :style="getStyle(level)"
        @click="toggle(level.id)"
      >
        <span v-if="level.icon" class="level-tag__icon">{{ level.icon }}</span>
        {{ level.name }}
        <span class="level-tag__count">{{ level.count }}个班级</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
interface RatingLevel {
  id: number
  name: string
  color: string
  icon?: string
  count: number
}

const props = defineProps<{
  modelValue: number[]
  levels: RatingLevel[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number[]]
}>()

const isSelected = (id: number) => props.modelValue.includes(id)

const toggle = (id: number) => {
  if (isSelected(id)) {
    emit('update:modelValue', props.modelValue.filter(i => i !== id))
  } else {
    emit('update:modelValue', [...props.modelValue, id])
  }
}

const getStyle = (level: RatingLevel) => {
  if (isSelected(level.id)) {
    return {
      background: level.color + '20',
      borderColor: level.color,
      color: level.color
    }
  }
  return {}
}
</script>

<style scoped>
.level-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.level-selector__label {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.level-selector__list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.level-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 500;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.level-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.level-tag__icon {
  font-size: 16px;
}

.level-tag__count {
  font-size: 12px;
  font-weight: 400;
  opacity: 0.7;
}
</style>
```

### 4.4 主页面布局

```vue
<template>
  <div class="export-center">
    <!-- 顶部标题栏 -->
    <header class="export-center__header">
      <button class="back-button" @click="goBack">
        <ArrowLeft class="w-5 h-5" />
      </button>
      <div class="header-title">
        <h1>导出中心</h1>
        <span class="header-subtitle">{{ planName }}</span>
      </div>
    </header>

    <!-- 场景选择 -->
    <section class="export-center__section">
      <ScenarioTabs v-model="currentScenario" />
    </section>

    <!-- 配置区域 -->
    <section class="export-center__section">
      <div class="config-panel">
        <component
          :is="currentPanel"
          :plan-id="planId"
          v-model:config="currentConfig"
          @preview="handlePreview"
        />
      </div>
    </section>

    <!-- 预览区域 -->
    <section class="export-center__section" v-if="previewData">
      <ExportPreview
        :data="previewData"
        :loading="loading"
        @export="handleExport"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import ScenarioTabs from './components/ScenarioTabs.vue'
import RealtimeDeductionPanel from './components/RealtimeDeductionPanel.vue'
import RatingReportPanel from './components/RatingReportPanel.vue'
import StatisticsPanel from './components/StatisticsPanel.vue'
import ExportPreview from './components/ExportPreview.vue'
import { preview, exportFile } from '@/api/exportCenter'
import type { ExportPreviewResult, ExportScenarioConfig } from '@/types/export'

const route = useRoute()
const router = useRouter()

const planId = computed(() => route.params.planId as string)
const planName = ref('检查计划')

const currentScenario = ref<'realtime' | 'rating' | 'statistics'>('realtime')
const currentConfig = ref<ExportScenarioConfig | null>(null)
const previewData = ref<ExportPreviewResult | null>(null)
const loading = ref(false)

const currentPanel = computed(() => {
  switch (currentScenario.value) {
    case 'realtime': return RealtimeDeductionPanel
    case 'rating': return RatingReportPanel
    case 'statistics': return StatisticsPanel
    default: return RealtimeDeductionPanel
  }
})

// 切换场景时清空预览
watch(currentScenario, () => {
  previewData.value = null
  currentConfig.value = null
})

const handlePreview = async () => {
  if (!currentConfig.value) return

  loading.value = true
  try {
    previewData.value = await preview({
      scenario: currentScenario.value,
      config: currentConfig.value
    })
  } finally {
    loading.value = false
  }
}

const handleExport = async (format: 'EXCEL' | 'WORD' | 'PDF') => {
  if (!currentConfig.value) return

  loading.value = true
  try {
    await exportFile({
      scenario: currentScenario.value,
      format,
      config: currentConfig.value
    })
  } finally {
    loading.value = false
  }
}

const goBack = () => router.back()
</script>

<style scoped>
.export-center {
  min-height: 100vh;
  background: #f8fafc;
  padding: 24px;
}

.export-center__header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.back-button:hover {
  background: #f1f5f9;
}

.header-title h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.header-subtitle {
  font-size: 14px;
  color: #64748b;
}

.export-center__section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.config-panel {
  min-height: 200px;
}
</style>
```

---

## 五、实施计划

### 5.1 开发阶段

| 阶段 | 任务 | 优先级 |
|------|------|--------|
| **阶段1** | 后端策略框架 + 实时扣分导出 | P0 |
| **阶段2** | 前端导出中心基础框架 | P0 |
| **阶段3** | 评级通报导出 | P1 |
| **阶段4** | 统计分析导出 | P1 |
| **阶段5** | 导出模板保存与管理 | P2 |

### 5.2 文件清单

#### 后端新增文件

```
backend/src/main/java/com/school/management/
├── controller/
│   └── ExportCenterController.java
├── service/
│   ├── ExportCenterService.java
│   └── impl/
│       └── ExportCenterServiceImpl.java
├── service/export/
│   ├── ExportStrategy.java
│   ├── ExportStrategyFactory.java
│   ├── RealtimeDeductionExportStrategy.java
│   ├── RatingReportExportStrategy.java
│   └── StatisticsExportStrategy.java
├── dto/export/
│   ├── ExportCenterRequest.java
│   ├── ExportScenarioConfig.java
│   ├── RealtimeDeductionConfig.java
│   ├── RatingReportConfig.java
│   ├── StatisticsConfig.java
│   ├── ExportPreviewResult.java
│   └── ExportColumn.java
└── enums/
    ├── ExportScenario.java
    └── ExportFormat.java
```

#### 前端新增文件

```
frontend/src/
├── views/quantification/
│   └── ExportCenterView.vue
├── views/quantification/components/export/
│   ├── ScenarioTabs.vue
│   ├── RealtimeDeductionPanel.vue
│   ├── RatingReportPanel.vue
│   ├── StatisticsPanel.vue
│   ├── ExportPreview.vue
│   └── common/
│       ├── CheckSelector.vue
│       ├── RoundChips.vue
│       ├── ItemSelector.vue
│       ├── LevelSelector.vue
│       └── DateRangePicker.vue
├── api/
│   └── exportCenter.ts
└── types/
    └── export.ts
```

### 5.3 API 端点汇总

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/export-center/preview` | 获取预览数据 |
| POST | `/api/export-center/export` | 导出文件 |
| GET | `/api/export-center/daily-checks` | 获取日常检查列表 |
| GET | `/api/export-center/deduction-items` | 获取扣分项列表 |
| GET | `/api/export-center/rating-rules` | 获取评级规则列表 |
| GET | `/api/export-center/statistics-options` | 获取统计选项 |

---

## 六、与现有系统的兼容性

### 6.1 现有导出功能处理

**建议**：保留现有的 `CheckExportTemplateController` 和相关功能，新的导出中心作为增强版本并行存在。

**理由**：
1. 现有功能已稳定运行，避免引入风险
2. 部分用户可能已习惯现有方式
3. 后续可逐步迁移，最终废弃旧接口

### 6.2 入口位置

在以下位置添加"导出中心"入口：

1. **检查计划详情页** - 顶部操作栏
2. **日常检查页** - 操作按钮区域
3. **检查记录详情页** - 顶部操作栏
4. **统计分析页** - 导出按钮替换

---

## 七、疑问与讨论点

### 7.1 待确认事项

1. **评级维度**：当前评级规则是否有明确的"维度"概念（如卫生/纪律）？
   - 如果没有，需要扩展 `CheckPlanRatingRule` 表增加 `dimension` 字段
   - 或者通过规则名称约定来区分

2. **混寝分摊**：实时导出时，是否需要体现混寝分摊逻辑？
   - 当前 `DailyCheckDetail` 有 `scoreRatio` 字段
   - 导出时是否按比例分摊显示？

3. **权限控制**：导出中心是否需要单独的权限点？
   - 建议：复用现有的检查计划查看权限

4. **大数据量处理**：预计单次导出最大数据量？
   - 如果超过1000条，建议增加异步导出功能

### 7.2 我的建议

1. **评级维度**：建议在评级规则中增加 `dimension` 或 `category` 字段，明确支持多维度评级，这样可以更灵活地支持"卫生优秀"、"纪律优秀"等场景。

2. **文档模板**：考虑提供几个预设的通报模板，用户可以直接使用或在此基础上修改，降低配置门槛。

3. **导出历史**：建议增加导出历史记录表，便于追溯和重复下载。

---

## 八、混寝分摊处理方案

### 8.1 现有数据结构

`DailyCheckDetail` 表已有完善的混寝分摊字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalStudents` | INT | 宿舍总人数 |
| `classStudents` | INT | 该班在宿舍的人数 |
| `scoreRatio` | DECIMAL(5,4) | 分摊比例 = classStudents / totalStudents |
| `deductScore` | DECIMAL | **已分摊后的实际扣分** |
| `linkType` | INT | 1=宿舍关联（混寝标记） |
| `linkNo` | VARCHAR | 宿舍号 |

### 8.2 导出处理策略

**核心原则**：导出时直接使用 `deductScore`（已分摊后的分数），同时标注混寝信息。

```java
/**
 * 扣分记录导出DTO - 包含混寝信息
 */
@Data
public class DeductionExportRecord {
    private String studentNo;
    private String studentName;
    private String className;
    private String deductionItemName;

    // 实际扣分（已分摊）
    private BigDecimal deductScore;

    // 混寝标记
    private Boolean isMixedDorm;      // 是否混寝
    private String dormitoryNo;        // 宿舍号
    private String shareInfo;          // 分摊说明，如 "2/4人分摊"

    private String remark;
}
```

### 8.3 导出列配置

| 列名 | 字段 | 说明 | 是否可选 |
|------|------|------|---------|
| 序号 | index | 自增序号 | 必选 |
| 学号 | studentNo | 学生学号 | 必选 |
| 姓名 | studentName | 学生姓名 | 必选 |
| 班级 | className | 所在班级 | 必选 |
| 扣分项目 | deductionItemName | 扣分项名称 | 必选 |
| 扣分 | deductScore | 已分摊后的扣分 | 必选 |
| 宿舍号 | dormitoryNo | 混寝时显示 | 可选 |
| 分摊 | shareInfo | 如"2/4人" | 可选 |
| 备注 | remark | 扣分备注 | 可选 |

### 8.4 前端显示

混寝记录在预览时使用特殊标记：

```vue
<template>
  <tr v-for="record in records" :key="record.id">
    <td>{{ record.studentName }}</td>
    <td>{{ record.className }}</td>
    <td>{{ record.deductionItemName }}</td>
    <td>
      {{ record.deductScore }}
      <!-- 混寝标记 -->
      <span v-if="record.isMixedDorm" class="mixed-dorm-badge">
        {{ record.dormitoryNo }} ({{ record.shareInfo }})
      </span>
    </td>
  </tr>
</template>

<style scoped>
.mixed-dorm-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 6px;
  font-size: 11px;
  color: #7c3aed;
  background: #ede9fe;
  border-radius: 4px;
}
</style>
```

---

## 九、大数据量导出方案

### 9.1 阈值定义

| 数据量 | 处理方式 | 响应时间 |
|--------|---------|---------|
| < 500条 | 同步导出 | < 3秒 |
| 500-2000条 | 同步导出+进度提示 | 3-10秒 |
| > 2000条 | 异步导出 | 后台处理 |

### 9.2 异步导出架构

```
用户点击导出
     │
     ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  创建任务   │────▶│  任务队列   │────▶│  后台处理   │
│  返回任务ID │     │  (Redis)    │     │  (异步线程) │
└─────────────┘     └─────────────┘     └──────┬──────┘
     │                                         │
     ▼                                         ▼
┌─────────────┐                         ┌─────────────┐
│  前端轮询   │◀────────────────────────│  生成文件   │
│  任务状态   │                         │  存储OSS    │
└─────────────┘                         └─────────────┘
     │
     ▼
┌─────────────┐
│  下载文件   │
└─────────────┘
```

### 9.3 数据库设计

```sql
-- 导出任务表
CREATE TABLE export_tasks (
  id BIGINT PRIMARY KEY,

  -- 任务信息
  task_code VARCHAR(32) NOT NULL COMMENT '任务编号',
  export_type VARCHAR(30) NOT NULL COMMENT '导出类型',
  export_format VARCHAR(10) NOT NULL COMMENT '导出格式',
  config_json TEXT COMMENT '导出配置JSON',

  -- 状态信息
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/COMPLETED/FAILED',
  progress INT DEFAULT 0 COMMENT '进度百分比',
  total_count INT COMMENT '总记录数',
  processed_count INT DEFAULT 0 COMMENT '已处理数',

  -- 结果信息
  file_path VARCHAR(500) COMMENT '文件存储路径',
  file_name VARCHAR(200) COMMENT '文件名',
  file_size BIGINT COMMENT '文件大小(字节)',
  download_url VARCHAR(500) COMMENT '下载链接',
  expire_time DATETIME COMMENT '链接过期时间',

  -- 错误信息
  error_message TEXT COMMENT '错误信息',

  -- 审计字段
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME,

  INDEX idx_task_code (task_code),
  INDEX idx_status (status),
  INDEX idx_created_by (created_by)
) COMMENT '导出任务表';
```

### 9.4 后端实现

#### 9.4.1 异步导出服务

```java
@Service
@Slf4j
public class AsyncExportService {

    private final ExportTaskMapper taskMapper;
    private final ExportStrategyFactory strategyFactory;
    private final FileStorageService fileStorageService;
    private final ThreadPoolTaskExecutor exportExecutor;

    /**
     * 创建异步导出任务
     */
    @Transactional
    public ExportTaskDTO createAsyncExport(ExportCenterRequest request, Long userId) {
        // 1. 创建任务记录
        ExportTask task = new ExportTask();
        task.setTaskCode(generateTaskCode());
        task.setExportType(request.getScenario().getCode());
        task.setExportFormat(request.getFormat().name());
        task.setConfigJson(toJson(request.getConfig()));
        task.setStatus("PENDING");
        task.setCreatedBy(userId);
        taskMapper.insert(task);

        // 2. 提交异步任务
        exportExecutor.execute(() -> processExportTask(task.getId()));

        // 3. 返回任务信息
        return toDTO(task);
    }

    /**
     * 处理导出任务
     */
    @Async("exportExecutor")
    public void processExportTask(Long taskId) {
        ExportTask task = taskMapper.selectById(taskId);

        try {
            // 更新状态为处理中
            task.setStatus("PROCESSING");
            taskMapper.updateById(task);

            // 获取策略并执行导出
            ExportStrategy strategy = strategyFactory.getStrategy(
                ExportScenario.fromCode(task.getExportType())
            );

            ExportScenarioConfig config = parseConfig(task.getConfigJson());

            // 分批处理，更新进度
            byte[] data = strategy.exportWithProgress(config, progress -> {
                task.setProgress(progress);
                task.setProcessedCount((int)(task.getTotalCount() * progress / 100));
                taskMapper.updateById(task);
            });

            // 保存文件
            String fileName = strategy.getFileName(config,
                ExportFormat.valueOf(task.getExportFormat()));
            String filePath = fileStorageService.saveExportFile(data, fileName);

            // 生成下载链接（7天有效）
            String downloadUrl = fileStorageService.generateDownloadUrl(filePath, 7);

            // 更新任务完成
            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setFilePath(filePath);
            task.setFileName(fileName);
            task.setFileSize((long) data.length);
            task.setDownloadUrl(downloadUrl);
            task.setExpireTime(LocalDateTime.now().plusDays(7));
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            // 发送通知（WebSocket或消息推送）
            notifyUser(task.getCreatedBy(), task);

        } catch (Exception e) {
            log.error("导出任务失败: taskId={}", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            taskMapper.updateById(task);
        }
    }

    /**
     * 查询任务状态
     */
    public ExportTaskDTO getTaskStatus(String taskCode) {
        ExportTask task = taskMapper.selectByTaskCode(taskCode);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return toDTO(task);
    }
}
```

#### 9.4.2 策略接口扩展

```java
public interface ExportStrategy<C extends ExportScenarioConfig> {

    // ... 其他方法 ...

    /**
     * 带进度回调的导出
     */
    default byte[] exportWithProgress(C config, Consumer<Integer> progressCallback) {
        progressCallback.accept(10);

        // 查询数据
        List<?> data = fetchData(config);
        progressCallback.accept(50);

        // 生成文件
        byte[] result = generateFile(data, config);
        progressCallback.accept(100);

        return result;
    }

    /**
     * 获取预估记录数（用于判断是否需要异步）
     */
    default int estimateCount(C config) {
        return 0; // 默认返回0，由具体策略实现
    }
}
```

### 9.5 前端实现

#### 9.5.1 导出按钮组件

```vue
<template>
  <div class="export-button-group">
    <!-- 同步导出按钮 -->
    <button
      v-if="!isLargeExport"
      :class="['export-btn', { 'export-btn--loading': exporting }]"
      :disabled="exporting"
      @click="handleSyncExport"
    >
      <Download v-if="!exporting" class="w-4 h-4" />
      <Loader2 v-else class="w-4 h-4 animate-spin" />
      <span>{{ exporting ? '导出中...' : '导出' }}</span>
    </button>

    <!-- 异步导出按钮 -->
    <button
      v-else
      class="export-btn"
      @click="handleAsyncExport"
    >
      <FileDown class="w-4 h-4" />
      <span>后台导出</span>
    </button>

    <!-- 格式选择下拉 -->
    <div class="format-dropdown">
      <button class="format-btn" @click="showFormatMenu = !showFormatMenu">
        {{ currentFormat }}
        <ChevronDown class="w-4 h-4" />
      </button>
      <div v-if="showFormatMenu" class="format-menu">
        <button @click="selectFormat('EXCEL')">Excel (.xlsx)</button>
        <button @click="selectFormat('WORD')">Word (.docx)</button>
        <button @click="selectFormat('PDF')">PDF (.pdf)</button>
      </div>
    </div>
  </div>

  <!-- 异步任务进度弹窗 -->
  <Teleport to="body">
    <div v-if="asyncTask" class="export-progress-modal">
      <div class="progress-card">
        <div class="progress-header">
          <FileText class="w-6 h-6 text-blue-500" />
          <span>正在生成导出文件</span>
        </div>

        <div class="progress-body">
          <div class="progress-bar">
            <div
              class="progress-fill"
              :style="{ width: asyncTask.progress + '%' }"
            ></div>
          </div>
          <div class="progress-text">
            {{ asyncTask.processedCount }} / {{ asyncTask.totalCount }} 条
          </div>
        </div>

        <div v-if="asyncTask.status === 'COMPLETED'" class="progress-footer">
          <a :href="asyncTask.downloadUrl" class="download-link">
            <Download class="w-4 h-4" />
            下载文件
          </a>
          <span class="expire-tip">链接7天内有效</span>
        </div>

        <button class="close-btn" @click="asyncTask = null">
          <X class="w-4 h-4" />
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { Download, Loader2, FileDown, ChevronDown, FileText, X } from 'lucide-vue-next'
import { createAsyncExport, getTaskStatus } from '@/api/exportCenter'
import type { ExportTaskDTO } from '@/types/export'

const props = defineProps<{
  config: any
  scenario: string
  estimatedCount: number
}>()

const emit = defineEmits<{
  'export': [format: string]
}>()

const exporting = ref(false)
const currentFormat = ref('Excel')
const showFormatMenu = ref(false)
const asyncTask = ref<ExportTaskDTO | null>(null)
let pollTimer: number | null = null

// 是否需要异步导出
const isLargeExport = computed(() => props.estimatedCount > 2000)

const handleSyncExport = () => {
  emit('export', currentFormat.value)
}

const handleAsyncExport = async () => {
  try {
    const task = await createAsyncExport({
      scenario: props.scenario,
      format: currentFormat.value,
      config: props.config
    })
    asyncTask.value = task
    startPolling(task.taskCode)
  } catch (e) {
    console.error('创建导出任务失败', e)
  }
}

const startPolling = (taskCode: string) => {
  pollTimer = window.setInterval(async () => {
    try {
      const status = await getTaskStatus(taskCode)
      asyncTask.value = status

      if (status.status === 'COMPLETED' || status.status === 'FAILED') {
        stopPolling()
      }
    } catch (e) {
      stopPolling()
    }
  }, 2000) // 每2秒轮询一次
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.export-button-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.export-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  color: white;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.export-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
}

.export-btn--loading {
  opacity: 0.8;
  cursor: wait;
}

.export-progress-modal {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
}

.progress-card {
  position: relative;
  width: 320px;
  padding: 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-weight: 600;
  color: #1e293b;
}

.progress-bar {
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #1d4ed8);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  margin-top: 8px;
  font-size: 13px;
  color: #64748b;
  text-align: center;
}

.progress-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.download-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  color: white;
  background: #10b981;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s ease;
}

.download-link:hover {
  background: #059669;
}

.expire-tip {
  font-size: 12px;
  color: #94a3b8;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px;
  color: #94a3b8;
  background: none;
  border: none;
  cursor: pointer;
}
</style>
```

### 9.6 配置参数

```yaml
# application.yml
export:
  # 同步导出阈值
  sync-threshold: 500
  # 异步导出阈值
  async-threshold: 2000
  # 文件保留天数
  file-retention-days: 7
  # 线程池配置
  thread-pool:
    core-size: 2
    max-size: 5
    queue-capacity: 100
```

---

## 十、总结

本设计方案通过统一的"导出中心"解决了三种复杂场景的导出需求：

1. **实时扣分导出**：支持检查进行中导出，可按轮次、扣分项灵活筛选
2. **评级通报导出**：利用现有评级规则的高度自定义能力（按类别评级），支持多维度评级结果导出
3. **统计分析导出**：支持单次排名和周期汇总统计导出

### 关键设计决策

| 问题 | 解决方案 |
|------|---------|
| **混寝分摊** | 直接使用已分摊的 `deductScore`，增加混寝标记列显示宿舍号和分摊比例 |
| **评级维度** | 复用现有的 `scoreSource=CATEGORY` + `categoryId` 机制，通过创建多个规则实现多维度评级 |
| **大数据量** | 阈值判断 + 异步导出 + 进度轮询 + 下载链接（7天有效） |

### 前端设计特点

- 场景驱动的交互设计，减少选择框使用
- 卡片式、标签式选择和实时预览
- 异步导出时的浮动进度卡片
- 符合现代、简约、美观的设计要求

### 后端设计特点

- 策略模式便于扩展新的导出场景
- 复用现有的数据查询能力
- 异步任务支持断点续传和重复下载
- 最小化对现有代码的侵入
