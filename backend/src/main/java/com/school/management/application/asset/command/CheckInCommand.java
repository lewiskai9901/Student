package com.school.management.application.asset.command;

import lombok.Builder;
import lombok.Data;

/**
 * 学生入住命令
 */
@Data
@Builder
public class CheckInCommand {

    private Long studentId;
    private String studentName;
    private Long dormitoryId;
    private Integer bedNumber;
}
