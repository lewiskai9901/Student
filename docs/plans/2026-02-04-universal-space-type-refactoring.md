# 通用空间类型系统重构设计

## 概述

### 目标
将现有的学校专属空间管理系统重构为通用的空间管理系统，适用于学校、医院、公司、政府、工厂等各类场景。

### 策略
采用"新版本开发后删除旧版本"策略：
1. 保留现有代码，开发新版本（v7前缀）
2. 新版本验证通过后，删除旧版本
3. 现有数据可清空，无需迁移

### 核心设计理念
- **零硬编码**：所有类型通过配置定义，无领域专属命名
- **类型驱动**：层级结构由SpaceType配置决定
- **行为抽象**：通过布尔特性标记空间能力

---

## 数据库设计

### 新表：space_types（空间类型配置）

```sql
CREATE TABLE space_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基础信息
    type_code VARCHAR(50) NOT NULL UNIQUE COMMENT '类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    icon VARCHAR(50) COMMENT '图标',
    description VARCHAR(500) COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统预置',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',

    -- 层级关系
    allowed_child_types JSON COMMENT '允许的子类型编码列表',
    is_root_type BOOLEAN DEFAULT FALSE COMMENT '是否可作为根节点',

    -- 行为特性
    has_capacity BOOLEAN DEFAULT FALSE COMMENT '是否有容量',
    bookable BOOLEAN DEFAULT FALSE COMMENT '是否可预订',
    assignable BOOLEAN DEFAULT FALSE COMMENT '是否可分配给组织',
    occupiable BOOLEAN DEFAULT FALSE COMMENT '是否可入住/占用',

    -- 容量配置
    capacity_unit VARCHAR(20) COMMENT '容量单位(人/床位/工位/㎡)',
    default_capacity INT COMMENT '默认容量',

    -- 扩展属性
    attribute_schema JSON COMMENT '扩展属性JSON Schema',

    -- 审计字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_type_code (type_code),
    INDEX idx_is_root (is_root_type),
    INDEX idx_enabled (is_enabled)
) COMMENT '空间类型配置表';
```

### 新表：spaces（空间实例）

```sql
CREATE TABLE spaces (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基础信息
    space_code VARCHAR(50) NOT NULL UNIQUE COMMENT '空间编码',
    space_name VARCHAR(100) NOT NULL COMMENT '空间名称',
    type_code VARCHAR(50) NOT NULL COMMENT '空间类型编码',
    description VARCHAR(500) COMMENT '描述',

    -- 层级关系
    parent_id BIGINT COMMENT '父级ID',
    path VARCHAR(500) COMMENT '物化路径',
    level INT DEFAULT 0 COMMENT '层级深度',

    -- 容量（当类型has_capacity=true时使用）
    capacity INT COMMENT '容量',
    current_occupancy INT DEFAULT 0 COMMENT '当前占用数',

    -- 归属（当类型assignable=true时使用）
    org_unit_id BIGINT COMMENT '所属组织单元ID',
    responsible_user_id BIGINT COMMENT '负责人ID',

    -- 状态
    status TINYINT DEFAULT 1 COMMENT '状态: 0-停用 1-正常 2-维护中',

    -- 扩展属性
    attributes JSON COMMENT '扩展属性值',

    -- 审计字段
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_space_code (space_code),
    INDEX idx_type_code (type_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_path (path),
    INDEX idx_org_unit (org_unit_id),
    INDEX idx_status (status)
) COMMENT '空间实例表';
```

### 新表：space_occupants（空间占用记录）

```sql
CREATE TABLE space_occupants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    space_id BIGINT NOT NULL COMMENT '空间ID',
    occupant_type VARCHAR(20) NOT NULL COMMENT '占用者类型',
    occupant_id BIGINT NOT NULL COMMENT '占用者ID',
    position_no VARCHAR(20) COMMENT '位置号(床位号/工位号)',

    check_in_time DATETIME NOT NULL COMMENT '入住时间',
    check_out_time DATETIME COMMENT '退出时间',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-已退出 1-在住',

    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_space_id (space_id),
    INDEX idx_occupant (occupant_type, occupant_id),
    INDEX idx_status (status)
) COMMENT '空间占用记录表';
```

### 新表：space_bookings（空间预订记录）

```sql
CREATE TABLE space_bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    space_id BIGINT NOT NULL COMMENT '空间ID',
    booker_id BIGINT NOT NULL COMMENT '预订人ID',
    title VARCHAR(200) COMMENT '预订标题',

    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',

    status TINYINT DEFAULT 1 COMMENT '状态: 0-已取消 1-待使用 2-使用中 3-已完成',

    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_space_id (space_id),
    INDEX idx_booker (booker_id),
    INDEX idx_time_range (start_time, end_time),
    INDEX idx_status (status)
) COMMENT '空间预订记录表';
```

### 预置数据（通用示例）

```sql
-- 通用空间类型（用户可自定义）
INSERT INTO space_types (type_code, type_name, icon, is_root_type, allowed_child_types,
                         has_capacity, bookable, assignable, occupiable,
                         capacity_unit, is_system) VALUES
-- 根节点类型
('SITE', '场地', 'Location', TRUE, '["BUILDING"]', FALSE, FALSE, FALSE, FALSE, NULL, TRUE),

-- 建筑类型
('BUILDING', '建筑', 'Building', FALSE, '["FLOOR"]', FALSE, FALSE, TRUE, FALSE, NULL, TRUE),

-- 楼层类型
('FLOOR', '楼层', 'Layers', FALSE, '["ROOM","AREA"]', FALSE, FALSE, TRUE, FALSE, NULL, TRUE),

-- 房间类型（通用）
('ROOM', '房间', 'Door', FALSE, '[]', TRUE, TRUE, TRUE, FALSE, '人', TRUE),

-- 区域类型（开放空间）
('AREA', '区域', 'Grid', FALSE, '["STATION"]', TRUE, FALSE, TRUE, FALSE, '㎡', TRUE),

-- 工位类型
('STATION', '工位', 'Armchair', FALSE, '[]', FALSE, TRUE, TRUE, TRUE, NULL, TRUE);
```

---

## 后端架构设计

### 包结构（v7命名空间）

```
com.school.management/
├── domain/space/
│   ├── model/
│   │   ├── aggregate/
│   │   │   └── Space.java              # 空间聚合根
│   │   ├── entity/
│   │   │   ├── SpaceType.java          # 空间类型实体
│   │   │   ├── SpaceOccupant.java      # 占用记录实体
│   │   │   └── SpaceBooking.java       # 预订记录实体
│   │   └── valueobject/
│   │       └── SpaceStatus.java        # 状态枚举（仅保留通用状态）
│   └── repository/
│       ├── SpaceRepository.java
│       ├── SpaceTypeRepository.java
│       ├── SpaceOccupantRepository.java
│       └── SpaceBookingRepository.java
│
├── application/space/
│   ├── SpaceApplicationService.java     # 空间应用服务
│   ├── SpaceTypeApplicationService.java # 类型配置服务
│   ├── SpaceOccupancyService.java       # 占用管理服务
│   ├── SpaceBookingService.java         # 预订管理服务
│   ├── command/
│   │   ├── CreateSpaceCommand.java
│   │   ├── UpdateSpaceCommand.java
│   │   ├── CheckInCommand.java
│   │   └── BookingCommand.java
│   └── query/
│       ├── SpaceDTO.java
│       ├── SpaceTypeDTO.java
│       ├── SpaceTreeNode.java
│       └── SpaceQueryCriteria.java
│
├── infrastructure/persistence/space/
│   ├── SpacePO.java
│   ├── SpaceTypePO.java
│   ├── SpaceOccupantPO.java
│   ├── SpaceBookingPO.java
│   ├── SpaceMapper.java
│   ├── SpaceTypeMapper.java
│   ├── SpaceOccupantMapper.java
│   ├── SpaceBookingMapper.java
│   └── *RepositoryImpl.java
│
└── interfaces/rest/space/
    ├── SpaceController.java             # 空间管理API
    ├── SpaceTypeController.java         # 类型配置API
    └── dto/
        ├── CreateSpaceRequest.java
        ├── UpdateSpaceRequest.java
        └── ...
```

### SpaceType 领域实体

```java
public class SpaceType implements Entity<Long> {
    private Long id;

    // 基础信息
    private String typeCode;
    private String typeName;
    private String icon;
    private String description;
    private Integer sortOrder;
    private boolean isSystem;
    private boolean isEnabled;

    // 层级关系
    private List<String> allowedChildTypes;
    private boolean isRootType;

    // 行为特性
    private boolean hasCapacity;
    private boolean bookable;
    private boolean assignable;
    private boolean occupiable;

    // 容量配置
    private String capacityUnit;
    private Integer defaultCapacity;

    // 扩展属性Schema
    private String attributeSchema;

    // 业务方法
    public boolean canHaveChild(String childTypeCode) {
        return allowedChildTypes != null && allowedChildTypes.contains(childTypeCode);
    }

    public boolean isLeafType() {
        return allowedChildTypes == null || allowedChildTypes.isEmpty();
    }
}
```

### Space 聚合根

```java
public class Space extends AggregateRoot<Long> {
    private Long id;

    // 基础信息
    private String spaceCode;
    private String spaceName;
    private String typeCode;  // 关联SpaceType
    private String description;

    // 层级关系
    private Long parentId;
    private String path;
    private Integer level;

    // 容量
    private Integer capacity;
    private Integer currentOccupancy;

    // 归属
    private Long orgUnitId;
    private Long responsibleUserId;

    // 状态
    private SpaceStatus status;

    // 扩展属性
    private Map<String, Object> attributes;

    // 工厂方法
    public static Space create(String spaceCode, String spaceName, String typeCode, Long parentId) {
        // ...
    }

    // 业务方法
    public void checkIn(SpaceOccupant occupant) {
        // 验证是否可入住
        // 更新currentOccupancy
    }

    public void checkOut(Long occupantId) {
        // 更新currentOccupancy
    }

    public boolean hasAvailableCapacity() {
        return capacity == null || currentOccupancy < capacity;
    }
}
```

---

## 前端架构设计

### 文件结构

```
frontend/src/
├── api/
│   ├── space.ts              # 空间管理API
│   ├── spaceType.ts          # 类型配置API
│   ├── spaceOccupancy.ts     # 占用管理API
│   └── spaceBooking.ts       # 预订管理API
│
├── types/
│   ├── space.ts              # 空间类型定义
│   └── spaceType.ts          # 类型配置定义
│
├── views/space/
│   ├── SpaceManagement.vue       # 空间管理主页
│   ├── SpaceTypeConfig.vue       # 类型配置页
│   ├── components/
│   │   ├── SpaceTree.vue         # 空间树组件
│   │   ├── SpaceForm.vue         # 空间表单
│   │   ├── SpaceTypeForm.vue     # 类型配置表单
│   │   ├── OccupancyPanel.vue    # 占用管理面板
│   │   └── BookingPanel.vue      # 预订管理面板
│   └── ...
```

### TypeScript 类型定义

```typescript
// 空间类型配置
export interface SpaceType {
  id: number
  typeCode: string
  typeName: string
  icon?: string
  description?: string
  sortOrder: number
  isSystem: boolean
  isEnabled: boolean

  // 层级关系
  allowedChildTypes: string[]
  isRootType: boolean

  // 行为特性
  hasCapacity: boolean
  bookable: boolean
  assignable: boolean
  occupiable: boolean

  // 容量配置
  capacityUnit?: string
  defaultCapacity?: number

  // 扩展属性
  attributeSchema?: object
}

// 空间实例
export interface Space {
  id: number
  spaceCode: string
  spaceName: string
  typeCode: string
  typeName?: string  // 冗余显示
  description?: string

  parentId?: number
  path?: string
  level: number

  capacity?: number
  currentOccupancy?: number

  orgUnitId?: number
  orgUnitName?: string
  responsibleUserId?: number
  responsibleUserName?: string

  status: SpaceStatus
  attributes?: Record<string, any>
}

// 空间状态（通用）
export enum SpaceStatus {
  DISABLED = 0,
  NORMAL = 1,
  MAINTENANCE = 2
}
```

---

## API 设计

### 空间类型配置 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v7/space-types` | 获取所有类型 |
| GET | `/api/v7/space-types/tree` | 获取类型树（按层级关系） |
| GET | `/api/v7/space-types/root` | 获取所有根类型 |
| GET | `/api/v7/space-types/{typeCode}/children` | 获取允许的子类型 |
| POST | `/api/v7/space-types` | 创建类型 |
| PUT | `/api/v7/space-types/{id}` | 更新类型 |
| DELETE | `/api/v7/space-types/{id}` | 删除类型 |
| PUT | `/api/v7/space-types/{id}/toggle` | 启用/禁用 |

### 空间管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v7/spaces` | 分页查询空间 |
| GET | `/api/v7/spaces/tree` | 获取空间树 |
| GET | `/api/v7/spaces/{id}` | 获取空间详情 |
| GET | `/api/v7/spaces/{parentId}/children` | 获取子空间 |
| POST | `/api/v7/spaces` | 创建空间 |
| PUT | `/api/v7/spaces/{id}` | 更新空间 |
| DELETE | `/api/v7/spaces/{id}` | 删除空间 |
| PUT | `/api/v7/spaces/{id}/status` | 变更状态 |

### 占用管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v7/spaces/{spaceId}/occupants` | 获取当前占用者 |
| POST | `/api/v7/spaces/{spaceId}/check-in` | 入住 |
| POST | `/api/v7/spaces/{spaceId}/check-out/{occupantId}` | 退出 |
| GET | `/api/v7/spaces/{spaceId}/occupancy-history` | 占用历史 |

### 预订管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v7/spaces/{spaceId}/bookings` | 获取预订列表 |
| POST | `/api/v7/spaces/{spaceId}/bookings` | 创建预订 |
| PUT | `/api/v7/bookings/{id}` | 更新预订 |
| DELETE | `/api/v7/bookings/{id}` | 取消预订 |
| GET | `/api/v7/spaces/{spaceId}/availability` | 查询可用时段 |

---

## 实施计划

### 第一阶段：数据库与领域层
1. 创建新数据库表（space_types, spaces, space_occupants, space_bookings）
2. 实现 SpaceType 领域实体
3. 实现 Space 聚合根
4. 实现仓储接口

### 第二阶段：基础设施层
1. 实现 PO 类
2. 实现 Mapper
3. 实现 Repository 实现类

### 第三阶段：应用层
1. 实现 SpaceTypeApplicationService
2. 实现 SpaceApplicationService
3. 实现 SpaceOccupancyService
4. 实现 SpaceBookingService

### 第四阶段：接口层
1. 实现 SpaceTypeController
2. 实现 SpaceController
3. 实现请求/响应 DTO

### 第五阶段：前端
1. 实现 API 调用层
2. 实现类型定义
3. 实现空间类型配置页面
4. 实现空间管理页面
5. 实现占用/预订组件

### 第六阶段：测试与清理
1. 功能测试
2. 删除旧版本代码
3. 数据库迁移脚本清理

---

## 删除清单（完成后）

### 后端删除
- `domain/space/model/valueobject/RoomType.java`
- `domain/space/model/valueobject/BuildingType.java`
- `domain/space/model/valueobject/GenderType.java`
- `domain/space/model/valueobject/OccupantType.java`
- 旧的 `space_type_config` 表相关代码

### 前端删除
- `types/space.ts` 中的硬编码枚举
- `ROOM_TYPE_CONFIG` 等硬编码配置

---

## 配置示例

### 学校场景

```json
[
  {"typeCode": "CAMPUS", "typeName": "校区", "isRootType": true, "allowedChildTypes": ["TEACHING_BUILDING", "DORM_BUILDING"]},
  {"typeCode": "TEACHING_BUILDING", "typeName": "教学楼", "allowedChildTypes": ["FLOOR"]},
  {"typeCode": "DORM_BUILDING", "typeName": "宿舍楼", "allowedChildTypes": ["FLOOR"]},
  {"typeCode": "FLOOR", "typeName": "楼层", "allowedChildTypes": ["CLASSROOM", "DORM_ROOM"]},
  {"typeCode": "CLASSROOM", "typeName": "教室", "hasCapacity": true, "capacityUnit": "人", "bookable": true},
  {"typeCode": "DORM_ROOM", "typeName": "宿舍", "hasCapacity": true, "capacityUnit": "床位", "occupiable": true, "assignable": true}
]
```

### 公司场景

```json
[
  {"typeCode": "PARK", "typeName": "园区", "isRootType": true, "allowedChildTypes": ["OFFICE_BUILDING"]},
  {"typeCode": "OFFICE_BUILDING", "typeName": "办公楼", "allowedChildTypes": ["FLOOR"]},
  {"typeCode": "FLOOR", "typeName": "楼层", "allowedChildTypes": ["OFFICE", "MEETING_ROOM", "WORKSTATION_AREA"]},
  {"typeCode": "OFFICE", "typeName": "办公室", "hasCapacity": true, "capacityUnit": "人", "assignable": true},
  {"typeCode": "MEETING_ROOM", "typeName": "会议室", "hasCapacity": true, "capacityUnit": "人", "bookable": true},
  {"typeCode": "WORKSTATION_AREA", "typeName": "工位区", "allowedChildTypes": ["WORKSTATION"]},
  {"typeCode": "WORKSTATION", "typeName": "工位", "bookable": true, "assignable": true}
]
```

### 医院场景

```json
[
  {"typeCode": "HOSPITAL", "typeName": "院区", "isRootType": true, "allowedChildTypes": ["OUTPATIENT", "INPATIENT"]},
  {"typeCode": "OUTPATIENT", "typeName": "门诊楼", "allowedChildTypes": ["FLOOR"]},
  {"typeCode": "INPATIENT", "typeName": "住院楼", "allowedChildTypes": ["FLOOR"]},
  {"typeCode": "FLOOR", "typeName": "楼层", "allowedChildTypes": ["CLINIC", "WARD", "OPERATING_ROOM"]},
  {"typeCode": "CLINIC", "typeName": "诊室", "bookable": true, "assignable": true},
  {"typeCode": "WARD", "typeName": "病房", "hasCapacity": true, "capacityUnit": "床位", "occupiable": true},
  {"typeCode": "OPERATING_ROOM", "typeName": "手术室", "bookable": true}
]
```
