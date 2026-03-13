package com.school.management.interfaces.rest.space.dto;

import com.school.management.domain.space.model.valueobject.BuildingType;
import com.school.management.domain.space.model.valueobject.RoomType;
import com.school.management.domain.space.model.valueobject.SpaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 创建场所请求
 */
@Data
public class CreateSpaceRequest {

    @NotNull(message = "场所类型不能为空")
    private SpaceType spaceType;

    private String spaceCode;

    @NotBlank(message = "场所名称不能为空")
    private String spaceName;

    /**
     * 房间类型（仅ROOM类型需要）
     */
    private RoomType roomType;

    /**
     * 楼宇类型（仅BUILDING类型需要）
     */
    private BuildingType buildingType;

    /**
     * 楼号（仅BUILDING类型需要，如 1, A, 甲）
     */
    private String buildingNo;

    /**
     * 房间号（仅ROOM类型需要，如 101, 302）
     */
    private String roomNo;

    /**
     * 父级ID（BUILDING需要CAMPUS的ID，FLOOR需要BUILDING的ID，ROOM需要FLOOR的ID）
     */
    private Long parentId;

    /**
     * 楼层号（仅FLOOR类型需要）
     */
    private Integer floorNumber;

    /**
     * 容量（仅ROOM类型需要）
     */
    private Integer capacity;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 归属班级ID
     */
    private Long classId;

    /**
     * 责任人用户ID
     */
    private Long responsibleUserId;

    /**
     * 性别类型：0-不限，1-男，2-女
     */
    private Integer genderType;

    /**
     * 描述
     */
    private String description;

    /**
     * 扩展属性
     */
    private Map<String, Object> attributes;
}
