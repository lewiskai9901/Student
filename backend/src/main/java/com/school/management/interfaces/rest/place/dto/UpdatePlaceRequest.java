package com.school.management.interfaces.rest.place.dto;

import lombok.Data;

import java.util.Map;

/**
 * 更新场所请求
 */
@Data
public class UpdatePlaceRequest {

    private String placeName;

    private String description;

    /**
     * 楼号（仅BUILDING类型有效，如 1, A, 甲）
     */
    private String buildingNo;

    /**
     * 房间号（仅ROOM类型有效，如 101, 302）
     */
    private String roomNo;

    private Integer capacity;

    private Long orgUnitId;

    private Long classId;

    private Long responsibleUserId;

    private Integer genderType;

    private Map<String, Object> attributes;
}
