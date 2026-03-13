package com.school.management.interfaces.rest.space.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量分配班级请求
 */
@Data
public class BatchAssignClassRequest {

    @NotEmpty(message = "场所ID列表不能为空")
    private List<Long> spaceIds;

    @NotNull(message = "班级ID不能为空")
    private Long classId;
}
