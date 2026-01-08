package com.school.management.dto.record;

import lombok.Data;

/**
 * 申诉查询DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class CheckRecordAppealQueryDTO {

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 状态：0=待处理 1=处理中 2=已通过 3=已驳回
     */
    private Integer status;

    /**
     * 申诉人角色：TEACHER/STUDENT
     */
    private String appellantRole;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
