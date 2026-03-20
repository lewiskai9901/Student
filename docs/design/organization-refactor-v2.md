# 组织管理模块重构方案 V2

> 对标钉钉/飞书/Workday，全面升级组织管理能力
> 涵盖后端（Spring Boot DDD）+ 前端（Vue 3）+ 数据库

---

## 一、现状问题总结

| # | 问题 | 当前状态 | 业界标准 |
|---|------|---------|---------|
| 1 | 无岗位概念 | 组织直接挂 leaderId | 组织 → 岗位 → 人 三层模型 |
| 2 | 无变更历史 | 只有 created_at/updated_at | 完整字段级变更日志 |
| 3 | 组织生命周期简陋 | OrgUnit 仅 enabled 布尔值 | 草稿→生效→冻结→合并/拆分→撤销 |
| 4 | 无乐观锁 | 并发覆盖静默丢失 | @Version 乐观锁 |
| 5 | 无编制管理 | OrgUnit 无人数概念 | 编制数/在编数/超编预警 |
| 6 | 无生效日期 | 变更立即生效 | effective_date 预排变更 |
| 7 | 单一汇报线 | 只有 parentId 树形 | 实线/虚线双线汇报，矩阵组织 |
| 8 | 前端缺功能 | 侧边栏树 + 基础CRUD | 组织架构图、拖拽、批量导入导出 |
| 9 | 无属性扩展 | 字段固定 | attributes JSON 可扩展 |
| 10 | PO字段冗余 | unitType + typeCode 双字段 | 统一为 unitType 引用 org_unit_types |

---

## 二、重构范围总览

```
Phase 1 (P0): 岗位模型 + 变更历史            ← 基础能力
Phase 2 (P1): 组织生命周期 + 乐观锁 + 编制    ← 治理能力
Phase 3 (P1): 前端组织架构图 + 拖拽排序        ← 体验升级
Phase 4 (P2): 生效日期 + 多线汇报 + 属性扩展   ← 高级能力
Phase 5 (P2): 批量导入/导出                   ← 运维效率
```

---

## 三、Phase 1：岗位模型 + 变更历史

### 3.1 数据库变更

#### 新增表：`positions`（岗位）

```sql
CREATE TABLE positions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    position_code   VARCHAR(50)  NOT NULL COMMENT '岗位编码',
    position_name   VARCHAR(100) NOT NULL COMMENT '岗位名称',
    org_unit_id     BIGINT       NOT NULL COMMENT '所属组织单元',
    job_level       VARCHAR(50)  DEFAULT NULL COMMENT '职级(高层/中层/基层/执行)',
    headcount       INT          DEFAULT 1 COMMENT '编制数',
    reports_to_id   BIGINT       DEFAULT NULL COMMENT '汇报岗位ID',
    responsibilities TEXT        DEFAULT NULL COMMENT '岗位职责',
    requirements    TEXT         DEFAULT NULL COMMENT '任职要求',
    sort_order      INT          DEFAULT 0,
    is_key_position TINYINT(1)   DEFAULT 0 COMMENT '是否关键岗位',
    enabled         TINYINT(1)   DEFAULT 1,
    tenant_id       BIGINT       NOT NULL DEFAULT 1,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    deleted         TINYINT      DEFAULT 0,
    version         INT          DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_position_code (position_code, deleted),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_reports_to (reports_to_id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_deleted (deleted)
) COMMENT='岗位表';
```

#### 新增表：`user_positions`（人岗关系）

```sql
CREATE TABLE user_positions (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    position_id       BIGINT       NOT NULL,
    is_primary        TINYINT(1)   DEFAULT 1 COMMENT '是否主岗',
    appointment_type  VARCHAR(30)  DEFAULT 'FORMAL' COMMENT 'FORMAL/ACTING/CONCURRENT/PROBATION',
    start_date        DATE         NOT NULL COMMENT '任命日期',
    end_date          DATE         DEFAULT NULL COMMENT '离任日期(NULL=在任)',
    appointment_reason VARCHAR(500) DEFAULT NULL COMMENT '任命原因',
    departure_reason  VARCHAR(500) DEFAULT NULL COMMENT '离任原因',
    tenant_id         BIGINT       NOT NULL DEFAULT 1,
    created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT,
    updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_position_id (position_id),
    INDEX idx_is_primary (is_primary),
    INDEX idx_end_date (end_date),
    INDEX idx_tenant (tenant_id)
) COMMENT='人岗关系表（支持历史）';
```

#### 新增表：`org_change_logs`（组织变更日志）

```sql
CREATE TABLE org_change_logs (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type    VARCHAR(50)  NOT NULL COMMENT 'ORG_UNIT/POSITION/USER_POSITION/CLASS/GRADE',
    entity_id      BIGINT       NOT NULL,
    change_type    VARCHAR(30)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE/ENABLE/DISABLE/MERGE/SPLIT/MOVE',
    changes        JSON         NOT NULL COMMENT '[{field, oldValue, newValue}]',
    reason         VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    operator_id    BIGINT       NOT NULL,
    operator_name  VARCHAR(50),
    operated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    tenant_id      BIGINT       NOT NULL DEFAULT 1,
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_operator (operator_id),
    INDEX idx_operated_at (operated_at),
    INDEX idx_tenant (tenant_id)
) COMMENT='组织变更日志';
```

#### 修改表：`org_units` 新增字段

```sql
ALTER TABLE org_units ADD COLUMN attributes JSON DEFAULT NULL COMMENT '扩展属性';
ALTER TABLE org_units ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE org_units ADD COLUMN headcount INT DEFAULT NULL COMMENT '编制数';
```

#### 清理：`org_units` 移除冗余

```sql
-- OrgUnitPO 中 typeCode 与 unitType 重复，保留 unit_type 字段
-- 确认 type_code 列是否存在，如存在则移除
ALTER TABLE org_units DROP COLUMN IF EXISTS type_code;
```

> **注意**：MySQL 8.0 不支持 `DROP COLUMN IF EXISTS`，需先 `SHOW COLUMNS` 检查。

---

### 3.2 后端领域层变更

#### 新增聚合根：`Position`

```
domain/organization/model/Position.java
```

**字段：**
- `Long id`
- `String positionCode`
- `String positionName`
- `Long orgUnitId`
- `String jobLevel`（HIGH / MIDDLE / BASE / EXECUTIVE）
- `int headcount`
- `Long reportsToId`（汇报岗位）
- `String responsibilities`
- `String requirements`
- `int sortOrder`
- `boolean keyPosition`
- `boolean enabled`
- `int version`
- 审计字段

**工厂方法：**
- `static Position create(code, name, orgUnitId, headcount, createdBy)` → 发布 `PositionCreatedEvent`

**业务方法：**
- `void update(name, jobLevel, headcount, reportsToId, responsibilities, requirements, updatedBy)` → 发布 `PositionUpdatedEvent`
- `void enable()` / `void disable()`
- `boolean isVacant(int currentCount)` — headcount > currentCount
- `boolean isOverstaffed(int currentCount)` — currentCount > headcount

#### 新增实体：`UserPosition`

```
domain/organization/model/entity/UserPosition.java
```

**字段：**
- `Long id`
- `Long userId`
- `Long positionId`
- `boolean primary`
- `AppointmentType appointmentType`（枚举：FORMAL / ACTING / CONCURRENT / PROBATION）
- `LocalDate startDate`
- `LocalDate endDate`（null = 在任）
- `String appointmentReason`
- `String departureReason`

**方法：**
- `static UserPosition appoint(userId, positionId, isPrimary, type, startDate, reason)` → 发布 `UserAppointedEvent`
- `void endAppointment(endDate, reason)` → 发布 `UserDepartedEvent`
- `boolean isCurrent()` — endDate == null

#### 新增值对象：`FieldChange`

```
domain/organization/model/valueobject/FieldChange.java
```

**字段（不可变）：**
- `String fieldName`
- `String oldValue`
- `String newValue`

#### 新增领域事件

```
domain/organization/event/
├── PositionCreatedEvent.java
├── PositionUpdatedEvent.java
├── UserAppointedEvent.java
├── UserDepartedEvent.java
└── OrgChangeLogEvent.java    ← 通用变更事件
```

`OrgChangeLogEvent` 字段：
- `String entityType`
- `Long entityId`
- `String changeType`
- `List<FieldChange> changes`
- `String reason`
- `Long operatorId`

#### 新增仓储接口

```
domain/organization/repository/PositionRepository.java
```

**方法：**
- `Optional<Position> findById(Long id)`
- `Optional<Position> findByPositionCode(String code)`
- `List<Position> findByOrgUnitId(Long orgUnitId)`
- `List<Position> findByReportsToId(Long reportsToId)`
- `boolean existsByPositionCode(String code)`
- `Position save(Position position)`
- `void delete(Long id)`

```
domain/organization/repository/UserPositionRepository.java
```

**方法：**
- `List<UserPosition> findByUserId(Long userId)`
- `List<UserPosition> findCurrentByUserId(Long userId)`
- `List<UserPosition> findByPositionId(Long positionId)`
- `List<UserPosition> findCurrentByPositionId(Long positionId)`
- `Optional<UserPosition> findPrimaryByUserId(Long userId)`
- `int countCurrentByPositionId(Long positionId)`
- `UserPosition save(UserPosition up)`

```
domain/organization/repository/OrgChangeLogRepository.java
```

**方法：**
- `void save(OrgChangeLog log)`
- `List<OrgChangeLog> findByEntity(String entityType, Long entityId)`
- `List<OrgChangeLog> findByEntityPaged(String entityType, Long entityId, int page, int size)`
- `List<OrgChangeLog> findByOperator(Long operatorId, LocalDateTime from, LocalDateTime to)`

#### 修改聚合根：`OrgUnit`

**新增字段：**
- `Map<String, Object> attributes`
- `int version`
- `Integer headcount`

**移除字段：**
- `Long leaderId` → 改用 `岗位.人` 关系表达
- `List<Long> deputyLeaderIds` → 同上

**新增方法：**
- `void setAttributes(Map<String, Object> attrs)`
- `void setHeadcount(Integer headcount)`

**移除方法：**
- `void assignLeader(...)` → 迁移到 PositionApplicationService

> **迁移策略**：leaderId/deputyLeaderIds 数据迁移到 positions + user_positions 表后，删除 OrgUnit 中的这两个字段。具体：
> 1. 为每个有 leaderId 的 OrgUnit 创建一个 "负责人" 岗位
> 2. 把 leaderId 写入 user_positions
> 3. deputyLeaderIds 同理创建 "副负责人" 岗位
> 4. 最后 ALTER TABLE DROP COLUMN

#### 修改 `OrgUnit.update()` 方法

**变更前：**
```java
void update(String unitName, Long leaderId, List<Long> deputyLeaderIds, Integer sortOrder, Long updatedBy)
```

**变更后：**
```java
List<FieldChange> update(String unitName, Integer sortOrder, Integer headcount, Map<String, Object> attributes, Long updatedBy)
```

返回 `List<FieldChange>` 用于自动生成变更日志。内部对比旧值和新值，只记录实际变化的字段。

---

### 3.3 后端应用层变更

#### 新增：`PositionApplicationService`

```
application/organization/PositionApplicationService.java
```

**方法：**
- `PositionDTO createPosition(CreatePositionCommand cmd)`
- `PositionDTO updatePosition(Long id, UpdatePositionCommand cmd)`
- `void deletePosition(Long id)`
- `void enablePosition(Long id)` / `void disablePosition(Long id)`
- `PositionDTO getPosition(Long id)`
- `List<PositionDTO> getPositionsByOrgUnit(Long orgUnitId)`
- `List<PositionDTO> getPositionTree(Long orgUnitId)` — 含汇报关系的树
- `PositionStaffingDTO getStaffing(Long orgUnitId)` — 编制统计

#### 新增：`UserPositionApplicationService`

```
application/organization/UserPositionApplicationService.java
```

**方法：**
- `UserPositionDTO appointUser(AppointUserCommand cmd)` — 任命
- `void endAppointment(Long userPositionId, EndAppointmentCommand cmd)` — 离任
- `void transferUser(TransferUserCommand cmd)` — 调岗（结束旧任命 + 创建新任命）
- `List<UserPositionDTO> getUserPositions(Long userId)` — 用户的所有岗位（含历史）
- `List<UserPositionDTO> getCurrentByPosition(Long positionId)` — 岗位当前在编人员
- `UserPositionDTO getPrimaryPosition(Long userId)` — 用户主岗

#### 新增：`OrgChangeLogService`

```
application/organization/OrgChangeLogService.java
```

**方法：**
- `void recordChange(OrgChangeLogEvent event)` — 记录变更（由事件监听器调用）
- `PageResult<OrgChangeLogDTO> getChangeLogs(String entityType, Long entityId, int page, int size)`
- `List<OrgChangeLogDTO> getRecentChanges(Long orgUnitId, int limit)`

#### 新增 Command 对象

```
application/organization/command/
├── CreatePositionCommand.java      (positionCode, positionName, orgUnitId, headcount, jobLevel, reportsToId, responsibilities, requirements)
├── UpdatePositionCommand.java      (positionName, headcount, jobLevel, reportsToId, responsibilities, requirements)
├── AppointUserCommand.java         (userId, positionId, isPrimary, appointmentType, startDate, reason)
├── EndAppointmentCommand.java      (endDate, reason)
└── TransferUserCommand.java        (userId, fromPositionId, toPositionId, transferDate, reason)
```

#### 新增 DTO 对象

```
application/organization/query/
├── PositionDTO.java                (id, positionCode, positionName, orgUnitId, orgUnitName, jobLevel, headcount, currentCount, vacantCount, reportsToId, reportsToName, responsibilities, requirements, keyPosition, enabled, holders[])
├── UserPositionDTO.java            (id, userId, userName, positionId, positionName, orgUnitId, orgUnitName, isPrimary, appointmentType, startDate, endDate, appointmentReason, departureReason, current)
├── PositionStaffingDTO.java        (orgUnitId, orgUnitName, totalHeadcount, actualCount, vacantCount, overstaffedCount, positions[{positionName, headcount, actualCount}])
└── OrgChangeLogDTO.java            (id, entityType, entityId, entityName, changeType, changes[], reason, operatorName, operatedAt)
```

#### 修改：`OrgUnitApplicationService`

- `createOrgUnit()` 方法内发布 `OrgChangeLogEvent(CREATE, ...)`
- `updateOrgUnit()` 方法调用 `orgUnit.update()` 获取 `List<FieldChange>`，发布 `OrgChangeLogEvent(UPDATE, changes)`
- `deleteOrgUnit()` 发布 `OrgChangeLogEvent(DELETE, ...)`
- 移除 `assignLeader()` 方法（迁移到 UserPositionApplicationService）
- `getOrgUnitTree()` 返回的 DTO 增加 `headcount`、`currentCount`（从 positions 聚合）

#### 修改：`OrgUnitTreeDTO`

**新增字段：**
- `Integer headcount` — 组织编制数
- `Integer currentCount` — 组织当前在编人数
- `String leaderName` — 从岗位关系查出的负责人姓名（兼容旧 UI）
- `List<PositionSummary> keyPositions` — 关键岗位摘要（positionName, holderName, vacant）

**移除字段：**
- `Long leaderId` → 改为从岗位查询

---

### 3.4 后端基础设施层变更

#### 新增持久化对象

```
infrastructure/persistence/organization/
├── PositionPO.java
├── PositionMapper.java
├── PositionRepositoryImpl.java
├── UserPositionPO.java
├── UserPositionMapper.java
├── UserPositionRepositoryImpl.java
├── OrgChangeLogPO.java
├── OrgChangeLogMapper.java
└── OrgChangeLogRepositoryImpl.java
```

#### 新增事件监听器

```
infrastructure/event/OrgChangeLogListener.java
```

- 监听 `OrgChangeLogEvent` → 写入 `org_change_logs` 表
- 监听 `PositionCreatedEvent` / `UserAppointedEvent` 等 → 转换为 `OrgChangeLogEvent` 再记录

#### 修改：`OrgUnitPO`

- 新增 `attributes` (JSON)、`version` (Integer)、`headcount` (Integer)
- 移除 `typeCode` 字段（与 `unitType` 重复）
- 移除 `leaderId`、`deputyLeaderIds`（迁移到 positions 后）

#### 修改：`OrgUnitRepositoryImpl`

- PO ↔ Domain 映射中移除 leaderId/deputyLeaderIds
- 新增 `attributes` / `version` / `headcount` 映射

---

### 3.5 后端接口层变更

#### 新增 Controller：`PositionController`

```
interfaces/rest/organization/PositionController.java
```

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/positions?orgUnitId={id}` | 按组织查岗位列表 |
| GET | `/positions/{id}` | 岗位详情 |
| POST | `/positions` | 创建岗位 |
| PUT | `/positions/{id}` | 更新岗位 |
| DELETE | `/positions/{id}` | 删除岗位 |
| PUT | `/positions/{id}/enable` | 启用 |
| PUT | `/positions/{id}/disable` | 禁用 |
| GET | `/positions/{id}/holders` | 在编人员 |
| GET | `/org-units/{id}/staffing` | 组织编制统计 |

#### 新增 Controller：`UserPositionController`

```
interfaces/rest/organization/UserPositionController.java
```

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user-positions/appoint` | 任命 |
| PUT | `/user-positions/{id}/end` | 离任 |
| POST | `/user-positions/transfer` | 调岗 |
| GET | `/user-positions?userId={id}` | 用户岗位列表（含历史） |
| GET | `/user-positions/primary?userId={id}` | 用户主岗 |

#### 新增 Controller：`OrgChangeLogController`

```
interfaces/rest/organization/OrgChangeLogController.java
```

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/org-change-logs?entityType=ORG_UNIT&entityId={id}&page=1&size=20` | 变更日志（分页） |
| GET | `/org-units/{id}/change-logs` | 组织变更日志（快捷路径） |

#### 修改：`OrgUnitController`

- 移除 `PUT /org-units/{id}/leader` 端点
- `PUT /org-units/{id}` 请求体新增 `headcount`、`attributes`、`reason`（变更原因）
- 响应体 OrgUnitTreeDTO 新增 `headcount`、`currentCount`、`keyPositions`

#### 修改：`UpdateOrgUnitRequest`

**新增字段：**
- `Integer headcount`
- `Map<String, Object> attributes`
- `String reason` — 变更原因（写入变更日志）

---

### 3.6 前端变更（Phase 1）

#### 新增类型定义

```typescript
// types/position.ts

export type AppointmentType = 'FORMAL' | 'ACTING' | 'CONCURRENT' | 'PROBATION'
export type JobLevel = 'HIGH' | 'MIDDLE' | 'BASE' | 'EXECUTIVE'

export interface Position {
  id: number
  positionCode: string
  positionName: string
  orgUnitId: number
  orgUnitName?: string
  jobLevel?: JobLevel
  headcount: number
  currentCount: number
  vacantCount: number
  reportsToId?: number
  reportsToName?: string
  responsibilities?: string
  requirements?: string
  keyPosition: boolean
  enabled: boolean
  holders: UserPositionSummary[]
}

export interface UserPosition {
  id: number
  userId: number
  userName: string
  positionId: number
  positionName: string
  orgUnitId: number
  orgUnitName: string
  isPrimary: boolean
  appointmentType: AppointmentType
  startDate: string
  endDate?: string
  appointmentReason?: string
  departureReason?: string
  current: boolean
}

export interface UserPositionSummary {
  userId: number
  userName: string
  isPrimary: boolean
  appointmentType: AppointmentType
}

export interface PositionStaffing {
  orgUnitId: number
  orgUnitName: string
  totalHeadcount: number
  actualCount: number
  vacantCount: number
  overstaffedCount: number
  positions: PositionStaffingItem[]
}

export interface PositionStaffingItem {
  positionName: string
  headcount: number
  actualCount: number
}

export interface OrgChangeLog {
  id: number
  entityType: string
  entityId: number
  entityName: string
  changeType: string
  changes: FieldChange[]
  reason?: string
  operatorName: string
  operatedAt: string
}

export interface FieldChange {
  fieldName: string
  oldValue: string
  newValue: string
}
```

#### 新增 API 文件

```typescript
// api/position.ts

export const positionApi = {
  list(orgUnitId: number): Promise<Position[]>
  get(id: number): Promise<Position>
  create(data: CreatePositionRequest): Promise<Position>
  update(id: number, data: UpdatePositionRequest): Promise<Position>
  delete(id: number): Promise<void>
  enable(id: number): Promise<void>
  disable(id: number): Promise<void>
  getHolders(id: number): Promise<UserPosition[]>
  getStaffing(orgUnitId: number): Promise<PositionStaffing>
}

export const userPositionApi = {
  appoint(data: AppointRequest): Promise<UserPosition>
  endAppointment(id: number, data: EndAppointmentRequest): Promise<void>
  transfer(data: TransferRequest): Promise<void>
  listByUser(userId: number): Promise<UserPosition[]>
  getPrimary(userId: number): Promise<UserPosition>
}

export const orgChangeLogApi = {
  list(entityType: string, entityId: number, page: number, size: number): Promise<PageResult<OrgChangeLog>>
  listByOrgUnit(orgUnitId: number): Promise<OrgChangeLog[]>
}
```

#### 修改：`OrgDetailPanel.vue`

**当前 Tab 结构：**
```
[子级] [成员] [场所] [信息]
```

**新增 Tab：**
```
[子级] [岗位] [成员] [场所] [变更记录] [信息]
```

**「岗位」Tab 内容：**
- 顶部：编制统计条（总编制 / 在编 / 空缺 / 超编），紧凑风格
- 列表：岗位卡片，每个卡片显示：
  - 岗位名称 + 职级标签
  - 编制：2/3（currentCount / headcount）
  - 在岗人员名字列表
  - 操作：编辑、删除、任命人员
- 底部：「新增岗位」按钮

**「变更记录」Tab 内容：**
- 时间线组件，每条记录显示：
  - 时间 + 操作人
  - 变更类型标签（创建/更新/启用/禁用/...）
  - 字段变更明细（旧值 → 新值）
  - 变更原因

#### 新增组件：`PositionCard.vue`

```
views/organization/structure/components/PositionCard.vue
```

- 单个岗位的展示卡片
- 显示岗位名、职级、编制比、在岗人员
- 操作按钮：编辑、任命、离任

#### 新增组件：`PositionFormDialog.vue`

```
views/organization/structure/components/PositionFormDialog.vue
```

- 创建/编辑岗位的表单对话框
- 字段：岗位名称、编码（自动生成）、职级、编制数、汇报岗位（树选择）、职责、要求

#### 新增组件：`AppointmentDialog.vue`

```
views/organization/structure/components/AppointmentDialog.vue
```

- 任命人员对话框
- 字段：人员选择（搜索用户）、是否主岗、任命类型、生效日期、任命原因

#### 新增组件：`OrgChangeTimeline.vue`

```
views/organization/structure/components/OrgChangeTimeline.vue
```

- 变更记录时间线
- 自动加载分页
- 展示字段级变更对比

#### 修改：`OrgSidebar.vue`

- 节点信息中 "负责人" 改为从岗位关系获取（`keyPositions[0].holderName`）
- 移除直接编辑 leader 的快捷操作

#### 修改：`OrgStructure.vue`

- 统计条新增：总编制数、在编人数、空缺数
- loadData 时同时获取 staffing 统计

#### 修改：`types/organization.ts`

**OrgUnitTreeNode 新增字段：**
```typescript
headcount?: number
currentCount?: number
keyPositions?: { positionName: string; holderName?: string; vacant: boolean }[]
```

**OrgUnit 移除字段：**
```typescript
// 以下字段在迁移完成后移除
leaderId: number | null        // → 改用岗位关系
leaderName?: string            // → 改用岗位关系
deputyLeaderIds: number[]      // → 改用岗位关系
```

---

## 四、Phase 2：组织生命周期 + 乐观锁 + 编制预警

### 4.1 数据库变更

#### 修改 `org_units` 表

```sql
-- 状态从 TINYINT(0/1) 改为 VARCHAR，支持更多状态
ALTER TABLE org_units MODIFY COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'DRAFT/ACTIVE/FROZEN/MERGING/DISSOLVED';

-- 合并/拆分支持
ALTER TABLE org_units ADD COLUMN merged_into_id BIGINT DEFAULT NULL COMMENT '合并到哪个组织';
ALTER TABLE org_units ADD COLUMN split_from_id BIGINT DEFAULT NULL COMMENT '从哪个组织拆分';
ALTER TABLE org_units ADD COLUMN dissolved_at DATETIME DEFAULT NULL COMMENT '撤销时间';
ALTER TABLE org_units ADD COLUMN dissolved_reason VARCHAR(500) DEFAULT NULL COMMENT '撤销原因';
```

### 4.2 领域模型变更

#### 新增枚举：`OrgUnitStatus`

```java
public enum OrgUnitStatus {
    DRAFT("草稿"),          // 新建未生效
    ACTIVE("正常"),         // 正常运行
    FROZEN("冻结"),         // 暂停运行（不可增人）
    MERGING("合并中"),      // 正在执行合并
    DISSOLVED("已撤销");    // 已撤销
}
```

#### 修改 `OrgUnit` 聚合根

**字段变更：**
- `Boolean enabled` → `OrgUnitStatus status`
- 新增 `Long mergedIntoId`
- 新增 `Long splitFromId`
- 新增 `LocalDateTime dissolvedAt`
- 新增 `String dissolvedReason`

**新增方法：**
- `void freeze(String reason, Long updatedBy)` → 发布 OrgUnitStatusChangedEvent
- `void unfreeze(Long updatedBy)` → 发布 OrgUnitStatusChangedEvent
- `void dissolve(String reason, Long updatedBy)` → 设置 dissolvedAt，发布 OrgUnitDissolvedEvent
- `boolean canAddPositions()` — status == ACTIVE
- `boolean canAddChildren()` — status == ACTIVE || DRAFT

**合并/拆分（在 DomainService 中）：**
- `OrgUnitDomainService.mergeOrgUnits(sourceId, targetId, reason)` →
  1. source.status = DISSOLVED, source.mergedIntoId = targetId
  2. 把 source 的子组织挂到 target 下
  3. 把 source 的岗位迁移到 target
  4. 发布 OrgUnitMergedEvent
- `OrgUnitDomainService.splitOrgUnit(sourceId, newUnits[], reason)` →
  1. 按配置创建新组织
  2. 新组织 splitFromId = sourceId
  3. 迁移指定的子组织和岗位到新组织
  4. 发布 OrgUnitSplitEvent

### 4.3 乐观锁

#### 所有核心实体 PO 新增 `version` 字段

```java
// OrgUnitPO.java
@Version
@TableField("version")
private Integer version;
```

MyBatis Plus 的 `@Version` 注解自动在 UPDATE 时追加 `WHERE version = ?` 并自增，更新失败时抛出 `OptimisticLockException`。

**需要在 MybatisPlusConfig 中注册拦截器：**

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
    return interceptor;
}
```

**前端处理：** 409 Conflict → 提示 "数据已被其他用户修改，请刷新后重试"

### 4.4 前端变更（Phase 2）

#### 修改：组织状态显示

- 侧边栏节点增加状态标签（草稿/冻结/已撤销 用不同颜色标签）
- 详情面板显示状态 + 可执行的操作按钮（冻结/解冻/撤销）
- 已撤销的组织灰显，默认折叠

#### 新增：合并/拆分对话框

```
views/organization/structure/components/MergeOrgDialog.vue
views/organization/structure/components/SplitOrgDialog.vue
```

**合并对话框：**
- 选择目标组织（树选择器，排除自身及子级）
- 预览：将要迁移的子组织列表、岗位列表
- 输入合并原因
- 确认后执行

**拆分对话框：**
- 输入新组织数量和名称
- 拖拽分配子组织到不同新组织
- 输入拆分原因
- 确认后执行

#### 新增：编制预警

- 统计条显示：空缺岗位数（黄色）、超编岗位数（红色）
- 岗位列表中超编岗位标红
- 首页仪表盘可增加"编制异常"提醒卡片

---

## 五、Phase 3：前端组织架构图 + 拖拽排序

### 5.1 组织架构图组件

```
views/organization/structure/components/OrgChart.vue
```

**功能：**
- 基于 `vue-flow` 或 `@antv/g6` 的树形图/组织架构图
- 每个节点显示：组织名称、类型标签、负责人头像、编制比
- 支持缩放、平移、居中
- 点击节点 → 弹出详情面板
- 支持截图/导出为图片

**视图切换：**
- 在 OrgStructure.vue 顶部增加视图切换按钮：
  - 树形视图（当前侧边栏）← 默认
  - 架构图视图（OrgChart）

### 5.2 拖拽排序

**在侧边栏树中：**
- 长按节点可拖拽
- 拖拽到另一个节点上方/下方 → 调整 sortOrder
- 拖拽到另一个节点内部 → 移动组织（调用 moveOrgUnit API）
- 拖拽时显示放置指示器
- 释放后调用 API 保存新顺序

**API 新增：**
```
PUT /org-units/reorder  Body: [{id, sortOrder}]
PUT /org-units/{id}/move  Body: {newParentId, reason}
```

### 5.3 搜索增强

**当前：** 只能过滤树节点

**增强：**
- 搜索结果列表 + 高亮定位：搜索后点击结果项 → 自动展开到该节点并高亮
- 支持按类型、状态筛选
- 支持模糊搜索组织名称、编码、负责人姓名

---

## 六、Phase 4：生效日期 + 多线汇报 + 属性扩展

### 6.1 生效日期

#### 数据库新增字段

```sql
ALTER TABLE org_units ADD COLUMN effective_date DATE DEFAULT NULL COMMENT '生效日期';
ALTER TABLE positions ADD COLUMN effective_date DATE DEFAULT NULL COMMENT '生效日期';
```

#### 领域模型变更

- OrgUnit 新增 `LocalDate effectiveDate`
- 创建时可指定未来生效日期 → status = DRAFT
- 定时任务在每天 00:00 扫描 `effective_date <= TODAY AND status = DRAFT` → 自动激活

#### 前端

- 创建/编辑组织时新增 "生效日期" 日期选择器
- 未生效的组织显示 "待生效" 标签 + 生效倒计时

### 6.2 多线汇报关系

#### 新增表：`org_relations`

```sql
CREATE TABLE org_relations (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id       BIGINT NOT NULL,
    child_id        BIGINT NOT NULL,
    relation_type   VARCHAR(30) NOT NULL COMMENT 'ADMINISTRATIVE/BUSINESS/PROJECT',
    is_primary      TINYINT(1) DEFAULT 0,
    description     VARCHAR(200),
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_relation (parent_id, child_id, relation_type),
    INDEX idx_child (child_id),
    INDEX idx_type (relation_type)
) COMMENT='组织多线关系';
```

#### 说明

- `ADMINISTRATIVE`（行政线）= 当前的 parentId 关系
- `BUSINESS`（业务线）= 虚线汇报
- `PROJECT`（项目线）= 临时项目制关系
- 现有的 `parentId` 树结构保持不变（= 主行政线）
- `org_relations` 表作为补充关系

#### 前端

- 组织详情新增 "关联组织" Tab
- 显示该组织的所有关系（行政上级、业务上级、项目关联等）
- 在架构图中用实线/虚线区分不同关系类型

### 6.3 属性扩展

Phase 1 已在 org_units 表增加 `attributes JSON`。本阶段在前端实现：

- 组织详情 "信息" Tab 中显示扩展属性
- 编辑表单底部增加 "自定义属性" 区域
- 动态表单：key-value 输入（支持文本、数字、日期、下拉选择）
- 属性定义可从 `org_unit_types` 表的扩展配置中读取（每种类型有不同的属性 schema）

---

## 七、Phase 5：批量导入/导出

### 7.1 组织导入

#### API

```
POST /org-units/import  Body: MultipartFile (Excel)
GET  /org-units/import/template  → 下载导入模板
```

#### Excel 模板格式

| 组织编码 | 组织名称 | 类型编码 | 上级编码 | 编制数 | 排序号 | 备注 |
|---------|---------|---------|---------|--------|-------|------|
| DEP-001 | 信息技术部 | department | ROOT | 20 | 1 | |

#### 后端

```
application/organization/OrgUnitImportService.java
```

- 读取 Excel（Apache POI）
- 校验：编码唯一性、类型存在性、上级存在性
- 按树形层级排序后批量创建
- 返回导入结果（成功数、失败数、失败详情）

### 7.2 组织导出

#### API

```
GET /org-units/export?format=excel  → 导出完整组织树
```

- 导出为 Excel，含层级缩进
- 可选导出格式：扁平列表 / 带缩进的树形

### 7.3 前端

- 组织管理页面顶部工具栏增加 "导入" / "导出" 按钮
- 导入：上传对话框 + 进度显示 + 结果报告
- 导出：直接下载

---

## 八、数据库迁移汇总

### V27.0.0__org_positions.sql

```sql
-- 1. 岗位表
CREATE TABLE positions (...);

-- 2. 人岗关系表
CREATE TABLE user_positions (...);

-- 3. 组织变更日志表
CREATE TABLE org_change_logs (...);

-- 4. org_units 新增字段
ALTER TABLE org_units ADD COLUMN attributes JSON DEFAULT NULL;
ALTER TABLE org_units ADD COLUMN version INT DEFAULT 0;
ALTER TABLE org_units ADD COLUMN headcount INT DEFAULT NULL;

-- 5. 数据迁移：leaderId → positions + user_positions
-- (由后端 DataMigrationService 执行，不在 SQL 中)
```

### V28.0.0__org_lifecycle.sql

```sql
-- 1. org_units 状态改为 VARCHAR
ALTER TABLE org_units MODIFY COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';
UPDATE org_units SET status = 'ACTIVE' WHERE status = '1';
UPDATE org_units SET status = 'DISSOLVED' WHERE status = '0';

-- 2. 合并/拆分字段
ALTER TABLE org_units ADD COLUMN merged_into_id BIGINT DEFAULT NULL;
ALTER TABLE org_units ADD COLUMN split_from_id BIGINT DEFAULT NULL;
ALTER TABLE org_units ADD COLUMN dissolved_at DATETIME DEFAULT NULL;
ALTER TABLE org_units ADD COLUMN dissolved_reason VARCHAR(500) DEFAULT NULL;

-- 3. 生效日期
ALTER TABLE org_units ADD COLUMN effective_date DATE DEFAULT NULL;
ALTER TABLE positions ADD COLUMN effective_date DATE DEFAULT NULL;

-- 4. 多线关系表
CREATE TABLE org_relations (...);
```

### V29.0.0__cleanup_leader_fields.sql

```sql
-- 确认迁移完成后执行
ALTER TABLE org_units DROP COLUMN leader_id;
ALTER TABLE org_units DROP COLUMN deputy_leader_ids;
```

---

## 九、文件变更清单

### 新建文件（~35个）

| 层 | 文件 | 说明 |
|---|------|------|
| **Domain** | `domain/organization/model/Position.java` | 岗位聚合根 |
| | `domain/organization/model/entity/UserPosition.java` | 人岗关系实体 |
| | `domain/organization/model/valueobject/FieldChange.java` | 变更字段值对象 |
| | `domain/organization/model/valueobject/OrgUnitStatus.java` | 组织状态枚举 |
| | `domain/organization/model/valueobject/AppointmentType.java` | 任命类型枚举 |
| | `domain/organization/model/valueobject/JobLevel.java` | 职级枚举 |
| | `domain/organization/repository/PositionRepository.java` | 岗位仓储接口 |
| | `domain/organization/repository/UserPositionRepository.java` | 人岗仓储接口 |
| | `domain/organization/repository/OrgChangeLogRepository.java` | 变更日志仓储 |
| | `domain/organization/event/PositionCreatedEvent.java` | |
| | `domain/organization/event/PositionUpdatedEvent.java` | |
| | `domain/organization/event/UserAppointedEvent.java` | |
| | `domain/organization/event/UserDepartedEvent.java` | |
| | `domain/organization/event/OrgChangeLogEvent.java` | |
| | `domain/organization/event/OrgUnitMergedEvent.java` | |
| | `domain/organization/event/OrgUnitSplitEvent.java` | |
| **Application** | `application/organization/PositionApplicationService.java` | 岗位应用服务 |
| | `application/organization/UserPositionApplicationService.java` | 人岗应用服务 |
| | `application/organization/OrgChangeLogService.java` | 变更日志服务 |
| | `application/organization/command/CreatePositionCommand.java` | |
| | `application/organization/command/UpdatePositionCommand.java` | |
| | `application/organization/command/AppointUserCommand.java` | |
| | `application/organization/command/EndAppointmentCommand.java` | |
| | `application/organization/command/TransferUserCommand.java` | |
| | `application/organization/query/PositionDTO.java` | |
| | `application/organization/query/UserPositionDTO.java` | |
| | `application/organization/query/PositionStaffingDTO.java` | |
| | `application/organization/query/OrgChangeLogDTO.java` | |
| **Infra** | `infrastructure/persistence/organization/PositionPO.java` | |
| | `infrastructure/persistence/organization/PositionMapper.java` | |
| | `infrastructure/persistence/organization/PositionRepositoryImpl.java` | |
| | `infrastructure/persistence/organization/UserPositionPO.java` | |
| | `infrastructure/persistence/organization/UserPositionMapper.java` | |
| | `infrastructure/persistence/organization/UserPositionRepositoryImpl.java` | |
| | `infrastructure/persistence/organization/OrgChangeLogPO.java` | |
| | `infrastructure/persistence/organization/OrgChangeLogMapper.java` | |
| | `infrastructure/persistence/organization/OrgChangeLogRepositoryImpl.java` | |
| | `infrastructure/event/OrgChangeLogListener.java` | 事件监听 |
| **Interfaces** | `interfaces/rest/organization/PositionController.java` | |
| | `interfaces/rest/organization/UserPositionController.java` | |
| | `interfaces/rest/organization/OrgChangeLogController.java` | |
| **Frontend** | `frontend/src/types/position.ts` | 岗位类型定义 |
| | `frontend/src/api/position.ts` | 岗位 API |
| | `frontend/src/api/orgChangeLog.ts` | 变更日志 API |
| | `frontend/src/views/organization/structure/components/PositionCard.vue` | 岗位卡片 |
| | `frontend/src/views/organization/structure/components/PositionFormDialog.vue` | 岗位表单 |
| | `frontend/src/views/organization/structure/components/AppointmentDialog.vue` | 任命对话框 |
| | `frontend/src/views/organization/structure/components/OrgChangeTimeline.vue` | 变更时间线 |
| | `frontend/src/views/organization/structure/components/OrgChart.vue` | 架构图 |
| | `frontend/src/views/organization/structure/components/MergeOrgDialog.vue` | 合并对话框 |
| | `frontend/src/views/organization/structure/components/SplitOrgDialog.vue` | 拆分对话框 |
| **DB** | `database/migrations/V27.0.0__org_positions.sql` | |
| | `database/migrations/V28.0.0__org_lifecycle.sql` | |
| | `database/migrations/V29.0.0__cleanup_leader_fields.sql` | |

### 修改文件（~15个）

| 文件 | 改动 |
|------|------|
| `domain/organization/model/OrgUnit.java` | 新增 attributes/version/headcount/status枚举，移除 leaderId/deputyLeaderIds，update() 返回 FieldChange |
| `domain/organization/service/OrgUnitDomainService.java` | 新增 mergeOrgUnits()、splitOrgUnit() |
| `application/organization/OrgUnitApplicationService.java` | 移除 assignLeader，新增变更日志发布，树 DTO 增加岗位统计 |
| `application/organization/query/OrgUnitTreeDTO.java` | 新增 headcount、currentCount、keyPositions |
| `application/organization/command/UpdateOrgUnitCommand.java` | 新增 headcount、attributes、reason |
| `infrastructure/persistence/organization/OrgUnitPO.java` | 新增 attributes/version/headcount，移除 typeCode/leaderId/deputyLeaderIds |
| `infrastructure/persistence/organization/OrgUnitRepositoryImpl.java` | 更新 PO ↔ Domain 映射 |
| `interfaces/rest/organization/OrgUnitController.java` | 移除 leader 端点，新增 reorder/move/import/export |
| `interfaces/rest/organization/UpdateOrgUnitRequest.java` | 新增 headcount/attributes/reason |
| `frontend/src/types/organization.ts` | OrgUnitTreeNode 新增 headcount/currentCount/keyPositions，移除 leaderId |
| `frontend/src/api/organization.ts` | 新增 reorder/move/import/export API |
| `frontend/src/views/organization/structure/OrgStructure.vue` | 统计条增加编制数据，视图切换，导入导出按钮 |
| `frontend/src/views/organization/structure/components/OrgDetailPanel.vue` | 新增 岗位/变更记录 Tab |
| `frontend/src/views/organization/structure/components/OrgSidebar.vue` | 负责人改从岗位获取，增加拖拽排序 |
| `frontend/src/views/organization/structure/components/OrgUnitForm.vue` | 新增编制数、扩展属性、变更原因字段 |

---

## 十、实施顺序与验证

### Phase 1（约 2-3 天）
1. 创建 V27 迁移 SQL → 执行
2. 后端：Position 领域模型 + 仓储 + 持久化 + API
3. 后端：UserPosition 领域模型 + 仓储 + 持久化 + API
4. 后端：OrgChangeLog 服务 + 事件监听 + API
5. 后端：修改 OrgUnit（新增 attributes/version/headcount）
6. 前端：types/position.ts + api/position.ts
7. 前端：PositionCard + PositionFormDialog + AppointmentDialog
8. 前端：OrgDetailPanel 新增岗位 Tab + 变更记录 Tab
9. **验证**：创建岗位 → 任命人员 → 查看编制统计 → 查看变更日志

### Phase 2（约 1-2 天）
1. 创建 V28 迁移 SQL → 执行
2. 后端：OrgUnitStatus 枚举 + OrgUnit 状态机 + 乐观锁
3. 后端：合并/拆分 DomainService
4. 前端：状态标签 + 冻结/解冻/撤销操作
5. 前端：MergeOrgDialog + SplitOrgDialog
6. **验证**：冻结组织 → 验证无法新增岗位 → 合并两个组织 → 查看变更日志

### Phase 3（约 2 天）
1. 前端：OrgChart 组织架构图（引入 vue-flow 或 @antv/g6）
2. 前端：视图切换按钮
3. 前端：拖拽排序（sortOrder）+ 拖拽移动（moveOrgUnit）
4. 前端：搜索增强（高亮定位）
5. 后端：新增 reorder / move API
6. **验证**：切换架构图视图 → 拖拽排序 → 搜索定位

### Phase 4（约 1-2 天）
1. 后端：effective_date + 定时激活任务
2. 后端：org_relations 多线关系 CRUD
3. 前端：生效日期选择器 + 待生效标签
4. 前端：关联组织 Tab
5. **验证**：创建未来生效组织 → 验证定时激活 → 添加业务线关系

### Phase 5（约 1 天）
1. 后端：OrgUnitImportService（Apache POI）
2. 后端：导出 API
3. 前端：导入对话框 + 导出按钮
4. **验证**：下载模板 → 填入数据 → 导入 → 验证结果

### 收尾
1. 执行 V29 迁移：删除 leaderId / deputyLeaderIds 字段
2. 全量回归测试
3. 更新 API 文档
