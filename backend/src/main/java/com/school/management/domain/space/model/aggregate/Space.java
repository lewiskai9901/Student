package com.school.management.domain.space.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.space.model.valueobject.*;
import com.school.management.exception.BusinessException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 场所聚合根
 * 统一管理校区、楼宇、楼层、房间等各类场所
 */
@Getter
@NoArgsConstructor
public class Space extends AggregateRoot {

    private Long id;
    private String spaceCode;
    private String spaceName;
    private SpaceType spaceType;
    private RoomType roomType;
    private BuildingType buildingType;

    // 楼号和房间号
    private String buildingNo;      // 楼号（如 1, A, 甲）- BUILDING类型
    private String roomNo;          // 房间号（如 101, 302）- ROOM类型

    // 层级关系
    private Long parentId;
    private SpacePath path;
    private Integer level;

    // 位置信息（冗余字段）
    private Long campusId;
    private Long buildingId;
    private Integer floorNumber;

    // 容量
    private Capacity capacity;

    // 归属
    private Long orgUnitId;
    private Long classId;              // 归属班级ID（班主任管理）
    private Long responsibleUserId;

    // 性别限制
    private GenderType genderType;

    // 状态
    private SpaceStatus status;

    // 扩展属性
    private Map<String, Object> attributes;

    private String description;

    // 审计
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    // ========== 工厂方法 ==========

    /**
     * 创建校区
     */
    public static Space createCampus(String code, String name) {
        Space space = new Space();
        space.spaceCode = code;
        space.spaceName = name;
        space.spaceType = SpaceType.CAMPUS;
        space.level = 0;
        space.path = SpacePath.root();
        space.status = SpaceStatus.NORMAL;
        space.attributes = new HashMap<>();
        space.createdAt = LocalDateTime.now();
        return space;
    }

    /**
     * 创建楼宇
     */
    public static Space createBuilding(String code, String name, BuildingType buildingType,
                                       String buildingNo, Space campus) {
        validateParent(campus, SpaceType.CAMPUS);

        Space space = new Space();
        space.spaceCode = code;
        space.spaceName = name;
        space.spaceType = SpaceType.BUILDING;
        space.buildingType = buildingType;
        space.buildingNo = buildingNo;
        space.parentId = campus.getId();
        space.campusId = campus.getId();
        space.level = campus.getLevel() + 1;
        space.status = SpaceStatus.NORMAL;
        space.attributes = new HashMap<>();
        space.createdAt = LocalDateTime.now();
        return space;
    }

    /**
     * 创建楼层
     */
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
        space.attributes = new HashMap<>();
        space.createdAt = LocalDateTime.now();
        return space;
    }

    /**
     * 创建房间
     */
    public static Space createRoom(String code, String name, RoomType roomType,
                                   Integer capacity, String roomNo, Space floor) {
        validateParent(floor, SpaceType.FLOOR);

        Space space = new Space();
        space.spaceCode = code;
        space.spaceName = name;
        space.spaceType = SpaceType.ROOM;
        space.roomType = roomType;
        space.roomNo = roomNo;
        space.floorNumber = floor.getFloorNumber();
        space.parentId = floor.getId();
        space.campusId = floor.getCampusId();
        space.buildingId = floor.getBuildingId();
        space.level = floor.getLevel() + 1;
        space.capacity = capacity != null ? Capacity.of(capacity) : Capacity.empty();
        space.status = SpaceStatus.NORMAL;
        space.attributes = new HashMap<>();
        space.createdAt = LocalDateTime.now();
        return space;
    }

    /**
     * 从持久化数据重建
     */
    public static Space reconstitute(Long id, String spaceCode, String spaceName,
                                     SpaceType spaceType, RoomType roomType, BuildingType buildingType,
                                     String buildingNo, String roomNo,
                                     Long parentId, String path, Integer level,
                                     Long campusId, Long buildingId, Integer floorNumber,
                                     Integer maxCapacity, Integer currentOccupancy,
                                     Long orgUnitId, Long classId, Long responsibleUserId,
                                     GenderType genderType,
                                     SpaceStatus status, Map<String, Object> attributes, String description,
                                     Long createdBy, LocalDateTime createdAt,
                                     Long updatedBy, LocalDateTime updatedAt) {
        Space space = new Space();
        space.id = id;
        space.spaceCode = spaceCode;
        space.spaceName = spaceName;
        space.spaceType = spaceType;
        space.roomType = roomType;
        space.buildingType = buildingType;
        space.buildingNo = buildingNo;
        space.roomNo = roomNo;
        space.parentId = parentId;
        space.path = SpacePath.of(path);
        space.level = level;
        space.campusId = campusId;
        space.buildingId = buildingId;
        space.floorNumber = floorNumber;
        space.capacity = Capacity.of(maxCapacity, currentOccupancy);
        space.orgUnitId = orgUnitId;
        space.classId = classId;
        space.responsibleUserId = responsibleUserId;
        space.genderType = genderType != null ? genderType : GenderType.MIXED;
        space.status = status;
        space.attributes = attributes != null ? attributes : new HashMap<>();
        space.description = description;
        space.createdBy = createdBy;
        space.createdAt = createdAt;
        space.updatedBy = updatedBy;
        space.updatedAt = updatedAt;
        return space;
    }

    // ========== 业务方法 ==========

    /**
     * 设置ID（保存后回填）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 更新路径
     */
    public void updatePath(SpacePath newPath) {
        this.path = newPath;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新基本信息
     */
    public void updateInfo(String name, String description) {
        this.spaceName = name;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新楼号（仅BUILDING类型）
     */
    public void updateBuildingNo(String buildingNo) {
        if (this.spaceType == SpaceType.BUILDING) {
            this.buildingNo = buildingNo;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 更新房间号（仅ROOM类型）
     */
    public void updateRoomNo(String roomNo) {
        if (this.spaceType == SpaceType.ROOM) {
            this.roomNo = roomNo;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 更新扩展属性
     */
    public void updateAttributes(Map<String, Object> attributes) {
        if (attributes != null) {
            this.attributes = new HashMap<>(attributes);
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新容量
     */
    public void updateCapacity(Integer newCapacity) {
        if (this.capacity == null) {
            this.capacity = Capacity.of(newCapacity);
        } else {
            this.capacity = this.capacity.updateMax(newCapacity);
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 分配给组织单元（部门）
     */
    public void assignToOrgUnit(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 分配给班级
     */
    public void assignToClass(Long classId) {
        this.classId = classId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消班级分配
     */
    public void unassignFromClass() {
        this.classId = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置性别类型限制
     */
    public void setGenderRestriction(GenderType genderType) {
        this.genderType = genderType != null ? genderType : GenderType.MIXED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 指定负责人
     */
    public void assignResponsible(Long userId) {
        this.responsibleUserId = userId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 启用
     */
    public void enable() {
        if (this.status == SpaceStatus.DISABLED) {
            this.status = SpaceStatus.NORMAL;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 停用
     */
    public void disable() {
        if (this.capacity != null && !this.capacity.isEmpty()) {
            throw new BusinessException("场所有占用者，无法停用");
        }
        this.status = SpaceStatus.DISABLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 开始维修
     */
    public void startMaintenance() {
        this.status = SpaceStatus.MAINTENANCE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 完成维修
     */
    public void completeMaintenance() {
        if (this.status == SpaceStatus.MAINTENANCE) {
            this.status = SpaceStatus.NORMAL;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 入住/占用
     */
    public void checkIn() {
        validateCheckIn();
        this.capacity = this.capacity.increment();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 退出
     */
    public void checkOut() {
        if (this.capacity == null || this.capacity.isEmpty()) {
            throw new BusinessException("当前无人入住");
        }
        this.capacity = this.capacity.decrement();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置创建人
     */
    public void setCreatedBy(Long userId) {
        this.createdBy = userId;
    }

    /**
     * 设置更新人
     */
    public void setUpdatedBy(Long userId) {
        this.updatedBy = userId;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 查询方法 ==========

    /**
     * 是否可入住
     */
    public boolean canCheckIn() {
        return this.status == SpaceStatus.NORMAL
            && this.spaceType == SpaceType.ROOM
            && this.capacity != null
            && !this.capacity.isFull();
    }

    /**
     * 是否是房间
     */
    public boolean isRoom() {
        return this.spaceType == SpaceType.ROOM;
    }

    /**
     * 是否是宿舍
     */
    public boolean isDormitory() {
        return isRoom() && this.roomType != null && this.roomType.isDormitory();
    }

    /**
     * 是否是教室
     */
    public boolean isClassroom() {
        return isRoom() && this.roomType != null && this.roomType.isClassroom();
    }

    /**
     * 是否是实验室
     */
    public boolean isLab() {
        return isRoom() && this.roomType != null && this.roomType.isLab();
    }

    /**
     * 是否是办公室
     */
    public boolean isOffice() {
        return isRoom() && this.roomType != null && this.roomType.isOffice();
    }

    /**
     * 是否已分配给班级
     */
    public boolean isAssignedToClass() {
        return this.classId != null;
    }

    /**
     * 是否限制性别
     */
    public boolean hasGenderRestriction() {
        return this.genderType != null && this.genderType.isRestricted();
    }

    /**
     * 检查学生性别是否匹配
     * @param studentGender 学生性别 ("男"/"女")
     * @return 是否匹配
     */
    public boolean matchesStudentGender(String studentGender) {
        if (this.genderType == null || this.genderType == GenderType.MIXED) {
            return true;
        }
        return this.genderType.matchesStudentGender(studentGender);
    }

    /**
     * 获取入住率
     */
    public double getOccupancyRate() {
        return this.capacity != null ? this.capacity.getOccupancyRate() : 0;
    }

    /**
     * 获取可用容量
     */
    public int getAvailableCapacity() {
        return this.capacity != null ? this.capacity.getAvailable() : 0;
    }

    /**
     * 获取当前占用数
     */
    public int getCurrentOccupancy() {
        return this.capacity != null && this.capacity.getCurrentOccupancy() != null
            ? this.capacity.getCurrentOccupancy() : 0;
    }

    /**
     * 获取最大容量
     */
    public Integer getMaxCapacity() {
        return this.capacity != null ? this.capacity.getMaxCapacity() : null;
    }

    // ========== 私有方法 ==========

    private static void validateParent(Space parent, SpaceType expectedType) {
        if (parent == null) {
            throw new BusinessException("父级场所不能为空");
        }
        if (parent.getId() == null) {
            throw new BusinessException("父级场所必须先保存");
        }
        if (parent.getSpaceType() != expectedType) {
            throw new BusinessException("父级场所类型不正确，期望: " + expectedType.getDescription());
        }
    }

    private void validateCheckIn() {
        if (this.spaceType != SpaceType.ROOM) {
            throw new BusinessException("只有房间类型场所才能入住");
        }
        if (this.status != SpaceStatus.NORMAL) {
            throw new BusinessException("场所状态不可用");
        }
        if (this.capacity == null) {
            throw new BusinessException("该场所不支持入住");
        }
        if (this.capacity.isFull()) {
            throw new BusinessException("场所已满员");
        }
    }
}
