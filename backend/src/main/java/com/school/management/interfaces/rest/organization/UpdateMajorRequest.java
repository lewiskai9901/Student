package com.school.management.interfaces.rest.organization;

import lombok.Data;

/**
 * 更新专业请求
 */
@Data
public class UpdateMajorRequest {
    private String majorName;
    private String description;
    private Integer status;
}
