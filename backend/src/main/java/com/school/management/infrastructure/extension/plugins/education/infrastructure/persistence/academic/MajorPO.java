package com.school.management.infrastructure.persistence.academic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专业持久化对象
 * 映射到 majors 表
 */
@Data
@TableName("majors")
public class MajorPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 专业编码
     */
    private String majorCode;

    /**
     * 所属组织单元ID（映射到OrgUnit）
     */
    @TableField("org_unit_id")
    private Long orgUnitId;

    /**
     * 专业描述
     */
    private String description;

    /**
     * 状态(0-禁用 1-启用)
     */
    private Integer status;

    // ========== 新增字段: 技工院校增强 ==========

    /**
     * 所属专业大类编码
     */
    private String majorCategoryCode;

    /**
     * 招生对象
     */
    private String enrollmentTarget;

    /**
     * 办学形式
     */
    private String educationForm;

    /**
     * 专业带头人用户ID
     */
    private Long leadTeacherId;

    /**
     * 专业带头人姓名
     */
    private String leadTeacherName;

    /**
     * 批准设置年份
     */
    private Integer approvalYear;

    /**
     * 专业状态(PREPARING/ENROLLING/SUSPENDED/REVOKED)
     */
    private String majorStatus;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 删除标志(0-未删除 1-已删除)
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
