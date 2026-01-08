# 学生管理系统 - 架构迁移与优化计划

**版本**: v1.0
**制定日期**: 2026-01-06
**预计周期**: 6-8 周
**目标**: 完成V1→V2迁移，统一DDD架构，消除技术债务

---

## 一、总体目标

### 1.1 核心目标

| 目标 | 当前状态 | 目标状态 | 优先级 |
|------|---------|---------|--------|
| API统一 | V1/V2并存 | 仅V2 | P0 |
| 数据库Schema | 9个碎片文件 | 3个规范文件 | P0 |
| Event Store | 仅接口 | 完整实现 | P1 |
| CQRS分离 | 混合 | 读写分离 | P1 |
| 代码规范 | 不统一 | 统一标准 | P2 |
| 测试覆盖 | <10% | >60% | P2 |

### 1.2 架构迁移原则

```
1. 渐进式迁移 - 不停服，逐模块切换
2. 向后兼容 - V1 API保留3个月过渡期
3. 先核心后辅助 - 优先迁移量化检查、组织架构
4. 混合架构 - 复杂业务用DDD，简单CRUD保持简单
5. 测试先行 - 每个迁移模块必须有测试覆盖
```

---

## 二、模块分类与迁移策略

### 2.1 模块分类

```
┌─────────────────────────────────────────────────────────────┐
│                      模块分类矩阵                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  业务复杂度 ↑                                                │
│      │                                                       │
│  高  │  ┌──────────────┐     ┌──────────────┐              │
│      │  │ 量化检查系统  │     │  任务工作流   │              │
│      │  │ (完整DDD)    │     │  (完整DDD)   │              │
│      │  └──────────────┘     └──────────────┘              │
│      │                                                       │
│  中  │  ┌──────────────┐     ┌──────────────┐              │
│      │  │  组织架构     │     │   学生管理    │              │
│      │  │ (DDD聚合根)  │     │  (DDD聚合根) │              │
│      │  └──────────────┘     └──────────────┘              │
│      │                                                       │
│  低  │  ┌──────────────┐     ┌──────────────┐              │
│      │  │  公告管理     │     │   系统配置    │              │
│      │  │ (简单CRUD)   │     │  (简单CRUD)  │              │
│      │  └──────────────┘     └──────────────┘              │
│      │                                                       │
│      └──────────────────────────────────────────→ 变更频率  │
│                低                      高                    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 迁移优先级

| 优先级 | 模块 | 迁移策略 | 工作量 |
|--------|------|---------|--------|
| P0 | 组织架构 (organization) | V2已完成，删除V1 | 1周 |
| P0 | 权限管理 (access) | V2已完成，删除V1 | 0.5周 |
| P1 | 量化检查 (inspection) | V2部分完成，补全迁移 | 2周 |
| P1 | 任务工作流 (task) | V2进行中，继续完善 | 1周 |
| P2 | 学生管理 (student) | V2已完成，删除V1 | 0.5周 |
| P2 | 宿舍管理 (asset) | V2已完成，删除V1 | 0.5周 |
| P3 | 公告管理 | 保持V1简单CRUD | 不迁移 |
| P3 | 系统配置 | 保持V1简单CRUD | 不迁移 |
| P3 | 文件上传 | 保持V1简单CRUD | 不迁移 |

---

## 三、分阶段执行计划

### 阶段一：准备与清理（第1周）

#### 1.1 建立规范文档

| 任务 | 产出物 | 负责 | 工时 |
|------|--------|------|------|
| 编写API命名规范 | `docs/standards/api-naming.md` | - | 2h |
| 编写代码风格规范 | `docs/standards/coding-style.md` | - | 2h |
| 编写DDD分层规范 | `docs/standards/ddd-layers.md` | - | 2h |

#### 1.2 合并数据库Schema

**当前状态**:
```
database/schema/
├── evaluation_schema.sql
├── evaluation_schema_update.sql
├── evaluation_extend_quantification.sql
├── rating_enhancement_schema.sql
├── check_plan_rating_schema.sql
├── task_management_schema.sql
├── task_approval_config.sql
├── alter_task_tables.sql
└── INDEX_OPTIMIZATION_RECOMMENDATIONS.sql
```

**目标状态**:
```
database/schema/
├── 01_core_schema.sql          # 用户、组织、权限、基础数据
├── 02_business_schema.sql      # 量化检查、评级、任务
├── 03_audit_schema.sql         # 日志、快照、事件存储
└── 04_indexes.sql              # 所有索引定义
```

| 任务 | 工时 |
|------|------|
| 分析现有9个Schema文件 | 2h |
| 设计合并方案 | 2h |
| 执行合并，生成4个文件 | 4h |
| 验证迁移脚本 | 2h |

#### 1.3 标记V1代码为Deprecated

```java
// 所有V1 Controller添加注解
@Deprecated(since = "2.0", forRemoval = true)
@RestController
@RequestMapping("/api/classes")  // V1路径
public class ClassController {
    // 在Swagger中提示迁移
    @Operation(summary = "获取班级列表",
               description = "⚠️ 已废弃，请使用 /api/v2/classes")
    public Result<List<ClassResponse>> getClasses() { ... }
}
```

| 任务 | 工时 |
|------|------|
| 标记50+个V1 Controller | 4h |
| 更新Swagger文档说明 | 2h |

#### 1.4 阶段一检查清单

- [ ] 规范文档已创建并评审
- [ ] 数据库Schema已合并为4个文件
- [ ] 合并后的Schema已在测试环境验证
- [ ] 所有V1 Controller已标记@Deprecated
- [ ] Swagger文档已更新迁移提示

---

### 阶段二：基础设施完善（第2周）

#### 2.1 实现Event Store持久化

**数据库表设计**:
```sql
-- 添加到 03_audit_schema.sql
CREATE TABLE domain_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL COMMENT '事件唯一ID',
    aggregate_type VARCHAR(100) NOT NULL COMMENT '聚合根类型',
    aggregate_id BIGINT NOT NULL COMMENT '聚合根ID',
    event_type VARCHAR(100) NOT NULL COMMENT '事件类型',
    event_data JSON NOT NULL COMMENT '事件数据',
    metadata JSON COMMENT '元数据(用户ID、时间等)',
    version INT NOT NULL COMMENT '聚合版本号',
    occurred_at DATETIME NOT NULL COMMENT '发生时间',
    published_at DATETIME COMMENT '发布时间',
    INDEX idx_aggregate (aggregate_type, aggregate_id),
    INDEX idx_event_type (event_type),
    INDEX idx_occurred_at (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领域事件存储';
```

**实现代码**:
```
infrastructure/event/
├── JdbcDomainEventStore.java      # Event Store实现
├── DomainEventMapper.java         # MyBatis Mapper
└── DomainEventPO.java             # 持久化对象
```

| 任务 | 工时 |
|------|------|
| 设计domain_events表 | 1h |
| 实现JdbcDomainEventStore | 4h |
| 实现DomainEventMapper | 2h |
| 编写单元测试 | 2h |
| 集成测试 | 1h |

#### 2.2 CQRS查询服务分离

**目标结构**:
```
application/organization/
├── command/
│   ├── CreateOrgUnitCommand.java
│   ├── UpdateOrgUnitCommand.java
│   └── OrgUnitCommandService.java    # 新增：命令服务
├── query/
│   ├── OrgUnitDTO.java
│   ├── OrgUnitTreeDTO.java
│   └── OrgUnitQueryService.java      # 新增：查询服务
└── OrgUnitApplicationService.java    # 保留：向后兼容，内部委托
```

**示例代码**:
```java
// OrgUnitQueryService.java - 纯查询，可独立优化
@Service
@Transactional(readOnly = true)
public class OrgUnitQueryService {

    private final OrgUnitQueryMapper queryMapper; // 专用查询Mapper

    public OrgUnitDTO getOrgUnit(Long id) { ... }

    public List<OrgUnitTreeDTO> getOrgUnitTree() { ... }

    public Page<OrgUnitDTO> searchOrgUnits(OrgUnitSearchQuery query) { ... }
}

// OrgUnitCommandService.java - 写操作，使用聚合根
@Service
@Transactional
public class OrgUnitCommandService {

    private final OrgUnitRepository repository;
    private final DomainEventPublisher eventPublisher;

    public Long createOrgUnit(CreateOrgUnitCommand command) { ... }

    public void updateOrgUnit(Long id, UpdateOrgUnitCommand command) { ... }
}
```

| 任务 | 工时 |
|------|------|
| 分离organization模块 | 4h |
| 分离access模块 | 3h |
| 分离inspection模块 | 4h |
| 分离student模块 | 2h |
| 分离task模块 | 3h |

#### 2.3 阶段二检查清单

- [ ] domain_events表已创建
- [ ] JdbcDomainEventStore已实现并测试
- [ ] 5个核心模块已完成CQRS分离
- [ ] 所有查询服务标记@Transactional(readOnly = true)
- [ ] 事件发布已切换到持久化模式

---

### 阶段三：核心模块迁移（第3-4周）

#### 3.1 组织架构模块 - 删除V1

**V2已完成，需要删除的V1文件**:
```
待删除：
├── controller/
│   ├── ClassController.java
│   ├── GradeController.java
│   ├── DepartmentController.java
│   ├── MajorController.java
│   └── MajorDirectionController.java
├── service/
│   ├── ClassService.java
│   ├── ClassServiceImpl.java
│   ├── GradeService.java
│   ├── GradeServiceImpl.java
│   ├── DepartmentService.java
│   ├── DepartmentServiceImpl.java
│   ├── MajorService.java
│   ├── MajorServiceImpl.java
│   └── ...
└── dto/
    ├── ClassCreateRequest.java
    ├── ClassResponse.java
    └── ...
```

| 任务 | 工时 |
|------|------|
| 确认前端已切换到V2 API | 2h |
| 删除V1 Controller (5个) | 1h |
| 删除V1 Service (10个) | 1h |
| 删除V1 DTO (15个) | 1h |
| 运行全量测试 | 1h |

#### 3.2 量化检查模块 - 补全V2

**需要迁移的V1 Controller**:
```
controller/
├── CheckTemplateController.java      → interfaces/rest/inspection/
├── DailyCheckController.java         → interfaces/rest/inspection/
├── DeductionItemController.java      → interfaces/rest/inspection/
├── CheckItemAppealController.java    → 已有V2
├── CheckPlanController.java          → interfaces/rest/inspection/
├── CheckPlanStatisticsController.java → interfaces/rest/inspection/
└── ...
```

**V2补全任务**:
```
1. 迁移CheckTemplate相关API
   - GET/POST/PUT/DELETE /v2/inspection/templates
   - GET /v2/inspection/templates/{id}/items

2. 迁移DailyCheck相关API
   - GET/POST /v2/inspection/records
   - POST /v2/inspection/records/{id}/submit
   - POST /v2/inspection/records/{id}/approve
   - POST /v2/inspection/records/{id}/publish

3. 迁移CheckPlan相关API
   - GET/POST/PUT /v2/inspection/plans
   - GET /v2/inspection/plans/{id}/statistics

4. 迁移统计导出API
   - GET /v2/inspection/statistics/summary
   - GET /v2/inspection/export/excel
```

| 任务 | 工时 |
|------|------|
| 迁移CheckTemplate API | 6h |
| 迁移DailyCheck API | 8h |
| 迁移CheckPlan API | 6h |
| 迁移统计导出API | 4h |
| 编写集成测试 | 4h |

#### 3.3 任务工作流模块 - 完善V2

**当前V2状态**:
- ✅ 任务创建、查询
- ✅ 任务提交、审批
- ⚠️ 缺少工作流模板管理
- ⚠️ 缺少审批配置管理

**补全任务**:
```
interfaces/rest/task/
├── TaskController.java           # 已有
├── WorkflowTemplateController.java  # 新增
└── ApprovalConfigController.java    # 新增
```

| 任务 | 工时 |
|------|------|
| 实现WorkflowTemplate V2 API | 4h |
| 实现ApprovalConfig V2 API | 4h |
| 完善任务状态机 | 3h |
| 编写测试 | 2h |

#### 3.4 阶段三检查清单

- [ ] 组织架构V1代码已删除
- [ ] 量化检查V2 API已补全
- [ ] 任务工作流V2 API已完善
- [ ] 前端已全部切换到V2 API
- [ ] 所有迁移模块有测试覆盖

---

### 阶段四：辅助模块处理（第5周）

#### 4.1 学生管理 - 删除V1

| 任务 | 工时 |
|------|------|
| 确认V2 API完整性 | 1h |
| 删除V1 StudentController | 0.5h |
| 删除V1 StudentService | 0.5h |
| 删除相关DTO | 0.5h |

#### 4.2 宿舍管理 - 删除V1

| 任务 | 工时 |
|------|------|
| 确认V2 API完整性 | 1h |
| 删除V1 DormitoryController | 0.5h |
| 删除V1相关Service | 1h |

#### 4.3 保留V1的简单模块

以下模块保持V1，**不迁移**（复杂度不值得）:
```
保留V1：
├── AnnouncementController.java    # 公告管理
├── SystemConfigController.java    # 系统配置
├── FileController.java            # 文件管理
├── FileUploadController.java      # 文件上传
├── OperationLogController.java    # 操作日志
└── WechatController.java          # 微信相关
```

**但需要**:
1. 移除@Deprecated注解（这些不再废弃）
2. 更新文档说明这些是有意保留的V1模块

| 任务 | 工时 |
|------|------|
| 更新保留模块的注解和文档 | 2h |

#### 4.4 阶段四检查清单

- [ ] 学生管理V1已删除
- [ ] 宿舍管理V1已删除
- [ ] 保留V1模块已明确标注
- [ ] 架构文档已更新

---

### 阶段五：代码质量优化（第6周）

#### 5.1 引入MapStruct

**目标**: 替换手写的toDTO()方法

```java
// 当前：手写映射（容易出错）
private OrgUnitDTO toDTO(OrgUnit orgUnit) {
    OrgUnitDTO dto = new OrgUnitDTO();
    dto.setId(orgUnit.getId());
    dto.setUnitCode(orgUnit.getUnitCode());
    // ... 20行重复代码
    return dto;
}

// 目标：MapStruct自动生成
@Mapper(componentModel = "spring")
public interface OrgUnitMapper {
    OrgUnitDTO toDTO(OrgUnit orgUnit);
    List<OrgUnitDTO> toDTOList(List<OrgUnit> orgUnits);
}
```

| 任务 | 工时 |
|------|------|
| 添加MapStruct依赖 | 0.5h |
| 创建organization模块Mapper | 2h |
| 创建inspection模块Mapper | 2h |
| 创建其他模块Mapper | 2h |
| 删除手写映射代码 | 2h |

#### 5.2 统一验证逻辑

**目标**: 提取通用验证器

```java
// 当前：每个Service重复验证
if (name == null || name.isBlank()) {
    throw new IllegalArgumentException("名称不能为空");
}
if (name.length() > 100) {
    throw new IllegalArgumentException("名称长度不能超过100");
}

// 目标：统一验证器
@Component
public class CommonValidator {
    public void requireNotBlank(String value, String fieldName) { ... }
    public void requireMaxLength(String value, int max, String fieldName) { ... }
    public void requirePositive(Number value, String fieldName) { ... }
}
```

| 任务 | 工时 |
|------|------|
| 创建CommonValidator | 2h |
| 重构现有验证代码 | 4h |

#### 5.3 统一异常处理

```java
// 创建业务异常体系
domain/shared/exception/
├── DomainException.java           # 领域异常基类
├── EntityNotFoundException.java   # 实体不存在
├── BusinessRuleException.java     # 业务规则违反
└── ValidationException.java       # 验证失败

// 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public Result<?> handleNotFound(EntityNotFoundException e) {
        return Result.fail(404, e.getMessage());
    }
    // ...
}
```

| 任务 | 工时 |
|------|------|
| 设计异常体系 | 1h |
| 实现异常类 | 2h |
| 重构现有异常处理 | 3h |

#### 5.4 阶段五检查清单

- [ ] MapStruct已集成
- [ ] 所有DTO映射使用MapStruct
- [ ] CommonValidator已实现
- [ ] 异常处理已统一
- [ ] 代码重复率降低到<10%

---

### 阶段六：测试与文档（第7-8周）

#### 6.1 测试覆盖

**测试目录结构**:
```
backend/src/test/java/com/school/management/
├── domain/                    # 领域层单元测试
│   ├── organization/
│   │   ├── SchoolClassTest.java
│   │   ├── OrgUnitTest.java
│   │   └── GradeTest.java
│   └── inspection/
│       ├── InspectionRecordTest.java
│       └── AppealTest.java
├── application/               # 应用层测试
│   ├── OrgUnitCommandServiceTest.java
│   └── InspectionApplicationServiceTest.java
├── infrastructure/            # 基础设施测试
│   └── JdbcDomainEventStoreTest.java
└── integration/               # 集成测试
    ├── OrganizationApiTest.java
    └── InspectionApiTest.java
```

| 任务 | 工时 |
|------|------|
| 领域层单元测试 (10个) | 8h |
| 应用层测试 (5个) | 4h |
| 集成测试 (5个) | 6h |

#### 6.2 架构文档更新

```
docs/
├── architecture/
│   ├── overview.md              # 架构总览
│   ├── ddd-structure.md         # DDD分层说明
│   ├── module-classification.md # 模块分类说明
│   └── api-versioning.md        # API版本策略
├── standards/
│   ├── api-naming.md            # API命名规范
│   ├── coding-style.md          # 代码风格
│   └── ddd-layers.md            # DDD分层规范
└── migration/
    ├── v1-to-v2-guide.md        # V1到V2迁移指南
    └── changelog.md             # 变更记录
```

| 任务 | 工时 |
|------|------|
| 编写架构文档 | 4h |
| 编写迁移指南 | 2h |
| 更新README | 1h |

#### 6.3 阶段六检查清单

- [ ] 核心领域测试覆盖>80%
- [ ] 应用层测试覆盖>60%
- [ ] 集成测试覆盖主要API
- [ ] 架构文档完整
- [ ] README已更新

---

## 四、风险控制

### 4.1 风险识别

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| 前端未及时切换V2 | 高 | 中 | 提前2周通知前端，提供API对照表 |
| 数据迁移出错 | 高 | 低 | Schema合并前备份，测试环境验证 |
| 删除V1影响线上 | 高 | 低 | 灰度发布，保留回滚能力 |
| 工期延误 | 中 | 中 | 每周检查进度，及时调整优先级 |

### 4.2 回滚策略

```
1. 代码回滚
   - 所有删除操作前创建Git Tag
   - 保留V1分支至少3个月

2. 数据库回滚
   - Schema合并前完整备份
   - 保留原始Schema文件副本

3. API回滚
   - V1 API保留30天废弃期
   - 监控V1 API调用量，确认归零后再删除
```

---

## 五、里程碑与交付物

| 里程碑 | 时间 | 交付物 |
|--------|------|--------|
| M1: 准备完成 | 第1周末 | 规范文档、合并后的Schema、V1标记完成 |
| M2: 基础设施完成 | 第2周末 | Event Store、CQRS分离完成 |
| M3: 核心迁移完成 | 第4周末 | 组织架构、量化检查V2完成 |
| M4: 全部迁移完成 | 第5周末 | 所有模块迁移/处理完成 |
| M5: 优化完成 | 第6周末 | MapStruct、验证器、异常处理统一 |
| M6: 项目交付 | 第8周末 | 测试覆盖、文档完整 |

---

## 六、资源需求

### 6.1 人力

| 角色 | 人数 | 参与阶段 |
|------|------|---------|
| 后端开发 | 1-2 | 全程 |
| 前端开发 | 1 | 阶段3-4（API切换） |
| 测试 | 1 | 阶段6 |

### 6.2 工时估算

| 阶段 | 工时 | 说明 |
|------|------|------|
| 阶段一 | 20h | 准备清理 |
| 阶段二 | 24h | 基础设施 |
| 阶段三 | 48h | 核心迁移 |
| 阶段四 | 8h | 辅助模块 |
| 阶段五 | 18h | 代码优化 |
| 阶段六 | 25h | 测试文档 |
| **总计** | **143h** | 约4周全职 |

---

## 七、附录

### A. V1→V2 API对照表

| V1 API | V2 API | 状态 |
|--------|--------|------|
| GET /api/classes | GET /api/v2/classes | ✅ 已迁移 |
| POST /api/classes | POST /api/v2/classes | ✅ 已迁移 |
| GET /api/grades | GET /api/v2/grades | ✅ 已迁移 |
| GET /api/daily-checks | GET /api/v2/inspection/records | ⏳ 待迁移 |
| ... | ... | ... |

### B. 决策记录

#### ADR-001: 采用混合架构

- **背景**: 系统包含不同复杂度的模块
- **决策**: 复杂业务用DDD，简单CRUD保持传统分层
- **后果**: 需要明确模块分类标准

#### ADR-002: 保留部分V1模块

- **背景**: 公告、配置等模块业务简单
- **决策**: 这些模块不迁移到DDD
- **后果**: 代码库存在两种风格，需要文档说明

---

**文档结束**

*此计划将根据实际执行情况进行调整*
