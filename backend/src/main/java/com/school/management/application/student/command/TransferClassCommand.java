package com.school.management.application.student.command;

import lombok.Builder;
import lombok.Data;

/**
 * 学生转班命令
 */
@Data
@Builder
public class TransferClassCommand {

    private Long studentId;
    private Long newClassId;
    private String reason;
}
