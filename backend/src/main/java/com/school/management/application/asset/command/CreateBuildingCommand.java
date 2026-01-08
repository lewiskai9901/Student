package com.school.management.application.asset.command;

import lombok.Builder;
import lombok.Data;

/**
 * 创建楼宇命令
 */
@Data
@Builder
public class CreateBuildingCommand {

    private String buildingNo;
    private String buildingName;
    private Integer buildingType;
    private Integer totalFloors;
    private String location;
    private Integer constructionYear;
    private String description;
}
