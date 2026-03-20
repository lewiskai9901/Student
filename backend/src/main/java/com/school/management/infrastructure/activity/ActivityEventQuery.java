package com.school.management.infrastructure.activity;

import lombok.Data;

/**
 * 活动事件查询条件
 */
@Data
public class ActivityEventQuery {

    private String module;
    private String resourceType;
    private String resourceId;
    private String action;
    private Long userId;
    private String result;
    private String startTime;
    private String endTime;
    private String keyword;

    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
