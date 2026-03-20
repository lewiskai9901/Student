package com.school.management.application.place.command;

import com.school.management.domain.place.model.valueobject.OccupantType;
import lombok.Data;

import java.time.LocalDate;

/**
 * 入住命令
 */
@Data
public class CheckInCommand {

    private Long placeId;
    private OccupantType occupantType;
    private Long occupantId;
    private Integer positionNo;
    private LocalDate checkInDate;
    private String remark;
}
