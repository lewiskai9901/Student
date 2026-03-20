package com.school.management.interfaces.rest.organization;

import lombok.Data;

/**
 * 更新专业方向请求
 */
@Data
public class UpdateMajorDirectionRequest {
    private String directionName;
    private String level;
    private Integer years;
    private Integer isSegmented;
    private String phase1Level;
    private Integer phase1Years;
    private String phase2Level;
    private Integer phase2Years;
    private String remarks;
}
