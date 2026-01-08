# 代码风格规范

**版本**: v1.0
**生效日期**: 2026-01-06

---

## 一、Java代码规范

### 1.1 命名规范

#### 类命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 聚合根/实体 | 名词，PascalCase | `SchoolClass`, `InspectionRecord` |
| 值对象 | 名词，PascalCase | `ClassStatus`, `DeductionMode` |
| 仓储接口 | `{Entity}Repository` | `SchoolClassRepository` |
| 仓储实现 | `{Entity}RepositoryImpl` | `SchoolClassRepositoryImpl` |
| 应用服务 | `{Domain}ApplicationService` | `OrgUnitApplicationService` |
| 命令服务 | `{Domain}CommandService` | `OrgUnitCommandService` |
| 查询服务 | `{Domain}QueryService` | `OrgUnitQueryService` |
| 领域服务 | `{Domain}DomainService` | `OrgUnitDomainService` |
| Controller | `{Resource}Controller` | `SchoolClassController` |
| DTO | `{Purpose}{Request/Response/DTO}` | `CreateClassRequest`, `ClassDTO` |
| 领域事件 | `{Entity}{Action}Event` | `ClassCreatedEvent`, `ClassStatusChangedEvent` |
| 异常 | `{Type}Exception` | `EntityNotFoundException` |

#### 方法命名

| 操作类型 | 前缀 | 示例 |
|---------|------|------|
| 查询单个 | `get`, `find` | `getById()`, `findByCode()` |
| 查询列表 | `list`, `find` | `listByStatus()`, `findAll()` |
| 查询存在 | `exists`, `has` | `existsByCode()`, `hasPermission()` |
| 统计 | `count` | `countByStatus()` |
| 创建 | `create` | `create()`, `createOrgUnit()` |
| 更新 | `update` | `update()`, `updateInfo()` |
| 删除 | `delete`, `remove` | `delete()`, `removeItem()` |
| 状态变更 | 动词 | `activate()`, `publish()`, `approve()` |

#### 变量命名

```java
// 正确：camelCase，有意义的名称
Long orgUnitId;
String className;
List<SchoolClass> activeClasses;
Map<Long, String> classNameMap;

// 错误
Long id1;           // 无意义
String str;         // 过于简单
List<SchoolClass> list;  // 不够描述性
```

### 1.2 包结构

```
com.school.management
├── domain/                     # 领域层
│   ├── {context}/
│   │   ├── model/             # 聚合根、实体、值对象
│   │   ├── repository/        # 仓储接口
│   │   ├── service/           # 领域服务
│   │   └── event/             # 领域事件
│   └── shared/                # 共享内核
├── application/               # 应用层
│   └── {context}/
│       ├── command/           # 命令对象
│       ├── query/             # 查询DTO
│       └── *Service.java      # 应用服务
├── infrastructure/            # 基础设施层
│   ├── persistence/           # 持久化
│   │   └── {context}/
│   │       ├── mapper/        # MyBatis Mapper
│   │       ├── po/            # 持久化对象
│   │       └── *RepositoryImpl.java
│   ├── event/                 # 事件基础设施
│   └── external/              # 外部服务
├── interfaces/                # 接口层
│   └── rest/
│       └── {context}/
│           ├── dto/           # REST DTO
│           └── *Controller.java
└── common/                    # 通用
    ├── exception/             # 异常
    ├── result/                # 响应封装
    └── util/                  # 工具类
```

### 1.3 代码格式

#### 类结构顺序

```java
public class SchoolClass extends AggregateRoot<Long> {

    // 1. 静态常量
    private static final int MAX_NAME_LENGTH = 100;

    // 2. 实例字段
    private Long id;
    private String classCode;
    private String className;

    // 3. 构造函数
    protected SchoolClass() { }

    private SchoolClass(Builder builder) { }

    // 4. 静态工厂方法
    public static SchoolClass create(...) { }

    // 5. 业务方法
    public void activate() { }
    public void graduate() { }

    // 6. 查询方法
    public boolean isFull() { }
    public int getAvailableSlots() { }

    // 7. Getter/Setter
    public Long getId() { }

    // 8. Builder（如有）
    public static class Builder { }
}
```

#### 方法长度

- 单个方法不超过 **30行**
- 超过则提取私有方法

#### 参数数量

- 方法参数不超过 **5个**
- 超过则使用参数对象

```java
// 错误：参数过多
public SchoolClass create(String code, String name, Long orgUnitId,
    Integer year, Long majorId, Integer size, Long createdBy) { }

// 正确：使用命令对象
public SchoolClass create(CreateClassCommand command) { }
```

### 1.4 注释规范

#### 类注释

```java
/**
 * 班级聚合根
 *
 * <p>管理班级的核心业务逻辑，包括教师任职、状态变更等。
 *
 * <p>状态流转：PREPARING -> ACTIVE -> GRADUATED/DISSOLVED
 *
 * @author system
 * @since 2.0
 */
public class SchoolClass extends AggregateRoot<Long> { }
```

#### 方法注释

```java
/**
 * 激活班级（从筹备状态变为正式状态）
 *
 * @param updatedBy 操作人ID
 * @throws IllegalStateException 如果班级不是筹备状态
 */
public void activate(Long updatedBy) { }
```

#### 行内注释

```java
// 仅在逻辑不明显时添加
// 优先结束当前班主任的任职，再分配新班主任
getCurrentHeadTeacher().ifPresent(current ->
    endTeacherAssignment(current.getTeacherId(), HEAD_TEACHER));
```

---

## 二、Service层规范

### 2.1 应用服务

```java
@Service
@Transactional
public class OrgUnitCommandService {

    private final OrgUnitRepository repository;
    private final OrgUnitDomainService domainService;
    private final DomainEventPublisher eventPublisher;

    // 构造器注入（不使用@Autowired）
    public OrgUnitCommandService(OrgUnitRepository repository, ...) {
        this.repository = repository;
        // ...
    }

    /**
     * 创建组织单元
     */
    public Long createOrgUnit(CreateOrgUnitCommand command) {
        // 1. 调用领域服务/聚合根
        OrgUnit orgUnit = domainService.createOrgUnit(...);

        // 2. 持久化
        orgUnit = repository.save(orgUnit);

        // 3. 发布事件
        publishEvents(orgUnit);

        // 4. 返回结果
        return orgUnit.getId();
    }

    private void publishEvents(AggregateRoot<?> aggregate) {
        aggregate.getDomainEvents().forEach(eventPublisher::publish);
        aggregate.clearDomainEvents();
    }
}
```

### 2.2 查询服务

```java
@Service
@Transactional(readOnly = true)  // 只读事务
public class OrgUnitQueryService {

    private final OrgUnitQueryMapper queryMapper;  // 专用查询Mapper

    public OrgUnitDTO getOrgUnit(Long id) {
        return queryMapper.selectById(id);  // 直接返回DTO
    }

    public Page<OrgUnitDTO> searchOrgUnits(OrgUnitSearchQuery query) {
        return queryMapper.selectPage(query);
    }
}
```

---

## 三、Controller层规范

### 3.1 基本结构

```java
@RestController
@RequestMapping("/api/v2/organization/classes")
@Tag(name = "班级管理", description = "V2 班级管理接口")
public class SchoolClassController {

    private final SchoolClassCommandService commandService;
    private final SchoolClassQueryService queryService;

    @PostMapping
    @Operation(summary = "创建班级")
    public Result<Long> createClass(@Valid @RequestBody CreateClassRequest request) {
        Long id = commandService.createClass(toCommand(request));
        return Result.success(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取班级详情")
    public Result<ClassDTO> getClass(@PathVariable Long id) {
        return Result.success(queryService.getClass(id));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活班级")
    public Result<Void> activateClass(@PathVariable Long id) {
        commandService.activateClass(id);
        return Result.success();
    }
}
```

### 3.2 参数校验

```java
public class CreateClassRequest {

    @NotBlank(message = "班级编码不能为空")
    @Size(max = 50, message = "班级编码不能超过50个字符")
    private String classCode;

    @NotBlank(message = "班级名称不能为空")
    @Size(max = 100, message = "班级名称不能超过100个字符")
    private String className;

    @NotNull(message = "组织单元ID不能为空")
    private Long orgUnitId;

    @NotNull(message = "入学年份不能为空")
    @Min(value = 2000, message = "入学年份不能早于2000年")
    @Max(value = 2100, message = "入学年份不能晚于2100年")
    private Integer enrollmentYear;
}
```

---

## 四、异常处理规范

### 4.1 异常类型

```java
// 领域异常基类
public abstract class DomainException extends RuntimeException {
    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }
}

// 实体不存在
public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String entityType, Object id) {
        super("NOT_FOUND", entityType + " not found: " + id);
    }
}

// 业务规则违反
public class BusinessRuleException extends DomainException {
    public BusinessRuleException(String message) {
        super("BUSINESS_RULE", message);
    }
}
```

### 4.2 异常抛出

```java
// 在领域层抛出领域异常
public void activate() {
    if (this.status != ClassStatus.PREPARING) {
        throw new BusinessRuleException("只有筹备中的班级才能激活");
    }
    // ...
}

// 在应用层处理
public void activateClass(Long id) {
    SchoolClass clazz = repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("SchoolClass", id));
    clazz.activate();
    repository.save(clazz);
}
```

---

## 五、日志规范

### 5.1 日志级别

| 级别 | 使用场景 |
|------|---------|
| ERROR | 系统错误，需要立即处理 |
| WARN | 潜在问题，需要关注 |
| INFO | 重要业务操作 |
| DEBUG | 开发调试信息 |

### 5.2 日志格式

```java
@Slf4j
public class OrgUnitCommandService {

    public Long createOrgUnit(CreateOrgUnitCommand command) {
        log.info("Creating org unit: code={}, name={}",
            command.getUnitCode(), command.getUnitName());

        try {
            // ...
            log.info("Org unit created: id={}", orgUnit.getId());
            return orgUnit.getId();
        } catch (Exception e) {
            log.error("Failed to create org unit: code={}",
                command.getUnitCode(), e);
            throw e;
        }
    }
}
```

---

## 六、测试规范

### 6.1 测试命名

```java
// 格式：should{Expected}When{Condition}
@Test
void shouldActivateClassWhenStatusIsPreparing() { }

@Test
void shouldThrowExceptionWhenActivatingActiveClass() { }

@Test
void shouldEndAllTeacherAssignmentsWhenClassGraduates() { }
```

### 6.2 测试结构

```java
@Test
void shouldActivateClassWhenStatusIsPreparing() {
    // Given - 准备
    SchoolClass clazz = SchoolClass.create("CS01", "计算机1班", ...);

    // When - 执行
    clazz.activate(1L);

    // Then - 验证
    assertThat(clazz.getStatus()).isEqualTo(ClassStatus.ACTIVE);
    assertThat(clazz.getDomainEvents()).hasSize(1);
    assertThat(clazz.getDomainEvents().get(0))
        .isInstanceOf(ClassStatusChangedEvent.class);
}
```

---

**文档结束**
