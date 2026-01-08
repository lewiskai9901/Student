# DDD 分层规范

**版本**: v1.0
**生效日期**: 2026-01-06

---

## 一、架构概览

### 1.1 六边形架构

```
                    ┌─────────────────────────────────────┐
                    │           Interfaces Layer          │
                    │   (REST Controllers, Schedulers)    │
                    └──────────────────┬──────────────────┘
                                       │
                                       ▼
                    ┌─────────────────────────────────────┐
                    │         Application Layer           │
                    │   (Application Services, DTOs)      │
                    └──────────────────┬──────────────────┘
                                       │
                                       ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                            Domain Layer                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │ Aggregates  │  │  Entities   │  │Value Objects│  │   Events    │      │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘      │
│  ┌─────────────────────────────┐  ┌─────────────────────────────┐        │
│  │    Domain Services          │  │    Repository Interfaces    │        │
│  └─────────────────────────────┘  └─────────────────────────────┘        │
└───────────────────────────────────────────────────────────────────────────┘
                                       ▲
                                       │ (依赖倒置)
                                       │
                    ┌─────────────────────────────────────┐
                    │       Infrastructure Layer          │
                    │  (Repository Impl, Mappers, PO)     │
                    └─────────────────────────────────────┘
```

### 1.2 依赖规则

```
interfaces → application → domain ← infrastructure
                             ↑
                             │
                    (domain 不依赖任何外层)
```

---

## 二、领域层 (Domain Layer)

### 2.1 职责

- 包含核心业务逻辑
- 定义聚合根、实体、值对象
- 定义仓储接口（不是实现）
- 定义领域事件
- **不依赖任何框架**（纯Java）

### 2.2 聚合根 (Aggregate Root)

```java
package com.school.management.domain.organization.model;

/**
 * 聚合根：对外的唯一访问入口
 *
 * 规则：
 * 1. 继承 AggregateRoot<ID>
 * 2. 封装业务逻辑
 * 3. 通过工厂方法创建
 * 4. 发布领域事件
 * 5. 保护内部一致性
 */
public class SchoolClass extends AggregateRoot<Long> {

    // 私有字段
    private Long id;
    private String classCode;
    private ClassStatus status;
    private List<TeacherAssignment> teacherAssignments;  // 内部实体

    // 受保护的无参构造（ORM需要）
    protected SchoolClass() { }

    // 私有构造（通过Builder创建）
    private SchoolClass(Builder builder) { ... }

    // 工厂方法（唯一的创建入口）
    public static SchoolClass create(String classCode, String className, ...) {
        SchoolClass clazz = builder()...build();
        clazz.registerEvent(new ClassCreatedEvent(clazz));  // 发布事件
        return clazz;
    }

    // 业务方法（封装状态变更逻辑）
    public void activate(Long updatedBy) {
        if (this.status != ClassStatus.PREPARING) {
            throw new IllegalStateException("只有筹备中的班级才能激活");
        }
        this.status = ClassStatus.ACTIVE;
        registerEvent(new ClassStatusChangedEvent(this, PREPARING, ACTIVE));
    }

    // 管理内部实体
    public void assignHeadTeacher(Long teacherId, String teacherName, Long updatedBy) {
        // 先结束当前班主任
        getCurrentHeadTeacher().ifPresent(current ->
            endTeacherAssignment(current.getTeacherId(), HEAD_TEACHER));

        // 添加新班主任
        TeacherAssignment assignment = TeacherAssignment.create(teacherId, teacherName, HEAD_TEACHER);
        this.teacherAssignments.add(assignment);

        registerEvent(new TeacherAssignedEvent(this, assignment));
    }

    // 只读访问内部集合
    public List<TeacherAssignment> getTeacherAssignments() {
        return Collections.unmodifiableList(teacherAssignments);
    }
}
```

### 2.3 实体 (Entity)

```java
package com.school.management.domain.organization.model;

/**
 * 实体：有唯一标识，生命周期依附于聚合根
 *
 * 规则：
 * 1. 继承 Entity<ID>
 * 2. 只能通过聚合根访问
 * 3. 不能直接持久化
 */
public class TeacherAssignment extends Entity<Long> {

    private Long id;
    private Long teacherId;
    private String teacherName;
    private TeacherRole role;
    private LocalDate startDate;
    private LocalDate endDate;

    // 工厂方法
    public static TeacherAssignment create(Long teacherId, String teacherName,
                                           TeacherRole role, LocalDate startDate) {
        TeacherAssignment assignment = new TeacherAssignment();
        assignment.teacherId = teacherId;
        assignment.teacherName = teacherName;
        assignment.role = role;
        assignment.startDate = startDate;
        return assignment;
    }

    // 业务方法
    public TeacherAssignment endAssignment(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public boolean isCurrent() {
        return endDate == null || endDate.isAfter(LocalDate.now());
    }
}
```

### 2.4 值对象 (Value Object)

```java
package com.school.management.domain.organization.model;

/**
 * 值对象：无唯一标识，通过属性值判断相等
 *
 * 规则：
 * 1. 不可变（final字段，无setter）
 * 2. 通过构造器创建
 * 3. 重写equals/hashCode
 */
public enum ClassStatus {
    PREPARING("筹备中"),
    ACTIVE("在读"),
    GRADUATED("已毕业"),
    DISSOLVED("已解散");

    private final String description;

    ClassStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

// 复杂值对象示例
public final class DateRange {
    private final LocalDate startDate;
    private final LocalDate endDate;

    public DateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) &&
               (endDate == null || !date.isAfter(endDate));
    }

    // equals, hashCode...
}
```

### 2.5 仓储接口 (Repository Interface)

```java
package com.school.management.domain.organization.repository;

/**
 * 仓储接口：定义在领域层，实现在基础设施层
 *
 * 规则：
 * 1. 每个聚合根一个仓储
 * 2. 方法以聚合根为单位
 * 3. 不暴露SQL细节
 */
public interface SchoolClassRepository extends Repository<SchoolClass, Long> {

    Optional<SchoolClass> findById(Long id);

    Optional<SchoolClass> findByClassCode(String classCode);

    List<SchoolClass> findByOrgUnitId(Long orgUnitId);

    List<SchoolClass> findByStatus(ClassStatus status);

    SchoolClass save(SchoolClass schoolClass);

    void deleteById(Long id);

    boolean existsByClassCode(String classCode);
}
```

### 2.6 领域事件 (Domain Event)

```java
package com.school.management.domain.organization.event;

/**
 * 领域事件：表示领域中发生的重要事情
 *
 * 规则：
 * 1. 不可变
 * 2. 过去时命名
 * 3. 包含足够的上下文信息
 */
public class ClassCreatedEvent implements DomainEvent {

    private final Long classId;
    private final String classCode;
    private final String className;
    private final Long orgUnitId;
    private final LocalDateTime occurredAt;

    public ClassCreatedEvent(SchoolClass schoolClass) {
        this.classId = schoolClass.getId();
        this.classCode = schoolClass.getClassCode();
        this.className = schoolClass.getClassName();
        this.orgUnitId = schoolClass.getOrgUnitId();
        this.occurredAt = LocalDateTime.now();
    }

    // Getters...
}
```

### 2.7 领域服务 (Domain Service)

```java
package com.school.management.domain.organization.service;

/**
 * 领域服务：跨聚合根的业务逻辑
 *
 * 规则：
 * 1. 无状态
 * 2. 操作多个聚合根
 * 3. 不直接访问数据库
 */
public class OrgUnitDomainService {

    private final OrgUnitRepository orgUnitRepository;

    public OrgUnit createOrgUnit(String unitCode, String unitName,
                                  OrgUnitType unitType, Long parentId,
                                  Long createdBy) {
        // 验证编码唯一性
        if (orgUnitRepository.existsByUnitCode(unitCode)) {
            throw new BusinessRuleException("组织单元编码已存在: " + unitCode);
        }

        // 获取父节点信息
        String treePath = "";
        int treeLevel = 1;
        if (parentId != null) {
            OrgUnit parent = orgUnitRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("OrgUnit", parentId));
            treePath = parent.getTreePath();
            treeLevel = parent.getTreeLevel() + 1;
        }

        return OrgUnit.create(unitCode, unitName, unitType, parentId,
                              treePath, treeLevel, createdBy);
    }
}
```

---

## 三、应用层 (Application Layer)

### 3.1 职责

- 编排领域对象完成用例
- 管理事务边界
- 发布领域事件
- 不包含业务逻辑

### 3.2 命令服务

```java
package com.school.management.application.organization;

@Service
@Transactional
public class OrgUnitCommandService {

    private final OrgUnitRepository repository;
    private final OrgUnitDomainService domainService;
    private final DomainEventPublisher eventPublisher;

    public OrgUnitCommandService(OrgUnitRepository repository,
                                  DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.domainService = new OrgUnitDomainService(repository);
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建组织单元
     */
    public Long createOrgUnit(CreateOrgUnitCommand command) {
        // 1. 调用领域服务
        OrgUnit orgUnit = domainService.createOrgUnit(
            command.getUnitCode(),
            command.getUnitName(),
            command.getUnitType(),
            command.getParentId(),
            command.getCreatedBy()
        );

        // 2. 持久化
        orgUnit = repository.save(orgUnit);

        // 3. 发布事件
        publishEvents(orgUnit);

        return orgUnit.getId();
    }

    /**
     * 更新组织单元
     */
    public void updateOrgUnit(Long id, UpdateOrgUnitCommand command) {
        OrgUnit orgUnit = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("OrgUnit", id));

        orgUnit.update(
            command.getUnitName(),
            command.getLeaderId(),
            command.getDeputyLeaderIds(),
            command.getSortOrder(),
            command.getUpdatedBy()
        );

        repository.save(orgUnit);
        publishEvents(orgUnit);
    }

    private void publishEvents(AggregateRoot<?> aggregate) {
        aggregate.getDomainEvents().forEach(eventPublisher::publish);
        aggregate.clearDomainEvents();
    }
}
```

### 3.3 查询服务

```java
package com.school.management.application.organization;

@Service
@Transactional(readOnly = true)
public class OrgUnitQueryService {

    private final OrgUnitQueryMapper queryMapper;

    public OrgUnitDTO getOrgUnit(Long id) {
        OrgUnitDTO dto = queryMapper.selectById(id);
        if (dto == null) {
            throw new EntityNotFoundException("OrgUnit", id);
        }
        return dto;
    }

    public List<OrgUnitTreeDTO> getOrgUnitTree() {
        return queryMapper.selectTree();
    }

    public Page<OrgUnitDTO> searchOrgUnits(OrgUnitSearchQuery query) {
        return queryMapper.selectPage(query);
    }
}
```

### 3.4 命令对象

```java
package com.school.management.application.organization.command;

/**
 * 命令对象：封装写操作的输入
 */
@Data
@Builder
public class CreateOrgUnitCommand {
    private String unitCode;
    private String unitName;
    private OrgUnitType unitType;
    private Long parentId;
    private Long createdBy;
}
```

---

## 四、基础设施层 (Infrastructure Layer)

### 4.1 职责

- 实现仓储接口
- 数据库访问（MyBatis/JPA）
- 外部服务集成
- 事件存储

### 4.2 仓储实现

```java
package com.school.management.infrastructure.persistence.organization;

@Repository
public class SchoolClassRepositoryImpl implements SchoolClassRepository {

    private final SchoolClassMapper mapper;
    private final TeacherAssignmentMapper assignmentMapper;

    @Override
    public Optional<SchoolClass> findById(Long id) {
        SchoolClassPO po = mapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        List<TeacherAssignmentPO> assignmentPOs = assignmentMapper.selectByClassId(id);
        return Optional.of(toDomain(po, assignmentPOs));
    }

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        SchoolClassPO po = toPO(schoolClass);
        if (schoolClass.getId() == null) {
            mapper.insert(po);
            schoolClass.setId(po.getId());
        } else {
            mapper.updateById(po);
        }

        // 保存教师任职记录
        saveTeacherAssignments(schoolClass);

        return schoolClass;
    }

    // PO ↔ Domain 转换方法
    private SchoolClass toDomain(SchoolClassPO po, List<TeacherAssignmentPO> assignments) {
        return SchoolClass.builder()
            .id(po.getId())
            .classCode(po.getClassCode())
            .className(po.getClassName())
            .status(ClassStatus.valueOf(po.getStatus()))
            .teacherAssignments(assignments.stream()
                .map(this::toAssignmentDomain)
                .collect(Collectors.toList()))
            .build();
    }

    private SchoolClassPO toPO(SchoolClass domain) {
        SchoolClassPO po = new SchoolClassPO();
        po.setId(domain.getId());
        po.setClassCode(domain.getClassCode());
        po.setClassName(domain.getClassName());
        po.setStatus(domain.getStatus().name());
        return po;
    }
}
```

### 4.3 持久化对象 (PO)

```java
package com.school.management.infrastructure.persistence.organization;

/**
 * 持久化对象：数据库表映射
 *
 * 规则：
 * 1. 与表结构一一对应
 * 2. 使用MyBatis Plus注解
 * 3. 不包含业务逻辑
 */
@Data
@TableName("classes")
public class SchoolClassPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String classCode;
    private String className;
    private String shortName;
    private Long orgUnitId;
    private Long gradeId;
    private Integer enrollmentYear;
    private Integer gradeLevel;
    private Long majorDirectionId;
    private Integer schoolingYears;
    private Integer standardSize;
    private Integer currentSize;
    private String status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Integer deleted;
}
```

---

## 五、接口层 (Interfaces Layer)

### 5.1 职责

- 处理HTTP请求
- 参数校验
- 调用应用服务
- 响应格式化

### 5.2 Controller

```java
package com.school.management.interfaces.rest.organization;

@RestController
@RequestMapping("/api/v2/organization/classes")
@Tag(name = "班级管理")
public class SchoolClassController {

    private final SchoolClassCommandService commandService;
    private final SchoolClassQueryService queryService;

    @PostMapping
    @Operation(summary = "创建班级")
    public Result<Long> createClass(@Valid @RequestBody CreateClassRequest request) {
        CreateClassCommand command = CreateClassCommand.builder()
            .classCode(request.getClassCode())
            .className(request.getClassName())
            .orgUnitId(request.getOrgUnitId())
            .enrollmentYear(request.getEnrollmentYear())
            .majorDirectionId(request.getMajorDirectionId())
            .createdBy(SecurityUtils.getCurrentUserId())
            .build();

        Long id = commandService.createClass(command);
        return Result.success(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取班级详情")
    public Result<ClassDTO> getClass(@PathVariable Long id) {
        return Result.success(queryService.getClass(id));
    }

    @GetMapping
    @Operation(summary = "查询班级列表")
    public Result<Page<ClassDTO>> listClasses(ClassSearchRequest request) {
        return Result.success(queryService.searchClasses(request.toQuery()));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活班级")
    public Result<Void> activateClass(@PathVariable Long id) {
        commandService.activateClass(id, SecurityUtils.getCurrentUserId());
        return Result.success();
    }
}
```

---

## 六、层间通信规则

### 6.1 数据流向

```
Request DTO (interfaces)
    ↓ 转换
Command/Query (application)
    ↓ 调用
Domain Model (domain)
    ↓ 转换
PO (infrastructure)
    ↓ 存储
Database

Database
    ↓ 查询
PO (infrastructure)
    ↓ 转换（查询可直接返回DTO）
Response DTO (interfaces)
```

### 6.2 禁止事项

| 禁止 | 原因 |
|------|------|
| Controller直接调用Repository | 跳过应用层，失去事务控制 |
| Domain依赖Spring注解 | 领域层应纯净 |
| Application直接操作数据库 | 应通过Repository |
| Domain返回PO | 泄露持久化细节 |
| Controller包含业务逻辑 | 违反职责分离 |

---

## 七、模块分类指南

### 7.1 何时使用完整DDD

```
✅ 使用完整DDD：
- 有复杂状态机（3个以上状态）
- 有多个业务规则
- 需要领域事件
- 聚合根包含多个实体

示例：量化检查、任务工作流、班级管理
```

### 7.2 何时使用简化版

```
⚠️ 使用简化版（只有聚合根+仓储）：
- 有一定业务逻辑
- 但状态简单
- 无复杂关联

示例：学生管理、权限管理
```

### 7.3 何时保持传统分层

```
❌ 不使用DDD：
- 纯CRUD操作
- 无业务规则
- 配置类数据

示例：公告管理、系统配置、文件上传
```

---

**文档结束**
