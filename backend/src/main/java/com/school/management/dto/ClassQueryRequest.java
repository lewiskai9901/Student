package com.school.management.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Year;
import java.util.List;

/**
 * 班级查询请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClassQueryRequest extends BaseQueryRequest {

    /**
     * 班级名称
     */
    private String className;

    /**
     * 班级编码
     */
    private String classCode;

    /**
     * 年级
     */
    private Integer gradeLevel;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 入学年份
     */
    private Year enrollmentYear;

    /**
     * 班级类型: 1普通班 2重点班 3实验班
     */
    private Integer classType;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    // ========== 数据权限过滤字段 ==========

    /**
     * 班级ID列表(数据权限过滤用)
     */
    private List<Long> classIds;

    /**
     * 部门ID列表(数据权限过滤用)
     */
    private List<Long> departmentIds;

    /**
     * 排序字段
     */
    private String sortBy = "gradeLevel";

    /**
     * 排序方向: asc/desc
     */
    private String sortOrder = "asc";

    /**
     * 自定义setter防止SQL注入
     */
    public void setSortOrder(String sortOrder) {
        if (sortOrder == null || sortOrder.trim().isEmpty()) {
            this.sortOrder = "asc";
        } else {
            String normalized = sortOrder.trim().toLowerCase();
            if ("asc".equals(normalized) || "desc".equals(normalized)) {
                this.sortOrder = normalized.toUpperCase();
            } else {
                this.sortOrder = "ASC"; // 默认值,防止注入
            }
        }
    }

    /**
     * 自定义setter防止SQL注入 - 白名单验证sortBy字段
     */
    public void setSortBy(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            this.sortBy = "gradeLevel";
        } else {
            // 白名单验证
            switch (sortBy) {
                case "gradeLevel":
                case "className":
                case "studentCount":
                case "createdAt":
                    this.sortBy = sortBy;
                    break;
                default:
                    this.sortBy = "gradeLevel"; // 默认值
            }
        }
    }
}