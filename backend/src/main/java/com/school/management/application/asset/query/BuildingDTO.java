package com.school.management.application.asset.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 楼宇查询DTO
 */
@Data
@Builder
public class BuildingDTO {

    private Long id;
    private String buildingNo;
    private String buildingName;
    private Integer buildingType;
    private String buildingTypeName;
    private Integer totalFloors;
    private String location;
    private Integer constructionYear;
    private String description;
    private Integer status;
    private String statusName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
