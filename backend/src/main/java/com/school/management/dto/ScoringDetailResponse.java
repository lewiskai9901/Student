package com.school.management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 扣分明细响应
 *
 * @author system
 * @since 1.0.6
 */
@Data
public class ScoringDetailResponse {

    /**
     * 明细ID
     */
    private Long id;

    /**
     * 检查ID
     */
    private Long checkId;

    /**
     * 类别ID
     */
    private Long categoryId;

    /**
     * 类别名称
     */
    private String categoryName;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 扣分项ID
     */
    private Long deductionItemId;

    /**
     * 扣分项名称
     */
    private String deductionItemName;

    /**
     * 关联类型 (1=学生, 2=宿舍, 3=教室)
     */
    private Integer linkType;

    /**
     * 关联类型名称
     */
    private String linkTypeName;

    /**
     * 关联ID
     */
    private Long linkId;

    /**
     * 关联名称 (学生姓名/宿舍号/教室号)
     */
    private String linkName;

    /**
     * 扣分分数
     */
    private BigDecimal deductScore;

    /**
     * 备注
     */
    private String remark;

    /**
     * 照片URL
     */
    private String photoUrls;

    /**
     * 申诉状态 (0=未申诉, 1=申诉中, 2=申诉通过, 3=申诉驳回)
     */
    private Integer appealStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
