package com.school.management.application.student.command;

import lombok.Builder;
import lombok.Data;

/**
 * 分配宿舍命令
 */
@Data
@Builder
public class AssignDormitoryCommand {

    private Long studentId;
    private Long dormitoryId;
    private Integer bedNumber;
}
