# 学生管理系统 - 终极架构设计方案

**版本**: v1.0
**日期**: 2026-01-02
**设计原则**: 业界最佳实践，不考虑重构成本，追求最优架构

---

## 一、架构概览

### 1.1 设计理念

本方案采用**领域驱动设计（DDD）**作为核心架构思想，结合以下业界最佳实践：

| 方面 | 采用方案 | 参考来源 |
|------|---------|---------|
| 整体架构 | 六边形架构 (Hexagonal) | Alistair Cockburn |
| 领域建模 | DDD 聚合根模式 | Eric Evans |
| 权限模型 | RBAC + ABAC 混合 | AWS IAM, 阿里云 RAM |
| 数据权限 | Casbin 策略引擎 | 开源社区 |
| 事件架构 | 领域事件 + 事件溯源 | Martin Fowler |
| API 设计 | RESTful + CQRS | Microsoft |

### 1.2 限界上下文

```
┌───────────────────────────────────────────────────────────────────┐
│                         学生管理系统                               │
├───────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐  │
│  │ 身份与访问  │  │  组织管理   │  │  学生管理   │  │  量化检查   │  │
│  │  Context   │  │  Context   │  │  Context   │  │  Context   │  │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘  │
│                                                                    │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐  │
│  │  资产管理   │  │ 任务工作流  │  │  消息通知   │  │  审计日志   │  │
│  │  Context   │  │  Context   │  │  Context   │  │  Context   │  │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘  │
│                                                                    │
└───────────────────────────────────────────────────────────────────┘
```

---

## 二、组织架构设计

### 2.1 核心实体关系

```
org_units (组织单元)           academic_years (学年)
     │                               │
     ├── majors (专业)               │
     │      └── major_directions     │
     │                               │
     └─────────┬─────────────────────┘
               │
               ↓
          classes (班级)
               │
               ├── teacher_assignments (教师任职)
               │
               └── grade_directors (年级主任配置)
```

### 2.2 关键表结构

#### org_units (组织单元)
```sql
CREATE TABLE org_units (
    id BIGINT PRIMARY KEY,
    unit_code VARCHAR(50) NOT NULL,
    unit_name VARCHAR(100) NOT NULL,
    unit_type ENUM('SCHOOL', 'COLLEGE', 'DEPARTMENT', 'TEACHING_GROUP'),
    parent_id BIGINT,
    tree_path VARCHAR(500),
    tree_level INT DEFAULT 1,
    leader_id BIGINT,
    deputy_leader_ids JSON,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    -- 审计字段...
    UNIQUE KEY uk_unit_code (unit_code)
);
```

#### classes (班级)
```sql
CREATE TABLE classes (
    id BIGINT PRIMARY KEY,
    class_code VARCHAR(50) NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    short_name VARCHAR(50),

    -- 多维度归属
    org_unit_id BIGINT NOT NULL,
    enrollment_year INT NOT NULL,
    grade_level INT NOT NULL,
    major_direction_id BIGINT,

    -- 班级属性
    class_type ENUM('NORMAL', 'KEY', 'EXPERIMENTAL', 'INTERNATIONAL'),
    max_capacity INT DEFAULT 50,
    current_count INT DEFAULT 0,

    -- 毕业信息
    expected_graduation_year INT,
    graduation_status ENUM('STUDYING', 'GRADUATED', 'DISSOLVED'),

    version INT DEFAULT 0,
    -- 审计字段...
    UNIQUE KEY uk_class_code (class_code)
);
```

#### teacher_assignments (教师任职)
```sql
CREATE TABLE teacher_assignments (
    id BIGINT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,

    role_type ENUM('HEAD_TEACHER', 'DEPUTY_HEAD', 'SUBJECT_TEACHER', 'COUNSELOR'),
    subject_id BIGINT,

    is_primary TINYINT DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE,

    status ENUM('ACTIVE', 'TRANSFERRED', 'RESIGNED', 'EXPIRED'),
    transfer_reason VARCHAR(500),
    handover_teacher_id BIGINT,

    -- 审计字段...
    UNIQUE KEY uk_primary_teacher (class_id, role_type, is_primary, status)
);
```

---

## 三、量化检查设计

### 3.1 核心流程

```
模板定义 → 创建检查 → 录入扣分 → 提交审核 → 发布结果 → 申诉处理 → 定稿归档
    ↓           ↓          ↓          ↓          ↓          ↓
版本控制    复制快照    关联证据    权限检查    计算评级    重算得分
```

### 3.2 模板系统

#### inspection_templates
```sql
CREATE TABLE inspection_templates (
    id BIGINT PRIMARY KEY,
    template_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    description TEXT,
    current_version INT DEFAULT 1,
    applicable_scope ENUM('ALL', 'DEPARTMENT', 'GRADE', 'CUSTOM'),
    applicable_config JSON,
    default_weight_scheme_id BIGINT,
    status ENUM('DRAFT', 'PUBLISHED', 'DEPRECATED'),
    is_default TINYINT DEFAULT 0,
    -- 审计字段...
    UNIQUE KEY uk_template_code (template_code)
);
```

#### inspection_rounds (轮次 - 正规化)
```sql
CREATE TABLE inspection_rounds (
    id BIGINT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    round_code VARCHAR(30) NOT NULL,
    round_name VARCHAR(50) NOT NULL,
    scheduled_start_time TIME,
    scheduled_end_time TIME,
    weight DECIMAL(5,4) DEFAULT 1.0000,
    is_required TINYINT DEFAULT 1,
    applicable_days JSON,
    sort_order INT DEFAULT 0,
    UNIQUE KEY uk_template_round (template_id, round_code)
);
```

#### deduction_items (扣分项)
```sql
CREATE TABLE deduction_items (
    id BIGINT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    description TEXT,

    deduction_mode ENUM('FIXED', 'PER_PERSON', 'RANGE', 'FORMULA'),

    -- 固定扣分
    fixed_score DECIMAL(6,2),

    -- 按人数扣分
    base_score DECIMAL(6,2),
    per_person_score DECIMAL(6,2),
    max_persons INT,

    -- 区间扣分
    range_config JSON,

    -- 扣分限制
    min_score DECIMAL(6,2) DEFAULT 0,
    max_score DECIMAL(6,2),

    -- 附加配置
    requires_photo TINYINT DEFAULT 0,
    requires_student_select TINYINT DEFAULT 0,
    check_points JSON,

    sort_order INT DEFAULT 0,
    UNIQUE KEY uk_category_item (category_id, item_code)
);
```

### 3.3 检查记录

#### inspection_records
```sql
CREATE TABLE inspection_records (
    id BIGINT PRIMARY KEY,
    record_code VARCHAR(50) NOT NULL,
    record_name VARCHAR(100) NOT NULL,
    check_date DATE NOT NULL,
    check_type ENUM('DAILY', 'WEEKLY', 'SPECIAL', 'RANDOM'),

    template_id BIGINT NOT NULL,
    template_version INT NOT NULL,

    -- 快照 (不可变)
    template_snapshot JSON NOT NULL,
    weight_scheme_snapshot JSON,

    -- 检查范围
    scope_type ENUM('ALL', 'DEPARTMENT', 'GRADE', 'CUSTOM'),
    scope_config JSON,

    -- 统计数据
    total_classes INT,
    total_deduction DECIMAL(10,2),
    avg_deduction DECIMAL(10,2),
    max_deduction DECIMAL(10,2),
    min_deduction DECIMAL(10,2),

    -- 状态机
    status ENUM('DRAFT', 'CHECKING', 'SUBMITTED', 'REVIEWING',
                'PUBLISHED', 'APPEALING', 'FINALIZED', 'ARCHIVED'),

    -- 流程时间戳
    check_started_at DATETIME,
    submitted_at DATETIME,
    reviewed_at DATETIME,
    published_at DATETIME,
    finalized_at DATETIME,

    -- 责任人
    checker_id BIGINT,
    reviewer_id BIGINT,
    publisher_id BIGINT,

    version INT DEFAULT 0,
    UNIQUE KEY uk_record_code (record_code)
);
```

#### class_scores (班级得分)
```sql
CREATE TABLE class_scores (
    id BIGINT PRIMARY KEY,
    record_id BIGINT NOT NULL,

    -- 班级信息快照
    class_id BIGINT NOT NULL,
    class_code VARCHAR(50) NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    class_size INT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    org_unit_name VARCHAR(100),

    -- 得分
    original_deduction DECIMAL(10,2) DEFAULT 0,
    weight_coefficient DECIMAL(5,4) DEFAULT 1.0000,
    weighted_deduction DECIMAL(10,2),
    final_score DECIMAL(10,2),
    final_deduction DECIMAL(10,2),

    -- 排名
    overall_rank INT,
    dept_rank INT,
    grade_rank INT,

    -- 对比分析
    vs_prev_score DECIMAL(10,2),
    vs_avg_score DECIMAL(10,2),

    -- 申诉统计
    appeal_count INT DEFAULT 0,
    appeal_approved INT DEFAULT 0,
    appeal_score_change DECIMAL(10,2) DEFAULT 0,

    -- 版本控制
    score_version INT DEFAULT 1,
    is_latest TINYINT DEFAULT 1,

    UNIQUE KEY uk_record_class_version (record_id, class_id, score_version)
);
```

### 3.4 申诉状态机

```
┌─────────┐   提交    ┌─────────┐
│  初始   │ ────────→ │ PENDING │
└─────────┘           └────┬────┘
                           │
              ┌────────────┼────────────┐
              │ 一级审核    │            │ 撤回/过期
              ↓            ↓            ↓
┌────────────────────┐  ┌──────────┐  ┌──────────┐
│ LEVEL1_REVIEWING   │  │WITHDRAWN │  │ EXPIRED  │
└─────────┬──────────┘  └──────────┘  └──────────┘
          │
  ┌───────┴────────┐
  │ 通过            │ 驳回
  ↓                ↓
┌────────────────────┐  ┌──────────┐
│ LEVEL2_REVIEWING   │  │ REJECTED │
└─────────┬──────────┘  └──────────┘
          │
  ┌───────┴────────┐
  │ 通过            │ 驳回
  ↓                ↓
┌──────────┐      ┌──────────┐
│ APPROVED │      │ REJECTED │
└────┬─────┘      └──────────┘
     │ 进入公示
     ↓
┌────────────┐
│PUBLICIZING │
└─────┬──────┘
      │ 公示结束
      ↓
┌────────────┐
│ EFFECTIVE  │ → 触发分数重算
└────────────┘
```

---

## 四、权限系统设计

### 4.1 三层权限模型

```
┌─────────────────────────────────────────────────────────┐
│                    功能权限层 (RBAC)                     │
│   User ←→ Role ←→ Permission                           │
│   实现: Spring Security + @PreAuthorize                │
├─────────────────────────────────────────────────────────┤
│                    数据权限层 (ABAC)                     │
│   基于属性的访问控制                                     │
│   实现: Casbin + 自定义规则引擎                         │
├─────────────────────────────────────────────────────────┤
│                 行级权限层 (Row-Level)                   │
│   数据范围: 全部/本部门/本年级/本班级/仅本人             │
│   实现: MyBatis 拦截器 + 动态 SQL                       │
└─────────────────────────────────────────────────────────┘
```

### 4.2 数据权限配置

```sql
CREATE TABLE data_permissions (
    id BIGINT PRIMARY KEY,
    principal_type ENUM('USER', 'ROLE') NOT NULL,
    principal_id BIGINT NOT NULL,
    resource_module VARCHAR(50) NOT NULL,
    data_scope ENUM('ALL', 'DEPARTMENT', 'DEPARTMENT_AND_CHILD',
                    'SELF_DEPARTMENT', 'GRADE', 'CLASS', 'SELF', 'CUSTOM'),
    custom_org_unit_ids JSON,
    custom_class_ids JSON,
    allowed_actions JSON DEFAULT '["read"]',
    conditions JSON,
    priority INT DEFAULT 0
);
```

---

## 五、事件驱动架构

### 5.1 核心领域事件

| 事件 | 触发时机 | 处理器 |
|------|---------|-------|
| HeadTeacherAssignedEvent | 分配班主任 | 通知原班主任、新班主任 |
| InspectionPublishedEvent | 检查发布 | 计算评级、发送通知、更新统计 |
| AppealEffectiveEvent | 申诉生效 | 重算得分、更新排名、重新评级 |
| ClassGraduatedEvent | 班级毕业 | 归档数据、结束任职 |

### 5.2 事件处理模式

```java
// 同步处理 - 在同一事务内
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
public void calculateRating(InspectionPublishedEvent event) {
    ratingService.calculateAndSaveRatings(event.getRecordId());
}

// 异步处理 - 事务提交后
@Async("taskExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void sendNotifications(InspectionPublishedEvent event) {
    notificationService.notifyInspectionPublished(event.getRecordId());
}
```

---

## 六、包结构规范

```
com.school.management
├── application/           # 应用层
│   ├── organization/
│   │   ├── command/      # 命令处理
│   │   ├── query/        # 查询处理
│   │   └── assembler/    # DTO转换
│   └── inspection/
│
├── domain/               # 领域层
│   ├── organization/
│   │   ├── model/
│   │   │   ├── aggregate/
│   │   │   ├── entity/
│   │   │   └── valueobject/
│   │   ├── repository/
│   │   ├── service/
│   │   └── event/
│   └── inspection/
│
├── infrastructure/       # 基础设施层
│   ├── persistence/
│   │   ├── repository/
│   │   ├── mapper/
│   │   ├── converter/
│   │   └── po/
│   ├── message/
│   └── external/
│
├── interfaces/           # 接口层
│   ├── rest/
│   ├── facade/
│   └── scheduler/
│
└── common/              # 通用
    ├── exception/
    ├── result/
    └── util/
```

---

## 七、与现有设计的对比

| 方面 | 现有设计 | 优化设计 | 改进点 |
|------|---------|---------|-------|
| 组织架构 | Grade 全校共享，Class.gradeLevel 冗余 | 删除冗余，引入 org_units 统一管理 | 清晰、规范 |
| 班主任分配 | Class.teacherId 一对一 | teacher_assignments 多对多+历史 | 可追溯 |
| 量化轮次 | JSON 存储 | 独立表正规化 | 可查询 |
| 加权配置 | 三层冗余存储 | 统一 weight_schemes | 简化 |
| 申诉流程 | 状态混乱 | 完整状态机 | 规范 |
| 权限系统 | 两套并存 | 统一 Casbin + ABAC | 标准化 |
| 版本控制 | 无 | 模板版本 + 快照 | 可追溯 |

---

## 八、实施建议

### 8.1 数据库迁移策略

1. **创建新表结构** - 按本文档设计创建新表
2. **数据迁移脚本** - 编写迁移脚本将旧数据转换到新结构
3. **双写过渡期** - 新旧表同时写入，验证数据一致性
4. **切换读取** - 逐步将读取切换到新表
5. **清理旧表** - 确认无问题后删除旧表

### 8.2 代码重构顺序

1. **领域层** - 先定义领域模型、值对象、聚合根
2. **基础设施层** - 实现仓储、持久化映射
3. **应用层** - 实现命令/查询处理器
4. **接口层** - 重构 Controller，保持 API 兼容

### 8.3 测试策略

- **单元测试** - 领域逻辑测试
- **集成测试** - 仓储、事件处理测试
- **端到端测试** - API 兼容性测试
- **性能测试** - 新旧方案对比

---

## 九、附录

### A. 完整 ER 图

见 `docs/design/er-diagram.png`

### B. API 设计规范

见 `docs/design/api-specification.md`

### C. Casbin 策略模板

见 `docs/design/casbin-policies.csv`

---

**文档结束**

*本设计方案基于业界最佳实践，参考了阿里巴巴、腾讯、字节跳动、Google、Microsoft 等大厂的架构经验。*
