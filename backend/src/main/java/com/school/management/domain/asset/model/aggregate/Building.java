package com.school.management.domain.asset.model.aggregate;

import com.school.management.domain.asset.event.BuildingCreatedEvent;
import com.school.management.domain.asset.event.BuildingUpdatedEvent;
import com.school.management.domain.asset.model.valueobject.BuildingType;
import com.school.management.domain.shared.AggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 楼宇聚合根
 */
@Getter
@Setter
public class Building extends AggregateRoot<Long> {

    private String buildingNo;
    private String buildingName;
    private BuildingType buildingType;
    private Integer totalFloors;
    private String location;
    private Integer constructionYear;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Building() {}

    /**
     * 创建楼宇
     */
    public static Building create(String buildingNo, String buildingName, BuildingType buildingType,
                                   Integer totalFloors, String location) {
        Building building = new Building();
        building.buildingNo = buildingNo;
        building.buildingName = buildingName;
        building.buildingType = buildingType;
        building.totalFloors = totalFloors;
        building.location = location;
        building.status = 1; // 默认启用
        building.createdAt = LocalDateTime.now();
        building.updatedAt = LocalDateTime.now();

        building.registerEvent(new BuildingCreatedEvent(
                null, // ID will be set after save
                buildingNo,
                buildingName,
                buildingType
        ));

        return building;
    }

    /**
     * 从持久化重建
     */
    public static Building reconstruct(Long id, String buildingNo, String buildingName,
                                        BuildingType buildingType, Integer totalFloors,
                                        String location, Integer constructionYear,
                                        String description, Integer status,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        Building building = new Building();
        building.setId(id);
        building.buildingNo = buildingNo;
        building.buildingName = buildingName;
        building.buildingType = buildingType;
        building.totalFloors = totalFloors;
        building.location = location;
        building.constructionYear = constructionYear;
        building.description = description;
        building.status = status;
        building.createdAt = createdAt;
        building.updatedAt = updatedAt;
        return building;
    }

    /**
     * 更新楼宇信息
     */
    public void updateInfo(String buildingName, BuildingType buildingType,
                           Integer totalFloors, String location,
                           Integer constructionYear, String description) {
        this.buildingName = buildingName;
        this.buildingType = buildingType;
        this.totalFloors = totalFloors;
        this.location = location;
        this.constructionYear = constructionYear;
        this.description = description;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new BuildingUpdatedEvent(
                String.valueOf(getId()),
                buildingNo,
                buildingName
        ));
    }

    /**
     * 启用楼宇
     */
    public void enable() {
        this.status = 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 停用楼宇
     */
    public void disable() {
        this.status = 0;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 是否为宿舍楼
     */
    public boolean isDormitoryBuilding() {
        return buildingType != null && buildingType.isDormitory();
    }

    /**
     * 是否启用
     */
    public boolean isActive() {
        return status != null && status == 1;
    }
}
