package com.school.management.application.place.query;

import com.school.management.domain.place.model.valueobject.BuildingType;
import com.school.management.domain.place.model.valueobject.RoomType;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import com.school.management.domain.place.model.valueobject.PlaceType;
import lombok.Data;

/**
 * 场所查询条件
 */
@Data
public class PlaceQueryCriteria {

    private PlaceType placeType;
    private RoomType roomType;
    private BuildingType buildingType;
    private Long buildingId;
    private Integer floorNumber;
    private Long orgUnitId;
    private PlaceStatus status;
    private String keyword;

    private int page = 1;
    private int pageSize = 20;

    public int getOffset() {
        return (page - 1) * pageSize;
    }
}
