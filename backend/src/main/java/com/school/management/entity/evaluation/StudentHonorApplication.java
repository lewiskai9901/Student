package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生荣誉申报实体类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("student_honor_application")
public class StudentHonorApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 申报编号
     */
    private String applicationCode;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 综测周期ID
     */
    private Long evaluationPeriodId;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 荣誉类型ID
     */
    private Long honorTypeId;

    /**
     * 荣誉等级配置ID
     */
    private Long honorLevelConfigId;

    /**
     * 荣誉名称/竞赛名称
     */
    private String honorName;

    /**
     * 级别
     */
    private String honorLevel;

    /**
     * 名次/等级
     */
    private String honorRank;

    /**
     * 获得日期
     */
    private LocalDate honorDate;

    /**
     * 颁发机构
     */
    private String issuingAuthority;

    /**
     * 证书编号
     */
    private String certificateNo;

    /**
     * 详细描述
     */
    private String description;

    /**
     * 附件列表(JSON)
     */
    private String attachments;

    /**
     * 关联申报ID(同一事迹更高级别)
     */
    private Long relatedApplicationId;

    /**
     * 是否该事迹最高级别
     */
    private Integer isHighestLevel;

    /**
     * 同一事迹分组标识
     */
    private String sameEventGroup;

    /**
     * 预期得分
     */
    private BigDecimal expectedScore;

    /**
     * 实际得分
     */
    private BigDecimal actualScore;

    /**
     * 影响维度
     */
    private String evaluationDimension;

    /**
     * 状态: 0待提交,1待班级审核,2待系部审核,3已通过,4已驳回,5已撤销
     */
    private Integer status;

    /**
     * 当前审核步骤
     */
    private Integer currentStep;

    /**
     * 班级审核人
     */
    private Long classReviewerId;

    /**
     * 班级审核时间
     */
    private LocalDateTime classReviewTime;

    /**
     * 班级审核意见
     */
    private String classReviewOpinion;

    /**
     * 班级审核结果: 1通过, 2驳回
     */
    private Integer classReviewStatus;

    /**
     * 系部审核人
     */
    private Long deptReviewerId;

    /**
     * 系部审核时间
     */
    private LocalDateTime deptReviewTime;

    /**
     * 系部审核意见
     */
    private String deptReviewOpinion;

    /**
     * 系部审核结果: 1通过, 2驳回
     */
    private Integer deptReviewStatus;

    /**
     * 是否已同步到综测
     */
    private Integer syncedToEvaluation;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 申报人类型: 1学生自主, 2班主任代录, 3系部录入
     */
    private Integer applicantType;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    // ==================== 状态常量 ====================

    public static final int STATUS_DRAFT = 0;              // 待提交
    public static final int STATUS_CLASS_REVIEW = 1;       // 待班级审核
    public static final int STATUS_DEPT_REVIEW = 2;        // 待系部审核
    public static final int STATUS_APPROVED = 3;           // 已通过
    public static final int STATUS_REJECTED = 4;           // 已驳回
    public static final int STATUS_WITHDRAWN = 5;          // 已撤销

    // ==================== 申报人类型常量 ====================

    public static final int APPLICANT_STUDENT = 1;         // 学生自主
    public static final int APPLICANT_TEACHER = 2;         // 班主任代录
    public static final int APPLICANT_DEPARTMENT = 3;      // 系部录入

    // ==================== 审核结果常量 ====================

    public static final int REVIEW_APPROVED = 1;           // 通过
    public static final int REVIEW_REJECTED = 2;           // 驳回
}
