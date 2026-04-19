package com.school.management.infrastructure.extension.plugins.education.application.student.command;

import com.school.management.infrastructure.extension.plugins.education.domain.student.model.valueobject.StudentStatus;
import lombok.Builder;
import lombok.Data;

/**
 * 变更学生状态命令
 */
@Data
@Builder
public class ChangeStudentStatusCommand {

    private Long studentId;
    private StudentStatus newStatus;
    private String reason;
}
