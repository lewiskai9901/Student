# V5 检查系统完全重构方案

> **版本**: 2.0
> **日期**: 2026-01-30
> **状态**: 最终方案
> **设计理念**: 以用户为中心，概念最简化，功能最完整

---

## 一、核心设计哲学

### 1.1 业界最佳实践提炼

| 产品 | 设计模式 | 核心理念 |
|------|---------|---------|
| Google Calendar | 事件优先 | 重复规则是属性，不是容器 |
| Jira/Linear | 任务独立 | Epic是可选标签，Sprint是时间窗口 |
| 工业巡检系统 | 任务驱动 | 计划是生成器，不是容器 |
| Notion | 数据库视图 | 同一数据，多维度筛选展示 |

### 1.2 设计原则

```
原则1: 概念最少化
  - 用户只需理解"检查"和"模板"两个核心概念
  - "系列"是可选的高级功能，不强制使用

原则2: 操作最短路径
  - 快速检查: 2步完成 (选模板 → 开始)
  - 完整检查: 3步完成 (选模板 → 配置 → 开始)

原则3: 统计最灵活
  - 支持按模板、按系列、按时间、按部门多维度统计
  - 无需预先创建"计划"也能做趋势分析

原则4: 数据不丢失
  - 历史数据可迁移
  - 向后兼容现有功能
```

### 1.3 核心变革

```
旧架构 (V4):
┌─────────────────────────────────────────────────────┐
│  Plan (计划)                                        │
│    └── Session (会话)                               │
│          └── ClassRecord (班级记录)                 │
│                └── Deduction (扣分)                 │
└─────────────────────────────────────────────────────┘
问题: 4层结构，概念复杂，创建流程长

新架构 (V5):
┌─────────────────────────────────────────────────────┐
│  Inspection (检查) ←── Series (系列) [可选标签]     │
│    └── ClassRecord (班级记录)                       │
│          └── Deduction (扣分)                       │
└─────────────────────────────────────────────────────┘
优势: 3层结构，概念清晰，即开即用
```

---

## 二、新数据模型设计

### 2.1 核心实体关系

```
┌─────────────────────────────────────────────────────────────────┐
│                         V5 数据模型                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  InspectionTemplate (检查模板) ─────────────────────┐           │
│  ├── id, name, description                          │           │
│  ├── categories[]                                   │           │
│  │     └── deductionItems[]                         │           │
│  └── status: DRAFT | PUBLISHED | ARCHIVED           │           │
│                                                     │           │
│                        ┌────────────────────────────┘           │
│                        ▼                                        │
│  InspectionSeries (检查系列) [可选]    Inspection (检查)        │
│  ├── id, name                          ├── id                   │
│  ├── templateId (默认模板)        ◄────┼── seriesId [可选]      │
│  ├── dateRange (统计范围)              ├── templateId           │
│  └── defaultConfig                     ├── inspectionDate       │
│                                        ├── status               │
│                                        ├── inputMode            │
│                                        ├── scoringMode          │
│                                        └── config{}             │
│                                                 │                │
│                                                 │ 1:n            │
│                                                 ▼                │
│                                        ClassRecord (班级记录)   │
│                                        ├── id                   │
│                                        ├── inspectionId         │
│                                        ├── classId              │
│                                        ├── scores               │
│                                        └── status               │
│                                                 │                │
│                              ┌──────────────────┼────────┐      │
│                              │ 1:n              │ 1:n    │ 1:n  │
│                              ▼                  ▼        ▼      │
│                        Deduction          Response    Bonus     │
│                        (扣分明细)         (清单响应)  (加分)    │
│                                                                 │
│  ════════════════════════════════════════════════════════════  │
│                                                                 │
│  Appeal (申诉) [全新设计]                                       │
│  ├── id                                                         │
│  ├── inspectionId ←─────── 直接关联检查                         │
│  ├── classRecordId ←────── 直接关联班级记录                     │
│  ├── deductionId ←──────── 直接关联扣分明细                     │
│  ├── status (简化状态机)                                        │
│  └── approvals[]                                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Inspection (检查) - 核心聚合根

```java
/**
 * 检查 - V5核心聚合根
 *
 * 设计理念:
 * - 这是用户直接操作的对象
 * - 可以独立存在，无需关联任何"计划"
 * - 通过 seriesId 可选地归类到某个系列（用于统计）
 */
@Entity
@Table(name = "inspections")
public class Inspection extends AggregateRoot {

    // ===== 基础标识 =====
    @Id
    private Long id;

    @Column(unique = true)
    private String inspectionCode;  // 唯一编码，如 "INS-20260130-001"

    // ===== 核心关联 =====
    @Column(nullable = false)
    private Long templateId;        // 必须：使用的检查模板

    @Column
    private Long seriesId;          // 可选：所属的检查系列（用于统计聚合）

    private Integer templateVersion; // 模板版本快照

    // ===== 检查信息 =====
    @Column(nullable = false)
    private LocalDate inspectionDate;

    private String title;           // 检查标题，如"1月30日卫生检查"

    private String description;     // 备注说明

    // ===== 配置 =====
    @Enumerated(EnumType.STRING)
    private InputMode inputMode;    // SPACE_FIRST | PERSON_FIRST | CHECKLIST | ORG_FIRST

    @Enumerated(EnumType.STRING)
    private ScoringMode scoringMode; // DEDUCTION_ONLY | BASE_SCORE | DUAL_TRACK

    private Integer baseScore;       // 基准分（默认100）

    @Enumerated(EnumType.STRING)
    private InspectionLevel level;   // CLASS | GRADE | DEPARTMENT

    // ===== 状态 =====
    @Enumerated(EnumType.STRING)
    private InspectionStatus status; // DRAFT | IN_PROGRESS | SUBMITTED | PUBLISHED | CANCELLED

    // ===== 操作人 =====
    private Long creatorId;
    private Long inspectorId;
    private Long publisherId;

    // ===== 时间线 =====
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime publishedAt;

    // ===== 统计快照 =====
    private Integer totalClasses;    // 检查班级数
    private BigDecimal averageScore; // 平均分
    private Integer deductionCount;  // 扣分次数

    // ===== 领域方法 =====

    public static Inspection create(CreateInspectionCommand cmd) {
        Inspection inspection = new Inspection();
        inspection.id = IdGenerator.nextId();
        inspection.inspectionCode = generateCode(cmd.getInspectionDate());
        inspection.templateId = cmd.getTemplateId();
        inspection.seriesId = cmd.getSeriesId(); // 可选
        inspection.inspectionDate = cmd.getInspectionDate();
        inspection.title = cmd.getTitle() != null ? cmd.getTitle()
            : generateDefaultTitle(cmd);
        inspection.inputMode = cmd.getInputMode();
        inspection.scoringMode = cmd.getScoringMode();
        inspection.baseScore = cmd.getBaseScore() != null ? cmd.getBaseScore() : 100;
        inspection.status = InspectionStatus.DRAFT;
        inspection.creatorId = cmd.getCreatorId();
        inspection.createdAt = LocalDateTime.now();
        return inspection;
    }

    public void start() {
        if (this.status != InspectionStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态可以开始");
        }
        this.status = InspectionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        registerEvent(new InspectionStartedEvent(this.id));
    }

    public void submit(Long inspectorId) {
        if (this.status != InspectionStatus.IN_PROGRESS) {
            throw new IllegalStateException("只有进行中状态可以提交");
        }
        this.status = InspectionStatus.SUBMITTED;
        this.inspectorId = inspectorId;
        this.submittedAt = LocalDateTime.now();
        registerEvent(new InspectionSubmittedEvent(this.id));
    }

    public void publish(Long publisherId) {
        if (this.status != InspectionStatus.SUBMITTED) {
            throw new IllegalStateException("只有已提交状态可以发布");
        }
        this.status = InspectionStatus.PUBLISHED;
        this.publisherId = publisherId;
        this.publishedAt = LocalDateTime.now();
        registerEvent(new InspectionPublishedEvent(this.id));
    }

    public void cancel(String reason) {
        if (this.status == InspectionStatus.PUBLISHED) {
            throw new IllegalStateException("已发布的检查不能取消");
        }
        this.status = InspectionStatus.CANCELLED;
        registerEvent(new InspectionCancelledEvent(this.id, reason));
    }

    public void updateStatistics(int totalClasses, BigDecimal avgScore, int deductionCount) {
        this.totalClasses = totalClasses;
        this.averageScore = avgScore;
        this.deductionCount = deductionCount;
    }
}
```

### 2.3 InspectionSeries (检查系列) - 可选聚合标签

```java
/**
 * 检查系列 - 用于统计聚合的可选标签
 *
 * 设计理念:
 * - 不是容器，而是标签
 * - 检查可以不属于任何系列
 * - 主要用途是统计分析时的分组
 * - 可以事后给检查打标签
 */
@Entity
@Table(name = "inspection_series")
public class InspectionSeries extends AggregateRoot {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;            // 系列名称，如"2024学年第一学期卫生检查"

    private String description;

    // ===== 默认配置 =====
    private Long defaultTemplateId; // 默认使用的模板

    @Enumerated(EnumType.STRING)
    private InputMode defaultInputMode;

    @Enumerated(EnumType.STRING)
    private ScoringMode defaultScoringMode;

    private Integer defaultBaseScore;

    // ===== 时间范围（用于统计筛选）=====
    private LocalDate startDate;
    private LocalDate endDate;

    // ===== 状态 =====
    @Enumerated(EnumType.STRING)
    private SeriesStatus status;    // ACTIVE | ARCHIVED

    // ===== 统计信息（冗余缓存）=====
    private Integer inspectionCount;  // 检查次数
    private BigDecimal averageScore;  // 平均分
    private LocalDate lastInspectionDate;

    // ===== 创建信息 =====
    private Long creatorId;
    private LocalDateTime createdAt;

    // ===== 领域方法 =====

    public static InspectionSeries create(CreateSeriesCommand cmd) {
        InspectionSeries series = new InspectionSeries();
        series.id = IdGenerator.nextId();
        series.name = cmd.getName();
        series.description = cmd.getDescription();
        series.defaultTemplateId = cmd.getTemplateId();
        series.defaultInputMode = cmd.getInputMode();
        series.defaultScoringMode = cmd.getScoringMode();
        series.defaultBaseScore = cmd.getBaseScore() != null ? cmd.getBaseScore() : 100;
        series.startDate = cmd.getStartDate();
        series.endDate = cmd.getEndDate();
        series.status = SeriesStatus.ACTIVE;
        series.inspectionCount = 0;
        series.creatorId = cmd.getCreatorId();
        series.createdAt = LocalDateTime.now();
        return series;
    }

    public void refreshStatistics(int count, BigDecimal avgScore, LocalDate lastDate) {
        this.inspectionCount = count;
        this.averageScore = avgScore;
        this.lastInspectionDate = lastDate;
    }

    public void archive() {
        this.status = SeriesStatus.ARCHIVED;
    }
}
```

### 2.4 ClassRecord (班级记录) - 保持V4设计

```java
/**
 * 班级检查记录 - 与V4基本相同
 *
 * 变更点:
 * - sessionId 改为 inspectionId
 * - 其他保持不变
 */
@Entity
@Table(name = "class_inspection_records")
public class ClassRecord extends AggregateRoot {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long inspectionId;      // 变更：原 sessionId

    @Column(nullable = false)
    private Long classId;

    private String className;       // 冗余快照
    private Long orgUnitId;
    private String orgUnitName;     // 冗余快照

    // ===== 分数 =====
    private Integer baseScore;
    private BigDecimal totalDeduction;
    private BigDecimal bonusScore;
    private BigDecimal finalScore;

    // ===== 状态 =====
    @Enumerated(EnumType.STRING)
    private RecordStatus status;    // PENDING | RECORDING | COMPLETED

    // ===== 嵌入集合 =====
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Deduction> deductions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChecklistResponse> responses = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bonus> bonuses = new ArrayList<>();

    // 其他方法与V4相同...
}
```

### 2.5 Appeal (申诉) - 全新设计

```java
/**
 * 申诉 - V5全新设计
 *
 * 核心变更:
 * - 直接关联 Inspection（不是旧的 InspectionRecord）
 * - 简化状态机（9状态 → 5状态）
 * - 支持批量申诉
 */
@Entity
@Table(name = "appeals_v5")
public class Appeal extends AggregateRoot {

    @Id
    private Long id;

    @Column(unique = true)
    private String appealCode;      // 申诉编号

    // ===== 核心关联 =====
    @Column(nullable = false)
    private Long inspectionId;      // 关联的检查

    @Column(nullable = false)
    private Long classRecordId;     // 关联的班级记录

    private Long deductionId;       // 申诉的具体扣分项（可选，批量申诉时为null）

    // ===== 申诉信息 =====
    @Enumerated(EnumType.STRING)
    private AppealType type;        // SINGLE_DEDUCTION | MULTIPLE_DEDUCTIONS | FULL_RECORD

    private String reason;          // 申诉理由

    private List<String> evidenceUrls; // 佐证材料

    // ===== 分数信息 =====
    private BigDecimal originalDeduction;  // 原扣分
    private BigDecimal requestedDeduction; // 申请调整为
    private BigDecimal approvedDeduction;  // 批准的扣分

    // ===== 状态 =====
    @Enumerated(EnumType.STRING)
    private AppealStatus status;    // PENDING | REVIEWING | APPROVED | REJECTED | WITHDRAWN

    // ===== 申诉人 =====
    private Long appellantId;       // 申诉人ID
    private String appellantName;
    private Long classId;
    private String className;

    // ===== 审批信息 =====
    @OneToMany(cascade = CascadeType.ALL)
    private List<AppealApproval> approvals = new ArrayList<>();

    // ===== 时间线 =====
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime resolvedAt;

    // ===== 领域方法 =====

    public static Appeal create(CreateAppealCommand cmd) {
        Appeal appeal = new Appeal();
        appeal.id = IdGenerator.nextId();
        appeal.appealCode = generateCode();
        appeal.inspectionId = cmd.getInspectionId();
        appeal.classRecordId = cmd.getClassRecordId();
        appeal.deductionId = cmd.getDeductionId();
        appeal.type = cmd.getType();
        appeal.reason = cmd.getReason();
        appeal.evidenceUrls = cmd.getEvidenceUrls();
        appeal.originalDeduction = cmd.getOriginalDeduction();
        appeal.requestedDeduction = cmd.getRequestedDeduction();
        appeal.status = AppealStatus.PENDING;
        appeal.appellantId = cmd.getAppellantId();
        appeal.appellantName = cmd.getAppellantName();
        appeal.classId = cmd.getClassId();
        appeal.className = cmd.getClassName();
        appeal.createdAt = LocalDateTime.now();
        return appeal;
    }

    public void startReview(Long reviewerId, String reviewerName) {
        if (this.status != AppealStatus.PENDING) {
            throw new IllegalStateException("只有待处理的申诉可以开始审核");
        }
        this.status = AppealStatus.REVIEWING;
        this.reviewedAt = LocalDateTime.now();
        this.approvals.add(AppealApproval.startReview(reviewerId, reviewerName));
    }

    public void approve(Long approverId, String approverName, BigDecimal approvedDeduction, String comment) {
        if (this.status != AppealStatus.REVIEWING) {
            throw new IllegalStateException("只有审核中的申诉可以批准");
        }
        this.status = AppealStatus.APPROVED;
        this.approvedDeduction = approvedDeduction;
        this.resolvedAt = LocalDateTime.now();
        this.approvals.add(AppealApproval.approve(approverId, approverName, approvedDeduction, comment));
        registerEvent(new AppealApprovedEvent(this.id, this.classRecordId,
            this.originalDeduction, this.approvedDeduction));
    }

    public void reject(Long approverId, String approverName, String reason) {
        if (this.status != AppealStatus.REVIEWING) {
            throw new IllegalStateException("只有审核中的申诉可以驳回");
        }
        this.status = AppealStatus.REJECTED;
        this.resolvedAt = LocalDateTime.now();
        this.approvals.add(AppealApproval.reject(approverId, approverName, reason));
        registerEvent(new AppealRejectedEvent(this.id));
    }

    public void withdraw() {
        if (this.status == AppealStatus.APPROVED || this.status == AppealStatus.REJECTED) {
            throw new IllegalStateException("已处理的申诉不能撤回");
        }
        this.status = AppealStatus.WITHDRAWN;
        registerEvent(new AppealWithdrawnEvent(this.id));
    }

    public BigDecimal getScoreAdjustment() {
        if (this.status != AppealStatus.APPROVED) {
            return BigDecimal.ZERO;
        }
        return this.originalDeduction.subtract(this.approvedDeduction);
    }
}
```

### 2.6 状态枚举

```java
/**
 * 检查状态 - 简化为5个状态
 */
public enum InspectionStatus {
    DRAFT,        // 草稿：刚创建，未开始
    IN_PROGRESS,  // 进行中：正在录入数据
    SUBMITTED,    // 已提交：等待审核发布
    PUBLISHED,    // 已发布：分数生效，可以申诉
    CANCELLED     // 已取消：作废
}

/**
 * 申诉状态 - 简化为5个状态
 */
public enum AppealStatus {
    PENDING,      // 待处理
    REVIEWING,    // 审核中
    APPROVED,     // 已批准
    REJECTED,     // 已驳回
    WITHDRAWN     // 已撤回
}

/**
 * 系列状态
 */
public enum SeriesStatus {
    ACTIVE,       // 活跃
    ARCHIVED      // 已归档
}
```

---

## 三、数据库设计

### 3.1 新表结构

```sql
-- =====================================================
-- V5 检查系统数据库设计
-- =====================================================

-- 1. 检查表（核心表）
CREATE TABLE inspections (
    id BIGINT PRIMARY KEY,
    inspection_code VARCHAR(32) NOT NULL UNIQUE,

    -- 关联
    template_id BIGINT NOT NULL,
    series_id BIGINT NULL,                    -- 可选的系列关联
    template_version INT DEFAULT 1,

    -- 检查信息
    inspection_date DATE NOT NULL,
    title VARCHAR(200),
    description TEXT,

    -- 配置
    input_mode VARCHAR(20) NOT NULL DEFAULT 'SPACE_FIRST',
    scoring_mode VARCHAR(20) NOT NULL DEFAULT 'BASE_SCORE',
    base_score INT NOT NULL DEFAULT 100,
    inspection_level VARCHAR(20) DEFAULT 'CLASS',

    -- 状态
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- 操作人
    creator_id BIGINT,
    inspector_id BIGINT,
    publisher_id BIGINT,

    -- 时间线
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME,
    submitted_at DATETIME,
    published_at DATETIME,

    -- 统计快照
    total_classes INT DEFAULT 0,
    average_score DECIMAL(5,2),
    deduction_count INT DEFAULT 0,

    -- 索引
    INDEX idx_template (template_id),
    INDEX idx_series (series_id),
    INDEX idx_date (inspection_date),
    INDEX idx_status (status),
    INDEX idx_creator (creator_id),

    -- 外键
    FOREIGN KEY (template_id) REFERENCES inspection_templates(id),
    FOREIGN KEY (series_id) REFERENCES inspection_series(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 检查系列表
CREATE TABLE inspection_series (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,

    -- 默认配置
    default_template_id BIGINT,
    default_input_mode VARCHAR(20),
    default_scoring_mode VARCHAR(20),
    default_base_score INT DEFAULT 100,

    -- 时间范围
    start_date DATE,
    end_date DATE,

    -- 状态
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 统计缓存
    inspection_count INT DEFAULT 0,
    average_score DECIMAL(5,2),
    last_inspection_date DATE,

    -- 创建信息
    creator_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,

    -- 索引
    INDEX idx_status (status),
    INDEX idx_template (default_template_id),
    INDEX idx_date_range (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 班级检查记录表（基本不变，改字段名）
CREATE TABLE class_records (
    id BIGINT PRIMARY KEY,
    inspection_id BIGINT NOT NULL,            -- 原 session_id

    -- 班级信息
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),
    org_unit_id BIGINT,
    org_unit_name VARCHAR(100),

    -- 分数
    base_score INT NOT NULL DEFAULT 100,
    total_deduction DECIMAL(10,2) DEFAULT 0,
    bonus_score DECIMAL(10,2) DEFAULT 0,
    final_score DECIMAL(10,2) DEFAULT 100,

    -- 状态
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- 时间
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,

    -- 索引
    INDEX idx_inspection (inspection_id),
    INDEX idx_class (class_id),
    INDEX idx_org_unit (org_unit_id),
    UNIQUE INDEX uk_inspection_class (inspection_id, class_id),

    -- 外键
    FOREIGN KEY (inspection_id) REFERENCES inspections(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 扣分明细表（基本不变）
CREATE TABLE deductions (
    id BIGINT PRIMARY KEY,
    inspection_id BIGINT NOT NULL,
    class_record_id BIGINT NOT NULL,

    -- 扣分项信息
    deduction_item_id BIGINT,
    item_name VARCHAR(200),
    category_name VARCHAR(100),

    -- 空间信息
    space_type VARCHAR(20),
    space_id BIGINT,
    space_name VARCHAR(100),

    -- 人员信息
    student_ids JSON,
    student_names JSON,
    person_count INT DEFAULT 0,

    -- 扣分
    deduction_amount DECIMAL(10,2) NOT NULL,
    input_source VARCHAR(30),

    -- 佐证
    evidence_urls JSON,
    remark TEXT,

    -- 记录人
    recorded_by BIGINT,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 索引
    INDEX idx_inspection (inspection_id),
    INDEX idx_class_record (class_record_id),
    INDEX idx_item (deduction_item_id),

    -- 外键
    FOREIGN KEY (class_record_id) REFERENCES class_records(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. V5申诉表（全新设计）
CREATE TABLE appeals_v5 (
    id BIGINT PRIMARY KEY,
    appeal_code VARCHAR(32) NOT NULL UNIQUE,

    -- 关联
    inspection_id BIGINT NOT NULL,
    class_record_id BIGINT NOT NULL,
    deduction_id BIGINT,                      -- 可选，批量申诉时为null

    -- 申诉信息
    appeal_type VARCHAR(30) NOT NULL,         -- SINGLE_DEDUCTION, MULTIPLE_DEDUCTIONS, FULL_RECORD
    reason TEXT NOT NULL,
    evidence_urls JSON,

    -- 分数
    original_deduction DECIMAL(10,2) NOT NULL,
    requested_deduction DECIMAL(10,2) NOT NULL,
    approved_deduction DECIMAL(10,2),

    -- 状态
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- 申诉人
    appellant_id BIGINT NOT NULL,
    appellant_name VARCHAR(100),
    class_id BIGINT,
    class_name VARCHAR(100),

    -- 时间线
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME,
    resolved_at DATETIME,

    -- 索引
    INDEX idx_inspection (inspection_id),
    INDEX idx_class_record (class_record_id),
    INDEX idx_status (status),
    INDEX idx_appellant (appellant_id),
    INDEX idx_created (created_at),

    -- 外键
    FOREIGN KEY (inspection_id) REFERENCES inspections(id),
    FOREIGN KEY (class_record_id) REFERENCES class_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 申诉审批记录表
CREATE TABLE appeal_approvals_v5 (
    id BIGINT PRIMARY KEY,
    appeal_id BIGINT NOT NULL,

    -- 审批信息
    action VARCHAR(20) NOT NULL,              -- START_REVIEW, APPROVE, REJECT
    approver_id BIGINT NOT NULL,
    approver_name VARCHAR(100),
    approved_deduction DECIMAL(10,2),
    comment TEXT,

    -- 时间
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 外键
    FOREIGN KEY (appeal_id) REFERENCES appeals_v5(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.2 数据迁移SQL

```sql
-- =====================================================
-- V4 → V5 数据迁移脚本
-- =====================================================

-- 1. 将 Plan 转换为 Series
INSERT INTO inspection_series (
    id, name, description, default_template_id,
    start_date, end_date, status, creator_id, created_at
)
SELECT
    id, name, description, template_id,
    start_date, end_date,
    CASE WHEN status = 'ACTIVE' THEN 'ACTIVE' ELSE 'ARCHIVED' END,
    creator_id, created_at
FROM inspection_plans
WHERE deleted = 0;

-- 2. 将 Session 转换为 Inspection
INSERT INTO inspections (
    id, inspection_code, template_id, series_id, template_version,
    inspection_date, title, input_mode, scoring_mode, base_score,
    inspection_level, status, creator_id, inspector_id, publisher_id,
    created_at, started_at, submitted_at, published_at,
    total_classes, average_score, deduction_count
)
SELECT
    s.id,
    s.session_code,
    s.template_id,
    s.plan_id,                                -- plan_id 变为 series_id
    s.template_version,
    s.inspection_date,
    s.inspection_period,                      -- 用作 title
    s.input_mode,
    s.scoring_mode,
    s.base_score,
    s.inspection_level,
    CASE s.status
        WHEN 'CREATED' THEN 'DRAFT'
        WHEN 'IN_PROGRESS' THEN 'IN_PROGRESS'
        WHEN 'SUBMITTED' THEN 'SUBMITTED'
        WHEN 'PUBLISHED' THEN 'PUBLISHED'
        ELSE 'CANCELLED'
    END,
    s.creator_id,
    s.inspector_id,
    s.publisher_id,
    s.created_at,
    s.started_at,
    s.submitted_at,
    s.published_at,
    (SELECT COUNT(*) FROM class_inspection_records WHERE session_id = s.id),
    (SELECT AVG(final_score) FROM class_inspection_records WHERE session_id = s.id),
    (SELECT COUNT(*) FROM inspection_deductions WHERE session_id = s.id)
FROM inspection_sessions s
WHERE s.deleted = 0;

-- 3. 迁移班级记录（改字段名）
INSERT INTO class_records (
    id, inspection_id, class_id, class_name, org_unit_id, org_unit_name,
    base_score, total_deduction, bonus_score, final_score, status,
    created_at, updated_at
)
SELECT
    id, session_id, class_id, class_name, org_unit_id, org_unit_name,
    base_score, total_deduction, bonus_score, final_score, status,
    created_at, updated_at
FROM class_inspection_records
WHERE deleted = 0;

-- 4. 迁移扣分明细（改字段名）
INSERT INTO deductions (
    id, inspection_id, class_record_id, deduction_item_id, item_name,
    category_name, space_type, space_id, space_name, student_ids,
    student_names, person_count, deduction_amount, input_source,
    evidence_urls, remark, recorded_by, recorded_at
)
SELECT
    id, session_id, class_record_id, deduction_item_id, item_name,
    category_name, space_type, space_id, space_name, student_ids,
    student_names, person_count, deduction_amount, input_source,
    evidence_urls, remark, recorded_by, recorded_at
FROM inspection_deductions
WHERE deleted = 0;

-- 5. 更新 Series 的统计信息
UPDATE inspection_series s SET
    inspection_count = (SELECT COUNT(*) FROM inspections WHERE series_id = s.id),
    average_score = (SELECT AVG(average_score) FROM inspections WHERE series_id = s.id),
    last_inspection_date = (SELECT MAX(inspection_date) FROM inspections WHERE series_id = s.id);
```

---

## 四、API设计

### 4.1 检查API（核心）

```yaml
# =====================================================
# 检查 API - RESTful 设计
# =====================================================

# 检查 CRUD
POST   /api/v5/inspections                    # 创建检查
GET    /api/v5/inspections                    # 查询检查列表
GET    /api/v5/inspections/{id}               # 获取检查详情
PUT    /api/v5/inspections/{id}               # 更新检查
DELETE /api/v5/inspections/{id}               # 删除检查（仅草稿）

# 检查状态操作
POST   /api/v5/inspections/{id}/start         # 开始检查
POST   /api/v5/inspections/{id}/submit        # 提交检查
POST   /api/v5/inspections/{id}/publish       # 发布检查
POST   /api/v5/inspections/{id}/cancel        # 取消检查

# 班级记录
GET    /api/v5/inspections/{id}/records       # 获取班级记录列表
POST   /api/v5/inspections/{id}/records       # 初始化班级记录
GET    /api/v5/inspections/{id}/records/{classId}  # 获取单个班级记录

# 扣分操作
POST   /api/v5/inspections/{id}/deductions/space    # 按空间扣分
POST   /api/v5/inspections/{id}/deductions/person   # 按人员扣分
POST   /api/v5/inspections/{id}/deductions/class    # 按班级扣分
POST   /api/v5/inspections/{id}/deductions/checklist # 清单扣分
DELETE /api/v5/inspections/{id}/deductions/{deductionId}  # 删除扣分

# 加分操作
POST   /api/v5/inspections/{id}/bonuses       # 添加加分
DELETE /api/v5/inspections/{id}/bonuses/{bonusId}  # 删除加分

# 快速开始（一步创建+开始）
POST   /api/v5/inspections/quick-start        # 快速开始检查
```

### 4.2 系列API

```yaml
# =====================================================
# 检查系列 API
# =====================================================

# 系列 CRUD
POST   /api/v5/series                         # 创建系列
GET    /api/v5/series                         # 查询系列列表
GET    /api/v5/series/{id}                    # 获取系列详情
PUT    /api/v5/series/{id}                    # 更新系列
DELETE /api/v5/series/{id}                    # 删除系列

# 系列操作
POST   /api/v5/series/{id}/archive            # 归档系列
GET    /api/v5/series/{id}/inspections        # 获取系列下的检查列表
GET    /api/v5/series/{id}/statistics         # 获取系列统计

# 给检查打标签
POST   /api/v5/inspections/{id}/series        # 将检查加入系列
DELETE /api/v5/inspections/{id}/series        # 将检查移出系列
```

### 4.3 申诉API（V5）

```yaml
# =====================================================
# 申诉 API - V5全新设计
# =====================================================

# 申诉 CRUD
POST   /api/v5/appeals                        # 提交申诉
GET    /api/v5/appeals                        # 查询申诉列表
GET    /api/v5/appeals/{id}                   # 获取申诉详情

# 申诉操作
POST   /api/v5/appeals/{id}/review            # 开始审核
POST   /api/v5/appeals/{id}/approve           # 批准申诉
POST   /api/v5/appeals/{id}/reject            # 驳回申诉
POST   /api/v5/appeals/{id}/withdraw          # 撤回申诉

# 按检查查询申诉
GET    /api/v5/inspections/{id}/appeals       # 获取检查的所有申诉
```

### 4.4 统计API

```yaml
# =====================================================
# 统计分析 API
# =====================================================

# 多维度统计
GET    /api/v5/statistics/overview            # 总览统计
GET    /api/v5/statistics/by-template         # 按模板统计
GET    /api/v5/statistics/by-series           # 按系列统计
GET    /api/v5/statistics/by-department       # 按部门统计
GET    /api/v5/statistics/by-class            # 按班级统计
GET    /api/v5/statistics/trend               # 趋势分析

# 排名
GET    /api/v5/rankings/classes               # 班级排名
GET    /api/v5/rankings/departments           # 部门排名
```

### 4.5 核心DTO设计

```java
// ===== 创建检查请求 =====
@Data
public class CreateInspectionRequest {
    @NotNull
    private Long templateId;           // 必须：检查模板

    private Long seriesId;             // 可选：所属系列

    private LocalDate inspectionDate;  // 默认今天

    private String title;              // 默认自动生成

    private InputMode inputMode;       // 默认 SPACE_FIRST

    private ScoringMode scoringMode;   // 默认 BASE_SCORE

    private Integer baseScore;         // 默认 100
}

// ===== 快速开始请求 =====
@Data
public class QuickStartRequest {
    @NotNull
    private Long templateId;

    private LocalDate inspectionDate;  // 默认今天

    // 其他配置使用模板默认值或用户偏好
}

// ===== 检查响应 =====
@Data
public class InspectionVO {
    private Long id;
    private String inspectionCode;
    private String title;
    private LocalDate inspectionDate;
    private InspectionStatus status;

    // 关联信息
    private Long templateId;
    private String templateName;
    private Long seriesId;
    private String seriesName;

    // 配置
    private InputMode inputMode;
    private ScoringMode scoringMode;
    private Integer baseScore;

    // 统计
    private Integer totalClasses;
    private BigDecimal averageScore;
    private Integer deductionCount;

    // 时间
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    // 操作人
    private String creatorName;
    private String inspectorName;
}

// ===== 申诉请求 =====
@Data
public class CreateAppealRequest {
    @NotNull
    private Long inspectionId;

    @NotNull
    private Long classRecordId;

    private Long deductionId;          // 可选，批量申诉时不传

    @NotNull
    private AppealType type;

    @NotNull
    private String reason;

    private List<String> evidenceUrls;

    @NotNull
    private BigDecimal requestedDeduction;
}
```

---

## 五、前端设计

### 5.1 页面结构重组

```
/inspection (量化检查模块)
│
├── /inspection/dashboard              【新增】检查工作台（首页）
│   ├── 快速开始检查
│   ├── 今日/本周概览
│   ├── 最近检查记录
│   └── 功能入口导航
│
├── /inspection/list                   【新增】检查列表
│   ├── 全部检查记录
│   ├── 多维度筛选（模板/系列/状态/时间）
│   └── 批量操作
│
├── /inspection/:id                    【新增】检查详情
│   ├── 检查信息概览
│   ├── 班级记录列表
│   └── 操作按钮（继续/提交/发布）
│
├── /inspection/:id/execute            【保留】检查执行
│   └── 打分界面（保持现有设计）
│
├── /inspection/series                 【新增】系列管理
│   ├── 系列列表
│   ├── 创建/编辑系列
│   └── 系列统计
│
├── /inspection/series/:id             【新增】系列详情
│   ├── 系列信息
│   ├── 下属检查列表
│   └── 趋势统计图
│
├── /inspection/templates              【保留】模板管理
├── /inspection/appeals                【优化】申诉管理（使用V5 API）
├── /inspection/rankings               【保留】排名结果
├── /inspection/analytics              【保留】数据分析
├── /inspection/corrective-actions     【保留】整改管理
├── /inspection/student-behavior       【保留】学生行为
├── /inspection/teacher-dashboard      【保留】班主任工作台
└── /inspection/config                 【保留】系统配置

【删除】
- /inspection/check-plan               删除：被 /inspection/series 替代
- /inspection/check-plan/:id           删除：被 /inspection/series/:id 替代
```

### 5.2 检查工作台设计

```vue
<!-- InspectionDashboard.vue -->
<template>
  <div class="inspection-dashboard">
    <!-- 顶部：快速开始 -->
    <section class="quick-start">
      <h2>快速开始检查</h2>
      <div class="template-grid">
        <TemplateCard
          v-for="tpl in frequentTemplates"
          :key="tpl.id"
          :template="tpl"
          @click="quickStart(tpl)"
        />
        <AddTemplateCard @click="showTemplateSelector = true" />
      </div>
    </section>

    <!-- 中部：今日概览 -->
    <section class="today-overview">
      <StatCard title="今日检查" :value="overview.today" icon="calendar" />
      <StatCard title="进行中" :value="overview.inProgress" icon="clock" color="amber" />
      <StatCard title="待发布" :value="overview.pending" icon="upload" color="blue" />
      <StatCard title="待处理申诉" :value="overview.appeals" icon="alert" color="red" />
    </section>

    <!-- 下部：最近记录 + 功能入口 -->
    <div class="bottom-grid">
      <section class="recent-inspections">
        <h3>最近检查</h3>
        <InspectionList :inspections="recentInspections" compact />
        <router-link to="/inspection/list">查看全部 →</router-link>
      </section>

      <section class="quick-links">
        <h3>功能入口</h3>
        <QuickLinkGrid :links="quickLinks" />
      </section>
    </div>

    <!-- 快速开始对话框 -->
    <QuickStartDialog
      v-model="showQuickStart"
      :template="selectedTemplate"
      @confirm="handleQuickStart"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { quickStartInspection, getFrequentTemplates, getTodayOverview, getRecentInspections } from '@/api/inspection'

const router = useRouter()

const frequentTemplates = ref([])
const overview = ref({ today: 0, inProgress: 0, pending: 0, appeals: 0 })
const recentInspections = ref([])
const showQuickStart = ref(false)
const selectedTemplate = ref(null)

const quickLinks = [
  { path: '/inspection/list', label: '全部检查', icon: 'list' },
  { path: '/inspection/series', label: '检查系列', icon: 'folder' },
  { path: '/inspection/rankings', label: '排名结果', icon: 'trophy' },
  { path: '/inspection/appeals', label: '申诉管理', icon: 'message-square' },
  { path: '/inspection/analytics', label: '数据分析', icon: 'bar-chart' },
  { path: '/inspection/templates', label: '模板管理', icon: 'file-text' },
]

function quickStart(template) {
  selectedTemplate.value = template
  showQuickStart.value = true
}

async function handleQuickStart(config) {
  const inspection = await quickStartInspection({
    templateId: selectedTemplate.value.id,
    ...config
  })
  router.push(`/inspection/${inspection.id}/execute`)
}

onMounted(async () => {
  const [templates, ov, recent] = await Promise.all([
    getFrequentTemplates(),
    getTodayOverview(),
    getRecentInspections(5)
  ])
  frequentTemplates.value = templates
  overview.value = ov
  recentInspections.value = recent
})
</script>
```

### 5.3 快速开始对话框

```vue
<!-- QuickStartDialog.vue -->
<template>
  <el-dialog
    v-model="visible"
    :title="`开始 ${template?.name || '检查'}`"
    width="450px"
  >
    <el-form :model="form" label-width="80px">
      <!-- 核心配置（始终显示）-->
      <el-form-item label="检查日期">
        <el-date-picker
          v-model="form.inspectionDate"
          type="date"
          placeholder="默认今天"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <el-form-item label="检查名称">
        <el-input v-model="form.title" placeholder="自动生成，可修改" />
      </el-form-item>

      <!-- 可选：关联系列 -->
      <el-form-item label="所属系列">
        <el-select v-model="form.seriesId" placeholder="不关联系列（独立检查）" clearable>
          <el-option
            v-for="s in activeSeries"
            :key="s.id"
            :label="s.name"
            :value="s.id"
          />
        </el-select>
        <div class="form-tip">关联系列后，可在系列中统计此次检查</div>
      </el-form-item>

      <!-- 高级配置（折叠）-->
      <el-collapse v-model="showAdvanced">
        <el-collapse-item title="高级配置" name="advanced">
          <el-form-item label="录入模式">
            <el-radio-group v-model="form.inputMode">
              <el-radio value="SPACE_FIRST">按空间</el-radio>
              <el-radio value="ORG_FIRST">按班级</el-radio>
              <el-radio value="PERSON_FIRST">按人员</el-radio>
              <el-radio value="CHECKLIST">按清单</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="计分模式">
            <el-radio-group v-model="form.scoringMode">
              <el-radio value="BASE_SCORE">基准分制</el-radio>
              <el-radio value="DEDUCTION_ONLY">纯扣分制</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="form.scoringMode === 'BASE_SCORE'" label="基准分">
            <el-input-number v-model="form.baseScore" :min="0" :max="1000" />
          </el-form-item>
        </el-collapse-item>
      </el-collapse>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="confirm" :loading="loading">
        开始检查
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getActiveSeries } from '@/api/series'
import { formatDate } from '@/utils/date'

const props = defineProps<{
  modelValue: boolean
  template: any
}>()

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const showAdvanced = ref([])
const activeSeries = ref([])

const form = ref({
  inspectionDate: formatDate(new Date()),
  title: '',
  seriesId: null,
  inputMode: 'SPACE_FIRST',
  scoringMode: 'BASE_SCORE',
  baseScore: 100
})

// 自动生成标题
watch(() => [props.template, form.value.inspectionDate], () => {
  if (props.template && form.value.inspectionDate) {
    form.value.title = `${form.value.inspectionDate} ${props.template.name}`
  }
}, { immediate: true })

// 加载可用系列
watch(() => props.modelValue, async (val) => {
  if (val && props.template) {
    activeSeries.value = await getActiveSeries(props.template.id)
  }
})

function confirm() {
  emit('confirm', { ...form.value })
}
</script>
```

### 5.4 检查列表页

```vue
<!-- InspectionList.vue -->
<template>
  <div class="inspection-list-page">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="filters.templateId" placeholder="全部模板" clearable>
        <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>

      <el-select v-model="filters.seriesId" placeholder="全部系列" clearable>
        <el-option v-for="s in series" :key="s.id" :label="s.name" :value="s.id" />
        <el-option :value="0" label="独立检查（无系列）" />
      </el-select>

      <el-select v-model="filters.status" placeholder="全部状态" clearable>
        <el-option value="DRAFT" label="草稿" />
        <el-option value="IN_PROGRESS" label="进行中" />
        <el-option value="SUBMITTED" label="已提交" />
        <el-option value="PUBLISHED" label="已发布" />
      </el-select>

      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
      />

      <el-button type="primary" @click="showCreateDialog = true">
        <Plus class="w-4 h-4 mr-1" /> 新建检查
      </el-button>
    </div>

    <!-- 检查列表 -->
    <el-table :data="inspections" v-loading="loading">
      <el-table-column prop="inspectionCode" label="编号" width="150" />
      <el-table-column prop="title" label="检查名称" min-width="200">
        <template #default="{ row }">
          <router-link :to="`/inspection/${row.id}`" class="text-blue-600 hover:underline">
            {{ row.title }}
          </router-link>
        </template>
      </el-table-column>
      <el-table-column prop="templateName" label="模板" width="150" />
      <el-table-column prop="seriesName" label="系列" width="180">
        <template #default="{ row }">
          <span v-if="row.seriesName">{{ row.seriesName }}</span>
          <span v-else class="text-gray-400">独立检查</span>
        </template>
      </el-table-column>
      <el-table-column prop="inspectionDate" label="检查日期" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <StatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="totalClasses" label="班级数" width="80" align="center" />
      <el-table-column prop="averageScore" label="平均分" width="80" align="center">
        <template #default="{ row }">
          {{ row.averageScore?.toFixed(1) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'DRAFT' || row.status === 'IN_PROGRESS'"
            type="primary"
            link
            @click="continueInspection(row)"
          >
            继续
          </el-button>
          <el-button
            v-if="row.status === 'SUBMITTED'"
            type="success"
            link
            @click="publishInspection(row)"
          >
            发布
          </el-button>
          <el-button type="info" link @click="viewDetail(row)">查看</el-button>
          <el-dropdown v-if="row.status !== 'PUBLISHED'">
            <el-button type="info" link>更多</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="cancelInspection(row)">取消</el-dropdown-item>
                <el-dropdown-item v-if="!row.seriesId" @click="addToSeries(row)">
                  加入系列
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      layout="total, sizes, prev, pager, next"
      @change="loadInspections"
    />
  </div>
</template>
```

### 5.5 系列管理页

```vue
<!-- SeriesList.vue -->
<template>
  <div class="series-list-page">
    <div class="page-header">
      <h1>检查系列</h1>
      <p class="text-gray-500">将相同类型的检查归类到系列中，便于统计分析</p>
      <el-button type="primary" @click="showCreateDialog = true">
        <Plus class="w-4 h-4 mr-1" /> 新建系列
      </el-button>
    </div>

    <!-- 系列卡片网格 -->
    <div class="series-grid">
      <div
        v-for="s in seriesList"
        :key="s.id"
        class="series-card"
        @click="goToDetail(s)"
      >
        <div class="card-header">
          <h3>{{ s.name }}</h3>
          <StatusBadge :status="s.status" />
        </div>

        <div class="card-meta">
          <span>模板: {{ s.defaultTemplateName }}</span>
          <span v-if="s.startDate">{{ s.startDate }} ~ {{ s.endDate || '至今' }}</span>
        </div>

        <div class="card-stats">
          <div class="stat">
            <span class="stat-value">{{ s.inspectionCount }}</span>
            <span class="stat-label">检查次数</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ s.averageScore?.toFixed(1) || '-' }}</span>
            <span class="stat-label">平均分</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ s.lastInspectionDate || '-' }}</span>
            <span class="stat-label">最近检查</span>
          </div>
        </div>

        <div class="card-actions">
          <el-button type="primary" size="small" @click.stop="createInspection(s)">
            新建检查
          </el-button>
          <el-button size="small" @click.stop="goToDetail(s)">
            查看详情
          </el-button>
        </div>
      </div>
    </div>

    <!-- 创建系列对话框 -->
    <CreateSeriesDialog v-model="showCreateDialog" @created="loadSeries" />
  </div>
</template>
```

---

## 六、与现有模块的兼容性

### 6.1 兼容性矩阵

| 模块 | 影响 | 改动点 | 工作量 |
|------|------|--------|--------|
| **排名结果** | 低 | 将 sessionId 改为 inspectionId | 1天 |
| **申诉管理** | 高 | 使用全新V5 API | 3天 |
| **整改工单** | 低 | 将 sessionId 改为 inspectionId | 1天 |
| **数据分析** | 中 | 新增按系列统计维度 | 2天 |
| **学生行为** | 低 | 事件名改变 | 0.5天 |
| **班主任台** | 低 | API路径变化 | 1天 |
| **评级统计** | 低 | 无影响 | 0天 |
| **导出中心** | 中 | 新增系列导出 | 1天 |

### 6.2 事件驱动兼容

```java
// 旧事件
SessionPublishedEvent →
// 新事件
InspectionPublishedEvent

// 监听器更新
@EventListener
public void onInspectionPublished(InspectionPublishedEvent event) {
    // 原有逻辑不变，只是事件名变化
    rankingService.calculateRankings(event.getInspectionId());
    behaviorService.syncBehaviorRecords(event.getInspectionId());
    notificationService.notifyPublished(event.getInspectionId());
}
```

### 6.3 API兼容层

```java
/**
 * V4 兼容层 - 支持旧版API调用
 * 在过渡期保留，6个月后移除
 */
@RestController
@RequestMapping("/inspection/sessions")
@Deprecated
public class V4CompatibilityController {

    private final InspectionApplicationService inspectionService;

    @PostMapping
    public Result<InspectionVO> createSession(@RequestBody CreateSessionRequest request) {
        // 转换为V5请求
        CreateInspectionRequest v5Request = convertToV5(request);
        return inspectionService.createInspection(v5Request);
    }

    @GetMapping("/{sessionId}")
    public Result<InspectionVO> getSession(@PathVariable Long sessionId) {
        // sessionId 和 inspectionId 是同一个ID
        return inspectionService.getInspection(sessionId);
    }

    // ... 其他兼容方法
}
```

---

## 七、实施计划

### 7.1 阶段划分

```
Phase 1: 数据模型重构 (1周)
├── 创建新表结构
├── 编写数据迁移脚本
├── 实现新的领域模型
├── 编写单元测试
└── 验证数据迁移

Phase 2: 后端API重构 (1周)
├── 实现 InspectionController
├── 实现 SeriesController
├── 实现 AppealController (V5)
├── 实现兼容层
├── 编写API测试
└── Swagger文档

Phase 3: 前端重构 (2周)
├── 实现 InspectionDashboard
├── 实现 InspectionList
├── 实现 SeriesList / SeriesDetail
├── 重构 AppealManagement
├── 更新路由配置
└── 前端集成测试

Phase 4: 集成与迁移 (1周)
├── 数据迁移执行
├── 全链路测试
├── 性能测试
├── 用户验收测试
└── 文档更新

Phase 5: 旧代码清理 (1周)
├── 移除V3代码
├── 移除V4兼容层（延后6个月）
├── 清理数据库旧表
└── 最终回归测试
```

### 7.2 风险控制

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 数据迁移丢失 | 低 | 高 | 完整备份 + 迁移验证脚本 |
| API不兼容 | 中 | 高 | 保留兼容层6个月 |
| 前端bug | 中 | 中 | 分模块灰度发布 |
| 性能下降 | 低 | 中 | 添加必要索引 + 监控 |

### 7.3 回滚方案

```sql
-- 如需回滚到V4
-- 1. 停止V5服务
-- 2. 恢复V4代码
-- 3. 执行以下SQL恢复数据

-- 恢复 Session 表
UPDATE inspection_sessions s
SET s.plan_id = (
    SELECT i.series_id FROM inspections i WHERE i.id = s.id
)
WHERE EXISTS (SELECT 1 FROM inspections i WHERE i.id = s.id);

-- 其他表数据保持不变（字段名兼容）
```

---

## 八、总结

### 8.1 核心改进

| 方面 | V4 | V5 | 改进 |
|------|----|----|------|
| **概念数量** | 4个（Plan/Session/Record/Deduction） | 3个（Inspection/Record/Deduction） | 减少25% |
| **创建流程** | 4步 | 2步 | 减少50% |
| **申诉模型** | 与V3绑定，9状态 | 独立V5，5状态 | 大幅简化 |
| **统计维度** | 仅按Plan | 按模板/系列/时间多维度 | 灵活性提升 |
| **概念理解** | Plan与Session混淆 | 检查+可选系列标签 | 清晰直观 |

### 8.2 用户体验提升

**快速检查场景**:
```
旧流程: 进入计划列表 → 选择/创建计划 → 进入详情 → 创建会话 → 填写配置 → 开始
新流程: 点击模板 → 确认配置 → 开始
```

**有规划的检查场景**:
```
旧流程: 创建计划 → 配置计划 → 进入计划 → 创建会话 → 执行
新流程: 创建系列（可选）→ 点击"新建检查" → 选择系列 → 执行
```

### 8.3 技术债清理

- ✅ 移除V3遗留代码
- ✅ 统一数据模型
- ✅ 申诉与检查数据对齐
- ✅ 简化状态机
- ✅ 清晰的领域边界

---

**文档版本**: 2.0
**最后更新**: 2026-01-30
**作者**: Claude (AI Assistant)
