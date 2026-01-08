package com.school.management.application.asset.command;

import lombok.Builder;
import lombok.Data;

/**
 * 学生退宿命令
 */
@Data
@Builder
public class CheckOutCommand {

    private Long studentId;
    private String studentName;
    private Long dormitoryId;
    private Integer bedNumber;
}
