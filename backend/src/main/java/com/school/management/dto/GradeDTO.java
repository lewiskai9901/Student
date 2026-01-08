package com.school.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 年级DTO
 * 年级为全校共享资源，不再绑定特定院系
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-29
 */
@Data
public class GradeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 年级ID
     */
    private Long id;

    /**
     * 年级名称(如"2024级高一")
     */
    private String gradeName;

    /**
     * 年级编码(如"GRADE_2024_10")
     */
    private String gradeCode;

    /**
     * 入学年份
     */
    private Integer enrollmentYear;

    /**
     * 年级主任ID
     */
    private Long gradeDirectorId;

    /**
     * 年级主任姓名
     */
    private String gradeDirectorName;

    /**
     * 班级数量
     */
    private Integer classCount;

    /**
     * 学生总数
     */
    private Integer studentCount;

    /**
     * 标准班级人数
     */
    private Integer standardClassSize;

    /**
     * 平均班级人数
     */
    private Double averageClassSize;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态(1=正常,0=停用)
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否删除
     */
    private Integer deleted;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "正常" : "停用";
    }
}
