package com.school.management.dto.quickentry;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 快捷录入 - 录入记录DTO
 *
 * @author system
 * @since 1.0.7
 */
@Data
public class QuickEntryRecordDTO {

    /**
     * 记录ID (daily_check_details表的id)
     */
    private Long id;

    /**
     * 扣分项ID
     */
    private Long deductionItemId;

    /**
     * 扣分项名称
     */
    private String deductionItemName;

    /**
     * 类别名称
     */
    private String categoryName;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 扣分分数
     */
    private BigDecimal deductScore;

    /**
     * 备注
     */
    private String remark;

    /**
     * 照片URL列表
     */
    private List<String> photoUrls;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否可撤销 (仅当天录入的可撤销)
     */
    private Boolean canRevoke;
}
