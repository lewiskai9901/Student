# 统一场所管理系统设计方案

> 基于业界最佳实践的场所管理系统重构方案，支持宿舍、教室、实训室、办公室等多种场所类型的统一管理

## 1. 设计目标

### 1.1 核心理念

- **统一抽象**：所有场所（楼宇、宿舍、教室、办公室等）都是「空间(Space)」的实例
- **树形层级**：校区 → 楼宇 → 楼层 → 房间，支持无限层级扩展
- **类型可配置**：房间类型从硬编码改为可配置，新增类型无需改代码
- **扩展属性分离**：通用字段在主表，特定类型字段在扩展表
- **资产直接关联**：资产直接关联 Space.id，无需维护 LocationType 枚举

### 1.2 支持的场所类型

| 层级类型 | 房间类型 | 说明 |
|---------|---------|------|
| CAMPUS | - | 校区 |
| BUILDING | - | 楼宇 |
| FLOOR | - | 楼层 |
| ROOM | DORMITORY | 宿舍 |
| ROOM | CLASSROOM | 普通教室 |
| ROOM | MULTIMEDIA | 多媒体教室 |
| ROOM | LAB | 实验室 |
| ROOM | TRAINING | 实训室 |
| ROOM | OFFICE | 办公室 |
| ROOM | MEETING | 会议室 |
| ROOM | LIBRARY | 图书馆/阅览室 |
| ROOM | STORAGE | 仓库 |
| ROOM | UTILITY | 功能房(配电室/卫生间) |

### 1.3 层级结构示例

```
学校 (CAMPUS)
├── A教学楼 (BUILDING, buildingType=TEACHING)
│   ├── 1F (FLOOR)
│   │   ├── A101 (ROOM, roomType=CLASSROOM)
│   │   ├── A102 (ROOM, roomType=MULTIMEDIA)
│   │   └── A103 (ROOM, roomType=LAB)
│   └── 2F (FLOOR)
│       ├── A201 (ROOM, roomType=CLASSROOM)
│       └── A202 (ROOM, roomType=TRAINING)
├── 1号宿舍楼 (BUILDING, buildingType=DORMITORY)
│   ├── 1F (FLOOR)
│   │   ├── 101 (ROOM, roomType=DORMITORY)
│   │   ├── 102 (ROOM, roomType=DORMITORY)
│   │   └── 配电室 (ROOM, roomType=UTILITY)
│   └── 2F (FLOOR)
│       └── ...
└── 行政楼 (BUILDING, buildingType=OFFICE)
    └── 1F (FLOOR)
        ├── 校长办公室 (ROOM, roomType=OFFICE)
        ├── 会议室A (ROOM, roomType=MEETING)
        └── 档案室 (ROOM, roomType=STORAGE)
```

---

## 2. 数据库设计

### 2.1 核心表结构

```sql
-- ============================================================
-- 1. 场所类型配置表（支持动态扩展房间类型）
-- ============================================================
CREATE TABLE space_type_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_code VARCHAR(32) NOT NULL UNIQUE COMMENT '类型编码',
    type_name VARCHAR(64) NOT NULL COMMENT '类型名称',
    type_category VARCHAR(32) NOT NULL COMMENT '类型分类: BUILDING/ROOM',
    icon VARCHAR(64) COMMENT '图标名称(lucide图标)',
    color VARCHAR(16) COMMENT '主题色(tailwind类名)',
    has_capacity TINYINT DEFAULT 1 COMMENT '是否有容量概念',
    has_occupancy TINYINT DEFAULT 0 COMMENT '是否有入住/使用人员',
    has_gender TINYINT DEFAULT 0 COMMENT '是否区分性别',
    default_capacity INT COMMENT '默认容量',
    attribute_schema JSON COMMENT '扩展属性JSON Schema',
    sort_order INT DEFAULT 0 COMMENT '排序',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '场所类型配置表';

-- ============================================================
-- 2. 场所主表（统一抽象）
-- ============================================================
CREATE TABLE space (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基本信息
    space_code VARCHAR(64) NOT NULL COMMENT '场所编码(唯一)',
    space_name VARCHAR(128) NOT NULL COMMENT '场所名称',
    space_type VARCHAR(32) NOT NULL COMMENT '场所类型: CAMPUS/BUILDING/FLOOR/ROOM',
    room_type VARCHAR(32) COMMENT '房间类型(仅ROOM有): DORMITORY/CLASSROOM/OFFICE...',
    building_type VARCHAR(32) COMMENT '楼宇类型(仅BUILDING有): TEACHING/DORMITORY/OFFICE/MIXED',

    -- 层级关系
    parent_id BIGINT COMMENT '父级ID',
    path VARCHAR(512) COMMENT '物化路径: /1/2/3/',
    level INT NOT NULL DEFAULT 0 COMMENT '层级深度(0=根)',

    -- 位置信息（冗余字段，加速查询）
    campus_id BIGINT COMMENT '所属校区ID',
    building_id BIGINT COMMENT '所属楼宇ID',
    floor_number INT COMMENT '楼层号',

    -- 容量信息
    capacity INT COMMENT '容量/座位数/床位数',
    current_occupancy INT DEFAULT 0 COMMENT '当前占用数',

    -- 归属信息
    org_unit_id BIGINT COMMENT '所属组织单元',
    responsible_user_id BIGINT COMMENT '负责人ID',

    -- 状态
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0停用 1正常 2维修中',

    -- 扩展属性（JSON格式，存储类型特有的属性）
    attributes JSON COMMENT '扩展属性',

    -- 描述
    description VARCHAR(512) COMMENT '描述/备注',

    -- 审计字段
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    -- 唯一约束
    UNIQUE KEY uk_space_code (space_code),
    UNIQUE KEY uk_parent_name (parent_id, space_name, deleted),

    -- 索引
    INDEX idx_parent_id (parent_id),
    INDEX idx_space_type (space_type),
    INDEX idx_room_type (room_type),
    INDEX idx_building_type (building_type),
    INDEX idx_building_id (building_id),
    INDEX idx_floor_number (floor_number),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_status (status),
    INDEX idx_path (path(255))
) COMMENT '场所统一表';

-- ============================================================
-- 3. 宿舍扩展表（宿舍特有属性）
-- ============================================================
CREATE TABLE space_dormitory_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL UNIQUE COMMENT '场所ID',

    -- 宿舍特有属性
    gender_type TINYINT NOT NULL DEFAULT 1 COMMENT '性别类型: 1男 2女 3混合',
    bed_count INT COMMENT '床位数(可能与capacity不同)',
    facilities VARCHAR(255) COMMENT '设施配置(空调/热水器/独卫等)',

    -- 分配信息
    assigned_class_ids VARCHAR(512) COMMENT '指定班级ID列表(逗号分隔)',
    supervisor_id BIGINT COMMENT '宿管员ID',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_space_id (space_id),
    INDEX idx_gender_type (gender_type)
) COMMENT '宿舍扩展属性表';

-- ============================================================
-- 4. 教室扩展表（教室特有属性）
-- ============================================================
CREATE TABLE space_classroom_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL UNIQUE COMMENT '场所ID',

    -- 教室特有属性
    classroom_category VARCHAR(32) COMMENT '教室分类: NORMAL/MULTIMEDIA/SMART',
    assigned_class_id BIGINT COMMENT '固定使用班级ID',
    has_projector TINYINT DEFAULT 0 COMMENT '是否有投影仪',
    has_air_conditioner TINYINT DEFAULT 0 COMMENT '是否有空调',
    has_computer TINYINT DEFAULT 0 COMMENT '是否有电脑',
    equipment_info VARCHAR(255) COMMENT '设备配置说明',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_space_id (space_id),
    INDEX idx_assigned_class_id (assigned_class_id)
) COMMENT '教室扩展属性表';

-- ============================================================
-- 5. 实验室/实训室扩展表
-- ============================================================
CREATE TABLE space_lab_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL UNIQUE COMMENT '场所ID',

    -- 实验室特有属性
    lab_category VARCHAR(32) COMMENT '实验室分类: PHYSICS/CHEMISTRY/COMPUTER/TRAINING',
    safety_level TINYINT DEFAULT 1 COMMENT '安全等级: 1普通 2中等 3高',
    major_id BIGINT COMMENT '所属专业ID',
    equipment_list TEXT COMMENT '设备清单(JSON)',
    safety_notice TEXT COMMENT '安全须知',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_space_id (space_id)
) COMMENT '实验室/实训室扩展属性表';

-- ============================================================
-- 6. 办公室扩展表
-- ============================================================
CREATE TABLE space_office_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL UNIQUE COMMENT '场所ID',

    -- 办公室特有属性
    office_type VARCHAR(32) COMMENT '办公室类型: PRIVATE/SHARED/OPEN',
    department_id BIGINT COMMENT '所属部门ID',
    workstation_count INT COMMENT '工位数量',
    phone_number VARCHAR(32) COMMENT '办公电话',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_space_id (space_id),
    INDEX idx_department_id (department_id)
) COMMENT '办公室扩展属性表';

-- ============================================================
-- 7. 学生-宿舍分配表（替代原 student_dormitory）
-- ============================================================
CREATE TABLE space_occupant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL COMMENT '场所ID(宿舍)',
    occupant_type VARCHAR(32) NOT NULL COMMENT '占用者类型: STUDENT/TEACHER/STAFF',
    occupant_id BIGINT NOT NULL COMMENT '占用者ID',

    -- 床位/工位信息
    position_no INT COMMENT '位置编号(床位号/工位号)',

    -- 时间信息
    check_in_date DATE COMMENT '入住日期',
    check_out_date DATE COMMENT '退出日期',

    -- 状态
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0已退出 1在住',

    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_space_position (space_id, position_no, status),
    INDEX idx_space_id (space_id),
    INDEX idx_occupant (occupant_type, occupant_id),
    INDEX idx_status (status)
) COMMENT '场所占用表(学生入住/工位分配)';

-- ============================================================
-- 8. 场所-组织单元分配表（支持按楼层分配）
-- ============================================================
CREATE TABLE space_org_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL COMMENT '场所ID(通常是楼宇)',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',

    -- 范围限定（可选）
    floor_start INT COMMENT '起始楼层',
    floor_end INT COMMENT '结束楼层',

    -- 分配类型
    assignment_type VARCHAR(32) DEFAULT 'EXCLUSIVE' COMMENT '分配类型: EXCLUSIVE独占/SHARED共享',

    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0无效 1有效',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_space_org (space_id, org_unit_id),
    INDEX idx_space_id (space_id),
    INDEX idx_org_unit_id (org_unit_id)
) COMMENT '场所-组织单元分配表';

-- ============================================================
-- 9. 修改资产表，关联到统一的space
-- ============================================================
-- 资产表增加 space_id 字段（替代原来的 location_type + location_id）
ALTER TABLE asset
    ADD COLUMN space_id BIGINT COMMENT '场所ID',
    ADD INDEX idx_space_id (space_id);

-- 保留原字段用于过渡，后续可删除
-- location_type, location_id, location_name 暂时保留
```

### 2.2 初始化数据

```sql
-- ============================================================
-- 初始化场所类型配置
-- ============================================================
INSERT INTO space_type_config (type_code, type_name, type_category, icon, color, has_capacity, has_occupancy, has_gender, default_capacity, sort_order) VALUES
-- 楼宇类型
('TEACHING', '教学楼', 'BUILDING', 'GraduationCap', 'blue', 0, 0, 0, NULL, 1),
('DORMITORY_BUILDING', '宿舍楼', 'BUILDING', 'Home', 'teal', 0, 0, 0, NULL, 2),
('OFFICE_BUILDING', '办公楼', 'BUILDING', 'Building2', 'gray', 0, 0, 0, NULL, 3),
('MIXED', '综合楼', 'BUILDING', 'Layers', 'purple', 0, 0, 0, NULL, 4),

-- 房间类型
('DORMITORY', '学生宿舍', 'ROOM', 'BedDouble', 'teal', 1, 1, 1, 6, 10),
('STAFF_DORMITORY', '教职工宿舍', 'ROOM', 'Bed', 'cyan', 1, 1, 0, 2, 11),
('CLASSROOM', '普通教室', 'ROOM', 'School', 'blue', 1, 0, 0, 50, 20),
('MULTIMEDIA', '多媒体教室', 'ROOM', 'Monitor', 'indigo', 1, 0, 0, 60, 21),
('SMART_CLASSROOM', '智慧教室', 'ROOM', 'Cpu', 'violet', 1, 0, 0, 40, 22),
('LAB', '实验室', 'ROOM', 'FlaskConical', 'amber', 1, 0, 0, 30, 30),
('COMPUTER_LAB', '计算机房', 'ROOM', 'Monitor', 'sky', 1, 0, 0, 50, 31),
('TRAINING', '实训室', 'ROOM', 'Wrench', 'orange', 1, 0, 0, 40, 32),
('OFFICE', '办公室', 'ROOM', 'Briefcase', 'slate', 1, 1, 0, 4, 40),
('MEETING', '会议室', 'ROOM', 'Users', 'emerald', 1, 0, 0, 20, 41),
('LIBRARY', '图书馆/阅览室', 'ROOM', 'BookOpen', 'amber', 1, 0, 0, 100, 50),
('STORAGE', '仓库', 'ROOM', 'Package', 'stone', 0, 0, 0, NULL, 60),
('UTILITY', '功能房', 'ROOM', 'Settings', 'zinc', 0, 0, 0, NULL, 70),
('BATHROOM', '卫生间', 'ROOM', 'Bath', 'gray', 0, 0, 1, NULL, 71),
('POWER_ROOM', '配电室', 'ROOM', 'Zap', 'yellow', 0, 0, 0, NULL, 72);

-- ============================================================
-- 初始化默认校区
-- ============================================================
INSERT INTO space (space_code, space_name, space_type, parent_id, path, level, status) VALUES
('CAMPUS_MAIN', '主校区', 'CAMPUS', NULL, '/1/', 0, 1);
```

---

## 3. 后端架构设计

### 3.1 领域模型结构

```
backend/src/main/java/com/school/management/
├── domain/
│   └── space/                              # Space领域
│       ├── model/
│       │   ├── aggregate/
│       │   │   └── Space.java              # 场所聚合根
│       │   ├── entity/
│       │   │   ├── SpaceOccupant.java      # 占用者实体
│       │   │   └── SpaceOrgAssignment.java # 组织分配实体
│       │   └── valueobject/
│       │       ├── SpaceType.java          # 场所类型(CAMPUS/BUILDING/FLOOR/ROOM)
│       │       ├── RoomType.java           # 房间类型枚举
│       │       ├── BuildingType.java       # 楼宇类型枚举
│       │       ├── SpacePath.java          # 物化路径值对象
│       │       ├── SpaceStatus.java        # 状态值对象
│       │       ├── GenderType.java         # 性别类型
│       │       ├── Capacity.java           # 容量值对象
│       │       └── OccupantType.java       # 占用者类型
│       ├── repository/
│       │   ├── SpaceRepository.java        # 场所仓储接口
│       │   └── SpaceOccupantRepository.java
│       ├── service/
│       │   └── SpaceDomainService.java     # 领域服务
│       └── event/
│           ├── SpaceCreatedEvent.java
│           ├── SpaceStatusChangedEvent.java
│           ├── OccupantCheckedInEvent.java
│           └── OccupantCheckedOutEvent.java
│
├── application/
│   └── space/                              # 应用层
│       ├── SpaceApplicationService.java    # 场所应用服务
│       ├── SpaceQueryService.java          # 查询服务
│       ├── SpaceOccupantService.java       # 占用管理服务
│       ├── command/
│       │   ├── CreateSpaceCommand.java
│       │   ├── UpdateSpaceCommand.java
│       │   ├── ChangeSpaceStatusCommand.java
│       │   ├── CheckInOccupantCommand.java
│       │   └── CheckOutOccupantCommand.java
│       └── query/
│           ├── SpaceDTO.java
│           ├── SpaceTreeDTO.java
│           ├── SpaceDetailDTO.java
│           ├── SpaceOccupantDTO.java
│           ├── SpaceStatisticsDTO.java
│           └── SpaceQueryCriteria.java
│
├── infrastructure/
│   └── persistence/
│       └── space/                          # 持久化实现
│           ├── SpaceMapper.java            # MyBatis Mapper
│           ├── SpaceOccupantMapper.java
│           ├── SpacePO.java                # 持久化对象
│           ├── SpaceOccupantPO.java
│           ├── SpaceRepositoryImpl.java
│           └── SpaceOccupantRepositoryImpl.java
│
└── interfaces/
    └── rest/
        └── space/                          # REST接口
            ├── SpaceController.java        # 场所管理API
            ├── SpaceOccupantController.java # 占用管理API
            ├── SpaceTypeConfigController.java # 类型配置API
            └── dto/
                ├── CreateSpaceRequest.java
                ├── UpdateSpaceRequest.java
                ├── SpaceResponse.java
                ├── SpaceTreeResponse.java
                ├── CheckInRequest.java
                └── BatchUpdateOrgRequest.java
```

### 3.2 聚合根设计

```java
// domain/space/model/aggregate/Space.java

@Getter
public class Space extends AggregateRoot {

    private Long id;
    private String spaceCode;
    private String spaceName;
    private SpaceType spaceType;        // CAMPUS, BUILDING, FLOOR, ROOM
    private RoomType roomType;          // 仅ROOM有效
    private BuildingType buildingType;  // 仅BUILDING有效

    // 层级关系
    private Long parentId;
    private SpacePath path;             // 物化路径值对象
    private Integer level;

    // 位置信息（冗余）
    private Long campusId;
    private Long buildingId;
    private Integer floorNumber;

    // 容量
    private Capacity capacity;

    // 归属
    private Long orgUnitId;
    private Long responsibleUserId;

    // 状态
    private SpaceStatus status;

    // 扩展属性
    private Map<String, Object> attributes;

    private String description;

    // ========== 工厂方法 ==========

    public static Space createCampus(String code, String name) {
        Space space = new Space();
        space.spaceCode = code;
        space.spaceName = name;
        space.spaceType = SpaceType.CAMPUS;
        space.level = 0;
        space.path = SpacePath.root();
        space.status = SpaceStatus.NORMAL;
        space.registerEvent(new SpaceCreatedEvent(space));
        return space;
    }

    public static Space createBuilding(String code, String name, BuildingType buildingType, Space parent) {
        validateParent(parent, SpaceType.CAMPUS);
        Space space = new Space();
        space.spaceCode = code;
        space.spaceName = name;
        space.spaceType = SpaceType.BUILDING;
        space.buildingType = buildingType;
        space.parentId = parent.getId();
        space.campusId = parent.getId();
        space.level = parent.getLevel() + 1;
        space.status = SpaceStatus.NORMAL;
        space.registerEvent(new SpaceCreatedEvent(space));
        return space;
    }

    public static Space createFloor(Integer floorNumber, Space building) {
        validateParent(building, SpaceType.BUILDING);
        Space space = new Space();
        space.spaceCode = building.getSpaceCode() + "-F" + floorNumber;
        space.spaceName = floorNumber + "层";
        space.spaceType = SpaceType.FLOOR;
        space.floorNumber = floorNumber;
        space.parentId = building.getId();
        space.campusId = building.getCampusId();
        space.buildingId = building.getId();
        space.level = building.getLevel() + 1;
        space.status = SpaceStatus.NORMAL;
        space.registerEvent(new SpaceCreatedEvent(space));
        return space;
    }

    public static Space createRoom(String code, String name, RoomType roomType,
                                   Integer capacity, Space floor) {
        validateParent(floor, SpaceType.FLOOR);
        Space space = new Space();
        space.spaceCode = code;
        space.spaceName = name;
        space.spaceType = SpaceType.ROOM;
        space.roomType = roomType;
        space.floorNumber = floor.getFloorNumber();
        space.parentId = floor.getId();
        space.campusId = floor.getCampusId();
        space.buildingId = floor.getBuildingId();
        space.level = floor.getLevel() + 1;
        space.capacity = Capacity.of(capacity);
        space.status = SpaceStatus.NORMAL;
        space.registerEvent(new SpaceCreatedEvent(space));
        return space;
    }

    // ========== 业务方法 ==========

    public void updatePath(SpacePath newPath) {
        this.path = newPath;
    }

    public void updateInfo(String name, String description, Map<String, Object> attributes) {
        this.spaceName = name;
        this.description = description;
        if (attributes != null) {
            this.attributes = attributes;
        }
    }

    public void assignToOrgUnit(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public void assignResponsible(Long userId) {
        this.responsibleUserId = userId;
    }

    public void enable() {
        if (this.status == SpaceStatus.DISABLED) {
            this.status = SpaceStatus.NORMAL;
            registerEvent(new SpaceStatusChangedEvent(this, SpaceStatus.DISABLED, SpaceStatus.NORMAL));
        }
    }

    public void disable() {
        // 检查是否有占用者
        if (this.capacity != null && this.capacity.getCurrentOccupancy() > 0) {
            throw new BusinessException("场所有占用者，无法停用");
        }
        this.status = SpaceStatus.DISABLED;
        registerEvent(new SpaceStatusChangedEvent(this, this.status, SpaceStatus.DISABLED));
    }

    public void startMaintenance() {
        this.status = SpaceStatus.MAINTENANCE;
        registerEvent(new SpaceStatusChangedEvent(this, this.status, SpaceStatus.MAINTENANCE));
    }

    public void checkIn() {
        if (this.capacity == null) {
            throw new BusinessException("该场所不支持入住");
        }
        this.capacity = this.capacity.increment();
    }

    public void checkOut() {
        if (this.capacity == null || this.capacity.getCurrentOccupancy() <= 0) {
            throw new BusinessException("当前无人入住");
        }
        this.capacity = this.capacity.decrement();
    }

    public boolean canCheckIn() {
        return this.status == SpaceStatus.NORMAL
            && this.capacity != null
            && !this.capacity.isFull();
    }

    public boolean isRoom() {
        return this.spaceType == SpaceType.ROOM;
    }

    public boolean isDormitory() {
        return isRoom() && this.roomType == RoomType.DORMITORY;
    }

    public boolean isClassroom() {
        return isRoom() && (this.roomType == RoomType.CLASSROOM
            || this.roomType == RoomType.MULTIMEDIA
            || this.roomType == RoomType.SMART_CLASSROOM);
    }

    // ========== 私有方法 ==========

    private static void validateParent(Space parent, SpaceType expectedType) {
        if (parent == null || parent.getSpaceType() != expectedType) {
            throw new BusinessException("父级场所类型不正确");
        }
    }
}
```

### 3.3 值对象设计

```java
// SpacePath.java - 物化路径值对象
@Value
public class SpacePath {
    String value;

    public static SpacePath root() {
        return new SpacePath("/");
    }

    public static SpacePath of(String path) {
        return new SpacePath(path);
    }

    public SpacePath append(Long id) {
        return new SpacePath(this.value + id + "/");
    }

    public boolean isAncestorOf(SpacePath other) {
        return other.value.startsWith(this.value);
    }

    public List<Long> getAncestorIds() {
        return Arrays.stream(value.split("/"))
            .filter(s -> !s.isEmpty())
            .map(Long::parseLong)
            .collect(Collectors.toList());
    }
}

// Capacity.java - 容量值对象
@Value
public class Capacity {
    Integer maxCapacity;
    Integer currentOccupancy;

    public static Capacity of(Integer max) {
        return new Capacity(max, 0);
    }

    public static Capacity of(Integer max, Integer current) {
        return new Capacity(max, current);
    }

    public Capacity increment() {
        if (isFull()) {
            throw new BusinessException("容量已满");
        }
        return new Capacity(maxCapacity, currentOccupancy + 1);
    }

    public Capacity decrement() {
        if (currentOccupancy <= 0) {
            throw new BusinessException("当前无占用");
        }
        return new Capacity(maxCapacity, currentOccupancy - 1);
    }

    public boolean isFull() {
        return maxCapacity != null && currentOccupancy >= maxCapacity;
    }

    public int getAvailable() {
        return maxCapacity != null ? maxCapacity - currentOccupancy : 0;
    }

    public double getOccupancyRate() {
        if (maxCapacity == null || maxCapacity == 0) return 0;
        return (double) currentOccupancy / maxCapacity * 100;
    }
}

// SpaceType.java
public enum SpaceType {
    CAMPUS("校区"),
    BUILDING("楼宇"),
    FLOOR("楼层"),
    ROOM("房间");

    private final String description;

    SpaceType(String description) {
        this.description = description;
    }
}

// RoomType.java
public enum RoomType {
    DORMITORY("学生宿舍", true, true),
    STAFF_DORMITORY("教职工宿舍", true, false),
    CLASSROOM("普通教室", false, false),
    MULTIMEDIA("多媒体教室", false, false),
    SMART_CLASSROOM("智慧教室", false, false),
    LAB("实验室", false, false),
    COMPUTER_LAB("计算机房", false, false),
    TRAINING("实训室", false, false),
    OFFICE("办公室", true, false),
    MEETING("会议室", false, false),
    LIBRARY("图书馆", false, false),
    STORAGE("仓库", false, false),
    UTILITY("功能房", false, false),
    BATHROOM("卫生间", false, true),
    POWER_ROOM("配电室", false, false);

    private final String description;
    private final boolean hasOccupancy;  // 是否有入住/使用人员
    private final boolean hasGender;     // 是否区分性别

    RoomType(String description, boolean hasOccupancy, boolean hasGender) {
        this.description = description;
        this.hasOccupancy = hasOccupancy;
        this.hasGender = hasGender;
    }
}

// BuildingType.java
public enum BuildingType {
    TEACHING("教学楼"),
    DORMITORY("宿舍楼"),
    OFFICE("办公楼"),
    MIXED("综合楼");

    private final String description;

    BuildingType(String description) {
        this.description = description;
    }
}

// SpaceStatus.java
public enum SpaceStatus {
    DISABLED(0, "停用"),
    NORMAL(1, "正常"),
    MAINTENANCE(2, "维修中");

    private final int code;
    private final String description;

    SpaceStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

### 3.4 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| **场所管理** | | |
| GET | `/api/v2/spaces/tree` | 获取场所树 |
| GET | `/api/v2/spaces` | 场所列表(分页/筛选) |
| GET | `/api/v2/spaces/{id}` | 场所详情 |
| POST | `/api/v2/spaces` | 创建场所 |
| PUT | `/api/v2/spaces/{id}` | 更新场所 |
| DELETE | `/api/v2/spaces/{id}` | 删除场所 |
| PUT | `/api/v2/spaces/{id}/status` | 变更状态 |
| GET | `/api/v2/spaces/{id}/children` | 获取子级 |
| GET | `/api/v2/spaces/{id}/ancestors` | 获取祖先链 |
| **按类型查询** | | |
| GET | `/api/v2/spaces/buildings` | 楼宇列表 |
| GET | `/api/v2/spaces/buildings/{id}/floors` | 楼宇的楼层 |
| GET | `/api/v2/spaces/buildings/{id}/rooms` | 楼宇的所有房间 |
| GET | `/api/v2/spaces/rooms` | 房间列表(可按roomType筛选) |
| GET | `/api/v2/spaces/dormitories` | 宿舍列表(roomType=DORMITORY) |
| GET | `/api/v2/spaces/classrooms` | 教室列表(多种roomType) |
| **占用管理** | | |
| GET | `/api/v2/spaces/{id}/occupants` | 场所占用者列表 |
| POST | `/api/v2/spaces/{id}/check-in` | 入住/分配 |
| POST | `/api/v2/spaces/{id}/check-out` | 退出 |
| POST | `/api/v2/spaces/{id}/swap` | 交换位置 |
| **组织分配** | | |
| GET | `/api/v2/spaces/{id}/org-assignments` | 组织分配列表 |
| POST | `/api/v2/spaces/{id}/org-assignments` | 添加组织分配 |
| DELETE | `/api/v2/spaces/{id}/org-assignments/{orgId}` | 移除组织分配 |
| POST | `/api/v2/spaces/batch-assign-org` | 批量分配组织 |
| **统计** | | |
| GET | `/api/v2/spaces/statistics` | 整体统计 |
| GET | `/api/v2/spaces/{id}/statistics` | 单个场所统计 |
| **类型配置** | | |
| GET | `/api/v2/space-types` | 类型配置列表 |
| POST | `/api/v2/space-types` | 创建类型 |
| PUT | `/api/v2/space-types/{id}` | 更新类型 |

---

## 4. 前端架构设计

### 4.1 目录结构

```
frontend/src/
├── api/v2/
│   └── space.ts                        # 场所API
├── types/v2/
│   └── space.ts                        # 类型定义
├── stores/
│   └── space.ts                        # 场所状态管理
└── views/space/
    ├── SpaceManagementCenter.vue       # 场所管理中心(主页面)
    ├── components/
    │   ├── SpaceTree.vue               # 左侧树形导航
    │   ├── SpaceList.vue               # 房间列表视图
    │   ├── SpaceCard.vue               # 场所卡片
    │   ├── SpaceFormDialog.vue         # 创建/编辑对话框
    │   ├── SpaceControlPanel.vue       # 右侧详情控制面板
    │   ├── SpaceStatistics.vue         # 统计面板
    │   ├── OccupantList.vue            # 占用者列表
    │   ├── OccupantAssignDialog.vue    # 分配对话框
    │   ├── OrgAssignmentPanel.vue      # 组织分配面板
    │   └── extensions/                 # 类型专属扩展组件
    │       ├── DormitoryExtension.vue  # 宿舍扩展(床位管理)
    │       ├── ClassroomExtension.vue  # 教室扩展(班级关联)
    │       ├── LabExtension.vue        # 实验室扩展
    │       └── OfficeExtension.vue     # 办公室扩展
    └── views/                          # 快捷入口视图
        ├── DormitoryQuickView.vue      # 宿舍快捷视图
        ├── ClassroomQuickView.vue      # 教室快捷视图
        └── LabQuickView.vue            # 实验室快捷视图
```

### 4.2 TypeScript 类型定义

```typescript
// types/v2/space.ts

// ========== 枚举类型 ==========

export enum SpaceType {
  CAMPUS = 'CAMPUS',
  BUILDING = 'BUILDING',
  FLOOR = 'FLOOR',
  ROOM = 'ROOM'
}

export enum BuildingType {
  TEACHING = 'TEACHING',
  DORMITORY = 'DORMITORY',
  OFFICE = 'OFFICE',
  MIXED = 'MIXED'
}

export enum RoomType {
  DORMITORY = 'DORMITORY',
  STAFF_DORMITORY = 'STAFF_DORMITORY',
  CLASSROOM = 'CLASSROOM',
  MULTIMEDIA = 'MULTIMEDIA',
  SMART_CLASSROOM = 'SMART_CLASSROOM',
  LAB = 'LAB',
  COMPUTER_LAB = 'COMPUTER_LAB',
  TRAINING = 'TRAINING',
  OFFICE = 'OFFICE',
  MEETING = 'MEETING',
  LIBRARY = 'LIBRARY',
  STORAGE = 'STORAGE',
  UTILITY = 'UTILITY',
  BATHROOM = 'BATHROOM',
  POWER_ROOM = 'POWER_ROOM'
}

export enum SpaceStatus {
  DISABLED = 0,
  NORMAL = 1,
  MAINTENANCE = 2
}

export enum GenderType {
  MALE = 1,
  FEMALE = 2,
  MIXED = 3
}

export enum OccupantType {
  STUDENT = 'STUDENT',
  TEACHER = 'TEACHER',
  STAFF = 'STAFF'
}

// ========== 显示映射 ==========

export const SpaceTypeMap: Record<SpaceType, string> = {
  [SpaceType.CAMPUS]: '校区',
  [SpaceType.BUILDING]: '楼宇',
  [SpaceType.FLOOR]: '楼层',
  [SpaceType.ROOM]: '房间'
}

export const BuildingTypeMap: Record<BuildingType, string> = {
  [BuildingType.TEACHING]: '教学楼',
  [BuildingType.DORMITORY]: '宿舍楼',
  [BuildingType.OFFICE]: '办公楼',
  [BuildingType.MIXED]: '综合楼'
}

export const RoomTypeMap: Record<RoomType, string> = {
  [RoomType.DORMITORY]: '学生宿舍',
  [RoomType.STAFF_DORMITORY]: '教职工宿舍',
  [RoomType.CLASSROOM]: '普通教室',
  [RoomType.MULTIMEDIA]: '多媒体教室',
  [RoomType.SMART_CLASSROOM]: '智慧教室',
  [RoomType.LAB]: '实验室',
  [RoomType.COMPUTER_LAB]: '计算机房',
  [RoomType.TRAINING]: '实训室',
  [RoomType.OFFICE]: '办公室',
  [RoomType.MEETING]: '会议室',
  [RoomType.LIBRARY]: '图书馆',
  [RoomType.STORAGE]: '仓库',
  [RoomType.UTILITY]: '功能房',
  [RoomType.BATHROOM]: '卫生间',
  [RoomType.POWER_ROOM]: '配电室'
}

export const SpaceStatusMap: Record<SpaceStatus, string> = {
  [SpaceStatus.DISABLED]: '停用',
  [SpaceStatus.NORMAL]: '正常',
  [SpaceStatus.MAINTENANCE]: '维修中'
}

export const GenderTypeMap: Record<GenderType, string> = {
  [GenderType.MALE]: '男',
  [GenderType.FEMALE]: '女',
  [GenderType.MIXED]: '混合'
}

// ========== 图标和颜色映射 ==========

export const RoomTypeIconMap: Record<RoomType, string> = {
  [RoomType.DORMITORY]: 'BedDouble',
  [RoomType.STAFF_DORMITORY]: 'Bed',
  [RoomType.CLASSROOM]: 'School',
  [RoomType.MULTIMEDIA]: 'Monitor',
  [RoomType.SMART_CLASSROOM]: 'Cpu',
  [RoomType.LAB]: 'FlaskConical',
  [RoomType.COMPUTER_LAB]: 'Monitor',
  [RoomType.TRAINING]: 'Wrench',
  [RoomType.OFFICE]: 'Briefcase',
  [RoomType.MEETING]: 'Users',
  [RoomType.LIBRARY]: 'BookOpen',
  [RoomType.STORAGE]: 'Package',
  [RoomType.UTILITY]: 'Settings',
  [RoomType.BATHROOM]: 'Bath',
  [RoomType.POWER_ROOM]: 'Zap'
}

export const RoomTypeColorMap: Record<RoomType, string> = {
  [RoomType.DORMITORY]: 'teal',
  [RoomType.STAFF_DORMITORY]: 'cyan',
  [RoomType.CLASSROOM]: 'blue',
  [RoomType.MULTIMEDIA]: 'indigo',
  [RoomType.SMART_CLASSROOM]: 'violet',
  [RoomType.LAB]: 'amber',
  [RoomType.COMPUTER_LAB]: 'sky',
  [RoomType.TRAINING]: 'orange',
  [RoomType.OFFICE]: 'slate',
  [RoomType.MEETING]: 'emerald',
  [RoomType.LIBRARY]: 'amber',
  [RoomType.STORAGE]: 'stone',
  [RoomType.UTILITY]: 'zinc',
  [RoomType.BATHROOM]: 'gray',
  [RoomType.POWER_ROOM]: 'yellow'
}

// ========== 接口定义 ==========

export interface Space {
  id: number
  spaceCode: string
  spaceName: string
  spaceType: SpaceType
  roomType?: RoomType
  buildingType?: BuildingType

  // 层级
  parentId?: number
  parentName?: string
  path: string
  level: number

  // 位置
  campusId?: number
  campusName?: string
  buildingId?: number
  buildingName?: string
  floorNumber?: number

  // 容量
  capacity?: number
  currentOccupancy?: number

  // 归属
  orgUnitId?: number
  orgUnitName?: string
  responsibleUserId?: number
  responsibleUserName?: string

  // 状态
  status: SpaceStatus

  // 扩展
  attributes?: Record<string, any>
  description?: string

  // 树形结构
  children?: Space[]

  // 扩展属性（根据类型动态加载）
  dormitoryExt?: DormitoryExtension
  classroomExt?: ClassroomExtension
  labExt?: LabExtension
  officeExt?: OfficeExtension
}

export interface DormitoryExtension {
  genderType: GenderType
  bedCount?: number
  facilities?: string
  assignedClassIds?: string
  assignedClassNames?: string
  supervisorId?: number
  supervisorName?: string
}

export interface ClassroomExtension {
  classroomCategory?: string
  assignedClassId?: number
  assignedClassName?: string
  hasProjector?: boolean
  hasAirConditioner?: boolean
  hasComputer?: boolean
  equipmentInfo?: string
}

export interface LabExtension {
  labCategory?: string
  safetyLevel?: number
  majorId?: number
  majorName?: string
  equipmentList?: string[]
  safetyNotice?: string
}

export interface OfficeExtension {
  officeType?: string
  departmentId?: number
  departmentName?: string
  workstationCount?: number
  phoneNumber?: string
}

// 占用者
export interface SpaceOccupant {
  id: number
  spaceId: number
  occupantType: OccupantType
  occupantId: number
  occupantName?: string
  occupantNo?: string  // 学号/工号
  positionNo?: number
  checkInDate?: string
  checkOutDate?: string
  status: number
  remark?: string
}

// 场所树节点
export interface SpaceTreeNode extends Space {
  children?: SpaceTreeNode[]
  isLeaf?: boolean
  statistics?: {
    totalRooms: number
    totalCapacity: number
    totalOccupancy: number
    occupancyRate: number
  }
}

// 统计数据
export interface SpaceStatistics {
  totalBuildings: number
  totalRooms: number
  totalCapacity: number
  totalOccupancy: number
  occupancyRate: number
  byRoomType: {
    roomType: RoomType
    count: number
    capacity: number
    occupancy: number
  }[]
  byBuildingType: {
    buildingType: BuildingType
    count: number
    roomCount: number
  }[]
}

// ========== 请求/响应类型 ==========

export interface CreateSpaceRequest {
  spaceCode?: string  // 可选，不填则自动生成
  spaceName: string
  spaceType: SpaceType
  roomType?: RoomType
  buildingType?: BuildingType
  parentId?: number
  floorNumber?: number
  capacity?: number
  orgUnitId?: number
  responsibleUserId?: number
  description?: string
  attributes?: Record<string, any>

  // 扩展属性
  dormitoryExt?: Partial<DormitoryExtension>
  classroomExt?: Partial<ClassroomExtension>
  labExt?: Partial<LabExtension>
  officeExt?: Partial<OfficeExtension>
}

export interface UpdateSpaceRequest {
  spaceName?: string
  capacity?: number
  orgUnitId?: number
  responsibleUserId?: number
  description?: string
  attributes?: Record<string, any>

  // 扩展属性
  dormitoryExt?: Partial<DormitoryExtension>
  classroomExt?: Partial<ClassroomExtension>
  labExt?: Partial<LabExtension>
  officeExt?: Partial<OfficeExtension>
}

export interface SpaceQueryParams {
  keyword?: string
  spaceType?: SpaceType
  roomType?: RoomType | RoomType[]
  buildingType?: BuildingType
  buildingId?: number
  floorNumber?: number
  orgUnitId?: number
  status?: SpaceStatus
  parentId?: number
  page?: number
  pageSize?: number
}

export interface CheckInRequest {
  occupantType: OccupantType
  occupantId: number
  positionNo?: number
  checkInDate?: string
  remark?: string
}

export interface BatchAssignOrgRequest {
  spaceIds: number[]
  orgUnitId: number | null  // null表示清除分配
  floorStart?: number
  floorEnd?: number
}

// 类型配置
export interface SpaceTypeConfig {
  id: number
  typeCode: string
  typeName: string
  typeCategory: 'BUILDING' | 'ROOM'
  icon?: string
  color?: string
  hasCapacity: boolean
  hasOccupancy: boolean
  hasGender: boolean
  defaultCapacity?: number
  attributeSchema?: Record<string, any>
  sortOrder: number
  enabled: boolean
}
```

### 4.3 API 模块

```typescript
// api/v2/space.ts

import request from '@/utils/request'
import type {
  Space,
  SpaceTreeNode,
  SpaceOccupant,
  SpaceStatistics,
  SpaceTypeConfig,
  CreateSpaceRequest,
  UpdateSpaceRequest,
  SpaceQueryParams,
  CheckInRequest,
  BatchAssignOrgRequest
} from '@/types/v2/space'

const BASE_URL = '/v2/spaces'

export const spaceApi = {
  // ========== 场所管理 ==========

  // 获取场所树
  getTree: (params?: { buildingType?: string; includeStatistics?: boolean }) =>
    request.get<SpaceTreeNode[]>(`${BASE_URL}/tree`, { params }),

  // 场所列表
  getList: (params: SpaceQueryParams) =>
    request.get<{ records: Space[]; total: number }>(`${BASE_URL}`, { params }),

  // 场所详情
  getById: (id: number) =>
    request.get<Space>(`${BASE_URL}/${id}`),

  // 创建场所
  create: (data: CreateSpaceRequest) =>
    request.post<number>(`${BASE_URL}`, data),

  // 更新场所
  update: (id: number, data: UpdateSpaceRequest) =>
    request.put(`${BASE_URL}/${id}`, data),

  // 删除场所
  delete: (id: number, force?: boolean) =>
    request.delete(`${BASE_URL}/${id}`, { params: { force } }),

  // 变更状态
  changeStatus: (id: number, status: number) =>
    request.put(`${BASE_URL}/${id}/status`, { status }),

  // 获取子级
  getChildren: (id: number) =>
    request.get<Space[]>(`${BASE_URL}/${id}/children`),

  // 获取祖先链
  getAncestors: (id: number) =>
    request.get<Space[]>(`${BASE_URL}/${id}/ancestors`),

  // ========== 快捷查询 ==========

  // 楼宇列表
  getBuildings: (params?: { buildingType?: string; status?: number }) =>
    request.get<Space[]>(`${BASE_URL}/buildings`, { params }),

  // 楼宇的楼层
  getBuildingFloors: (buildingId: number) =>
    request.get<Space[]>(`${BASE_URL}/buildings/${buildingId}/floors`),

  // 楼宇的所有房间
  getBuildingRooms: (buildingId: number, params?: { roomType?: string; floorNumber?: number }) =>
    request.get<Space[]>(`${BASE_URL}/buildings/${buildingId}/rooms`, { params }),

  // 房间列表
  getRooms: (params: SpaceQueryParams) =>
    request.get<{ records: Space[]; total: number }>(`${BASE_URL}/rooms`, { params }),

  // 宿舍列表（快捷方法）
  getDormitories: (params?: Omit<SpaceQueryParams, 'roomType'>) =>
    request.get<{ records: Space[]; total: number }>(`${BASE_URL}/dormitories`, { params }),

  // 教室列表（快捷方法）
  getClassrooms: (params?: Omit<SpaceQueryParams, 'roomType'>) =>
    request.get<{ records: Space[]; total: number }>(`${BASE_URL}/classrooms`, { params }),

  // ========== 占用管理 ==========

  // 获取占用者列表
  getOccupants: (spaceId: number) =>
    request.get<SpaceOccupant[]>(`${BASE_URL}/${spaceId}/occupants`),

  // 入住/分配
  checkIn: (spaceId: number, data: CheckInRequest) =>
    request.post(`${BASE_URL}/${spaceId}/check-in`, data),

  // 退出
  checkOut: (spaceId: number, occupantId: number) =>
    request.post(`${BASE_URL}/${spaceId}/check-out`, { occupantId }),

  // 交换位置
  swap: (spaceId: number, data: { occupantAId: number; occupantBId: number }) =>
    request.post(`${BASE_URL}/${spaceId}/swap`, data),

  // ========== 组织分配 ==========

  // 获取组织分配
  getOrgAssignments: (spaceId: number) =>
    request.get(`${BASE_URL}/${spaceId}/org-assignments`),

  // 添加组织分配
  addOrgAssignment: (spaceId: number, data: { orgUnitId: number; floorStart?: number; floorEnd?: number }) =>
    request.post(`${BASE_URL}/${spaceId}/org-assignments`, data),

  // 移除组织分配
  removeOrgAssignment: (spaceId: number, orgUnitId: number) =>
    request.delete(`${BASE_URL}/${spaceId}/org-assignments/${orgUnitId}`),

  // 批量分配组织
  batchAssignOrg: (data: BatchAssignOrgRequest) =>
    request.post(`${BASE_URL}/batch-assign-org`, data),

  // ========== 统计 ==========

  // 整体统计
  getStatistics: (params?: { buildingId?: number; buildingType?: string }) =>
    request.get<SpaceStatistics>(`${BASE_URL}/statistics`, { params }),

  // 单个场所统计
  getSpaceStatistics: (id: number) =>
    request.get<SpaceStatistics>(`${BASE_URL}/${id}/statistics`),
}

// ========== 类型配置 API ==========

export const spaceTypeApi = {
  getList: () =>
    request.get<SpaceTypeConfig[]>('/v2/space-types'),

  create: (data: Partial<SpaceTypeConfig>) =>
    request.post<number>('/v2/space-types', data),

  update: (id: number, data: Partial<SpaceTypeConfig>) =>
    request.put(`/v2/space-types/${id}`, data),

  delete: (id: number) =>
    request.delete(`/v2/space-types/${id}`),
}
```

### 4.4 页面布局设计

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 场所管理中心                                        [新增楼宇] [新增房间] │
├──────────────────────┬──────────────────────────────────────────────────┤
│ 左侧导航树 (w-72)     │  右侧内容区                                      │
│                      │                                                  │
│ 🔍 搜索场所...       │  ┌──────────────────────────────────────────┐   │
│                      │  │ 统计面板 (可折叠)                          │   │
│ 📍 主校区            │  │ ┌────┬────┬────┬────┐                    │   │
│  ├─ 🏫 A教学楼       │  │ │楼宇│房间│容量│入住率│                    │   │
│  │   ├─ 1F          │  │ │ 5 │120│2400│ 68% │                    │   │
│  │   │  ├─ A101 教室│  │ └────┴────┴────┴────┘                    │   │
│  │   │  └─ A102 实验│  └──────────────────────────────────────────┘   │
│  │   └─ 2F          │                                                  │
│  │      └─ ...      │  ┌──────────────────────────────────────────┐   │
│  ├─ 🏠 1号宿舍楼     │  │ 筛选区                                     │   │
│  │   ├─ 1F          │  │ [类型▼] [楼宇▼] [楼层▼] [状态▼] 🔍搜索...  │   │
│  │   │  ├─ 101 宿舍 │  └──────────────────────────────────────────┘   │
│  │   │  └─ 102 宿舍 │                                                  │
│  │   └─ 2F          │  ┌──────────────────────────────────────────┐   │
│  │      └─ ...      │  │ 房间网格/列表视图                          │   │
│  └─ 🏢 行政楼       │  │                                            │   │
│      └─ 1F          │  │ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │   │
│         ├─ 校长办公室│  │ │ 101  │ │ 102  │ │ 103  │ │ 104  │      │   │
│         └─ 会议室A   │  │ │ 4/6  │ │ 6/6  │ │ 空置 │ │ 2/6  │      │   │
│                      │  │ │ 🟢   │ │ 🟡   │ │ ⚪   │ │ 🟢   │      │   │
│ ─────────────────── │  │ └──────┘ └──────┘ └──────┘ └──────┘      │   │
│ 快捷入口:            │  │                                            │   │
│ [宿舍] [教室] [实验室]│  └──────────────────────────────────────────┘   │
└──────────────────────┴──────────────────────────────────────────────────┘

点击房间卡片后展开控制面板:
┌──────────────────────────────────────────────────────────────────────────┐
│                                                      控制面板 (w-[560px]) │
│ ┌──────────────────────────────────────────────────────────────────────┐ │
│ │ 头部: 101宿舍 · A教学楼 · 1F                                [关闭] │ │
│ ├──────────────────────────────────────────────────────────────────────┤ │
│ │ [床位管理] [场所信息] [关联资产] [入住历史]                         │ │
│ ├──────────────────────────────────────────────────────────────────────┤ │
│ │                                                                      │ │
│ │ 入住状态: 4/6人  入住率: 66%                                        │ │
│ │ ████████████░░░░                                                    │ │
│ │                                                                      │ │
│ │ 床位网格:                                                           │ │
│ │ ┌─────┐ ┌─────┐ ┌─────┐                                            │ │
│ │ │ ① │ │ ② │ │ ③ │                                            │ │
│ │ │张三 │ │李四 │ │ 空 │                                            │ │
│ │ └─────┘ └─────┘ └─────┘                                            │ │
│ │ ┌─────┐ ┌─────┐ ┌─────┐                                            │ │
│ │ │ ④ │ │ ⑤ │ │ ⑥ │                                            │ │
│ │ │王五 │ │赵六 │ │ 空 │                                            │ │
│ │ └─────┘ └─────┘ └─────┘                                            │ │
│ │                                                                      │ │
│ └──────────────────────────────────────────────────────────────────────┘ │
│ [刷新]                                                          [关闭] │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 5. 路由配置

```typescript
// router/index.ts

// ==================== 场所管理 /space (order: 3) - Space领域 ====================
{
  path: '/space',
  name: 'Space',
  redirect: '/space/center',
  meta: {
    title: '场所管理',
    icon: 'Building2',
    requiresAuth: true,
    order: 3,
    group: 'space'
  },
  children: [
    {
      path: '/space/center',
      name: 'SpaceManagementCenter',
      component: () => import('@/views/space/SpaceManagementCenter.vue'),
      meta: {
        title: '场所管理中心',
        requiresAuth: true,
        permission: 'space:list',
        order: 1
      }
    },
    // 快捷入口（实际跳转到管理中心并带筛选参数）
    {
      path: '/space/dormitories',
      name: 'DormitoryQuickView',
      component: () => import('@/views/space/views/DormitoryQuickView.vue'),
      meta: {
        title: '宿舍管理',
        requiresAuth: true,
        permission: 'space:dormitory:list',
        order: 2
      }
    },
    {
      path: '/space/classrooms',
      name: 'ClassroomQuickView',
      component: () => import('@/views/space/views/ClassroomQuickView.vue'),
      meta: {
        title: '教室管理',
        requiresAuth: true,
        permission: 'space:classroom:list',
        order: 3
      }
    },
    {
      path: '/space/labs',
      name: 'LabQuickView',
      component: () => import('@/views/space/views/LabQuickView.vue'),
      meta: {
        title: '实验室管理',
        requiresAuth: true,
        permission: 'space:lab:list',
        order: 4
      }
    },
    {
      path: '/space/offices',
      name: 'OfficeQuickView',
      component: () => import('@/views/space/views/OfficeQuickView.vue'),
      meta: {
        title: '办公室管理',
        requiresAuth: true,
        permission: 'space:office:list',
        order: 5
      }
    },
    {
      path: '/space/types',
      name: 'SpaceTypeConfig',
      component: () => import('@/views/space/SpaceTypeConfigView.vue'),
      meta: {
        title: '类型配置',
        requiresAuth: true,
        permission: 'space:type:manage',
        order: 10
      }
    }
  ]
}

// 向后兼容重定向
{
  path: '/dormitory/:pathMatch(.*)*',
  redirect: to => ({ path: '/space/dormitories', query: to.query })
},
{
  path: '/organization/dormitory/:pathMatch(.*)*',
  redirect: to => ({ path: '/space/dormitories', query: to.query })
},
{
  path: '/organization/teaching/classrooms',
  redirect: '/space/classrooms'
}
```

---

## 6. 实现步骤

### 阶段一：数据库和后端基础（第1周）

1. **创建数据库迁移脚本**
   - `V20260122_2__unified_space_management.sql`
   - 包含所有表创建和初始化数据

2. **创建领域模型**
   - Space 聚合根
   - 所有值对象
   - 仓储接口

3. **创建基础设施层**
   - PO 对象
   - Mapper 接口和 XML
   - 仓储实现

4. **创建应用服务**
   - SpaceApplicationService
   - SpaceQueryService

5. **创建 REST 接口**
   - SpaceController
   - SpaceOccupantController

### 阶段二：前端基础（第2周）

1. **创建类型定义和 API**
   - `types/v2/space.ts`
   - `api/v2/space.ts`

2. **创建管理中心主页面**
   - SpaceManagementCenter.vue
   - SpaceTree.vue
   - SpaceList.vue

3. **创建通用组件**
   - SpaceCard.vue
   - SpaceFormDialog.vue
   - SpaceControlPanel.vue

4. **配置路由**

### 阶段三：类型扩展组件（第3周）

1. **宿舍扩展**
   - DormitoryExtension.vue
   - 床位管理逻辑
   - 学生分配功能

2. **教室扩展**
   - ClassroomExtension.vue
   - 班级关联功能

3. **实验室扩展**
   - LabExtension.vue
   - 设备管理

4. **办公室扩展**
   - OfficeExtension.vue
   - 工位管理

### 阶段四：集成和优化（第4周）

1. **资产模块集成**
   - 修改 Asset 关联到 Space
   - 更新 LocationSelector 使用 Space API

2. **快捷视图**
   - DormitoryQuickView
   - ClassroomQuickView
   - LabQuickView

3. **删除旧代码**
   - 删除旧的 Building/Dormitory/Classroom 相关代码
   - 清理旧的 API 和类型定义

4. **测试和文档**
   - 端到端测试
   - API 文档更新

---

## 7. 新增场所类型示例

未来如需新增「音乐教室」类型，只需：

### 7.1 数据库配置

```sql
-- 在 space_type_config 添加一条记录
INSERT INTO space_type_config (type_code, type_name, type_category, icon, color, has_capacity, sort_order)
VALUES ('MUSIC_ROOM', '音乐教室', 'ROOM', 'Music', 'pink', 1, 23);
```

### 7.2 前端类型（可选，如需TypeScript支持）

```typescript
// 在 RoomType 枚举添加
MUSIC_ROOM = 'MUSIC_ROOM'

// 在映射中添加
[RoomType.MUSIC_ROOM]: '音乐教室'
```

### 7.3 扩展组件（可选，如需专属UI）

```vue
<!-- MusicRoomExtension.vue -->
<template>
  <div class="space-y-4">
    <div class="grid grid-cols-2 gap-4">
      <div>
        <label>乐器配置</label>
        <el-input v-model="ext.instruments" />
      </div>
      <div>
        <label>隔音等级</label>
        <el-select v-model="ext.soundproofLevel">
          <el-option label="普通" :value="1" />
          <el-option label="专业" :value="2" />
        </el-select>
      </div>
    </div>
  </div>
</template>
```

**无需修改核心代码，系统自动支持新类型的 CRUD 操作！**

---

## 8. 总结

### 设计优势

| 维度 | 当前设计 | 新方案 | 提升 |
|------|---------|--------|------|
| 代码复用 | 3套独立代码 | 1套通用代码 | 70%↓ 代码量 |
| 新增类型 | 需要全套开发 | 仅需配置 | 95%↓ 工作量 |
| 维护成本 | 高 | 低 | 显著降低 |
| 扩展性 | 差 | 优秀 | 支持无限类型 |
| 数据一致性 | 多表分散 | 统一管理 | 提升 |

### 风险和注意事项

1. **数据清空**：需要清空现有宿舍分配数据
2. **前端改动较大**：建议分阶段上线
3. **向后兼容**：保留旧路由重定向，确保书签可用
4. **性能考虑**：树形查询需要索引优化

### 后续扩展方向

1. **楼层平面图编辑器**：可视化管理房间布局
2. **预约系统集成**：会议室、实验室预约
3. **物联网集成**：门禁、空调、照明控制
4. **数据大屏**：场所使用率实时监控
