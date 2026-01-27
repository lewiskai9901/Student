package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配教师请求
 */
@Data
public class AssignTeachersRequest {

    @NotEmpty(message = "教师列表不能为空")
    private List<TaskTeacherItem> teachers;

    @Data
    public static class TaskTeacherItem {
        @NotNull(message = "教师ID不能为空")
        private Long teacherId;
        private Boolean isMain;
        private String teachingContent;
    }
}
