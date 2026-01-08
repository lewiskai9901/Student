package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评级结果-学生影响实体类
 * 记录评级结果对学生的综测影响
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("rating_result_student_effects")
public class RatingResultStudentEffect implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评级结果ID
     */
    private Long ratingResultId;

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 检查日期
     */
    private LocalDate checkDate;

    /**
     * 评级模板ID
     */
    private Long ratingTemplateId;

    /**
     * 评级模板名称
     */
    private String ratingTemplateName;

    /**
     * 等级ID
     */
    private Long levelId;

    /**
     * 等级名称(快照)
     */
    private String levelName;

    /**
     * 等级编码
     */
    private String levelCode;

    /**
     * 评级对象类型: CLASS/DORMITORY
     */
    private String targetType;

    /**
     * 对象ID
     */
    private Long targetId;

    /**
     * 对象名称
     */
    private String targetName;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 姓名
     */
    private String studentName;

    /**
     * 学生宿舍ID(快照)
     */
    private Long studentDormitoryId;

    /**
     * 学生宿舍号(快照)
     */
    private String studentDormitoryNo;

    /**
     * 学生角色: MEMBER/DORM_LEADER/CLASS_LEADER
     */
    private String studentRole;

    /**
     * 影响类型: 0无影响,1加分,2扣分
     */
    private Integer evaluationEffectType;

    /**
     * 影响维度
     */
    private String evaluationDimension;

    /**
     * 基础分
     */
    private BigDecimal evaluationBaseScore;

    /**
     * 角色额外分
     */
    private BigDecimal evaluationExtraScore;

    /**
     * 总影响分
     */
    private BigDecimal evaluationTotalScore;

    /**
     * 是否已同步到综测
     */
    private Integer syncedToEvaluation;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 综测周期ID
     */
    private Long evaluationPeriodId;

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

    // ==================== 对象类型常量 ====================

    public static final String TARGET_CLASS = "CLASS";           // 班级
    public static final String TARGET_DORMITORY = "DORMITORY";   // 宿舍

    // ==================== 学生角色常量 ====================

    public static final String ROLE_MEMBER = "MEMBER";           // 普通成员
    public static final String ROLE_DORM_LEADER = "DORM_LEADER"; // 宿舍长
    public static final String ROLE_CLASS_LEADER = "CLASS_LEADER"; // 班长

    // ==================== 影响类型常量 ====================

    public static final int EFFECT_NONE = 0;     // 无影响
    public static final int EFFECT_BONUS = 1;    // 加分
    public static final int EFFECT_DEDUCT = 2;   // 扣分

    /**
     * 计算总影响分
     */
    public void calculateTotalScore() {
        BigDecimal base = evaluationBaseScore != null ? evaluationBaseScore : BigDecimal.ZERO;
        BigDecimal extra = evaluationExtraScore != null ? evaluationExtraScore : BigDecimal.ZERO;
        this.evaluationTotalScore = base.add(extra);
    }
}
