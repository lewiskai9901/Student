package com.school.management.application.space.query;

import com.school.management.domain.space.model.valueobject.BuildingType;
import com.school.management.domain.space.model.valueobject.RoomType;
import com.school.management.domain.space.model.valueobject.SpaceStatus;
import com.school.management.domain.space.model.valueobject.SpaceType;
import lombok.Data;

/**
 * 场所查询条件
 */
@Data
public class SpaceQueryCriteria {

    private SpaceType spaceType;
    private RoomType roomType;
    private BuildingType buildingType;
    private Long buildingId;
    private Integer floorNumber;
    private Long orgUnitId;
    private SpaceStatus status;
    private String keyword;

    private int page = 1;
    private int pageSize = 20;

    public int getOffset() {
        return (page - 1) * pageSize;
    }
}
