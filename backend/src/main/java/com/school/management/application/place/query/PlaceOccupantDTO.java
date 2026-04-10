package com.school.management.application.place.query;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 场所占用者DTO
 */
@Data
public class PlaceOccupantDTO {

    private Long id;
    private Long placeId;
    private String placeName;
    private String occupantType;
    private Long occupantId;
    private String occupantName;
    private String occupantNo;
    private Integer positionNo;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer status;
    private String statusText;
    private String remark;
    private LocalDateTime createdAt;

    // 额外信息
    private String orgUnitName;
    private Integer gender;
    private String phone;
}
