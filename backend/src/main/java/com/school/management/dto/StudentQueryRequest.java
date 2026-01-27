package com.school.management.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 学生查询请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class StudentQueryRequest {

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 年级ID（关联年级管理）
     */
    private Long gradeId;

    /**
     * 年级
     */
    private Integer gradeLevel;

    /**
     * 学生状态
     */
    private Integer studentStatus;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 入学日期开始
     */
    private LocalDate admissionDateStart;

    /**
     * 入学日期结束
     */
    private LocalDate admissionDateEnd;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 页大小
     */
    private Integer pageSize = 10;

    /**
     * 班级ID列表(数据权限过滤用)
     */
    private List<Long> classIds;

    /**
     * 组织单元ID列表(数据权限过滤用)
     */
    private List<Long> orgUnitIds;

    /**
     * 仅查询自己的数据(学生角色用)
     */
    private Long selfUserId;
}