# 事件系统重构设计方案

> 日期: 2026-04-07
> 范围: 检查平台评分事件提取 + 触发器标准化 + 消息系统全链路

---

## 1. 背景与目标

### 当前问题

1. **检查平台事件提取**: 仅处理 VIOLATION_RECORD 类型，其他 21 种检查项类型（通过不通过、打分、评级、人次扣分等）的扣分不触发任何事件
2. **硬编码绕过触发器**: `publishInspectionEvents()` 直接调 `entityEventApplicationService.createEvent()`，绕过触发器系统，管理员配置无效
3. **触发器条件不可靠**: `result == "扣分"` 无法适配多种评分方式
4. **JSON 嵌套数据**: VIOLATION_RECORD 和 PERSON_SCORE 的多主体数据压在 responseValue JSON 里，每次使用都要解析，无法 SQL 查询
5. **缺乏统计分析基础**: "哪个班扣分最多""哪个检查项问题最多"等查询需要解析 JSON，性能极差

### 目标

- 所有 22 种检查项类型的扣分统一接入事件系统
- 归一化评分观察数据，支持事件触发 + 统计分析双用途
- 可扩展：新增检查项类型只需加一个 Extractor 类
- 标准化 context 字段，触发器条件通用可靠

---

## 2. 架构设计

### 2.1 总体架构

```
评分引擎 (ScoreAggregation)
    │
    ├── 计算每个 SubmissionDetail 的 score / isFlagged
    │
    ↓
ObservationExtractor (Strategy 模式)
    ├── DefaultExtractor          → 1 detail → 0~1 observation
    ├── ViolationRecordExtractor  → 1 detail → N observations (逐条违规)
    ├── PersonScoreExtractor      → 1 detail → N observations (逐人)
    └── (未来新类型...)           → 加一个 @Component 即可
    │
    ↓
submission_observations 表 (归一化读模型)
    │ 结构化存储，每条 = 一个主体的一次评分观察
    │
    ├──→ 事件触发: WHERE is_negative = true → triggerService.fire()
    │      context 标准字段: isNegative, severity, eventTypeHint
    │      → 触发器匹配 → entity_events → MessageDispatcher → 通知
    │
    └──→ 统计分析: SQL 直接查询（扣分排行、趋势、分布等）
```

### 2.2 数据流

```
Inspector 提交检查
    ↓
InspSubmissionApplicationService.completeSubmission()
    ├── 1. 计算评分 (ScoreAggregationService)
    ├── 2. 保存 Submission (status=COMPLETED, score, grade)
    ├── 3. 提取观察 (ObservationExtractors)
    │       遍历每个 SubmissionDetail
    │       找到匹配的 Extractor
    │       提取 0~N 个 ScoringObservation
    ├── 4. 批量写入 submission_observations
    ├── 5. 触发事件 (observations WHERE is_negative)
    │       triggerService.fire("INSP_ITEM_RESULT", obs.toContext())
    ├── 6. 等级事件
    │       triggerService.fire("INSP_GRADE_RESULT", gradeContext)
    └── 7. 完成事件
            triggerService.fire("INSP_RECORD_COMPLETE", completeContext)
```

---

## 3. 数据库设计

### 3.1 新增表: submission_observations

```sql
CREATE TABLE submission_observations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,

    -- 关联 (支持快速查询)
    submission_id BIGINT NOT NULL,
    detail_id BIGINT NOT NULL,
    project_id BIGINT COMMENT '冗余: 加速按项目查询',
    task_id BIGINT COMMENT '冗余: 加速按任务查询',

    -- 检查项信息
    item_code VARCHAR(50),
    item_name VARCHAR(200),
    item_type VARCHAR(30),
    section_name VARCHAR(200) COMMENT '所属板块名称',

    -- 主体 (谁被观察/打分)
    subject_type VARCHAR(20) NOT NULL COMMENT 'USER/ORG_UNIT/PLACE',
    subject_id BIGINT NOT NULL,
    subject_name VARCHAR(100),
    class_id BIGINT COMMENT '冗余: 主体所属班级',
    class_name VARCHAR(100),

    -- 评分结果
    score DECIMAL(10,2) NOT NULL DEFAULT 0,
    is_negative TINYINT NOT NULL DEFAULT 0,
    severity VARCHAR(10) COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    is_flagged TINYINT NOT NULL DEFAULT 0,

    -- 事件关联
    linked_event_type_code VARCHAR(50),

    -- 原始数据
    response_value TEXT COMMENT '原始输入',
    description VARCHAR(500),

    -- 时间
    observed_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_submission (submission_id),
    INDEX idx_detail (detail_id),
    INDEX idx_subject (subject_type, subject_id),
    INDEX idx_class (class_id),
    INDEX idx_project (project_id),
    INDEX idx_negative (is_negative, severity),
    INDEX idx_event_type (linked_event_type_code),
    INDEX idx_observed (observed_at)
) COMMENT='评分观察记录(检查平台归一化读模型)';
```

### 3.2 更新: trigger_points contextSchema

为 INSP_ITEM_RESULT 添加标准字段注解:

```json
{
  "isNegative":    {"type":"Boolean","label":"是否负面","role":"standard"},
  "severity":      {"type":"String","label":"严重程度","role":"standard"},
  "eventTypeHint": {"type":"String","label":"事件类型","role":"eventType"},
  "studentId":     {"type":"Long","label":"学生ID","subject":"USER","role":"id"},
  "studentName":   {"type":"String","label":"学生姓名","subject":"USER","role":"name"},
  "classId":       {"type":"Long","label":"班级ID","subject":"ORG_UNIT","role":"id"},
  "className":     {"type":"String","label":"班级名称","subject":"ORG_UNIT","role":"name"},
  "placeId":       {"type":"Long","label":"场所ID","subject":"PLACE","role":"id"},
  "placeName":     {"type":"String","label":"场所名称","subject":"PLACE","role":"name"},
  "itemName":      {"type":"String","label":"检查项"},
  "score":         {"type":"Number","label":"分数"},
  "projectName":   {"type":"String","label":"项目名称"}
}
```

### 3.3 更新: 默认触发器条件

```sql
-- 旧: {"result":"扣分"}  → 不可靠
-- 新: {"isNegative": true} → 所有模块通用，100% 可靠
UPDATE event_triggers
SET condition_json = '{"isNegative": true}'
WHERE trigger_point_code = 'INSP_ITEM_RESULT' AND deleted = 0;
```

---

## 4. 后端设计

### 4.1 领域模型: ScoringObservation

```
位置: domain/inspection/model/v7/execution/ScoringObservation.java
```

```java
@Value
@Builder
public class ScoringObservation {
    String subjectType;       // USER / ORG_UNIT / PLACE
    Long subjectId;
    String subjectName;
    Long classId;             // 主体所属班级(冗余)
    String className;
    BigDecimal score;
    boolean negative;         // score < 0 || flagged
    String severity;          // LOW / MEDIUM / HIGH / CRITICAL
    boolean flagged;
    String linkedEventType;   // 从 templateItem.linked_event_type_code
    String itemCode;
    String itemName;
    String itemType;
    String sectionName;
    String responseValue;     // 原始输入
    String description;       // 描述/备注
    LocalDateTime observedAt;

    /** 转换为触发器 context Map */
    public Map<String, Object> toContextMap() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("isNegative", negative);
        ctx.put("severity", severity);
        ctx.put("eventTypeHint", linkedEventType);
        ctx.put("subjectType", subjectType);  // 供触发器内部使用
        if ("USER".equals(subjectType)) {
            ctx.put("studentId", subjectId);
            ctx.put("studentName", subjectName);
        }
        ctx.put("classId", classId);
        ctx.put("className", className);
        ctx.put("itemName", itemName);
        ctx.put("score", score);
        ctx.put("description", description);
        return ctx;
    }
}
```

### 4.2 Strategy 接口: ObservationExtractor

```
位置: domain/inspection/service/ObservationExtractor.java
```

```java
/**
 * 从 SubmissionDetail 提取评分观察
 * 每种需要特殊处理的检查项类型实现一个，其余走 DefaultExtractor
 */
public interface ObservationExtractor {

    /** 是否支持该检查项类型 (返回 true 表示优先使用) */
    boolean supports(String itemType);

    /** 提取观察列表 (1 detail → 0~N observations) */
    List<ScoringObservation> extract(SubmissionDetail detail, ObservationContext ctx);
}
```

### 4.3 ObservationContext (提取上下文)

```java
@Value
@Builder
public class ObservationContext {
    Long submissionId;
    Long projectId;
    Long taskId;
    String targetType;      // 检查对象类型
    Long targetId;          // 检查对象ID
    String targetName;      // 检查对象名称
    Long classId;           // 检查对象所属班级
    String className;
    LocalDateTime observedAt;

    /** 查询检查项关联的事件类型编码 */
    String getLinkedEventType(SubmissionDetail detail);
}
```

### 4.4 三个 Extractor 实现

#### DefaultObservationExtractor (覆盖 90% 类型)

```
支持: PASS_FAIL, SELECT, RADIO, MULTI_SELECT, CHECKBOX,
     NUMBER, SLIDER, RATING, CHECKLIST, CALCULATED,
     TEXT/TEXTAREA/RICH_TEXT (仅 flagged 时), PHOTO/VIDEO 等
逻辑: score < 0 || isFlagged → 1 个观察, 主体=检查对象
     否则 → 0 个观察
```

#### ViolationRecordObservationExtractor

```
支持: VIOLATION_RECORD
逻辑: 解析 responseValue JSON 数组
     每条违规记录 → 1 个观察, 主体=记录中的学生
     severity 从 record.severity 映射
```

#### PersonScoreObservationExtractor

```
支持: PERSON_SCORE
逻辑: 解析 responseValue JSON 数组
     每个负分人员 → 1 个观察, 主体=该人员
     正分人员跳过
```

### 4.5 severity 计算规则

```java
public static String calcSeverity(BigDecimal score, boolean isFlagged) {
    if (isFlagged && score == null) return "MEDIUM";
    if (score == null) return "LOW";
    double abs = score.abs().doubleValue();
    if (abs >= 10) return "CRITICAL";
    if (abs >= 5)  return "HIGH";
    if (abs >= 2)  return "MEDIUM";
    return "LOW";
}

// 违纪记录的 severity 直接从记录的 severity 字段映射
public static String mapViolationSeverity(String severity) {
    return switch (severity) {
        case "SEVERE"   -> "CRITICAL";
        case "MODERATE" -> "HIGH";
        case "MINOR"    -> "MEDIUM";
        default         -> "LOW";
    };
}
```

### 4.6 InspSubmissionApplicationService 改造

```java
// 注入
private final List<ObservationExtractor> extractors;
private final SubmissionObservationRepository observationRepository;
private final TriggerService triggerService;

// completeSubmission() 中替换 publishInspectionEvents()
private void publishObservationsAndEvents(InspSubmission submission,
                                           List<SubmissionDetail> details,
                                           InspProject project,
                                           InspTask task) {
    ObservationContext ctx = buildObservationContext(submission, project, task);

    // ── 1. 提取并存储观察 ──
    List<ScoringObservation> allObservations = new ArrayList<>();
    for (SubmissionDetail detail : details) {
        ObservationExtractor extractor = findExtractor(detail.getItemType());
        allObservations.addAll(extractor.extract(detail, ctx));
    }
    observationRepository.batchInsert(submission.getId(), allObservations);

    // ── 2. 负面观察 → 触发事件 ──
    for (ScoringObservation obs : allObservations) {
        if (!obs.isNegative()) continue;
        Map<String, Object> context = obs.toContextMap();
        context.put("_refType", "inspection_submission");
        context.put("_refId", submission.getId());
        context.put("projectName", project.getProjectName());
        triggerService.fire("INSP_ITEM_RESULT", context);
    }

    // ── 3. 等级事件 ──
    if (submission.getGrade() != null) {
        triggerService.fire("INSP_GRADE_RESULT", Map.of(
            "isNegative", !Boolean.TRUE.equals(submission.getPassed()),
            "severity", gradeSeverity(submission.getGrade(), submission.getPassed()),
            "eventTypeHint", lookupGradeEventType(submission.getGrade(), project),
            "targetId", submission.getTargetId(),
            "targetName", submission.getTargetName(),
            "targetType", submission.getTargetType(),
            "score", submission.getFinalScore(),
            "grade", submission.getGrade(),
            "passed", submission.getPassed(),
            "projectName", project.getProjectName(),
            "_refType", "inspection_submission",
            "_refId", submission.getId()
        ));
    }

    // ── 4. 完成事件 ──
    triggerService.fire("INSP_RECORD_COMPLETE", Map.of(
        "isNegative", false,
        "taskId", task.getId(),
        "targetId", submission.getTargetId(),
        "targetName", submission.getTargetName(),
        "score", submission.getFinalScore(),
        "inspectorName", SecurityUtils.getCurrentUsername(),
        "_refType", "inspection_submission",
        "_refId", submission.getId()
    ));
}

private ObservationExtractor findExtractor(String itemType) {
    return extractors.stream()
        .filter(e -> e.supports(itemType))
        .findFirst()
        .orElse(defaultExtractor);
}
```

### 4.7 ConditionMatcher 适配

确保支持 boolean 值匹配:

```java
// isNegative == true 的匹配
// ConditionMatcher 已支持 == 运算符
// 需确认: context 中 Boolean true 与条件 JSON 中的 true 能正确比较
```

---

## 5. 前端设计

### 5.1 检查模板编辑 — 检查项事件类型绑定

**位置**: 检查模板编辑页面，每个检查项的配置面板中

**UI 风格**: 遵循 CourseListView 设计规范

```
检查项编辑面板:
┌─────────────────────────────────────────────┐
│  检查项: 迟到                                │
│  响应类型: 有/无                              │
│  评分模式: 扣分                               │
│  ...                                         │
│                                              │
│  ┌─ 事件关联 ──────────────────────────────┐ │
│  │  关联事件类型: [迟到 (LATE) ▼]           │ │
│  │                                         │ │
│  │  选择后，该检查项扣分时将自动触发         │ │
│  │  对应类型的事件记录                       │ │
│  └─────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

**交互**:
- 下拉从 entity_event_types 拉取（按分类分组）
- 可选为空（不关联事件）
- 保存时写入 `insp_template_items.linked_event_type_code`

### 5.2 等级定义 — 事件类型绑定

**位置**: 评分方案的等级定义配置中

```
等级定义:
  A 优秀 (90-100) → 关联事件: [纪律优秀 ▼]
  B 良好 (80-89)  → 关联事件: (空)
  C 及格 (60-79)  → 关联事件: (空)
  D 不合格 (<60)  → 关联事件: [纪律不合格 ▼]
```

### 5.3 评分观察分析页面 (新页面)

**路由**: `/inspection/v7/analytics/observations`

**UI 风格**: CourseListView 风格 — 统计头部 + 筛选栏 + 表格 + 抽屉详情

```
┌─ 页面头部 ─────────────────────────────────────────┐
│  评分观察记录                                       │
│  总数 1,234 | 负面 456 | 严重 23 | 本周 89          │
│                                    [导出Excel]      │
├─ 筛选栏 ───────────────────────────────────────────┤
│  [搜索...] [项目▼] [严重程度▼] [主体类型▼] [日期范围]│
├─ 表格 ─────────────────────────────────────────────┤
│  检查项    主体      分数  严重程度  事件类型  时间   │
│  迟到      张三      -2   低       迟到     04-06  │
│  打架      李四      -5   高       打架斗殴 04-06  │
│  地面卫生  3栋201   -3   中       卫生违纪 04-06  │
│  ...                                               │
└────────────────────────────────────────────────────┘
```

**表格列**:
- 检查项名称 (item_name)
- 主体 (subject_name + subject_type 图标)
- 所属班级 (class_name)
- 分数 (score, 红色负数)
- 严重程度 (severity badge: LOW灰/MEDIUM黄/HIGH橙/CRITICAL红)
- 事件类型 (linked_event_type_code → typeName)
- 检查项目 (project_name)
- 观察时间 (observed_at)

**筛选器**:
- 关键词搜索 (item_name / subject_name)
- 按项目筛选 (project_id)
- 按严重程度 (severity)
- 按主体类型 (subject_type: USER/ORG_UNIT/PLACE)
- 按日期范围 (observed_at)
- 仅负面 (is_negative toggle)

---

## 6. 文件清单

### 6.1 新增文件

| 文件 | 说明 |
|------|------|
| `domain/inspection/model/v7/execution/ScoringObservation.java` | 评分观察值对象 |
| `domain/inspection/service/ObservationExtractor.java` | 提取器接口 |
| `domain/inspection/service/ObservationContext.java` | 提取上下文 |
| `domain/inspection/service/DefaultObservationExtractor.java` | 默认提取器 |
| `domain/inspection/service/ViolationRecordObservationExtractor.java` | 违纪记录提取器 |
| `domain/inspection/service/PersonScoreObservationExtractor.java` | 人次扣分提取器 |
| `domain/inspection/repository/SubmissionObservationRepository.java` | 观察仓储接口 |
| `infrastructure/persistence/inspection/v7/execution/SubmissionObservationPO.java` | PO |
| `infrastructure/persistence/inspection/v7/execution/SubmissionObservationMapper.java` | Mapper |
| `infrastructure/persistence/inspection/v7/execution/SubmissionObservationRepositoryImpl.java` | 仓储实现 |
| `interfaces/rest/inspection/v7/ObservationController.java` | 观察记录 API |
| `database/schema/V102.0.0__submission_observations.sql` | 建表 + 触发器更新 |
| `frontend/src/views/inspection/v7/analytics/ObservationListView.vue` | 观察分析页面 |
| `frontend/src/api/observation.ts` | 前端 API |
| `frontend/src/types/observation.ts` | 前端类型 |

### 6.2 修改文件

| 文件 | 改动 |
|------|------|
| `application/inspection/v7/InspSubmissionApplicationService.java` | 重写 `publishInspectionEvents()` → 调 Extractor + 写 observations + fire 触发器 |
| `application/event/ConditionMatcher.java` | 确认支持 boolean 匹配 |
| `frontend/src/router/index.ts` | 新增观察分析页面路由 |
| 检查模板编辑组件 | 检查项配置中新增 linked_event_type_code 下拉 |
| 等级定义编辑组件 | 等级配置中新增 linked_event_type_code 下拉 |

### 6.3 删除 / 废弃

| 文件/代码 | 原因 |
|-----------|------|
| `publishInspectionEvents()` 中旧的 VIOLATION_RECORD 处理逻辑 | 被 ObservationExtractor 替代 |
| `entityEventApplicationService.createEvent()` 的直接调用(检查模块内) | 改为走 triggerService.fire() |

---

## 7. 实施步骤

### Phase 1: 基础设施 (后端)

1. 创建 `submission_observations` 表 (V102 迁移)
2. 创建 `ScoringObservation` 值对象
3. 创建 `ObservationExtractor` 接口
4. 创建 `SubmissionObservationPO` + Mapper + Repository
5. 更新 trigger_points contextSchema (标准字段)
6. 更新默认触发器条件 → `isNegative == true`

### Phase 2: 三个 Extractor 实现

7. `DefaultObservationExtractor` — 通用类型
8. `ViolationRecordObservationExtractor` — 违纪记录
9. `PersonScoreObservationExtractor` — 人次扣分
10. 单元测试: 每个 Extractor 独立测试

### Phase 3: 集成

11. 重写 `InspSubmissionApplicationService.publishInspectionEvents()`
12. 接入 triggerService.fire() 替代直接 createEvent()
13. 集成测试: 完整提交流程 → 观察写入 → 事件触发 → 通知分发

### Phase 4: 前端

14. 观察记录 API (`ObservationController`)
15. 观察分析页面 (`ObservationListView.vue`, CourseListView 风格)
16. 检查模板编辑: 检查项绑定事件类型下拉
17. 等级定义编辑: 等级绑定事件类型下拉
18. 路由注册

### Phase 5: 验收

19. 端到端测试: 检查提交 → 观察 → 事件 → 通知
20. 清理旧代码: 删除直接 createEvent 调用

---

## 8. 类型覆盖验证

| 检查项类型 | Extractor | 触发条件 | 主体 |
|-----------|-----------|---------|------|
| PASS_FAIL | Default | score < 0 (FAIL) | 检查对象 |
| SELECT / RADIO | Default | 选中负分选项 | 检查对象 |
| MULTI_SELECT / CHECKBOX | Default | 选中含负分选项 | 检查对象 |
| NUMBER / SLIDER | Default | 扣分模式 score < 0 | 检查对象 |
| RATING | Default | 低评分映射负分 | 检查对象 |
| CHECKLIST | Default | 未勾选项累计扣分 | 检查对象 |
| CALCULATED | Default | 公式结果 < 0 | 检查对象 |
| VIOLATION_RECORD | ViolationRecord | 每条记录天生负面 | 记录中的学生 |
| PERSON_SCORE | PersonScore | 逐人判断 score < 0 | 负分人员 |
| TEXT / TEXTAREA / RICH_TEXT | Default | 仅 isFlagged 时 | 检查对象 |
| PHOTO / VIDEO / SIGNATURE | Default | 仅 isFlagged 时 | 检查对象 |
| DATE / TIME / DATETIME | Default | 通常不触发 | -- |
| GPS / BARCODE | Default | 通常不触发 | -- |

全部 22 种类型已覆盖，无遗漏。
