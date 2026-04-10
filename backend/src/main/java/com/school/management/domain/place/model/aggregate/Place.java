package com.school.management.domain.place.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.place.model.valueobject.*;
import com.school.management.exception.BusinessException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 场所聚合根 — 通用多级场所树结构
 */
@Getter
@NoArgsConstructor
public class Place extends AggregateRoot<Long> {

    private Long id;
    private String placeCode;
    private String placeName;
    private PlaceType placeType;
    private Long categoryId;              // V10: 分类ID（楼栋分类/房间分类）
    private RoomType roomType;            // 保留兼容旧代码
    private BuildingType buildingType;    // 保留兼容旧代码

    // 楼号和房间号（V10: 改为数字类型）
    private Integer buildingNo;           // 楼号（数字）- BUILDING类型
    private Integer roomNo;               // 房间号（数字）- ROOM类型
    private Integer floorCount;           // 楼层数 - BUILDING类型

    // 层级关系
    private Long parentId;
    private PlacePath path;
    private Integer level;

    // 位置信息（冗余字段）
    private Long campusId;
    private Long buildingId;
    private Integer floorNumber;

    // 容量
    private Capacity capacity;

    // 归属
    private Long orgUnitId;
    private Long responsibleUserId;

    // 性别限制
    private GenderType genderType;

    // 状态
    private PlaceStatus status;

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
    public static Place createCampus(String code, String name) {
        Place place = new Place();
        place.placeCode = code;
        place.placeName = name;
        place.placeType = PlaceType.CAMPUS;
        place.level = 0;
        place.path = PlacePath.root();
        place.status = PlaceStatus.NORMAL;
        place.attributes = new HashMap<>();
        place.createdAt = LocalDateTime.now();
        return place;
    }

    /**
     * 创建楼宇（V10版本）
     */
    public static Place createBuilding(String code, String name, Long categoryId,
                                       Integer buildingNo, Integer floorCount, Place campus) {
        validateParent(campus, PlaceType.CAMPUS);

        Place place = new Place();
        place.placeCode = code;
        place.placeName = name;
        place.placeType = PlaceType.BUILDING;
        place.categoryId = categoryId;
        place.buildingNo = buildingNo;
        place.floorCount = floorCount;
        place.parentId = campus.getId();
        place.campusId = campus.getId();
        place.level = campus.getLevel() + 1;
        place.status = PlaceStatus.NORMAL;
        place.attributes = new HashMap<>();
        place.createdAt = LocalDateTime.now();
        return place;
    }

    /**
     * 创建楼宇（兼容旧版本）
     * @deprecated 请使用新版本的 createBuilding(code, name, categoryId, buildingNo, floorCount, campus)
     */
    @Deprecated
    public static Place createBuilding(String code, String name, BuildingType buildingType,
                                       String buildingNo, Place campus) {
        validateParent(campus, PlaceType.CAMPUS);

        Place place = new Place();
        place.placeCode = code;
        place.placeName = name;
        place.placeType = PlaceType.BUILDING;
        place.buildingType = buildingType;
        try {
            place.buildingNo = Integer.parseInt(buildingNo);
        } catch (NumberFormatException e) {
            place.buildingNo = null;
        }
        place.parentId = campus.getId();
        place.campusId = campus.getId();
        place.level = campus.getLevel() + 1;
        place.status = PlaceStatus.NORMAL;
        place.attributes = new HashMap<>();
        place.createdAt = LocalDateTime.now();
        return place;
    }

    /**
     * 创建楼层
     */
    public static Place createFloor(Integer floorNumber, Place building) {
        validateParent(building, PlaceType.BUILDING);

        Place place = new Place();
        place.placeCode = building.getPlaceCode() + "-F" + floorNumber;
        place.placeName = floorNumber + "层";
        place.placeType = PlaceType.FLOOR;
        place.floorNumber = floorNumber;
        place.parentId = building.getId();
        place.campusId = building.getCampusId();
        place.buildingId = building.getId();
        place.level = building.getLevel() + 1;
        place.status = PlaceStatus.NORMAL;
        place.attributes = new HashMap<>();
        place.createdAt = LocalDateTime.now();
        return place;
    }

    /**
     * 创建房间（V10版本）
     */
    public static Place createRoom(String code, String name, Long categoryId,
                                   Integer roomNo, Integer capacity, GenderType genderType,
                                   Place floor) {
        validateParent(floor, PlaceType.FLOOR);

        Place place = new Place();
        place.placeCode = code;
        place.placeName = name;
        place.placeType = PlaceType.ROOM;
        place.categoryId = categoryId;
        place.roomNo = roomNo;
        place.floorNumber = floor.getFloorNumber();
        place.parentId = floor.getId();
        place.campusId = floor.getCampusId();
        place.buildingId = floor.getBuildingId();
        place.level = floor.getLevel() + 1;
        place.capacity = capacity != null ? Capacity.of(capacity) : Capacity.empty();
        place.genderType = genderType != null ? genderType : GenderType.MIXED;
        place.status = PlaceStatus.NORMAL;
        place.attributes = new HashMap<>();
        place.createdAt = LocalDateTime.now();
        return place;
    }

    /**
     * 创建房间（兼容旧版本）
     * @deprecated 请使用新版本的 createRoom(code, name, categoryId, roomNo, capacity, genderType, floor)
     */
    @Deprecated
    public static Place createRoom(String code, String name, RoomType roomType,
                                   Integer capacity, String roomNo, Place floor) {
        validateParent(floor, PlaceType.FLOOR);

        Place place = new Place();
        place.placeCode = code;
        place.placeName = name;
        place.placeType = PlaceType.ROOM;
        place.roomType = roomType;
        try {
            place.roomNo = Integer.parseInt(roomNo);
        } catch (NumberFormatException e) {
            place.roomNo = null;
        }
        place.floorNumber = floor.getFloorNumber();
        place.parentId = floor.getId();
        place.campusId = floor.getCampusId();
        place.buildingId = floor.getBuildingId();
        place.level = floor.getLevel() + 1;
        place.capacity = capacity != null ? Capacity.of(capacity) : Capacity.empty();
        place.status = PlaceStatus.NORMAL;
        place.attributes = new HashMap<>();
        place.createdAt = LocalDateTime.now();
        return place;
    }

    /**
     * 从持久化数据重建（V10版本）
     */
    public static Place reconstitute(Long id, String placeCode, String placeName,
                                     PlaceType placeType, Long categoryId,
                                     RoomType roomType, BuildingType buildingType,
                                     Integer buildingNo, Integer roomNo, Integer floorCount,
                                     Long parentId, String path, Integer level,
                                     Long campusId, Long buildingId, Integer floorNumber,
                                     Integer maxCapacity, Integer currentOccupancy,
                                     Long orgUnitId, Long responsibleUserId,
                                     GenderType genderType,
                                     PlaceStatus status, Map<String, Object> attributes, String description,
                                     Long createdBy, LocalDateTime createdAt,
                                     Long updatedBy, LocalDateTime updatedAt) {
        Place place = new Place();
        place.id = id;
        place.placeCode = placeCode;
        place.placeName = placeName;
        place.placeType = placeType;
        place.categoryId = categoryId;
        place.roomType = roomType;
        place.buildingType = buildingType;
        place.buildingNo = buildingNo;
        place.roomNo = roomNo;
        place.floorCount = floorCount;
        place.parentId = parentId;
        place.path = PlacePath.of(path);
        place.level = level;
        place.campusId = campusId;
        place.buildingId = buildingId;
        place.floorNumber = floorNumber;
        place.capacity = Capacity.of(maxCapacity, currentOccupancy);
        place.orgUnitId = orgUnitId;
        place.responsibleUserId = responsibleUserId;
        place.genderType = genderType != null ? genderType : GenderType.MIXED;
        place.status = status;
        place.attributes = attributes != null ? attributes : new HashMap<>();
        place.description = description;
        place.createdBy = createdBy;
        place.createdAt = createdAt;
        place.updatedBy = updatedBy;
        place.updatedAt = updatedAt;
        return place;
    }

    /**
     * 从持久化数据重建（兼容旧版本）
     * @deprecated 请使用新版本的 reconstitute
     */
    @Deprecated
    public static Place reconstitute(Long id, String placeCode, String placeName,
                                     PlaceType placeType, RoomType roomType, BuildingType buildingType,
                                     String buildingNo, String roomNo,
                                     Long parentId, String path, Integer level,
                                     Long campusId, Long buildingId, Integer floorNumber,
                                     Integer maxCapacity, Integer currentOccupancy,
                                     Long orgUnitId, Long responsibleUserId,
                                     GenderType genderType,
                                     PlaceStatus status, Map<String, Object> attributes, String description,
                                     Long createdBy, LocalDateTime createdAt,
                                     Long updatedBy, LocalDateTime updatedAt) {
        Place place = new Place();
        place.id = id;
        place.placeCode = placeCode;
        place.placeName = placeName;
        place.placeType = placeType;
        place.roomType = roomType;
        place.buildingType = buildingType;
        try {
            place.buildingNo = buildingNo != null ? Integer.parseInt(buildingNo) : null;
        } catch (NumberFormatException e) {
            place.buildingNo = null;
        }
        try {
            place.roomNo = roomNo != null ? Integer.parseInt(roomNo) : null;
        } catch (NumberFormatException e) {
            place.roomNo = null;
        }
        place.parentId = parentId;
        place.path = PlacePath.of(path);
        place.level = level;
        place.campusId = campusId;
        place.buildingId = buildingId;
        place.floorNumber = floorNumber;
        place.capacity = Capacity.of(maxCapacity, currentOccupancy);
        place.orgUnitId = orgUnitId;
        place.responsibleUserId = responsibleUserId;
        place.genderType = genderType != null ? genderType : GenderType.MIXED;
        place.status = status;
        place.attributes = attributes != null ? attributes : new HashMap<>();
        place.description = description;
        place.createdBy = createdBy;
        place.createdAt = createdAt;
        place.updatedBy = updatedBy;
        place.updatedAt = updatedAt;
        return place;
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
    public void updatePath(PlacePath newPath) {
        this.path = newPath;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新基本信息
     */
    public void updateInfo(String name, String description) {
        this.placeName = name;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新楼号（仅BUILDING类型）
     */
    public void updateBuildingNo(Integer buildingNo) {
        if (this.placeType == PlaceType.BUILDING) {
            this.buildingNo = buildingNo;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 更新楼层数（仅BUILDING类型）
     */
    public void updateFloorCount(Integer floorCount) {
        if (this.placeType == PlaceType.BUILDING) {
            this.floorCount = floorCount;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 更新房间号（仅ROOM类型）
     */
    public void updateRoomNo(Integer roomNo) {
        if (this.placeType == PlaceType.ROOM) {
            this.roomNo = roomNo;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 更新分类（楼栋或房间）
     */
    public void updateCategory(Long categoryId) {
        if (this.placeType == PlaceType.BUILDING || this.placeType == PlaceType.ROOM) {
            this.categoryId = categoryId;
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
        if (this.status == PlaceStatus.DISABLED) {
            this.status = PlaceStatus.NORMAL;
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
        this.status = PlaceStatus.DISABLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 开始维修
     */
    public void startMaintenance() {
        this.status = PlaceStatus.MAINTENANCE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 完成维修
     */
    public void completeMaintenance() {
        if (this.status == PlaceStatus.MAINTENANCE) {
            this.status = PlaceStatus.NORMAL;
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
        return this.status == PlaceStatus.NORMAL
            && this.placeType == PlaceType.ROOM
            && this.capacity != null
            && !this.capacity.isFull();
    }

    /**
     * 是否是房间
     */
    public boolean isRoom() {
        return this.placeType == PlaceType.ROOM;
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
        return this.orgUnitId != null;
    }

    /**
     * 是否限制性别
     */
    public boolean hasGenderRestriction() {
        return this.genderType != null && this.genderType.isRestricted();
    }

    /**
     * 检查性别是否匹配场所限制
     */
    public boolean matchesGender(String gender) {
        if (this.genderType == null || this.genderType == GenderType.MIXED) return true;
        return this.genderType.matchesGender(gender);
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

    private static void validateParent(Place parent, PlaceType expectedType) {
        if (parent == null) {
            throw new BusinessException("父级场所不能为空");
        }
        if (parent.getId() == null) {
            throw new BusinessException("父级场所必须先保存");
        }
        if (parent.getPlaceType() != expectedType) {
            throw new BusinessException("父级场所类型不正确，期望: " + expectedType.getDescription());
        }
    }

    private void validateCheckIn() {
        if (this.placeType != PlaceType.ROOM) {
            throw new BusinessException("只有房间类型场所才能入住");
        }
        if (this.status != PlaceStatus.NORMAL) {
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
