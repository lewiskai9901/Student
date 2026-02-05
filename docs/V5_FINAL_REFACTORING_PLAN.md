# V5 量化检查系统最终重构方案

> **版本**: 1.1 (决策确认版)
> **日期**: 2026-01-31
> **状态**: 综合设计方案 - 核心决策已确认
> **基于**: V5_COMPLETE_INSPECTION_SYSTEM.md + 现有系统架构分析
>
> **决策状态**:
> - ✅ 决策1-17: 全部已确认

---

## 一、系统架构全景

### 1.1 现有系统领域划分

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           学生管理系统 - 领域全景图                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │                         Access 领域 (权限管理)                             │ │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐      │ │
│  │  │  User   │  │  Role   │  │Permission│  │DataScope │  │ UserRole │      │ │
│  │  │ 聚合根  │  │ 聚合根  │  │  实体    │  │ 值对象   │  │  实体    │      │ │
│  │  └─────────┘  └─────────┘  └─────────┘  └──────────┘  └──────────┘      │ │
│  │                                                                           │ │
│  │  用户类型: ADMIN | TEACHER | STUDENT                                      │ │
│  │  数据范围: ALL | DEPARTMENT | DEPARTMENT_AND_BELOW | SELF | CUSTOM        │ │
│  └───────────────────────────────────────────────────────────────────────────┘ │
│                                       │                                         │
│                                       ▼ (关联)                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │                      Organization 领域 (组织管理)                          │ │
│  │  ┌─────────┐  ┌──────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │ │
│  │  │ OrgUnit │  │SchoolClass│  │ Student │  │  Grade  │  │  Major  │       │ │
│  │  │ 聚合根  │  │  聚合根   │  │ 聚合根  │  │  实体   │  │  实体   │       │ │
│  │  └─────────┘  └──────────┘  └─────────┘  └─────────┘  └─────────┘       │ │
│  │       │              │            │                                       │ │
│  │       └──────────────┼────────────┘                                       │ │
│  │                      ▼                                                    │ │
│  │         组织层级: School → Department → Grade → Class → Student           │ │
│  └───────────────────────────────────────────────────────────────────────────┘ │
│                                       │                                         │
│                                       ▼ (关联)                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │                        Space 领域 (场所管理)                               │ │
│  │  ┌─────────┐  ┌───────────────┐  ┌────────────────────┐                  │ │
│  │  │  Space  │  │ SpaceOccupant │  │SpaceClassAssignment│                  │ │
│  │  │ 聚合根  │  │    实体       │  │       实体          │                  │ │
│  │  └─────────┘  └───────────────┘  └────────────────────┘                  │ │
│  │                                                                           │ │
│  │  场所层级: Campus → Building → Floor → Room                               │ │
│  │  房间类型: DORMITORY | CLASSROOM | LAB | OFFICE | ...                     │ │
│  └───────────────────────────────────────────────────────────────────────────┘ │
│                                       │                                         │
│                                       ▼ (新增)                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │                      Inspection 领域 (量化检查) [V5重构]                    │ │
│  │  ┌──────────────┐  ┌─────────────┐  ┌──────────────┐  ┌────────────┐     │ │
│  │  │   Template   │  │   Project   │  │    Task      │  │  Summary   │     │ │
│  │  │   (模板)     │  │   (项目)    │  │   (任务)     │  │  (汇总)    │     │ │
│  │  └──────────────┘  └─────────────┘  └──────────────┘  └────────────┘     │ │
│  │         │                 │                │                │             │ │
│  │         ▼                 ▼                ▼                ▼             │ │
│  │  ┌────────────┐   ┌────────────┐   ┌────────────┐   ┌────────────┐       │ │
│  │  │  Category  │   │CategoryCfg │   │ TaskRecord │   │  Ranking   │       │ │
│  │  │  (类别)    │   │ (类别配置) │   │  (记录)    │   │  (排名)    │       │ │
│  │  └────────────┘   └────────────┘   └────────────┘   └────────────┘       │ │
│  │         │                                  │                              │ │
│  │         ▼                                  ▼                              │ │
│  │  ┌────────────┐                    ┌────────────┐   ┌────────────┐       │ │
│  │  │ ScoreItem  │                    │  Appeal    │   │ Corrective │       │ │
│  │  │ (扣分项)   │                    │  (申诉)    │   │  (整改)    │       │ │
│  │  └────────────┘                    └────────────┘   └────────────┘       │ │
│  └───────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 领域间关系图

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              领域间核心关系                                       │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│     Access                Organization                Space                      │
│   ┌───────┐              ┌───────────┐             ┌────────┐                   │
│   │ User  │──────────────│  Student  │─────────────│ Space  │                   │
│   │       │  1:1         │           │   N:1       │(Room)  │                   │
│   └───┬───┘              └─────┬─────┘             └────┬───┘                   │
│       │                        │                        │                        │
│       │ 1:N                    │ N:1                    │                        │
│       ▼                        ▼                        │                        │
│   ┌───────┐              ┌───────────┐                  │                        │
│   │ Role  │              │SchoolClass│──────────────────┘                        │
│   └───────┘              └─────┬─────┘   N:M (SpaceClassAssignment)             │
│                                │                                                 │
│                                │ N:1                                             │
│                                ▼                                                 │
│                          ┌───────────┐                                          │
│                          │  OrgUnit  │                                          │
│                          │ (部门)    │                                          │
│                          └───────────┘                                          │
│                                │                                                 │
│                                │                                                 │
│                                ▼                                                 │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │                         Inspection 领域关联                                │  │
│  │                                                                           │  │
│  │   检查对象:                                                               │  │
│  │     - Student (学生)     ← 按学生录入模式 (PERSON)                         │  │
│  │     - SchoolClass (班级) ← 按班级录入模式 (CLASS)                          │  │
│  │     - Space (宿舍/教室)  ← 按空间录入模式 (SPACE)                          │  │
│  │                                                                           │  │
│  │   检查人员:                                                               │  │
│  │     - User (检查员)      ← 执行检查任务                                    │  │
│  │     - User (审核员)      ← 审核申诉、复查整改                              │  │
│  │                                                                           │  │
│  │   数据范围:                                                               │  │
│  │     - OrgUnit (部门)     ← 限定检查范围、数据权限                          │  │
│  │     - Grade (年级)       ← inspection_level = GRADE                       │  │
│  │     - SchoolClass        ← inspection_level = CLASS                       │  │
│  │                                                                           │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、现有模块关键数据结构

### 2.1 用户管理 (Access 领域)

#### 核心聚合: User

```java
public class User extends AggregateRoot<Long> {
    // 身份标识
    private Long id;
    private String username;
    private String password;

    // 基本信息
    private String realName;
    private String phone;
    private String email;
    private String employeeNo;
    private Integer gender;

    // 组织关联 (关键!)
    private Long orgUnitId;          // 所属部门 → 用于数据权限控制
    private Long classId;            // 所属班级 (学生用户)

    // 用户类型
    private UserType userType;       // ADMIN | TEACHER | STUDENT
    private UserStatus status;

    // 角色关联
    private List<Long> roleIds;
}
```

#### 数据权限模型

```java
public enum DataScope {
    ALL("all", "全部数据"),
    DEPARTMENT_AND_BELOW("department_and_below", "本部门及以下"),
    DEPARTMENT("department", "仅本部门"),
    CUSTOM("custom", "自定义范围"),
    SELF("self", "仅本人");
}

public enum DataModule {
    // Organization 领域
    ORG_UNIT, STUDENT, DORMITORY, CLASSROOM,

    // Inspection 领域 (需扩展)
    INSPECTION_TEMPLATE, INSPECTION_RECORD, APPEAL,

    // 其他领域
    RATING, TASK;
}
```

**与检查系统的关联点:**
- `User.orgUnitId` → 检查项目的数据权限边界
- `User.userType = TEACHER` → 可作为检查员、审核员
- `User.userType = STUDENT` → 作为被检查对象、申诉人
- `DataScope` → 控制检查记录、汇总数据的可见范围

### 2.2 组织管理 (Organization 领域)

#### 核心聚合: SchoolClass

```java
public class SchoolClass extends AggregateRoot<Long> {
    private Long id;
    private String classCode;
    private String className;

    // 组织层级
    private Long orgUnitId;          // 所属部门
    private Long gradeId;            // 所属年级
    private Integer enrollmentYear;  // 入学年份
    private Integer gradeLevel;      // 年级级别 (1-6)

    // 班级规模
    private Integer standardSize;
    private Integer currentSize;

    // 状态
    private ClassStatus status;      // PREPARING | ACTIVE | GRADUATED | DISSOLVED

    // 班主任关联
    private List<TeacherAssignment> teacherAssignments;
}
```

#### 核心聚合: Student

```java
public class Student extends AggregateRoot<Long> {
    // 身份信息
    private String studentNo;
    private String name;
    private Gender gender;

    // 班级关联 (关键!)
    private Long classId;

    // 宿舍关联 (关键!)
    private Long dormitoryId;
    private Integer bedNumber;

    // 学籍状态
    private StudentStatus status;    // STUDYING | SUSPENDED | WITHDRAWN | GRADUATED
}
```

**与检查系统的关联点:**
- `SchoolClass` → 检查记录的归属目标 (target_type = CLASS)
- `SchoolClass.orgUnitId` → 检查结果汇总的组织维度
- `Student` → 检查记录的关联对象 (按学生录入模式)
- `Student.classId` → 学生检查分数归属到班级
- `Student.dormitoryId` → 宿舍检查时的学生关联

### 2.3 场所管理 (Space 领域)

#### 核心聚合: Space

```java
public class Space extends AggregateRoot<Long> {
    // 基本信息
    private String spaceCode;
    private String spaceName;

    // 类型层级
    private SpaceType spaceType;     // CAMPUS | BUILDING | FLOOR | ROOM
    private RoomType roomType;       // DORMITORY | CLASSROOM | LAB | ...
    private BuildingType buildingType;

    // 树形结构
    private Long parentId;
    private SpacePath path;          // 物化路径 /1/2/3/
    private Integer level;

    // 冗余字段 (查询优化)
    private Long campusId;
    private Long buildingId;
    private Integer floorNumber;

    // 容量管理
    private Capacity capacity;       // maxCapacity + currentOccupancy

    // 组织关联 (关键!)
    private Long orgUnitId;
    private Long classId;            // 班级关联 (如专属教室)

    // 性别限制 (宿舍)
    private GenderType genderType;   // MIXED | MALE | FEMALE
}
```

#### 关联实体: SpaceOccupant

```java
public class SpaceOccupant {
    private Long spaceId;
    private OccupantType occupantType;  // STUDENT | TEACHER | STAFF
    private Long occupantId;
    private Integer positionNo;         // 床位号
    private LocalDate checkInDate;
    private Integer status;             // 0=已退出, 1=在住
}
```

**与检查系统的关联点:**
- `Space (RoomType=DORMITORY)` → 宿舍检查的目标对象
- `Space (RoomType=CLASSROOM)` → 教室检查的目标对象
- `SpaceOccupant` → 宿舍检查时关联到具体学生
- `Space.path` → 支持按楼栋、楼层汇总检查结果
- `Space.classId` → 混合宿舍分配策略的依据

---

## 三、检查系统与现有模块的整合设计

### 3.1 检查目标类型设计

```java
public enum TargetType {
    STUDENT("学生"),      // 对应 Student 聚合
    CLASS("班级"),        // 对应 SchoolClass 聚合
    DORMITORY("宿舍"),    // 对应 Space (RoomType=DORMITORY)
    CLASSROOM("教室"),    // 对应 Space (RoomType=CLASSROOM)
    SPACE("场所");        // 通用场所
}
```

### 3.2 检查记录关联设计

```sql
-- 检查记录核心表 (target_inspection_records)
CREATE TABLE target_inspection_records (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,

    -- 检查目标 (多态关联)
    target_type ENUM('STUDENT', 'CLASS', 'DORMITORY', 'CLASSROOM') NOT NULL,
    target_id BIGINT NOT NULL,               -- Student.id / SchoolClass.id / Space.id
    target_name VARCHAR(100),                -- 冗余: 目标名称

    -- 班级归属 (所有检查最终归属到班级)
    class_id BIGINT NOT NULL,                -- SchoolClass.id
    class_name VARCHAR(100),                 -- 冗余: 班级名称

    -- 组织归属 (用于数据权限)
    org_unit_id BIGINT NOT NULL,             -- OrgUnit.id

    -- 检查分数
    base_score DECIMAL(10,2) NOT NULL,
    raw_score DECIMAL(10,2) NOT NULL,
    weighted_score DECIMAL(10,2),
    fair_adjusted_score DECIMAL(10,2),

    -- 状态
    status ENUM('DRAFT', 'SUBMITTED', 'REVIEWED', 'PUBLISHED') NOT NULL,

    INDEX idx_task_id (task_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_class_id (class_id),
    INDEX idx_org_unit_id (org_unit_id)
);
```

### 3.3 录入模式与目标映射

```
录入模式              检查目标              关联逻辑
─────────────────────────────────────────────────────────
SPACE (按空间)  →    DORMITORY/CLASSROOM   Space.id
                     ↓
                     通过 SpaceOccupant / SpaceClassAssignment
                     ↓
                     关联到 Student → 归属 SchoolClass

PERSON (按学生) →    STUDENT               Student.id
                     ↓
                     通过 Student.classId
                     ↓
                     归属 SchoolClass

CLASS (按班级)  →    CLASS                 SchoolClass.id
                     ↓
                     直接归属

ITEM (按扣分项) →    批量选择目标           多个 target_id
                     ↓
                     分别关联

CHECKLIST      →     逐项检查              checklist_responses 表
(清单模式)
```

### 3.4 混合宿舍分数分配

**场景**: 一个宿舍住着来自不同班级的学生

```java
// 混合宿舍分配策略
public enum MixedDormitoryStrategy {
    RATIO("按人数比例"),      // 按各班级学生人数比例分配
    AVERAGE("平均分配"),      // 所有班级平均分
    FULL("全额分配"),         // 每个班级都计全分
    MAIN("主责班级");         // 只分配给主责班级
}
```

**实现逻辑:**

```java
public class MixedDormitoryScoreAllocator {

    /**
     * 分配宿舍扣分到各班级
     *
     * @param dormitoryId 宿舍ID
     * @param deductionScore 扣分值
     * @param strategy 分配策略
     * @return 各班级的分配结果
     */
    public Map<Long, BigDecimal> allocate(
        Long dormitoryId,
        BigDecimal deductionScore,
        MixedDormitoryStrategy strategy
    ) {
        // 1. 查询宿舍中的学生及其班级
        List<SpaceOccupant> occupants = spaceOccupantRepository
            .findBySpaceIdAndStatus(dormitoryId, 1);

        // 2. 按班级分组统计人数
        Map<Long, Long> classCounts = occupants.stream()
            .collect(Collectors.groupingBy(
                o -> studentRepository.findById(o.getOccupantId())
                    .map(Student::getClassId).orElse(null),
                Collectors.counting()
            ));

        // 3. 根据策略分配
        return switch (strategy) {
            case RATIO -> allocateByRatio(classCounts, deductionScore);
            case AVERAGE -> allocateAverage(classCounts.keySet(), deductionScore);
            case FULL -> allocateFull(classCounts.keySet(), deductionScore);
            case MAIN -> allocateToMain(dormitoryId, deductionScore);
        };
    }
}
```

### 3.5 公平权重计算

**场景**: 人数多的班级可能扣分机会更多，需要公平化处理

```java
public enum FairWeightMode {
    DIVIDE("除以人数"),       // 扣分 / 班级人数
    BENCHMARK("基准人数法");  // 扣分 * (基准人数 / 实际人数)
}

public class FairWeightCalculator {

    /**
     * 计算公平调整后的分数
     */
    public BigDecimal calculateFairScore(
        BigDecimal rawScore,
        Integer classSize,
        FairWeightMode mode,
        Integer benchmarkCount
    ) {
        return switch (mode) {
            case DIVIDE -> rawScore.divide(
                BigDecimal.valueOf(classSize),
                2, RoundingMode.HALF_UP
            );
            case BENCHMARK -> rawScore.multiply(
                BigDecimal.valueOf(benchmarkCount)
            ).divide(
                BigDecimal.valueOf(classSize),
                2, RoundingMode.HALF_UP
            );
        };
    }
}
```

---

## 四、数据权限整合设计

### 4.1 检查系统数据权限矩阵

```
┌─────────────────┬─────────────────────────────────────────────────────────────┐
│   DataScope     │                    检查数据可见范围                          │
├─────────────────┼─────────────────────────────────────────────────────────────┤
│ ALL             │ 可查看所有检查记录、汇总、排名                               │
│                 │ 适用: 超级管理员、学生处领导                                  │
├─────────────────┼─────────────────────────────────────────────────────────────┤
│ DEPARTMENT_     │ 可查看本部门及下属组织的检查数据                             │
│ AND_BELOW       │ 适用: 系部主任、年级主任                                     │
├─────────────────┼─────────────────────────────────────────────────────────────┤
│ DEPARTMENT      │ 只能查看本部门的检查数据                                     │
│                 │ 适用: 普通教师                                               │
├─────────────────┼─────────────────────────────────────────────────────────────┤
│ SELF            │ 只能查看自己执行的检查任务                                   │
│                 │ 适用: 检查员                                                 │
├─────────────────┼─────────────────────────────────────────────────────────────┤
│ CUSTOM          │ 自定义选择可访问的组织单元                                   │
│                 │ 适用: 跨部门检查员                                           │
└─────────────────┴─────────────────────────────────────────────────────────────┘
```

### 4.2 扩展 DataModule 枚举

```java
public enum DataModule {
    // 现有模块
    ORG_UNIT("org_unit", "组织架构", "organization"),
    STUDENT("student", "学生信息", "organization"),
    DORMITORY("dormitory", "宿舍管理", "organization"),
    CLASSROOM("classroom", "教室管理", "organization"),

    // V5 检查系统模块 (新增)
    INSPECTION_TEMPLATE("inspection_template", "检查模板", "inspection"),
    INSPECTION_PROJECT("inspection_project", "检查项目", "inspection"),
    INSPECTION_TASK("inspection_task", "检查任务", "inspection"),
    INSPECTION_RECORD("inspection_record", "检查记录", "inspection"),
    INSPECTION_SUMMARY("inspection_summary", "检查汇总", "inspection"),
    APPEAL("appeal", "申诉管理", "inspection"),
    CORRECTIVE("corrective", "整改管理", "inspection"),
    BONUS_ITEM("bonus_item", "加分项", "inspection");
}
```

### 4.3 权限编码设计

```java
// 检查系统权限编码
public class InspectionPermissions {
    // 模板管理
    public static final String TEMPLATE_VIEW = "inspection:template:view";
    public static final String TEMPLATE_CREATE = "inspection:template:create";
    public static final String TEMPLATE_EDIT = "inspection:template:edit";
    public static final String TEMPLATE_DELETE = "inspection:template:delete";

    // 项目管理
    public static final String PROJECT_VIEW = "inspection:project:view";
    public static final String PROJECT_CREATE = "inspection:project:create";
    public static final String PROJECT_MANAGE = "inspection:project:manage";

    // 任务执行
    public static final String TASK_VIEW = "inspection:task:view";
    public static final String TASK_EXECUTE = "inspection:task:execute";
    public static final String TASK_REVIEW = "inspection:task:review";
    public static final String TASK_PUBLISH = "inspection:task:publish";

    // 申诉管理
    public static final String APPEAL_VIEW = "inspection:appeal:view";
    public static final String APPEAL_CREATE = "inspection:appeal:create";
    public static final String APPEAL_REVIEW = "inspection:appeal:review";

    // 整改管理
    public static final String CORRECTIVE_VIEW = "inspection:corrective:view";
    public static final String CORRECTIVE_EXECUTE = "inspection:corrective:execute";
    public static final String CORRECTIVE_VERIFY = "inspection:corrective:verify";

    // 汇总与排名
    public static final String SUMMARY_VIEW = "inspection:summary:view";
    public static final String RANKING_VIEW = "inspection:ranking:view";
    public static final String RANKING_PUBLISH = "inspection:ranking:publish";
}
```

---

## 五、Saga 异步流程与领域事件

### 5.1 检查任务发布 Saga

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class InspectionCompletionSaga {

    private final TargetInspectionRecordRepository recordRepository;
    private final DailySummaryRepository summaryRepository;
    private final RatingCalculationService ratingService;
    private final AutoBonusRuleService bonusService;
    private final NotificationService notificationService;

    /**
     * 任务发布后触发的Saga流程
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskPublished(TaskPublishedEvent event) {
        Long taskId = event.getTaskId();
        log.info("开始处理任务发布Saga, taskId={}", taskId);

        try {
            // Step 1: 加载所有班级检查记录
            List<TargetInspectionRecord> records =
                recordRepository.findByTaskId(taskId);

            // Step 2: 计算班级评级
            records.forEach(record -> {
                String rating = ratingService.calculateRating(record);
                record.setRating(rating);
                recordRepository.save(record);
            });

            // Step 3: 检查是否触发自动加分
            records.forEach(record -> {
                bonusService.checkAndApplyAutoBonus(record);
            });

            // Step 4: 生成/更新每日汇总
            LocalDate inspectionDate = event.getInspectionDate();
            summaryRepository.regenerateForDate(inspectionDate, taskId);

            // Step 5: 发送通知
            notificationService.notifyTaskPublished(event);

            log.info("任务发布Saga完成, taskId={}", taskId);

        } catch (Exception e) {
            log.error("任务发布Saga执行失败, taskId={}", taskId, e);
            // 发布补偿事件或标记需要人工介入
            applicationEventPublisher.publishEvent(
                new SagaFailedEvent(taskId, e.getMessage())
            );
        }
    }
}
```

### 5.2 领域事件定义

```java
// 任务发布事件
@Getter
public class TaskPublishedEvent extends DomainEvent {
    private final Long taskId;
    private final Long projectId;
    private final LocalDate inspectionDate;
    private final Long publishedBy;
}

// 整改需求事件
@Getter
public class CorrectiveRequiredEvent extends DomainEvent {
    private final Long recordId;
    private final Long classId;
    private final String className;
    private final String reason;
    private final LocalDate deadline;
}

// 申诉状态变更事件
@Getter
public class AppealStatusChangedEvent extends DomainEvent {
    private final Long appealId;
    private final Long recordId;
    private final AppealStatus oldStatus;
    private final AppealStatus newStatus;
    private final BigDecimal scoreAdjustment;
}

// 排名发布事件
@Getter
public class RankingPublishedEvent extends DomainEvent {
    private final Long summaryId;
    private final LocalDate summaryDate;
    private final SummaryType summaryType;
}
```

---

## 六、建议与疑问

### 6.1 设计建议

#### 建议 1: 统一场所模型

**现状**: 系统中存在两种场所管理方式
- `Space` 聚合 (新设计): 统一的树形场所模型
- `dormitories` 表 (旧设计): 独立的宿舍表

**建议**:
- 将旧的 `dormitories` 表数据迁移到 `Space` 模型
- V5检查系统统一使用 `Space` 作为场所引用
- 保留兼容层支持旧API

```sql
-- 迁移脚本示例
INSERT INTO space (space_code, space_name, space_type, room_type, ...)
SELECT dormitory_no, CONCAT(building_name, '-', dormitory_no),
       'ROOM', 'DORMITORY', ...
FROM dormitories d
JOIN buildings b ON d.building_id = b.id;
```

#### 建议 2: 班级-宿舍关联优化

**现状**:
- `Student.dormitoryId` 直接关联宿舍
- `space_class_assignment` 表记录班级-场所分配

**建议**:
- 优先使用 `space_class_assignment` 管理班级-宿舍关系
- `Student.dormitoryId` 作为冗余字段用于快速查询
- 混合宿舍分配时，根据 `space_class_assignment.assigned_beds` 计算比例

#### 建议 3: 用户-检查员关系

**现状**:
- `User.userType` 区分管理员、教师、学生
- 检查员角色通过 `Role` 分配

**建议**:
- 新增 `INSPECTOR` 角色类型
- 检查项目中的 `inspector_ids` 关联到具有检查员角色的用户
- 检查任务分配时验证用户的检查员权限

```java
// 检查项目分配检查员时的验证
public void assignInspector(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

    boolean hasInspectorRole = user.getRoleIds().stream()
        .anyMatch(roleId -> roleRepository.findById(roleId)
            .map(Role::getRoleType)
            .filter(type -> type == RoleType.INSPECTOR)
            .isPresent());

    if (!hasInspectorRole) {
        throw new BusinessException("用户没有检查员角色");
    }

    this.inspectorIds.add(userId);
}
```

#### 建议 4: 检查级别与组织层级对齐

**V5设计**: `inspection_level` = CLASS | GRADE | DEPARTMENT

**建议**: 与现有组织层级对齐
- CLASS: 结果直接归属班级，按班级排名
- GRADE: 按 `SchoolClass.gradeId` 聚合，年级内排名
- DEPARTMENT: 按 `SchoolClass.orgUnitId` 聚合，部门内排名

```java
public List<RankingVO> generateRanking(
    Long summaryId,
    InspectionLevel level
) {
    return switch (level) {
        case CLASS -> rankingRepository.rankByClass(summaryId);
        case GRADE -> {
            // 先按年级分组，再在年级内排名
            List<DailySummary> summaries = summaryRepository.findById(summaryId);
            yield summaries.stream()
                .collect(Collectors.groupingBy(s ->
                    classRepository.findById(s.getClassId())
                        .map(SchoolClass::getGradeId).orElse(null)))
                .entrySet().stream()
                .flatMap(e -> rankWithinGroup(e.getValue()).stream())
                .toList();
        }
        case DEPARTMENT -> {
            // 先按部门分组，再在部门内排名
            // 类似逻辑
        }
    };
}
```

### 6.2 设计决策记录 (ADR)

> 以下疑问已确认决策，作为后续实现的依据。

#### 决策 1: 学生用户账号 ✅ 已确认

**问题**: 学生是否需要系统登录账号?

**决策**: 学生可以登录，但权限非常小，只能做特定事情

**实现方案**:
```java
// 学生角色权限定义
public class StudentPermissions {
    // 允许的权限 (白名单)
    public static final String[] ALLOWED = {
        "inspection:personal:view",      // 查看个人扣分记录
        "inspection:appeal:create",      // 提交申诉
        "inspection:appeal:my",          // 查看我的申诉
        "profile:view",                  // 查看个人信息
        "profile:password:change"        // 修改密码
    };
}
```

**学生端功能清单**:
| 功能 | 说明 | 权限 |
|------|------|------|
| 个人扣分查看 | 查看本人的扣分记录 | `inspection:personal:view` |
| 提交申诉 | 对扣分记录提出申诉 | `inspection:appeal:create` |
| 申诉进度 | 查看申诉处理状态 | `inspection:appeal:my` |
| 个人中心 | 查看/修改个人信息 | `profile:*` |

#### 决策 2: 检查模板可见性 ✅ 已确认

**问题**: 检查模板是全校共享还是部门私有?

**决策**: 部门私有 + 用户私有（创建者私有）

**实现方案**:
```sql
-- 模板可见性设计
ALTER TABLE inspection_templates ADD COLUMN visibility ENUM(
    'PRIVATE',      -- 仅创建者可见
    'DEPARTMENT',   -- 本部门可见
    'PUBLIC'        -- 全校可见 (需审批)
) DEFAULT 'PRIVATE';

ALTER TABLE inspection_templates ADD COLUMN created_by BIGINT NOT NULL;
ALTER TABLE inspection_templates ADD COLUMN org_unit_id BIGINT;
```

**可见性规则**:
```
┌─────────────┬─────────────────────────────────────────────────┐
│ visibility  │ 可见范围                                        │
├─────────────┼─────────────────────────────────────────────────┤
│ PRIVATE     │ 仅 created_by 用户可见可用                       │
│ DEPARTMENT  │ org_unit_id 部门内所有用户可见可用               │
│ PUBLIC      │ 全校可见（需要管理员审批后生效）                  │
└─────────────┴─────────────────────────────────────────────────┘
```

**补充规则**:
- 模板采用"复制引用"模式：项目创建时复制模板配置
- 原模板修改不影响已创建的项目
- 部门模板不可被其他部门直接引用，需先复制为私有

#### 决策 3: 混合宿舍主责班级 ✅ 已确认

**问题**: 混合宿舍使用 `MAIN` 策略时，如何确定主责班级?

**决策**: 优先使用 `space_class_assignment.priority`，若未配置则按人数最多

**实现方案**:
```java
public Long determineMainClass(Long dormitoryId) {
    // 1. 查询宿舍的班级分配
    List<SpaceClassAssignment> assignments =
        assignmentRepository.findBySpaceId(dormitoryId);

    if (assignments.isEmpty()) {
        // 2. 无分配记录，按实际入住学生人数
        return findClassWithMostOccupants(dormitoryId);
    }

    // 3. 优先按priority排序
    return assignments.stream()
        .filter(SpaceClassAssignment::isEnabled)
        .max(Comparator.comparing(SpaceClassAssignment::getPriority))
        .map(SpaceClassAssignment::getClassId)
        .orElseGet(() -> findClassWithMostOccupants(dormitoryId));
}
```

#### 决策 4: 检查记录班级归属 ✅ 已确认

**问题**: 学生转班后，历史检查记录归属哪个班级?

**业界最佳实践分析**:

| 方案 | 说明 | 使用场景 | 优缺点 |
|------|------|---------|--------|
| **快照模式** | 记录创建时固化班级信息 | 财务、审计、考勤 | ✅推荐：数据一致，历史可追溯 |
| 当前关联 | 始终使用当前班级 | 实时统计 | ❌历史数据失真 |
| 双重记录 | 同时记录检查时和当前班级 | 复杂报表需求 | 复杂度高 |
| 时态模式 | 记录关联有效期 | 精确历史查询 | 查询复杂 |

**决策**: 采用**快照模式**（业界主流做法）

**实现方案**:
```sql
-- 检查记录表设计（快照模式）
CREATE TABLE target_inspection_records (
    -- ...其他字段...

    -- 学生信息快照（检查时刻的状态）
    student_id BIGINT,
    student_no VARCHAR(50),           -- 快照：学号
    student_name VARCHAR(50),         -- 快照：姓名

    -- 班级信息快照（检查时刻的归属）
    class_id BIGINT NOT NULL,         -- 快照：班级ID
    class_name VARCHAR(100),          -- 快照：班级名称
    class_code VARCHAR(50),           -- 快照：班级编码

    -- 组织信息快照
    org_unit_id BIGINT NOT NULL,      -- 快照：部门ID
    org_unit_name VARCHAR(100),       -- 快照：部门名称

    -- 宿舍信息快照（如适用）
    dormitory_id BIGINT,
    dormitory_name VARCHAR(100),      -- 快照：宿舍名称
    bed_number VARCHAR(20),           -- 快照：床位号

    INDEX idx_class_id (class_id),
    INDEX idx_student_id (student_id)
);
```

**关键规则**:
1. 记录创建时，立即固化所有关联信息
2. 学生后续转班、退学不影响历史记录
3. 汇总统计始终按记录中的快照班级计算
4. 若需查询"学生历史所有记录"，按 `student_id` 查询即可

#### 决策 5: 缺失类别处理 ✅ 已确认

**问题**: 某次检查缺失部分类别时如何处理?

**决策**: 默认使用 EXCLUDE，并引入三态标记

**实现方案**:

```java
// 类别检查状态（三态）
public enum CategoryCheckStatus {
    CHECKED("已检查"),       // 正常检查，有分数
    NOT_APPLICABLE("无此项"), // 检查员确认该类别不适用
    NOT_CHECKED("未检查");    // 检查员未检查（异常状态）
}
```

**数据表设计**:
```sql
-- 每个类别的检查状态
CREATE TABLE task_category_status (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    target_record_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,

    check_status ENUM('CHECKED', 'NOT_APPLICABLE', 'NOT_CHECKED') NOT NULL,

    -- CHECKED 时有分数
    raw_score DECIMAL(10,2),
    deduction_count INT DEFAULT 0,
    bonus_count INT DEFAULT 0,

    -- NOT_APPLICABLE 时的说明
    na_reason VARCHAR(200),

    checked_at DATETIME,
    checked_by BIGINT,

    UNIQUE KEY uk_task_target_category (task_id, target_record_id, category_id)
);
```

**汇总计算规则**:
```java
public BigDecimal calculateWeightedScore(List<CategoryScore> scores) {
    // 过滤掉 NOT_APPLICABLE 的类别
    List<CategoryScore> applicableScores = scores.stream()
        .filter(s -> s.getStatus() != NOT_APPLICABLE)
        .toList();

    // 检查是否有 NOT_CHECKED（异常提醒）
    long notCheckedCount = scores.stream()
        .filter(s -> s.getStatus() == NOT_CHECKED)
        .count();
    if (notCheckedCount > 0) {
        log.warn("存在{}个未检查类别，可能影响汇总准确性", notCheckedCount);
    }

    // 重新计算权重基数（仅适用类别）
    BigDecimal totalWeight = applicableScores.stream()
        .map(CategoryScore::getWeight)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 加权计算
    return applicableScores.stream()
        .map(s -> s.getScore().multiply(s.getWeight()).divide(totalWeight, 4, HALF_UP))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

---

### 6.3 补充决策记录

#### 决策 6: 学生账号创建方式 ✅ 已确认

**问题**: 学生账号如何创建？

**决策**: 批量导入时自动创建 + 按需创建补充

**实现方案**:
```java
@Service
public class StudentImportService {

    /**
     * 导入学生时自动创建账号
     */
    @Transactional
    public void importStudent(StudentImportDTO dto) {
        // 1. 创建学生记录
        Student student = Student.enroll(dto.getStudentNo(), dto.getName(), ...);
        studentRepository.save(student);

        // 2. 自动创建用户账号
        String username = dto.getStudentNo();  // 用户名 = 学号
        String password = generateInitialPassword(dto.getIdCard());  // 身份证后6位

        User user = User.create(username, passwordEncoder.encode(password),
            dto.getName(), UserType.STUDENT, dto.getOrgUnitId());
        user.assignRoles(List.of(STUDENT_ROLE_ID));
        userRepository.save(user);

        // 3. 关联学生和用户
        student.bindUser(user.getId());
        studentRepository.save(student);
    }

    private String generateInitialPassword(String idCard) {
        // 身份证后6位作为初始密码
        if (idCard != null && idCard.length() >= 6) {
            return idCard.substring(idCard.length() - 6);
        }
        return "123456";  // 默认密码
    }
}
```

**补充机制**: 若导入时未创建账号，学生首次访问时可通过"学号+身份证"验证后自助激活

#### 决策 7: 模板审批流程 ✅ 已确认

**问题**: 部门模板升级为全校公开(PUBLIC)是否需要审批？

**决策**: 需要审批，采用 **纯角色** 方案

**审批人查找**:
```
1. 查找有"模板审核员"(TEMPLATE_REVIEWER)角色的用户
2. 若无审核员，降级到超级管理员(SUPER_ADMIN)
```

**实现方案**:
```java
public List<User> findApprovers() {
    // 1. 查找模板审核员角色
    List<User> reviewers = userRepository.findByRoleCode("TEMPLATE_REVIEWER");
    if (!reviewers.isEmpty()) {
        return reviewers;
    }

    // 2. 兜底：超级管理员
    return userRepository.findByRoleType(RoleType.SUPER_ADMIN);
}
```

**角色初始化**:
```sql
INSERT INTO roles (role_code, role_name, role_type, description) VALUES
('TEMPLATE_REVIEWER', '模板审核员', 'CUSTOM', '负责审核公开模板申请');
```

**说明**: 管理员需要为相关人员分配"模板审核员"角色，否则审批请求将由超级管理员处理

#### 决策 8: 检查记录修改权限 ✅ 已确认

**问题**: 检查记录提交后是否允许修改？谁可以修改？

**决策**: 按任务状态分级控制

**权限矩阵**:
```
┌─────────────────┬──────────────┬──────────────┬─────────────────┐
│   任务状态       │   检查员     │   审核员     │   修改方式       │
├─────────────────┼──────────────┼──────────────┼─────────────────┤
│ DRAFT          │ ✅ 可修改    │ ✅ 可修改    │ 直接修改         │
│ IN_PROGRESS    │ ✅ 可修改    │ ✅ 可修改    │ 直接修改         │
│ SUBMITTED      │ ❌ 不可改    │ ✅ 可修改    │ 审核时修改       │
│ PUBLISHED      │ ❌ 不可改    │ ❌ 不可改    │ 仅通过申诉调整   │
└─────────────────┴──────────────┴──────────────┴─────────────────┘
```

**实现方案**:
```java
public void updateRecord(Long recordId, UpdateRecordCommand cmd, Long operatorId) {
    InspectionTask task = taskRepository.findByRecordId(recordId);

    switch (task.getStatus()) {
        case DRAFT, IN_PROGRESS -> {
            // 检查员或审核员都可改
            record.update(cmd);
        }
        case SUBMITTED -> {
            // 仅审核员可改
            if (!isReviewer(operatorId, task)) {
                throw new ForbiddenException("任务已提交，仅审核员可修改");
            }
            record.update(cmd);
        }
        case PUBLISHED -> {
            throw new ForbiddenException("任务已发布，请通过申诉流程调整");
        }
    }
}
```

#### 决策 9: 排名并列处理 ✅ 已确认

**问题**: 当多个班级分数相同时，如何处理排名？

**决策**: 采用**标准竞赛排名**（1,1,3,4,5）

**示例**:
```
分数: 98, 98, 95, 90, 90, 85
排名:  1,  1,  3,  4,  4,  6   ← 标准竞赛排名
```

**实现方案**:
```java
public List<RankingVO> calculateRanking(List<ClassScore> scores) {
    // 按分数降序排序
    scores.sort(Comparator.comparing(ClassScore::getScore).reversed());

    List<RankingVO> rankings = new ArrayList<>();
    int rank = 1;

    for (int i = 0; i < scores.size(); i++) {
        ClassScore current = scores.get(i);

        // 标准竞赛排名：并列后跳过
        if (i > 0 && current.getScore().compareTo(scores.get(i-1).getScore()) != 0) {
            rank = i + 1;  // 跳到当前位置
        }

        rankings.add(new RankingVO(current.getClassId(), current.getScore(), rank));
    }

    return rankings;
}
```

#### 决策 10: 历史数据查询范围 ✅ 已确认

**问题**: 用户可以查询多长时间的历史检查数据？

**决策**: 默认当前学期，支持按学期切换

**实现方案**:
```java
public class InspectionQueryService {

    /**
     * 查询检查记录（默认当前学期）
     */
    public Page<InspectionRecordVO> queryRecords(QueryParams params) {
        // 默认使用当前学期
        if (params.getSemesterId() == null) {
            params.setSemesterId(semesterService.getCurrentSemesterId());
        }

        return recordRepository.findBySemester(params);
    }

    /**
     * 获取可选学期列表（供前端下拉）
     */
    public List<SemesterVO> getAvailableSemesters() {
        // 返回所有有数据的学期
        return semesterRepository.findAllWithInspectionData();
    }
}
```

**前端交互**:
```vue
<template>
  <div class="semester-selector">
    <el-select v-model="currentSemester" placeholder="选择学期">
      <el-option
        v-for="sem in semesters"
        :key="sem.id"
        :label="sem.name"
        :value="sem.id"
      />
    </el-select>
  </div>
</template>
```

**数据保留策略**: 所有历史数据永久保留，按学期索引优化查询性能

---

### 6.4 补充决策记录 (第二轮)

#### 决策 11: 学期管理 ✅ 已确认

**问题**: 系统中学期如何定义和管理？

**决策**: 复用现有学期管理模块

**说明**: 系统已有学期管理功能，检查系统直接关联现有学期表，无需重复开发

#### 决策 12: 通知机制 ✅ 已确认

**问题**: 系统通知如何发送？

**决策**: 先实现站内信，后续扩展其他渠道

**实现方案**:
```java
public interface NotificationService {
    // 发送站内信（首期实现）
    void sendInApp(Long userId, String title, String content);

    // 后续扩展
    void sendSms(String phone, String content);
    void sendWechat(String openId, String templateId, Map<String, String> data);
}
```

#### 决策 13: 移动端支持 ✅ 已确认

**问题**: 检查系统是否需要移动端？

**决策**: 首期响应式适配，后续开发小程序

**实施计划**:
- V5首期：Web端响应式布局，支持移动浏览器
- V5.1：微信小程序（检查员录入、学生查看）

#### 决策 14: 汇总生成时机 ✅ 已确认

**问题**: 每日汇总是手动生成还是自动生成？

**决策**: 自动时间可配置 + 手动触发

**实现方案**:
```java
// 汇总生成配置（项目级别）
public class SummaryConfig {
    Boolean autoGenerate;          // 是否自动生成
    String cronExpression;         // 自动生成时间（cron表达式，可配置）
    Boolean allowManualRegenerate; // 是否允许手动重新生成
}

// 默认配置
// autoGenerate = true
// cronExpression = "0 0 2 * * ?" (每天凌晨2点，可自定义)
// allowManualRegenerate = true
```

#### 决策 15: 整改流程细节 ✅ 已确认

**问题**: 整改工单的完整流程是什么？

**决策**: 期限、轮次、是否影响扣分均可自定义配置

**整改配置**:
```java
public class CorrectiveConfig {
    Integer defaultDays;           // 默认整改期限（天）
    Integer maxRounds;             // 最大整改轮次
    Boolean affectScore;           // 是否影响原扣分
    BigDecimal scoreAdjustment;    // 整改完成后的分数调整值
}
```

**整改责任人规则**:
```
┌─────────────────┬─────────────────────────────────────┐
│   检查目标       │   整改责任人                         │
├─────────────────┼─────────────────────────────────────┤
│ 学生            │ 班主任                               │
│ 场所→分配给班级  │ 班主任                               │
│ 场所→分配给系部  │ 系部负责人（部门管理员）              │
└─────────────────┴─────────────────────────────────────┘
```

**实现方案**:
```java
public Long determineCorrectiveResponsible(TargetInspectionRecord record) {
    return switch (record.getTargetType()) {
        case STUDENT -> {
            // 学生 → 班主任
            SchoolClass clazz = classRepository.findById(record.getClassId());
            yield clazz.getHeadTeacherId();
        }
        case DORMITORY, CLASSROOM -> {
            Space space = spaceRepository.findById(record.getTargetId());
            if (space.getClassId() != null) {
                // 场所分配给班级 → 班主任
                SchoolClass clazz = classRepository.findById(space.getClassId());
                yield clazz.getHeadTeacherId();
            } else {
                // 场所分配给系部 → 系部管理员
                yield userRepository.findDeptAdminByOrgUnitId(space.getOrgUnitId());
            }
        }
        default -> null;
    };
}
```

#### 决策 16: 自动加分规则 ✅ 已确认

**问题**: V5设计中提到自动加分，具体规则是什么？

**决策**: 高度自定义，由检查项目创建者配置

**加分规则配置**:
```java
public class AutoBonusRule {
    Long id;
    Long projectId;                // 所属项目
    String ruleCode;               // 规则编码
    String ruleName;               // 规则名称
    RuleType ruleType;             // 规则类型
    String triggerCondition;       // 触发条件（JSON/表达式）
    BigDecimal bonusScore;         // 加分分值
    Boolean enabled;               // 是否启用
    Long createdBy;                // 创建人（项目创建者）
}

public enum RuleType {
    CONSECUTIVE_FULL_SCORE,    // 连续满分
    MONTHLY_EXCELLENT,         // 月度优秀
    IMPROVEMENT,               // 进步奖励
    CUSTOM                     // 自定义规则
}
```

**触发条件示例**:
```json
// 连续满分
{
  "type": "CONSECUTIVE_FULL_SCORE",
  "params": {
    "days": 7,
    "minScore": 100
  }
}

// 月度优秀
{
  "type": "MONTHLY_EXCELLENT",
  "params": {
    "topN": 3,
    "minAvgScore": 95
  }
}

// 进步奖励
{
  "type": "IMPROVEMENT",
  "params": {
    "compareWeeks": 2,
    "minImprovement": 10
  }
}
```

**权限**: 检查项目创建者负责配置该项目的自动加分规则

#### 决策 17: 证据管理 ✅ 已确认

**问题**: 检查证据（照片）如何管理？

**决策**: 首期存放本地服务器

**实现方案**:
```java
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload.path:/data/uploads}")
    private String uploadPath;

    @Override
    public String store(MultipartFile file) {
        // 按日期分目录存储
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID() + getExtension(file);
        Path targetPath = Paths.get(uploadPath, "inspection", datePath, fileName);

        Files.createDirectories(targetPath.getParent());
        file.transferTo(targetPath);

        return "/inspection/" + datePath + "/" + fileName;
    }
}
```

**配置**:
```yaml
file:
  upload:
    path: /data/uploads           # 存储根路径
    max-size: 10MB                # 单文件最大大小
    max-count: 5                  # 单条记录最多文件数
    allowed-types: jpg,jpeg,png   # 允许的文件类型
    compress:
      enabled: true               # 是否压缩
      max-width: 1920             # 最大宽度
      quality: 0.8                # 压缩质量
```

**后续扩展**: 可通过配置切换到云存储(OSS/COS)

---

## 七、数据库迁移策略

### 7.1 新增表清单

| 序号 | 表名 | 说明 | 依赖 |
|------|------|------|------|
| 1 | inspection_templates | 检查模板 | - |
| 2 | template_categories | 模板类别 | inspection_templates |
| 3 | category_score_items | 类别扣分项 | template_categories |
| 4 | inspection_projects | 检查项目 | inspection_templates |
| 5 | project_category_configs | 项目类别配置 | inspection_projects |
| 6 | inspection_tasks | 检查任务 | inspection_projects |
| 7 | target_inspection_records | 目标检查记录 | inspection_tasks |
| 8 | deduction_records | 扣分记录 | target_inspection_records |
| 9 | bonus_items | 加分项 | inspection_projects |
| 10 | checklist_responses | 清单响应 | inspection_tasks |
| 11 | daily_summaries | 每日汇总 | inspection_projects |
| 12 | ranking_snapshots | 排名快照 | daily_summaries |
| 13 | appeals | 申诉 | deduction_records |
| 14 | corrective_orders | 整改工单 | target_inspection_records |
| 15 | inspection_evidences | 检查证据 | deduction_records |

### 7.2 现有表修改

| 表名 | 修改内容 |
|------|---------|
| users | 无修改，通过角色关联检查员 |
| roles | 新增 INSPECTOR 角色类型数据 |
| permissions | 新增检查系统权限数据 |
| role_data_permissions | 新增检查模块的数据权限配置 |
| data_module (如存在) | 新增检查相关模块枚举 |

### 7.3 迁移执行顺序

```
Phase 1: 基础表 (无依赖)
├── inspection_templates
├── template_categories
└── category_score_items

Phase 2: 项目表 (依赖模板)
├── inspection_projects
└── project_category_configs

Phase 3: 任务和记录表 (依赖项目)
├── inspection_tasks
├── target_inspection_records
├── deduction_records
├── bonus_items
└── checklist_responses

Phase 4: 汇总和排名表 (依赖记录)
├── daily_summaries
└── ranking_snapshots

Phase 5: 业务流程表 (依赖记录)
├── appeals
├── corrective_orders
└── inspection_evidences

Phase 6: 权限数据 (任何时候)
├── INSERT INTO roles (检查员角色)
└── INSERT INTO permissions (检查权限)
```

---

## 八、实施路线图

### Phase 1: 基础架构 (第1-2周)

- [ ] 创建检查领域包结构
- [ ] 实现 Template、Category、ScoreItem 聚合
- [ ] 实现模板管理 API
- [ ] 前端模板管理页面

### Phase 2: 项目管理 (第3-4周)

- [ ] 实现 Project 聚合
- [ ] 实现项目配置 (权重、打分、录入模式)
- [ ] 项目与模板关联
- [ ] 前端项目管理页面

### Phase 3: 任务执行 (第5-7周)

- [ ] 实现 Task 聚合
- [ ] 实现多种录入模式 (SPACE/PERSON/CLASS/ITEM/CHECKLIST)
- [ ] 混合宿舍分数分配
- [ ] 公平权重计算
- [ ] 前端检查执行页面

### Phase 4: 汇总与排名 (第8-9周)

- [ ] 实现 Summary 聚合
- [ ] 实现排名计算和发布
- [ ] Saga 异步编排
- [ ] 前端汇总和排名页面

### Phase 5: 申诉与整改 (第10-11周)

- [ ] 实现 Appeal 聚合
- [ ] 实现 CorrectiveOrder 聚合
- [ ] 领域事件处理
- [ ] 前端申诉和整改页面

### Phase 6: 数据迁移 (第12周)

- [ ] V4数据迁移脚本
- [ ] 数据验证
- [ ] 并行运行测试

### Phase 7: 测试验收 (第13-14周)

- [ ] 功能测试
- [ ] 性能测试
- [ ] 用户验收测试

### Phase 8: 上线部署 (第15周)

- [ ] 正式切换
- [ ] 监控告警配置
- [ ] 运维文档完善

---

## 九、附录

### A. 关键文件清单

| 领域 | 文件路径 | 说明 |
|------|---------|------|
| Access | `domain/user/model/aggregate/User.java` | 用户聚合根 |
| Access | `domain/access/model/Role.java` | 角色聚合根 |
| Access | `domain/access/model/DataScope.java` | 数据范围枚举 |
| Organization | `domain/student/model/aggregate/Student.java` | 学生聚合根 |
| Organization | `domain/organization/model/SchoolClass.java` | 班级聚合根 |
| Space | `domain/space/model/aggregate/Space.java` | 场所聚合根 |
| Space | `domain/space/model/entity/SpaceOccupant.java` | 场所占用者 |
| Inspection | `domain/inspection/model/` | V5检查领域模型 (待实现) |

### B. 参考文档

- [V5_COMPLETE_INSPECTION_SYSTEM.md](./V5_COMPLETE_INSPECTION_SYSTEM.md) - 完整V5设计方案
- [CLAUDE.md](../CLAUDE.md) - 项目开发指南
- [complete_schema_v2.sql](../database/schema/complete_schema_v2.sql) - 数据库Schema

---

**文档版本**: 1.0
**最后更新**: 2026-01-31
**作者**: Claude (基于系统分析)
